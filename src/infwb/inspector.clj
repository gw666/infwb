(ns infwb.inspector
  (:use [seesaw   core]))

(def *icard-text* (atom "wakka!"))

(def test-action (action :handler #(println "Clicked panel" %)
			 :enabled? true))

(def inspector-content
  (vertical-panel :id   :vpanel
		  :items [(top-bottom-split
			   (scrollable
			    (text :id             :slip1
				  :multi-line?    true
				  :editable?      false
				  :wrap-lines?    true
				  :rows           20)
			    :id :scr1)
			   (top-bottom-split
			    (scrollable
			     (text :id             :slip2
				   :multi-line?    true
				   :editable?      false
				   :wrap-lines?    true
				   :rows           20))
			    (scrollable
			     (text :id             :slip3
				   :multi-line?    true
				   :editable?      false
				   :wrap-lines?    true
				   :rows           20))))]))
(defn inspector
  ""
  []
  (frame :title          "Inspector"
	 :minimum-size   [300 :by 600]
	 :content        inspector-content
	 :visible?       true
	 :on-close       :hide))
