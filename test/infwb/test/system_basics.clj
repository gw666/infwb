(ns infwb.test.system-basics
  (:require [infwb.core])
  (:require [infwb.slip-display])
  (:require [infwb.sedna :as db])

  (:import (edu.umd.cs.piccolox         PFrame)
           (edu.umd.cs.piccolo.event    PDragEventHandler))
  
  (:use seesaw.core)
  (:use clojure.test)
  (:use midje.sweet))

;; Preconditions for these tests to work:
;;
;; * You must compile core.clj, then compile this file
;;
;; * Sedna must have a db named "brain" and an _empty_ collection named 'test'
;;
;; * (def trio (-main)) has been run  (not sure about this)

;; NOTE: Many of the 'checks' (lines containing =>) in this file are
;; wrapped with a 'let' clause that does a lot of calculation. This
;; occurs because the interactive debugging package 'midje-mode' doesn't
;; seem to work properly when it comes to the 'around' clause making
;; its value available to the corresponding test. (Executing this
;; file using 'lein midje' work just fine that way.) I made these
;; changes just so I could test interactively using 'midje-mode'.


;; icard low-level tests

; test 1; compiles 120124
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

; ===============================================

; slip low-level tests

; test 2; compiles 120124
(against-background
    
    [(before :contents
             (db/SYSload "four-notecards"))
     
     (around :facts
             (let [IGNORE       (db/SYSclear-all) ; new test; clear local data
                   IGNORE       (db/SYSsetup-InfWb "brain" "test") ;more setup
                   
                   ]
               ?form ))

     (after :contents
            (db/SYSdrop "four-notecards"))
     ]

    
    (fact
; FCNS TESTED: get-all-icards, localDB->icdata, icdata-field,
;    get-all-slips, SYSsldata-field
      (let [test-icard   (nth (db/get-all-icards) 0)
            test-icdata  (db/localDB->icdata test-icard)
            test-btxt    (db/icdata-field test-icdata :btxt)
            test-ttxt    (db/icdata-field test-icdata :ttxt)
            test-sldata  (db/new-sldata test-icard)
            test-slip    (nth (db/get-all-slips) 0)]
        (count (db/get-all-slips)) => 1
        (db/SYSsldata-field test-sldata :icard) => test-icard
        (db/SYSsldata-field test-sldata :btxt) => test-btxt
        (db/SYSsldata-field test-sldata :ttxt) => test-ttxt
        (db/SYSsldata-field test-sldata :slip) => test-slip
        ))

    )   ; close "against-background"

; ------------------------------------------------------

; test 3; compiles 120124
(against-background
    
    [(before :contents
             (doall
              (db/SYSsetup-InfWb "brain" "test")
              (db/SYSload "four-notecards"))
             )
     
     (after :contents
            (db/SYSdrop "four-notecards"))
     ]

    
    (fact
; FCNS TESTED: move-to, get-sldata, round-to-int, SYSsldata-field
      (let [IGNORE       (db/SYSclear-all) ; new test; clear local data
            test-icard   (nth (db/get-all-icards) 0)
            test-icdata  (db/localDB->icdata test-icard)
            test-sldata  (db/new-sldata test-icard)
            test-slip    (nth (db/get-all-slips) 0)
            new-x         62
            new-y        118
            IGNORE       (db/move-to test-slip new-x new-y)
            new-sldata   (db/get-sldata test-slip)]
        (db/round-to-int (db/SYSsldata-field new-sldata :x)) => new-x
        (db/round-to-int (db/SYSsldata-field new-sldata :y)) => new-y ))
    
    )  ; close "against-background"

; ------------------------------------------------------

; test 4; compiles 120124
(against-background
    
    [(before :contents
             (doall
              (db/SYSsetup-InfWb "brain" "test")
              (db/SYSload "four-notecards")))
     
     (around :facts
             (let [IGNORE       (db/SYSclear-all) ; new test; clear local data
                   test-icard   (nth (db/get-all-icards) 0)
                   test-sldata  (db/new-sldata test-icard)
                   

                   ]
               ?form ))

     (after :contents
            (doall
             (Thread/sleep 8000)
             (db/SYSdrop "four-notecards")))
     ]

    (fact
; FCNS TESTED: show
      (let [test-slip    (nth (db/get-all-slips) 0)
            frame1    (PFrame.)
            canvas1   (.getCanvas frame1)
            layer1    (.getLayer canvas1)
            dragger   (PDragEventHandler.)
            IGNORE    (.setMoveToFrontOnPress dragger true)
            IGNORE    (.setPanEventHandler canvas1 nil)
            IGNORE    (.addInputEventListener canvas1 dragger)
            IGNORE    (println
                       "Check the topmost window; it should show"
                       "one slip")
            ]    
        (db/show test-slip 50 150 layer1) => nil))

    )   ; close "against-background"

; ------------------------------------------------------

; test 5; compiles 120124
(against-background
    
    [(before :contents
             (db/SYSload "four-notecards"))
     
     (around :contents
             (let [frame1    (PFrame.)
                   canvas1   (.getCanvas frame1)
                   layer1    (.getLayer canvas1)
                   dragger   (PDragEventHandler.)
                   IGNORE    (.setMoveToFrontOnPress dragger true)
                   IGNORE    (.setPanEventHandler canvas1 nil)
                   IGNORE    (.addInputEventListener canvas1 dragger)

                   IGNORE       (db/SYSclear-all) ; new test; clear local data
                   IGNORE       (db/SYSsetup-InfWb "brain" "test") ;more setup
                   test-icard   (nth (db/get-all-icards) 0)
                   test-sldata  (db/new-sldata test-icard)
                   test-slip    (nth (db/get-all-slips) 0)
                   ]
               ?form ))

     (after :contents
            (db/SYSdrop "four-notecards"))
     ]
    
; For some reason, the fact below does not "see" test-card or sldata2, even
; though it should be available because of the bindings in 'around' stmt above
    (fact
; FCNS TESTED:
      (let [icard2       (nth (db/get-all-icards) 1)
            slip2        (db/icard->sldata->localDB icard2)
            sldata2      (db/get-sldata slip2)]
        
        (db/SYSsldata-field sldata2 :icard) => "gw667_090815163031398"
        ))

    )   ; close "against-background"

