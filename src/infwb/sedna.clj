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
;;
;; IMPLEMENTATION DETAILS
;;
;;   The permanent XML database being used is Sedna (see sedna.org).
;;   Multiple permDB connections can be made, but there is only one
;;   infocard (aka icard) database within Sedna; it is defined by
;;   global variables *icard-db-name* and *icard-coll-name*
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; GLOBAL VARIABLES
;
(def ^{:dynamic true} *icard-connection* (SednaXQDataSource.)) 
(def ^{:dynamic true} *icard-coll-name*)
(def ^{:dynamic true} *icard-db-name*)

;; icdata db is 0th element of @*localDB*
(def ^{:dynamic true} *icdata-idx*   0)
  
;; sldata db is 1st element of @*localDB*
(def ^{:dynamic true} *sldata-idx*    1)
  
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
  "Performs roughly the same task as the UNIX `ls`.  That is, returns a seq
of the filenames at a given directory.  If a path to a file is supplied,
then the seq contains only the original path given."
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

(defn set-connector-db
  "Used before any XQuery operation to specify the database to be used

From Sedna's point of view, the database to be used is given by the
databaseName property of the SednaConnection named"
  [connection-name db-name]
  (doto connection-name
    (.setProperty "serverName"   "localhost")
    (.setProperty "databaseName"   db-name)))

(defn set-icard-db-name
  "Used only before InfWb-specific XQuery queries to specify the
permDB database to be used"
  [name]
  (def *icard-db-name*   name))

(defn set-icard-coll-name
  "Used only before InfWb-specific XQuery queries to specify the
collection to be used"
  [name]
  (def *icard-coll-name*   name))

(defn clear-localDB []
    (def ^{:dynamic true} *localDB* (atom [{} {}])))

(defn icard-db-startup
  "does all database setup for current session of work; should be
executed once; WARNING: deletes the database of icdatas and sldatas"
  [icard-db-name icard-coll-name]
  
  (set-connector-db *icard-connection* icard-db-name)
  (set-icard-db-name icard-db-name)
  (set-icard-coll-name icard-coll-name)
  )

(defn reset-icards-db
  "Clears out the icards-db in localDB (helpful for testing)"
  []
  (swap! *localDB* assoc-in [*icdata-idx*] {})
  nil)
  
(defn reset-slips-db
  "Clears out the sldata-db in localDB (helpful for testing)"
  []
  (swap! *localDB* assoc-in [*sldata-idx*] {})
  nil)
  


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; DATABASE ACCESS USING XQUERY
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;insights taken from ~/tech/schemestuff/InfWb/main/sedna-utilities.ss

(declare get-result)

;; for convenience, this ns has already defined the Sedna
;; connection named *icard-connection*
(defn run-XQuery
  "runs specified XQuery query through the given connection,
returning result(s) in a vector, within given database and collection

This function is IMPLEMENTATION-DEPENDENT. It assumes that the Sedna XML
database (http://www.sedna.org/) is running.

The appropriate collection name, if any, must be part of query-str"
  
  [query-str connection]
  (let [conn (.getConnection connection "SYSTEM" "MANAGER")
	xqe   (.createExpression conn)
	rs   (.executeQuery xqe query-str)
	result (get-result rs)]
    (.close conn)
    result))

(defn run-infoml-query
  "Returns results of an InfoML query

filter arg selects records; result arg extracts data from selected records
(current infoml element is held in variable $card).  Hardwired to use
*icard-connection* and *icard-coll-name*"
  [filter result]

  (let [infoml-query
	(str
	 "declare default element namespace 'http://infoml.org/infomlFile';\n"
	 "for $card in collection('"
	 *icard-coll-name*
	 "')/infomlFile/"
	 filter "\n"
	 "return " result)]
    (run-XQuery infoml-query *icard-connection*)))

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

(def ^{:dynamic true} *icdata-fields* (list :icard :ttxt :btxt :tags))

(declare icdata-field)

(defn- get-all-fields
  "returns list of all the infocard's fields"
  [icdata]
  (map #(icdata-field icdata %) *icdata-fields*))

(defn new-icdata
  [icard ttxt btxt tags]
  ;; icard will never change, so no need for using an atom
  (icdata. icard (atom ttxt) (atom btxt) (atom tags)))

(defn valid-from-permDB?
  "returns false if icdata is the result of asking for the data of a
icard that does not exist in the permanent database; else returns true"
  [icdata]
  (let [result
	(or @(:ttxt icdata) @(:btxt icdata) (> 0 (count @(:tags icdata))))]
    (if (= result false)
      false
      true)))

(defn valid-from-localDB?
  "returns false if icdata is the result of asking for the data of a
icard that does not exist in the local database; else returns true"
  [icdata]
  (if (= icdata nil)
    false
    true))

(defn permDB->icdata
  "returns icdata record from permDB; if nil, icard was invalid"
  [icard]
  (let [data-vec 
	(run-infoml-query (str "infoml[@cardId = '" icard "']")
			  "($card/data/title/string(), $card/data/content/string(), $card/selectors/tag/string())")]

    ; At this point, if the icard *was* found in permDB, data-vec contains
    ; the expected contents: title-string in position 0, body-string in
    ; position 1, and zero or more tags in the rest of the vector
    ;
    ; If the icard was *not* found, data-vec contains an empty vector
    ; (i.e., [])
    
    (if (= 0 (count data-vec))
      (doall
       (println "WARNING: '" icard
		"' is not a valid infocard (from permDB->icdata)")
       nil)
      (new-icdata icard (get data-vec 0)
		  (get data-vec 1)
		  (drop 2 data-vec)))))

(defn permDB->all-icards
  "from permanent database, get seq of all icards"
  []
  ;; assumes that position 1 contains the file's "all-pointers" record,
  ;; which is not an end-user "actual" infocard; this assumption
  ;; may change in the future
  (run-infoml-query "infoml[position() != 1]"
		    "$card/@cardId/string()"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; SLDATAS: creating
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord sldata [slip	 ;;string; id of sldata
		   icard ;;string; card-id of icdata to be displayed
		   pobj] ;;atom to Piccolo object that implements sldata
  )

(declare localDB->icdata)

(defn new-sldata
  "create sldata from infocard, with its pobj at (x y), or default to (0 0)--
NOTE: does *not* add sldata to *localDB*"
  ([icard x y]
  (let [icdata (localDB->icdata icard)
	rand-key   (rand-kayko 3)
	pobj   (make-pinfocard
		x
		y
		(icdata-field icdata :ttxt)
		(icdata-field icdata :btxt))]
    (sldata. rand-key icard (atom pobj))))
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
  "Stores the icdata record in the localDB; returns icdata"
  [icdata]
  (let [icard (:icard icdata)
	icdata-idx   *icdata-idx*
	id-exists?  (get-in @*localDB* [icdata-idx icard])]
    (if id-exists?   ;;if true, replaces existing; false adds new icdata
      (swap! *localDB* assoc-in [icdata-idx icard] icdata)
      (swap! *localDB* update-in [icdata-idx] assoc icard icdata)))
  icdata)

(defn permDB->localDB
  "copy icdata (if found) from (persistent) db to localDB"
  [icard]
  (let [icdata (permDB->icdata icard)]
    (if (valid-from-permDB? icdata)
      (icdata->localDB icdata)
      (println "ERROR: card with id =" icard "not found")
      )))

(defn load-icard-seq-to-localDB
  "populates *localDB* with infocards given by the sequence"
  [icard-seq]
      (doseq [icard icard-seq]
	(permDB->localDB icard)))

(defn load-all-infocards
  "loads all infocards in permanentDB to localDB"
  []
  (load-icard-seq-to-localDB (permDB->all-icards)))



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
    ;; then branch--icard stored directly
    (:icard icdata)
    ;; else branch--all others stored w/atoms, must be dereferenced
    @(field-key icdata)))

(defn localDB->icdata  ;; aka "lookup-icdata" (from localDB)
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

(defn slip->icdata
  "given a slip, return its icdata from the localDB"
  [slip]
  ;;the :icard field of the sldata contains the id of the corresp. icdata
  (localDB->icdata (:icard slip)))

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
  (let [all-icards (permDB->all-icards)]
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
      {:slip  (str "ERROR: Sldata '" slip "' is INVALID")
       :icard (str "ERROR: Sldata '" slip "' is INVALID")
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

  (cond   (contains? #{:slip :icard} field-key)
	  ;; these fields are stored as themselves
	  (field-key sldata)

	  (contains? #{:pobj} field-key)
	  ;; these return atoms to the datum we actually want
	  @(field-key sldata)
	
	  (contains? #{:ttxt :btxt :tags} field-key)

	  ;;executed for icdata fields
	  (let [icdata (localDB->icdata (sldata-field sldata :icard))] 
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

(defn slip-localDB-size
  "number of icards in the application's internal icdata db"
  []
  (count (keys (nth @*localDB* *sldata-idx*))))

(defn move-to
  "move a slip's Piccolo infocard to a given location; returns: sldata"
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
; DISPLAYING SLIPS ON THE DESKTOP
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
  "display seq of slips, starting at (x y), using dx, dy as offset
for each next slip to be displayed"
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