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
executed once; WARNING: deletes the database of icdatas and slips"
  []
  ;WARNING - RE-EXECUTING THIS DELETES ICDATA DATABASE
  (def ^{:dynamic true} *icdata-idx*   0) ;;icdata db is 0th element of @*appdb*
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

;; One important characteristic of the icdata "section" of \*appdb\* (itself a
;; map) is that the value of the id field of the icdata is also the key
;; of that map.

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ICARDS: creating
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord icdata [icard    ;;string; id of infocard
		  ttxt   ;;atom pointing to string; title text
		  btxt]  ;;atom pointing to string; body text
  )

(def ^{:dynamic true} *icdata-fields* (list :icard :ttxt :btxt))

(defn new-icdata [icard ttxt btxt]
  (icdata. icard (atom ttxt) (atom btxt)))

(defn db->icdata
  "get icdata data from appn database, return it as an icdata record"
  [icard]
  (let [data-vec 
	(run-db-query (str "infoml[@cardId = '" icard "']")
		 "($card/data/title/string(), $card/data/content/string())")]
    (new-icdata icard (get data-vec 0) (get data-vec 1))))

(defn db->all-icards
  "from Sedna database, get seq of all icdata IDs"
  []
  ;; assumes that position 1 contains the file's "all-pointers" record,
  ;; which is not an end-user "actual" infocard; this assumption
  ;; may change in the future
  (run-db-query "infoml[position() != 1]" "$card/@cardId/string()"))

(declare icdata-field)

(defn get-all-fields
  "returns list of all the infocard's fields"
  [icdata]
  (map #(icdata-field icdata %) *icdata-fields*))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; SLIPS: creating
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord slip [sid      ;;string; id of slip
		 icard     ;;string; card-id of icdata to be displayed
		 pobj]   ;;Piccolo object that implements slip
  )

(declare get-icdata)

(defn new-slip
  "create slip from infocard, with its pobj at (x y), or default to (0 0)--
NOTE: does *not* add slip to *appdb*"
  ([icard x y]
  (let [icdata (get-icdata icard)
	; this does nothing, for now
;	icdata-field-list (get-all-fields icdata)
	rand-key   (rand-kayko 3)
	pobj   (make-pinfocard
		x
		y
		(icdata-field icdata :ttxt)
		(icdata-field icdata :btxt))]
    (slip. rand-key (atom icard) (atom pobj))))
  ([icard]   (new-slip icard 0 0 )))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; APPDB: populating it with icdatas
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

(defn icdata->appdb
  "Stores the icdata record in the in-memory database"
  [icdata]
  (let [icard (:icard icdata)
	icdata-idx   *icdata-idx*
	id-exists?  (get-in @*appdb* [icdata-idx icard])]
    (if id-exists?   ;;if true, replaces existing; false adds new icdata
      (swap! *appdb* assoc-in [icdata-idx icard] icdata)
      (swap! *appdb* update-in [icdata-idx] assoc icard icdata)))
  nil)

(defn db->appdb
  "copy icdata (if found) from (persistent) db to appdb"
  [icard]
  (let [icdata (db->icdata icard)
	not-found? (and
		    (nil? (:ttxt icdata)) (nil? (:btxt icdata)))]
    (if not-found?
      (println "ERROR: card with id =" icard "not found")
      (do
;	(println "Storing" icard)
	(icdata->appdb icdata) ))))

(defn load-icard-seq-to-appdb
  "populates *appdb* with infocards given by the sequence"
  [icard-seq]
    (let [all-icards icard-seq]
      (doseq [icard all-icards]
	(db->appdb icard))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ACCESSING ICARDS AND THEIR DATA
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn icdata-field
  "given icdata, get value of field named field-key (e.g.,:cid)"
  [icdata field-key]
  ;this fcn isolates the operation from its implementation
  (if (= field-key :icard)
    (:icard icdata)
    @(field-key icdata)))

(defn get-icdata  ;; aka "lookup-icdata" (from appdb)
  "given its id (the 'icard' variable), retrieve an icdata from the appdb"
  [icard]
    (get-in @*appdb* [*icdata-idx* icard]))

(defn appdb->all-icards
"return a seq of all the id values of the appdb icdata database"
  []
    (keys (get-in @*appdb* [*icdata-idx*])))

(defn icdata-appdb-size
  "number of icdatas in the application's internal icdata db"
  []
  (count (keys (nth @*appdb* *icdata-idx*))))

(defn slip->icdata
  "given a slip id, return its icdata from the appdb"
  [slip]
  ;;the :icard field of the slip contains the id of the corresp. icdata
  (get-icdata (:icard slip)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; APPDB: populating it with slips    
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Definition of '(swap! atom f x y & args)':
;; Atomically swaps the value of atom to be:
;; (apply f current-value-of-atom args). Note that f may be called
;; multiple times, and thus should be free of side effects. Returns
;; the value that was swapped in.

;; Explanations of two lines of code within slip->appdb:

;; `(swap! *appdb* assoc-in [*slip-idx* sid] slip)` is equiv to
;; `(apply assoc-in <curr value of *appdb*> [*slip-idx* sid] slip)`,
;; then assigning the new value back to *appdb*.

;; Let KEY = `(nth *appdb* *slip-idx* sid)`, which is the key `sid`
;; within the map for slips.

;; The above swap...assoc-in line takes the value `slip` and uses it
;; to *replace* the value associated with KEY. 

;; -----
;; `(swap! *appdb* update-in [*slip-idx*] assoc sid slip)` is equiv to
;; `(apply update-in <curr value of *appdb*> [*slip-idx*] assoc sid slip)`,
;; then assigning the new value back to *appdb*.

;; Let VAL = `(nth *appdb* *slip-idx*)`, which is the map for slips.

;; The above swap...update-in line performs the following action:

;; `assoc VAL sid slip`, which *prepends* the key/value pair to VAL.

(defn slip->appdb
  "Stores the slip record in the in-memory database--NOTE: new slip is
inserted at the *front* of the map, *before* all existing slips"
  [slip]
  (let [sid (:sid slip)
	id-exists?  (get-in @*appdb* [*slip-idx* sid])]
    (if id-exists?   ;;if true, replaces existing; false adds new slip
      (swap! *appdb* assoc-in [*slip-idx* sid] slip)
      (swap! *appdb* update-in [*slip-idx*] assoc sid slip)))
  nil)

;; WARN: "bare" use of `:sid` to get data from within slip
(defn icard->slip->appdb
  "Given id (= icard), creates slip, adds slip to *appdb*; returns sid of new slip"
  [icard]
  (let [slip (new-slip icard)
	sid (:sid slip)]
    (slip->appdb slip)
    sid))

(defn load-all-slips-to-appdb []
  (let [all-icards (db->all-icards)]
    (doseq [icard all-icards]
      (icard->slip->appdb icard))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ACCESSING SLIPS AND THEIR DATA
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-slip  ;; aka "lookup-slip" (from appdb)
  "given its sid, retrieve a slip from the appdb"
  [sid]
  (let [slip   (get-in @*appdb* [*slip-idx* sid])]
    (if slip
      slip
      {:sid (str "ERROR: Slip '" sid "' is INVALID")
       :icard (atom (str "ERROR: Slip '" sid "' is INVALID"))
       :pobj  (atom nil)} )))

(defn appdb->all-sids
  "return a seq of all the id values of the appdb slip database"
  []
    (keys (get-in @*appdb* [*slip-idx*])))

;; TODO needs a test in slips.clj
(defn slip-field
  "given slip, get value of field named field-key (e.g.,:cid)"
  [slip field-key]
;  (swank.core/break)
  (cond   (= :sid field-key)
	  (:sid slip)
	  
	  (contains? #{:icard :pobj} field-key)
	  @(field-key slip) 
	
	  (contains? #{:ttxt :btxt} field-key)
	  (let [icdata (get-icdata (slip-field slip :icard))] ;;executed for icdata fields
;	  (let [icdata (get-icdata (:icard slip))] ;;executed for icdata fields
	    (icdata-field icdata field-key))
	
	  ;; icards are "moved" by changing their transform
	  ;; getXOffset, getYOffset access the transform's values directly,
	  ;; eliminating need to xform local X, Y (always 0 0) to globl coords
	  (= :x field-key)
	  (let [pobj (slip-field slip :pobj)]
	    (.getXOffset pobj))

	  (= :y field-key)
	  (let [pobj (slip-field slip :pobj)]
	    (.getYOffset pobj))
	  
	  ))

(defn slip-appdb-size
  "number of icards in the application's internal icdata db"
  []
  (count (keys (nth @*appdb* *slip-idx*))))

(defn move-to
  "move a slip's Piccolo infocard to a given location; returns: slip"
  [slip   ^Float x   ^Float y]
  (let [pobj   @(:pobj slip)
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