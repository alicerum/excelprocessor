(ns excelprocessor.imgs-check
  (:require [org.httpkit.client :as http-kit]
            [clj-time.core :as ctime]))

(def extensions
  ["gif" "jpg" "jpeg" "png"])

(def base-img-url "http://www.vtt.ru/CatalogPhoto/")

(def non-image-msg "Нет картинки")

(defn get-urls [id]
  (map #(str base-img-url id "." %) extensions))

(defn http-get-retry [url]
  (loop [i 0
         time nil]
    (if (and (not (nil? time))
             (ctime/after? (ctime/plus time (ctime/minutes 1)) (ctime/now)))
      (recur 0 time)
      (if (not (nil? time))
        (recur 0 nil)
        (if (> i 0)
          (recur 0 (ctime/now))
          (let [{:keys [headers error]} @(http-kit/get url)]
            (if error
              (do
                (println error)
                (recur (+ i 1) nil))
              headers)))))))

(defn get-responses [urls]
  (map #(http-get-retry %) urls))

(defn get-content-types [responses]
  (map #(:content-type %) responses))

(defn is-image? [content-types]
  (map #(.contains % "image") content-types))

(defn send-img-req [id]
  (if (.isEmpty (.trim id))
    ""
    (try
      (let [urls (get-urls id)
            is-image-seq (is-image? (get-content-types (get-responses urls)))]
        (some
          #(if (nil? %) false %)
          (map
            #(if %2 %1 nil)
            urls
            is-image-seq)))
      (catch Exception e
             "Ошибка"))))

(defn get-img-src-non-nil [id]
  (if-let [url (send-img-req id)]
    url
    non-image-msg))