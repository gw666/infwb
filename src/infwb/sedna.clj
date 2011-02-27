; project: github/gw666/infwb
; file: src/infwb/sedna.clj
; last changed: 2/19/11

; HISTORY:


;; see http://www.cfoster.net/articles/xqj-tutorial/simple-xquery.xml

(ns infwb.sedna
  (:gen-class)
;  (:require [clojure.string :as str])
  (:import (javax.xml.xquery   XQConnection XQDataSource
			     XQResultSequence)
	   (net.cfoster.sedna.xqj   SednaXQDataSource)
	   (java.util   Properties))
  (:use (infwb cards)))

(defn db-startup
  "does all database setup for current session of work; should be
executed once; WARNING: deletes the database of icards and slips"
  []
  ;WARNING - RE-EXECUTING THIS DELETES ICARD DATABASE
  (def ^{:dynamic true} *appdb* (atom [{} {}]))
  
  (def ^{:dynamic true} *xqs* (SednaXQDataSource.)) ;naughty, but okay for debugging
    (doto *xqs*
      (.setProperty "serverName" "localhost")
      (.setProperty "databaseName" "test")))
  
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
data from selected records. Influenced by (db-init)."
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
  (run-db-query "infoml" "$card/@cardId/string()"))

(defn icard->appdb
  "Stores the icard record in the in-memory database (0th map in *appdb*)"
  [record]
  (let [id (:id record)
	icard-idx 0
	id-exists?  (get-in @*appdb* [icard-idx id])]
    (if id-exists?
      (swap! *appdb* assoc-in [icard-idx id] record)
      (swap! *appdb* update-in [icard-idx] assoc id record))))

(defn icard-data
  ""
  [iid field-key]
  (let [icard-idx 0  ;indexes to the icard map within *appdb*
	;gets (as a map) the icard with key iid
	icard-map (get-in *appdb* [icard-idx iid])]
    (field-key icard-map)))