;; "new" infwb, using seesaw

(ns infwb.core
  (:gen-class)
  (:import
   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
   (edu.umd.cs.piccolo.nodes   PText)
   (edu.umd.cs.piccolo.event   PBasicInputEventHandler PDragEventHandler)
   (edu.umd.cs.piccolox.event  PSelectionEventHandler)
   (edu.umd.cs.piccolox        PFrame)

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
	 :on-close :hide))

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

; during debugging, do this manually, once only
;  (initialize)  

  (let [canvas       (new PCanvas)
	camera       (. canvas getCamera)
	tooltip-node (new PText)
	pan-handler  (. canvas getPanEventHandler)
	evt-handler  (. canvas getZoomEventHandler)
	frame        (make-app canvas)
	layer        (. canvas getLayer) ; desktop objs are children of this
;	dragger      (new PDragEventHandler)
	db-name      "brain"
	coll-name    "daily"
	]

    (. canvas removeInputEventListener pan-handler)
    (. canvas removeInputEventListener evt-handler)

    (SYSsetup-InfWb db-name coll-name)
    (. frame setSize 500 700)
    (. frame setVisible true)
;    (.setPanEventHandler canvas nil)
    (println (display-all layer))
;    (swank.core/break)
    (install-tooltip-handler camera tooltip-node)
    (install-selection-event-handler canvas layer)

    ; prepare tooltip (now empty) for use
    (. tooltip-node setPickable false)
    (. camera addChild tooltip-node)

    (list canvas camera layer)
    
;    layer   ; returns layer in which objects are placed
    ))

