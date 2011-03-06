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
;;     )
;;
;; When things don't seem to be going right, follow the procedure in
;; 'GW notes on Clojure', topic 'PROPOSED PROCEDURE for using InfWb'

(defn test-slips-setup []
  (println "1 test-slips-setup")
  (db-startup)
  (let [iid-seq ["gw667_090815161114586" "gw667_090815162059614"
		 "gw667_090815163031398" "gw667_090815164115403"
		 "gw667_090815164740709" "gw667_090815165446504"]
	]
    (doseq [card iid-seq]
      (db->appdb card))
    ))
  
(deftest test-write-1-slip []
	 (println "2 test-write-1-slip")
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
	 (println "3 test-create-pobj")
	 (let [slip-id (nth (appdb->all-sids) 0)
	       x-position 100
	       y-position 50
	       pobj (slip-pobj (lookup-slip slip-id x-position y-position))]
	   (and
	    (is (= x-position (.getX pobj)))
	    (is (= y-position (.getY pobj)))) ))


(defn test-ns-hook []
  (test-slips-setup)
  (test-write-1-slip)
  (test-create-pobj))

;; WARN: next assumes that frame1, etc. setup at the def level



