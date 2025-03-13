(ns tennis-app.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [tennis-app.core :refer [->GameScore ->TieBreakScore ->SetScore ->MatchScore
                                     add-point complete? display-score tie-break?
                                     init-match play-match new-match]]))

(deftest test-game-score
  (testing "GameScore add-point"
    (let [score (->GameScore {"player 1" 0 "player 2" 0})]
      (is (.equals {:score {"player 1" 1 "player 2" 0}} (add-point score ["player 1" "player 2"] "player 1"))
          "Simple add score")
      (is (.equals {:score {"player 1" 0 "player 2" 1}} (add-point score nil "player 2"))
          "players vector is ignored")
      (is (.equals {:score {"player 1" 0 "player 2" 0}} (add-point score ["player 1" "player 2"] "player 3"))
          "Ignore incorrect player point")
      (is (.equals {:score {"player 1" 2 "player 2" 1}}
                   (-> score
                       (add-point ["player 1" "player 2"] "player 1")
                       (add-point ["player 1" "player 2"] "player 1")
                       (add-point ["player 1" "player 2"] "player 2")))
          "Successive adds are accumulated")))

  (testing "GameScore complete?"
    (is (false? (complete? (->GameScore {"player 1" 0 "player 2" 0})))
        "Simple not complete")
    (is (false? (complete? (->GameScore {"player 1" 3 "player 2" 0})))
        "Not complete under 4 points")
    (is (false? (complete? (->GameScore {"player 1" 4 "player 2" 3})))
        "Not complete when difference under 2")
    (is (false? (complete? (->GameScore {"player 1" 5 "player 2" 4})))
        "Not complete when at advantage")
    (is (true? (complete? (->GameScore {"player 1" 4 "player 2" 2})))
        "Complete when game won no advantage")
    (is (true? (complete? (->GameScore {"player 1" 3 "player 2" 5})))
        "Complete when game won after advantage"))

  (testing "GameScore display-score"
    (is (nil? (display-score (->GameScore {"player 1" 0 "player 2" 0}) ["player 1" "player 2"]))
        "No score when 0 all")
    (is (= "15-30" (display-score (->GameScore {"player 1" 1 "player 2" 2}) ["player 1" "player 2"]))
        "Display game scores")
    (is (= "40-15" (display-score (->GameScore {"player 1" 3 "player 2" 1}) ["player 1" "player 2"]))
        "Display game scores")
    (is (= "Deuce" (display-score (->GameScore {"player 1" 3 "player 2" 3}) ["player 1" "player 2"]))
        "Display deuce")
    (is (= "Deuce" (display-score (->GameScore {"player 1" 4 "player 2" 4}) ["player 1" "player 2"]))
        "Display deuce")
    (is (= "Advantage player 1" (display-score (->GameScore {"player 1" 4 "player 2" 3}) ["player 1" "player 2"]))
        "Display player 1 advantage at 4-3")
    (is (= "Advantage player 2" (display-score (->GameScore {"player 1" 8 "player 2" 9}) ["player 1" "player 2"]))
        "Display player 2 advantage at higher score")))

(deftest test-tiebreak-score
  (testing "TieBreakScore add-point"
    (let [score (->TieBreakScore {"player 1" 0 "player 2" 0})]
      (is (.equals {:score {"player 1" 1 "player 2" 0}} (add-point score ["player 1" "player 2"] "player 1"))
          "Simple add score")
      (is (.equals {:score {"player 1" 0 "player 2" 1}} (add-point score nil "player 2"))
          "players vector is ignored")
      (is (.equals {:score {"player 1" 0 "player 2" 0}} (add-point score ["player 1" "player 2"] "player 3"))
          "Ignore incorrect player point")
      (is (.equals {:score {"player 1" 2 "player 2" 1}}
                   (-> score
                       (add-point ["player 1" "player 2"] "player 1")
                       (add-point ["player 1" "player 2"] "player 1")
                       (add-point ["player 1" "player 2"] "player 2")))
          "Successive adds are accumulated")))

  (testing "TieBreakScore complete?"
    (is (false? (complete? (->TieBreakScore {"player 1" 0 "player 2" 0})))
        "Simple not complete")
    (is (false? (complete? (->TieBreakScore {"player 1" 6 "player 2" 0})))
        "Not complete under 7 points")
    (is (false? (complete? (->TieBreakScore {"player 1" 9 "player 2" 8})))
        "Not complete when difference under 2")
    (is (false? (complete? (->TieBreakScore {"player 1" 6 "player 2" 7})))
        "Not complete when at advantage")
    (is (false? (complete? (->TieBreakScore {"player 1" 8 "player 2" 7})))
        "Not complete when at advantage")
    (is (true? (complete? (->TieBreakScore {"player 1" 10 "player 2" 8})))
        "Complete when game won no advantage")
    (is (true? (complete? (->TieBreakScore {"player 1" 7 "player 2" 5})))
        "Complete when game won after advantage"))

  (testing "TieBreakScore display-score"
    (is (nil? (display-score (->TieBreakScore {"player 1" 0 "player 2" 0}) ["player 1" "player 2"]))
        "No score when 0 all")
    (is (= "1-2" (display-score (->TieBreakScore {"player 1" 1 "player 2" 2}) ["player 1" "player 2"]))
        "Display game scores")
    (is (= "10-8" (display-score (->TieBreakScore {"player 1" 10 "player 2" 8}) ["player 1" "player 2"]))
        "Display game scores")))

(deftest test-set-score
  (let [make-game-score (fn [s1 s2] (->SetScore (zipmap ["player 1" "player 2"] s1)
                                                (->GameScore (zipmap ["player 1" "player 2"] s2))))
        make-tb-score (fn [s1 s2] (->SetScore (zipmap ["player 1" "player 2"] s1)
                                              (->TieBreakScore (zipmap ["player 1" "player 2"] s2))))]
    (testing "Test test helpers"
      (is (.equals {:score {"player 1" 0 "player 2" 0}
                    :game  {:score {"player 1" 0 "player 2" 0}}} (make-game-score [0 0] [0 0]))
          "Test test helpers")
      (is (.equals {:score {"player 1" 0 "player 2" 0}
                    :game  {:score {"player 1" 0 "player 2" 0}}} (make-tb-score [0 0] [0 0]))
          "Test test helpers"))

    (testing "SetScore tie break logic"
      (is (false? (tie-break? (make-game-score [0 0] [0 0]))))
      (is (false? (tie-break? (make-game-score [3 2] [0 0]))))
      (is (false? (tie-break? (make-game-score [6 4] [0 0]))))
      (is (true? (tie-break? (make-game-score [6 6] [0 0]))))
      (is (false? (tie-break? (make-game-score [7 6] [0 0]))))
      (is (false? (tie-break? (make-game-score [8 6] [0 0])))))

    (testing "SetScore add-point"
      (is (.equals {:score {"player 1" 0 "player 2" 0}
                    :game  {:score {"player 1" 1 "player 2" 0}}} (add-point (make-game-score [0 0] [0 0]) ["player 1" "player 2"] "player 1"))
          "Point is added to game within")
      (is (.equals {:score {"player 1" 1 "player 2" 0}
                    :game  {:score {"player 1" 0 "player 2" 0}}} (add-point (make-game-score [0 0] [4 3]) ["player 1" "player 2"] "player 1"))
          "Winner of game wins set point")
      (is (.equals {:score {"player 1" 0 "player 2" 1}
                    :game  {:score {"player 1" 0 "player 2" 0}}} (add-point (make-game-score [0 0] [4 5]) ["player 1" "player 2"] "player 2"))
          "Player 2 wins set point")
      (is (.equals {:score {"player 1" 6 "player 2" 6}
                    :game  {:score {"player 1" 0 "player 2" 0}}} (add-point (make-game-score [6 5] [4 5]) ["player 1" "player 2"] "player 2"))
          "Player 2 wins game point forcing tiebreak")
      (is (= tennis_app.core.TieBreakScore (type (:game (add-point (make-game-score [6 5] [4 5]) ["player 1" "player 2"] "player 2"))))
          "Forced tiebreak results in correct game type"))

    (testing "SetScore complete?"
      (is (false? (complete? (make-game-score [0 0] [0 0])))
          "Not complete")
      (is (false? (complete? (make-game-score [5 3] [0 0])))
          "Less than 6 games won")
      (is (false? (complete? (make-game-score [6 6] [0 0])))
          "6 games won each")
      (is (false? (complete? (make-game-score [7 6] [0 0])))
          "Tiebreak 1 point ahead")
      (is (true? (complete? (make-game-score [6 4] [0 0])))
          "Player 1 won by 2 points")
      (is (true? (complete? (make-game-score [2 6] [0 0])))
          "Player 2 won by 4 points")
      (is (false? (complete? (make-game-score [7 6] [0 0])))
          "No winner if incorrect game score")
      (is (false? (complete? (make-tb-score [7 6] [5 4])))
          "No winner if tiebreak not yet won")
      (is (true? (complete? (make-tb-score [7 6] [7 5])))
          "Player 1 wins tie break")
      (is (true? (complete? (make-tb-score [6 7] [42 45])))
          "Player 2 wins marathon tie break"))

    (testing "SetScore display-score"
      (is (= "0-0" (display-score (make-game-score [0 0] [0 0]) ["player 1" "player 2"]))
          "Zero score stills display score")
      (is (= "4-5" (display-score (make-game-score [4 5] [0 0]) ["player 1" "player 2"]))
          "Score is displayed")
      (is (= "5-4" (display-score (make-game-score [5 4] [0 0]) ["player 1" "player 2"]))
          "Score order is preserved")
      (is (= "5-4" (display-score (make-game-score [4 5] [0 0]) ["player 2" "player 1"]))
          "Reversing player order reverses score order"))))

(deftest test-match-score
  (let [make-score (fn [s1 s2 s3]
                     (->MatchScore (zipmap ["player 1" "player 2"] s1)
                                   (->SetScore (zipmap ["player 1" "player 2"] s2)
                                               (->GameScore (zipmap ["player 1" "player 2"] s3)))))]
    (testing "Test test helpers"
      (is (.equals {:score {"player 1" 0 "player 2" 0}
                    :set   {:score {"player 1" 0 "player 2" 0}
                            :game  {:score {"player 1" 0 "player 2" 0}}}} (make-score [0 0] [0 0] [0 0]))
          "Test test helpers"))

    (testing "MatchScore add-point"
      (is (.equals {:score {"player 1" 0 "player 2" 0}
                    :set   {:score {"player 1" 0 "player 2" 0}
                            :game  {:score {"player 1" 1 "player 2" 0}}}} (add-point (make-score [0 0] [0 0] [0 0]) ["player 1" "player 2"] "player 1"))
          "Point is added to game within")
      (is (.equals {:score {"player 1" 0 "player 2" 0}
                    :set   {:score {"player 1" 1 "player 2" 0}
                            :game  {:score {"player 1" 0 "player 2" 0}}}} (add-point (make-score [0 0] [0 0] [3 2]) ["player 1" "player 2"] "player 1"))
          "Player 1 wins a set")
      (is (.equals {:score {"player 1" 0 "player 2" 1}
                    :set   {:score {"player 1" 4 "player 2" 6}
                            :game  {:score {"player 1" 0 "player 2" 0}}}} (add-point (make-score [0 0] [4 5] [4 5]) ["player 1" "player 2"] "player 2"))
          "Player 2 wins a match"))

    (testing "MatchScore complete?"
      (is (false? (complete? (make-score [0 0] [0 0] [0 0])))
          "No points scored")
      (is (false? (complete? (make-score [0 0] [0 0] [4 2])))
          "Only game points scored")
      (is (false? (complete? (make-score [0 0] [4 5] [0 0])))
          "Only set points scored")
      (is (true? (complete? (make-score [1 0] [4 5] [100 -1])))
          "Single match point scored"))

    (testing "MatchScore display-score"
      (is (= "4-5" (display-score (make-score [0 0] [4 5] [0 0]) ["player 1" "player 2"]))
          "No game score displayed when 0 game points")
      (is (= "6-6, Advantage player 1" (display-score (make-score [0 0] [6 6] [4 3]) ["player 1" "player 2"]))
          "Normal score"))))

(deftest test-play-match
  (testing "init-match"
    (is (.equals {:score {"harry" 0 "pete" 0}
                  :set   {:score {"harry" 0 "pete" 0}
                          :game  {:score {"harry" 0 "pete" 0}}}} (init-match ["harry" "pete"]))))

  (testing "play-match"
    (is (= "0-0" (play-match ["p1" "p2"] []))
        "Initial score is zero")
    (is (= "1-0" (play-match ["p1" "p2"] ["p1" "p1" "p1" "p1"]))
        "Player 1 wins game")
    (is (= "0-0, Advantage Norris" (play-match ["Boris" "Norris"]
                                               ["Boris" "Norris" "Norris" "Boris" "Norris" "Boris" "Norris"]))
        "Player 2 has advantage")
    (is (= "6-0" (play-match ["player 1" "player 2"] (repeat 100 "player 1")))
        "Player 1 wins match"))

  (testing "new-match"
    (let [{:keys [score]} (new-match ["player 1" "player 2"])]
      (is (= "0-0" (score))
          "Initial score is zero"))

    (let [{:keys [point-won-by score]} (new-match ["player 1" "player 2"])]
      (is (= "0-0, 30-0" (do (point-won-by "player 1")
                             (point-won-by "player 1")
                             (score)))
          "Successive adds are cumulative"))

    (let [{:keys [point-won-by score]} (new-match ["George" "Foreman"])]
      (is (= "0-0, 30-15" (do (point-won-by "George")
                              (point-won-by "Foreman")
                              (point-won-by "George")
                              (score)))
          "Player names are matched"))))

