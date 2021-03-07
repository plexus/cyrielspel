(ns lambdaisland.puck.types
  "Extend Pixi and DOM types with ClojureScript protocols."
  (:require ["pixi.js" :as pixi]
            ["resource-loader" :as resource-loader]
            [clojure.string :as str]
            [applied-science.js-interop :as j]
            [camel-snake-kebab.core :as csk]
            [lambdaisland.data-printers :as data-printers]))

(defn lookupify
  "Access object properties via keyword access, and allow destructuring with
  `:keys`"
  [type]
  (extend-type type
    ILookup
    (-lookup
      ([this k]
       (j/get this k))
      ([this k not-found]
       (j/get this k not-found)))
    ITransientAssociative
    (-assoc! [this k v]
      (j/assoc! this k v)
      this)
    ITransientMap
    (-dissoc! [this k]
      (js-delete this k)
      this)))

(defn add-childify [type]
  (extend-type type
    ITransientCollection
    (-conj! [^js this v]
      (.addChild this v)
      this)
    ITransientSet
    (-disjoin! [^js this v]
      (.removeChild this v))))

(defn register-printer [type tag to-edn]
  (data-printers/register-print type tag to-edn)
  (data-printers/register-pprint type tag to-edn)
  (lookupify type))

(defn register-keys-printer [type tag keys]
  (register-printer type tag (fn [obj]
                               (reduce (fn [m k]
                                         (assoc m k (j/get obj k)))
                                       {}
                                       keys))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Pixi

(register-keys-printer pixi/Application 'pixi/Application [:stage :renderer])
(register-keys-printer pixi/Renderer 'pixi/Renderer [:view :screen :options])
(register-keys-printer pixi/Loader 'pixi/Loader [:baseUrl :resources :progress :loading])
(register-keys-printer pixi/resources.Resource 'pixi/Resource [:name :url :type :metadata :spritesheet :textures])

(register-keys-printer pixi/Point 'pixi/Point [:x :y])
(register-keys-printer pixi/ObservablePoint 'pixi/ObservablePoint [:x :y])
(register-keys-printer pixi/Matrix 'pixi/Matrix [:a :b :c :d :tx :ty])
(register-keys-printer pixi/Transform 'pixi/Transform [:worldTransform :localTransform :position :scale :pivot :skew])

(register-keys-printer pixi/Container 'pixi/Container [:children :transform :visible])
(register-keys-printer pixi/Sprite 'pixi/Sprite [:position :anchor :scale :texture])
(register-keys-printer pixi/Texture 'pixi/Texture [:baseTexture :orig :trim])
(register-keys-printer pixi/BaseTexture 'pixi/BaseTexture [:width :height :resolution :resource])
(register-keys-printer pixi/Rectangle 'pixi/Rectangle [:x :y :width :height])


(register-keys-printer resource-loader/Resource 'resource-loader/Resource [:name :url :type :metadata :spritesheet :textures])
(register-keys-printer pixi/resources.Resource 'pixi/Resource [:name :url :type :metadata :spritesheet :textures])
(register-keys-printer pixi/resources.ImageResource 'pixi/ImageResource [:url])

(register-keys-printer pixi/Ticker 'pixi/Ticker [:deltaTime :deltaMS :elapsedMS :lastTime :speed :started])

(add-childify pixi/Container)

(extend-type pixi/Loader
  ITransientCollection
  (-conj! [^js this [k v]]
    (.add this k v)))

(register-keys-printer pixi/InteractionEvent 'pixi/InteractionEvent [:type :target :stopped :stopsPropagationAt :stopPropagationHint :currentTarget :date])

(register-keys-printer pixi/InteractionData 'pixi/InteractionData [:global :target :originalEvent :identifier :isPrimary :button :buttons :width :height :tiltX :tiltY :pointerType :pressure :rotationAngle :twist :tangentialPressure])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DOM / browser types

(register-printer
 js/Element
 'js/Element (fn hiccupize [^js e]
               (if (string? e)
                 e
                 (let [el [(keyword (str/lower-case (.-tagName e)))]]
                   (into (if-let [attrs (seq (.getAttributeNames e))]
                           (conj el (into {}
                                          (map (juxt csk/->kebab-case-keyword
                                                     #(.getAttribute e %)))
                                          attrs))
                           el)
                         (map hiccupize)
                         (.-children e))))))

(register-printer
 js/HTMLDocument
 'js/HTMLDocument
 (fn [^js d]
   {:root (.-documentElement d)}))

(register-keys-printer js/Window 'js/Window [:location :document :devicePixelRatio :innerWidth :innerHeight])

(register-printer js/Location 'js/Location str)

(def has-touch-event? (exists? js/TouchEvent))

(when has-touch-event?
  (register-keys-printer js/TouchEvent 'js/TouchEvent [:altKey :changedTouches :ctrlKey :metaKey :shiftKey :targetTouches :touches])
  (register-keys-printer js/Touch 'js/Touch [:identifier :screenX :screenY :clientX :clientY :pageX :pageY :target])
  (register-printer js/TouchList 'js/TouchList (comp vec seq)))

(register-keys-printer js/PointerEvent 'js/PointerEvent [:pointerId :width :height :pressure :tangentialPressure :tiltX :tiltY :twist :pointerType :isPrimary])

(def mouse-event-keys [:altKey :button :buttons :clientX :clientY :ctrlKey :metaKey :movementX :movementY
                       ;; "These are experimental APIs that should not be used in production code" -- MDN
                       ;; :offsetX :offsetY :pageX :pageY
                       :region :relatedTarget :screenX :screenY :shiftKey])

(register-keys-printer js/MouseEvent 'js/MouseEvent mouse-event-keys)
(register-keys-printer js/WheelEvent 'js/WheelEvent (conj mouse-event-keys :deltaX :deltaY :deltaZ :deltaMode))
(register-keys-printer js/DragEvent 'js/DragEvent (conj mouse-event-keys :dataTransfer))
