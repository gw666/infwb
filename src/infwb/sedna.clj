;; see http://www.cfoster.net/articles/xqj-tutorial/simple-xquery.xml

(ns infwb.sedna
  (:gen-class)
  (:import (javax.xml.xquery   XQConnection XQDataSource
			       XQResultSequence)
	   (net.cfoster.sedna.xqj   SednaXQDataSource)
	   (java.util   Properties)
	   (java.awt.geom AffineTransform))
  (:use [infwb   slip-display])
  (:require [clojure.string :as str])
  )

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
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; ^{:dynamic true} after 'def' may suppress some error messages
(def *icard-connection* (SednaXQDataSource.)) 
(def *icard-coll-name*)
(def *icard-db-name*)

;; in-memory databases for icdata (icard) and sldata (slip) data
(def  *localDB-icdata* (atom {}))
(def  *localDB-sldata* (atom {}))

;; session-specific mapping of one icard to seq of multiple slips
(def *icard-to-slip-map* (atom {}))

(def *icard-fields* #{:icard :ttxt :btxt :tags})
(def *slip-fields*  #{:slip :icard :pobj})

;; KEY: icard; VALUE: seq of slips that are clones of icard
(def *icard->slips* (atom {}))

;; KEY: slip; VALUE: {attribute-name attr-value, ...)
(def *slip-attrs* (atom {}))


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


(defn new-assoc
  "In the atom-NAS, change the value associated with key. If key not found, 
adds new key-value pair. Work for multiple levels of nesting.

An atom-NAS is an atom that points to a NAS (nested associative structure).
Examples are:       (atom {k1 v1, k2 v2})
               and  (atom [{k1 v1, k2 v2} {k1 v3, k2 v4}])"
  [atom-map vec-of-keys value]
  (swap! atom-map assoc-in vec-of-keys value))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; HOUSEKEEPING
;
; Compiling this file automatically init'zs the two *localDB-xxx* globals
;
; SYSsetup-InfWb: use to set db and coll'n used in InfWb current session 
; 
; SYSrun-query: needs a connection that specifies the db to be used (see
;   SYSnew-connection, SYSset-connection). Using SYSrun-query with its 
;   own connection does _not_ disrupt simultaneous use of icard-specific 
;   queries (i.e., run-infocard-query). Sedna can have multiple dbs open
;   at the same time.

; NOTE: "Session db" is a newer term for "local db." Will renaming ever end?
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reset-icards []
    (reset! *localDB-icdata* {}))

(defn reset-slips []
    (reset! *localDB-sldata* {}))

(defn reset-slip-registry []
    (reset! *icard-to-slip-map* {}))

(defn reset-slip-attributes []
    (reset! *slip-attrs* {}))

(defn SYSclear-all []
  (reset-icards)
  (reset-slips)
  (reset-slip-registry)
  (reset-slip-attributes))

; not used by any other fcns--110901
(defn SYSreset-icard-conn
  "Clears the connection to the InfWb remote db for icards."
  []
  (reset! *icard-connection* (SednaXQDataSource.)))

; not used by any other fcns--110901
(defn SYSnew-connection
  "Creates a new remote-db connection. Usually fed to SYSset-connection
to create a remote-db connection for something other than icard access."
  []
  ; implementation dependent--returns a new Sedna data source
  (SednaXQDataSource.))

(defn SYSset-connection
  "Used before any XQuery operation to specify the connection and database
to be used."
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

(defn SYSsetup-InfWb
  "Does all InfWb-specific setup for current session of work; should be
executed once. WARNING: deletes the session db of icards and slips."
  [icard-db-name icard-coll-name]
  
  (SYSset-connection *icard-connection* icard-db-name)
  (set-icard-db-name icard-db-name)
  (set-icard-coll-name icard-coll-name)
  (SYSclear-all)
  )

(defn SYSdb-of-conn
  "Get name of db associated with this connection."
  [connection]
  (.getProperty connection "databaseName"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; DATABASE ACCESS USING XQUERY
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;insights taken from ~/tech/schemestuff/InfWb/main/sedna-utilities.ss

(defn get-result
  "gets the result of an XQuery"
  ([result-sequence]
     (get-result result-sequence (vector)))
  ([result-sequence result-vector]
     (if (not  (.next result-sequence))
       result-vector
       (recur result-sequence (conj result-vector (.getItemAsString result-sequence (Properties.)))))))


;; for convenience of fcn below, this ns has already defined the Sedna
;; connection named *icard-connection*

(defn SYSrun-command
  "Runs specified *Sedna command* through the given connection,
returning result(s) in a vector, then closes the connection.

This function is IMPLEMENTATION-DEPENDENT. It assumes that the Sedna XML
database (http://www.sedna.org/) is running.

The connection specifies db to be queried."
  
  [cmd-str connection]
  (let [conn (.getConnection connection "SYSTEM" "MANAGER")
	xqe   (.createExpression conn)
	rs   (.executeCommand xqe cmd-str)
;	result (get-result rs)
	]
    (.close conn)
;    result
    ))

(defn SYSrun-query
  "Runs specified *XQuery query* through the given connection,
returning result(s) in a vector, then closes the connection.

This function is IMPLEMENTATION-DEPENDENT. It assumes that the Sedna XML
database (http://www.sedna.org/) is running.

The appropriate collection name, if any, must be part of query-str.
The connection specifies db to be queried."
  
  [query-str connection]
  (let [conn (.getConnection connection "SYSTEM" "MANAGER")
	xqe   (.createExpression conn)
	rs   (.executeQuery xqe query-str)
	result (get-result rs)]
    (.close conn)
    result))

(defn SYSnew-icard-collection
  "Create a new, empty collection in the icard db currently in use
by Infocard Workbench."
  [coll-name]
  (let [cmd-str (str "CREATE COLLECTION '" coll-name "'")]
    (println cmd-str)
    (SYSrun-command cmd-str *icard-connection*)))

(defn SYSdrop
  "Drop (delete) named document from current collection within the icard db 
currently in use by Infocard Workbench."
  [base-name]
  (let [cmd-str (str "DROP DOCUMENT '" base-name "' IN COLLECTION '"
		     *icard-coll-name* "'")]
    (println cmd-str)
    (SYSrun-command cmd-str *icard-connection*)))

(defn SYSload
  "Load the document pointed to by base-name into the current collection
within the icard db currently in use by Infocard Workbench.

The document is the directory given by infocard-dir (binding)"
  [base-name]
  (let [infocard-dir "/Users/gw/Dropbox/infocards/"
	file-name (str base-name ".xml")
	file-path (str infocard-dir file-name)
	query-str (str "LOAD '"
		       file-path "' '"
		       base-name
		       "' '" *icard-coll-name* "'")]
    (println query-str)
    (SYSrun-command query-str *icard-connection*)))

(defn SYSreload
  ""
  [base-name]
  (SYSdrop base-name)
  (SYSload base-name))


; NOTE: Don't trust Emacs paren matching here--fouled up by parens in quotes
(defn run-infocard-query   ; API
  "Returns results of an InfoML query. API

filter arg selects records; return arg extracts data from selected records
(current infoml element is held in variable $base).  Hardwired to use
*icard-connection* and *icard-coll-name*"
  [filter return]

  (let [infoml-query
	(str
	 "declare default element namespace 'http://infoml.org/infomlFile';\n"
	 "for $base in collection('"
	 *icard-coll-name*
	 "')/infomlFile/"
	 filter "\n"
	 "return " return)]
    (SYSrun-query infoml-query *icard-connection*)))

(defn SYSpeek-into-db
  "Returns XML that displays the docs and collections inside the external InfWb db."
  []
  (SYSrun-query "doc('$documents')" *icard-connection*))


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

(def *icdata-fields* (list :icard :ttxt :btxt :tags))

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
  "Returns false if icdata is the result of asking for the data of a
icard that does not exist in the permanent database; else returns true."
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
  (not= icdata nil))

(defn register-icard
  "Create 'empty' entry for icard in *icard-to-slip-map*."
  [icard]
  (new-assoc *icard-to-slip-map* [icard] []))

(defn permDB->icdata
  "Returns icdata record from permDB. Use function valid-from-permDB? to
check for icard validity."
  [icard]

  ;; query rtns [icard title body tag1* tag2* ... tagN*]; * = if tags exist
  (let [data-vec
	(run-infocard-query (str "infoml[@cardId = '" icard "']")
			  "($base/data/title/string(), $base/data/content/string(), $base/selectors/tag/string())")]
    (new-icdata icard
		(get data-vec 0)
		(get data-vec 1)
		(drop 2 data-vec) )))


(defn make-invalid-icdata [icard]
  (new-icdata icard
	      (str "ERROR") ; ttxt
	      ""   ;btxt
	      ["permDB" "ERROR"]))   ;tags
	      
(defn get-icdata-from-permDB   ; API
  "Retrieves an icard's icdata from permDB. If icard is not found in
permDB, substitutes a default 'invalid' record. API"
  [icard]
  (let [perm-value (permDB->icdata icard)]
    (if (valid-from-permDB? perm-value)	
	perm-value
      (make-invalid-icdata icard))))
; TODO TEST: if icard d n exist, return value not= value in localDB (?)

(defn permDB->all-icards   ; API
  "From permanent database, get seq of all icards. API"
  []
  ;; assumes that position 1 contains the file's "all-pointers" record,
  ;; which is not an end-user "actual" infocard; this assumption
  ;; may change in the future
  (run-infocard-query "infoml[position() != 1]"
		    "$base/@cardId/string()"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; SLDATAS: creating
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord sldata [slip	 ;;string; id of sldata
		   icard ;;string; card-id of icdata to be displayed
		   pobj] ;;atom to Piccolo object that implements sldata
  )

(defn register-slip-with-icard
  "Registers slip as a clone of icard."
  ; NEEDS TESTING
  [icard slip]
  (swap! *icard-to-slip-map*
	 update-in [icard] (fn [x] (conj x slip))))

(declare get-icdata)

(defn new-sldata
  "create sldata from infocard, with its pobj at (x y), or default to (0 0)--
NOTE: does *not* add sldata to *localDB*"
  ([icard x y]
  (let [icdata (get-icdata icard)
	rand-key   (rand-kayko 3)
	pobj   (make-pinfocard
		x
		y
		(icdata-field icdata :ttxt)
		(icdata-field icdata :btxt))]
    ;; the value in rand-key is the "name" of the slip about to be created
    (register-slip-with-icard icard rand-key)
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
	id-exists?  (:icard @*localDB-icdata*)]
    (if id-exists?   ;;if true, replaces existing; false adds new icdata
      ; replaces << value associated with icard >> with (this) icdata
      (swap! *localDB-icdata* update-in [icard] (fn [x] icdata))
      ; adds << icard (key), icdata (value) >> pair to *localDB*
      (swap! *localDB-icdata* assoc icard icdata))
    (register-icard icard))
  icdata)

(defn permDB->localDB
  "copy icdata from (persistent) permDB to localDB"
  [icard]
  (icdata->localDB (get-icdata-from-permDB icard)))

(defn load-icard-seq-to-localDB
  "populates *localDB* with infocards given by the sequence"
  [icard-seq]
      (doseq [icard icard-seq]
	(permDB->localDB icard)))

(defn load-all-icards
  "Loads all infocards in permanentDB to localDB."
  []
  (load-icard-seq-to-localDB (permDB->all-icards)))

(declare get-all-icards)

(defn get-icards-in-localDB
  "Returns all the icards stored locally."
  []
  (keys @*localDB-icdata*))

(defn load-new-icards
  "Loads into localDB icards that are in permDB but not localDB; returns
seq of these newly-loaded icards."
  []
  (let [old (get-icards-in-localDB)
	new (permDB->all-icards)
	diff-seq (seq (clojure.set/difference (set new) (set old)))]
    
    (if (not (empty? diff-seq))
      (load-icard-seq-to-localDB diff-seq))
    
    diff-seq))



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
    (@*localDB-icdata* icard))

(defn get-icdata   ; API
  "Returns icdata for the given icard; uses localDB as cache. API

icdata fetched from localDB if present; if not, fetched from permDB and
copied into localDB; returns icdata w/ :ttxt = \"ERROR\" if not in permDB."
  [icard]
  (let [local-value (localDB->icdata icard)]
    (if (valid-from-localDB? local-value)
      local-value
      ; return value is the icdata that was returned by permDB
      (icdata->localDB (get-icdata-from-permDB icard)) )))

(defn iget   ; API
  "Returns specified field of icard; handles all db issues transparently.
API"
  [icard field-key]
  (field-key (get-icdata icard)))

(defn get-all-icards   ; API
  "Returns a seq of all the currently available icards. API"
  []
  ; If the local cache is empty, load all icards from permDB
  (if (= @*localDB-icdata* {})
    (load-all-icards))
  (keys @*localDB-icdata*))

(defn slip->icdata
  "given a slip, return its icdata from the localDB"
  [slip]
  ;;the :icard field of the sldata contains the id of the corresp. icdata
  (localDB->icdata (:icard slip)))

(defn icdata-localDB-size []
  (count (get-all-icards)))

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

;; Definition of '(apply f args* argseq)'

;; Applies fn f to the argument list formed by prepending args to argseq.

;; Ex: (def *strings* ["str1" "str2" "str3"])
;;     (apply str *strings*)
;;     ;returns:	"str1str2str3"

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

;; (defn sldata->localDB
;;   "Stores the sldata record in the in-memory database; NOTE: new sldata is
;; inserted at the *front* of the map, *before* all existing sldatas"
;;   [sldata]
;;   (let [slip (:slip sldata)
;; 	id-exists?  (get-in @*localDB* [*sldata-idx* slip])]
;;     (if id-exists?   ;;if true, replaces existing; false adds new sldata
;;       (swap! *localDB* assoc-in [*sldata-idx* slip] sldata)
;;       (swap! *localDB* update-in [*sldata-idx*] assoc slip sldata)))
;;   nil)

(defn sldata->localDB
  "Stores the sldata record in the localDB; returns sldata"
  [sldata]
  (let [slip (:slip sldata)
	id-exists?  (:slip @*localDB-sldata*)]
    (if id-exists?   ;;if true, replaces existing; false, adds new sldata
      ; replaces << value associated with slip >> with (this) sldata
      (swap! *localDB-sldata* update-in [slip] (fn [x] sldata))
      ; adds << slip (key), sldata (value) >> pair to *localDB*
      (swap! *localDB-sldata* assoc slip sldata)))
  sldata)

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

(defn localDB->sldata
  "given its slip, retrieve its sldata from  localDB-slip; if slip not
found, returns nil"
  [slip]
  (@*localDB-sldata* slip))

(defn make-invalid-sldata [slip]
  (sldata. slip   ;slip
	   (str "ERROR")   ;icard
	   (atom nil)))   ;pobj

(defn get-sldata   ; API
  "Returns sldata for the given slip. API

Returns a sldata w/ :icard = \"ERROR\" if slip does not exist.

IMPORTANT: Unlike with slips, if a slip does not exist, it has no
entry in *localDB-sldata*, even though this fcn returns an sldata."

  [slip]
  (let [sldata   (localDB->sldata slip)]
    (if sldata
      sldata
      (make-invalid-sldata slip))))

(defn get-all-slips   ; API
  "Returns a seq of all the currently available slips. API"
  []
    (keys @*localDB-sldata*))

;; TODO needs updating to use API-level access to icard data
(defn SYSsldata-field
  "given sldata, get value of field named field-key (e.g.,:cid)"
  [sldata field-key]

  (cond   (contains? #{:slip :icard} field-key)
	  ;; these fields are stored as themselves
	  (field-key sldata)

	  (contains? #{:pobj} field-key)
	  ;; these return atoms to the datum we actually want
	  @(field-key sldata)
	
	  (contains? #{:ttxt :btxt :tags} field-key)
	  ;;executed for icdata fields
	  (let [icdata (localDB->icdata (SYSsldata-field sldata :icard))] 
	    (icdata-field icdata field-key))
	
	  ;; icards are "moved" by changing their transform
	  ;; getXOffset, getYOffset access the transform's values directly,
	  ;; eliminating need to xform local X, Y (always 0 0) to globl coords
	  (= :x field-key)
	  (let [pobj (SYSsldata-field sldata :pobj)]
	    (.getXOffset pobj))

	  (= :y field-key)
	  (let [pobj (SYSsldata-field sldata :pobj)]
	    (.getYOffset pobj))
	  
	  ))

(defn sget   ; API
  "Returns specified field of slip; handles all db issues transparently.
Also uses field keys of underlying icard to return their value. Also uses
field keys :x and :y to get position of slip. API"
  [slip field-key]
  (let [sldata (get-sldata slip)]
    (SYSsldata-field sldata field-key) ))

(defn clone   ; API
  "Returns a new slip that is the clone of the icard. API"
  [icard]
  ; NOTE: changes to clone and clone-show should be synchronized
  (let [sldata   (new-sldata icard)
	_        (sldata->localDB sldata)
	slip     (SYSsldata-field sldata :slip)]
    slip))
    
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
	pobj   (SYSsldata-field sldata :pobj)]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; API-LEVEL SLIP DISPLAY



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clone-show   ; API
  "Clones icard and displays it in the selected layer. API"
  ([icard layer x y]
; NOTE: changes to clone and clone-show should be synchronized
      (let [sldata   (new-sldata icard)
	    _        (sldata->localDB sldata)
	    slip     (SYSsldata-field sldata :slip)]
	(show sldata x y layer)
	slip))
    
  ([icard layer]
; NOTE: changes to clone and clone-show should be synchronized
     (clone-show icard layer 0 0)))

(defn display-all
  "Resets the environment, gets all icards from the remote db, and creates
and displays a slip for each icard."
  [layer-name]
  (let [db-name   "brain"
	_         (SYSsetup-InfWb db-name *icard-coll-name*)
	icards    (get-all-icards)
	]
    (doseq [icard icards]
      (clone-show icard layer-name 0 0))))

(defn display-new
  "For all new icards, creates and displays a slip for each. Used only
after user has added new icards to the remote database."
  [layer-name]
  (let [new-icards (load-new-icards)]
    (if (not (empty? new-icards))
      (doseq [icard new-icards]
	(clone-show icard layer-name 0 0))
      (println "Warning: no new icards to show"))))

(defn clear-layer
  "Removes all slips (and any other Piccolo objects) from the layer."
  [layer-name]
  (.removeAllChildren layer-name))


(SYSclear-all)

;; InfWb 0.1 Workflow Cheat Sheet  110901

;; 	(SYSclear-all)
;; 	(SYSload "filename-without-.xml")
;;      (SYSreload "filename-without-.xml")
;; 	(display-all *piccolo-layer*)
;; 	(display-new *piccolo-layer*)

;; Other useful commands:

;; 	(SYSpeek-into-db)   ; see what documents and collections exist

;;      (SYSnew-icard-collection "collection-name")

;; 	(SYSdrop "filename-without-.xml")

;;      *icard-to-slip-map*

;;      (SYSsetup-InfWb "brain" "daily")

;;      (SYSset-connection *icard-connection* "brain")