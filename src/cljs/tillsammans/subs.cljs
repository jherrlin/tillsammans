(ns tillsammans.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::words
 (fn [db]
   (:words db)))
