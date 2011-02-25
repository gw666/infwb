; project: github/gw666/infwb
; file: sedna.clj
; last changed: 2/19/11

; HISTORY:


;; see http://www.cfoster.net/articles/xqj-tutorial/simple-xquery.xml

(ns infwb.sedna
  (:gen-class)
;  (:require [clojure.string :as str])
  (:import (javax.xml.xquery   XQConnection XQDataSource
			     XQResultSequence)
	   (net.cfoster.sedna.xqj   SednaXQDataSource)
	   (java.util   Properties)))

;(declare *db*)

(defn db-startup
  "does all database setup for current session of work"
  []
  ;WARNING - RE-EXECUTING THIS DELETES ICARD DATABASE
  (def ^{:dynamic true} *db* [{} {}])
  
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

(defn getdata [filter result]
  "Returns results of db query; filter selects records, result extracts
data from selected records. Influenced by (db-init)."
  (prn filter)
  (prn result)
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

(defn getquery
  "Returns full query that is executed by (getdata filter result)"
  [filter result]
  (vector
   "declare default element namespace 'http://infoml.org/infomlFile';"
   (str "for $card in collection('test')/infomlFile/" filter)
   (str "return " result)))

(defn icard-get
  "get icard data from appn database, return it as an icard record"
  [cid]
  (let [data-vec 
	(getdata (str "infoml[@cardId = '" cid "']")
		 "($card/data/title/string(), $card/data/content/string())")]
    (infwb.core/icard. cid (get data-vec 0) (get data-vec 1))))

(defn load-icard
  "Stores the icard record in the in-memory database (0th map in *db*)"
  [record]
  (let [id (:id record)
	icard-idx 0
	id-exists? (get-in @*db* [icard-idx id])]
    (if (id-exists?)
      (swap! *db* assoc-in [icard-idx :id] record)
      (swap! *db* update-in [icard-idx] assoc id record))))



  


