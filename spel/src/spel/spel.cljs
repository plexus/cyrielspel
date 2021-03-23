(ns spel.spel
  (:require ["hammerjs" :as Hammer]
            ["pixi.js" :as pixi]
            ["@pixi/filter-pixelate" :as pixelate]
            ["@pixi/filter-crt" :as crt]
            ["pixi-plugin-bump/src/Bump" :as Bump]
            [applied-science.js-interop :as j]
            [clojure.string :as str]
            [lambdaisland.puck :as p]
            [kitchen-async.promise :as promise]
            [lambdaisland.puck.math :as m]
            [lambdaisland.puck.daedalus :as puck-daedalus]
            [lambdaisland.daedalus :as daedalus]
            [lambdaisland.glogi.console :as glogi-console]
            [lambdaisland.glogi :as log]
            [spel.svg :as svg]))

(defonce state (atom {:scene nil
                      :time 0
                      :scenes {:sensei {}
                               :space-invaders {:virus-vx 1}}
                      :sprites {}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(glogi-console/install!)
(log/set-levels {:glogi/root :all})

(def images {:jacko             "images/jacko.png"
             :cliffs            "images/magic-cliffs-preview-detail.png"
             :fabriek-binnen    "images/fabriek_binnen.jpg"
             :fabriek-buiten    "images/fabriek_buiten.jpg"
             :hut               "images/hut.jpg"
             :oude-fabriek      "images/oude_fabriek.jpg"
             :supermarkt        "images/supermarkt.jpg"
             :werkplaats        "images/werkplaats.jpg"
             :bulkhead-back     "images/bulkhead-walls-back.png"
             :bulkhead-pipes    "images/bulkhead-walls-pipes.png"
             :bulkhead-platform "images/bulkhead-walls-platform.png"})

(def world-height 1000)
(def achtergrond-kleur 0xFFFFFF)

(defonce app (p/full-screen-app {:background-color "white"}))
(defonce stage (:stage app))
(defonce renderer (:renderer app))

(defonce graphics (p/graphics))

(defonce bg-layer (p/container {}))
(defonce sprite-layer (p/container {}))
(defonce viewport (p/container {} bg-layer sprite-layer))
(defonce world (p/container {} viewport))

(defonce fill-layer (p/container {} graphics))

(defonce add-layers-once (conj! stage fill-layer world))

;; stage
;; |- fill
;; |- world
;;    |- viewport
;;      |- bg-layer
;;      |- sprites
;;         |- virussen -> virus 1 / virus 2
;;         |- ruimteschip

(j/assoc! stage :filters
          #js [#_(doto (pixi/filters.ColorMatrixFilter.) (.polaroid))
               #_(pixelate/PixelateFilter. 5)
               #_(crt/CRTFilter. #js {:lineWidth 0.2
                                      :vignetting 0})])

(defmulti load-scene :scene)
(defmulti start-scene :scene)
(defmulti tick-scene :scene)
(defmulti stop-scene :scene)
(defmulti handle-event (fn [s e] (:scene s)))

(defmethod load-scene :default [_])
(defmethod start-scene :default [_])
(defmethod tick-scene :default [_])
(defmethod stop-scene :default [_]
  (p/remove-children bg-layer)
  (p/remove-children sprite-layer))
(defmethod handle-event :default [_ _])

(defn scene-state
  ([]
   (scene-state (:scene @state)))
  ([scene]
   (scene-state @state scene))
  ([state scene]
   (assoc (get-in state [:scenes scene]) :scene scene)))

(defn scene-swap! [f & args]
  (swap! state (fn [s]
                 (apply update-in s [:scenes (:scene s)] f args))))


(add-watch state ::switch-scene
           (fn [_ _ old new]
             (when (not= (:scene old) (:scene new))
               (let [scene-path [:scenes (:scene new)]
                     new-scene (scene-state new (:scene new))]
                 (log/info :switching-scene {:old (:scene old) :new (:scene new)} )
                 (promise/do
                   (stop-scene old)
                   (if-not (:loaded? new)
                     (do
                       (log/debug :loading-scene new)
                       (promise/let [new-state (load-scene new-scene)]
                         (swap! state update-in scene-path
                                (fn [scene]
                                  (cond-> scene
                                    (map? new-state)
                                    (merge new-state)
                                    :->
                                    (assoc :scene (:scene new)
                                           :loaded? true))))
                         (start-scene (get-in @state scene-path))))
                     (start-scene new-scene)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn screen-size []
  (get-in app [:renderer :screen]))

(defn screen-to-world-ratio []
  (let [{:keys [width height]} (screen-size)]
    (/ height world-height)))

(defn visible-world-width []
  (/ (:width (screen-size)) (screen-to-world-ratio)))

(defn resize-pixi []
  (let [{:keys [width height]} (screen-size)]
    (let [ratio (screen-to-world-ratio)]
      (p/merge! world {:x (/ width 2)
                       :scale {:x ratio
                               :y ratio}}))
    (.beginFill graphics achtergrond-kleur)
    (.drawRect graphics 0 0 width height)
    (.endFill graphics)))

(defn center-viewport [x])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-sprite!
  ([name texture]
   (log/trace :making-sprite {:name name :texture texture})
   (let [sprite (p/sprite texture)]
     (swap! state assoc-in [:sprites name] sprite)
     sprite)))

(defn sprite [name]
  (get-in @state [:sprites name]))

(defn init! []
  (resize-pixi)
  (p/listen! app :resize resize-pixi)
  (p/pixelate!)

  (promise/do
    (p/listen! (:ticker app)
               :tick
               (fn [delta]
                 (try
                   (swap! state update :time + delta)
                   (let [{:keys [loaded?] :as state} (scene-state)]
                     (when loaded? (tick-scene (assoc state :delta delta))))
                   (catch :default e
                     (prn "game-loop error" e)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw-background [sprite]
  (.removeChildren bg-layer)
  (let [{:keys [width height]} (screen-size)
        texture-height (:height (:texture sprite))]
    (p/merge! sprite {:anchor {:x 0.5 :y 0}
                      :scale {:x (/ world-height texture-height)
                              :y (/ world-height texture-height)}})
    #_(assoc! stage :x (- (/ width (screen-to-world-ratio) 2)
                          (/ (:width sprite) 2)))
    (conj! bg-layer sprite)))

(defn walk-step [sprite delta speed]
  (let [{:keys [destination position]} sprite]
    (when destination
      (when (< 5 (m/distance position destination))
        (let [diff (m/v- destination position)
              dist (m/vdiv diff (m/length diff))
              step (m/v* dist (* delta speed))]
          (m/!v+ position step))))))

(defn pad-hit-area! [target pad-x pad-y]
  (let [{:keys [x y width height]} (p/local-bounds target)]
    (j/assoc! target :hitArea (p/rectangle (- x (/ pad-x 2))
                                           (- y (/ pad-y 2))
                                           (+ width pad-x)
                                           (+ height pad-y)))))

(defn key-event->keyword [e]
  (let [k (.-key e)]
    (case k
      " " :Space
      (keyword k))))

(defn move-sprite [sprite delta]
  (j/update! sprite :x + (* (j/get sprite :vx 0) delta))
  (j/update! sprite :y + (* (j/get sprite :vy 0) delta)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod load-scene :sensei [_]
  (promise/let [{:keys [sprites cliffs]}
                (p/load-resources! app
                                   {:sprites "images/sprites.json"
                                    :cliffs "images/magic-cliffs-preview-detail.png"})]
    (make-sprite! :sensei (j/get (:textures sprites) "sensei.png"))
    (make-sprite! :cliffs cliffs)))

(defmethod start-scene :sensei [{}]
  (let [speler (sprite :sensei)
        achtergrond (sprite :cliffs)]
    (p/merge! speler {:x 500
                      :y 500
                      :anchor {:x 0.5 :y 0.5}
                      :scale {:x 0.1 :y 0.1}})
    (conj! sprite-layer speler)
    (draw-background achtergrond)))

(defmethod tick-scene :sensei [{:keys [delta]}]
  (walk-step (sprite :sensei) delta 2))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Minispel

(defmethod load-scene :space-invaders [_]
  (promise/let [{:keys [minispel]}
                (p/load-resources! app
                                   {:minispel "images/minispel/minispel.json"})]
    (doseq [k [:ruimteschip :loding :batterij :kogel
               :pijl-links :pijl-rechts :pijl-schiet]]
      (doto (make-sprite! k (j/get (:textures minispel) (str (name k) ".png")))
        (p/merge! {:anchor {:x 0.5 :y 0.5}})))
    (p/merge! (sprite :ruimteschip) {:scale {:x 0.5 :y 0.5}})
    (doseq [x (range 8)
            y (range 3)]
      (let [sprite (make-sprite! [:virus x y] (j/get (:textures minispel) "virus.png"))
            scale (+ 0.7 (* 0.2 (rand)))]
        (p/merge! sprite {:anchor {:x 0.5 :y 0.5} :scale {:x scale :y scale}})))))

(defn schiet-kogel! []
  (let [schip (sprite :ruimteschip)
        kogel (sprite :kogel)]
    (when (or (not (contains? sprite-layer kogel)) (<= (:y kogel) 0))
      (p/merge! kogel (doto {:x (:x schip)
                             :y (- (:y schip) 110)
                             :vy -10
                             :visible true} prn))
      (conj! sprite-layer kogel))))

(defmethod start-scene :space-invaders [_]
  (let [schip (sprite :ruimteschip)
        links (sprite :pijl-links)
        rechts (sprite :pijl-rechts)
        schiet (sprite :pijl-schiet)
        kogel (sprite :kogel)
        virussen (p/container {})
        ga-links! #(j/assoc! schip :vx -10)
        ga-rechts! #(j/assoc! schip :vx 10)]

    (doseq [sprite [links rechts schiet]]
      (j/assoc! sprite :interactive true)
      (pad-hit-area! sprite 50 50))

    (doseq [e [:touchstart :mousedown]]
      (p/listen! links e ga-links!)
      (p/listen! rechts e ga-rechts!)
      (p/listen! schiet e schiet-kogel!))

    (p/listen! js/window :keydown #(scene-swap! update :keys (fnil conj #{}) (key-event->keyword %)))
    (p/listen! js/window :keyup #(scene-swap! update :keys disj (keyword (key-event->keyword %))))
    (p/listen! js/window :blur #(scene-swap! update :keys empty))

    (doseq [e [:touchend :mouseup]]
      (p/listen! links e #(j/assoc! schip :vx 0))
      (p/listen! rechts e #(j/assoc! schip :vx 0)))

    (p/merge! virussen {:x 0 :vx 2})

    (scene-swap! assoc :virussen virussen)

    (doseq [x (range 8)
            y (range 3)
            :let [sprite (sprite [:virus x y])]]
      (p/merge! sprite {:x (+ -400 (* 100 x))
                        :y (+ 100 (* 100 y))})
      (conj! virussen sprite))

    (conj! sprite-layer virussen schip links rechts schiet)

    (p/merge! schip {:x 0 :y 850})

    (p/merge! links {:x -650 :y 930 :interactive true})
    (p/merge! rechts {:x -500 :y 930 :interactive true})
    (p/merge! schiet {:x 550 :y 900 :interactive true})
    nil))

(defmethod tick-scene :space-invaders [{:keys [delta virussen keys]}]
  (move-sprite virussen delta)

  (when (:ArrowLeft keys)
    (j/update! (sprite :ruimteschip) :x - 10))
  (when (:ArrowRight keys)
    (j/update! (sprite :ruimteschip) :x + 10))
  (when (:Space keys)
    (schiet-kogel!))

  (when (or (< (:x virussen) -400)
            (< 400 (:x virussen)))
    (j/update! virussen :vx * -1)
    (j/update! virussen :x + (* 2 (j/get virussen :vx)))
    (j/update! virussen :y + 20))

  (doseq [n [:ruimteschip :kogel]]
    (move-sprite (sprite n) delta))

  (let [kogel (sprite :kogel)]
    (doseq [x (range 8)
            y (range 3)
            :let [virus (sprite [:virus x y])]]
      (when (and (:visible kogel) (p/rect-overlap? kogel virus))
        (p/merge! kogel {:visible false})
        (disj! sprite-layer kogel)
        (disj! virussen virus)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Minispel

(def mm-svg-url "images/maniac-mansion-achtegronden.svg")

(defmethod load-scene :mm [scene]
  (promise/let [{:keys [jacko]} (p/load-resources! app {:jacko (:jacko images)})
                svg (svg/fetch-svg mm-svg-url)]
    (let [jacko (p/sprite jacko)

          elements (svg/elements svg)
          group    (group-by :type (vals elements))

          bg             (:background elements)
          base           (svg/base-texture (:html-image bg)
                                           mm-svg-url
                                           "maniac-mansion")
          debug-graphics (p/graphics)
          debug-view     (puck-daedalus/simple-view debug-graphics)

          player-loc   (daedalus/entity-ai {:x 2000 :y 950 :radius 20})
          rooms        (into {}
                             (map
                              (juxt :id
                                    (fn [{:keys [id rect]}]
                                      (let [obstacles (->> (group :obstacle)
                                                           (filter (comp #{id} :for))
                                                           (map :path))
                                            ratio     (/ 1000 (:height rect))
                                            width     (* (:width rect) ratio)]
                                        {:id     id
                                         :sprite (p/sprite (p/texture base rect))
                                         :width  width
                                         :mesh   (reduce
                                                  (fn [mesh path]
                                                    (run! #(m/!v* % ratio) path)
                                                    (conj! mesh (daedalus/polygon (map (juxt :x :y) path)))
                                                    mesh)
                                                  (daedalus/build-rect-mesh width 1000)
                                                  obstacles)}))))
                             (group :room))
          initial-room :hallway
          path         #js [2000 950]]
      (p/merge! jacko {:scale  {:x 0.2 :y 0.2}
                       :anchor {:x 0.5 :y 1}})
      (p/merge! debug-graphics {:x (- (/ (:width (get rooms initial-room)) 2))})
      (assoc scene
             :room initial-room
             :player-loc player-loc
             :jacko jacko
             :path path
             :path-finder (daedalus/path-finder {:entity player-loc
                                                 :mesh   (:mesh (get rooms initial-room))})
             :path-iterator (daedalus/linear-path-sampler {:entity           player-loc
                                                           :samplingDistance 10
                                                           :path             path})
             :rooms rooms
             :debug-graphics debug-graphics
             :debug-view debug-view
             :debug? true))))

(defmethod start-scene :mm [{:keys [rooms room jacko debug-graphics debug-view mesh debug? player-loc path]}]
  (draw-background (-> rooms room :sprite))
  (conj! sprite-layer jacko)
  (when debug?
    (conj! sprite-layer debug-graphics)
    (daedalus/draw-mesh debug-view mesh)
    (daedalus/draw-entity debug-view player-loc)
    (daedalus/draw-path debug-view path)))

(def debug-count (atom 0))

(defmethod tick-scene :mm [{:keys [jacko delta path-iterator player-loc path rooms room debug-view debug?]}]
  #_  (walk-step jacko delta 2)
  (let [room (get rooms room)]
    (.next path-iterator)
    (let [room-width (:width room)
          mesh (:mesh room)]
      (when-let [x (:x player-loc)] (j/assoc! jacko :x (- x (/ room-width 2))))
      (when-let [y (:y player-loc)] (j/assoc! jacko :y y))
      (when (and debug? (= 0 (mod (swap! debug-count inc) 10)))
        (daedalus/clear debug-view)
        (daedalus/draw-mesh debug-view mesh)
        (daedalus/draw-entity debug-view player-loc)
        (daedalus/draw-path debug-view path)))

    (let [viewport-limit (/ (- (:width (p/local-bounds bg-layer)) (visible-world-width)) 2)]
      (when (and (< 300 (+ (:x jacko) (:x viewport)))
                 (< (- (:x viewport)) viewport-limit))
        ;; Pan viewport to the right
        (j/assoc! viewport :x (- 300 (:x jacko))))
      (when (and (< (+ (:x jacko) (:x viewport)) -300)
                 (< (:x viewport) viewport-limit))
        ;; Pan viewport to the left
        (j/assoc! viewport :x (- (- (:x jacko)) 300))))))

(defmethod handle-event :mm [{:keys [jacko path-finder path-iterator path rooms room]} [t e]]
  (let [room-width (-> rooms room :width)
        coords (if (:clientX e) e (first (:touches e)))
        destination (.applyInverse (get-in viewport [:transform :worldTransform])
                                   (m/point (:clientX coords) (:clientY coords)))]

    (daedalus/find-path path-finder (+ (:x destination) (/ room-width 2)) (:y destination) path)
    (.reset path-iterator)
    #_(j/assoc! jacko :destination destination)))

(daedalus/path-iterator {:entity (daedalus/entity-ai {:x 10 :y 20})})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Minispel

(defonce init-once (promise/do
                     (init!)
                     (prn "set scene")
                     (swap! state assoc :scene #_:space-invaders :mm)
                     (doseq [t [:click :touchstart]]
                       (p/listen! (first (js/document.getElementsByTagName "canvas"))
                                  (name t) t
                                  (fn [e] (handle-event (scene-state) [t e]))))))

(defn on-hot-reload []
  (log/info :hot-reload! {})
  (stop-scene (scene-state))
  (start-scene (scene-state)))

(comment
  (swap! state assoc :scene :mm)
  (swap! state assoc :scene :space-invaders)

  (scene-state)

  (j/assoc! world :x 0)
  )
