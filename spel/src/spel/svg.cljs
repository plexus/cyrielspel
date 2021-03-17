(ns spel.svg
  (:require ["pixi.js" :as pixi]
            [applied-science.js-interop :as j]
            [kitchen-async.promise :as p]))

(defprotocol Coercions
  (>str [_])
  (>num [_]))

(extend-protocol Coercions
  js/SVGAnimatedLength
  (>num [this] (>num (.-baseVal this)))
  js/SVGLength
  (>num [this] (.-value this))
  js/SVGAnimatedString
  (>str [this] (.-baseVal this))
  js/CharacterData
  (>str [this] (.-data this)))

(defn query [^js dom selector]
  (.querySelector dom selector))

(defn query-all [^js dom selector]
  (.querySelectorAll dom selector))

(defn fetch-svg
  "Given a URL to an SVG file, return a Promise of the root of the SVG DOM tree."
  [url]
  (p/let [res (js/fetch url)
          txt (.text res)
          div (doto (js/document.createElement "div")
                (j/assoc! :style "display:none"))]
    (set! (.-innerHTML div) txt)
    (first (.-children div))))

(defn svg-image->html-img
  "Given an svg <image> element (SVGImageElement), return a equivalent
  HTMLImageElement."
  [image]
  (let [html-image (js/document.createElement "img")]
    (when-let [width (j/get image :width)]
      (j/assoc! html-image :width (>num width)))
    (when-let [height (j/get image :height)]
      (j/assoc! html-image :height (>num height)))
    (j/assoc! html-image :src (>str (j/get image :href)))))

(defn base-texture
  "Create a pixi BaseTexture that can be used to create multiple textures with the
  same underlying image data."
  [html-image url name]
  (j/get (pixi/Texture.fromLoader html-image url name) :baseTexture))

(defn svg-rect->pixi
  "Get a pixi Rectangle for a given SVGRectElement (`<rect>`)"
  [rect]
  (pixi/Rectangle. (.getAttribute rect "x")
                   (.getAttribute rect "y")
                   (.getAttribute rect "width")
                   (.getAttribute rect "height")))

(defn load-svg
  "Load an SVG and return Pixi stuff. Assumes the SVG contains a single `<image>`
  tag with a `data:` url, and a number a `<rect>` elements definining the
  bounding boxes of sub-textures."
  [url name]
  (p/let [svg (fetch-svg url)]
    (let [svg-img (query svg "image")
          html-img (svg-image->html-img svg-img)
          base (base-texture html-img url name)]
      {:base-texture base
       :textures (into {}
                       (for [rect (query-all svg "rect")]
                         #_(query (.-parentElement rect) "desc")
                         [(keyword (.getAttribute rect "id")) (pixi/Texture. base (svg-rect->pixi rect))]))})))



;; (p/let [res (load-svg "images/maniac-mansion-achtegronden.svg" "maniac-mansion")]
;;   (def rr res))

;; (def svg-img (query sss "image"))
;; (def html-img (svg-image->html-img svg-img))

;; (def base (base-texture html-img ))



;; (query sss "#g877")
;; (.getAttribute (query sss "path") "d")
;; #_
;; (conj! (.-body (:document js/window)) sss)
