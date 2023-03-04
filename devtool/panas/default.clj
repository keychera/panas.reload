(ns panas.default
  (:require [clojure.string :as str]))

;; still naive for now
(defn reloadable? [{:keys [request-method uri] :as req}]
  (and (= request-method :get)
       (not (:websocket? req)) ;; this is http-kit specific
       (not (str/starts-with? uri "/css"))
       (not (str/includes? uri ".css"))
       (not (str/starts-with? uri "/favicon.ico"))
       (not (str/starts-with? uri "/static"))))