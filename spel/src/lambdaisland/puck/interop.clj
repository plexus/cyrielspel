(ns lambdaisland.puck.interop
  (:require [applied-science.js-interop :as j]))

(defmacro merge! [obj m]
  (let [keypaths (fn keypaths [m]
                   (mapcat (fn [[k v]]
                             (if (map? v)
                               (map #(into [k] %) (keypaths v))
                               [[k]]))
                           m))]
    (cons 'do
          (for [p (keypaths m)
                :let [v (get-in m p)]]
            `(j/assoc-in! ~obj ~p ~v)))))
