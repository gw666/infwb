(ns infwb.test.icards
  (:use [infwb.sedna] :reload)
  (:use [infwb.core] :reload)
  (:use [clojure.test]))

;; Required manual setup: Sedna must have a db named "brain".
;; The 'brain' collection must have a copy of the file
;; "~/tech/clojurestuff/cljprojects/infwb/src/four-notecards.XML". The file's
;; lowest key is "gw667_090815161114586", and there should be 4 records.
;;
;; NB: The function (db-startup) is run at the start of testing.
;;
;; When things don't seem to be going right, follow the procedure in
;; 'GW notes on Clojure', topic 'PROPOSED PROCEDURE for using InfWb'

(deftest test-read-icard-from-permDB []
  (println "1 test-read-icard-from-db")
  (reset-icards-db)
  (let [bad-icard   "INVALID ICARD"   ;implementation-dependent icard
	icard-title (icdata-field (permDB->icdata "gw667_090815161114586")
				  :ttxt)
	invalid-icard (permDB->icdata bad-icard)
	]
    (is (not= nil icard-title))
    (is (= nil invalid-icard) ))
  ;; this test leaves icards-db empty
  (reset-icards-db))

(deftest test-get-all-icards []
	 (println "2 test-get-all-icards")
	 (is (= 4 (count (permDB->all-icards)))) )

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
		(count (map permDB->icdata (permDB->all-icards))) )))

;; vers below doesn't seem to work--too much for Clojure or Java to handle?
;;   Or maybe there is an issue of parallelism. Here's the err msg:
;;
;; 6 test-all-icards-to-appdb

;; FAIL in (test-all-icards-to-appdb) (core.clj:46)
;; expected: (= 4 (icdata-db-size))
;;   actual: (not (= 4 0)) 
;; (deftest test-all-icards-to-appdb []
;; 	 (println "6 test-all-icards-to-appdb")
;; 	 (map permDB->localDB (permDB->all-icards))
;; 	 (is (= 4 (icdata-appdb-size))) )

(deftest test-all-icards-to-localDB []
	 (println "6 test-all-icards-to-localDB")
	 (let [all-icards (permDB->all-icards)]
	   (doseq [card all-icards]
	     (permDB->localDB card)))
	 (is (= 4 (icdata-localDB-size))) )

(defn test-ns-hook
  "controls test sequence; NOTE: contains fixed Sedna db & collection names"
  []
  (println "##### Did you recompile the test file? #####")

  (let [db-name "brain"
	coll-name "test"]
    
    (icard-db-startup db-name coll-name)
    (test-read-icard-from-permDB)
    (test-get-all-icards)
    (test-write-1-icard-to-localDB)
    (test-db-to-localDB)
    (test-get-all-icards2)
    (test-all-icards-to-localDB)))

