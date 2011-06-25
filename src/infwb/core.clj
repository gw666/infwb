;; "new" infwb, using seesaw

(ns infwb.core
  (:gen-class)
  (:import
   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
   (edu.umd.cs.piccolo.event   PDragEventHandler)
   (edu.umd.cs.piccolox   PFrame)

   (java.awt.geom   AffineTransform))

  (:use seesaw.core)
  (:use [infwb.infocard])
  (:use [infwb.sedna])
  )

(defn initialize []
  (db-startup)
  (load-icard-seq-to-appdb (db->all-icards))
  (load-all-sldatas-to-appdb))

(defn -main []

  (initialize)

  (let [canvas1      (PCanvas.)
	frame1       (frame :title "Hello", 
				 :content canvas1,
				 :on-close :hide)
	
	;need to attach canvas1, a JComponent, to frame1
	layer1       (.getLayer canvas1)
	dragger      (PDragEventHandler.)
	slips         (appdb->all-slips)
	sldatas        (map get-sldata slips)
	[sldatas1 sldatas-temp]    (split-at 20 sldatas)
	[sldatas2 sldatas3]        (split-at 23 sldatas-temp)]

    (println frame1)
    (.setSize frame1 500 700)
    (.setVisible frame1 true)
    (.setMoveToFrontOnPress dragger true)
    (.setPanEventHandler canvas1 nil)
    (.addInputEventListener canvas1 dragger)
; (swank.core/break)
    (dorun (show-seq sldatas1    40 20    0 25  layer1))
    (dorun (show-seq sldatas2   370 20    0 25  layer1))
    (dorun (show-seq sldatas3   700 20    0 25  layer1))
    
					;    (swank.core/break)
    ))


;; (defn -main [& args]
;;   (invoke-later 
;;     (def foo (-> (frame :title "Hello", 
;;            :content "Hello, Seesaw",
;;            :on-close :exit)
;;      pack!
;;      show!))))