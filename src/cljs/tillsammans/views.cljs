(ns tillsammans.views
  (:require
   [re-frame.core :as re-frame]
   [tillsammans.subs :as subs]
   [tillsammans.events :as events]
   [goog.object]
   ))

(re-frame/dispatch [::events/init-app])



(defn main-panel []
  (let [words @(re-frame/subscribe [::subs/words])]
    (->> words
         vals
         (mapv (fn [{:keys [id x y text] :as word}]
                 [:div {:id id
                        :on-drag (fn [event]
                                   (js/console.log "john-debug on-drag:" [(.-clientX event) (.-clientY event)]))
                        :on-drag-end (fn [event]
                                       (re-frame/dispatch [::events/move id (.-clientX event) (.-clientY event)]))

                      :draggable true
                      :style {:position "absolute"
                              :left x
                              :top y}}
                  text]))
         (into [:div]))))



(comment
  (->> @re-frame.db/app-db
       :words)
  )
