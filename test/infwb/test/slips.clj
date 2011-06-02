; project: github/gw666/infwb
; file: /test/infwb/test/slips

(ns infwb.test.slips
  (:use [infwb.infocard] :reload)
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

(defn test-slips-setup []
  (println "doing setup; all icards being loaded into local db")
  (db-startup)
  (let [icard-seq (db->all-icards)]
    (doseq [card icard-seq]
      (db->appdb card))
    ))

(defn reset-slip-db
  "Clears out the slip-db so as to enable another round of testing without
having to run `(test-slips-setup)`, which is time-consuming, again"
  []
  (swap! *appdb* assoc-in [*slip-idx*] {})
  nil)
  


;; (test-slips-setup) should run before this fcn runs. This makes
;; appdb slip db empty.
;; At end of this fcn, appdb slip db has one entry based on first icard.
(deftest test-make-1-slip
  "Creates test suite's first slip, based on first icard"
  []
  (let [test-icard (nth (appdb->all-icards) 0)
	test-ttxt (icdata-field (get-icdata test-icard) :ttxt)
	test-slip (new-slip test-icard)
	_   (slip->appdb test-slip)]
;    (swank.core/break)
    (is (= test-ttxt (slip-field test-slip :ttxt)) ":ttxt field")
    (is (= 1 (count (appdb->all-sids)))) ))

;; DEV: This must be modified whenever a field is added
;; This test works on the first slip, which was created by (test-make-1-slip)
(deftest test-slip-field []
  (let [icard (nth (appdb->all-icards) 0)
	sid (nth (appdb->all-sids) 0)
	slip (get-slip sid)
	icdata (get-icdata icard)
	ttxt (icdata-field icdata :ttxt)
	btxt (icdata-field icdata :btxt)]
    (is (= sid (slip-field slip :sid)))
    (is (= icard (slip-field slip :icard)))
    (is (= ttxt (slip-field slip :ttxt)))
    (is (= btxt (slip-field slip :btxt))) ))
  
;; This test works on the first slip, which was created by (test-make-1-slip)
(deftest test-move-slip
  "Moves an existing slip, then checks to see whether move worked by
examining the pobj's x and y values (using `slip-field`)"
  []
  (let [slip-id (nth (appdb->all-sids) 0) ; get first slip in appdb
	new-x   62
	new-y  118
	test-slip (get-slip slip-id)]
    (move-to test-slip new-x new-y)
    (is (= new-x (round-to-int (slip-field test-slip :x))) "x")
    (is (= new-y (round-to-int (slip-field test-slip :y))) "y") ))

(deftest test-show-1-slip []
  (println "\n### WARN: Be sure that layer1 is defined ###\n")
  (let [slip-id (nth (appdb->all-sids) 0) 
	test-slip (get-slip slip-id)
	test-x   50
	test-y  150]
    (println "test-show-1-slip succeeds if you see slip named '"
	     (slip-field test-slip :ttxt) "' onscreen at ("
	     test-x " " test-y ")\n")
    (show test-slip test-x test-y layer1) ))

;; this fcn creates a second slip
(deftest test-make-slip-from-db []
  (let [icard2 (nth (appdb->all-icards) 1)
	sid2   (icard->slip->appdb icard2)
	slip2   (get-slip sid2)]
    (is (= icard2 (slip-field slip2 :icard)))
    (is (= sid2 (slip-field slip2 :sid)))
    (is (= (slip-field (get-slip "INVALID KEY") :icard)
	   "ERROR: Slip 'INVALID KEY' is INVALID"))
    ))

	 


(defn test-ns-hook []
  (println "### Did you define needed variables (e.g., layer1)? ###")
  (println "### Did you re-eval? '(ns infwb.test.slips ... )' ? ###\n")
  ;; (test-slips-setup)
  ;; (test-write-1-slip)
  ;; (test-create-pobj)
  ;; (test-add-pobj-to-appdb)
  ;; (test-display-1-slip)
  ;; (test-display-all-slips)
  (println "running (test-make-1-slip)")
  (test-make-1-slip)
  (println "running (test-slip-field)")
  (test-slip-field)
  (println "running (test-move-slip)")
  (test-move-slip)
  (println "running (test-show-1-slip)")
  (test-show-1-slip)
  (println "running (test-make-slip-from-db)")
  (test-make-slip-from-db)
  )

