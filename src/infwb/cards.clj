; project: github/gw666/infwb
; file: src/infwb/cards.clj

; HISTORY:

(ns infwb.cards
  (:gen-class))


  #_(defprotocol ICARDLIKE
  (ttext [this] "string; title text for infocard pointed to by id")
  (btext [this] "string; body text for infocard pointed to by id")
  )

(defrecord icard [id     ;string; card-id of infocard
		  ttxt   ;string; title text
		  btxt]  ;string; body text
  )

(defn new-icard [id ttxt btxt]
  (icard. id ttxt btxt))

(defrecord slip [id      ;string; id of slip
		 cid     ;string; card-id of infocard to be displayed
		 pobj]   ;Piccolo object that implements slip
  )

(defn new-slip [id cid pobj]
  (slip. id cid pobj))

