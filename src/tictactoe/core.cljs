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
(defonce play-stats (local-storage (atom {:player 0 :computer 0 :draw 0 :finished false}) :play-stats))
(defonce highlights (local-storage (atom #{}) :highlights))

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

(defn highlight-line [line]
  (go-loop [n 3]
    (when (pos? n)
      (reset! highlights (set line))
      (<! (timeout 400))
      (reset! highlights #{})
      (<! (timeout 200))
      (recur (dec n)))))

(defn finish-round []
  (swap! play-stats assoc :finished true))

(defn win [p line]
  (println "win with " line)
  (highlight-line line)
  (swap! play-stats update-in [(if (= (player) p) :player :computer)] inc)
  (finish-round))

(defn draw []
  (swap! play-stats update-in [:draw] inc)
  (finish-round))

(defn compare-lines [prev next]
  (let [pv (map @board-state prev)
        nv (map @board-state next)
        p (player)
        c (computer)]
    (cond
      (second (filter (partial = c) pv))
      true

      (second (filter (partial = c) nv))
      false

      (second (filter (partial = p) pv))
      true

      (second (filter (partial = p) nv))
      false
      
      :esle
      (> (count (filter (partial = c) pv))
         (count (filter (partial = c) nv))))))

(defn computer-move! []
  (let [c (computer)
        candid-lines (filter #(seq (filter (complement @board-state) %)) all-lines)
        cm (->> (sort compare-lines candid-lines)
                identity
                first
                (remove #(@board-state %))
                first)]
    (swap! board-state assoc cm c)))

(defn xy-of [index]
  [(quot index 3) (rem index 3)])

(defn win-with? [i k]
  (->> all-lines
       (filter (partial some (partial = i)))
       (map (fn [line] (map (fn [idx] [idx (@board-state idx)]) line)))
       (filter #(every? (comp (partial = k) second) %))
       first))

(defn draw? [board]
  (= (count (keys board)) 9))

(add-watch
 board-state
 :play-monitor
 (fn [_ _ o n]
   (let [i (first (remove o (keys n)))
         v (n i)
         player-move? (= (player) v)]
     (println "play-move?" player-move?)
     (if-let [w (win-with? i v)]
       (win v (map first w))
       (cond
         (draw? n)
         (draw)
         
         player-move?
         (computer-move!))))))

(defn reset-state! []
  (reset! board-state {})
  (reset! player-state {}))

(defn select [k]
  (swap! player-state assoc :player k))

(defn place-item [i]
  (when (:finished @play-stats)
    (swap! play-stats dissoc :finished)
    (reset! board-state {}))
  (when-not (@board-state i)
    (swap! board-state assoc i (player))))

(defn select-player []
  [:div.dialog
   [:div
    [:h3 "X or O?"]
    [:div
     [:button.xoro {:type :button :on-click #(select "x")} "X"]
     [:button.xoro {:type :button :on-click #(select "o")} "O"]]]])

(defn display-box []
  (fn [cls i]
    [:div.box
     {:class (str cls (when (@highlights i) " highlight"))
      :on-click #(place-item i)} [:span (@board-state i)]]))

(defn my-app []
  (fn []
    [:div
     (when-not (:player @player-state)
       [select-player])
     (when (:player @player-state)
       [:div
        [:div.top.row
         [display-box "top-left" 0]
         [display-box "top-center" 1]
         [display-box "top-right" 2]]
        [:div.middle.row
         [display-box "middle-left" 3]
         [display-box "middle-center" 4]
         [display-box "middle-right" 5]]
        [:div.bottom.row
         [display-box "bottom-left" 6]
         [display-box "bottom-center" 7]
         [display-box "bottom-right" 8]]
        [:div
         [:div.row
          [:div.player (str "player")]
          [:div.player (str "ties")]
          [:div.player (str "computer")]]
         [:div.row
          [:div.player (:player @play-stats)]
          [:div.player (:draw @play-stats)]
          [:div.player (:computer @play-stats)]]
         [:button.reset {:type :button :on-click #(reset-state!)} "Reset"]]])]))

(defn main []
  (reagent/render [#'my-app] (.getElementById js/document "app")))

(main)

