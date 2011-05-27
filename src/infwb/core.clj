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
   (edu.umd.cs.piccolox   PFrame)

   (java.awt.geom   AffineTransform))
  (:use [infwb.infocard])
  (:use [infwb.sedna])  )

;; =============== GLOBALS ===============

(defn -main []
  
  (db-startup)
  (load-all-icards-to-appdb)
  (load-all-slips-to-appdb)

  (let [frame1       (PFrame.)
	canvas1      (.getCanvas frame1)
	layer1       (.getLayer canvas1)
	dragger      (PDragEventHandler.)
	sids         (appdb->all-sids)
	slips        (map get-slip sids)
	[slips1 slips2]    (split-at 33 slips)]
    
    (.setSize frame1 500 700)
    (.setVisible frame1 true)
    (.setMoveToFrontOnPress dragger true)
    (.setPanEventHandler canvas1 nil)
    (.addInputEventListener canvas1 dragger)

    (show-seq slips1    20 20    0 25  layer1)
    
;    (swank.core/break)
    
    (show-seq slips2   370 20    0 25  layer1)))
    


		    


(comment   ;forms for displaying infocards

  (db-startup)
  

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

  (show (get-slip "sl:zepola") 100 50 layer1)
  (show (get-slip "sl:nufuxa") 100 50 layer1)
  (show (get-slip "sl:leluvu") 100 50 layer1)

  (def sids (appdb->all-sids))
  (def slips (map get-slip sids))
  (def s1-4 (take 4 slips))
  (show-seq s1-4 20 20    0 25  layer1)
  (show-seq s1-4 320 20    0 25  layer1)

  (let [[slips1 slips2] (split-at 33 slips)]
      (show-seq slips1    20 20    0 25  layer1)
      (show-seq slips2   370 20    0 25  layer1))
  
  (def s (new-slip "gw667_090815161114586"))
  (def pobj (slip-field s :pobj))  
  (show s 0 0 layer1)
  
  (def s2 (new-slip "gw667_090905202452835"))
  (def pobj2 (slip-field s2 :pobj))
  (show s2 0 00 layer1)
    



  (def s (new-slip "gw667_090815161114586"))
  (def pobj (slip-field s :pobj))
  (.addChild layer1 pobj)

  (def at1 (AffineTransform. 1. 0. 0. 1. 2. 5.))
  (.setTransform pobj at1)
  
  (show s 76 72 layer1)
  (move-to s 100 50)
  (move-to s 0 0)
  (move-to s 5 20)

  (.removeChild layer1 pobj)

  (def iids (appdb->all-iids))
  (def sids (appdb->all-sids))
 
  (def s2 (new-slip "gw667_090905202452835"))
  (def pobj2 (slip-field s2 :pobj))
  (show s2 0 00 layer1)
  (.translate pobj2 100 50)
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