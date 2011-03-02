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
  )

(defn db-startup
  "does all database setup for current session of work; should be
executed once; WARNING: deletes the database of icards and slips"
  []
  ;WARNING - RE-EXECUTING THIS DELETES ICARD DATABASE
  (def ^{:dynamic true} *appdb* (atom [{} {}]))
  
  (def ^{:dynamic true} *xqs* (SednaXQDataSource.)) ;naughty, but okay for debugging
    (doto *xqs*
      (.setProperty "serverName" "localhost")
      (.setProperty "databaseName" "brain")))
  
(defn get-result
  "gets the result of an XQuery"
  ([result-sequence]
     (get-result result-sequence (vector)))
  ([result-sequence result-vector]
    ; (swank.core/break)
     (if (not  (.next result-sequence))
       result-vector
       (recur result-sequence (conj result-vector (.getItemAsString result-sequence (Properties.)))))))

  

;;insights taken from ~/tech/schemestuff/InfWb/main/sedna-utilities.ss

(defn run-db-query [filter result]
  "Returns results of db query; filter selects records, result extracts
data from selected records. Influenced by (db-init). Assumes *xqs* is
a working XQDataSource."
;  (prn filter)
;  (prn result)
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

(defn show-db-query
  "Returns full query that is executed by (run-db-query filter result)"
  [filter result]
  (vector
   "declare default element namespace 'http://infoml.org/infomlFile';"
   (str "for $card in collection('test')/infomlFile/" filter)
   (str "return " result)))

(defn rand-kayko
  "creates a random key of 2*len characters"
  [len]
  (let [consons (repeatedly len #(rand-nth "bdfghjklmnpqrstvwxyz"))
	vowels (repeatedly len #(rand-nth "aeiou"))
	]
    (apply str (doall (interleave consons vowels))))) ;doall removes laziness

;; One important characteristic of the icard "section" of *appdb* (itself a
;; map) is that the value of the id field of the icard is also the key
;; of that map.
(defrecord icard [id     ;string; icard-id of infocard
		  ttxt   ;string; title text
		  btxt]  ;string; body text
  )

(defrecord slip [id      ;string; id of slip
		 iid     ;string; card-id of icard to be displayed
		 pobj]   ;Piccolo object that implements slip
  )

(defn new-icard [id ttxt btxt]
  (icard. id ttxt btxt))

(defn new-slip
  ([icard-id pobj]  (let [rand-key (rand-kayko 3)]
		     (slip. rand-key icard-id pobj)))
  ([icard-id]      (new-slip icard-id nil)))


(defn db->icard
  "get icard data from appn database, return it as an icard record"
  [iid]
  (let [data-vec 
	(run-db-query (str "infoml[@cardId = '" iid "']")
		 "($card/data/title/string(), $card/data/content/string())")]
    (new-icard iid (get data-vec 0) (get data-vec 1))))

(defn db->all-iids
  "get a sequence of all icard IDs from appn database"
  []
  (run-db-query "infoml[position() != 1]" "$card/@cardId/string()"))

;; TODO: confirm correct behavior for replace vs. add
(defn icard->appdb
  "Stores the icard record in the in-memory database (0th map in *appdb*)"
  [icard]
  (let [id (:id icard)
	icard-idx   infwb.core/*icard-idx*
	id-exists?  (get-in @*appdb* [icard-idx id])]
;    (swank.core/break)
    (if id-exists?   ;if true, replaces existing; false adds new icard
      (swap! *appdb* assoc-in [icard-idx id] icard)
      (swap! *appdb* update-in [icard-idx] assoc id icard))))

(defn db->appdb
  "copy icard (if found) from (persistent) db to appdb"
  [iid]
  (let [icard (db->icard iid)
	not-found? (and
		    (nil? (:ttxt icard)) (nil? (:btxt icard)))]
;    (swank.core/break)
    (if not-found?
      (println "ERROR: card with iid =" iid "not found")
      (icard->appdb icard) )))

(defn icard-field
  "for icard, get value of field named field-key (e.g.,:cid)"
  [icard field-key]
  (field-key icard))

(defn appdb
  "for icard w/ key iid, get value of field named field-key (e.g.,:cid)"
  [iid]
  (let [icard-idx   infwb.core/*icard-idx*]  ;icard db is 0th element of @*appdb*
    (get-in @*appdb* [icard-idx iid])))


(defn icard-db-size
  "number of icards in the application's internal icard db"
  []
  (count (keys (nth @*appdb* 0))))

