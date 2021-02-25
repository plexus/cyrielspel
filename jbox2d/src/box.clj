(ns box
  (:require [lambdaisland.jbox2d :as b]
            [quil.core :as q]))

(let [s 0.1]
  (def world (-> (b/world 0 1)
                 (b/populate [{:id       :ground
                               :position [6 9.8]
                               :fixtures [{:shape [:rect 12 0.4]}]}
                              {:id       :left-wall
                               :position [0.2 5]
                               :fixtures [{:shape [:rect 0.4 10]}]}
                              {:id       :right-wall
                               :position [11.8 5]
                               :fixtures [{:shape [:rect 0.4 10]}]}
                              #_{:id       :box
                                 :type     :dynamic
                                 :bullet?  true
                                 :position [2 2]
                                 :fixtures [{:shape    [:rect s s #_#_[0 0] 1]
                                             :density  0
                                             :friction 3
                                             :restitution 0.8}]}
                              #_{:id :circle
                                 :position [3 3]
                                 :type :dynamic
                                 :fixtures [{:shape [:circle 1]}]}
                              #_{:id       :box2
                                 :type     :dynamic
                                 :bullet?  true
                                 :position [1 1]
                                 :fixtures [{:shape    [:rect s s #_#_[0 0] 1]
                                             :density  0
                                             :friction 3
                                             :restitution 0.8}]}
                              #_{:id       :triangle
                                 :type     :dynamic
                                 :position [3 1]
                                 :fixtures [{:shape  [:polygon
                                                      [-1 -1]
                                                      [-1 1.5]
                                                      [1 1]]
                                             :density  1
                                             :friction 3
                                             :restitution 0.8}]}]
                             #_[{:type   :distance
                                 :bodies [:box :triangle]
                                 :length 2.5
                                 :frequency 2
                                 :damping 0.5}
                                {:type   :revolute
                                 :bodies [:box :box2]
                                 :local-anchors [[-1 -1] [0 0]]
                                 :collide-connected? false
                                 }]))))

(do (b/populate world [{:position [(q/random 1 11) (q/random -2 7)]
                        :type :dynamic
                        :fixtures [{:shape (rand-nth [[:circle (q/random 0.2 0.6)]
                                                      [:rect (q/random 0.4 1.2) (q/random 0.4 1.2)]])
                                    :restitution 0.1
                                    :density 1
                                    :friction 3}]}])
    nil)

(doseq [fixt (b/raycast-seq world [0 9] [12 9])]
  #_  (b/ctl! (b/body fixt) :linear-velocity [0 -2])
  (b/apply-impulse! (b/body fixt) [0.5 -0.1] [0 0.2] true)
  #_(b/apply-angular-impulse! (b/body fixt) 0.5)
  #_(b/apply-torque! (b/body fixt) 1))

(.setLinearVelocity (rand-nth (b/bodies world))
                    (b/vec2 0 -2))

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
  :size [1200 1000]
  :setup initialiseer
  :draw cyclus
  ;; :key-pressed toets-ingedrukt
  :frame-rate 60)

(alter-var-root #'quil.applet/*applet* (constantly box))

(comment
  (b/zoom! b/*camera* -1)
  (b/move-by! b/*camera* [1 0]))

  ;; (doseq [body (b/bodies world)
  ;;         :let [[^double pos-x ^double pos-y] (b/position body)]
  ;;         fixture (b/fixtures body)]
  ;;   (println "-----------------------" pos-x pos-y)
  ;;   (let [vs (b/vertices fixture)]
  ;;     (doseq [[^double x ^double y] vs]
  ;;       (prn x y))
  ;;     (let [[^double x ^double y] (first vs)]
  ;;       (prn x y))))
