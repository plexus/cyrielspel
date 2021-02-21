(ns colorjoy
  (:require [quil.core :as q :include-macros true]
            ))

;;;
;;; Example from Abe Pazos
;;; http://hamoid.com
;;;

(defn setup []
  (q/frame-rate 60)
  (q/color-mode :hsb 1)
  (q/background 0.05)
  )

(defn draw []
  (let [F (/ (q/frame-count) 500.0)]
    (doseq [x (range 0 (q/width))]
      (let [
            margin (/ (q/height) 6.0)
            i (/ x (q/width))
            sat (+ 0.5 (* 0.4 (Math/sin (* 14 (q/noise i 5 F)))))
            bri (+ 0.5 (* 0.4 (Math/sin (* 21 (q/noise 6 (- F) (* i 5))))))
            ]
        (q/stroke (mod (+ i F) 1.0) sat bri)
        (q/line x margin x (- (q/height) margin)))
      )
    )
  )

(q/defsketch colorjoy
  :host "host"
  :size [500 500]
  :setup setup
  :draw draw)