# tennis-app

A Clojure app implementing the DataRock Tennis programming challenge.

## Requirements

Java - https://adoptium.net/ - any LTS version (8, 11, 17 and 21)

Leiningen - https://leiningen.org/

## Usage

Run the sample app from the command line:
```
$ lein uberjar
$ java -jar target/tennis-app-0.1.0-SNAPSHOT-standalone.jar
```

Run tests:
```
$ lein test
```

From the REPL:

Use the `play-match` function to play out a whole match, given the current players and the winner of successive points. Example:
```
(play-match ["player 1" "player 2"] ["player 1" "player 1" "player 2" "player 1" "player 2" "player 2" "player 1"]) 
=> "0-0, Advantage player 1"
```

## Notes

The current implementation can be easily extended to score a match of multiple sets, by adding a history or set scores and checking the winner of each to determine the match winner (best of `n` sets).