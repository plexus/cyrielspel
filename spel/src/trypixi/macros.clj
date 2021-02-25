(ns trypixi.macros
  (:require [trypixi.data-printer :as data-printer]
            [applied-science.js-interop :as j]))

(defmacro register-printer [type keys]
  `(when (cljs.core/exists? ~type)
     (data-printer/register-type-printer ~type '~type #(select-keys (j/lookup %) ~keys))))

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
