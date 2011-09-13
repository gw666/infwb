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
  (:use [infwb   sedna infoml-utilities notecard slip-display])
  (:use [clojure.set :only (difference)])
  (:require [clojure.string :as str])
  )

(defn initialize
  "runs InfWb init code; loads all icards, creates slip for each icard"
  []
  (let [db-name "brain"
	coll-name "daily"]
    (SYSclear-all)
    (println "cleared global vars...")
    (SYSsetup-InfWb db-name coll-name)
    (println "cleared localDB...")
    (let [card-seq (permDB->all-icards)
	  num-cards (count card-seq)]
;      (swank.core/break)
      (load-icard-seq-to-localDB card-seq)
      (println "loaded" num-cards "icards to icdata DB...")
;      (load-all-sldatas-to-localDB)
;      (println "created one slip for each icard; done")
      )))

(defn new-notecard-handler
  "displays new-notecard window"
  [e]
  (let [notecard-frame
	(frame :title "New Notecard"
;;	       :visible true
	       :resizable? true
	       :minimum-size [600 :by 700]
	       :content (notecard-panel)
	       :on-close :hide)]
    ;; (println "Created new notecard-frame")
    (show! notecard-frame)
    ))

(def ^{:doc "creates New Notecard menu item, links it to new-notecard-handler"}
  new-notecard-action
  (action :name "New Notecard"
	  :key  "menu N"
	  :handler new-notecard-handler))

(defn make-app
  "Creates, displays top-level Infocard Workbench application"
  [canvas]
  (frame :title "Infocard Workbench", 
	 :content canvas
	 :menubar (menubar :items
		  [(menu :text "Actions" :items [new-notecard-action])])
	 :on-close :hide)
  )

(defn -main
  ""
  []

  (native!)

; during debugging, do this manually, once only
;  (initialize)  

  (let [canvas       (PCanvas.)
	frame        (make-app canvas)
	layer        (.getLayer canvas)
	dragger      (PDragEventHandler.)
	db-name      "brain"
	coll-name    "daily"

	]

    (SYSsetup-InfWb db-name coll-name)
    (.setSize frame 500 700)
    (.setVisible frame true)
    (.setMoveToFrontOnPress dragger true)
    (.setPanEventHandler canvas nil)
    (.addInputEventListener canvas dragger)
    (display-all layer)
   ; (swank.core/break)
;    (dorun (show-seq sldatas1    40 20    0 25  layer1))
;    (dorun (show-seq sldatas2   370 20    0 25  layer1))
;    (dorun (show-seq sldatas3   700 20    0 25  layer1))
					;    (swank.core/break)
    layer
    ))

(comment

  (def pobj (make-pinfocard 50 100 "New notecard" "It worked!"))
  (def layer (-main))
  (.addChild layer pobj)
  
 )