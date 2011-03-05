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
  ;; (java.awt   Point)
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
  (let [card (infocard 50 100 title-text body-text)
	card2 (infocard 100 150 title-text body-text)]
    (.addChild the-layer card)
    (.addChild the-layer card2)
    (vector card card2)))


(defn -main []
  (let [frame1 (PFrame.)
	canvas1 (.getCanvas frame1)
	layer1 (.getLayer canvas1)
	dragger (PDragEventHandler.)]
    
    (.setVisible frame1 true)
    (.setMoveToFrontOnPress dragger true)

;installs drag-PNode handler onto left-mouse button
    (.setPanEventHandler canvas1 nil)
    (.addInputEventListener canvas1 dragger)
;(swank.core/break)
    (let [card-vec (testme layer1 "my title" "my body")]
         (vector frame1 layer1 card-vec))))
		    


(comment

  (db-startup)

  (defn load-all-icards-to-appdb []
    (let [all-icards (db->all-iids)]
      (doseq [card all-icards]
	(db->appdb card))))

  (def frame1 (PFrame.))
  (def canvas1 (.getCanvas frame1))
  (def layer1 (.getLayer canvas1))
  (def dragger (PDragEventHandler.))
    
  (.setVisible frame1 true)
  (.setMoveToFrontOnPress dragger true)

  (.setPanEventHandler canvas1 nil)
  (def s (new-slip "gw667_090815161114586"))
  (slip-field s :ttxt)		       ;returns the icard's title text

  (def i1 (infocard 100 50 "another title" "blah blah blah"))
  (.addChild layer1 i1)
  (.removeChild layer1 i1)

  (def pcard (slip-pobj s 100 50))
  (.addChild layer1 pcard)

  ;; THE NEXT STEP is to go into s and define pcard as the :pobj value!

  (def s2 (new-slip "gw667_090905202452835"))
  (.addChild layer1 (slip-pobj s2 50 100))


  (def foo (-main))
  (def card-vec (nth foo 2))
  (def layer1 (nth foo 1))
  (def card (nth card-vec 0))
  (def card2 (nth card-vec 1))
  (.removeChild layer1 card)
  (.removeChild layer1 card2)

  

  )