(ns trypixi.core
  (:require ["hammerjs" :as Hammer]
            ["pixi.js" :as pixi]
            ["@pixi/filter-pixelate" :as pixelate]
            ["@pixi/filter-crt" :as crt]
            [applied-science.js-interop :as j]
            [clojure.string :as str]
            [lambdaisland.puck :as p]
            [kitchen-async.promise :as promise]
            [lambdaisland.puck.math :as m]
            [lambdaisland.daedalus :as daedalus])
  (:require-macros [lambdaisland.puck.interop :refer [merge!]]))

(def spritesheets {:sprites "images/sprites.json"
                   :minispel "images/minispel/minispel.json"})

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

(def state (atom {:speler :jacko
                  :achtergrond :hut}))

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

(defn screen-size []
  (get-in app [:renderer :screen]))

(defn screen-to-world-ratio []
  (let [{:keys [width height]} (screen-size)]
    (/ height world-height)))

(defn visible-world-width []
  (/ (:width (screen-size)) (screen-to-world-ratio)))

(defn resize-pixi []
  (let [ratio (screen-to-world-ratio)]
    (merge! world {:scale {:x ratio
                           :y ratio}}))
  (.beginFill graphics achtergrond-kleur)
  (let [{:keys [width height]} (screen-size)]
    (.drawRect graphics 0 0 width height))
  (.endFill graphics))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn make-sprite!
  ([name texture]
   (let [sprite (p/sprite texture)]
     (swap! state assoc-in [:sprites name] sprite)
     sprite)))

(defn sprite [name]
  (get-in @state [:sprites name]))

(defn handle-load-sprites [{:keys [sprites]}]
  (make-sprite! :sensei (p/resource-texture app :sprites "sensei.png"))
  (println :done-load-sprites))

(defn handle-load-images [images]
  (doseq [[k img] images]
    (make-sprite! k img))
  (println :done-load-images))

(defn draw-background [sprite]
  (.removeChildren bg-layer)
  (let [{:keys [width height]} (screen-size)]
    (merge! sprite {:anchor {:x 0 :y 0}
                    :scale {:x (/ world-height (:height sprite))
                            :y (/ world-height (:height sprite))}})
    (assoc! stage :x (- (/ width (screen-to-world-ratio) 2)
                        (/ (:width sprite) 2)))
    (conj! bg-layer sprite)))

(defn teken-scene [{:keys [speler achtergrond]}]
  (let [speler (sprite speler)
        achtergrond (sprite achtergrond)]
    (merge! speler {:x 500
                    :y 500
                    :anchor {:x 0.5 :y 0.5}
                    :scale {:x 0.1 :y 0.1}})
    (conj! sprite-layer speler)
    (draw-background achtergrond)))

(defn touch-start [e]
  (let [touch (first (:touches e))]
    (j/assoc! (sprite :sensei) :destination
              (.applyInverse (get-in app [:stage :transform :worldTransform])
                             (m/point (:clientX touch) (:clientY touch))))))

(defn game-loop [delta]
  (try
    (let [
          {:keys [destination position]} (sprite :sensei)]
      (when destination
        (when (< 5 (m/distance position destination))
          (let [diff (m/v- destination position)
                dist (m/vdiv diff (m/length diff))
                step (m/v* dist (* delta 2))]
            (m/!v+ position step)))))
    (catch :default e
      (prn "game-loop error" e))))

(j/assoc! stage
          :filters
          #js [#_(doto (pixi/filters.ColorMatrixFilter.) (.polaroid))
               #_(pixelate/PixelateFilter. 5)
               #_(crt/CRTFilter. #js {:lineWidth 0.2
                                      :vignetting 0})])
(declare minispel-init)

(defn init! []
  (resize-pixi)
  (p/listen! app :resize resize-pixi)
  (p/pixelate!)

  (promise/do
    (promise/then (p/load-resources! app spritesheets) handle-load-sprites)
    (promise/then (p/load-resources! app images) handle-load-images)
    #_(teken-scene @state)

    (.addEventListener (get-in app [:renderer :view])
                       "touchstart"
                       (fn [e] (touch-start e)))

    (conj! (:ticker app) (fn [d] (game-loop d)))
    (minispel-init)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Minispel 1

(defn minispel-sprites-maken []
  )

(defn minispel-init []
  (.removeChildren sprite-layer)
  (.removeChildren bg-layer)
  (doseq [k [:ruimteschip :virus :loding :batterij
             :pijl-links :pijl-rechts :pijl-schiet]]
    (make-sprite! k (p/resource-texture app :minispel (str (name k) ".png"))))

  (let [schip (sprite :ruimteschip)
        links (sprite :pijl-links)
        rechts (sprite :pijl-rechts)
        schiet (sprite :pijl-schiet)]

    (conj! sprite-layer schip links rechts schiet)

    (merge! schip {:x (/ (visible-world-width) 2)
                   :y 700})

    (merge! links {:x 100
                   :y 900
                   :interactive true})
    (merge! rechts {:x 250
                    :y 900
                    :interactive true})
    (merge! schiet {:x (- (visible-world-width) 200)
                    :y 850
                    :interactive true})))

pixi/interaction

(defonce init-once (init!))

(minispel-init)

#_(Hammer. (first (js/document.getElementsByTagName "canvas")))
