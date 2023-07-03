(ns panas.middleware-test
  (:require [clojure.core.match :refer [match]]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [hiccup2.core :refer [html]]
            [hickory.core :refer [parse as-hickory]]
            [hickory.select :as s]
            [hickory.render :refer [hickory-to-html]]
            [panas.default :as default]
            [panas.reload :refer [panas-middleware]]))

(defn router [req]
  (let [paths (some-> (:uri req) (str/split #"/") rest vec)
        verb (:request-method req)]
    (match [verb paths]
      [:get ["content-with-headers-set"]] {:headers {"Content-Type" "text/html; charset=utf-8"}
                                           :body (str (html [:html {:lang "en"}
                                                             [:head [:title "Document"]]
                                                             [:body [:h1 "Hello 🗿"]]]))}
      :else {:status 404 :body "not found"})))

(def panas-router (panas-middleware router {:reloadable? default/reloadable?}))

;; there are two "body", ring-request's body and html's body 
(defn get-html-body-content
  "getting the first element of html body"
  [{:keys [body]}]
  (-> (s/select (s/child (s/tag :body)) (-> (parse body) as-hickory))
      first :content first hickory-to-html))

(deftest content-with-header
  (testing "Testing server that returns content with headers"
    (let [req {:request-method :get :uri "/content-with-headers-set"}
          original (router req)
          panas (panas-router req)]
      (is (= (get-html-body-content original) (get-html-body-content panas))))))

(comment
  (require '[clojure.test :refer [run-tests]])
  (run-tests 'panas.middleware-test))
