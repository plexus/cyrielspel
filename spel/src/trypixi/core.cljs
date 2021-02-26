(ns trypixi.core
  (:require ["hammerjs" :as Hammer]
            ["pixi.js" :as pixi]
            [applied-science.js-interop :as j]
            [clojure.string :as str]
            [lambdaisland.puck :as p]
            [kitchen-async.promise :as promise]
            [lambdaisland.puck.math :as m]
            [lambdaisland.daedalus])
  (:require-macros [lambdaisland.puck.interop :refer [merge!]]))

#_(Hammer. (first (js/document.getElementsByTagName "canvas")))

(def world-height 1000)

(defonce ^js app (p/full-screen-app))

(defn resize-pixi []
  (let [{:keys [width height]} (get-in app [:renderer :screen])]
    (merge! app {:stage {:position {:x (/ width 2) :y (/ height 2)}
                         :scale {:x (/ height world-height)
                                 :y (/ height world-height)}}})))

(resize-pixi)

(p/listen! app :resize resize-pixi)
(p/pixelate!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce sprites (atom {}))

(defn sprite
  ([id]
   (get @sprites id))
  ([id texture]
   (when texture
     (if-let [sprite (get @sprites id)]
       sprite
       (let [sprite (pixi/Sprite. texture)]
         (swap! sprites assoc id sprite)
         sprite)))))

(declare sensei)

(defn init! []
  (promise/let [[sprites bg] (p/load-resources!
                              app
                              "images/sprites.json"
                              "images/magic-cliffs-preview-detail.png")]
    (let [se (p/sprite (j/get (:textures sprites) "sensei.png"))
          bg (p/sprite bg)]
      (set! sensei se)
      (merge! se {:anchor {:x 0.5 :y 0.5}
                  :scale {:x 8 :y 8}})
      (merge! bg {:anchor {:x 0.5 :y 0.5}
                  :scale {:x (/ world-height (:height bg))
                          :y (/ world-height (:height bg))}})
      (conj! (:stage app) bg se))))

;; (defonce display (text "hello"))

;; (j/assoc-in! display [:style :fill] "white")
;; (j/assoc! display :x 10 :y 100)

(defonce init-once (init!))

(defn touch-start [e]
  (let [touch (first (:touches e))]
    (j/assoc! sensei :destination
              (.applyInverse (get-in app [:stage :transform :worldTransform])
                             (m/point (:clientX touch) (:clientY touch))))))

(defonce touch-start-once
  (.addEventListener (get-in app [:renderer :view])
                     "touchstart"
                     (fn [e] (touch-start e))))

(defn game-loop [delta]
  (try
    (let [{:keys [destination position]} sensei]
      (when destination
        (when (< 5 (m/distance position destination))
          (let [diff (m/v- destination position)
                dist (m/vdiv diff (m/length diff))
                step (m/v* dist (* delta 2))]
            (m/!v+ position step)))))
    (catch :default e
      (prn "game-loop error" e))))

(defonce game-loop-once
  (p/add (:ticker app) (fn [d] (game-loop d))))
