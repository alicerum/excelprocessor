(ns excelprocessor.imgs-check
  (:require [org.httpkit.client :as http-kit]
            [clojure.core.async :as async]))

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
    (let [urls (get-urls id)
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
  (println url)
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

(defn read-urls-from-chan [count response-chan]
  (loop [counter 0
         limit count
         set #{}]
    (if (= counter limit)
      (conj set non-image-msg)
      (recur
        (+ counter 1)
        limit
        (conj set (async/<!! response-chan))))))

(defn get-url-responses [url-list]
  (let [nonempty-list (filter #(not (.isEmpty (.trim %))) url-list)
        resp-chan (async/chan)]
    (doseq [url nonempty-list]
      (async-get url resp-chan))
    (let [right-url-set (read-urls-from-chan (count nonempty-list) resp-chan)
          parted-initial-list (partition-all (count extensions) url-list)]
      (map #(transform-urls-from-set % right-url-set) parted-initial-list))))
