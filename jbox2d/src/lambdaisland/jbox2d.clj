(ns lambdaisland.jbox2d
  (:require [quil.core :as q]
            [lambdaisland.data-printer :as data-printer])
  (:import (org.jbox2d.common Vec2)
           (org.jbox2d.collision.shapes Shape PolygonShape)
           (org.jbox2d.dynamics World Body BodyDef BodyType FixtureDef)))

(defprotocol IValue
  (value [_]))

(extend-protocol IValue
  Vec2
  (value [v]
    [(.-x v) (.-y v)])
  BodyDef
  (value [b]
    {:active (.-active b)
     :allow-sleep? (.-allowSleep b)
     :angle (.-angle b)
     :angular-damping (.-angularDamping b)
     :angular-velocity (.-angularVelocity b)
     :awake (.-awake b)
     :bullet (.-bullet b)
     :fixed-rotation (.-fixedRotation b)
     :gravity-scale (.-gravityScale b)
     :linear-damping (.-linearDamping b)
     :linear-velocity (.-linearVelocity b)
     :position (.-position b)
     :type (.-type b)
     :user-data (.-userData b)})
  FixtureDef
  (value [f]
    {:density (.-density f)
     :filter (.-filter f)
     :friction (.-friction f)
     :sensor? (.-isSensor f)
     :restitution (.-restitution f)
     :shape (.-shape f)
     :user-data (.-userData f)})
  Body
  (value [b]
    {:position (.getPosition b)
     :angle (.getAngle b)}))

(data-printer/register-type-printer Vec2 'jbox2d/vec2 value)
(data-printer/register-type-printer BodyDef 'jbox2d/body-def value)
(data-printer/register-type-printer FixtureDef 'jbox2d/fixture-def value)
(data-printer/register-type-printer Body 'jbox2d/body value)
(data-printer/register-type-printer BodyType 'jbox2d/body-type str)

(defn vec2 ^Vec2 [^double x ^double y]
  (Vec2. x y))

(def body-type {:kinematic BodyType/KINEMATIC
                :dynamic BodyType/DYNAMIC
                :static BodyType/STATIC})

(defn body-def [{:keys [active allow-sleep? angle angular-damping angular-velocity awake
                        bullet fixed-rotation gravity-scale linear-damping linear-velocity position
                        type user-data]}]
  (let [b (BodyDef.)]
    (when (some? active) (set! (.-active b) active))
    (when (some? allow-sleep?) (set! (.-allowSleep b) allow-sleep?))
    (when (some? angle) (set! (.-angle b) angle))
    (when (some? angular-damping) (set! (.-angularDamping b) angular-damping))
    (when (some? angular-velocity) (set! (.-angularVelocity b) angular-velocity))
    (when (some? awake) (set! (.-awake b) awake))
    (when (some? bullet) (set! (.-bullet b) bullet))
    (when (some? fixed-rotation) (set! (.-fixedRotation b) fixed-rotation))
    (when (some? gravity-scale) (set! (.-gravityScale b) gravity-scale))
    (when (some? linear-damping) (set! (.-linearDamping b) linear-damping))
    (when (some? linear-velocity) (set! (.-linearVelocity b) linear-velocity))
    (when (some? position) (set! (.-position b) position))
    (when (some? type) (set! (.-type b) (get body-type type)))
    (when (some? user-data) (set! (.-userData b) user-data))
    b))

(defn fixture-def [{:keys [density filter friction sensor?
                           restitution shape user-data]}]
  (let [f (FixtureDef.)]
    (when (some? density) (set! (.-density f) density))
    (when (some? filter) (set! (.-filter f) filter))
    (when (some? friction) (set! (.-friction f) friction))
    (when (some? sensor?) (set! (.-isSensor f) sensor?))
    (when (some? restitution) (set! (.-restitution f) restitution))
    (when (some? shape) (set! (.-shape f) shape))
    (when (some? user-data) (set! (.-userData f) user-data))
    f))

(defn body [world props]
  (.createBody world (body-def props)))

(defn set-as-box [polygon-shape hx hy]
  (.setAsBox polygon-shape hx hy)
  polygon-shape)

(defn fixture
  ([body f]
   (cond
     (instance? Shape f)
     (fixture body f 0)
     (map? f)
     (fixture body (fixture-def f))
     (instance? FixtureDef f)
     (.createFixture body ^FixtureDef f)))
  ([body shape density]
   (.createFixture body ^Shape shape ^double density)))

(defn step-world
  ([world]
   (step-world world 1/60))
  ([world timestep]
   (step-world world timestep 4 2))
  ([world timestep velocity-iterations position-iterations]
   (.step world (float timestep) velocity-iterations position-iterations)))

(defn body-seq [world]
  (loop [body (.getBodyList world)
         bodies (list body)]
    (if-let [b (.getNext body)]
      (recur b (cons b bodies))
      bodies)))

(defn world [gravity-x gravity-y]
  (World. (vec2 gravity-x gravity-y)))

(defn rectangle [w h]
  (set-as-box (PolygonShape.) w h))