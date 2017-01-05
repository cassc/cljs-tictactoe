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
(defonce board-state (local-storage (atom {}) :board-state))
(defonce player-state (local-storage (atom {:player nil}) :player-state))

(def all-lines #{[0 1 2]
                 [3 4 5]
                 [6 7 8]
                 [0 3 6]
                 [1 4 7]
                 [2 5 8]
                 [0 4 8]
                 [2 4 6]})

(defn player []
  (:player @player-state))

(defn computer []
  (when-let [p (player)]
    (case p
      "x" "o"
      "o" "x")))

(defn computer-move! []
  (let [c (computer)
        match (fn [line] (seq (remove #(@board-state %) line)))
        lines (map match all-lines)
        ci (->> lines
                (filter identity)
                (sort ;; TODO sort should be smarter, so computer can stop human from winning
                 (fn [ra rb]
                   (- (count ra) (count rb))))
                first
                rand-nth)]
    (swap! board-state assoc ci c)))

(defn xy-of [index]
  [(quot index 3) (rem index 3)])

(defn win-with? [i k]
  (->> all-lines
       (filter (partial some (partial = i)))
       (map #(map @board-state %))
       (filter #(every? (partial = k) %))
       first))

(add-watch
 board-state
 :play-monitor
 (fn [_ _ o n]
   (let [i (first (remove o (keys n)))
         v (n i)
         player-move? (= (player) v)]
     (println "play-move?" player-move?)
     (if-let [w (win-with? i v)]
       (println v "wins" w)
       (when player-move?
         (computer-move!))))))

(defn reset-state! []
  (reset! board-state {})
  (reset! player-state {}))

(defn select [k]
  (swap! player-state assoc :player k))

(defn place-item [i]
  (when-not (@board-state i)
    (swap! board-state assoc i (player))))

(defn select-player []
  [:div.dialog
   [:div
    [:h3 "X or O?"]
    [:div
     [:button.xoro {:type :button :on-click #(select "x")} "X"]
     [:button.xoro {:type :button :on-click #(select "o")} "O"]]]])

(defn my-app []
  (fn []
    [:div
     (when-not (:player @player-state)
       [select-player])
     (when (:player @player-state)
       [:div
        [:div.top.row
         [:div.box.top-left {:on-click #(place-item 0)} (@board-state 0)]
         [:div.box.top-center {:on-click #(place-item 1)} (@board-state 1)]
         [:div.box.top-right {:on-click #(place-item 2)} (@board-state 2)]]
        [:div.middle.row
         [:div.box.middle-left {:on-click #(place-item 3)} (@board-state 3)]
         [:div.box.middle-center {:on-click #(place-item 4)}(@board-state 4)]
         [:div.box.middle-right {:on-click #(place-item 5)} (@board-state 5)]]
        [:div.bottom.row
         [:div.box.bottom-left {:on-click #(place-item 6)} (@board-state 6)]
         [:div.box.bottom-center {:on-click #(place-item 7)} (@board-state 7)]
         [:div.box.bottom-right {:on-click #(place-item 8)} (@board-state 8)]]
        [:div
         [:button.reset {:type :button :on-click #(reset-state!)} "Reset"]]])]))

(defn main []
  (reagent/render [#'my-app] (.getElementById js/document "app")))

(main)

