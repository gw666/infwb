(ns infwb.test.slips
  (:use [infwb.slip-display] :reload)
  (:use [infwb.sedna] :reload)
  (:use [infwb.core] :reload)
  (:use [clojure.test]))

;; Required manual setup: Sedna must have a database named "brain".
;; Database must have a copy of the file
;; "/Users/gw/Documents/99-IMPORTANT DOCUMENTS/permanent infocards, sch
;; ema v0.90/hofstadter, doidge.XML". The file's
;; lowest key is "gw667_090815161114586", and there should be 67 records.
;; NB: The function (db-startup) is run at the start of testing.
;;
;; Also, set needed top-level vars for Piccolo with the following:
;;
;;   (do
;;     (def frame1 (PFrame.))
;;     (def canvas1 (.getCanvas frame1))
;;     (def layer1 (.getLayer canvas1))
;;     (def dragger (PDragEventHandler.))    
;;     (.setVisible frame1 true)
;;     (.setMoveToFrontOnPress dragger true)
;;     (.setPanEventHandler canvas1 nil)
;;     (.addInputEventListener canvas1 dragger)
;;     )
;;
;; COMPILATION WILL FAIL if these variables aren't defined.
;;
;; When things don't seem to be going right, follow the procedure in
;; 'GW notes on Clojure', topic 'PROPOSED PROCEDURE for using InfWb'

;; not needed if (initialize) has been run
(defn test-slips-setup []
  (println "doing setup; all icards being loaded into local db")
  (icard-db-startup)
  (let [icard-seq (permDB->all-icards)]
    (doseq [card icard-seq]
      (permDB->localDB card))))

;; (test-slips-setup) should run before this fcn runs. This makes
;; localDB slip db empty.
;; At end of this fcn, localDB sldata db has one entry based on first icard.
(deftest test-make-1-sldata
  "Creates test suite's first sldata, based on first icard"
  []
  (reset-sldata-db)
  (let [test-icard (nth (localDB->all-icards) 0)
	test-ttxt (icdata-field (localDB->icdata test-icard) :ttxt)
	test-sldata (new-sldata test-icard)
	_   (sldata->localDB test-sldata)]
;    (swank.core/break)
    (is (= test-ttxt (sldata-field test-sldata :ttxt)) ":ttxt field")
    (is (= 1 (count (localDB->all-slips)))) ))

;; DEV: This must be modified whenever a field is added
;; This test works on the first sldata, which was created by (test-make-1-sldata)
(deftest test-sldata-field []
  (let [icard (nth (localDB->all-icards) 0)
	slip (nth (localDB->all-slips) 0)
	sldata (get-sldata slip)
	icdata (localDB->icdata icard)
	ttxt (icdata-field icdata :ttxt)
	btxt (icdata-field icdata :btxt)]
;    (swank.core/break)
    (is (= slip (sldata-field sldata :slip)))
    (is (= icard (sldata-field sldata :icard)))
    (is (= ttxt (sldata-field sldata :ttxt)))
    (is (= btxt (sldata-field sldata :btxt))) ))
  
;; This test works on the first sldata, which was created by (test-make-1-sldata)
(deftest test-move-sldata
  "Moves an existing sldata, then checks to see whether move worked by
examining the pobj's x and y values (using `sldata-field`)"
  []
  (let [sldata-id (nth (localDB->all-slips) 0) ; get first sldata in localDB
	new-x   62
	new-y  118
	test-sldata (get-sldata sldata-id)]
    (move-to test-sldata new-x new-y)
    (is (= new-x (round-to-int (sldata-field test-sldata :x))) "x")
    (is (= new-y (round-to-int (sldata-field test-sldata :y))) "y") ))

(deftest test-show-1-sldata []
  (println "\n### WARN: Be sure that layer1 is defined ###\n")
  (let [sldata-id (nth (localDB->all-slips) 0) 
	test-sldata (get-sldata sldata-id)
	test-x   50
	test-y  150]
    (println "test-show-1-sldata succeeds if you see sldata named '"
	     (sldata-field test-sldata :ttxt) "' onscreen at ("
	     test-x " " test-y ")\n")
    (show test-sldata test-x test-y layer1) ))

;; this fcn creates a second sldata
(deftest test-make-sldata-from-db []
  (let [icard2 (nth (localDB->all-icards) 1)
	slip2   (icard->sldata->localDB icard2)
	sldata2   (get-sldata slip2)]
    (is (= icard2 (sldata-field sldata2 :icard)))
    (is (= slip2 (sldata-field sldata2 :slip)))
    (is (= (sldata-field (get-sldata "INVALID KEY") :icard)
	   "ERROR: Sldata 'INVALID KEY' is INVALID"))
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
    (is (= 3 (count (localDB->all-slips))))
    (is (=  ["tag1" "tag2"] (sldata-field sldata :tags)))))
  )

	 


(defn test-ns-hook []
  (println "### Did you define needed variables (e.g., layer1)? ###")
  ;; (test-sldatas-setup)
  ;; (test-write-1-sldata)
  ;; (test-create-pobj)
  ;; (test-add-pobj-to-localDB)
  ;; (test-display-1-sldata)
  ;; (test-display-all-sldatas)
  (println "running (test-make-1-sldata)")
  (test-make-1-sldata)
  (println "running (test-sldata-field)")
  (test-sldata-field)
  (println "running (test-move-sldata)")
  (test-move-sldata)
  (println "running (test-show-1-sldata)")
  (test-show-1-sldata)
  (println "running (test-make-sldata-from-db)")
  (test-make-sldata-from-db)
  (println "running (test-tags-in-icards-and-slips)")
  (test-tags-in-icards-and-slips)
  )

