(ns excelprocessor.concur
  (:require [excelprocessor.imgs-check :as images]))

;; Will work with 5 threads for now
(def n 5)

(def processed (atom 0))
(def all (atom 1))

(defn get-percentage []
  (/ @processed @all))

(defn realize-futures [urls]
  (let [processed-urls (doall (map deref urls))]
    (swap! processed #(+ % (count urls)))
    processed-urls))

(defn run-futures [ids]
  (realize-futures
    (map #(future (images/get-img-src-non-nil %)) ids)))

(defn run-all-in-parts [ids]
  (let [parted (partition n n nil ids)]
    (reset! all (count ids))
    (reset! processed 0)
    (flatten (doall (map run-futures parted)))))
