(ns infwb.core
  (:import
   (edu.umd.cs.piccolo         PCanvas PNode PLayer)
   (edu.umd.cs.piccolo.event   PDragEventHandler)
   (edu.umd.cs.piccolox   PFrame)

   (java.awt.geom   AffineTransform))

  (:use seesaw.core)
  (:use [infwb.infocard])
  (:use [infwb.sedna])
  )

(defn -main [& args]
  (invoke-later 
    (def foo (-> (frame :title "Hello", 
           :content "Hello, Seesaw",
           :on-close :exit)
     pack!
     show!))))