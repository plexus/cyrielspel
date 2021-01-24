(ns muis
  (:require [quil.core :as q]))

(defn initialiseer []
  (q/no-stroke)
  (q/fill 166 58 43))

(defn teken []
  (q/background 222 216 194)
  (let [x (q/mouse-x)
        y (q/mouse-y)]
    (q/ellipse x y 100 100)))

(q/defsketch muis
  :title "Muis en bal"
  :size [400 400]
  :setup initialiseer
  :draw teken
  :features [:keep-on-top])

#_(alter-var-root #'quil.applet/*applet* (constantly lijnkunst))
