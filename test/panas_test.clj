(ns panas-test
  (:require [clojure.test :refer [deftest is testing]]
            [etaoin.api :as e]))

(deftest a-test
  (testing "test with etaoin"
    (doto (e/chrome)
      (e/go "https://clojure.org/guides/weird_characters")
      (e/wait-visible [{:id :preamble}])
      (as-> it (is (true? (e/has-text? it "NotClojure"))))
      (e/quit))))
