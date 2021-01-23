(ns quil-inleiding
  (:require [quil.core :as q]))

(defn teken []
  (Thread/sleep 100))

(q/defsketch mijn-sketch
  :size [800 800]
  :draw teken
  :features [:keep-on-top])

(alter-var-root #'quil.applet/*applet* (constantly mijn-sketch))

;; Met (q/background ...) kan je de volledige achtergrond een kleur geven.
;; Er zijn twee versies:

;; Zwart - grijs - wit
;; Getallen zijn altijd tussen 0 en 255
(q/background 0) ; zwart
(q/background 100) ; grijs
(q/background 255) ; wit

;; Kleur, drie getallen voor rood, groen, blauw
(q/background 255 0 0) ; 100% rood
(q/background 0 255 0) ; 100% groen
(q/background 0 0 255) ; 100% blauw

(q/background 255 255 0) ; rood + groen = geel
(q/background 0 255 255) ; groen + blauw = lichtblauw
(q/background 255 0 255) ; rood + blauw = roos-paars-achtig (?)

(q/background 58 96 41) ; donker groen
(q/background 246 206 31) ; hel geel
(q/background 209 141 44) ; oker
(q/background 166 58 43) ; rode aarde
(q/background 110 171 183) ; babyblauw
(q/background 132 79 33) ; bruin
(q/background 161 165 134) ; beige
(q/background 222 216 194) ; beige
(q/background 239 239 230) ; wit-achtig

;; Kleur kiezen : https://www.google.com/search?q=color+picker

;; Lijnen tekenen
(q/line 338 42 628 329)

;; Kies de kleur voor de lijn
(q/stroke 166 58 43)

;; Kies de dikte van de lijn
(q/stroke-weight 5)

;; Teken de lijn, (q/line x1 y1 x2 y2)
(q/line 100 100 400 400)
(q/line 100 400 400 100)

;; Breedte van de tekening
(q/width)
;; => 800

;; Hoogte van de tekening
(q/height)
;; => 800

;; Willekeurig getal tussen 0 en 100
(q/random 100)
;; => 14.309019

;; Willekeurig getal tussen 0 en de breedte van de tekening
(q/random (q/width))
;; => 175.64554

;; Willekeurige lijn
(q/line (q/random (q/width)) (q/random (q/height))
        (q/random (q/width)) (q/random (q/height)))

(defn willekeurige-lijn []
  (q/line (q/random (q/width)) (q/random (q/height))
          (q/random (q/width)) (q/random (q/height))))

(willekeurige-lijn)

;; Willekeurige lijn-kleur
(q/stroke (q/random 255) (q/random 255) (q/random 255))

;; Rechthoek : x y breedte hoogte
;; x y = linkerbovenhoek
(q/rect 100 100 300 200)
(q/rect 400 300 200 300)

;; invul-kleur
(q/fill 209 141 44)

;; Ellips: x y breedte hoogte
;; x y = middelpunt
(q/ellipse 100 100 300 200)
(q/ellipse 250 200 300 200)

;; Zonder lijn, alleen invulling
(q/no-stroke)

(defn teken []
  (willekeurige-lijn))

(q/frame-rate 3)
