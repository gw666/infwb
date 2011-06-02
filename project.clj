; project: github/gw666/infwb
; file: project.clj

; HISTORY:

(defproject infwb "0.0.1-SNAPSHOT"
  :description "an evolving, experimental workspace for manipulating infocards"
  :main infwb.core
;  :eval-in-leiningen true
;  :hooks [leiningen.hooks.difftest]
  :dependencies [[org.clojure/clojure "1.3.0-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.2.0"]
		 [org.clojars.gw666/sxqj "beta2"]
		 [org.clojars.gw666/piccolo2dcore "1.3"]
		 [org.clojars.gw666/piccolo2dextras "1.3"]]
; project also uses 'infoml-classes-1_0.jar', which has been placed in  
; the lib folder manually--you MUST replace this if you do 'lein clean'
  :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]
;		     [marginalia "0.5.0"]
		     ])
