; project: github/gw666/infwb
; file: /test/infwb/test/icards

; HISTORY:

(ns infwb.test.icards
  (:use [infwb.infocard] :reload)
  (:use [infwb.sedna] :reload)
  (:use [infwb.core] :reload)
  (:use [clojure.test]))

;; Required manual setup: Sedna must have a db named "text" with a collection
;; called "test". Collection must have a copy of the file
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
	      (not= nil (:ttxt (db->icard "gw667_090815161114586")))
	      (= nil (:ttxt (db->icard "INVALID KEY"))) )))

(deftest test-get-all-iids []
	 (println "2 test-get-all-iids")
	 (is (= 67 (count (db->all-iids)))) )

(deftest test-write-1-icard []
  (println "3 test-write-1-icard")
  (let [icard1 (db->icard "gw667_090815161114586")
	_ (icard->appdb icard1)]
    (is (= "the ability to think"
	    (:ttxt (get-icard "gw667_090815161114586")) ))))

(deftest test-db-to-appdb []
	 (println "4 test-db-to-appdb")
	 (db->appdb "gw667_090815162059614")
	 (is (= "to label, categorize, and find precedents"
		  (:ttxt (get-icard "gw667_090815162059614")) )))

(deftest test-get-all-icards []
	 (println "5 test-get-all-icards")
	 (is (= 67
		(count (map db->icard (db->all-iids))) )))

;; vers below doesn't seem to work--too much for Clojure or Java to handle?
;;   Or maybe there is an issue of parallelism. Here's the err msg:
;;
;; 6 test-all-icards-to-appdb

;; FAIL in (test-all-icards-to-appdb) (core.clj:46)
;; expected: (= 67 (icard-db-size))
;;   actual: (not (= 67 0)) 
;; (deftest test-all-icards-to-appdb []
;; 	 (println "6 test-all-icards-to-appdb")
;; 	 (map db->appdb (db->all-iids))
;; 	 (is (= 67 (icard-appdb-size))) )

(deftest test-all-icards-to-appdb []
	 (println "6 test-all-icards-to-appdb")
	 (let [all-iids (db->all-iids)]
	   (doseq [card all-iids]
	     (db->appdb card)))
	 (is (= 67 (icard-appdb-size))) )

(defn test-ns-hook []
  (println "Did you recompile the test file?")
  (db-startup)
  (test-read-icard-from-db)
  (test-get-all-iids)
  (test-write-1-icard)
  (test-db-to-appdb)
  (test-get-all-icards)
  (test-all-icards-to-appdb))

;; WARN: next assumes that frame1, etc. setup at the def level



