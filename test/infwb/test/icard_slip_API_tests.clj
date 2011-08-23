(ns infwb.test.icard-slip-API-tests
  (:use [infwb.icard-slip-API] :reload)
  (:use [infwb.sedna] :reload)
  (:use [clojure.test]))

;; Required manual setup: Sedna must have a db named "brain".
;; The 'brain' collection must have a copy of the file
;; "~/tech/clojurestuff/cljprojects/infwb/src/four-notecards.XML". The file's
;; lowest key is "gw667_090815161114586", and there should be 4 infocards
;; with data in them (ignore the first "housekeeping" infoml element).
;;
;; NB: The function (SYSclear-all) is run at the start of testing.
;;
;; When things don't seem to be going right, follow the procedure in
;; 'GW notes on Clojure', topic 'PROPOSED PROCEDURE for using InfWb'

(defn icard-title [icard]
  (println icard
	   "   "
	   (iget icard :ttxt)))

(deftest test-get-all-icards []
  (println "1 test-get-all-icards")
  (SYSclear-all)
  (let [icard-seq (get-all-icards)
	]
    (doall (map icard-title icard-seq))
    (is (= 4 (icdata-localDB-size)))
  ))



(defn test-ns-hook
  "controls test sequence; NOTE: contains fixed Sedna db & collection names"
  []
  (println "##### Did you recompile the test file? #####")

  (let [db-name "brain"
	coll-name "test"]
    
    (icard-db-startup db-name coll-name)
    (test-get-all-icards)
   ))

