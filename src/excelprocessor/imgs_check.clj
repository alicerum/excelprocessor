(ns excelprocessor.imgs-check
  (:require [org.httpkit.client :as http-kit]))

(def extensions
  ["gif" "jpg" "jpeg" "png"])

(def base-img-url "http://www.vtt.ru/CatalogPhoto/")

(defn get-urls [id]
  (map #(str base-img-url id "." %) extensions))

(defn get-responses [urls]
  (map #(http-kit/get %) urls))

(defn get-content-types [responses]
  (map #(:content-type (:headers (deref %))) responses))

(defn is-image? [content-types]
  (map #(.contains % "image") content-types))

(defn send-img-req [id]
  (if (.isEmpty (.trim id))
    ""
    (let [urls (get-urls id)
          is-image-seq (is-image? (get-content-types (get-responses urls)))]
      (some
        #(if (nil? %) false %)
        (map #(if %2 %1 nil) urls is-image-seq)))))
