(ns panas.with-akar-test
  (:require [clojure.data :refer [diff]]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [hickory.core :refer [as-hickory parse]]
            [hickory.select :as s]
            [panas.reload :refer [with-akar]]))

(defn get-res [res-path]
  (io/file (io/resource res-path)))

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
    (let [normal-html-file (get-res "01_normal.html")
          normal-html-str (slurp normal-html-file)
          prev-hickory (-> (parse normal-html-str) as-hickory)]
      (testing "with File :body"
        (let [{body :body} (with-akar {:body normal-html-file})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (get prev-hickory :attrs) (at hickory :body)))))
      (testing "with String :body"
        (let [{body :body} (with-akar {:body normal-html-str})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (get prev-hickory :attrs) (at hickory :body))))))))

(deftest with-akar!test-existing-htmx
  (testing "Testing with-akar for html with existing htmx"
    (let [with-htmx-file (get-res "02_with_htmx.html")
          with-htmx-str (slurp with-htmx-file)
          prev-hickory (-> (parse with-htmx-str) as-hickory)]
      (testing "with File :body"
        (let [{body :body} (with-akar {:body with-htmx-file})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 existing-htmx (at hickory :head)))
          (is (should-have-src? 0 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (get prev-hickory :attrs) (at hickory :body)))))
      (testing "with String :body"
        (let [{body :body} (with-akar {:body with-htmx-str})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 existing-htmx (at hickory :head)))
          (is (should-have-src? 0 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 1 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (get prev-hickory :attrs) (at hickory :body))))))))

(deftest with-akar!test-existing-htmx-ws
  (testing "Testing with-akar for html with existing htmx and ws extension"
    (let [with-htmx-ws-file (get-res "03_with_htmx_ws.html")
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
          (is (should-have-attrs? (get prev-hickory :attrs) (at hickory :body)))))
      (testing "with String :body"
        (let [{body :body} (with-akar {:body with-htmx-ws-str})
              hickory (->> (parse body) as-hickory)]
          (is (should-have-src? 1 existing-htmx (at hickory :head)))
          (is (should-have-src? 1 existing-ws (at hickory :head)))
          (is (should-have-src? 0 "https://unpkg.com/htmx.org@1.8.4" (at hickory :head)))
          (is (should-have-src? 0 "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js" (at hickory :head)))
          (is (should-have-attrs? {:id "akar" :hx-ext "ws" :ws-connect "/panas"} (at hickory :body)))
          (is (should-have-attrs? (get prev-hickory :attrs) (at hickory :body))))))))

(deftest with-akar!test-no-head
  (testing "Testing with-akar for html with no head"
    (let [no-head-html (get-res "04_no_head.html")
          no-head-str (slurp no-head-html)]
      (testing "with File :body"
        (let [{body :body} (with-akar {:body no-head-html})]
          (is (= body no-head-html) "should have no change")))
      (testing "with String :body"
        (let [{body :body} (with-akar {:body no-head-str})]
          (is (= body no-head-str) "should have no change"))))))

(deftest with-akar!test-no-body
  (testing "Testing with-akar for html with no body"
    (let [no-body-html (get-res "05_no_body.html")
          no-body-str (slurp no-body-html)]
      (testing "with File :body"
        (let [{body :body} (with-akar {:body no-body-html})]
          (is (= body no-body-html) "should have no change")))
      (testing "with String :body"
        (let [{body :body} (with-akar {:body no-body-str})]
          (is (= body no-body-str) "should have no change"))))))

(deftest with-akar!test-no-html-tag
  (testing "Testing with-akar for html with html tag"
    (let [no-htmltag-html (get-res "06_no_html_tag.html")
          no-htmltag-str (slurp no-htmltag-html)]
      (testing "with File :body"
        (let [{body :body} (with-akar {:body no-htmltag-html})]
          (is (= body no-htmltag-html) "should have no change")))
      (testing "with String :body"
        (let [{body :body} (with-akar {:body no-htmltag-str})]
          (is (= body no-htmltag-str) "should have no change"))))))

(deftest with-akar!test-not-html-at-all
  (testing "Testing with-akar for css body"
    (let [css-body (get-res "mock/style.css")
          {body :body} (with-akar {:body css-body})]
      (is (= body css-body) "should have no change")))
  (testing "Testing with-akar for svg body"
    (let [svg-body (get-res "mock/some.svg")
          {body :body} (with-akar {:body svg-body})]
      (is (= body svg-body) "should have no change")))
  ;; these two below might change
  (testing "Testing with-akar for Input stream"
    (let [input-stream-body (io/input-stream (io/resource "01_normal.html"))
          {body :body} (with-akar {:body input-stream-body})]
      (is (= body input-stream-body) "should have no change")))
  (testing "Testing with-akar for ISeq"
    (let [iseq-body ["this" "is" "hello!"]
          {body :body} (with-akar {:body iseq-body})]
      (is (= body iseq-body) "should have no change"))))
