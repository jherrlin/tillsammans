(ns tillsammans.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [tillsammans.subs :as subs]
   [tillsammans.events :as events]
   [goog.object]
   ))

(re-frame/dispatch [::events/init-app])



(defn word [{:keys [id x y text] :as word}]
  (let [state (reagent/atom false)]
    (fn [{:keys [id x y text] :as word}]
      [:div {:id id
             :on-drag-start (fn [event]
                              (reset! state true))
             :on-drag-end (fn [event]
                            (do
                              (re-frame/dispatch [::events/move id (- (.-clientX event) 10) (- (.-clientY event) 10)])
                              (reset! state false)))
             :draggable true
             :style {:position "absolute"
                     :display (when @state "none")
                     :box-shadow "2px 2px 2px grey"
                     :left x
                     :top y}}
       text])))


(defn main-panel []
  (let [words @(re-frame/subscribe [::subs/words])]
    (->> words
         vals
         (mapv (fn [w]
                 [word w]))
         (into [:div]))))




(comment
  (->> @re-frame.db/app-db
       :words)
  )
