(ns lambdaisland.daedalus.extract-types
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(def files (->> "node_modules/hxdaedalus-js/src/hxDaedalus"
                io/file
                file-seq
                (filter #(str/ends-with? % ".hx"))))

(spit "resources/daedalus_types.edn"
      (pr-str
       (for [file files]
         (let [source (slurp file)
               [_ package] (re-find #"package ([^;]+)" source)
               [_ klass] (re-find #"class ([^\s\{]*)" source)
               var-lines (filter #(re-find #"public var" %) (str/split source #"\R"))
               plain-vars (remove #(re-find #"\(" %) var-lines)
               getters (filter #(re-find #"\(get" %) var-lines)
               varname #(second (re-find #"public var\s+(\w+)" %))]
           [package klass (map varname plain-vars) (map varname getters)]))))

(run! prn
      (for [{:keys [package klass fullname plain-vars getters]}
            (sort-by :fullname
                     (for [file files]
                       (let [source (slurp file)
                             [_ package] (re-find #"package ([^;]+)" source)
                             [_ klass] (re-find #"class ([^\s\{]*)" source)
                             var-lines (filter #(re-find #"public var" %) (str/split source #"\R"))
                             plain-vars (remove #(re-find #"\(" %) var-lines)
                             getters (filter #(re-find #"\(get" %) var-lines)
                             varname #(keyword (second (re-find #"public var\s+(\w+)" %)))]
                         {:package package
                          :klass klass
                          :plain-vars (mapv varname plain-vars)
                          :getters (mapv varname getters)
                          :fullname (symbol (str "daedalus/" package "." klass))})))]
        #_`(~'def ~(symbol klass) ~fullname)
        `(~'setup-type ~(symbol klass) ~(symbol (str "'daedalus." (str/replace package "hxDaedalus." "") "/" klass)) ~plain-vars ~getters)
        ))
