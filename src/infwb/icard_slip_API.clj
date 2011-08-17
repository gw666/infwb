(ns infwb.icard-slip-API
  (:gen-class)
  (:use [infwb.sedna]))

(defn make-invalid-icdata [icard]
  (new-icdata icard
	      (str "ERROR: infocard '" icard "' not found") ; ttxt
	      ""   ;btxt
	      ["permDB" "ERROR"]))   ;tags
	      
(defn get-icdata-from-permDB [icard]
  (let [perm-value (permDB->icdata icard)]
    (if (valid-from-permDB? perm-value)
      perm-value
      (make-invalid-icdata icard))))

(defn get-icdata
  "returns icdata for the given icard; uses localDB as cache

icdata fetched from localDB if present; if not, fetched from permDB and
copied into localDB; returns special \"ERROR\" icdata if not in permDB"
  [icard]
  (let [local-value (localDB->icdata icard)]
    (if (valid-from-localDB? local-value)
      local-value
      ; return value is the icdata that was returned by permDB
      (icdata->localDB (get-icdata-from-permDB icard)) )))

(defn get-icard-data
  "returns specified field of icard; handles all db issues transparently"
  [icard field-key]
  (field-key (get-icdata icard)))



  