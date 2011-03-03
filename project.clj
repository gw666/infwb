; project: github/gw666/infwb
; file: project.clj

; HISTORY:

(defproject infwb "0.0.1-SNAPSHOT"
  :description "an evolving, experimental workspace for manipulating infocards"
  :main infwb.core
;  :hooks [leiningen.hooks.difftest]
  :dependencies [[org.clojure/clojure "1.3.0-SNAPSHOT"]
                 [org.clojure/clojure-contrib "1.2.0"]
		 [org.clojars.gw666/sxqj "beta2"]
		 [org.clojars.gw666/piccolo2dcore "1.3"]
		 [org.clojars.gw666/piccolo2dextras "1.3"]]
  :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]])
