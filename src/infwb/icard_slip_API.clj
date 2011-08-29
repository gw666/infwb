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

(defn sget				; API
  "Returns specified field of slip; handles all db issues transparently.
Also uses field keys of underlying icard to return their value. Also uses
field keys :x and :y to get position of slip. API"
  [slip field-key]
  (let [sldata (get-sldata slip)]
    (SYSsldata-field sldata field-key) ))

(defn clone   ; API
  "Returns a new slip that is the clone of the icard. API"
  [icard]
  (let [sldata   (new-sldata icard)
	_        (sldata->localDB sldata)
	slip     (SYSsldata-field sldata :slip)]
    slip))
    

(defn display-all
  ""
  [coll-name]
  (let [db-name   "brain"
	_         (SYSsetup-InfWb db-name coll-name)
	icards    (get-all-icards)
	]
    ; first, make API primitives for all slips!
    
    ; make slip for each icard
    ; get seq of all slips
    ; show all slips

    ))





  