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
;; * (def trio (-main)) has been run


;; icard low-level tests

#_(against-background
  
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

; ===============================================

; slip low-level tests

;.;. Code you'd be proud to give your mom to show off on the fridge. --
;.;. Mike Cohn
(against-background
    
  [(before :contents
           (doall
            (db/SYSsetup-InfWb "brain" "test")
            (db/SYSload "four-notecards")))
   
   (around :facts
           (let [IGNORE       (db/SYSclear-all) ; new test; clear local data
                 test-icard   (nth (db/get-all-icards) 0)
                 test-icdata  (db/localDB->icdata test-icard)
                 test-btxt    (db/icdata-field test-icdata :btxt)
                 test-ttxt    (db/icdata-field test-icdata :ttxt)
                 test-sldata  (db/new-sldata test-icard)
                 test-slip    (nth (db/get-all-slips) 0)
                 ]
             ?form ))

   (after :contents
          (db/SYSdrop "four-notecards"))
   ]

  
  (fact
    ; FCNS TESTED: get-all-icards, localDB->icdata, icdata-field,
    ;    get-all-slips, SYSsldata-field
    (count (db/get-all-slips)) => 1
    (db/SYSsldata-field test-sldata :slip) => test-slip
    (db/SYSsldata-field test-sldata :icard) => test-icard
    (db/SYSsldata-field test-sldata :btxt) => test-btxt
    (db/SYSsldata-field test-sldata :ttxt) => test-ttxt
    )

    
  )   ; close "against-background"

