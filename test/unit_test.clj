(ns unit-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [panas.reload :refer [with-akar]]))

(defn get-html [res-path]
  (io/file (io/resource (str "htmls/" res-path))))

(declare htmx-installed?)
(deftest test!with-akar
  (testing "Testing with-akar for"
    (let [normal-html-file (get-html "01_normal.html")]
      (testing "normal html"
        (is (htmx-installed? (with-akar {:body normal-html-file})) "with File :body")
        (is (htmx-installed? (with-akar {:body (slurp normal-html-file)})) "with String :body")))))

(defn htmx-installed? [{:keys [body]}] false)

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
