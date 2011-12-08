(ns infwb.misc-dialogs
  (:import
   (javax.swing   JFrame))
  (:require [infwb.sedna :as db] :reload-all)
  (:use [seesaw   core]))

(defn reset-last-shortname
  "Reset the last-shortname to have no value."
  []
  (def ^:dyanamic *last-shortname* (atom nil)))

(defn set-last-shortname
  "Set the shortname of the file that was most recently loaded into InfWb."
  [shortname]
  (swap! *last-shortname* (fn [x] shortname)))

(reset-last-shortname)

(defn get-last-shortname
  "Get the shortname of the file that was most recently loaded into InfWb."
  []
  @*last-shortname*)

(defn reset-last-snapshot
  "Reset the last-snapshot to have no value."
  []
  (def ^:dyanamic *last-snapshot* (atom nil)))


(defn SYSsetup-misc-dialogs
  ""
  []
  (reset-last-shortname)
  (reset-last-snapshot))

(defn generic-shortname-dialog
  "Creates a generic panel for getting the shortname of an InfoML file."
  [dialog-title panel-text field-text]
  (let [shortname-field    (text
			    :text          field-text
			    :multi-line?   false)
	_                  (text! shortname-field field-text)
	content-panel   (vertical-panel
			 :items
			 [(label :text panel-text
				 :h-text-position :left)
			  shortname-field])]
    (dialog :id :loadfile
	    :title dialog-title
	    :option-type   :ok-cancel
	    :type   :plain
	    :minimum-size [250 :by 128]
	    :content   content-panel
	    :success-fn   (fn [pane]
			    (text shortname-field)))))

; ----- File>Open: shortname-dialog and -handler -----

(defn shortname-dialog
  "Creates a panel for getting the short name of an InfoML file to load."
  []
  (let [field-text   (get-last-shortname)]
  
    (generic-shortname-dialog "Load New Infocard File"
			      "Short name of file to load"
			      field-text)))

(defn shortname-handler   ; NEW API   111002
  "Shows dialog asking for file to load, then calls
db/display-file-icard to process and display all icards in file."
  [layer-name]
  (let [shortname     (shortname-dialog)
	filename      (-> shortname pack! show!)]
    (set-last-shortname filename)
    (if filename			;is not nil
      (let [
;	    _           (db/SYSload filename)
	    coll-name   (db/get-icard-coll-name)]
	(db/display-file-icards filename coll-name layer-name)))))

; ----- File>Reload: reload-dialog and -handler   

(defn reload-dialog
  "Creates a panel for getting the short name of an InfoML file to REload."
  []
  (let [field-text   (get-last-shortname)]
    (generic-shortname-dialog "Reload Infocard File"
			      "Short name of file to reload"
			      field-text)))

(defn reload-handler
  "Shows dialog asking for file to load, adds file to remote DB, then calls
display-new to process and display all icards in file."
  [layer]
  (let [reload        (reload-dialog)
	shortname      (-> reload pack! show!)]
    (if shortname	;is not nil
      (let [_   (db/SYSreload shortname)]
	(db/display-new shortname layer))
      (println "ERROR, reload-handler: filename '" shortname "' is nil (?)"))))

; ----- Actions>Save Snapshot

(defn set-last-snapshot
  "Set the shortname of the current snapshot file."
  [shortname]
  (swap! *last-snapshot* (fn [x] shortname)))

(defn savesnap-dialog
  "Creates a panel for getting the short name of an InfoML file to load."
  []
  (generic-shortname-dialog "Save Snapshot of Desktop to File"
			    "Short name for snapshot file"
			    ""))

(defn savesnap-handler
  ""
  [layer]
  (let [shortname     (savesnap-dialog)
	filename      (-> shortname pack! show!)]
    (set-last-snapshot filename)
    (if filename	;is not nil
      (db/save-desktop filename layer))))


; ----- Actions>Restore Snapshot  

(defn get-last-snapshot
  "Get the shortname of the most recent snapshot file."
  []
  @*last-snapshot*)

(defn restoresnap-dialog
  "Display dialog used to reload a saved snapshot of the InfWb workspace."
  []
  (let [field-text   (get-last-snapshot)]
    (generic-shortname-dialog "Restore Saved Snapshot"
			      "Short name of file to reload"
			      field-text)))

(defn restoresnap-handler
  ""
  [layer]
  (let [restoresnap   (restoresnap-dialog)
	filename      (-> restoresnap pack! show!)]
    (if filename	;is not nil
      ; NOTE: called this line w/ zero params, compiled, ran--why???
      (db/restore-desktop filename layer)
      (println "ERROR, restoresnap-handler: filename '" filename "' is nil (?)"))))

; ----- Actions>Import Infocard File

(defn import-dialog
  ""
  []
    (generic-shortname-dialog "Import Infocard File"
			      "Short name of file to import into Infocard Workbench"
			      ""))

(defn import-handler
  ""
  []
  (let [import        (import-dialog)
	shortname      (-> import pack! show!)]
    (if shortname			;is not nil
      (do
	(set-last-shortname shortname)
        ; NOTE: called this line w/ zero params, compiled, ran--why???
	(db/SYSload shortname))
      (println "ERROR, import-handler: filename '" shortname "' is nil (?)"))))

; ===========================
