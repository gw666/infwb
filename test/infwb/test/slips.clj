; project: github/gw666/infwb
; file: /test/infwb/test/slips

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
  (println "doing setup; all icards being loaded into local db")
  (db-startup)
  (let [iid-seq (db->all-iids)]
    (doseq [card iid-seq]
      (db->appdb card))
    ))
  

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

(deftest test-make-1-slip []
  (let [test-iid "gw667_090815162059614"
	test-ttxt (icard-field (get-icard test-iid) :ttxt)
	test-slip (new-slip test-iid)
	_   (slip->appdb test-slip)]
    (is (= test-ttxt (slip-field test-slip :ttxt)) ":ttxt field")))

(deftest test-slip-field []
  (let [slip-id (nth (appdb->all-sids) 0)
	new-x   62
	new-y  118
	test-slip (new-slip slip-id)]
    (move-to test-slip new-x new-y)
    (is (= new-x (round-to-int (slip-field test-slip :x))) "x")
    (is (= new-y (round-to-int (slip-field test-slip :y))) "y") ))

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

