(ns infwb.test.system-basics
  (:require [infwb.core])
  (:require [infwb.sedna :as db])
  (:use clojure.test)
  (:use midje.sweet))

;; Preconditions for these tests to work:
;;
;; * You must compile core.clj
;;
;; * Sedna must have a db named "brain" and an _empty_ collection named 'test'
;;


;.;. Simplicity, carried to the extreme, becomes elegance. -- Jon Franklin
(against-background
  [(before :contents
           (doall
            (db/SYSsetup-InfWb "brain" "test")
            (db/SYSload "four-notecards")))
   (after :contents
          (db/SYSdrop "four-notecards"))]

  
  (fact
    ; FCNS TESTED: permDB->all-icards
    (count (db/permDB->all-icards)) => 4)

  
  (fact
    ; TESTS correctly reading the :ttxt field of an icdata record
    ; FCNS TESTED: get-icdata-from-permDB, icdata->localDB, localDB->icdata
    (db/icdata-field (db/localDB->icdata "gw667_090815161114586") :ttxt)
    => "the ability to think"

    ; LOCAL SETUP: create icdata record in localDB from full rcd in permDB
    (against-background
      (before :facts
              (let [icd  (db/get-icdata-from-permDB "gw667_090815161114586")
                    _    (db/icdata->localDB icd)]))))


  (fact
    ; FCNS TESTED: permDB->localDB, localDB->icdata
    (db/icdata-field (db/localDB->icdata "gw667_090815162059614") :ttxt)
    => "to label, categorize, and find precedents"

    (against-background
      (before :facts (db/permDB->localDB "gw667_090815162059614"))))


  (fact
    ; FCNS TESTED: permDB->all-icards, get-icdata-from-permDB
    (count (map db/get-icdata-from-permDB (db/permDB->all-icards))) => 4)

  
  (fact
    ; FCNS TESTED: permDB->all-icards, permDB->localDB, get-all-icards
    (count (db/get-all-icards)) => 4

    (against-background
      (before :facts
              (let [all-icards (db/permDB->all-icards)]
                (doseq [card all-icards]
                  (db/permDB->localDB card))))))
    

  
  )   ; close "against-background"

