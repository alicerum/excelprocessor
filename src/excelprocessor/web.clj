(ns excelprocessor.web
  (:use [excelprocessor.imgs-check :as images])
  (:require [compojure.core :refer :all]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.resource :as resource]
            [ring.middleware.content-type :as ct]
            [ring.middleware.not-modified :as notmod]
            [ring.middleware.multipart-params :as mp]
            [ring.util.response :as resp]
            [environ.core :refer [env]]
            [clojure.string :as clstr]
            [clojure.data.json :as json]))

(def result (promise))

(defn work-all [ids]
  (let [a (map #(future (images/get-img-src-non-nil %)) ids)]
    (deliver result a)))

(defn start-working [ids]
  (do
    (def result (promise))
    (future (work-all ids))
    (resp/redirect "/working.html")))

(defn wrap-dir-index [handler]
  (fn [req]
    (handler
      (update-in req [:uri]
                 #(if (= "/" %) "/index.html" %)))))

(defn get-count-realized [list]
  (reduce + (map #(if (realized? %) 1 0) list)))

(defroutes app-routes
  (POST "/post/fileData" req
        (start-working
          (clstr/split (get-in req [:params :ids]) #"\r\n")))
  (GET "/get/check" req
       (let [count-realized (get-count-realized @result)
             count-all (count @result)]
         (if (= count-all count-realized)
           (json/write-str {:done true :result (clstr/join "<br>" (map deref @result))})
           (json/write-str {:done false :result (/ count-realized count-all)}))))
  (route/resources "/")
  (route/not-found "404.html"))

(def app (-> (handler/site app-routes)
             (wrap-dir-index)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty app {:port port :join? false})))
