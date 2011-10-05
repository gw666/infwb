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
	
; ----- File>Show New Infocards: reload-dialog and -handler   
	reload-h   (fn [e]
		   (let [mylayer   (. canvas getLayer)]
		     (md/reload-handler mylayer)))
	reload-a   (action :handler reload-h  :name "Show New Infocards"  :key "menu N")
	
; ----- Actions>Save Snapshot	
	savesnap-h   (fn [e]
		   (let [mylayer   (. canvas getLayer)]
		     (md/savesnap-handler mylayer)))
	savesnap-a   (action :handler savesnap-h
			     :name "Save Snapshot"  :key "menu S")
	
; ----- Actions>Restore Snapshot: reload-dialog and -handler   
	restoresnap-h   (fn [e]
		   (let [mylayer   (. canvas getLayer)]
		     (md/restoresnap-handler mylayer)))
	restoresnap-a   (action :handler restoresnap-h
				:name "Restore Snapshot"  :key "menu L")

; ----- Actions>Clear Desktop       
	clear-h   (fn [e]
		   (let [mylayer   (. canvas getLayer)]
		     (db/clear-layer mylayer)))
	clear-a   (action :handler clear-h
				:name "Erase All")
	
; ----- Actions>Import Infocard File       
	import-h   (fn [e]
		     (md/import-handler))
	import-a   (action :handler import-h  :name "Import Infocard File"  :key "menu I")
	
; ---------------------------------------------	
	mybar    (menubar :items [(menu :text "File"
					:items [open-a reload-a])
				  (menu :text "Actions"
					:items [savesnap-a restoresnap-a
						clear-a import-a])])
    	myframe  (frame :title "Infocard Workbench" 
			:content canvas
			:menubar mybar)]
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
;	dragger      (PDragEventHandler.)
	frame        (make-app canvas)
	db-name      "brain"
	coll-name    "daily"
	]

    (. canvas removeInputEventListener pan-handler)
    (. canvas removeInputEventListener evt-handler)

    (db/SYSsetup-InfWb db-name coll-name)
    (md/SYSsetup-misc-dialogs)
    
    (. frame setSize 500 700)
    (. frame setVisible true)
;    (. dragger setMoveToFrontOnPress true)
    (.setPanEventHandler canvas nil)
;    (. canvas addInputEventListener dragger)
;    (println (display-all layer))
    (install-selection-event-handler canvas layer)
    layer   ; returns the value of the layer in which objects are placed
    ))

