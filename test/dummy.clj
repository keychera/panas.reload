(ns dummy
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]))

(defn router [custom]
  (fn [{:keys [uri]}]
    (println uri)
    (match [uri]
      ["/"] {:body (io/file (io/resource "index.html"))}
      ["/hello"] {:body (str "Hello " custom)}
      :else {:status 404 :body "not found!"})))