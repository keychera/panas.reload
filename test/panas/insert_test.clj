(ns panas.insert-test
  (:require [clojure.data :refer [diff]]
            [clojure.test :refer [deftest is testing]]
            [panas.reload :refer [insert-htmx insert-htmx-ws]]))


(def header-hickory {:type :element, :attrs nil, :tag :head
                     :content
                     [{:type :element, :attrs {:charset "UTF-8"}, :tag :meta, :content nil}
                      {:type :element, :attrs {:http-equiv "X-UA-Compatible", :content "IE=edge"}, :tag :meta, :content nil}
                      {:type :element,
                       :attrs {:name "viewport", :content "width=device-width, initial-scale=1.0"},
                       :tag :meta,
                       :content nil}
                      {:type :element, :attrs nil, :tag :title, :content ["Document"]}]})

(deftest insert-htmx!test-normal-hickory
  (testing "Testing insert-htmx with normal hickory"
    (let [expectation #{{:type :element, :attrs {:src "https://unpkg.com/htmx.org@1.8.4"}, :tag :script, :content nil}}
          orig-content (set (:content header-hickory))
          inserted-content (set (:content (insert-htmx header-hickory)))
          [_ on-inserted on-both] (diff orig-content inserted-content)]
      (is (= orig-content on-both) "should contain original headers")
      (is (= expectation on-inserted) "should contain inserted htmx header"))))

(deftest insert-htmx-ws!test-normal-hickory
  (testing "Testing insert-htmx with normal hickory"
    (let [expectation  #{{:type :element, :attrs {:src "https://unpkg.com/htmx.org@1.8.4/dist/ext/ws.js"}, :tag :script, :content nil}}
          orig-content (set (:content header-hickory))
          inserted-content (set (:content (insert-htmx-ws header-hickory)))
          [_ on-inserted on-both] (diff orig-content inserted-content)]
      (is (= orig-content on-both) "should contain original headers")
      (is (= expectation on-inserted) "should contain inserted htmx-ws header"))))
