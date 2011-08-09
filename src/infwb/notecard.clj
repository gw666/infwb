;; Basic pane consisting of title, content, and tags fields.
;; The content text input area is scrollable.

(ns infwb.notecard
  (:import
   (javax.swing   JFrame))
  (:use [infwb   slip-display])
  (:use [seesaw   core mig]))

(defn vanish [e]
  (.dispose (to-frame e)))

(defn show-notecard
  "TEMPORARY fcn to create and show a minimal notecard"
  [e]
  (let [pobj   (make-pinfocard 50 100 "New notecard" "It worked!")
	frame  (to-frame e)
	canvas (.getContentPane frame)
	layer  (.getLayer canvas)
	]
    (.addChild layer pobj)
    (vanish e)
    ))

(defn notecard-panel
  "creates a panel for entering data for a new notecard; returns a border-panel"[]
  (let [title-field    (editor-pane   :text "")
	content-field  (editor-pane   :text "")
	tags-field    (editor-pane   :text "")
	cancel-button (action
		       :name "Cancel"
		       :handler (fn [e] (vanish e)))
	notecard-button (action
			 :name "Make Notecard"
			 :handler (fn [e] (show-notecard e)))
	]
    (border-panel
     :center
     (mig-panel :constraints ["flowy, filly",
			      "",
			      "[grow 0] 10 [grow 100] 10 [grow 0]"]
		:items [
			[ "Title"         "split 2"]
			[ title-field     "width 400:600:800"]
			[ "Content"       "split 2"]
			[ (scrollable content-field)    "width 400:600:800, growy"]
			[ "Tags (separate with ';')"  "split 2"]
			[ tags-field      "width 400:600:800"]
			])
     
     :south
     (horizontal-panel
      :items [cancel-button notecard-button])
     )))

;; (defn app []
;;   (frame :title "MigLayout Example" :resizable? true :content (frame-content)))
