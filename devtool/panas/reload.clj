(ns panas.reload
  (:require [babashka.fs :as fs]
            [babashka.pods :as pods]
            [clojure.core.async :refer [<! go-loop timeout]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.server :refer [as-channel run-server send!]]
            [panas.default :refer [reloadable?]])
  (:import (java.nio.file Path)))

(pods/load-pod 'retrogradeorbit/bootleg "0.1.9")
(require '[pod.retrogradeorbit.bootleg.utils :as utils])
(require '[pod.retrogradeorbit.hickory.select :as s])
(pods/load-pod 'org.babashka/fswatcher "0.0.3")
(require '[pod.babashka.fswatcher :as fw])

(defonce ^:dynamic panas-url "0.0.0.0")
(defonce ^:dynamic panas-port 8090)
(defonce panas-clients (atom nil))
(defonce current-url (atom "/"))

(declare panas-websocket with-akar)
(defn panas-middleware [handler]
  (fn [req]
    (let [{uri :uri verb :request-method} req
          paths (-> uri (str/split #"/") rest vec)]
      (when (reloadable? req) ;; need better way to detect reloadable url
        (reset! current-url uri)
        (println "currently on" uri))
      (match [verb paths]
        [:get ["panas"]] (panas-websocket req)
        :else (let [res (handler req)]
                (cond (:websocket? req) res
                      (= (:async-channel req) (:body res)) res
                      (and (= (type res) java.io.File) (not= (fs/extension res) "html")) res
                      :else (with-akar res)))))))

(defn panas-websocket [req]
  (if (:websocket? req)
    (as-channel req
                {:on-open  (fn [ch]
                             (println "[panas] on-open")
                             (swap! panas-clients assoc (hash ch) ch))
                 :on-close (fn [ch status]
                             (println "[panas] on-close" status)
                             (swap! panas-clients dissoc (hash ch)))})
    {:status 200 :body "<h1>It's not panas here</h1>"}))

(declare body->str not-empty? insert-htmx insert-htmx-ws css-refresher-js)
(defn with-akar [response]
  (let [body-str (body->str response)
        hick-seq (utils/convert-to body-str :hickory-seq)
        root-html? (->> hick-seq (map :tag) (filter #(= % :html)) not-empty?)]
    (if-not root-html? response
            (let [;; conj with "\n"  ensure `partition-by` returns at least three element, destructuring [_ front] ignores it back
                  ;; TODO bug: this transformation causes emoji unicode to break
                  [[_ & front] [html] & rest] (partition-by #(= (:tag %) :html) (-> hick-seq (conj "\n")))
                  [[_ & body-front] [body] & body-rest] (partition-by #(= (:tag %) :body) (-> (:content html) seq (conj "\n")))
                  [[_ & head-front] [head] & head-rest] (partition-by #(= (:tag %) :head) (-> body-front (conj "\n")))
                  akar-head (-> head insert-htmx insert-htmx-ws)
                  akar-body (assoc body
                                   :attrs (merge (:attrs body) {:id "akar" :hx-ext "ws" :ws-connect "/panas"})
                                   :content (conj (:content body) css-refresher-js))
                  akar-html (assoc html :content (->> [head-front akar-head head-rest akar-body body-rest] (remove nil?) flatten vec))
                  akar-seq (->> [front akar-html rest] (remove nil?) flatten seq)]
              (assoc response :body (utils/convert-to akar-seq :html))))))

(defn insert-htmx [head]
  (let [content (:content head)
        scripts (->> content (filter #(= (:tag %) :script)))
        htmx? (->> scripts (map :attrs) (map :src) (filter #(str/includes? % "htmx.org")) not-empty?)]
    (if htmx? head
        (assoc head :content (conj content {:type :element :attrs {:src "https://unpkg.com/htmx.org@1.8.4"} :tag :script :content nil})))))

(defn insert-htmx-ws [head]
  (let [content (:content head)
        scripts (->> content (filter #(= (:tag %) :script)))
        htmx-ws? (->> scripts (map :attrs) (map :src) (filter #(str/includes? % "dist/ext/ws.js")) not-empty?)]
    (if htmx-ws? head
        (assoc head :content (conj content {:type :element :attrs {:src "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js"} :tag :script :content nil})))))

(def css-refresher-js {:type :element :attrs nil :tag :script
                       :content [(slurp (io/resource "panas/cssRefresher.js"))]})

;; https://clojuredocs.org/clojure.core/empty_q
(defn not-empty? [coll] (seq coll))

(defn body->str [{:keys [body]}]
  (cond (#{java.io.File} (type body)) (slurp body)
        :else body))

(defn tell-clients [router]
  (let [clients @panas-clients]
    (if (empty? clients) (println "[panas][warn] no opened panas client!")
        (doseq [[_ client] clients]
          (let [msg {:body (let [res (router {:request-method :get :uri @current-url}) ;; assuming :get url return main html body
                                 hick-res (utils/convert-to (body->str res) :hickory)
                                 [{:keys [attrs] :as body}] (s/select (s/child (s/tag :body)) hick-res)]
                             (-> body
                                 (assoc :attrs (assoc attrs :id "akar" :hx-swap-oob "innerHtml"))
                                 (assoc :tag :div)
                                 (utils/convert-to :html)))}]
            (send! client msg))))))

(defn on-event [fs-event action-fn]
  (when (= :write (:type fs-event))
    (println "======")
    (try
      (let [changed-file (:path fs-event)]
        (cond
          (str/ends-with? changed-file ".clj") (do (println "[panas][clj] reloading" changed-file)
                                                   (load-file changed-file))
          (str/ends-with? changed-file ".html") (println "[panas][html] changes on" changed-file)
          :else (println "[panas][other] changes on" changed-file)))
      (action-fn)
      (catch Throwable e
        (let [{:keys [cause]} (Throwable->map e)]
          (println) (println "[panas][ERROR]" cause) (println))))))

(defn default-dir [] (some-> (io/resource "") .toURI (Path/of) .toString))

(defn -main
  ([router server-opts]
   (-main router server-opts {}))
  ([router {:keys [url port] :as server-opts} {:keys [watch-dir]}]
   (binding [panas-url (or url panas-url)
             panas-port (or port panas-port)]
     (let [root-url (str "http://" panas-url ":" panas-port)]
       (println "[panas] starting the server")
       (run-server (panas-middleware router) server-opts)
       (let [latest-event (atom nil)
             dir (or (some-> watch-dir fs/absolutize .toString) (default-dir))]
         (println "[panas] watching" dir)
         (fw/watch dir (fn [e] (reset! latest-event e)) {:recursive true :delay-ms 100})
         (go-loop [time-unit 1]
           (<! (timeout 100))
           (let [[event _] (reset-vals! latest-event nil)]
             (some-> event (on-event
                            (fn []
                              (println "[panas] swapping" (str root-url @current-url))
                              (-> router tell-clients)))))
           (recur (inc time-unit))))
       (println "[panas] serving" root-url)
       @(promise)))))
