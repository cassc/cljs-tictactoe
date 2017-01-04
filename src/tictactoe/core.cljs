(ns tictactoe.core
  (:require
   [clojure.string :as s]
   [alandipert.storage-atom :refer [local-storage]]
   [reagent.core :as reagent :refer [atom]]
   [cljs.core.async :refer [<! chan sliding-buffer put! close! timeout]])
  (:require-macros
   [cljs.core.async.macros :refer [go-loop go]]))

(enable-console-print!)

;;(def prefs (local-storage (atom {}) :prefs))
(defonce board-state (local-storage (atom {0 "x" 7 "o"}) :board-state))

(defn my-app []
  (fn []
    [:div
     [:div.top.row
      [:div.box.top-left ;; (@board-state 0 "x")
       ]
      [:div.box.top-center (@board-state 1)]
      [:div.box.top-right (@board-state 2)]]
     [:div.middle.row
      [:div.box.middle-left (@board-state 3)]
      [:div.box.middle-center [:span (@board-state 4)]]
      [:div.box.middle-right (@board-state 5)]]
     [:div.bottom.row
      [:div.box.bottom-left (@board-state 6)]
      [:div.box.bottom-center (@board-state 7)]
      [:div.box.bottom-right (@board-state 8)]]]))

(defn main []
  (reagent/render [#'my-app] (.getElementById js/document "app")))

(main)

