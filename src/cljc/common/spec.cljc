(ns common.spec
  (:require [clojure.spec.alpha :as s]
            [medley.core]))

(s/def ::id medley.core/uuid?)
(s/def ::m-type #{:client-init :server-init :move :update})
(s/def ::text string?)
(s/def ::x int?)
(s/def ::y int?)
(s/def ::word (s/keys :req-un [::id ::text ::x ::y]))
(s/def ::words (s/and
                (s/map-of ::id ::word)
                (s/every (fn [[k v]] (= (:id v) k)))))


(s/def ::client-init
  (s/keys :req-un [::id
                   ::m-type]))

(s/def ::server-init
  (s/keys :req-un [::id
                   ::m-type
                   ::words]))

(s/def ::move
  (s/keys :req-un [::id  ;; word id
                   ::m-type
                   ::x
                   ::y]))

(s/def ::update
  (s/keys :req-un [::id  ;; word id
                   ::m-type
                   ::x
                   ::y]))
