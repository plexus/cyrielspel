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
            [lambdaisland.daedalus :as daedalus]
            [lambdaisland.glogi.console :as glogi-console]
            [lambdaisland.glogi :as log])
  (:require-macros [lambdaisland.puck.interop :refer [merge!]]))

(defonce state (atom {:scene nil
                      :time 0
                      :scenes {:sensei {}
                               :space-invaders {:virus-vx 1}}
                      :sprites {}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(defonce ^js app (p/full-screen-app {:background-color "white"}))
(defonce ^js stage (:stage app))
(defonce ^js world (pixi/Container.))
(defonce ^js fill-layer (pixi/Container.))
(defonce ^js bg-layer (pixi/Container.))
(defonce ^js sprite-layer (pixi/Container.))
(defonce ^js renderer (:renderer app))

(defonce ^js graphics (pixi/Graphics.))

(defonce add-layers-once
  (do
    (conj! stage fill-layer world)
    (conj! fill-layer graphics)
    (conj! world bg-layer sprite-layer)))

;; stage
;; |- fill
;; |- world
;;    |- bg-layer
;;    |- sprites
;;       |- virussen -> virus 1 / virus 2
;;       |- ruimteschip

(j/assoc! stage :filters
          #js [#_(doto (pixi/filters.ColorMatrixFilter.) (.polaroid))
               #_(pixelate/PixelateFilter. 5)
               #_(crt/CRTFilter. #js {:lineWidth 0.2
                                      :vignetting 0})])


(defmulti load-scene :scene)
(defmulti start-scene :scene)
(defmulti tick-scene :scene)
(defmulti stop-scene :scene)
(defmethod load-scene :default [])
(defmethod start-scene :default [])
(defmethod tick-scene :default [])
(defmethod stop-scene :default [])

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
               (log/info :switching-scene {:old (:scene old) :new (:scene new)} )
               (promise/do
                 (stop-scene old)
                 (when-not (:loaded? new)
                   (log/debug :loading-scene new)
                   (promise/do
                     (load-scene new)
                     (swap! state assoc-in [:scenes (:scene new) :loaded?] true)))
                 (start-scene new)))))

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
      (merge! world {:x (/ width 2)
                     :scale {:x ratio
                             :y ratio}}))
    (.beginFill graphics achtergrond-kleur)
    (.drawRect graphics 0 0 width height)
    (.endFill graphics)))

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
  (let [{:keys [width height]} (screen-size)]
    (merge! sprite {:anchor {:x 0 :y 0}
                    :scale {:x (/ world-height (:height sprite))
                            :y (/ world-height (:height sprite))}})
    (assoc! stage :x (- (/ width (screen-to-world-ratio) 2)
                        (/ (:width sprite) 2)))
    (conj! bg-layer sprite)))

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
    (merge! speler {:x 500
                    :y 500
                    :anchor {:x 0.5 :y 0.5}
                    :scale {:x 0.1 :y 0.1}})
    (conj! sprite-layer speler)
    (draw-background achtergrond)))

(defmethod tick-scene :sensei [{:keys [delta]}]
  (let [{:keys [destination position]} (sprite :sensei)]
    (when destination
      (when (< 5 (m/distance position destination))
        (let [diff (m/v- destination position)
              dist (m/vdiv diff (m/length diff))
              step (m/v* dist (* delta 2))]
          (m/!v+ position step))))))

(defmethod stop-scene :sensei [{:keys [speler achtergrond]}]
  (disj! sprite-layer (sprite :sensei))
  (disj! bg-layer (sprite :cliffs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Minispel

(defn pad-hit-area! [target pad-x pad-y]
  (let [{:keys [x y width height]} (p/local-bounds target)]
    (j/assoc! target :hitArea (p/rectangle (- x (/ pad-x 2))
                                           (- y (/ pad-y 2))
                                           (+ width pad-x)
                                           (+ height pad-y)))))

(defmethod load-scene :space-invaders [_]
  (promise/let [{:keys [minispel]}
                (p/load-resources! app
                                   {:minispel "images/minispel/minispel.json"})]
    (doseq [k [:ruimteschip :loding :batterij :kogel
               :pijl-links :pijl-rechts :pijl-schiet]]
      (doto (make-sprite! k (j/get (:textures minispel) (str (name k) ".png")))
        (merge! {:anchor {:x 0.5 :y 0.5}})))
    (doseq [x (range 8)
            y (range 3)]
      (doto (make-sprite! [:virus x y] (j/get (:textures minispel) "virus.png"))
        (merge! {:anchor {:x 0.5 :y 0.5}})))))

(defmethod start-scene :space-invaders [_]
  (let [schip (sprite :ruimteschip)
        links (sprite :pijl-links)
        rechts (sprite :pijl-rechts)
        schiet (sprite :pijl-schiet)
        kogel (sprite :kogel)
        virussen (pixi/Container.)]

    (doseq [sprite [links rechts schiet]]
      (j/assoc! sprite :interactive true)
      (pad-hit-area! sprite 50 50))

    (doseq [e [:touchstart :mousedown]]
      (p/listen! links e #(j/assoc! schip :vx -10))
      (p/listen! rechts e #(j/assoc! schip :vx 10))
      (p/listen! schiet e (fn []
                            (merge! kogel {:x (:x schip)
                                           :y (- (:y schip) 110)
                                           :vy -10
                                           :visible true})
                            (conj! sprite-layer kogel))))

    (doseq [e [:touchend :mouseup]]
      (p/listen! links e #(j/assoc! schip :vx 0))
      (p/listen! rechts e #(j/assoc! schip :vx 0)))

    (merge! virussen {:x 0
                      :vx 2})

    (scene-swap! assoc :virussen virussen)

    (doseq [x (range 8)
            y (range 3)
            :let [sprite (sprite [:virus x y])]]
      (merge! sprite {:x (+ -400 (* 100 x))
                      :y (+ 100 (* 100 y))})
      (conj! virussen sprite))

    (conj! sprite-layer virussen schip links rechts schiet)

    (merge! schip {:x 0
                   :y 800})

    (merge! links {:x -650
                   :y 900
                   :interactive true})
    (merge! rechts {:x -500
                    :y 900
                    :interactive true})
    (merge! schiet {:x 550
                    :y 900
                    :interactive true})))

(defmethod stop-scene :space-invaders [_]
  (.removeChildren sprite-layer)
  (.removeChildren bg-layer))

(defn move-sprite [sprite delta]
  (j/update! sprite :x + (* (j/get sprite :vx 0) delta))
  (j/update! sprite :y + (* (j/get sprite :vy 0) delta)))

(def bump (Bump.))
(defn collission? [a b]
  #_(.hitTestRectangle bump a b)

  (let [ab (.getBounds ^js a)
        bb (.getBounds ^js b)]
    (and (< (:x bb) (+ (:x ab) (:width ab)))
         (< (:x ab) (+ (:x bb) (:width bb)))
         (< (:y bb) (+ (:y ab) (:height ab)))
         (< (:y ab) (+ (:y bb) (:height bb))))))

(defmethod tick-scene :space-invaders [{:keys [delta virussen]}]
  (move-sprite virussen delta)

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
      (when (and (:visible kogel) (collission? kogel virus))
        (merge! kogel {:visible false})
        (disj! sprite-layer kogel)
        (disj! virussen virus)))))

(defonce init-once (promise/do
                     (init!)
                     (swap! state assoc :scene :space-invaders)))

(defn on-hot-reload []
  (log/info :hot-reload! {})
  (stop-scene (scene-state))
  (start-scene (scene-state)))

#_(swap! state assoc :scene :space-invaders)

#_(Hammer. (first (js/document.getElementsByTagName "canvas")))

#_(defn touch-start [e]
    (let [touch (first (:touches e))]
      (j/assoc! (sprite :sensei) :destination
                (.applyInverse (get-in app [:stage :transform :worldTransform])
                               (m/point (:clientX touch) (:clientY touch))))))
