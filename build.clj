(load-file "src/infwb/infocard.clj")
(load-file "src/infwb/sedna.clj")
(load-file "src/infwb/core.clj")

(load-file "test/infwb/test/icards.clj")

  (do
    (def frame1 (PFrame.))
    (.setSize frame1 500 700)
    (def canvas1 (.getCanvas frame1))
    (def layer1 (.getLayer canvas1))
    (def dragger (PDragEventHandler.))    
    (.setVisible frame1 true)
    (.setMoveToFrontOnPress dragger true)
    (.setPanEventHandler canvas1 nil)
    (.addInputEventListener canvas1 dragger)
    )

(load-file "test/infwb/test/slips.clj")
