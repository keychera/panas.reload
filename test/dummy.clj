(ns dummy 
  (:require [clojure.java.io :as io]))

(defn router [_] {:body (io/file (io/resource "index.html"))})