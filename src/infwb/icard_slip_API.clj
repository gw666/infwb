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

(defn display-all
  ""
  [coll-name]
  (let [db-name   "brain"
	_         (SYSsetup-InfWb db-name coll-name)
	icards    (get-all-icards)
	]
    
    ; make slip for each icard
    ; get seq of all slips
    ; show all slips

    ))





  