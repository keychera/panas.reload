(ns panas.reload
  (:require [babashka.fs :as fs]
            [babashka.pods :as pods]
            [clojure.core.async :refer [<! go-loop timeout]]
            [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.server :refer [as-channel run-server send!]])
  (:import (java.nio.file Path)))

(pods/load-pod 'retrogradeorbit/bootleg "0.1.9")
(require '[pod.retrogradeorbit.bootleg.utils :as utils])
(require '[pod.retrogradeorbit.hickory.select :as s])
(pods/load-pod 'org.babashka/fswatcher "0.0.3")
(require '[pod.babashka.fswatcher :as fw])

(defonce panas-ch (atom nil))
(defonce current-url (atom "/"))

(defn panas-websocket [req]
  (if (:websocket? req)
    (as-channel req
                {:on-open  (fn [ch]
                             (println "[panas] on-open")
                             (reset! panas-ch ch))
                 :on-close (fn [_ status]
                             (println "[panas] on-close" status)
                             (reset! panas-ch nil))})
    {:status 200 :body "<h1>It's not panas here</h1>"}))

;; https://clojuredocs.org/clojure.core/empty_q
(defn not-empty? [coll] (seq coll))

(defn with-htmx [head]
  (let [content (:content head)
        scripts (->> content (filter #(= (:tag %) :script)))
        htmx? (->> scripts (map :attrs) (map :src) (filter #(str/includes? % "htmx.org")) not-empty?)]
    (if htmx? head
        (assoc head :content (conj content {:type :element :attrs {:src "https://unpkg.com/htmx.org@1.8.4"} :tag :script :content nil})))))

(defn with-htmx-ws [head]
  (let [content (:content head)
        scripts (->> content (filter #(= (:tag %) :script)))
        htmx-ws? (->> scripts (map :attrs) (map :src) (filter #(str/includes? % "dist/ext/ws.js")) not-empty?)]
    (if htmx-ws? head
        (assoc head :content (conj content {:type :element :attrs {:src "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js"} :tag :script :content nil})))))

(def css-refresher-js {:type :element :attrs nil :tag :script
                       :content [(slurp (io/resource "panas/cssRefresher.js"))]})

(defn body->str [{:keys [body]}]
  (cond (#{java.io.File} (type body)) (slurp body)
        :else body))

(defn with-akar
  [response]
  (let [body-str (body->str response)
        hick-seq (utils/convert-to body-str :hickory-seq)
        root-html? (->> hick-seq (map :tag) (filter #(= % :html)) not-empty?)]
    (if-not root-html? response
            (let [;; conj with "\n"  ensure `partition-by` returns at least three element, destructuring [_ front] ignores it back
                  ;; TODO bug: this transformation causes emoji unicode to break
                  [[_ & front] [html] & rest] (partition-by #(= (:tag %) :html) (-> hick-seq (conj "\n")))
                  [[_ & body-front] [body] & body-rest] (partition-by #(= (:tag %) :body) (-> (:content html) seq (conj "\n")))
                  [[_ & head-front] [head] & head-rest] (partition-by #(= (:tag %) :head) (-> body-front (conj "\n")))
                  akar-head (-> head with-htmx with-htmx-ws)
                  akar-body (assoc body
                                   :attrs (merge (:attrs body) {:id "akar" :hx-ext "ws" :ws-connect "/panas"})
                                   :content (conj (:content body) css-refresher-js))
                  akar-html (assoc html :content (->> [head-front akar-head head-rest akar-body body-rest] (remove nil?) flatten vec))
                  akar-seq (->> [front akar-html rest] (remove nil?) flatten seq)]
              (assoc response :body (utils/convert-to akar-seq :html))))))

(defn panas-middleware [handler]
  (fn [req]
    (let [uri (:uri req) ;; probably need better way to detect reloadable url
          verb (:request-method req)
          paths (vec (rest (str/split uri #"/")))]
      (when (and (= verb :get)
                 (not (:websocket? req))
                 (not (str/starts-with? uri "/css"))
                 (not (str/starts-with? uri "/favicon.ico"))
                 (not (str/starts-with? uri "/static")))
        (reset! current-url uri)
        (println "currently on" uri))
      (match [verb paths]
        [:get ["panas"]] (panas-websocket req)
        :else (let [res (handler req)]
                (cond (:websocket? req) res
                      (= (:async-channel req) (:body res)) res
                      (and (= (type res) java.io.File) (not= (fs/extension res) "html")) res
                      :else (with-akar res)))))))

(defn start-panasin [server-to-embed opts]
  (run-server (panas-middleware server-to-embed) opts))

(defn default-dir [] (some-> (io/resource "") .toURI (Path/of) .toString))

(defn -main
  ([handler server-opts]
   (-main handler server-opts {}))
  ([handler {:keys [url port] :as server-opts} {:keys [watch-dir]}]
   (let [ns-name (symbol (namespace handler))
         router-name (symbol (name handler))
         _  (require ns-name)
         router (some-> (find-ns ns-name) (ns-resolve router-name))
         url (str "http://" (or url "0.0.0.0") ":" (or port 8090))
         swap-body! (fn [embedded-server ch]
                      (println "[panas] swapping" (str url @current-url))
                      (if (nil? ch) (println "[panas][warn] no opened panas client!")
                          (send! ch {:body (let [res (embedded-server {:request-method :get :uri @current-url}) ;; assuming :get url return main html body
                                                 hick-res (utils/convert-to (body->str res) :hickory)
                                                 [{:keys [attrs] :as body}] (s/select (s/child (s/tag :body)) hick-res)]
                                             (-> body
                                                 (assoc :attrs (assoc attrs :id "akar" :hx-swap-oob "innerHtml"))
                                                 (assoc :tag :div)
                                                 (utils/convert-to :html)))})))]
     (when (nil? router)
       (println (str "[panas] `" handler "` not found!"))
       (System/exit 1))
     (println "[panas] starting" handler)
     (start-panasin router server-opts)
     (let [latest-event (atom nil)
           dir (or (some-> watch-dir fs/absolutize .toString) (default-dir))
           event-handler (fn [event]
                           (when (= :write (:type event))
                             (println "======")
                             (try
                               (let [changed-file (:path event)]
                                 (cond
                                   (str/ends-with? changed-file ".clj") (do (println "[panas][clj] reloading" changed-file)
                                                                            (load-file changed-file))
                                   (str/ends-with? changed-file ".html") (println "[panas][html] changes on" changed-file)
                                   :else (println "[panas][other] changes on" changed-file)))
                               (swap-body! router @panas-ch)
                               (catch Throwable e
                                 (let [{:keys [cause]} (Throwable->map e)]
                                   (println) (println "[panas][ERROR]" cause) (println))))))]
       (println "[panas] watching" dir)
       (fw/watch dir (fn [e] (reset! latest-event e)) {:recursive true :delay-ms 100})
       (go-loop [time-unit 1]
         (<! (timeout 100))
         (let [[event _] (reset-vals! latest-event nil)]
           (some-> event event-handler))
         (recur (inc time-unit))))
     (println "[panas] serving" url)
     @(promise))))
