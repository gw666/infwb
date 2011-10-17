;; "new" infwb, using seesaw

(ns infwb.core
  (:gen-class)
  (:import
   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
   (edu.umd.cs.piccolo.nodes   PText)
   (edu.umd.cs.piccolo.event   PDragEventHandler PInputEvent)
   (edu.umd.cs.piccolox.event  PNotificationCenter PSelectionEventHandler)
   (edu.umd.cs.piccolox        PFrame)

   (java.awt        Color)
   (java.awt.geom   AffineTransform)
   (javax.swing     JFrame))

  (:use seesaw.core)
;  (:use [infwb   sedna
;	 slip-display infoml-utilities  notecard] :reload-all)
  (:require [infwb.misc-dialogs :as md] :reload-all)
  (:require [infwb.sedna :as db] :reload-all)
  (:require [infwb.inspector :as in] :reload-all)
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
					        import-a clear-a])])
    	myframe  (frame :title "Infocard Workbench" 
			:content canvas
			:menubar mybar)]
    myframe))

(defn custom-selection-event-handler	; WITH COMMENTS
  ""
  [marqueeParent selectableParent]
  (proxy [PSelectionEventHandler]  [marqueeParent selectableParent]
    (decorateSelectedNode [node]
			  (let [stroke-color (Color/red)]
			    (.setStrokePaint node stroke-color)
					;			    (println "Selected: " node)
			    ))
    (undecorateSelectedNode [node]
			    (let [stroke-color (Color/black)]
			      (.setStrokePaint node stroke-color)

					;			    (println "UNSELECTED: " node)
			      ))
    (endStandardSelection [pie]		; pie is a PInputEvent
			  (let [pobj   (.getPickedNode pie)
				slip   (. pobj getAttribute "slip")
				icard  (db/sget slip :icard)]
			    (print "picked node is " pobj
				   "\nicard is '" icard
				   "', slip is '" slip "'\n\n")
			    (println "---")
			    (proxy-super endStandardSelection pie)))))

#_(defn custom-selection-event-handler ; NO comments; will print 
  ""
  [marqueeParent selectableParent]
  (proxy [PSelectionEventHandler]  [marqueeParent selectableParent]
    (decorateSelectedNode [node]
			  (let [stroke-color (Color/red)]
			    (.setStrokePaint node stroke-color)
			    (println "Selected: " node)
			    ))
    (undecorateSelectedNode [node]
			  (let [stroke-color (Color/black)]
			    (.setStrokePaint node stroke-color)
			    (println "UNSELECTED: " node)
			    ))))

(defn install-selection-event-handler
  ""
  [frame canvas layer]
  ;; code taken from Piccolo2D SelectionExample.java
  (let [pseh   (custom-selection-event-handler layer layer)
	pnc    (PNotificationCenter/defaultCenter)
	callbackMethodName   "selectionChanged"
	notificationName
	PSelectionEventHandler/SELECTION_CHANGED_NOTIFICATION
	pan-handler  (. canvas getPanEventHandler)
	]
    (. canvas removeInputEventListener pan-handler)
    ;; add custom handler to canvas...
    (. canvas addInputEventListener pseh)
    ;; ...and direct keyboard events to it...
    (.. canvas (getRoot) (getDefaultInputManager) (setKeyboardFocus pseh))
    ;; ...and register frame to receive a notification of the new seln handler
    (. pnc addListener frame callbackMethodName notificationName pseh)
    ))

(defn selection-handler
  "Debugging: Returns running pgm's PSelectionEventHandler"
  [canvas]
  (let [l (.getLayer canvas)
	r (.getParent l)
	pim (.getDefaultInputManager r)
	pseh (.getKeyboardFocus pim)]
    pseh))

#_(defn flush-handler ;NOT WKG CORRECTLY?
  "Debugging: Causes running pgm to use new custom-selection-event-handler."
  [canvas]
  (let [layer (.getLayer canvas)]
    (install-selection-event-handler canvas layer)))


(defn -main
  ""
  []

  (native!)

  (let [canvas       (new PCanvas)
	layer        (. canvas getLayer) ; objects are placed here
	pan-handler  (. canvas getPanEventHandler)
	zoom-handler  (. canvas getZoomEventHandler)
;	dragger      (PDragEventHandler.)
	frame        (make-app canvas)
	db-name      "brain"
	coll-name    "daily"
	inspect      (in/inspector)
	]

    (. canvas removeInputEventListener pan-handler)
    (. canvas removeInputEventListener zoom-handler)

    (db/SYSsetup-InfWb db-name coll-name)
    (md/SYSsetup-misc-dialogs)
    
    (. frame setSize 500 700)
    (. frame setVisible true)
;    (. dragger setMoveToFrontOnPress true)
    (.setPanEventHandler canvas nil)
;    (. canvas addInputEventListener dragger)
    (install-selection-event-handler frame canvas layer)
    (list frame inspect)  ; returns the value of the frames
    ))

