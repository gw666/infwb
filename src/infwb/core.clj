; project: github/gw666/infwb
; file: src/infwb/core.clj

; HISTORY:

; IMPORTANT: To start: compile sedna, core; run (ns infwb.core), (db-startup)
;

(ns infwb.core
  (:gen-class)
  (:import
   ; for displaying infocards
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
;;   (println "If no error, InfWb app db has started up.")
;;   (println "If **ERROR**, have you started the Sedna database?"))

  
;; (declare title-text)
;; (declare body-text)

;; (defn testme [the-layer title-text body-text]
;;   (let [card (infocard 50 100 title-text body-text)
;; 	card2 (infocard 100 150 title-text body-text)]
;;     (.addChild the-layer card)
;;     (.addChild the-layer card2)
;;     (vector card card2)))


(defn -main []
  (db-startup)
  (let [frame1 (PFrame.)
	canvas1 (.getCanvas frame1)
	layer1 (.getLayer canvas1)
	dragger (PDragEventHandler.)]
    
    (.setVisible frame1 true)
    (.setMoveToFrontOnPress dragger true)

;installs drag-PNode handler onto left-mouse button
    (.setPanEventHandler canvas1 nil)
    (.addInputEventListener canvas1 dragger)))


  
		    


(comment   ;forms for displaying infocards

  (db-startup)
  

  (defn load-all-icards-to-appdb []
    (let [all-icards (db->all-iids)]
      (doseq [card all-icards]
	(db->appdb card))))

  (load-all-icards-to-appdb)
  
  (do
    (def frame1 (PFrame.))
    (.setSize frame1 500 700)
    (def canvas1 (.getCanvas frame1))
    (def layer1 (.getLayer canvas1))
    (def dragger (PDragEventHandler.))    
    (.setVisible frame1 true)
    (.setMoveToFrontOnPress dragger true)
    (.setPanEventHandler canvas1 nil)
    (.addInputEventListener canvas1 dragger)
    )

  
  (def s (new-slip "gw667_090815161114586"))
  (def pobj (slip-field s :pobj))
  
  (show s 0 00 layer1)
  (.translate s 100 50)
  (move-to s 100 50)
  (move-to s 0 0)
  (move-to s 5 20)

  (.removeChild layer1 pobj)
 
  (slip-field s :ttxt)		       ;returns the icard's title text

  (def i1 (make-icard 100 50 "another title" "blah blah blah"))
  (.addChild layer1 i1)
  (.removeChild layer1 i1)

  (def pcard (slip-pobj s 100 50))
  (.addChild layer1 pcard)

  ;; THE NEXT STEP is to go into s and define pcard as the :pobj value!

  (def s2 (new-slip "gw667_090905202452835"))
  (def pobj2 (slip-field s2 :pobj))
  (.addChild layer1 pobj2)
  (.removeChild layer1 pobj2)

  (show s2 0 0 layer1)
  (move-to s2 100 50)
  (move-to s2 0 0)


  (def foo (-main))
  (def card-vec (nth foo 2))
  (def layer1 (nth foo 1))
  (def card (nth card-vec 0))
  (def card2 (nth card-vec 1))
  (.removeChild layer1 card)
  (.removeChild layer1 card2)

  (defn abs[n]
    (if (neg? n) (- n) n))

  (defn round-to-int [n]
    (let [sign (if (neg? n) -1 1)
	  rounded-abs (int (+ (abs n) 0.5))]
      (* sign rounded-abs)))

  

  )