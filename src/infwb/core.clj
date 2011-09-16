;; "new" infwb, using seesaw

(ns infwb.core
  (:gen-class)
  (:import
   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
   (edu.umd.cs.piccolo.event   PDragEventHandler)
   (edu.umd.cs.piccolox.event  PSelectionEventHandler)
   (edu.umd.cs.piccolox   PFrame)

   (java.awt.geom   AffineTransform)
   (javax.swing   JFrame))

  (:use seesaw.core)
  (:use [infwb   sedna infoml-utilities notecard slip-display])
  (:use [clojure.set :only (difference)])
  (:require [clojure.string :as str])
  )

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

(defn install-selection-event-handler
  ""
  [canvas-name layer-name]
  (let [pseh   (new PSelectionEventHandler layer-name layer-name)]
    (. canvas-name addInputEventListener pseh)
    (.. canvas-name (getRoot) (getDefaultInputManager)
	(setKeyboardFocus pseh))))

(defn -main
  ""
  []

  (native!)

; during debugging, do this manually, once only
;  (initialize)  

  (let [canvas       (new PCanvas)
	frame        (make-app canvas)
	layer        (. canvas getLayer) ; objects are placed here
	dragger      (new PDragEventHandler)
	db-name      "brain"
	coll-name    "daily"

	]

    (SYSsetup-InfWb db-name coll-name)
    (. frame setSize 500 700)
    (. frame setVisible true)
;    (. dragger setMoveToFrontOnPress true)
    (.setPanEventHandler canvas nil)
    (. canvas addInputEventListener dragger)
    (println (display-all layer))
;    (swank.core/break)
    (install-selection-event-handler canvas layer)
    layer   ; returns the value of the layer in which objects are placed
    ))

