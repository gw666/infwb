;; "new" infwb, using seesaw

(ns infwb.core
  (:gen-class)
  (:import
   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
   (edu.umd.cs.piccolo.event   PDragEventHandler)
   (edu.umd.cs.piccolox   PFrame)

   (java.awt.geom   AffineTransform)
   (javax.swing   JFrame))

  (:use seesaw.core)
  (:use [infwb.infocard])
  (:use [infwb.sedna])
  (:use [infwb.notecard])
  )

(defn initialize []
  (db-startup)
  (load-icard-seq-to-appdb (db->all-icards))
  (load-all-sldatas-to-appdb))

(defn new-notecard-handler [e]
  ;do nothing for now
  )

(def new-notecard-action
  (action :name "New Notecard"
	  :key  "menu N"
	  :handler new-notecard-handler))

(defn make-app [canvas]
  (frame :title "Infocard Workbench", 
	 :content canvas,
	 :menubar (menubar :items
		  [(menu :text "Actions" :items [new-notecard-action])])
	 :on-close :hide)
  )

(defn -main []

; during debugging, do this manually, once only
;  (initialize)  

  (let [canvas1      (PCanvas.)
	frame1       (make-app canvas1)	
	layer1       (.getLayer canvas1)
	dragger      (PDragEventHandler.)

	;; IMPORTANT: uncomment these when using infocards!
	
	;; slips         (appdb->all-slips)
	;; sldatas        (map get-sldata slips)
	;; [sldatas1 sldatas-temp]    (split-at 20 sldatas)
	;; [sldatas2 sldatas3]        (split-at 23 sldatas-temp)
	]

    (println frame1)
    (.setSize frame1 500 700)
    (.setVisible frame1 true)
    (.setMoveToFrontOnPress dragger true)
    (.setPanEventHandler canvas1 nil)
    (.addInputEventListener canvas1 dragger)
   ; (swank.core/break)
;    (dorun (show-seq sldatas1    40 20    0 25  layer1))
;    (dorun (show-seq sldatas2   370 20    0 25  layer1))
;    (dorun (show-seq sldatas3   700 20    0 25  layer1))
;    (swank.core/break)
    ))

