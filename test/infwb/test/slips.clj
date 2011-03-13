; project: github/gw666/infwb
; file: /test/infwb/test/slips

; HISTORY:

(ns infwb.test.slips
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
  (println "doing setup")
  (db-startup)
  (let [iid-seq (db->all-iids)]
    (doseq [card iid-seq]
      (db->appdb card))
    ))
  
(deftest test-write-1-slip []
	 (let [test-iid   "gw667_090815161114586"
	       an-icard (db->icard test-iid)
	       a-slip (icard->new-slip an-icard)
	       slip-id (slip-field a-slip :id)
	       _ (slip->appdb a-slip)	;new slip added to appdb
	       slip-retrieved (lookup-slip slip-id)]
	   (and
	    (is (= "gw667_090815161114586"
		   (:iid slip-retrieved) ))
	    (is (= 1 (count (appdb->all-sids)))) )
	   ))

(deftest test-create-pobj []
	 (let [slip-id (nth (appdb->all-sids) 0)
	       x-position 100
	       y-position 50
	       pobj (slip-pobj (lookup-slip slip-id) x-position y-position)]
	    (is (= (round-to-int x-position) (round-to-int (.getX pobj)))
	    (is (= (round-to-int y-position) (round-to-int (.getY pobj)))
	    ))))

(deftest test-add-pobj-to-appdb []
	 (let [slip-id (nth (appdb->all-sids) 0)
	       partial-slip (lookup-slip slip-id)
	       x-position 100
	       y-position 50
	       pobj (slip-pobj (lookup-slip slip-id) x-position y-position)
	       full-slip   (new-full-slip partial-slip pobj)]
	   (slip->appdb full-slip)
	   (is (= pobj (:pobj (lookup-slip slip-id)))) ))

(deftest test-display-1-slip []
	 (let [slip-id (nth (appdb->all-sids) 0)
	       slip (lookup-slip slip-id)
	       card (:pobj slip)]
	   (.addChild layer1 card)
	   (println "test-display-1-slip passes if card is visible in window")
	   (is true)))

(deftest test-display-all-slips []
  (let [iid-seq (appdb->all-iids)
	]
    (doseq [iid iid-seq]
      (let [icard (lookup-icard iid)
	    slip (icard->new-slip icard)
	    _   (slip->appdb slip)
	    pobj (slip-pobj slip (rand-int 300) (rand-int 200))
	    full-slip   (new-full-slip slip pobj)
	    _   (slip->appdb full-slip)
	       card (:pobj full-slip)]
	   (.addChild layer1 card)
	   ))
    (println "test-display-all-slips passes if cards are visible in window")
    (is true)))

(deftest test-make-1-slip []
  (let [test-iid "gw667_090815162059614"
	test-ttxt "to label, categorize, and find precedents"
	default-x   0
	default-y   0
	test-slip (new-slip test-iid)
	_   (slip->appdb test-slip)]
    (is (= test-ttxt (slip-field test-slip :ttxt) ":ttxt field")) ))

(deftest test-slip-field []
  (let [slip-id (nth (appdb->all-sids) 0)
	new-x   62
	new-y  118
	test-slip (new-slip slip-id)]
    (position-to test-slip new-x new-y)
    (is (= new-x (round-to-int (slip-field test-slip :getX)) "getX"))
    (is (= new-y (round-to-int (slip-field test-slip :getY)) "getY")) ))

(deftest test-show-1-slip []
  (println "\n### WARN: Be sure that layer1 is defined ###\n")
  (let [slip-id (nth (appdb->all-sids) 0) 
	test-slip (new-slip slip-id)
	test-x   51
	test-y  149]
    (println "test-show-1-slip succeeds if you see slip named"
	     (slip-field test-slip :ttxt) "onscreen") ))

	 


(defn test-ns-hook []
  (println "### Did you define needed variables (e.g., layer1)? ###\n")
  (test-slips-setup)
  ;; (test-write-1-slip)
  ;; (test-create-pobj)
  ;; (test-add-pobj-to-appdb)
  ;; (test-display-1-slip)
  ;; (test-display-all-slips)
  (test-make-1-slip)
  (test-slip-field)
  (test-show-1-slip)
  )

