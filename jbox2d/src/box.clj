(ns box
  (:require [lambdaisland.jbox2d :as b]
            [quil.core :as q]))

(def world (-> (b/world 0 10)
               (b/populate [{:id       :ground
                             :position [3 9]
                             :fixtures [{:shape [:rect 8 0.4]}]}
                            {:id       :box
                             :type     :dynamic
                             :bullet?  true
                             :position [0 0]
                             :fixtures [{:shape    [:rect 2 1 #_#_[0 0] 1]
                                         :density  0
                                         :friction 3}]}
                            {:id       :circle
                             :type     :dynamic
                             :position [3 1]
                             :fixtures [{:shape  [:polygon
                                                  [-1 -1]
                                                  [-1 0.5]
                                                  [1 1]]
                                         :density  1
                                         :friction 3}]}]
                           #_[{:type   :constant-volume
                               :bodies [:box :box2 :ground]}])))

(defn initialiseer []
  (q/stroke-weight 5))

(defn cyclus []
  (b/step-world world)
  (q/background 161 165 134) ; beige
  (b/draw! world)
  #_(doseq [[idx body] (map vector (range) (b/bodies world))
            :let [[^double pos-x ^double pos-y] (b/position body)]
            fixture (b/fixtures body)]
      (q/begin-shape)
      (let [vs (b/screen-vertices vp fixture)]
        (doseq [[^double x ^double y] vs]
          (q/vertex x y))
        (let [[^double x ^double y] (first vs)]
          (q/vertex x y)))
      (q/end-shape)))

(q/defsketch box
  :size [1200 1200]
  :setup initialiseer
  :draw cyclus
  ;; :key-pressed toets-ingedrukt
  :frame-rate 60)

(b/zoom! b/*camera* -1)
(b/move-by! b/*camera* [-1 0])

;; (doseq [body (b/bodies world)
;;         :let [[^double pos-x ^double pos-y] (b/position body)]
;;         fixture (b/fixtures body)]
;;   (println "-----------------------" pos-x pos-y)
;;   (let [vs (b/vertices fixture)]
;;     (doseq [[^double x ^double y] vs]
;;       (prn x y))
;;     (let [[^double x ^double y] (first vs)]
;;       (prn x y))))
