;; Basic pane consisting of title, content, and tags fields.
;; The content text input area is scrollable.

(ns infwb.notecard
  (:use [seesaw   core mig]))

; http://www.devx.com/Java/Article/38017/1954

(defn frame-content []
  (let [title-field    (editor-pane   :text "")
	content-field  (editor-pane   :text "")
	tags-field    (editor-pane   :text "")]
    (mig-panel :constraints ["flowy, filly",
			     "",
			     "[grow 0] 10 [grow 100] 10 [grow 0]"]
	       :items [
		       [ "Title"         "split 2"]
		       [ title-field     "width 400:600"]
		       [ "Content"       "split 2"]
		       [ (scrollable content-field)    "width 400:600, growy"]
		       [ "Tags (separate with ';')"  "split 2"]
		       [ tags-field      "width 400:600"]
		       ])))

(defn app []
  (frame :title "MigLayout Example" :resizable? true :content (frame-content)))
