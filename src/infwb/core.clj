; project: github/gw666/infwb
; file: src/infwb/core.clj

; HISTORY:

; To start: compile sedna, core; run db-startup

(ns infwb.core
  (:gen-class)
  (:import
   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
   (edu.umd.cs.piccolo.event   PDragEventHandler)
   ;; (edu.umd.cs.piccolo.event   PBasicInputEventHandler PDragEventHandler
   ;; 			       PDragSequenceEventHandler PInputEvent
   ;;                           PInputEventFilter PPanEventHandler
   ;; 			       PZoomEventHandler)
   ;; (edu.umd.cs.piccolo.nodes   PPath PText)
   ;; (edu.umd.cs.piccolo.util   PBounds)
   (edu.umd.cs.piccolox   PFrame)
   ;; (edu.umd.cs.piccolox.nodes   PClip)
  ;; (java.awt.geom   Dimension2D Point2D)
  ;; (java.awt   BasicStroke Color Font GraphicsEnvironment Rectangle)
  ;; (java.util Properties)
  ;; (javax.xml.xquery   XQConnection XQDataSource XQResultSequence)
  ;; (net.cfoster.sedna.xqj   SednaXQDataSource)
   )
  (:use [infwb.infocard])
  (:use [infwb.sedna])
  )

;; =============== GLOBALS ===============


;; (defn -main[]
;;   (db-startup)
;;   (prn "InfWb app db has started up"))
(declare title-text)
(declare body-text)

(defn testme [the-layer title-text body-text]
  (def card (infocard 50 100 270 175 title-text body-text))
  (def card2 (infocard 100 150 270 175 title-text body-text))
  (.addChild the-layer card)
  (.addChild the-layer card2))


(defn -main []
  (let [frame1 (PFrame.)
	canvas1 (.getCanvas frame1)
	title-text "\"Boof, boof, boof!\", says the dog! Boof, boof, boof!\", says the dog! Boof, boof, boof!\", says the dog!"
	body-text  "Woo! This text is much longer and will go outside box. Woo! This text is much longer and will go outside box. Woo! This text is much longer and will go outside box. Woo! This text is much longer and will go outside box. Woo! This text is much longer and will go outside box. Woo! This text is much longer and will go outside box. Woo! This text is much longer and will go outside box. Woo! This text is much longer and will go outside box. Woo! This text is much longer and will go outside box. Bye!"
	layer1 (.getLayer canvas1)
	dragger (PDragEventHandler.)]
    
    (.setVisible frame1 true)
    (.setMoveToFrontOnPress dragger true)

    ;installs drag-PNode handler onto left-mouse button
    (.setPanEventHandler canvas1 nil)
    (.addInputEventListener canvas1 dragger)
    ;(swank.core/break)
    (testme layer1 title-text body-text)))
		    


