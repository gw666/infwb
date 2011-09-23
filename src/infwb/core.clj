;; "new" infwb, using seesaw

(ns infwb.core
  (:gen-class)
  (:import
   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
   (edu.umd.cs.piccolo.nodes   PText)
   (edu.umd.cs.piccolo.event   PDragEventHandler)
   (edu.umd.cs.piccolox.event  PSelectionEventHandler)
   (edu.umd.cs.piccolox   PFrame)

   (java.awt.geom   AffineTransform)
   (javax.swing   JFrame))

  (:use seesaw.core)
  (:use [infwb   sedna infoml-utilities notecard misc-dialogs slip-display])
  (:use [clojure.set :only (difference)])
  (:require [clojure.string :as str])
  )

;; (def ^{:doc "creates New Notecard menu item, links it to new-notecard-handler"}
;;   new-notecard-action
;;   (action :name "New Notecard"
;; 	  :key  "menu N"
;; 	  :handler notecard-dialog))

(defn make-app
  "Creates, displays top-level Infocard Workbench application"
  [canvas]
  (let [mylayer   (. canvas getLayer)
;	open-h   (fn [e] (display-all mylayer))
	open-h   (fn [e]
		   (let [root (to-root e)
			 otherlayer (. (. root getContentPane)
				       getLayer)]
;		     (println (= mylayer otherlayer))
		     (. otherlayer addChild
			(new PText
			     (if (= mylayer otherlayer)
			       "true" "false")))))
	open-a   (action :handler open-h  :name "Open"  :key "menu O")
	bar    (menubar :items [(menu :text "File" :items [open-a])])
    	myframe (frame :title "Infocard Workbench" 
		       :content canvas
		       :menubar bar)]
    myframe))

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

  (let [canvas       (new PCanvas)
	layer        (. canvas getLayer) ; objects are placed here
	pan-handler  (. canvas getPanEventHandler)
	evt-handler  (. canvas getZoomEventHandler)
	frame        (make-app canvas)
	db-name      "brain"
	coll-name    "daily"
	]

    (. canvas removeInputEventListener pan-handler)
    (. canvas removeInputEventListener evt-handler)

    (SYSsetup-InfWb db-name coll-name)
    (. frame setSize 500 700)
    (. frame setVisible true)
;    (. dragger setMoveToFrontOnPress true)
    (.setPanEventHandler canvas nil)
;    (. canvas addInputEventListener dragger)
;    (println (display-all layer))
    (install-selection-event-handler canvas layer)
    layer   ; returns the value of the layer in which objects are placed
    ))

