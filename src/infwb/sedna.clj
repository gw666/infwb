; project: github/gw666/infwb
; file: src/infwb/sedna.clj
; last changed: 2/19/11

; HISTORY:


;; see http://www.cfoster.net/articles/xqj-tutorial/simple-xquery.xml

(ns infwb.sedna
  (:gen-class)
  (:import (javax.xml.xquery   XQConnection XQDataSource
			       XQResultSequence)
	   (net.cfoster.sedna.xqj   SednaXQDataSource)
	   (java.util   Properties))
    (:use [infwb.infocard]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; MISCELLANEOUS ROUTINES
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
  
(defn rand-kayko
  "creates a random key of 2*len characters"
  [len]
  (let [consons (repeatedly len #(rand-nth "bdfghjklmnpqrstvwxyz"))
	vowels (repeatedly len #(rand-nth "aeiou"))
	]
    (apply str (interleave consons vowels))))

  (defn abs[n]
    (if (neg? n) (- n) n))

  (defn round-to-int [n]
    (let [sign (if (neg? n) -1 1)
	  rounded-abs (int (+ (abs n) 0.5))]
      (* sign rounded-abs)))

(defn ls
  "Performs roughly the same task as the UNIX `ls`.  That is, returns a seq of the filenames
   at a given directory.  If a path to a file is supplied, then the seq contains only the
   original path given."
  [path]
  (let [file (java.io.File. path)]
    (if (.isDirectory file)
      (seq (.list file))
      (when (.exists file)
        [path]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; HOUSEKEEPING
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn db-startup
  "does all database setup for current session of work; should be
executed once; WARNING: deletes the database of icards and slips"
  []
  ;WARNING - RE-EXECUTING THIS DELETES ICARD DATABASE
  (def ^{:dynamic true} *icard-idx*   0) ;;icard db is 0th element of @*appdb*
  (def ^{:dynamic true} *slip-idx*    1) ;;slip db is 1st element of @*appdb*
  (def ^{:dynamic true} *appdb* (atom [{} {}]))
  
  (def ^{:dynamic true} *xqs* (SednaXQDataSource.)) ;naughty; OK for debugging
  (doto *xqs*
    (.setProperty "serverName" "localhost")
    (.setProperty "databaseName" "brain")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; DATABASE ACCESS USING XQUERY
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;insights taken from ~/tech/schemestuff/InfWb/main/sedna-utilities.ss

(declare get-result)

(defn run-db-query
  "Returns results of db query; filter selects records, result extracts
data from selected records. Influenced by (db-init). Assumes *xqs* is
a working XQDataSource."
  [filter result]
  (let [conn (.getConnection *xqs* "SYSTEM" "MANAGER")
	xqe (.createExpression conn)
	xqueryString
	(str
	 "declare default element namespace 'http://infoml.org/infomlFile';\n"
	 "for $card in collection('test')/infomlFile/"
	 filter "\n"
	 "return " result)
	rs (.executeQuery xqe xqueryString)
	result (get-result rs)]
    (.close conn)
    result))

(defn get-result
  "gets the result of an XQuery"
  ([result-sequence]
     (get-result result-sequence (vector)))
  ([result-sequence result-vector]
     (if (not  (.next result-sequence))
       result-vector
       (recur result-sequence (conj result-vector (.getItemAsString result-sequence (Properties.)))))))

;(defn show-db-query
;  "Returns full query that is executed by (run-db-query filter result)"
;  [filter result]
;  (vector
;   "declare default element namespace 'http://infoml.org/infomlFile';"
;   (str "for $card in collection('test')/infomlFile/" filter)
;   (str "return " result)))

;; One important characteristic of the icard "section" of \*appdb\* (itself a
;; map) is that the value of the id field of the icard is also the key
;; of that map.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ICARDS: creating
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord icard [id     ;;string; icard-id (iid) of infocard
		  ttxt   ;;string; title text
		  btxt]  ;;string; body text
  )

(defn new-icard [id ttxt btxt]
  (icard. id ttxt btxt))

(defn db->icard
  "get icard data from appn database, return it as an icard record"
  [iid]
  (let [data-vec 
	(run-db-query (str "infoml[@cardId = '" iid "']")
		 "($card/data/title/string(), $card/data/content/string())")]
    (new-icard iid (get data-vec 0) (get data-vec 1))))

(defn db->all-iids
  "from appn database, get seq of all icard IDs"
  []
  (run-db-query "infoml[position() != 1]" "$card/@cardId/string()"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; SLIPS: creating
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord slip [id      ;;string; id of slip
		 iid     ;;string; card-id of icard to be displayed
		 pobj]   ;;Piccolo object that implements slip
  )

(declare slip-pobj)

(defn new-slip
  [icard-id]  (let [rand-key   (str "sl:" (rand-kayko 3))
		    default-x   0
		    default-y   0
		    pobj   (slip-pobj slip default-x default-y)]
		(slip. rand-key icard-id pobj)))

(defn icard->new-slip
  "given icard, create the corresponding partial slip"
  [icard]
  (new-slip (:id icard)))

;; used only when populating \*appdb\* with new icards

;; TODO: confirm correct behavior for replace vs. add

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; APPDB: populating it with icards
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn icard->appdb
  "Stores the icard record in the in-memory database"
  [icard]
  (let [id (:id icard)
	icard-idx   *icard-idx*
	id-exists?  (get-in @*appdb* [icard-idx id])]
    (if id-exists?   ;;if true, replaces existing; false adds new icard
      (swap! *appdb* assoc-in [icard-idx id] icard)
      (swap! *appdb* update-in [icard-idx] assoc id icard)))
  nil)

(defn db->appdb
  "copy icard (if found) from (persistent) db to appdb"
  [iid]
  (let [icard (db->icard iid)
	not-found? (and
		    (nil? (:ttxt icard)) (nil? (:btxt icard)))]
    (if not-found?
      (println "ERROR: card with iid =" iid "not found")
      (do
;	(println "Storing" iid)
	(icard->appdb icard) ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ACCESSING ICARDS AND THEIR DATA
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn icard-field
  "given icard, get value of field named field-key (e.g.,:cid)"
  [icard field-key]
  (field-key icard))

(defn iid->icard  ;; aka "lookup-icard" (from appdb)
  "given its id, retrieve an icard from the appdb"
  [id]
  (let [icard-idx   *icard-idx*]
    (get-in @*appdb* [icard-idx id])))

(defn appdb->all-iids
  "return a seq of all the id values of the appdb icard database"
  []
  (let [icard-idx   *icard-idx*]
    (keys (get-in @*appdb* [icard-idx]))))

(defn icard-db-size
  "number of icards in the application's internal icard db"
  []
  (count (keys (nth @*appdb* 0))))

(defn slip->icard
  "given a slip id, return its icard from the appdb"
  [slip]
  ;;the :iid field of the slip contains the id of the corresp. icard
  (iid->icard (:iid slip)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; APPDB: populating it with slips
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; TODO: unit tests for this fcn
(defn slip->appdb
  "Stores the slip record in the in-memory database"
  [slip]
  (let [id (:id slip)
	slip-idx   *slip-idx*
	id-exists?  (get-in @*appdb* [slip-idx id])]
    (if id-exists?   ;;if true, replaces existing; false adds new slip
      (swap! *appdb* assoc-in [slip-idx id] slip)
      (swap! *appdb* update-in [slip-idx] assoc id slip)))
  nil)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ACCESSING SLIPS AND THEIR DATA
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sid->slip  ;; aka "lookup-slip" (from appdb)
  "given its id, retrieve a slip from the appdb"
  [id]
  (let [slip-idx   *slip-idx*]
    (get-in @*appdb* [slip-idx id])))

(defn appdb->all-sids
  "return a seq of all the id values of the appdb slip database"
  []
  (let [slip-idx   *slip-idx*]
    (keys (get-in @*appdb* [slip-idx]))))

;; TODO needs a test in slips.clj
(defn slip-field
  "given slip, get value of field named field-key (e.g.,:cid)"
  [slip field-key]
  (cond (contains? #{:id :iid :pobj} field-key)   (field-key slip)
	(contains? #{:id :ttxt :btxt} field-key)
	(let [icard (iid->icard (:iid slip))] ;;executed for icard fields
	  (icard-field icard field-key))
	;; eg, (. <pobject> :getX) is same as (.getX <pobject>)
	:else (. (:pobj slip) field-name) ))

(defn slip-pobj
  "given a slip & its position, return its Piccolo infocard"
  [slip x y]
  (let [ttxt (slip-field slip :ttxt)
	btxt (slip-field slip :btxt)]
;    (swank.core/break)
    (infocard x y ttxt btxt)))

(defn position-to
  "move a slip's Piccolo infocard to a given location; returns: slip"
  [slip x y]
  (let [pobj   (:pobj slip)]
    (.setX pobj x)
    (.setY pobj y))
  slip)
  
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; DISPLAYING SLIPS ON THE DESKTOP
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; InfWb is largely about slips. If a function does say what it is operating
;; on, it is probably doing so on a slip. Omitting mention of a slip in a
;; function name is a way of keeping code succinct.

(defn show
  "display a slip at a given location in a given layer"
  [slip   x y   layer]
  (.addChild layer (position-to slip x y layer)))

(defn show-seq
  "display seq of slips, starting at (x y), using dx, dy as offset
for each next slip to be displayed"
  [slip-seq   x y   dx dy   layer]
  (let [x-coords   (iterate x #(+ % dx))
	y-coords   (iterate y #(+ % dy))]
    (map show slip-seq x-coords y-coords layer)))