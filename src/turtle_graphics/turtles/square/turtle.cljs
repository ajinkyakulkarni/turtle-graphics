(ns turtle-graphics.turtles.square.turtle
  "square turtle implementation"
  (:require [complex.number :as n]
            [cljs.core.match :refer-macros [match]]))

(defprotocol simple-turtle
  "turtle commands"
  (move [_ d])
  (forward [_ d])
  (left [_])
  (right [_])
  (circle [_ style])
  (point [_ style]))

(defrecord turtle [position heading]
  simple-turtle
  (move [t d]
    ;; return new turtle with updated position
    [t nil])
  (forward [t d]
    ;; return updated turtle and
    ;; an svg move command
    [t [:M :position]]))

(def initial-turtle (turtle. n/zero n/one))

;; thread a sequence of commands to a turtle
;; reduce a program given an initial turtle
;; collecting svg paths as you go
