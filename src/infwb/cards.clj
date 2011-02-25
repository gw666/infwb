; project: github/gw666/infwb
; file: cards.clj

; HISTORY:

#_(defprotocol ICARDLIKE
  (ttext [this] "string; title text for infocard pointed to by id")
  (btext [this] "string; body text for infocard pointed to by id")
  )

(defrecord icard [id     ;string; card-id of infocard
		  ttxt   ;string; title text
		  btxt]  ;string; body text
  )

(defrecord slip [id      ;string; id of slip
		 cid     ;string; card-id of infocard to be displayed
		 cdobj]  ;Java object; Piccolo object that implements slip
  )
