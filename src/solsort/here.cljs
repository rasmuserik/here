(ns solsort.here
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop alt!]]
   [reagent.ratom :as ratom :refer  [reaction]]
   [clojure.string :as string])
  (:require
   [solsort.toolbox.setup]
   [solsort.toolbox.appdb :refer [db db!]]
   [solsort.util
    :refer
    [<ajax <seq<! js-seq load-style! put!close!
     parse-json-or-nil log page-ready render dom->clj]]
   [reagent.core :as reagent :refer []]
   [cljs.core.async :refer [>! <! chan put! take! timeout close! pipe]]
   [solsort.toolbox.leaflet :refer [openstreetmap]]))

(defn url []
  (str "https://here.solsort.com/?"
       (clojure.string/join ":" (conj (db [:marker-pos]) (db [:map :zoom])))))

(defn pin [pos]
  (let [zoom (db [:map :zoom])]
    (js/history.pushState
     nil nil
     (str "?" (clojure.string/join ":" (conj pos zoom))))
    (db! [:marker-pos] pos)))

(defn handle-gps [o]
  (let [coords (aget o "coords")
        pos [(aget coords "latitude") (aget coords "longitude")]]
    (when coords
      (db! [:map :zoom] 13)
      (pin pos)
      (db! [:map :pos] pos))))
(defn gps [] (js/navigator.geolocation.getCurrentPosition handle-gps))

(def url-pos (js->clj (.split (.slice js/location.search 1) ":")))
(when (= url-pos [""])
  (def url-pos ["55" "10" "3"])
  (js/setTimeout gps 100))

(db! [:marker-pos] (subvec url-pos 0 2))

(aset js/window "onpopstate"
      (fn []
       (let [[lat lng zoom] (js->clj (.split (.slice js/location.search 1) ":"))]
         (db! [:map :zoom] zoom)
         (db! [:map :pos] [lat lng])
         (db! [:marker-pos] [lat lng]))))
(def button-style {:display :inline-block
                   :vertical-align :middle
                   :border-radius 8
                   :padding 5
                   :height 28
                   :box-shadow "1px 1px 3px black"
                   :text-align "center"
                   :text-decoration :none
                   :color :black
                   :margin 3
                   :background "rgba(255,255,255,0.9)"})

(defn app []
  [:div
  [openstreetmap
   {:db [:map]
    :on-click #(pin (:pos %))
    :pos0 (subvec url-pos 0 2)
    :zoom0 (nth url-pos 2)
    :style {:position :absolute
            :top 0 :left 0
            :z-index 0
            :height "100%" :width "100%"}}
   [:marker {:pos (db [:marker-pos])}]]
   [:div {:style
          {:position :absolute
           :font-size 12
           :font-family "sans-serif"
           ;:text-shadow "0px 0px 3px white"
           :bottom 0}}

    (and (db [:marker-pos]) "")
    [:span {:style
            {
             :display :inline-block
             :box-shadow "0px 0px 5px white"
             :text-align "center"
             :border-radius 20
             :color :black
             :margin 3
             :background "rgba(255,255,255,0.7)"

            }}
     "Click to set the marker."] [:br]
    [:span {:style button-style
            :on-click gps}
     "Mark my" [:br] "location"]
    [:span {:style button-style
            :on-click #(db! [:map :pos] (db [:marker-pos]))}
     "Center" [:br] "marker"]
    [:a {:style button-style
         :href (str "geo:" (clojure.string/join "," (db [:marker-pos])))} "Open" [:br] "in map"]
    [:a {:style button-style
         :href (str "mailto:?subject=here&body=" (url))} "Share" [:br] "as email"]
    [:a {:style button-style
         :href (str "sms:?&body=Here " (url))} "Share" [:br] "as sms"]
    ;[:span.button "GPS"]
    ]]
  )
(render [app])
