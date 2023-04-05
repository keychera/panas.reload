(ns unit-test
  (:require [clojure.data :refer [diff]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [hickory.core :refer [as-hickory parse]]
            [hickory.select :as s]
            [panas.reload :refer [with-akar]]))

(defn get-html [res-path]
  (io/file (io/resource (str "htmls/" res-path))))

(defn at [hickory tag] (->> hickory (s/select (s/tag tag)) first))

(defn should-have-src? [cnt src at]
  (->> at :content
       (filter #(= (get-in % [:attrs :src]) src))
       count (= cnt)))

(defn should-have-attrs? [attrs at]
  (let [[_ _ both] (diff (at :attrs) attrs)]
    (= both attrs)))

;; make sure this value is the same as test htmls
(def existing-htmx "https://unpkg.com/htmx.org@1.8.6")
(def existing-ws "https://unpkg.com/htmx.org@1.8.6/dist/ext/ws.js")

(deftest with-akar!test-normal-htmx
  (testing "Testing with-akar for normal html"
    (let [normal-html-file (get-html "01_normal.html")
          normal-html-str (slurp normal-html-file)
          prev-hickory (-> (parse normal-html-str) as-hickory)]
      (testing "with File :body"
        (let [{body :body} (with-akar {:body normal-html-file})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (:attrs prev-hickory) (at hickory :body)))))
      (testing "with String :body"
        (let [{body :body} (with-akar {:body normal-html-str})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (:attrs prev-hickory) (at hickory :body))))))))

(deftest with-akar!test-existing-htmx
  (testing "Testing with-akar for html with existing htmx"
    (let [with-htmx-file (get-html "02_with_htmx.html")
          with-htmx-str (slurp with-htmx-file)
          prev-hickory (-> (parse with-htmx-str) as-hickory)]
      (testing "with File :body"
        (let [{body :body} (with-akar {:body with-htmx-file})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 existing-htmx (at hickory :head)))
          (is (should-have-src? 0 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (:attrs prev-hickory) (at hickory :body)))))
      (testing "with String :body"
        (let [{body :body} (with-akar {:body with-htmx-str})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 existing-htmx (at hickory :head)))
          (is (should-have-src? 0 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (:attrs prev-hickory) (at hickory :body))))))))

(deftest with-akar!test-existing-htmx-ws
  (testing "Testing with-akar for html with existing htmx and ws extension"
    (let [with-htmx-ws-file (get-html "03_with_htmx_ws.html")
          with-htmx-ws-str (slurp with-htmx-ws-file)
          prev-hickory (-> (parse with-htmx-ws-str) as-hickory)]
      (testing "with File :body"
        (let [{body :body} (with-akar {:body with-htmx-ws-file})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 existing-htmx (at hickory :head)))
          (is (should-have-src? 1 existing-ws (at hickory :head)))
          (is (should-have-src? 0 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 0 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (:attrs prev-hickory) (at hickory :body)))))
      (testing "with String :body"
        (let [{body :body} (with-akar {:body with-htmx-ws-str})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 existing-htmx (at hickory :head)))
          (is (should-have-src? 1 existing-ws (at hickory :head)))
          (is (should-have-src? 0 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 0 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (:attrs prev-hickory) (at hickory :body))))))))

;; String

;; html no head
;; html no body
;; html no head body
;; html imbalance
;; html no first tag

;; File, the same as above but via file

;; not html at all (.css .svg .png)
;; not File or string (InputStream, ISeq)
