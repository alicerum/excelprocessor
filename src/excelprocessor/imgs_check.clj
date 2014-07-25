(ns excelprocessor.imgs-check
  (:require [org.httpkit.client :as http-kit]
            [clojure.core.async :as async]))

(def extensions
  ["gif" "jpg" "jpeg" "png"])

(def base-img-url "http://www.vtt.ru/CatalogPhoto/")

(def non-image-msg "Нет картинки")

(def percentage (atom 0))

(defn prepare-url [id]
  (if (.isEmpty (.trim id))
    (repeat
      (count extensions)
      "")
    (map #(str base-img-url (http-kit/url-encode id) "." %)
         extensions)))

(defn prepare-urls [ids]
  (flatten (map prepare-url ids)))

(defn right-content-type? [resp]
  (if-let [error (:error resp)]
    (do (println error)
        nil))
  (let [content-type (:content-type (:headers resp))]
    (.contains content-type "image")))

(defn async-get [url response-chan]
  (http-kit/get url #(if-let [error (:error %)]
                      (async-get url response-chan) ; Doh. Fuck you, will just spam requests until you process them.
                      (async/go (async/>! response-chan
                                          (if (right-content-type? %)
                                            url
                                            non-image-msg))))))

(defn transform-urls-from-set [part set]
  (let [urls-in-set (map #(set %) part)]
    (if-let [result (some identity urls-in-set)]
      result
      non-image-msg)))

(def all-count (atom 1))
(def processed-count (atom 0))

(defn reset-counters [all]
  (reset! all-count
          (if (> all 0) all 1)) ; Don't want to get division by zero here
  (reset! processed-count 0))

(defn read-urls-from-chan [count response-chan]
  (loop [counter 0
         limit count
         set #{}]
    (if (= counter limit)
      (conj set non-image-msg)
      (recur
        (+ counter 1)
        limit
        (conj set
              (let [processed-url (async/<!! response-chan)] ; Need to get the url from channel, only then we can
                (swap! processed-count inc)                  ; inc the counter
                processed-url))))))

(defn get-url-responses [url-list]
  (let [nonempty-list (filter #(not (.isEmpty (.trim %))) url-list)
        resp-chan (async/chan)]
    (reset-counters (count url-list))
    (doseq [url nonempty-list]
      (async-get url resp-chan))
    (let [right-url-set (read-urls-from-chan (count nonempty-list) resp-chan)
          parted-initial-list (partition-all (count extensions) url-list)]
      (map #(transform-urls-from-set % right-url-set) parted-initial-list))))
