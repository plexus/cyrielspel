(ns lambdaisland.puck
  (:require ["pixi.js" :as pixi]
            ["hammerjs" :as Hammer]
            [clojure.string :as str]
            [cljs-bean.core :as bean]
            [applied-science.js-interop :as j]
            [kitchen-async.promise :as p]
            [lambdaisland.puck.types]
            [camel-snake-kebab.core :as csk]))

(defn opts->js [m]
  (clj->js m :keyword-fn csk/->camelCaseString))

(defn application ^js [pixi-opts]
  (pixi/Application. (opts->js pixi-opts)))

(defn full-screen-app
  ([]
   (full-screen-app nil))
  ([opts]
   (let [app (application (merge {:width js/window.innerWidth
                                  :height js/window.innerHeight
                                  :resolution js/window.devicePixelRatio
                                  :auto-density true
                                  :auto-resize true
                                  :resize-to js/window}
                                 opts))]
     (js/document.body.appendChild (:view app))
     app)))

(defn say-hello! []
  (.sayHello ^js pixi/utils (if (.isWebGLSupported ^js pixi/utils)
                              "WebGL"
                              "canvas")))

(defn pixelate!
  "Tell pixi to not blur when scaling."
  []
  (j/assoc-in! pixi/settings [:SCALE_MODE] pixi/SCALE_MODES.NEAREST))

(defn listen!
  "Hook into pixi's \"Runners\", event listeners attached to the renderer. Known signals:
  - `:destroy`
  - `:contextChange`
  - `:reset`
  - `:update`
  - `:postrender`
  - `:prerender`
  - `:resize`"
  [app signal callback]
  (.add ^js (j/get (get-in app [:renderer :runners]) signal) (j/lit {signal callback})))

(defn resource [app rname]
  (j/get (get-in app [:loader :resources]) (if (keyword? rname)
                                             (name rname)
                                             name)))

(defn resource-texture [app rname tname]
  (j/get (:textures (resource app rname)) tname))

(defn load! [app cb]
  (.load ^js (:loader app) cb))

(defn load-resources! [app resources]
  (let [rkeys (keys resources)
        ^js loader (:loader app)]
    (p/promise [resolve reject]
      (when-let [missing (seq (remove #(resource app %) rkeys))]
        (doseq [k missing]
          (prn k)
          (.add loader (name k) (get resources k))))
      (.load loader
             (fn []
               (resolve (into {} (map (juxt identity #(resource app %))) rkeys)))))))

(defn sprite [resource-or-texture]
  (pixi/Sprite. (if-let [texture (:texture resource-or-texture)]
                  texture
                  resource-or-texture)))

(defn text
  ([msg] (pixi/Text. msg))
  ([msg style] (pixi/Text. msg style)))
