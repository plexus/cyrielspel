(ns lijnkunst
  (:require [quil.core :as q]))

;; https://www.google.com/search?q=jackson+pollock

(defn initialiseer []
  ;; begin met witte achtergrond
  (q/background 255)
  ;; teken vijf keer per seconde
  (q/frame-rate 5))

(defn teken []
  ;; willekeurige kleur
  (q/stroke (q/random 255) (q/random 255) (q/random 255))
  ;; willekeurige dikte
  (q/stroke-weight (q/random 10))
  ;; willekeurige lijn
  (q/line (q/random (q/width)) (q/random (q/height))
          (q/random (q/width)) (q/random (q/height))))

(q/defsketch lijnkunst
  :title "Lijnkunst"
  :size [400 400]
  :setup initialiseer
  :draw teken
  :features [:keep-on-top])

#_(alter-var-root #'quil.applet/*applet* (constantly lijnkunst))
