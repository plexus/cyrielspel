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

#_(Hammer. (first (js/document.getElementsByTagName "canvas")))

(def state (atom {:speler :jacko
                  :achtergrond :hut}))

(def world-height 1000)

(defonce ^js app (p/full-screen-app))
(defonce ^js stage (:stage app))
(defonce ^js bg-layer (pixi/Container.))
(defonce ^js sprite-layer (pixi/Container.))

(defonce add-layers-once (conj! stage bg-layer sprite-layer))

(defn screen-size []
  (get-in app [:renderer :screen]))

(defn screen-to-world-ratio []
  (let [{:keys [width height]} (screen-size)]
    (/ height world-height)))

(defn resize-pixi []
  (let [ratio (screen-to-world-ratio)]
    (merge! app {:stage {:scale {:x ratio
                                 :y ratio}}})))

(resize-pixi)

(p/listen! app :resize resize-pixi)
(p/pixelate!)

(j/assoc! stage
          :filters
          #js [(pixelate/PixelateFilter. 5)
               (crt/CRTFilter. #js {:lineWidth 0.2
                                    :vignetting 0})
               (doto (pixi/filters.ColorMatrixFilter.) (.polaroid))])



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def spritesheets {:sprites "images/sprites.json"})

(def images {:jacko          "images/jacko.png"
             :cliffs         "images/magic-cliffs-preview-detail.png"
             :fabriek-binnen "images/fabriek_binnen.jpg"
             :fabriek-buiten "images/fabriek_buiten.jpg"
             :hut            "images/hut.jpg"
             :oude-fabriek   "images/oude_fabriek.jpg"
             :supermarkt     "images/supermarkt.jpg"
             :werkplaats     "images/werkplaats.jpg"})

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

(defn init! []
  (promise/do
    (promise/then (p/load-resources! app spritesheets) handle-load-sprites)
    (promise/then (p/load-resources! app images) handle-load-images)
    (teken-scene @state)))

(sprite (:achtergrond @state))
;; (defonce display (text "hello"))

;; (j/assoc-in! display [:style :fill] "white")
;; (j/assoc! display :x 10 :y 100)

(defonce init-once (init!))

(defn touch-start [e]
  (let [touch (first (:touches e))]
    (j/assoc! (sprite :sensei) :destination
              (.applyInverse (get-in app [:stage :transform :worldTransform])
                             (m/point (:clientX touch) (:clientY touch))))))

(defonce touch-start-once
  (.addEventListener (get-in app [:renderer :view])
                     "touchstart"
                     (fn [e] (touch-start e))))

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

(defonce game-loop-once
  (conj! (:ticker app) (fn [d] (game-loop d))))
