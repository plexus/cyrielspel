(ns spel.spel
  (:require [quil.core :as q]
            [lambdaisland.quil-extras :as qe]))

(def moersleutel (qe/scale-up-pixels (qe/load-image "resources/moersleutel.png") 9))

(defn setup []
  (q/background 100))

(defn draw []
  (q/background 100)
  (q/image moersleutel 100 100))

(q/defsketch spel
             :size [500 500]
             :setup setup
             :draw draw
             :features [:keep-on-top])