(ns vtakt-server.core
  (:require [reitit.ring :as ring]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.util.response :refer [response content-type]]
            [ring.adapter.jetty9 :as jetty]
            [ring.websocket :as ringws]
            [ring.websocket.protocols :as ws]
            [clojure.core.async :as a :refer [thread <!!]]
            )
  ;;(:use overtone.live)
  (:gen-class))

(defn keep-alive [socket]
  (thread
    (while (ws/-open? socket)
      (<!! (a/timeout 1000))
      (ws/-ping socket nil))))

(defn ws-handler [upgrade-request]
  {:ring.websocket/listener
   {:on-open (fn on-connect [ws]
               (println "connect" (:headers upgrade-request))
               (keep-alive ws))
    :on-message (fn on-text [ws text-message]
                  (println "received msg:" text-message)
                  (ringws/send ws (str "echo: " text-message)))
    :on-close (fn on-close [_ status-code reason]
                (println "closed" status-code reason))
    :on-pong (fn on-pong [_ _]
               (println "pong"))
    :on-error (fn on-error [_ throwable]
                (.printStackTrace throwable)
                (println (.getMessage throwable)))}})

(def routes [["/" {:name :root}]
             ["/faq" {:name :faq}]
             ["/about" {:name :about}]
             ["/wsck" {:name :websocket}]
             ["/support" {:name :support}]])

(defn bidi-routes [_]
  (let [index (fn [req] (if (jetty/ws-upgrade-request? req)
                          (ws-handler req)
                          (-> (str "lol")
                              response
                              (content-type "text/html"))))]
    (into [] (for [[r1 r2] routes] [r1 (assoc r2 :get index)]))))

(defn default-handler [_]
  (ring/routes
   (ring/create-resource-handler {:path "/" :root ""})
   (ring/create-default-handler)))

(def middleware {:middleware [[wrap-defaults site-defaults]]})




(comment
;; "modulator" is just a defined frequency
;; you have two envs, pretty simple... linear envelops defined programatically.
;; The "out-bus" is basically your audio channel: 0 = l, 1 = r
(defsynth fm [tuning 440 a-ratio 1.0 b1-ratio 1.0 b2-ratio 1.0 c-ratio 1.0 depth 1.0 out-bus 0]
  (let [modulator1 (/ (* a-ratio tuning) b1-ratio)
        modulator2 (/ (* b2-ratio tuning) c-ratio)

        mod-env   (env-gen (lin 1 0 6))
        amp-env   (env-gen (lin 1 1 5) :action FREE)]
    (out 0 (pan2 (* 0.25 amp-env
                    (+
                     (sin-osc (* mod-env  (* tuning depth) (sin-osc modulator1)))
                     (sin-osc (* mod-env  (* tuning depth) (sin-osc modulator2)))
                       )

                    )))))

(fm 440
    ((patch-params :ratios) :a-ratio)
    ((patch-params :ratios) :b1-ratio)
    ((patch-params :ratios) :b2-ratio)
    ((patch-params :ratios) :c-ratio)
    )

((patch-params :ratios) :b2-ratio)
((patch-params :ratios) :c-ratio)

(fm 440 ((patch-params :ratios) :c-ratio))

(def patch-params {:ratios {;; Operators - DT is a 4op synth, and it has the ability to define ratios to second decimal
                            ;; point precision. There happens to be a VCA controlled by an AD (variable end level) (which
                            ;; can be set to gate).
                            ;; that can control the level of "a" and "b" operators in the algorithm. 
                            :a-ratio 1.00
                            :b1-ratio 2.00
                            :b2-ratio 2.50
                            :c-ratio 4.00}

                   :envelopes {;; Controls the attack envelope of the a1,b1,b2 operators and the filter.
                               ;; 0-127
                               :a-attack 55
                               :b-attack 55
                               :filter-attack 55
                               :amplitude-attack 65

                               ;; Controls the decay envelope of the a1,b1,b2 operators and the filter.
                               ;; 0-127
                               :a-decay 55
                               :b-decay 55
                               :filter-decay 55
                               :amplitude-decay 55

                               ;; Controls the final level after the decay has complete of the a1 and b1,b2 operators
                               ;; respectively.
                               ;; 0-127
                               :a-end 55
                               :b-end 55

                               ;; Controls global level of the a1 and b1,b2 operators respectively.
                               ;; 0-127
                               :a-level 55
                               :b-level 55

                               ;; Defines the sustain level for the filter
                               :filter-sustain 55
                               :amplitude-sustain 55

                               ;; Defines the filter release time
                               :filter-release 55
                               :amplitude-release 55}

                   :filter {;; The cut-off point of the filter
                            ;; 0.00 -> 127.00 (2 decimal points of precision)
                            :frequency 23.00

                            ;; The resonance of the filter
                            ;; 0.00 -> 127.00 (2 decimal points of precision)
                            :resonance 125.00

                            ;; Defines if the envelope that is directed towards this filter affects it in a
                            ;; positve manner (increases the cutoff) or a negative manner (decreases the cutoff)
                            ;; -64.00->63.00 (2 decimal points of precision)
                            :envelope-polarity 3.00

                            ;; Defines what type of filter this is. Can be: :off, :lp2 (2 pole lp), :hp (high pass),
                            ;; :lp4 ( 4 pole lp)
                            :filter-setting :lp4

                            ;; Defines the amount of delay until the filter takes affect
                            ;; 0->127
                            :envelope-delay 55

                            ;; Base-width filter is basically a bandpass with no ability to be modulated by envelope.
                            ;; base = the high pass portion. wdth = the low pass portion. These are both 0->127.
                            :base 55
                            :width 55}

                   :amplitude {;; The base level of the amplitude: basically a master volume
                               ;; 0->127
                               :level 55}

;; This is a global selection for the patch that modifies the wave-form of the relevant
                   ;; operators: - values cause the c operator to trend towards a texas wave, + values
                   ;; cause the a and b operators to trend towards a texas wave. In-between, you get saw,
                   ;; squares, and formant waves.
                   ;; -26.00->26.00 (two decimals of precision)
                   :harm -100

                   ;; Detunes the individual operators against each other. NOT A DETUNE across voices!
                   ;; 0->127
                   :detune 0

                   ;; Algorithm dependent: certain operators will have, depending on the algorithm, a feedback
                   ;; loop - you will find on DTs algorithms that it will always be only 1 operator per algorithm.
                   ;; 0->127
                   :feedback 0

                   ;; Again, dependent on the algorithm. Compared to a DX7 where carriers have a level that you define,
                   ;; and that defines the level of your output, DT has an extra way of abstracting mixing of operators:
                   ;; this is that abstraction: negative values will shift the output to operators on an "x" channel,
                   ;; whereas positive values will shift the output to operators on a "y" channel.
                   ;; -64 -> 63
                   :xymix 63})

)
