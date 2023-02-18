(ns serve
  (:require [clojure.core.match :refer [match]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [org.httpkit.server :refer [run-server]]
            [selmer.parser :refer [render-file]]))

(defonce port 4242)
(defonce url (str "http://localhost:" port "/"))

(defn index [& _]
  (render-file "index.html" {}))

(defn your-router [req]
  (let [paths (some-> (:uri req) (str/split #"/") rest vec)
        verb (:request-method req)]
    (match [verb paths]
      [:get []] {:body (index)}
      [:get ["css" "style.css"]] {:body (slurp (io/resource "style.css"))}
      :else {:status 404 :body "not found"})))


(defn -main [& _]
  (run-server #'your-router {:port port :thread 12})
  (println "[panas] serving" url)
  @(promise))
