;; see http://www.cfoster.net/articles/xqj-tutorial/simple-xquery.xml

(ns infwb.sedna
  (:gen-class)
  (:import (javax.xml.xquery   XQConnection XQDataSource
			       XQResultSequence)
	   (net.cfoster.sedna.xqj   SednaXQDataSource)
	   (java.util   Properties)
	   (java.awt.geom AffineTransform))
    (:use [infwb   slip-display]))


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
executed once; WARNING: deletes the database of icdatas and sldatas"
  []
  ;WARNING - RE-EXECUTING THIS DELETES ICDATA DATABASE
  (def ^{:dynamic true} *icdata-idx*   0) ;;icdata db is 0th element of @*localDB*
  (def ^{:dynamic true} *sldata-idx*    1) ;;sldata db is 1st element of @*localDB*
  (def ^{:dynamic true} *localDB* (atom [{} {}]))
  
  (def ^{:dynamic true} *xqs* (SednaXQDataSource.)) ;naughty; OK for development
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ICARDS: creating
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord icdata [icard ;;string; id of infocard
		   ttxt	 ;;atom pointing to string; title text
		   btxt  ;;atom pointing to string; body text
		   tags] ;;atom pointing to vector of tag strings
  )

;; (defrecord icdata [icard    ;;string; id of infocard
;; 		  ttxt   ;;atom pointing to string; title text
;; 		  btxt]  ;;atom pointing to string; body text
;;   )

(def ^{:dynamic true} *icdata-fields* (list :icard :ttxt :btxt :tags))

(declare icdata-field)

(defn- get-all-fields
  "returns list of all the infocard's fields"
  [icdata]
  (map #(icdata-field icdata %) *icdata-fields*))

(defn new-icdata
  [icard ttxt btxt tags]
  (icdata. icard (atom ttxt) (atom btxt) (atom tags)))

(defn db->icdata
  "returns icdata record from localDB"
  [icard]
  (let [data-vec 
	(run-db-query (str "infoml[@cardId = '" icard "']")
		 "($card/data/title/string(), $card/data/content/string(), $card/selectors/tag/string())")]
    (new-icdata icard (get data-vec 0)
		(get data-vec 1)
		(drop 2 data-vec) )))

(defn db->all-icards
  "from permanent database, get seq of all icards"
  []
  ;; assumes that position 1 contains the file's "all-pointers" record,
  ;; which is not an end-user "actual" infocard; this assumption
  ;; may change in the future
  (run-db-query "infoml[position() != 1]" "$card/@cardId/string()"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; SLDATAS: creating
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord sldata [slip      ;;string; id of sldata
		 icard     ;;string; card-id of icdata to be displayed
		 pobj]   ;;Piccolo object that implements sldata
  )

(declare get-icdata)

(defn new-sldata
  "create sldata from infocard, with its pobj at (x y), or default to (0 0)--
NOTE: does *not* add sldata to *localDB*"
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
    (sldata. rand-key (atom icard) (atom pobj))))
  ([icard]   (new-sldata icard 0 0 )))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; LOCALDB: populating it with icdatas
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; The data defining an infocard within InfWb is in an icdata (InfoCard DATA)
; record. This data is a very small subset of all the info that exists
; in an infoml (XML) element.

; A slip is a visual representation of an icard. There can be multiple
; slips for a given icard. The data defining a slip is in a sldata 
; (SLip DATA) record.
;
; Currently, an infocard, or icard, is defined by the icard field of
; the icdata record that represents the actual infocard.
;
; Similarly, a slip is defined by the slip field of the icdata record 
; that represents the actual infocard.
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn icdata->localDB
  "Stores the icdata record in the localDB"
  [icdata]
  (let [icard (:icard icdata)
	icdata-idx   *icdata-idx*
	id-exists?  (get-in @*localDB* [icdata-idx icard])]
    (if id-exists?   ;;if true, replaces existing; false adds new icdata
      (swap! *localDB* assoc-in [icdata-idx icard] icdata)
      (swap! *localDB* update-in [icdata-idx] assoc icard icdata)))
  nil)

(defn db->localDB
  "copy icdata (if found) from (persistent) db to localDB"
  [icard]
  (let [icdata (db->icdata icard)
	not-found? (and
		    (nil? (:ttxt icdata)) (nil? (:btxt icdata)))]
    (if not-found?
      (println "ERROR: card with id =" icard "not found")
      (do
;	(println "Storing" icard)
	(icdata->localDB icdata) ))))

(defn load-icard-seq-to-localDB
  "populates *localDB* with infocards given by the sequence"
  [icard-seq]
      (doseq [icard icard-seq]
	(db->localDB icard)))

(defn load-all-infocards
  "loads all infocards in permanentDB to localDB"
  []
  (load-icard-seq-to-localDB (db->all-icards)))



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

(defn get-icdata  ;; aka "lookup-icdata" (from localDB)
  "given its id (the 'icard' variable), retrieve an icdata from the localDB"
  [icard]
    (get-in @*localDB* [*icdata-idx* icard]))

(defn localDB->all-icards
"return a seq of all the id values of the localDB icdata database"
  []
    (keys (get-in @*localDB* [*icdata-idx*])))

(defn icdata-localDB-size
  "number of icdatas in the application's internal icdata db"
  []
  (count (keys (nth @*localDB* *icdata-idx*))))

(defn sldata->icdata
  "given a sldata id, return its icdata from the localDB"
  [sldata]
  ;;the :icard field of the sldata contains the id of the corresp. icdata
  (get-icdata (:icard sldata)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; LOCALDB: populating it with sldatas    
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Definition of '(swap! atom f x y & args)':
;; Atomically swaps the value of atom to be:
;; (apply f current-value-of-atom args). Note that f may be called
;; multiple times, and thus should be free of slipe effects. Returns
;; the value that was swapped in.

;; Explanations of two lines of code within sldata->localDB:

;; `(swap! *localDB* assoc-in [*sldata-idx* slip] sldata)` is equiv to
;; `(apply assoc-in <curr value of *localDB*> [*sldata-idx* slip] sldata)`,
;; then assigning the new value back to *localDB*.

;; Let KEY = `(nth *localDB* *sldata-idx* slip)`, which is the key `slip`
;; within the map for sldatas.

;; The above swap...assoc-in line takes the value `sldata` and uses it
;; to *replace* the value associated with KEY. 

;; -----
;; `(swap! *localDB* update-in [*sldata-idx*] assoc slip sldata)` is equiv to
;; `(apply update-in <curr value of *localDB*> [*sldata-idx*] assoc slip sldata)`,
;; then assigning the new value back to *localDB*.

;; Let VAL = `(nth *localDB* *sldata-idx*)`, which is the map for sldatas.

;; The above swap...update-in line performs the following action:

;; `assoc VAL slip sldata`, which *prepends* the key/value pair to VAL.

(defn sldata->localDB
  "Stores the sldata record in the in-memory database; NOTE: new sldata is
inserted at the *front* of the map, *before* all existing sldatas"
  [sldata]
  (let [slip (:slip sldata)
	id-exists?  (get-in @*localDB* [*sldata-idx* slip])]
    (if id-exists?   ;;if true, replaces existing; false adds new sldata
      (swap! *localDB* assoc-in [*sldata-idx* slip] sldata)
      (swap! *localDB* update-in [*sldata-idx*] assoc slip sldata)))
  nil)

;; WARN: "bare" use of `:slip` to get data from within sldata
(defn icard->sldata->localDB
  "Given id (= icard), creates sldata, adds sldata to *localDB*; returns slip of new sldata"
  [icard]
  (let [sldata (new-sldata icard)
	slip (:slip sldata)]
    (sldata->localDB sldata)
    slip))

(defn load-all-sldatas-to-localDB []
  (let [all-icards (db->all-icards)]
    (doseq [icard all-icards]
      (icard->sldata->localDB icard))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ACCESSING SLDATAS AND THEIR DATA
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-sldata  ;; aka "lookup-sldata" (from localDB)
  "given its slip, retrieve a sldata from the localDB"
  [slip]
  (let [sldata   (get-in @*localDB* [*sldata-idx* slip])]
    (if sldata
      sldata
      {:slip (str "ERROR: Sldata '" slip "' is INVALID")
       :icard (atom (str "ERROR: Sldata '" slip "' is INVALID"))
       :pobj  (atom nil)} )))

(defn localDB->all-slips
  "return a seq of all the id values of the localDB sldata database"
  []
    (keys (get-in @*localDB* [*sldata-idx*])))

;; TODO needs a test in sldatas.clj
(defn sldata-field
  "given sldata, get value of field named field-key (e.g.,:cid)"
  [sldata field-key]
;  (swank.core/break)
  (cond   (= :slip field-key)
	  (:slip sldata)
	  
	  (contains? #{:icard :pobj} field-key)
	  @(field-key sldata) 
	
	  (contains? #{:ttxt :btxt} field-key)
	  (let [icdata (get-icdata (sldata-field sldata :icard))] ;;executed for icdata fields
;	  (let [icdata (get-icdata (:icard sldata))] ;;executed for icdata fields
	    (icdata-field icdata field-key))
	
	  ;; icards are "moved" by changing their transform
	  ;; getXOffset, getYOffset access the transform's values directly,
	  ;; eliminating need to xform local X, Y (always 0 0) to globl coords
	  (= :x field-key)
	  (let [pobj (sldata-field sldata :pobj)]
	    (.getXOffset pobj))

	  (= :y field-key)
	  (let [pobj (sldata-field sldata :pobj)]
	    (.getYOffset pobj))
	  
	  ))

(defn sldata-localDB-size
  "number of icards in the application's internal icdata db"
  []
  (count (keys (nth @*localDB* *sldata-idx*))))

(defn move-to
  "move a sldata's Piccolo infocard to a given location; returns: sldata"
  [sldata   x   y]
  (let [pobj   @(:pobj sldata)
	dx     (double x)
	dy     (double y)
	at1    (AffineTransform. 1. 0. 0. 1. dx dy)]
;    (swank.core/break)
    (.setTransform pobj at1))
  sldata)
  

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; DISPLAYING SLDATAS ON THE DESKTOP
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; InfWb is largely about sldatas. If a function does not say what it is 
;; operating on, it is probably doing so on a sldata. Omitting mention of 
;; a sldata in a function name is a way of keeping code succinct.

(defn show
  "display a sldata at a given location in a given layer"
  ; BUG: move-to moves the PClip but not its contents
  [sldata   x y   layer]
  (let [_   (move-to sldata (float x) (float y))
	pobj   (sldata-field sldata :pobj)]
    (.addChild layer pobj)))

(defn show-seq
  "display seq of sldatas, starting at (x y), using dx, dy as offset
for each next sldata to be displayed"
  [sldata-seq   x y   dx dy   layer]
  (println "Reached show-seq, x y = " x " " y)
  (let [x-coords   (iterate #(+ % dx) x)
	y-coords   (iterate #(+ % dy) y)
        layer-seq  (repeat layer)]
;    (dorun
;    (swank.core/break)
    (map show sldata-seq x-coords y-coords layer-seq))
;    )
  )