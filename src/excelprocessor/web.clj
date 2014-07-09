(ns excelprocessor.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.resource :as resource]
            [ring.middleware.content-type :as ct]
            [ring.middleware.not-modified :as notmod]
            [environ.core :refer [env]]))


(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Hello World!"})

(def app
  (-> handler
      (resource/wrap-resource "public")
      (ct/wrap-content-type)
      (notmod/wrap-not-modified)))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty app {:port port :join? false})))
