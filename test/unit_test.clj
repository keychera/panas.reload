(ns unit-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [hickory.core :refer [parse as-hickory]]
            [hickory.select :as s]
            [panas.reload :refer [with-akar]]))

(defn get-html [res-path]
  (io/file (io/resource (str "htmls/" res-path))))

(defn have?
  "true if coll have elm"
  [coll elm]
  (some #(= elm %) coll))

(defn htmx-installed? [{:keys [body]}]
  (let [script-srcs (->> (parse body) as-hickory
                         (s/select (s/tag :head)) first :content
                         (filter #(= (:tag %) :script)) (map :attrs) (map :src)
                         (into #{}))]
    (is (have? script-srcs "https://unpkg.com/htmx.org@1.8.4"))
    (is (have? script-srcs "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js"))))

(deftest test!with-akar
  (testing "Testing with-akar for"
    (let [normal-html-file (get-html "01_normal.html")]
      (testing "normal html"
        (testing "with File :body"
          (htmx-installed? (with-akar {:body normal-html-file})))
        (testing "with String :body"
          (htmx-installed? (with-akar {:body (slurp normal-html-file)})))))))


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
