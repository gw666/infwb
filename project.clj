(defproject infwb "1.0.0-SNAPSHOT"
  :description "an evolving, experimental workspace for manipulating infocards"
  :main infwb.core

  :dependencies [[org.clojure/clojure "1.3"]
                 [org.clojure/clojure-contrib "[1.2.0,1.2.1]"]
                 [seesaw "1.2.0"]
                 [org.clojars.gw666/sxqj "beta2"]
                 [org.clojars.gw666/piccolo2dcore "1.3"]
                 [org.clojars.gw666/piccolo2dextras "1.3"]
                 [com.miglayout/miglayout "3.7.4"]
                 ]
  :dev-dependencies [[midje "1.3.1"]
                     [clojure-source "1.3.0"]
                     [swank-clojure "1.3.4"]]
  :jvm-opts ["-agentlib:jdwp=transport=dt_socket,server=y,suspend=n"])
