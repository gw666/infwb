(ns infwb.test.system-basics
  (:require [infwb.core])
  (:require [infwb.sedna :as db])
  (:use clojure.test)
  (:use midje.sweet))

(db/SYSsetup-InfWb "brain" "test")
(db/SYSload "four-notecards")

(fact
  (count (db/permDB->all-icards)) => 4)

(db/SYSdrop "four-notecards")

