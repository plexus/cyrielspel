(ns animatie
  (:require [quil.core :as q]))

(def snelheid 1)

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
  (let [x (mod (* (q/millis) snelheid)  (+ (q/width) 100))
        y (/ (q/height) 2)]
    (q/ellipse x y 100 100)))

(+ (q/width) 100)

(map (fn [n]
       (mod n 3))
     [0 1 2 3 4 5 6 7 8 9 10])

(defn rest-3 [n]
  (mod n 3))

(/ 93898 32)
(rest-3 30)

(mod (q/millis) (q/width))

(map rest-3 [0 1 2 3 4 5 6 7 8 9 10])

(mod (q/millis) 900)


(q/defsketch animatie
  :title "Animatie"
  :size [800 400]
  :setup initialiseer
  :draw teken
  :features [:keep-on-top])

(alter-var-root #'quil.applet/*applet* (constantly animatie))
