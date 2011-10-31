(ns infwb.inspector
  (:use [seesaw   core]))

(def *icard-text* (atom "wakka!"))

(def test-action (action :handler #(println "Clicked panel" %)
			 :enabled? true))

(def inspector-content
  (vertical-panel :items [(top-bottom-split
			   (scrollable
			    (text :id             :slip0
				  :multi-line?    true
				  :editable?      false
				  :wrap-lines?    true
				  :rows           20))
			   (top-bottom-split
			    (scrollable
			     (text :id             :slip1
				   :multi-line?    true
				   :editable?      false
				   :wrap-lines?    true
				   :rows           20))
			    (scrollable
			     (text :id             :slip2
				   :multi-line?    true
				   :editable?      false
				   :wrap-lines?    true
				   :rows           20))
			    :divider-location (/ 1 3))
			   :divider-location (/ 1 3))]))
(defn inspector
  ""
  []
  (frame :title          "Inspector"
	 :minimum-size   [250 :by 980]
	 :content        inspector-content
	 :visible?       true
	 :on-close       :hide))
