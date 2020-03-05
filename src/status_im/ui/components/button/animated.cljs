(ns status-im.ui.components.button.animated
  (:require [reagent.core :as reagent]
            [status-im.ui.components.button.haptic :as haptic]
            [status-im.ui.components.reanimated :as reanimated]))

(def animation-states {:s0          0
                       :s1          1
                       :s2          2
                       :s3          3
                       :end-touched 7})

(defn button-press-animation
  [{:keys [animation-state duration-val finished frame-time gesture-state
           prev-gesture-state scale-value time to-value zoom-clock scale-to
           on-press on-press-start on-long-press]}]
  (reanimated/block [(reanimated/cond* (reanimated/neq prev-gesture-state gesture-state)
                                       [(reanimated/cond* (reanimated/or*
                                                           (reanimated/eq gesture-state (:active reanimated/states))
                                                           (reanimated/and* (reanimated/neq prev-gesture-state (:active reanimated/states))
                                                                            (reanimated/eq gesture-state (:undetermined reanimated/states))))
                                                          [(reanimated/set animation-state (:s0 animation-states))])
                                        (reanimated/cond* (reanimated/eq gesture-state (:end reanimated/states))
                                                          on-press)
                                        (reanimated/cond* (reanimated/eq gesture-state (:active reanimated/states))
                                                          [on-long-press
                                                           on-press-start])])
                     (reanimated/set prev-gesture-state gesture-state)
                     (reanimated/cond* (reanimated/eq animation-state (:s0 animation-states))
                                       [(reanimated/start-clock zoom-clock)
                                        (reanimated/set finished 0)
                                        (reanimated/set animation-state (:s1 animation-states))
                                        (reanimated/set frame-time 0)
                                        (reanimated/set time 0)
                                        (reanimated/set to-value scale-to)])
                     (reanimated/cond* (reanimated/and* (reanimated/eq animation-state (:s1 animation-states))
                                                        (reanimated/neq gesture-state (:active reanimated/states))
                                                        finished)
                                       [(reanimated/set finished 0)
                                        (reanimated/set animation-state (:s2 animation-states))
                                        (reanimated/set frame-time 0)
                                        (reanimated/set time 0)
                                        (reanimated/set to-value 1)])
                     (reanimated/cond* (reanimated/and* (reanimated/eq animation-state (:s2 animation-states))
                                                        finished)
                                       [(reanimated/set animation-state (:s3 animation-states))
                                        (reanimated/stop-clock zoom-clock)])
                     (reanimated/cond* (reanimated/or* (reanimated/eq animation-state (:s1 animation-states))
                                                       (reanimated/eq animation-state (:s2 animation-states)))
                                       (reanimated/timing zoom-clock
                                                          {:finished  finished
                                                           :frameTime frame-time
                                                           :position  scale-value
                                                           :time      time}
                                                          {:duration duration-val
                                                           :easing   (reanimated/bezier 0.25 0.46 0.45 0.94)
                                                           :toValue  to-value}))
                     (reanimated/cond* (reanimated/eq prev-gesture-state (:end reanimated/states))
                                       [(reanimated/set prev-gesture-state (:end-touched animation-states))
                                        (reanimated/set gesture-state (:end-touched animation-states))])
                     scale-value]))

(defn button-animation-helper
  [gesture-state prev-gesture-state zoom-clock]
  (reanimated/block [(reanimated/cond* (reanimated/and* (reanimated/eq gesture-state (:end reanimated/states))
                                                        (reanimated/eq prev-gesture-state (:end-touched animation-states))
                                                        (reanimated/not* (reanimated/clock-running zoom-clock)))
                                       (reanimated/set prev-gesture-state (:undetermined reanimated/states)))]))

(defn scale-animation
  [{:keys [animation-state duration-val finished frame-time gesture-state
           prev-gesture-state scale-to scale-value time to-value zoom-clock
           on-press on-press-start on-long-press]
    :or   {on-press-start identity
           on-long-press  identity}}]
  (reanimated/block [(button-animation-helper gesture-state prev-gesture-state zoom-clock)
                     (reanimated/cond* (reanimated/and* (reanimated/eq prev-gesture-state (:undetermined reanimated/states))
                                                        (reanimated/eq gesture-state (:end reanimated/states))
                                                        (reanimated/neq animation-state (:s0 animation-states)))
                                       (reanimated/set animation-state (:s0 animation-states)))
                     (reanimated/log animation-state)
                     (button-press-animation {:animation-state    animation-state
                                              :duration-val       duration-val
                                              :finished           finished
                                              :frame-time         frame-time
                                              :gesture-state      gesture-state
                                              :prev-gesture-state prev-gesture-state
                                              :scale-value        scale-value
                                              :time               time
                                              :to-value           to-value
                                              :zoom-clock         zoom-clock
                                              :scale-to           scale-to
                                              :on-press           (reanimated/call* [] on-press)
                                              :on-press-start     (reanimated/call* [] on-press-start)
                                              :on-long-press      (reanimated/call* [] on-long-press)})]))

(defn button []
  (let [this     (reagent/current-component)
        {:keys [duration style on-long-press on-press-start on-press
                enable-haptic-feedback? haptic-type scale-to opacity-to]
         :or   {enable-haptic-feedback? true
                scale-to                0.8
                opacity-to              1
                haptic-type             :selection}}
        (reagent/props this)
        children (reagent/children this)

        animation-state    (reanimated/value (:cancelled reanimated/states))
        duration-val       (reanimated/value duration)
        finished           (reanimated/value 0)
        frame-time         (reanimated/value 0)
        gesture-state      (reanimated/value (:undetermined reanimated/states))
        prev-gesture-state (reanimated/value (:undetermined reanimated/states))
        on-gesture-event   (reanimated/event [{:nativeEvent {:state gesture-state}}])
        scale-value        (reanimated/value 1)
        time               (reanimated/value 0)
        to-value           (reanimated/value 0.5)
        zoom-clock         (reanimated/clock)
        scale              (scale-animation (merge {:animation-state    animation-state
                                                    :duration-val       duration-val
                                                    :finished           finished
                                                    :frame-time         frame-time
                                                    :gesture-state      gesture-state
                                                    :on-press           (fn []
                                                                          (when enable-haptic-feedback?
                                                                            (haptic/trigger haptic-type))
                                                                          (on-press))
                                                    :on-gesture-event   on-gesture-event
                                                    :prev-gesture-state prev-gesture-state
                                                    :scale-to           scale-to
                                                    :scale-value        scale-value
                                                    :time               time
                                                    :to-value           to-value
                                                    :zoom-clock         zoom-clock}
                                                   (when on-long-press
                                                     {:on-long-press on-long-press})
                                                   (when on-press-start
                                                     {:on-press-start on-press-start})))]
    (into [reanimated/animated-raw-button
           {:on-handler-state-change on-gesture-event
            :style                   (merge style
                                            {:opacity   (reanimated/interpolate
                                                         scale-value
                                                         {:inputRange  (if (> scale-to 1) [1 scale-to] [scale-to 1])
                                                          :outputRange (if (> scale-to 1) [1 opacity-to] [opacity-to 1])})
                                             :transform [{:scale scale}]})}]
          children)))
