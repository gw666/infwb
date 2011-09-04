(ns infwb.test.api
  (:use [infwb.sedna] :reload)
  (:use [infwb.core] :reload)
  (:use [clojure.test]))

;; Required manual setup: Sedna must have a db named "brain".
;; The 'brain' db must have an empty collection (no files) named 'api'.
;; The Dropbox/infocards folder must contain "three-notecards.xml" and
;;   "four-notecards.xml".
;;
;;
;; To ensure repeatable and correct results, you should run:
;;
;;    ??  (SYSsetup-InfWb "brain" "api")


(deftest display-three-icards []
  (println "### Is 'api' collection empty of files?")
  (SYSload "three-notecards")
  (display-all infwb.core/*piccolo-layer*)
  (is (= 3 (count (get-icards-in-localDB))))
  (println "### Test is successful if desktop shows 3 icards.")
  (SYSdrop "three-notecards")
  )

(deftest add-new-icards []
  (SYSload "four-notecards")
  (display-new infwb.core/*piccolo-layer*)
  (is (= 4 (count (get-icards-in-localDB))))
  (println "### Test is successful if desktop shows four icards.")
  (SYSdrop "four-notecards")
  )

(deftest yyy []
  )


(defn test-ns-hook
  "controls test sequence; NOTE: contains fixed Sedna db & collection names"
  []
  (println "### Did you recompile the test file?                ###")

  (let [db-name "brain"
	coll-name "api"
	]
    
    (SYSsetup-InfWb db-name coll-name)
    (display-three-icards)
    (add-new-icards)
  ))

