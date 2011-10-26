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
   (java.awt.event  MouseEvent)
   (java.awt.geom   AffineTransform)
   (javax.swing     JFrame))

  (:use seesaw.core)
;  (:use [infwb   sedna
;	 slip-display infoml-utilities  notecard] :reload-all)
  (:require [infwb.misc-dialogs :as md] :reload-all)
  (:require [infwb.sedna :as db] :reload-all)
  (:require [infwb.inspector :as in] :reload-all)
  (:use [clojure.set :only (difference)]))


(def *last-slip-clicked* (atom nil))

(defn contains-item? 
  "true if seq contains elm"
  [seq elm]  
  (some #(= elm %) seq))


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

(declare *last-slip-clicked*)

(defn custom-selection-event-handler	; WITH COMMENTS
  ""
  [marqueeParent selectableParent]
  (proxy [PSelectionEventHandler]  [marqueeParent selectableParent]
    (decorateSelectedNode [node]
			  (let [stroke-color (Color/red)]
			    (. node setStrokePaint  stroke-color)))
    (undecorateSelectedNode [node]
			    (let [stroke-color (Color/black)]
			      (. node setStrokePaint  stroke-color)))
    (startDrag [pie]			; pie is a PInputEvent
	       ;; Either picked or picked-coll may be the pobj(s) to be moved
	       ;; to the front (see use-coll?), or this may be an irrelevant
	       ;; drag to be ignored (see invalid?).
	       (let [picked   (. pie getPickedNode)
		     picked-coll (seq (.. pie getInputManager
					getKeyboardFocus getSelection))
		     use-coll? (contains-item? picked-coll picked)
		     invalid?   (= "PCamera"
				   (. (class picked) getSimpleName))]
		 (println picked)
		 (println picked-coll)
		 (println "Single item?"
			  (if use-coll? "no" "yes"))
		 (println "Needs moving?"
			  (if invalid? "no" "yes"))
		 (println "----------")
		 (proxy-super startDrag pie)
		 ))
    (endStandardSelection [pie]		; pie is a PInputEvent
			  (let [picked   (. pie getPickedNode)
				slip   (. picked getAttribute "slip")
				]
			    (swap! *last-slip-clicked*
				   (fn [x] slip))))))



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

(defn panel-handler [choose panel-vector]
  (let [panel (nth panel-vector choose)]
    (fn [e] (if (= 2 (. e getClickCount))
	      (let [slip   @*last-slip-clicked*
		    slip-text     (str slip ": "
				       (db/sget slip :ttxt)
				       "\n---------------------\n"
				       (db/sget slip :btxt))]
		(text! panel slip-text))))))


(defn -main
  ""
  []

  (native!)

  (let [canvas       (new PCanvas)
	layer        (. canvas getLayer) ; objects are placed here
	pan-handler  (. canvas getPanEventHandler)
	zoom-handler  (. canvas getZoomEventHandler)
	frame        (make-app canvas)
	db-name      "brain"
	coll-name    "daily"
	]

    (. canvas removeInputEventListener pan-handler)
    (. canvas removeInputEventListener zoom-handler)

    (db/SYSsetup-InfWb db-name coll-name)
    (md/SYSsetup-misc-dialogs)
    
    (. frame setSize 500 700)
    (. frame setVisible true)
    (.setPanEventHandler canvas nil)
    (install-selection-event-handler frame canvas layer)
    
    (let [inspect   (in/inspector)
	  slip0   (select inspect [:#slip0])
	  slip1   (select inspect [:#slip1])
	  slip2   (select inspect [:#slip2])
	  insp-panels (vector slip0 slip1 slip2)
	  ]
      (listen slip0 :mouse (panel-handler 0 insp-panels))
      (listen slip1 :mouse (panel-handler 1 insp-panels))
      (listen slip2 :mouse (panel-handler 2 insp-panels))
    (list frame canvas layer))))    ; returns the value of the frames
