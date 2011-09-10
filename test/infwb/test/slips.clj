(ns infwb.test.slips
  (:use [infwb.slip-display] :reload)
  (:use [infwb.sedna] :reload)
  (:use [infwb.core] :reload)
  (:use [clojure.set :only (difference)])
  (:use [clojure.test]))

;; Required manual setup: Sedna must have a database named "brain".
;; Database must have a copy of the file
;; "~/tech/clojurestuff/cljprojects/infwb/src/four-notecards.XML". The file's
;; lowest key is "gw667_090815161114586", and there should be 4 records.
;;
;; NB: The function (db-startup) is run at the start of testing.
;;
;; Also, set needed top-level vars for Piccolo with the following:
;;
;;   (do
;;     (def *piccolo-frame* (PFrame.))
;;     (def canvas1 (.getCanvas frame1))
;;     (def *piccolo-frame* (.getLayer canvas1))
;;     (def dragger (PDragEventHandler.))    
;;     (.setVisible frame1 true)
;;     (.setMoveToFrontOnPress dragger true)
;;     (.setPanEventHandler canvas1 nil)
;;     (.addInputEventListener canvas1 dragger)
;;     )
;;
;; COMPILATION WILL FAIL if these variables aren't defined FIRST.
;;
;; To ensure repeatable and correct results, you should run:
;;
;; (initialize)
;; (clojure.test/run-tests 'infwb.test.icards)
;; (clojure.test/run-tests 'infwb.test.slips)
;;
;; If these two tests are not run *exactly* together, you may need to
;; run (initialize) again before the slips test runs correctly.

;; When things don't seem to be going right, follow the procedure in
;; 'GW notes on Clojure', topic 'PROPOSED PROCEDURE for using InfWb'

;; (infwb.core/initialize) should run before this fcn runs. 
;; This makes localDB slip db empty.
;; At end of this fcn, localDB sldata db has one entry based on first icard.
(deftest test-make-1-sldata
  "Creates test suite's first sldata, based on first icard"
  []
  (reset-slips)
  (let [test-icard (nth (get-all-icards) 0)
	;; get title of first icard, create slip based on it, add to sldata db
	test-ttxt (icdata-field (localDB->icdata test-icard) :ttxt)
	test-sldata (new-sldata test-icard)]
;    (swank.core/break)
    ;; Does SYSsldata-field get the title from the icard correctly?
    (is (= test-ttxt (SYSsldata-field test-sldata :ttxt)) ":ttxt field")
    (is (= 1 (count (get-all-slips)))) ))

;; DEV: This must be modified whenever a field is added
;; This test works on the first sldata, which was created by (test-make-1-sldata)
(deftest test-SYSsldata-field []
  (let [icard (nth (get-all-icards) 0)
	slip (nth (get-all-slips) 0)
	sldata (get-sldata slip)
	icdata (localDB->icdata icard)
	ttxt (icdata-field icdata :ttxt)
	btxt (icdata-field icdata :btxt)]
;    (swank.core/break)
    ;; tests if SYSsldata-field can access slip, icard, ttxt, btxt
    (is (= slip (SYSsldata-field sldata :slip)))
    (is (= icard (SYSsldata-field sldata :icard)))
    (is (= ttxt (SYSsldata-field sldata :ttxt)))
    (is (= btxt (SYSsldata-field sldata :btxt))) ))
  
;; This test works on the first sldata, which was created by (test-make-1-sldata)
(deftest test-move-sldata
  "Moves an existing sldata, then checks to see whether move worked by
examining the pobj's x and y values (using SYSsldata-field fcn)"
  []
  (let [sldata-id (nth (get-all-slips) 0) ; get first sldata in localDB
	new-x   62
	new-y  118
	test-sldata (get-sldata sldata-id)]
    (move-to test-sldata new-x new-y)
    (is (= new-x (round-to-int (SYSsldata-field test-sldata :x))) "x")
    (is (= new-y (round-to-int (SYSsldata-field test-sldata :y))) "y") ))

(deftest test-show-1-sldata []
  (println "\n### WARN: Be sure that *piccolo-frame* is defined ###\n")
  (let [sldata-id (nth (get-all-slips) 0) 
	test-sldata (get-sldata sldata-id)
	test-slip (:slip test-sldata)
	test-x   50
	test-y  150]
    (println "test-show-1-sldata succeeds if you see sldata named '"
	     (SYSsldata-field test-sldata :ttxt) "' onscreen at ("
	     test-x " " test-y ")\n")
    (show test-slip test-x test-y *piccolo-layer*) ))

;; this fcn creates a second sldata
(deftest test-make-sldata-from-db []
  (let [icard2 (nth (get-all-icards) 1)
	slip2   (icard->sldata->localDB icard2)
	sldata2   (get-sldata slip2)]
    (is (= icard2 (SYSsldata-field sldata2 :icard)))
    (is (= slip2 (SYSsldata-field sldata2 :slip)))
    ;; check for correct handling of non-existent slip
    (is (= (SYSsldata-field (get-sldata "INVALID KEY") :icard)
	   "ERROR"))
    ))

;; tests icard creation, retrieving of tag data; NOTE: assumes that
;; all the above tests have been run, leaving 1 slips in localDB
(deftest test-tags-in-icards-and-slips []
  (let [icdata (new-icdata "id1234" "title text" "body text" ["tag1" "tag2"])
	_      (icdata->localDB icdata)
	icard  (icdata-field icdata :icard)
	]
    (is (= "id1234" icard))
;    (swank.core/break)
    (let [
	slip   (icard->sldata->localDB icard)
	sldata (get-sldata slip)
	]
    (is (= 3 (count (get-all-slips))))
    (is (=  ["tag1" "tag2"] (SYSsldata-field sldata :tags)))))
  )

	 


(defn test-ns-hook []
  (println "### Did you recompile the test file?                ###")
  (println "### Did you define needed variables (e.g., *piccolo-layer*)? ###")
  (println "### Did you run (initialize) since last recompile?  ###")
  (println "running (test-make-1-sldata)")
  (test-make-1-sldata)
  (println "running (test-SYSsldata-field)")
  (test-SYSsldata-field)
  (println "running (test-move-sldata)")
  (test-move-sldata)
  (println "running (test-show-1-sldata)")
  (test-show-1-sldata)
  (println "running (test-make-sldata-from-db)")
  (test-make-sldata-from-db)
  (println "running (test-tags-in-icards-and-slips)")
  (test-tags-in-icards-and-slips)
  )

