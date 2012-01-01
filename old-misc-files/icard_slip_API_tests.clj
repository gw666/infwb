(ns infwb.test.icard-slip-API-tests
  (:import
   (edu.umd.cs.piccolo         PCanvas PLayer)
   (edu.umd.cs.piccolox        PFrame))
  (:use [infwb.sedna] :reload)
  (:use [clojure.test]))

;; Required manual setup: Sedna must have a db named 'brain' and a collection.
;; named 'test'. The infocards folder (/Users/gw/Dropbox/infocards) must
;; contain the file 'four-notecards.XML'. The file's
;; lowest key is "gw667_090815161114586", and there should be 4 infocards
;; with data in them (ignore the first "housekeeping" infoml element).
;;
;; NB: * The function (SYSclear-all) is run at the start of testing.
;;     * icard_slip_API.clj must be compiled before this test file.


(defn icard-title [icard]
  (println icard
	   "   "
	   (iget icard :ttxt)))

(deftest test-permDB->all-icards []
  (println "1 test-permDB->all-icards")
  (SYSclear-all)
  (let [icard-seq (permDB->all-icards)
	]
    (doall (map icard-title icard-seq))
    (is (= 4 (icdata-localDB-size)))
  ))

;; (deftest test-display-all [layer]
;;   (println "2 test-display-all")
;;   (println "### test succeeds if 4 slips are visible on desktop")
;;   (SYSclear-all)
;;   (let [_   (get-all-icards)  ;loads icards into *localDB-icdata*
;; 	]
;;     (display-new layer)))
    

(defn test-ns-hook
  "controls test sequence; NOTE: contains fixed Sedna db & collection names"
  []
  (println "##### Did you recompile the test file? #####")

  (let [db-name      "brain"
	coll-name    "test"
	frame       (new PFrame)
	layer        (.. frame getCanvas getLayer) ; objects are placed here
	]
					;    (set-icard-db-name db-name)
					;   (set-icard-coll-name coll-name)
    (SYSsetup-InfWb db-name coll-name)
    (. frame setSize 500 700)
    (. frame setVisible true)

    
    (SYSload "four-notecards")
    (SYSsetup-InfWb db-name coll-name)
    (test-permDB->all-icards)
					;    (test-display-all layer)
    (SYSclear-all)
    (swank.core/break)
    (display-new layer)

    (SYSdrop "four-notecards")
    ))

