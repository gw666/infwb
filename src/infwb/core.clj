;; "new" infwb, using seesaw

(ns infwb.core
  (:gen-class)
  (:import
   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
   (edu.umd.cs.piccolo.nodes   PText)
   (edu.umd.cs.piccolo.event   PDragEventHandler)
   (edu.umd.cs.piccolox.event  PSelectionEventHandler)
   (edu.umd.cs.piccolox        PFrame)

   (java.awt.geom   AffineTransform)
   (javax.swing   JFrame))

  (:use seesaw.core)
;  (:use [infwb   sedna
;	 slip-display infoml-utilities  notecard] :reload-all)
  (:require [infwb.misc-dialogs :as md] :reload-all)
  (:require [infwb.sedna :as db] :reload-all)
  (:use [clojure.set :only (difference)])
;  (:require [clojure.string :as str])
  )

;; (def ^{:doc "creates New Notecard menu item, links it to new-notecard-handler"}
;;   new-notecard-action
;;   (action :name "New Notecard"
;; 	  :key  "menu N"
;; 	  :handler notecard-dialog))

(defn make-app
  "Creates, displays top-level Infocard Workbench application"
  [canvas]
  (let [
; ----- File>Open: shortname-dialog and -handler -----
	open-h   (fn [e]
		   (let [mylayer   (. canvas getLayer)]
		     (md/shortname-handler mylayer)))
	open-a   (action :handler open-h  :name "Open"  :key "menu O")
; ----- File>Reload: reload-dialog and -handler   
	reload-h   (fn [e]
		   (let [mylayer   (. canvas getLayer)]
		     (md/reload-handler mylayer)))
	reload-a   (action :handler reload-h  :name "Reload"  :key "menu R")
; ---------------------------------------------	
	mybar    (menubar :items [(menu :text "File"
					:items [open-a reload-a])])
    	myframe  (frame :title "Infocard Workbench" 
			:content canvas
			:menubar mybar)]
    myframe))

(defn install-selection-event-handler
  ""
  [canvas layer]
  (let [custom-handler   (new PSelectionEventHandler layer layer)]
    (. canvas addInputEventListener custom-handler)
    (.. canvas (getRoot) (getDefaultInputManager)
	(setKeyboardFocus custom-handler))))

(defn update-tooltip
  ""
  [event camera tooltip-node]
  
  (let [node   (. event getPickedNode)
	tooltip-string    (str (. node getAttribute "title")
			       "\n\n"
			       (. node getAttribute "body"))
	point   (. event getCanvasPosition)
	x   (+ 20 (. point getX))
	y   (+ 10 (. point getY))]
    
;    (swank.core/break)
; NOTE: If something stops working, try uncommenting next line
;    (.. event (getPath) (canvasToLocal point camera))
    (. tooltip-node setConstrainWidthToTextWidth false)
    (. tooltip-node setText tooltip-string)
    (. tooltip-node setBounds 0 0 *tooltip-width* 100)
    (. tooltip-node setOffset x y)))

(defn install-tooltip-handler
  ""
  [camera tooltip-node]
  (let [custom-handler   ; its value is on next line
	(proxy [PBasicInputEventHandler] []
	  (mouseMoved [event]
		      (proxy-super mouseMoved event)
		      (update-tooltip event camera tooltip-node))
	  (mouseDragged [event]
			(proxy-super mouseDragged event)
		      (update-tooltip event camera tooltip-node))) ]
    (. camera addInputEventListener custom-handler)))

(defn -main
  ""
  []

  (native!)

  (let [canvas       (new PCanvas)
	layer        (. canvas getLayer) ; objects are placed here
	pan-handler  (. canvas getPanEventHandler)
	evt-handler  (. canvas getZoomEventHandler)
;	dragger      (PDragEventHandler.)
	frame        (make-app canvas)
	db-name      "brain"
	coll-name    "daily"
	]

    (. canvas removeInputEventListener pan-handler)
    (. canvas removeInputEventListener evt-handler)

    (SYSsetup-InfWb db-name coll-name)
    (md/SYSsetup-misc-dialogs)
    
    (. frame setSize 500 700)
    (. frame setVisible true)
;    (. dragger setMoveToFrontOnPress true)
    (.setPanEventHandler canvas nil)
;    (. canvas addInputEventListener dragger)
;    (println (display-all layer))
    (install-selection-event-handler canvas layer)

    ; prepare tooltip (now empty) for use
    (. tooltip-node setPickable false)
    (. camera addChild tooltip-node)

    (list canvas camera layer)
    
;    layer   ; returns layer in which objects are placed
    ))

