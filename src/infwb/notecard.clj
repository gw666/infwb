;; Basic pane consisting of title, content, and tags fields.
;; The content text input area is scrollable.

(ns infwb.notecard
  (:import
   (javax.swing   JFrame))
  (:use [infwb   slip-display])
  (:use [seesaw   core mig]))

(defn vanish [e]
  (.dispose (to-frame e)))

(defn notecard-dialog
  "creates a panel for entering data for a new notecard"
  []
  (let [title-field    (text   :text "")
	content-field  (scrollable
			(text   :text ""
				:multi-line? true))
	tags-field    (text   :text "")
	cancel-button (action
		       :name "Cancel"
		       :handler (fn [e] (vanish e)))
	notecard-button (action
			 :name "Make Notecard"
			 :handler (fn [e]
				    (vanish e)
				    (vector (text title-field)
				    	    (text content-field)
				    	    (text tags-field))
				    ))
	content-panel   (vertical-panel
			 :items
			 [(label :text "Title"
				 :h-text-position :left)
			  title-field
			  (label :text "Content"
				 :h-text-position :left)
			  content-field
			  (label :text "Tags (separate with ';')"
				 :h-text-position :left)
			  tags-field
			  (horizontal-panel
			   :items [cancel-button notecard-button])
			  ])]
    (dialog :id :notecard
	    :title "New Notecard"
	    :option-type   :ok-cancel
	    :type   :plain
;	    :minimum-size [400 :by 500]
	    :content   content-panel
	    :success-fn   (fn [pane]
			    (text title-field))
	    :visible?   true
	    :on-close   :hide
	    )
    ))

;; (defn app []
;;   (frame :title "MigLayout Example" :resizable? true :content (frame-content)))
