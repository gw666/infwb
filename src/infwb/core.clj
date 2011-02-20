; project: github/gw666/infwb
; file: core.clj
; last changed: 2/19/11

; HISTORY:


(ns infwb.core
  (:gen-class)
  (:import
;   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
;   (edu.umd.cs.piccolo.event   PBasicInputEventHandler PDragEventHandler
;			       PDragSequenceEventHandler PInputEvent PInputEventFilter PPanEventHandler
;			       PZoomEventHandler)
;   (edu.umd.cs.piccolo.nodes   PPath PText)
;   (edu.umd.cs.piccolo.util   PBounds)
;   (edu.umd.cs.piccolox   PFrame)
;   (edu.umd.cs.piccolox.nodes   PClip)
;   (java.awt.geom   Dimension2D Point2D)
;   (java.awt   BasicStroke Color Font GraphicsEnvironment Rectangle)
;   (java.util Properties)
   (javax.xml.xquery XQConnection XQDataSource XQResultSequence)
   (net.cfoster.sedna.xqj SednaXQDataSource))
  (:use (infwb   sedna)
	[clojure.string :only (split-lines) :verbose])
  )

(defprotocol ICARDLIKE
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

(db-init)

(getdata "infoml[@cardId = 'gw667_113']" "$card/@cardId/string()")

(prn (getdata "infoml[@cardId = 'gw667_76']" "$card/@cardId/string()"))

(prn (getdata "infoml" "$card/@cardId/string()"))

(getquery "infoml" "$card/@cardId/string()")

(require 'clojure.string)
(refer 'clojure.string :only '[split-lines])