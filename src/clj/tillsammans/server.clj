(ns tillsammans.server
  (:require [chord.http-kit :refer [with-channel]]
            [clojure.core.async :as async]
            [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [common.spec :as spec]
            [clojure.spec.alpha :as s]
            [config.core :refer [env]]
            [medley.core :refer [random-uuid dissoc-in]]
            [org.httpkit.server :as hk]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.response :refer [resource-response]]
            [clojure.string :as str])
  (:gen-class))

(def text "i och att det som en på är av för med till den har de inte om ett han men var jag sig från vi så kan man när år säger hon under också efter eller nu sin där vid mot ska skulle kommer ut får finns vara hade alla andra mycket än här då sedan över bara in blir upp även vad få två vill ha många hur mer går sverige kronor detta nya procent skall hans utan sina något svenska allt första fick måste mellan blev bli dag någon några sitt stora varit dem bland kl bra tre ta genom del hela annat fram gör ingen stockholm göra enligt mig redan inom kom du helt ju samma kanske själv oss tidigare se miljoner dock denna både tid kunna fått stor olika ser flera plats kunde gå ur gäller honom aldrig barn varje lite sätt just väl tar åt mest per står fem tror rätt dessa gång därför fyra ny gick hos dessutom ger lika eftersom vilket trots tycker människor ligger vet kvar bättre gjorde ändå inför regeringen senaste samtidigt annan ännu the blivit fall talet exempel gamla deras tiden min hennes sista komma större visar senare tog nästa göteborg ge mindre gjort usa innan alltid pengar sade först stället vår före svensk tillbaka ner nog samt ofta själva inget fler säga egen johansson igen tillsammans runt nästan lopp förra året mål längre svårt bästa andersson anders handlar dagens länge stort peter frågan spelar enda liv fortfarande medan bakom haft minst berättar fast lars personer början problem alltså bort varför anser våra företag mitt dess nytt tio inga fanns egna utanför ville kr långt framför båda behöver par nej miljarder största polisen världen direkt folk borde väg innebär sex klart göran bör vidare menar <3")

(defonce main-chan (async/chan))
(defonce main-mult (async/mult main-chan))
(def app-state (atom {:words (->> (str/split text #"\s")
                                  (map (fn [text]
                                         (let [id (random-uuid)]
                                           {id {:id id
                                                :text text
                                                :x (rand-int 1200)
                                                :y (rand-int 1000)}})))
                                  (into {}))}))


(defn update-word-pos! [id x y]
  (swap! app-state update-in [:words id] #(assoc % :x x :y y)))




(comment
  @app-state

  (update-word-pos! #uuid "b71ebb04-0058-4b7e-b212-5fa90d26c362" 0 0)

  (s/valid? ::spec/word {:id #uuid "2e3e9a53-c587-4257-83d9-daae1a6a9aca",
                         :text "magna", :x 249, :y 389})

  (s/valid? ::spec/words {#uuid "2e3e9a53-c587-4257-83d9-daae1a6a9aca"
                          {:id #uuid "2e3e9a53-c587-4257-83d9-daae1a6a9aca",
                           :text "magna", :x 249, :y 389}})

  (s/valid? ::spec/client-init
            {:id (random-uuid)
             :m-type :client-init})

  (s/valid? ::spec/server-init
            {:id (random-uuid)
             :m-type :server-init
             :words
             {#uuid "2e3e9a53-c587-4257-83d9-daae1a6a9aca"
              {:id #uuid "2e3e9a53-c587-4257-83d9-daae1a6a9aca",
               :text "magna", :x 249, :y 389}}})

  (s/valid? ::spec/move {:id (random-uuid)  ;; word id
                         :m-type :move
                         :x (rand-int 500)
                         :y (rand-int 400)})

  (s/valid? ::spec/update {:id (random-uuid)  ;; word id
                           :m-type :update
                           :x (rand-int 500)
                           :y (rand-int 400)})
  )



(defn ws-handler
  [req]
  (with-channel req ws-ch
    (let [client-tap (async/chan)]
      (async/tap main-mult client-tap)
      (async/go-loop []
        (async/alt!
          client-tap ([message]
                        (if message
                          (do
                            (async/>! ws-ch message)  ;; Send to all clients
                            (recur))
                          (async/close! ws-ch)))
          ws-ch ([{:keys [message]}]
                 (if message
                   (do
                     (clojure.pprint/pprint message)
                     (case (:m-type message)
                       :client-init (async/>! ws-ch (merge {:id (random-uuid)
                                                            :m-type :server-init}
                                                           @app-state))
                       :move (do
                               (update-word-pos! (:id message) (:x message) (:y message))
                               (async/>! main-chan (assoc message :m-type :update))))
                     (recur))
                   (async/untap main-mult client-tap))))))))




(defroutes routes
  (GET "/" [] (resource-response "index.html" {:root "public"}))
  (GET "/ws" [] ws-handler)
  (resources "/"))


(def dev-handler (-> #'routes wrap-reload))


(def handler routes)


(defn -main [& [port]]
  (hk/run-server handler {:port (or (Integer. port) 8080)}))
