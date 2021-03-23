(ns lambdaisland.puck.daedalus
  (:require [lambdaisland.daedalus :as dae]
            [lambdaisland.puck :as puck]
            [lambdaisland.puck.math :as math]))

(defrecord PixiBasicCanvas [graphics]
  Object
  (clear [this]
    (.clear graphics))
  (lineStyle [this thickness color alpha]
    (.lineStyle graphics thickness color alpha))
  (beginFill [this color alpha]
    (.beginFill graphics color alpha))
  (endFill [this]
    (.endFill graphics))
  (moveTo [this x y]
    (.moveTo graphics x y))
  (lineTo [this x y]
    (.lineTo graphics x y))
  (quadTo [cx cy ax ay]
    (.quadraticCurveTo cx cy ax ay))
  (drawCircle [cx cy radius]
    (.drawCircle graphics cx cy radius))
  (drawRect [x y width height]
    (.drawRect graphics x y width height))
  (drawTri [this points]
    (.drawPolygon graphics (into-array (map (fn [[x y]] (math/point x y))
                                            (partition 2 points))))))

(defn simple-view [pixi-graphics]
  (dae/simple-view (PixiBasicCanvas. pixi-graphics) {}))
