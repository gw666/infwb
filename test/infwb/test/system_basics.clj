(ns infwb.test.system-basics
  (:require [infwb.core])
  (:require [infwb.sedna :as db])
  (:use clojure.test)
  (:use midje.sweet))

(fact
  (count (db/permDB->all-icards)) => 4
  (against-background
    (before :facts
            (doall
              (db/SYSsetup-InfWb "brain" "test")
              (db/SYSload "four-notecards")))
    (after :facts
           (db/SYSdrop "four-notecards"))))

