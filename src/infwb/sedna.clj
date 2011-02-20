; project: github/gw666/infwb
					; file: sedna.clj
; last changed: 2/19/11

; HISTORY:


;; see http://www.cfoster.net/articles/xqj-tutorial/simple-xquery.xml

(ns infwb.sedna
  (:gen-class)
  (:require [clojure.string :as str])
  (:import (javax.xml.xquery XQConnection XQDataSource
			     XQResultSequence)
	   (net.cfoster.sedna.xqj SednaXQDataSource)
	   (java.util Properties)))

(defn get-result
  "gets the result of an XQuery"
  ([result-sequence]
     (get-result result-sequence (vector)))
  ([result-sequence result-vector]
    ; (swank.core/break)
     (if (not  (.next result-sequence))
       result-vector
       (recur result-sequence (conj result-vector (.getItemAsString result-sequence (Properties.)))))))

(defn db-init
  "does all database setup for current session of work"
  []
  (def xqs (SednaXQDataSource.)) ;naughty, but okay for debugging
    (doto xqs
      (.setProperty "serverName" "localhost")
      (.setProperty "databaseName" "test")))
  
  

;;insights taken from ~/tech/schemestuff/InfWb/main/sedna-utilities.ss

(defn getdata [filter result]
  "Returns results of db query; filter selects records, result extracts
data from selected records. Influenced by (db-init)."
  (let [conn (.getConnection xqs "SYSTEM" "MANAGER")
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
  (str
   "declare default element namespace 'http://infoml.org/infomlFile';\n"
   "for $card in collection('test')/infomlFile/"
   filter "\n"
   "return " result))
  



