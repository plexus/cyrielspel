(ns pong
  (:require [quil.core :as q]))

(def snelheid 4)

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
                             20
                             20
                             (:palet1 huidige-toestand)
                             20
                             140)
    (assoc huidige-toestand
           :bal-snelheid-x snelheid
           :bal-snelheid-y (- (/ (- (:bal-y huidige-toestand)
                                    (:palet1 huidige-toestand))
                                 45)
                              snelheid))
    ;; Rechtse palet raakt?
    (raakt-cirkel-rechthoek? (:bal-x huidige-toestand)
                             (:bal-y huidige-toestand)
                             20
                             860
                             (:palet2 huidige-toestand)
                             20
                             140)
    (assoc huidige-toestand
           :bal-snelheid-x (- snelheid)
           :bal-snelheid-y (- (/ (- (:bal-y huidige-toestand)
                                    (:palet2 huidige-toestand))
                                 45)
                              snelheid))
    ;; Geen van beide raakt, doe niets
    :else
    huidige-toestand))

(defn bots-kanten
  "Verander de richting van de bal als een van de kanten van het venster geraakt
  wordt."
  [huidige-toestand]
  (cond-> huidige-toestand
    ;; Links of rechts (x-as)
    (or (<= (:bal-x huidige-toestand) 20)
        (<= 880 (:bal-x huidige-toestand)))
    (update :bal-snelheid-x * -1)
    ;; Boven of onder (y-as)
    (or (<= (:bal-y huidige-toestand) 20)
        (<= 380 (:bal-y huidige-toestand)))
    (update :bal-snelheid-y * -1)))

(defn initialiseer []
  (q/smooth)
  (q/no-stroke))

(defn teken-palet [x y]
  (q/fill 255) ; vul-kleur = wit
  (q/rect x y 20 140)) ; teken rechthoek, breedte is 20, hoogte is 140

(defn teken-bal [x y]
  (q/fill 255) ; vul-kleur = wit
  (q/ellipse x y 40 40))

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
  (teken-palet 20 (:palet1 @toestand))
  (teken-palet 860 (:palet2 @toestand))
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
