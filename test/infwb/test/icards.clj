(ns infwb.test.icards
  (:use [infwb.sedna] :reload)
  (:use [infwb.core] :reload)
  (:use [clojure.test]))

;; Required manual setup: Sedna must have a db named "brain".
;; The 'brain' collection must have a copy of the file
;; "~/tech/clojurestuff/cljprojects/infwb/src/four-notecards.XML". The file's
;; lowest key is "gw667_090815161114586", and there should be 4 records.
;;
;;
;; To ensure repeatable and correct results, you should run:
;;
;; (initialize)
;; (clojure.test/run-tests 'infwb.test.icards)
;; (clojure.test/run-tests 'infwb.test.slips)
;;
;; When things don't seem to be going right, follow the procedure in
;; 'GW notes on Clojure', topic 'PROPOSED PROCEDURE for using InfWb'

(deftest test-read-icard-from-permDB []
  (println "1 test-read-icard-from-db")
  (reset-icards-db)
  (let [bad-icard   "INVALID ICARD"   ;implementation-dependent icard
	bad-retrieved-icard   (permDB->icdata bad-icard)
	icard-title (icdata-field (permDB->icdata "gw667_090815161114586")
				  :ttxt)
	invalid-title (icdata-field bad-retrieved-icard :ttxt)
	]
    (is (not= nil icard-title))
    (is (= invalid-title "ERROR: infocard 'INVALID ICARD' not found") ))
  ;; this test leaves icards-db empty
  (reset-icards-db))

(deftest test-get-all-icards []
	 (println "2 test-get-all-icards")
	 (is (= 4 (count (get-all-icards)))) )

(deftest test-write-1-icard-to-localDB []
  (println "3 test-write-1-icard")
  (let [icdata1 (permDB->icdata "gw667_090815161114586")
	_ (icdata->localDB icdata1)]
    (is (= "the ability to think"
	    (icdata-field (localDB->icdata "gw667_090815161114586") :ttxt) ))))

(deftest test-db-to-localDB []
	 (println "4 test-db-to-localDB")
	 (permDB->localDB "gw667_090815162059614")
	 (is (= "to label, categorize, and find precedents"
		  (icdata-field (localDB->icdata "gw667_090815162059614") :ttxt) )))

(deftest test-get-all-icards2 []
	 (println "5 test-get-all-icards2")
	 (is (= 4
		(count (map permDB->icdata (get-all-icards))) )))

(deftest test-all-icards-to-localDB []
	 (println "6 test-all-icards-to-localDB")
	 (let [all-icards (get-all-icards)]
	   (doseq [card all-icards]
	     (permDB->localDB card)))
	 (is (= 4 (count (localDB->all-icards)))) )

(defn test-ns-hook
  "controls test sequence; NOTE: contains fixed Sedna db & collection names"
  []
  (println "### Did you recompile the test file?                ###")

  (let [db-name "brain"
	coll-name "test"]
    
    (SYSsetup-InfWb db-name coll-name)
    (test-read-icard-from-permDB)
    (test-get-all-icards)
    (test-write-1-icard-to-localDB)
    (test-db-to-localDB)
    (test-get-all-icards2)
    (test-all-icards-to-localDB)))

