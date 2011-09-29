(ns infwb.slip-display
  (:gen-class)
  (:import
   (edu.umd.cs.piccolo   PCanvas PNode PLayer)
   (edu.umd.cs.piccolo.nodes   PPath PText)
   (edu.umd.cs.piccolo.event   PBasicInputEventHandler PDragEventHandler
			       PDragSequenceEventHandler PInputEvent
			       PInputEventFilter PPanEventHandler
			       PZoomEventHandler)
   (edu.umd.cs.piccolo.util   PBounds)
   (edu.umd.cs.piccolox   PFrame)
   (edu.umd.cs.piccolox.nodes   PClip)
   (java.awt   BasicStroke Color Font GraphicsEnvironment Point Rectangle))
  (:require [clojure.contrib.string :as st])
  )

(def ^{:doc "width of a slip"
       :dynamic true} *slip-width*   180)  ;;width of a slip--was 270

(def ^{:doc "height of a slip"
       :dynamic true} *slip-height*   115)  ;;height of a slip--was 175

(def ^{:doc "height of one line of slip text"
       :dynamic true} *slip-line-height*   21)  ;;height of a slip--was 175

(def *tooltip-width*   (* 1.8 *slip-width*))

(def *title-width-in-chars*   28)
(def *ellipsis-width*   3)
(def *title-char-length*   (- *title-width-in-chars* *ellipsis-width*))
(def *body-height-in-rows*   5)
(def *body-char-length*   (* (+ *title-width-in-chars* *ellipsis-width*)
			    *body-height-in-rows*))

(defn wrap
  "Return wrapped PText; inputs: text, width to wrap to"
  [text-str wrap-width]
  ;
  ; NOTE: text obj can't be remove!'d if you attempt to re-def it using
  ;  wrap again; you must remove! it, re-def it, then add! it again
  ;
  (let [wrapped-text (PText.)]
    (.setConstrainWidthToTextWidth wrapped-text false)
    (.setText wrapped-text text-str)
    (.setBounds wrapped-text 0 0 wrap-width 100)
    wrapped-text))

(defn make-pinfocard
  "Create generic infocard object (ready to be added to a Piccolo layer)"
  [box-x box-y title-text body-text]

  (let [pobj (PPath.)
	indent-x 5 
	indent-y 4
	adjust-in-pixels 10
	adjust-in-chars  12
	body-length   (- *body-char-length* adjust-in-chars)
	t-t   (str (st/take *title-char-length* title-text) "...")
	b-t   (str (st/take  body-length body-text) "...")
	title (PText. t-t)
	body (wrap b-t (- *slip-width*
			  (quot (inc indent-x) 2)
			  adjust-in-pixels))
	divider-height (inc *slip-line-height*)
	end-x (+ box-x *slip-width*) 
	line (PPath/createLine box-x (+ box-y divider-height)
			       end-x (+ box-y divider-height))
	backgd-color (Color. 245 245 245)
	divider-color (Color. 255 100 100)]

    (.translate title (+ box-x indent-x) (+ box-y indent-y))
    (.translate body (+ box-x indent-x) (+ box-y indent-y *slip-line-height*))
    (.setPathToRectangle pobj
			 box-x box-y
			 *slip-width* *slip-height*)
    (.setPaint pobj backgd-color)
    (.setStrokePaint line divider-color)
    (.addChild pobj title)		; = child 0
    (.addChild pobj line)		; = child 1
    (.addChild pobj body)		; = child 2
    (.setChildrenPickable pobj false)

    (. pobj addAttribute "title" title-text)
    (. pobj addAttribute "body" body-text)
    pobj))
