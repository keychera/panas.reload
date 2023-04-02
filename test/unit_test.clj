(ns unit-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [hickory.core :refer [parse as-hickory]]
            [hickory.select :as s]
            [panas.reload :refer [with-akar]]))

(defn get-html [res-path]
  (io/file (io/resource (str "htmls/" res-path))))

(defn should-have [count item freq]
  (= (get freq item) count))

(defn htmx-installed?
  ([ring-res] (htmx-installed? ring-res nil nil))
  ([{:keys [body]} htmx-src htmx-ws-src]
   (let [script-srcs (->> (parse body) as-hickory
                          (s/select (s/tag :head)) first :content
                          (filter #(= (:tag %) :script)) (map :attrs) (map :src)
                          frequencies)]
     (is (should-have 1 (or htmx-src "https://unpkg.com/htmx.org@1.8.4") script-srcs))
     (is (should-have 1 (or htmx-ws-src "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js") script-srcs)))))

(deftest test!with-akar
  (testing "Testing with-akar for"
    (let [normal-html-file (get-html "01_normal.html")]
      (testing "normal html"
        (testing "with File :body"
          (htmx-installed? (with-akar {:body normal-html-file})))
        (testing "with String :body"
          (htmx-installed? (with-akar {:body (slurp normal-html-file)})))))
    (let [with-htmx-file (get-html "02_with_htmx.html")]
      (testing "html with htmx"
        (testing "with File :body"
          (htmx-installed? (with-akar {:body with-htmx-file})))
        (testing "with String :body"
          (htmx-installed? (with-akar {:body (slurp with-htmx-file)})))))
    (let [with-htmx-ws-file (get-html "03_with_htmx_ws.html")]
      (testing "html with htmx and ws"
        (testing "with File :body"
          (htmx-installed? (with-akar {:body with-htmx-ws-file})))
        (testing "with String :body"
          (htmx-installed? (with-akar {:body (slurp with-htmx-ws-file)})))))
    (let [htmx-src "https://unpkg.com/htmx.org@1.8.6" ;; the same as inside the file
          htmx-ws-src "https://unpkg.com/htmx.org@1.8.6/dist/ext/ws.js"
          with-htmx-ws-file (get-html "04_with_newer_htmx.html")]
      (testing "html with newer htmx"
        (testing "with File :body"
          (htmx-installed? (with-akar {:body with-htmx-ws-file}) htmx-src htmx-ws-src))
        (testing "with String :body"
          (htmx-installed? (with-akar {:body (slurp with-htmx-ws-file)}) htmx-src htmx-ws-src))))))


;; String

;; html -> +htmx +ws
;; htmx -> +ws
;; htmx ws -> no change
;; htmx new version -> no change

;; html no head
;; html no body
;; html no head body
;; html imbalance
;; html no first tag

;; File, the same as above but via file

;; not html at all (.css .svg .png)
;; not File or string (InputStream, ISeq)
