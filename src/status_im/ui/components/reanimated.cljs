(ns status-im.ui.components.reanimated
  (:refer-clojure :exclude [set])
  (:require [reagent.core :as reagent]
            [oops.core :refer [oget ocall]]
            [status-im.react-native.js-dependencies :as js-deps]))

(def animated (oget js-deps/react-native-reanimated "default"))
(def createAnimatedComponent (oget animated "createAnimatedComponent"))

(def view (reagent/adapt-react-class (oget animated "View")))
(def clock-running (oget js-deps/react-native-reanimated "clockRunning"))
(def Easing (oget js-deps/react-native-reanimated "Easing"))

(def eq (oget animated "eq"))
(def neq (oget animated "neq"))
(def not* (oget animated "not"))
(def or* (oget animated "or"))
(def and* (oget animated "and"))

(def set (oget animated "set"))
(def start-clock (oget animated "startClock"))
(def stop-clock (oget animated "stopClock"))

(def bezier (oget Easing "bezier"))

(def Value (oget animated "Value"))

(defn value [x]
  (new Value x))

(def Clock (oget animated "Clock"))

(defn clock []
  (new Clock))

(def debug (oget animated "debug"))
(def log (oget animated "log"))

(defn event [config]
  (ocall animated "event" (clj->js config)))

(defn cond* [condition block]
  (ocall animated "cond"
         condition
         (if (vector? block)
           (clj->js block)
           block)))

(defn block [opts]
  (ocall animated "block" (clj->js opts)))

(defn interpolate [anim-value config]
  (ocall anim-value "interpolate" (clj->js config)))

(defn call* [args callback]
  (ocall animated "call" (clj->js args) callback))

(defn timing [clock opts config]
  (ocall animated "timing" clock (clj->js opts) (clj->js config)))

;; Gesture handler

(def tap-gesture-handler (reagent/adapt-react-class (oget js-deps/react-native-gesture-handler "TapGestureHandler")))
(def pure-native-button (oget js-deps/react-native-gesture-handler "PureNativeButton"))
(def createNativeWrapper (oget js-deps/react-native-gesture-handler "createNativeWrapper"))

(def animated-raw-button (reagent/adapt-react-class
                          (createNativeWrapper
                           (createAnimatedComponent pure-native-button)
                           #js {:shouldActivateOnStart   true
                                :shouldCancelWhenOutside true})))

(def state (oget js-deps/react-native-gesture-handler "State"))

(def states {:began        (oget state "BEGAN")
             :active       (oget state "ACTIVE")
             :cancelled    (oget state "CANCELLED")
             :end          (oget state "END")
             :failed       (oget state "FAILED")
             :undetermined (oget state "UNDETERMINED")})
