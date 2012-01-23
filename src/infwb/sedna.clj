;; see http://www.cfoster.net/articles/xqj-tutorial/simple-xquery.xml
(ns infwb.sedna
  (:gen-class)
  (:import (javax.xml.xquery XQConnection XQDataSource
			     XQResultSequence)
	   (net.cfoster.sedna.xqj SednaXQDataSource)
	   (java.util Properties)
	   (java.awt.geom AffineTransform))
  (:require [infwb.slip-display :as slip] :reload-all)
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
;;
;; 111002: Further work has revealed new needs. Here are some new
;; assumptions going forward. They may 1) change the meaning of some
;; existing functions; 2) make some functions go into disuse;
;; 3) cause the renaming of functions that otherwise stay the same;
;; 4) who knows what else.
;;
;; New assumptions:
;;
;; 1) Infocards exist within the remote DB but can't be accessed.
;; 2) Icards are a subset of infocards; they are created when an
;;    infocard is *loaded* into InfWb. In other words, icards must be
;;    created/loaded before they can be used.
;; 3) When an icard is created, a slip is always created--immediately,
;;    but such slips are not (normally) visible.
;; 4) When infocards are added to a file and the file is *reloaded*,
;;    the resulting slips are immediately made visible.
;;
;; NOTE: If using REPL, run (SYSsetup-InfWb "brain" "daily") first
;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; GLOBAL VARIABLES
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; 110906: Removed "bare" definitions of often-changing global vars
;; so as to permit recompilation of this file without clobbering
;; existing data while debugging.
;;
;; NOTE: This will require def'ing these vars before compiling this file.

;; ; ^{:dynamic true} after 'def' may suppress some error messages
;; (def *icard-connection* (SednaXQDataSource.))
;; (def *icard-coll-name*)
;; (def *icard-db-name*)

;; ;; in-memory databases for icdata (icard) and sldata (slip) data
;; (def  *localDB-icdata* (atom {}))
;; (def  *localDB-sldata* (atom {}))

;; ;; session-specific mapping of one icard to seq of multiple slips
;; (def *icard-to-slip-map* (atom {}))

;; ;; KEY: slip; VALUE: {attribute-name attr-value, ...)
;; (def *slip-attributes* (atom {}))

(def ^:dynamic *icard-fields* #{:icard :ttxt :btxt :tags})
(def ^:dynamic *slip-fields*  #{:slip :icard :pobj})

;; *slip-width*, *slip-height*, *slip-line-height* are defined in
;; slip_display.clj
;;
;;
;; ### EXTREMELY IMPORTANT ###
;;
;; *slip-width*, *slip-height*, *slip-line-height* are defined in
;; slip_display.clj
;;
;; slip_display.clj MUST BE COMPILED for everything to work
;;

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
  "Performs roughly the same task as the UNIX `ls`. That is, returns a seq
of the filenames at a given directory. If a path to a file is supplied,
then the seq contains only the original path given."
  [path]
  (let [file (java.io.File. path)]
    (if (.isDirectory file)
      (seq (.list file))
      (when (.exists file)
        [path]))))


(defn new-assoc
"In the atom-NAS, change the value associated with the key-value pair
found by following the vector of indices. If key not found, 
adds new key-value pair. Works for multiple levels of nesting.

An atom-NAS is an atom that points to a NAS (nested associative structure).
Examples:  (atom {k1 v1, k2 v2}), (atom [{k1 v1, k2 v2} {k1 v3, k2 v4}])

Given (def a (atom [{:k1 1, :k2 2} {:k1 3, :k2 4}])),
then (new-assoc a [1 :k2] 99) sets a = [{:k1 1, :k2 2} {:k1 3, :k2 99}]

Note that the first index chooses the particular map to target. Also, if the
atom contains only one map, then the surrounding brackets are not needed
around the map and vec-of-indices will have only a single value in it."
[atom-map vec-of-indices value]
  (swap! atom-map assoc-in vec-of-indices value))


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
    (def ^:dynamic *localDB-icdata* (atom {})))

(defn reset-slips []
    (def ^:dynamic *localDB-sldata* (atom {})))

(defn reset-slip-registry []
    (def ^:dynamic *icard-to-slip-map* (atom {})))

(defn reset-slip-attributes []
  (def ^:dynamic *slip-attributes* (atom {})))

;; (defn get-slip-from-icard
;;   ""
;;   (let [STARTHERE :DELETETHIS
;; 	]
;;     ))

(defn SYSclear-all
  "Clears all icard, slip, slip-registry, and slip-attribute data."
  []
  (reset-icards)
  (reset-slips)
  (reset-slip-registry)
  (reset-slip-attributes))

(defn SYSreset-icard-conn
  "Clears the connection to the InfWb remote db for icards."
  []
  (def ^:dynamic *icard-connection* (SednaXQDataSource.)))

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
  (def ^:dynamic *icard-db-name*   name))

(defn set-icard-coll-name
  "Used only before InfWb-specific XQuery queries to specify the
collection to be used"
  [name]
  (def ^:dynamic *icard-coll-name*   name))

(defn get-icard-db-name []
  *icard-db-name*)

(defn get-icard-coll-name []
  *icard-coll-name*)

(defn SYSsetup-InfWb
  "Does all InfWb-specific setup for current session of work; should be
executed once. WARNING: deletes the session db of icards and slips."
  [icard-db-name icard-coll-name]

  (SYSreset-icard-conn)
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

(defn get-file-icards   ; NEW API   111002
  "Returns a vector of all the icards in the file (already loaded into
the remote DB) referred to as shortname. API"
  [shortname coll-name]
  (let [query (str "declare default element namespace 'http://infoml.org/infomlFile'; for $base in doc('"
		   shortname
		   "', '"
		   coll-name
		   "')/infomlFile/infoml[position() != 1] return $base/@cardId/string()") ]
    (SYSrun-query query *icard-connection*)))

(defn SYSnew-icard-collection
  "Create a new, empty collection in the icard db currently in use
by Infocard Workbench."
  [coll-name]
  (let [cmd-str (str "CREATE COLLECTION '" coll-name "'")]
    (SYSrun-command cmd-str *icard-connection*)))

(defn SYSdrop
  "Drop (delete) named document from current collection within the icard db 
currently in use by Infocard Workbench."
  [base-name]
  (let [cmd-str (str "DROP DOCUMENT '" base-name "' IN COLLECTION '"
		     *icard-coll-name* "'")]
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
    (SYSrun-command query-str *icard-connection*)))

(defn SYSreload
  "Reloads a file in the permanent DB that has been changed."
  [base-name]
  (SYSdrop base-name)
  (SYSload base-name))


(defn run-infocard-query		; API
  "Returns results of an InfoML query. API

filter arg selects records; return arg extracts data from selected records;
current infoml element is held in variable $base.  Hardwired to use
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
		   btxt	 ;;atom pointing to string; body text
		   tags] ;;atom pointing to vector of tag strings
  )

(def ^:dynamic *icdata-fields* (list :icard :ttxt :btxt :tags))

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
			    "($base/data/title/string(), $base/selectors/tag/string())")
	paragraph-vec
	(run-infocard-query (str "infoml[@cardId = '" icard "']")
			    "($base/data/content/p/string())")
	raw-ttxt   (get data-vec 0)
	ttxt       (if (empty? raw-ttxt)
		     ""
		     raw-ttxt)
	;; btxt       (if (empty? paragraph-vec)
	;; 	     ""
	;; 	     paragraph-vec)
	btxt       paragraph-vec
	]
    (new-icdata icard
		ttxt
		btxt
		(rest data-vec) )))


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
  ; this is the fcn that adds slip to the icard entry of *icard-to-slip-map*
  ; NEEDS TESTING
  [icard slip]
  (swap! *icard-to-slip-map*
	 update-in [icard] (fn [x] (conj x slip))))

(defn add-to-slip-attributes
  "Adds new entry to *slip-attributes* with key = slip, value = (atom {})."
  [slip]
  (swap! *slip-attributes* assoc slip (atom {})))

(defn munge-btxt
  "converts btxt (a vector of strings, one for each para in body text) to
one text string for all paragraphs, with blank line between paragraphs"
  [btxt]
  (apply str (interpose "\n\n" btxt)))

(declare get-icdata sldata->localDB)

(defn new-sldata   ; NEW API   111002
  "Create sldata from infocard, with its pobj at (x y), or default to (0 0);
also saves new sldata in *localDB-sldata*. This is the fcn that *must* be
executed when a new slip is created. Does *not* check to see if icard
already has a slip. Returns: new sl-data record."
  ([icard x y]
  (let [icdata (get-icdata icard)
	slip   (rand-kayko 3)
	; returns a vector of strings, one for each para in body text
	btxt-text   (munge-btxt (icdata-field icdata :btxt))
	pobj   (slip/make-pinfocard
		x
		y
		(icdata-field icdata :ttxt)
		btxt-text
		icard)]
    ;; the value in slip is the "name" of the slip about to be created
    (. pobj addAttribute "slip" slip)
    (register-slip-with-icard icard slip)
    (add-to-slip-attributes slip)
    (let [new-sldata   (sldata. slip icard (atom pobj))]
      (sldata->localDB new-sldata)
      new-sldata) ))   ;return new sldata
  ([icard] (new-sldata icard 0 0)))


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
  "Stores the icdata record in the localDB; returns icdata. This is the
fcn that *must* be executed when a new icard is created."
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

(defn permDB->localDB   ; NEW API   111002
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

(defn get-icards-in-localDB
  "Returns all the icards stored locally."
  []
  (keys @*localDB-icdata*))

(defn get-new-icards   ; API
  " API"
  [shortname]
  (let [old (get-icards-in-localDB)
	new (get-file-icards shortname *icard-coll-name*)
	diff-seq (seq (clojure.set/difference (set new) (set old)))]

    (println "OLD:" old)
    (println "NEW:" new)
    (println "diff:" diff-seq)
    (println "------------------------------")
    
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

(defn localDB->icdata
  "Returns icdata value of icard iff icard is present in *localDB-icdata*;
else returns nil."
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

(defn title-str [icard]
  @(iget icard :ttxt))


;; (defn get-all-icards   ; API
;;   "Returns a seq of all the currently available icards (i.e., already
;; loaded in localDB); use permDB->all-icards to get all icards that
;; are in the remote DB. API"
;;   []
;;   ; If the local cache is empty, load all icards from permDB
;;   (if (= @*localDB-icdata* {})
;;     (load-all-icards))
;;   (keys @*localDB-icdata*))

; needs tests
(defn get-all-icards   ; API
  "NEEDS DEFINITION. API"
  []
  ; If the local cache is empty, load all icards from permDB
  (if (not= (count (get-icards-in-localDB)) (count (permDB->all-icards)))
    (load-all-icards))
  (get-icards-in-localDB))

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

;; Ex: (def ^:dynamic *strings* ["str1" "str2" "str3"])
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
  "Given icard, creates sldata, adds sldata to *localDB-sldata*; returns
slip of new sldata. Does *not* check to see if icard is already in
*localDB-icdata*."
  [icard]
  (let [sldata (new-sldata icard)
	slip (:slip sldata)]
    slip))

(defn load-all-sldatas-to-localDB []
  (let [all-icards (permDB->all-icards)]
    (doseq [icard all-icards]
      ;; icard is not added to *localDB-icdata* if it is already there
      ;; (i.e., (localDB->icdata icard) = nil, which evals as false)
      (if (localDB->icdata icard)
	(icard->sldata->localDB icard)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; ACCESSING SLDATAS AND THEIR DATA
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn localDB->sldata
  "given its slip, retrieve its sldata from localDB-slip; if slip not
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
  "Returns a seq of all the currently available slips (i.e., all slips
in localDB). API"
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
  (if (= *localDB-sldata* {})
    (println "YIKES! The local slip DB has been nuked--sorry!")
    (let [sldata (get-sldata slip)]
      (SYSsldata-field sldata field-key) )))

(defn get-pobj-title
  "Returns the text of a slip/pobj's 'slip' attribute, or error text.

Every pobj corresponding to a slip has that slip's title stored in a
'slip' attribute attached to the pobj."
  [item]
  (let [item-class-str (. (class item) getName)]
    (if (= item-class-str "edu.umd.cs.piccolo.nodes.PPath")
      ; if true, return the pobj's title string
      (let [slip (. item getAttribute "slip")]
	(str "   --" (sget slip :ttxt)))
      ; else return an error string
      (str item-class-str " is not a PPath"))
    ))

(defn clone   ; NEW API   111002
  "Returns a new slip that is the clone of the icard. API"
  [icard]
  ; NOTE: changes to clone and clone-show should be synchronized
  (let [sldata   (new-sldata icard)
	slip     (SYSsldata-field sldata :slip)]
    slip))
      
(defn move-to
  "move a slip's Piccolo infocard to a given location; returns: sldata"
  [slip   x   y]
  (let [sldata (get-sldata slip)
	pobj   @(:pobj sldata)
	dx     (double x)
	dy     (double y)
	at1    (AffineTransform. 1. 0. 0. 1. dx dy)]
;    (swank.core/break)
;    (. pobj setTransform at1)
    (. pobj setOffset dx dy)
    )
  sldata)
  

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; UNIFIED ICARD + SLIP MANIPULATION (111002 and later)
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(declare clone-show)

(defn unified-load   ; NEW API   111002
  "For a given icard (known to exist), create its icdata and sldata
records in localDB. Returns: the slip (created as part of sldata)."
  [icard]
  (permDB->localDB icard)
  ; commented out 120108
;;  (println (title-str icard))
  (clone icard))

(defn icards->slips   ; NEW API   111002
  ""
  [icard-seq]
  (loop [slips nil,   icards icard-seq]
    (if (empty? icards)
      slips
      (recur (cons (unified-load (first icards)) slips) (rest icards)))))

(declare display-seq)

(defn display-file-icards   ; NEW API   111002
  ""
  [shortname coll-name layer-name]
  (let [icard-seq (get-file-icards shortname coll-name)
	;; cd this be causing multiple-card drags to go wrong? 111029
        ;	slip-seq  (doall (map unified-load icard-seq))
	;; _   (println "--- begin display-file-icards ---")
	slip-seq  (for [icard icard-seq]
		    (unified-load icard))
	]
    (display-seq slip-seq layer-name)))
  

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; DISPLAYING SLIPS ON THE DESKTOP
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; InfWb is largely about sldatas. If a function does not say what it is
;; operating on, it is probably doing so on a sldata. Omitting mention of
;; a sldata in a function name is a way of keeping code succinct.

(defn show   ; API
  "Displays a slip at a given location in a given layer. API"
  [slip   x y   layer-name]
  (let [sldata (get-sldata slip)
	_   (move-to slip (float x) (float y))
	pobj   (SYSsldata-field sldata :pobj)]
    (.addChild layer-name pobj)))

(defn show-seq   ; API
  "Display seq of slips, starting at (x y), using dx, dy as offset
for each next slip to be displayed. API"
  [slip-seq   x y   dx dy   layer-name]
  (let [x-coords   (iterate #(+ % dx) x)
	y-coords   (iterate #(+ % dy) y)
        layer-seq  (repeat layer-name)]
;    (swank.core/break)
    (doall (map show slip-seq x-coords y-coords layer-seq))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; API-LEVEL SLIP DISPLAY
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn clone-show   ; API
  "Clones icard and displays it in the selected layer. Returns the
slip that was created. API"
  ([icard layer-name x y]
; NOTE: changes to clone and clone-show should be synchronized
      (let [sldata   (new-sldata icard)
	    slip     (SYSsldata-field sldata :slip)]
	(show slip x y layer-name)
	slip))
    
  ([icard layer-name]
; NOTE: changes to clone and clone-show should be synchronized
     (clone-show icard layer-name 0 0)))

(defn clone-show-col   ; API
  "Clones a seq of icards, displays them in one column of overlapping
slips. API"
  [icard-seq   x y   dx dy   layer-name]
  (let [x-seq   (iterate #(+ % dx) x)
	y-seq   (iterate #(+ % dy) y)
        layer-seq  (repeat layer-name)]
    (map clone-show icard-seq layer-seq x-seq y-seq)))

(defn slip-show-col
  ""
  [slip-seq x y layer-name]
  (let [dy (+ slip/*slip-line-height* 2)]
    (doall (show-seq slip-seq x y   0 dy layer-name))))

(defn display-seq   ; NEW API   111002
  "Displays columns of overlapping slips with all slip titles visible. API"
  [slip-seq layer-name]
  (let [max-in-col   6
	slip-groups (partition-all max-in-col slip-seq)
	x           10
	y           20
	x-offset    15	      ; space between two adj columns of slips
	x-seq       (iterate #(+ % slip/*slip-width* x-offset) x)
	y-seq       (repeat y)
	layer-seq   (repeat layer-name)
	]
    (if (seq? slip-seq)   ;i.e., if not empty
      (doall (map slip-show-col slip-groups x-seq y-seq layer-seq)))))

(defn icards->new-slips   ; API--but OLD
  "Creates new slips from icards, returns: list of slips. Does NOT display
slips. API"
  [icard-seq]
  (loop [slips nil,   icards icard-seq]
    (if (empty? icards)
      slips
      (recur (cons (clone (first icards)) slips) (rest icards)))))

     
(defn display-new			; API
  "Used after an existing file has been reloaded. Creates and displays
slips for all icards that do *not* have a slip already on deskotp. API"
  [shortname layer-name]
  (let [new-icards (get-new-icards shortname)]
    (if (seq? new-icards)		; i.e., if not empty
      (let [new-slips (icards->new-slips new-icards)]
	(doall (display-seq new-slips layer-name)))
    (println "Warning: no new icards to show"))))

(defn clear-layer   ; API
  "Removes all slips (and other Piccolo objects) from the layer. API"
  [layer-name]
  (.removeAllChildren layer-name))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; FCNS RETURNING DATA ABOUT SLIPS AND POBJs
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn slip-state
  "Returns a vector of the slip's icard, x-position, y-position."
  [slip]
  (let [icard   (sget slip :icard)
	pobj    (sget slip :pobj)
	gl-pt   (.getOffset pobj)
	x       (.x gl-pt)
	y       (.y gl-pt)
	]
    (vector icard x y)
    ))
;; ; not used by any other functions 110906
;; (defn slip-snapshot
;;   "returns a vector of pobjs, one for each slip onscreen"
;;   []
;;   (let [slips   (get-all-slips)
;; 	empty   (vector)]
;;     (loop [slip-list slips, result empty]
;;       (if (empty? slip-list)
;; 	result
;; 	(recur (rest slip-list) (conj result
;; 				  (sget (first slip-list) :pobj)))))))
      
(defn pobj->icard-map
  "Returns map: key = pobj, value = icard of slip that uses the pobj."
  []
  (let [slips   (get-all-slips)]
    (loop [slip-list slips, result {}]
      (if (empty? slip-list)
	result
	(recur (rest slip-list) (assoc result
				  (sget (first slip-list) :pobj)
				  (sget (first slip-list) :icard)))))))

(defn pobj-state
  "Returns a vector of the pobj's icard, x-position, y-position."
  [pobj]
  (let [pimap   (pobj->icard-map)
	gl-pt   (.getOffset pobj)
	x       (.x gl-pt)
	y       (.y gl-pt)
	icard   (pimap pobj)
	]
    (vector   icard x y)
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; SAVE AND RESTORE DESKTOP CONTENTS
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn save-desktop   ; NEW API   111002
  "Returns a vector of items. Each item names an icard and its x and y
location. Items are listed in order needed to recreate desktop (first item =
bottom, last = top). API"
  [base-name layer-name]
  (let [num-children (. layer-name getChildrenCount)
	desktop-dir  "./snapshots/"
	file-path    (str desktop-dir base-name ".txt")]
    (loop [i 0, result (vector)]
      (if (< i num-children)
	(recur (inc i) (conj result
			     (pobj-state (. layer-name getChild i))))
	(spit file-path result)))))

(defn get-desktop-data   ; NEW API   111002
  "Returns the contents of the saved-desktop file given by base-name."
  [base-name]
  (let [desktop-dir  "./snapshots/"
	file-path    (str desktop-dir base-name ".txt")]
    (read-string (slurp file-path))))
 
(defn restore-one-slip   ; NEW API   111002
  "Takes an [icard x y] vector, then creates a slip for the icard and
displays it at (x y) on the InfWb desktop."
  [vector layer-name]
  (let [[icard x y]   vector]
	(iget icard :ttxt)
	(clone-show icard layer-name x y)))

(defn restore-desktop   ; NEW API   111002
  "For the icards described in the saved-desktop file given by base-name,
displays slips representing these icards to recreate the saved InfWb
desktop exactly.

Does *not* clear the desktop of its previous contents.

This function exactly recreates the desktop and the internal relation-
ships of the InfWb system at the moment the desktop was saved. (Slip names
are not preserved, but icard-slip relationships are equivalent.) API"
  [base-name layer-name]
  (let [restore-vector (get-desktop-data base-name)]
    (doseq [vector restore-vector]
      (restore-one-slip vector layer-name))))
