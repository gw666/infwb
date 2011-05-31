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
  (let [iid-seq (db->all-iids)]
    (doseq [card iid-seq]
      (db->appdb card))
    ))

(defn reset-slip-db
  "Clears out the slip-db so as to enable another round of testing without
having to run `(test-slips-setup)`, which is time-consuming, again"
  []
  (swap! *appdb* assoc-in [*slip-idx*] {})
  nil)
  

;; (deftest test-display-all-slips []
;;   (let [iid-seq (appdb->all-iids)
;; 	]
;;     (doseq [iid iid-seq]
;;       (let [slip (new-slip iid)
;; 	    _   (slip->appdb slip)
;; 	    pobj (slip-pobj slip (rand-int 300) (rand-int 200))
;; 	    full-slip   (new-full-slip slip pobj)
;; 	    _   (slip->appdb full-slip)
;; 	       card (:pobj full-slip)]
;; 	   (.addChild layer1 card)
;; 	   ))
;;     (println "test-display-all-slips passes if cards are visible in window")
;;     (is true)))

;; (test-slips-setup) should run before this fcn runs. This makes
;; appdb slip db empty.
;; At end of this fcn, appdb slip db has one entry based on first iid.
(deftest test-make-1-slip
  "Creates test suite's first slip, based on first iid"
  []
  (let [test-iid (nth (appdb->all-iids) 0)
	test-ttxt (icard-field (get-icard test-iid) :ttxt)
	test-slip (new-slip test-iid)
	_   (slip->appdb test-slip)]
    (is (= test-ttxt (slip-field test-slip :ttxt)) ":ttxt field")
    (is (= 1 (count (appdb->all-sids)))) ))

;; DEV: This must be modified whenever a field is added
;; This test works on the first slip, which was created by (test-make-1-slip)
(deftest test-slip-field []
  (let [iid (nth (appdb->all-iids) 0)
	sid (nth (appdb->all-sids) 0)
	slip (get-slip sid)
	icard (get-icard iid)
	ttxt (icard-field icard :ttxt)
	btxt (icard-field icard :btxt)]
    (is (= sid (slip-field slip :sid)))
    (is (= iid (slip-field slip :iid)))
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
	     test-x " " test-y ")")
    (show test-slip test-x test-y layer1) ))

;; this fcn creates a second slip
(deftest test-make-slip-from-db []
  (let [iid2 (nth (appdb->all-iids) 1)
	sid2   (iid->slip->appdb iid2)
	slip2   (get-slip sid2)]
    (is (= iid2 (slip-field slip2 :iid)))
    (is (= sid2 (slip-field slip2 :sid)))
    (is (= (slip-field (get-slip "INVALID KEY") :iid)
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
  (test-make-1-slip)
  (test-slip-field)
  (test-move-slip)
  (test-show-1-slip)
  (test-make-slip-from-db)
  )

