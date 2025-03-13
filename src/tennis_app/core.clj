(ns tennis-app.core
  (:require [clojure.set :refer [difference]]
            [medley.core :refer [update-existing-in]])
  (:gen-class))

(defprotocol Score
  "Score interface for tennis match scoring."

  (add-point [this players point]
    "Adds a point scored by a player. Requires the list of players to record this.
     eg. (add-point score [\"player 1\" \"player 2\"] \"player 1\")")

  (complete? [this]
    "Is this score component complete?")

  (display-score [this players]
    "Returns the score component as a string, or nil if no score available."))


(defrecord GameScore [score]
  Score

  (add-point [this _ point]
    (update-existing-in this [:score point] inc))

  (complete? [this]
    (let [[s1 s2] (sort (vals (:score this)))]
      (and (>= s2 4)
           (>= (- s2 s1) 2))))

  (display-score [this players]
    (let [p1-score (get-in this [:score (first players)])
          p2-score (get-in this [:score (second players)])
          pts-score {0 0, 1 15, 2 30, 3 40}]
      (cond
        (= p1-score p2-score 0) nil
        (or (< p1-score 3)
            (< p2-score 3)) (format "%d-%d" (pts-score p1-score) (pts-score p2-score))
        (> p1-score p2-score) (str "Advantage " (first players))
        (< p1-score p2-score) (str "Advantage " (second players))
        :else "Deuce"))))


(defrecord TieBreakScore [score]
  Score

  (add-point [this _ point]
    (update-existing-in this [:score point] inc))

  (complete? [this]
    (let [[s1 s2] (sort (vals (:score this)))]
      (and (>= s2 7)
           (>= (- s2 s1) 2))))

  (display-score [this players]
    (let [p1-score (get-in this [:score (first players)])
          p2-score (get-in this [:score (second players)])]
      (if (= p1-score p2-score 0)
        nil
        (format "%d-%d" p1-score p2-score)))))


(defn tie-break?
  "Checks if a set score should trigger a tiebreak game."
  [set]
  (let [[s1 s2] (sort (vals (get set :score)))]
    (= s1 s2 6)))


(defrecord SetScore [score game]
  Score

  (add-point [this players point]
    (let [this (update this :game add-point players point)]
      (if (complete? (:game this))
        (let [this (update-existing-in this [:score point] inc)
              new-fn (if (tie-break? this) ->TieBreakScore ->GameScore)]
          ;; game is complete so start a new one
          (assoc this :game (new-fn (zipmap players (repeat 0)))))
        this)))

  (complete? [this]
    (let [[s1 s2] (sort (vals (:score this)))]
      (or (and (>= s2 6)
               (>= (- s2 s1) 2))
          (and (= s2 7)
               (complete? (:game this))))))

  (display-score [this players]
    (format "%d-%d"
            (get-in this [:score (first players)])
            (get-in this [:score (second players)]))))


(defrecord MatchScore [score set]
  Score

  (add-point [this players point]
    (let [this (update this :set add-point players point)]
      (if (complete? (:set this))
        (update-in this [:score point] inc)
        this)))

  (complete? [this]
    (not-every? zero? (vals (:score this))))

  (display-score [this players]
    (if-let [game-score (display-score (get-in this [:set :game]) players)]
      (format "%s, %s" (display-score (get this :set) players) game-score)
      (display-score (get this :set) players))))


(defn init-match
  "Returns a match score initialised to 0 for all players."
  [players]
  (let [zero (zipmap players (repeat 0))]
    (->MatchScore zero (->SetScore zero (->GameScore zero)))))


(defn play-match
  "Plays out a match and returns the final score, either when the match finishes
   or the points run out. eg.

     (play-match [\"player 1\" \"player 2\"]
                 [\"player 1\" \"player 1\" \"player 2\" \"player 1\" \"player 2\" \"player 2\" \"player 1\"])

   \"player 1\" leads the first game \"0-0, Advantage player 1\"."
  ([players points] (play-match players points (init-match players)))
  ([players points score]
   (assert (= (count players) 2) "No more than two players allowed in a match")
   (assert (every? (set players) points) (str "One or more points won by player not in the match "
                                              (difference (set points) (set players))))

   (loop [points' points
          score' score]
     (if (and (seq points') (not (complete? score')))
       (recur (rest points')
              (add-point score' players (first points')))
       (display-score score' players)))))


(defn new-match
  "Implement (close to) suggested interface. Wraps some functions around a 'match'
  atom initialised with 'players'. Returns 'point-won-by' and 'score' functions."
  [players]
  (let [match (atom (init-match players))]
    {:point-won-by (fn [player] (swap! match add-point players player))
     :score        (fn [] (display-score @match players))}))


(defn -main
  [& _]
  (let [{:keys [point-won-by score]} (new-match ["player 1" "player 2"])]

    (point-won-by "player 1")
    (point-won-by "player 2")
    (println (score))

    (point-won-by "player 1")
    (point-won-by "player 1")
    (println (score))

    (point-won-by "player 2")
    (point-won-by "player 2")
    (println (score))

    (point-won-by "player 1")
    (println (score))

    (point-won-by "player 1")
    (println (score))))