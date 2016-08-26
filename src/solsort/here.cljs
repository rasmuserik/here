(ns solsort.here
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]]
   [reagent.ratom :as ratom :refer  [reaction]]
   [clojure.string :as string])
  (:require
   ;[solsort.toolbox.setup]
   [solsort.toolbox.appdb :refer [db db!]]
   [solsort.util
    :refer
    [<ajax <seq<! js-seq load-style! put!close!
     parse-json-or-nil log page-ready render dom->clj]]
   [reagent.core :as reagent :refer []]
   [cljs.core.async :refer [>! <! chan put! take! timeout close! pipe]]
   [solsort.toolbox.leaflet :refer [openstreetmap]]))

(defn pin [pos]
  (let [zoom (db [:map :zoom])]
    (aset js/location "hash" (clojure.string/join ":" (conj pos zoom)))
    (db! [:marker-pos] pos)
    (db! [:map :pos] pos)
    ))
(def hash-pos (js->clj (.split (.slice js/location.hash 1) ":")))
(when (= hash-pos [""])
  (def hash-pos ["55" "10" "3"]))

(db! [:marker-pos] (subvec hash-pos 0 2))
(defn app []
  (log 'app)
  [:div
  [openstreetmap
   {:db [:map]
    :on-click #(pin (:pos %))
    :pos0 (subvec hash-pos 0 2)
    :zoom0 (nth hash-pos 2)
    :style {:position :absolute
            :top 0 :left 0
            :height "100%" :width "100%"}}
   [:marker
    {:pos (db [:marker-pos])}]]
   [:div {:style
          {:position :absolute
           :font-family "sans-serif"
           :text-shadow "0px 0px 3px white"
           :bottom 0}}
    "Share location with a link." [:br]
    ;[:span.button "GPS"]
    ]]
  )
(render [app])
