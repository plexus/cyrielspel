(ns pong
  (:require [quil.core :as q]))

(def snelheid 4)

(def palet-dikte 20)
(def palet-hoogte 140)
(def bal-doorsnede 40)

;; De toestand van het spel, we beginnen met de twee paletten en de bal allemaal
;; in het midden.
(def toestand (atom {;; Positie van de linkse palet langs de y-as, bovenzijde
                     :palet1 130
                     ;; Positie van de rechtse palet langs de y-as, bovenzijde
                     :palet2 130
                     ;; Positie van de bal (middelpunt van de cirkel)
                     :bal-x 450
                     :bal-y 200
                     ;; De richting waarin de bal beweegt, langs de x en de y as
                     :bal-snelheid-x snelheid
                     :bal-snelheid-y 0}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Wiskunde

(defn afstand
  "De afstand tussen twee punten (Pythagoras)"
  [x1 y1 x2 y2]
  (Math/sqrt (+ (* (- x1 x2) (- x1 x2))
                (* (- y1 y2) (- y1 y2)))))

(defn raakt-cirkel-rechthoek?
  "Raken of overlappen de cirkel en de rechthoek elkaar

  cx, cy - middelpunt van de cirkel
  straal - straal van de cirkel
  rx, ry - de linkerbovenhoek van de rechthoek
  breedte, hoogte - de afmetingen van de rechthoek"
  [cx cy straal rx ry breedte hoogte]
  (< (afstand
      (cond
        (< cx rx) rx
        (> cx (+ rx breedte)) (+ rx breedte)
        :else cx)
      (cond
        (< cy ry) ry
        (> cy (+ ry hoogte)) (+ ry hoogte)
        :else cy)
      cx
      cy)
     straal))

(defn beweeg-bal
  "Verander de positie van de bal"
  [huidige-toestand]
  (-> huidige-toestand
      (update :bal-x + (:bal-snelheid-x huidige-toestand))
      (update :bal-y + (:bal-snelheid-y huidige-toestand))))

(defn palet-bal-interactie
  "Controleer of de bal een van de paletten raakt, en indien dat het geval is,
  verander dan de richting van de bal (bots)."
  [huidige-toestand]
  (cond
    ;; Linkse palet raakt?
    (raakt-cirkel-rechthoek? (:bal-x huidige-toestand)
                             (:bal-y huidige-toestand)
                             palet-dikte
                             palet-dikte
                             (:palet1 huidige-toestand)
                             palet-dikte
                             palet-hoogte)
    (assoc huidige-toestand
           :bal-snelheid-x snelheid
           :bal-snelheid-y (* snelheid (- (/ (- (:bal-y huidige-toestand)
                                                (:palet1 huidige-toestand))
                                             palet-hoogte)
                                          0.5)))
    ;; Rechtse palet raakt?
    (raakt-cirkel-rechthoek? (:bal-x huidige-toestand)
                             (:bal-y huidige-toestand)
                             palet-dikte
                             (- (q/width) (* palet-dikte 2))
                             (:palet2 huidige-toestand)
                             palet-dikte
                             palet-hoogte)
    (assoc huidige-toestand
           :bal-snelheid-x (- snelheid)
           :bal-snelheid-y (* snelheid (- (/ (- (:bal-y huidige-toestand)
                                                (:palet2 huidige-toestand))
                                             palet-hoogte)
                                          0.5)))
    ;; Geen van beide raakt, doe niets
    :else
    huidige-toestand))

(defn bots-kanten
  "Verander de richting van de bal als een van de kanten van het venster geraakt
  wordt."
  [huidige-toestand]
  (let [bal-straal (/ bal-doorsnede 2)]
    (cond-> huidige-toestand
      ;; Links of rechts (x-as)
      (or (<= (:bal-x huidige-toestand) bal-straal)
          (<= (- (q/width) bal-straal) (:bal-x huidige-toestand)))
      (update :bal-snelheid-x * -1)
      ;; Boven of onder (y-as)
      (or (<= (:bal-y huidige-toestand) bal-straal)
          (<= (- (q/height) bal-straal) (:bal-y huidige-toestand)))
      (update :bal-snelheid-y * -1))))

(defn initialiseer []
  (q/smooth)
  (q/no-stroke))

(defn teken-palet [x y]
  (q/fill 255) ; vul-kleur = wit
  (q/rect x y palet-dikte palet-hoogte)) ; teken rechthoek

(defn teken-bal [x y]
  (q/fill 255) ; vul-kleur = wit
  (q/ellipse x y bal-doorsnede bal-doorsnede))

(defn volgende-toestand
  "Pas de toestand aan, beweeg eerst de bal, controleer dan of een van de paletten
  geraakt is, en of een van de kanten geraakt is."
  [huidige-toestand]
  (-> huidige-toestand
      beweeg-bal
      palet-bal-interactie
      bots-kanten))

(defn cyclus
  "Dit wordt 90 keer per seconde uitgevoerd, pas eerst de toestand aan, en teken
  dan alles."
  []
  (swap! toestand volgende-toestand)
  (q/background 32)
  (q/fill 255)
  (teken-palet palet-dikte (:palet1 @toestand))
  (teken-palet (- (q/width) (* palet-dikte 2)) (:palet2 @toestand))
  (teken-bal (:bal-x @toestand) (:bal-y @toestand)))

(defn toets-ingedrukt
  "Beweeg de paletten met z/s en boven/onder toetsen."
  []
  (let [toets (q/key-as-keyword)]
    (cond
      (= :z toets) (swap! toestand update :palet1 - 5)
      (= :s toets) (swap! toestand update :palet1 + 5)
      (= :up toets) (swap! toestand update :palet2 - 5)
      (= :down toets) (swap! toestand update :palet2 + 5))))

(q/defsketch pong
  :title "Pong"
  :size [900 400]
  :setup initialiseer
  :draw cyclus
  :key-pressed toets-ingedrukt
  :frame-rate 60)
