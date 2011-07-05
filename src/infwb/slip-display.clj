; project: github/gw666/infwb
; file: src/infwb/infocard.clj

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
   (java.awt.geom   Dimension2D Point2D)
   (java.awt   BasicStroke Color Font GraphicsEnvironment Rectangle)))

(def ^{:dynamic true} *width*   270)  ;;width of a slip
(def ^{:dynamic true} *height*   175)  ;;height of a slip

(defn wrap
  "Return PText containing given text & width to wrap to"
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
  "Create basic Piccolo infocard object (ready to be added to a layer)"
  [box-x box-y title-text body-text]

  (let [cbox (PClip.)
	title (PText. title-text)
	indent-x 5
	indent-y 4
	body (wrap body-text (- *width* (quot indent-x 2)))
	line-height 21
	divider-height 22
	end-x (+ box-x *width*)
	line (PPath/createLine box-x (+ box-y divider-height)
			       end-x (+ box-y divider-height))
	backgd-color (Color. 245 245 245)
	divider-color (Color. 255 100 100)]
    
    (.translate title (+ box-x indent-x) (+ box-y indent-y))
    (.translate body (+ box-x indent-x) (+ box-y indent-y line-height))
    (.setPathToRectangle cbox
			 box-x box-y
			 *width* *height*)
    (.setPaint cbox backgd-color)
    (.setStrokePaint line divider-color)
    (.addChild cbox title)
    (.addChild cbox line)
    (.addChild cbox body)
    (.setChildrenPickable cbox false)
    cbox))



