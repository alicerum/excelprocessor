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
            [environ.core :refer [env]]
            [clojure.string :as clstr]))

(defn render-uploaded-data [ids]
  (clojure.string/join
    "\n"
    (pmap images/get-img-src-non-nil ids)))

(defroutes app-routes
  (GET "/" [] "Hello world")
  (POST "/post/fileData.html" req
        (render-uploaded-data
          (clstr/split (get-in req [:params :ids]) #"\n")))
  (route/resources "/")
  (route/not-found "404.html"))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello World!"})

(def app
  (handler/site app-routes))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty app {:port port :join? false})))
