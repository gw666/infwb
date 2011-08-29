(ns infwb.icard-slip-API
  (:gen-class)
  (:use [infwb.sedna]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; IMPLEMENTATION DETAILS
;;
;; To start:
;;
;; 1. Compile sedna.clj
;; 2. Compile this file (icard_slip_API.clj)
;; 3. Execute (SYSsetup-InfWb "brain" "test")



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;
; GLOBAL VARIABLES
;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-icdata
  "Returns icdata for the given icard; uses localDB as cache. API

icdata fetched from localDB if present; if not, fetched from permDB and
copied into localDB; returns special \"ERROR\" icdata if not in permDB,"
  [icard]
  (let [local-value (localDB->icdata icard)]
    (if (valid-from-localDB? local-value)
      local-value
      ; return value is the icdata that was returned by permDB
      (icdata->localDB (get-icdata-from-permDB icard)) )))

(defn iget
  "returns specified field of icard; handles all db issues transparently"
  [icard field-key]
  (field-key (get-icdata icard)))



  