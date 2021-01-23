(ns animatie
  (:require [quil.core :as q]))

(def snelheid 0.5)

(defn initialiseer []
  (q/no-stroke)
  (q/fill 166 58 43))

;; (q/millis) is het aantal milliseconden sinds de sketch gestart is
;; (mod) is rest bij deling
;;
;; zo krijgen we een getal dat optelt van 0 tot de breedte van de sketch,
;; en dan terug naar 0 gaat
;; (mod (q/millis) (q/width))
;;
;; We vermenigvuldigen (q/millis) ten slotte nog met een snelheids-factor om
;; alles sneller of trager te laten gaan
;; (*) is vermenigvuldiging
;; (mod (* (q/millis) snelheid) (q/width))

(defn teken []
  (q/background 222 216 194)
  (let [x (mod (* (q/millis) snelheid) (q/width))
        y (/ (q/height) 2)]
    (q/ellipse x y 100 100)))

(q/defsketch animatie
  :size [800 800]
  :setup initialiseer
  :draw teken)

#_(alter-var-root #'quil.applet/*applet* (constantly lijnkunst))
