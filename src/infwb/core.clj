; project: github/gw666/infwb
; file: src/infwb/core.clj

; HISTORY:

; To start: compile cards, sedna, core; run db-startup

(ns infwb.core
  (:gen-class)
  (:import
;   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
;   (edu.umd.cs.piccolo.event   PBasicInputEventHandler PDragEventHandler
;			       PDragSequenceEventHandler PInputEvent
;                              PInputEventFilter PPanEventHandler
;			       PZoomEventHandler)
;   (edu.umd.cs.piccolo.nodes   PPath PText)
;   (edu.umd.cs.piccolo.util   PBounds)
;   (edu.umd.cs.piccolox   PFrame)
;   (edu.umd.cs.piccolox.nodes   PClip)
;   (java.awt.geom   Dimension2D Point2D)
;   (java.awt   BasicStroke Color Font GraphicsEnvironment Rectangle)
;   (java.util Properties)
   (javax.xml.xquery   XQConnection XQDataSource XQResultSequence)
   (net.cfoster.sedna.xqj   SednaXQDataSource))
  )

;; =============== GLOBALS ===============

(def ^{:dynamic true} *icard-idx*   0)  ;icard db is 0th element of @*appdb*
(def ^{:dynamic true} *slip-idx*    1)  ;slip db is 1st element of @*appdb*

(use 'infwb.sedna)


(defn -main[]
  (db-startup)
  (prn "InfWb app db has started up"))
		    


(comment		      ;What's below is misc statements to test

  (def foo (icard-get "gw667_79"))
  (load-icard foo)

  (prn (getdata "infoml[@cardId = 'gw667_76']" "($card/data/title/string(), $card/data/content/string())"))

  (prn (getdata "infoml[@cardId = 'gw667_76']" "$card/data/title/string()"))

  (prn (icard-get "gw667_76"))

  )