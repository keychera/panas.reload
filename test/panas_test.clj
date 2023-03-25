(ns panas-test
  (:require [etaoin.api :as e]
            [clojure.test :refer [testing]]))

(testing "test with etaoin"
  (doto (e/chrome)
    (e/go "https://clojure.org/guides/weird_characters")
    (e/wait-visible [{:id :preamble}])
    (e/has-text? "NotClojure")
    (e/quit)))
