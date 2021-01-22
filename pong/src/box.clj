(ns box
  (:require [lambdaisland.jbox2d :as b]
            [quil.core :as q]))

(def world (b/world 0 -10))

(let [ground-body  (b/create-body world {:position (b/vec2 0 -10)})
      box-body     (b/create-body world {:type     :dynamic
                                         :position (b/vec2 0 4)})]
  (b/fixture ground-body {:shape (b/rectangle 50 10)})
  (b/fixture box-body {:shape (b/rectangle 1 1)
                       :density 1
                       :friction 0.3}))

(b/body-seq world)    

body
(b/step-world world)
    

(defn initialiseer [])

(defn cyclus []
  (doseq [body (map b/value (body-seq world))]
    (q/rect)))

(q/defsketch box
  :title "Pong"
  :size [900 400]
  :setup initialiseer
  :draw cyclus
  ;; :key-pressed toets-ingedrukt
  :frame-rate 60)

