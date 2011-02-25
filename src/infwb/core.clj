; project: github/gw666/infwb
; file: core.clj
; last changed: 2/19/11

; HISTORY:

; To start: eval the ns declaration, run (db-startup)

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
   (javax.xml.xquery   XQConnection XQDataSource XQResultSequence)
   (net.cfoster.sedna.xqj   SednaXQDataSource))
  (:use (infwb   sedna cards)
      [clojure.repl :only (doc find-doc)])
  )




(defn -main[]
  (db-startup))

(comment		      ;what's below is misc statements to test

  

  (use '[clojure.core])

  (getdata "infoml[@cardId = 'gw667_113']" "$card/@cardId/string()")

  (prn (getdata "infoml[@cardId = 'gw667_76']" "$card/title/string()"))

  (prn (getdata "infoml" "$card/@cardId/string()"))

  (getquery "infoml" "$card/@cardId/string()")

  (icard-get "gw667_77")

  (prn (getdata "infoml[@cardId = 'gw667_76']" "($card/data/title/string(), $card/data/content/string())"))

  (prn (getdata "infoml[@cardId = 'gw667_76']" "$card/data/title/string()"))

  (prn (icard-get "gw667_76"))

  )