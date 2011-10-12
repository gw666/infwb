(ns infwb.inspector
  (:use [seesaw   core]))

(def *icard-text* (atom "wakka!"))

(def inspector-content
  (vertical-panel :id   :vpanel
		  :items [(text :id             :card1
				:multi-line?    true
				:editable?      false
				:wrap-lines?    true
				:rows           20)
			  (text :id             :card2
				:multi-line?    true
				:editable?      false
				:wrap-lines?    true
				:rows           20)
			  (text :id             :card3
				:multi-line?    true
				:editable?      false
				:wrap-lines?    true
				:rows           20)]))
(defn inspector
  ""
  []
  (frame :title          "Inspector"
	 :minimum-size   [300 :by 600]
	 :content        inspector-content
	 :visible?       true
	 :on-close       :hide))
