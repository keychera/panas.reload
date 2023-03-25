(ns panas-test
  (:require [clojure.test :refer [is testing]]))

(testing "some arithmetic"
  (is (= 1 (+ 1 0)))
  (is (= 1 (+ 2 0))))