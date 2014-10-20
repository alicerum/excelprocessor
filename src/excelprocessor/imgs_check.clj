(ns excelprocessor.imgs-check
  (:require [org.httpkit.client :as http-kit]))

(def extensions
  ["gif" "jpg" "jpeg" "png"])

(def base-img-url "http://www.vtt.ru/CatalogPhoto/")

(def non-image-msg "Нет картинки")

(defn get-urls [id]
  (map #(str base-img-url (http-kit/url-encode id) "." %) extensions))

(defn get-responses [urls]
  (map #(http-kit/get %) urls))

(defn get-content-types [responses]
  (map #(:content-type (:headers (deref %))) responses))

(defn is-image? [content-types]
  (map #(.contains % "image") content-types))

(defn send-img-req [id]
  (if (.isEmpty (.trim id))
    ""
    (let [urls (get-urls (.trim id))
          is-image-seq (is-image? (get-content-types (get-responses urls)))]
      (some
        #(if (nil? %) false %)
        (map
          #(if %2 %1 nil)
          urls
          is-image-seq)))))

(defn get-img-src-non-nil [id]
  (try
    (if-let [url (send-img-req id)]
      url
      non-image-msg)
    (catch Exception e
           "Ошибка")))