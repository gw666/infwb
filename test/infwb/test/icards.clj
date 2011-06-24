; project: github/gw666/infwb
; file: /test/infwb/test/icards

; HISTORY:

(ns infwb.test.icards
  (:use [infwb.infocard] :reload)
  (:use [infwb.sedna] :reload)
  (:use [infwb.core] :reload)
  (:use [clojure.test]))

;; Required manual setup: Sedna must have a db named "brain".
;; Collection must have a copy of the file
;; "/Users/gw/Documents/99-IMPORTANT DOCUMENTS/permanent infocards, sch
;; ema v0.90/hofstadter, doidge.XML". The file's
;; lowest key is "gw667_090815161114586", and there should be 67 records.
;; NB: The function (db-startup) is run at the start of testing.
;;
;; When things don't seem to be going right, follow the procedure in
;; 'GW notes on Clojure', topic 'PROPOSED PROCEDURE for using InfWb'

(deftest test-read-icard-from-db []
	 (println "1 test-read-icard-from-db")
	 (is (and
	      (not= nil (icdata-field (db->icdata "gw667_090815161114586") :ttxt))
	      (= nil (icdata-field (db->icdata "INVALID KEY") :ttxt)) )))

(deftest test-get-all-icards []
	 (println "2 test-get-all-icards")
	 (is (= 67 (count (db->all-icards)))) )

(deftest test-write-1-icard []
  (println "3 test-write-1-icard")
  (let [icdata1 (db->icdata "gw667_090815161114586")
	_ (icdata->appdb icdata1)]
    (is (= "the ability to think"
	    (icdata-field (db->icdata "gw667_090815161114586") :ttxt) ))))

(deftest test-db-to-appdb []
	 (println "4 test-db-to-appdb")
	 (db->appdb "gw667_090815162059614")
	 (is (= "to label, categorize, and find precedents"
		  (icdata-field (get-icdata "gw667_090815162059614") :ttxt) )))

(deftest test-get-all-icards2 []
	 (println "5 test-get-all-icards2")
	 (is (= 67
		(count (map db->icdata (db->all-icards))) )))

;; vers below doesn't seem to work--too much for Clojure or Java to handle?
;;   Or maybe there is an issue of parallelism. Here's the err msg:
;;
;; 6 test-all-icards-to-appdb

;; FAIL in (test-all-icards-to-appdb) (core.clj:46)
;; expected: (= 67 (icdata-db-size))
;;   actual: (not (= 67 0)) 
;; (deftest test-all-icards-to-appdb []
;; 	 (println "6 test-all-icards-to-appdb")
;; 	 (map db->appdb (db->all-icards))
;; 	 (is (= 67 (icdata-appdb-size))) )

(deftest test-all-icards-to-appdb []
	 (println "6 test-all-icards-to-appdb")
	 (let [all-icards (db->all-icards)]
	   (doseq [card all-icards]
	     (db->appdb card)))
	 (is (= 67 (icdata-appdb-size))) )

(defn test-ns-hook []
  (println "Did you recompile the test file?")
  (db-startup)
  (test-read-icard-from-db)
  (test-get-all-icards)
  (test-write-1-icard)
  (test-db-to-appdb)
  (test-get-all-icards2)
  (test-all-icards-to-appdb))

