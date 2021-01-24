(ns quil-extras
  (:require [quil.core :as q]
            [quil.applet :as ap]))

(defn achtergrond
  "Stel de afbeelding in als achtergrond. De grootte van de sketch wordt aangepast
  zodat de afbeelding past."
  [bestandsnaam]
  (let [img (.loadImage (ap/current-applet) bestandsnaam)
        ratio (max (/ (q/width) (.-width img))
                   (/ (q/height) (.-height img)))
        width (* (.-width img) ratio)
        height (* (.-height img) ratio)]
    (q/resize-sketch width height)
    (q/resize img width height)
    ;; Hack... if the sketch resize hasn't been fully finished yet we'll get an
    ;; error, so retry a few times with small sleep calls in between
    (loop [i 0]
      (when-let [ex (try
                      (q/background-image img)
                      nil
                      (catch java.lang.ArrayIndexOutOfBoundsException e
                        e))]
        (if (< i 5)
          (do
            (Thread/sleep 20)
            (recur (inc i)))
          (throw ex))))
    img))
