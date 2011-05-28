; project: github/gw666/infwb
; file: src/infwb/sedna.clj

;; see http://www.cfoster.net/articles/xqj-tutorial/simple-xquery.xml

(ns infwb.sedna
  (:gen-class)
  (:import (javax.xml.xquery   XQConnection XQDataSource
			       XQResultSequence)
	   (net.cfoster.sedna.xqj   SednaXQDataSource)
	   (java.util   Properties)
	   (java.awt.geom AffineTransform))
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

(defrecord icard [iid     ;;string; icard-id (iid) of infocard
		  ttxt   ;;string; title text
		  btxt]  ;;string; body text
  )

(def ^{:dynamic true} *icard-fields* (list :iid :ttxt :btxt))

(defn new-icard [iid ttxt btxt]
  (icard. iid ttxt btxt))

(defn db->icard
  "get icard data from appn database, return it as an icard record"
  [iid]
  (let [data-vec 
	(run-db-query (str "infoml[@cardId = '" iid "']")
		 "($card/data/title/string(), $card/data/content/string())")]
    (new-icard iid (get data-vec 0) (get data-vec 1))))

(defn db->all-iids
  "from Sedna database, get seq of all icard IDs"
  []
  ;; assumes that position 1 contains the file's "all-pointers" record,
  ;; which is not an end-user "actual" infocard; this assumption
  ;; may change in the future
  (run-db-query "infoml[position() != 1]" "$card/@cardId/string()"))

(declare icard-field)

(defn get-all-fields
  "returns list of all the infocard's fields"
  [icard]
  (map #(icard-field icard %) *icard-fields*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; SLIPS: creating
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord slip [sid      ;;string; id of slip
		 iid     ;;string; card-id of icard to be displayed
		 pobj]   ;;Piccolo object that implements slip
  )

(declare get-icard)

(defn new-slip
  "create slip from infocard, with its pobj at (x y), or default to (0 0)"
  ([iid x y]
  (let [icard (get-icard iid)
	; this does nothing, for now
;	icard-field-list (get-all-fields icard)
	rand-key   (str "sl:" (rand-kayko 3))
	pobj   (make-pinfocard
		x
		y
		(icard-field icard :ttxt)
		(icard-field icard :btxt))]
    (slip. rand-key iid pobj)))
  ([iid]   (new-slip iid 0 0 )))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; APPDB: populating it with icards
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; There is currently some confusion between an instance of an icard
; record--called 'icard' in source code--and the id value--the 'iid',
; or "icard ID".
;
; In general, we want the system to use values that it sees as infocards,
; without knowing what is "inside" the value. Infocards are operated on
; by various functions in an implementation-independent way, so that if
; I decide to change the implementation, none of the code "above" the
; implementation level need be modified.
; 
; Current implementation details: an infocard's value is its iid; to get
; its icard record, use (get-icard iid); to get an icard record's fields,
; use (icard-field icard :fieldname).
;
; NB: Slips are similar, with (get-slip sid), (slip-field slip :fieldname)
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn icard->appdb
  "Stores the icard record in the in-memory database"
  [icard]
  (let [iid (:iid icard)
	icard-idx   *icard-idx*
	id-exists?  (get-in @*appdb* [icard-idx iid])]
    (if id-exists?   ;;if true, replaces existing; false adds new icard
      (swap! *appdb* assoc-in [icard-idx iid] icard)
      (swap! *appdb* update-in [icard-idx] assoc iid icard)))
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

(defn load-all-icards-to-appdb
  "populates *appdb* with infocards; should be done once only"
  []
    (let [all-iids (db->all-iids)]
      (doseq [iid all-iids]
	(db->appdb iid))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ACCESSING ICARDS AND THEIR DATA
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn icard-field
  "given icard, get value of field named field-key (e.g.,:cid)"
  [icard field-key]
  ;this fcn isolates the operation from its implementation
  (field-key icard))

(defn get-icard  ;; aka "lookup-icard" (from appdb)
  "given its iid, retrieve an icard from the appdb"
  [iid]
  (let [icard-idx   *icard-idx*]
    (get-in @*appdb* [icard-idx iid])))

(defn appdb->all-iids
"return a seq of all the id values of the appdb icard database"
  []
  (let [icard-idx   *icard-idx*]
    (keys (get-in @*appdb* [icard-idx]))))

(defn icard-appdb-size
  "number of icards in the application's internal icard db"
  []
  (count (keys (nth @*appdb* 0))))

(defn slip->icard
  "given a slip id, return its icard from the appdb"
  [slip]
  ;;the :iid field of the slip contains the id of the corresp. icard
  (get-icard (:iid slip)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; APPDB: populating it with slips    
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; TODO: unit tests for this fcn
(defn slip->appdb
  "Stores the slip record in the in-memory database"
  [slip]
  (let [sid (:sid slip)
	slip-idx   *slip-idx*
	id-exists?  (get-in @*appdb* [slip-idx sid])]
    (if id-exists?   ;;if true, replaces existing; false adds new slip
      (swap! *appdb* assoc-in [slip-idx sid] slip)
      (swap! *appdb* update-in [slip-idx] assoc sid slip)))
  nil)

(defn iid->slip->appdb
  "Given iid, creates slip, then adds slip to *appdb*"
  [iid]
  (let [slip (new-slip iid)]
    (slip->appdb slip)))

(defn load-all-slips-to-appdb []
  (let [all-iids (db->all-iids)]
    (doseq [iid all-iids]
      (iid->slip->appdb iid))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ACCESSING SLIPS AND THEIR DATA
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-slip  ;; aka "lookup-slip" (from appdb)
  "given its sid, retrieve a slip from the appdb"
  [sid]
  (let [slip-idx   *slip-idx*]
    (get-in @*appdb* [slip-idx sid])))

(defn appdb->all-sids
  "return a seq of all the id values of the appdb slip database"
  []
  (let [slip-idx   *slip-idx*]
    (keys (get-in @*appdb* [slip-idx]))))

;; TODO needs a test in slips.clj
(defn slip-field
  "given slip, get value of field named field-key (e.g.,:cid)"
  [slip field-key]
  (cond (contains? #{:sid :iid :pobj} field-key)   (field-key slip)
	(contains? #{:id :ttxt :btxt} field-key)
	(let [icard (get-icard (:iid slip))] ;;executed for icard fields
	  (icard-field icard field-key))
	;; eg, (. <pobject> :getX) is same as (.getX <pobject>)
	:else (. (:pobj slip) field-name) ))

;; (defn move-to
;;   "move a slip's Piccolo infocard to a given location; returns: slip"
;;   [slip   ^Double x   ^Double y]
;;   (let [pobj   (:pobj slip)
;; 	at     (.AffineTranform (Double. 1.0) (Double. 0.0)
;; 				(Double. 0.0) (Double. 1.0) x y)]
;;     (.setTransform pobj at))
;;   slip)
  
(defn move-to
  "move a slip's Piccolo infocard to a given location; returns: slip"
  [slip   ^Float x   ^Float y]
  (let [pobj   (:pobj slip)
	at1    (AffineTransform. 1. 0. 0. 1. x y)]
    (.setTransform pobj at1))
  slip)
  

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; DISPLAYING SLIPS ON THE DESKTOP
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; InfWb is largely about slips. If a function does not say what it is 
;; operating on, it is probably doing so on a slip. Omitting mention of 
;; a slip in a function name is a way of keeping code succinct.

(defn show
  "display a slip at a given location in a given layer"
  ; BUG: move-to moves the PClip but not its contents
  [slip   x y   layer]
  (let [_   (move-to slip x y)
	pobj   (slip-field slip :pobj)]
    (println "Drawing slip " (slip-field slip :sid) "at (" x " " y ")")
    (.addChild layer pobj)))

(defn show-seq
  "display seq of slips, starting at (x y), using dx, dy as offset
for each next slip to be displayed"
  [slip-seq   x y   dx dy   layer]
  (println "Reached show-seq, x y = " x " " y)
  (let [x-coords   (iterate #(+ % dx) x)
	y-coords   (iterate #(+ % dy) y)
        layer-seq  (repeat layer)]
;    (dorun
     (map show slip-seq x-coords y-coords layer-seq))
;    )
  )