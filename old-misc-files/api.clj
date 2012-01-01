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
;; To ensure repeatable and correct results, you should:
;;
;;    * run the (do ... ) Piccolo setup snippet
;;    * compile this file, sedna.clj, and core.clj
;;    * execute (clojure.test/run-tests 'infwb.test.api)
;;


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
  (display-new *piccolo-layer*)
  (is (= 4 (count (get-icards-in-localDB))))
  (is (= 4 (count @*icard-to-slip-map*)))
  (is (= 4 (count @*slip-attrs*)))
  (println "### Test is successful if desktop shows four icards.")
  (SYSdrop "four-notecards")
  )

(deftest check-slip-globals []
  (SYSclear-all)
  (clear-layer *piccolo-layer*)
  (SYSload "four-notecards")
  (display-all *piccolo-layer*)
  (let [icard1 "gw667_090815164115403"
	icard4 "gw667_090815161114586"]
    (clone-show icard1 *piccolo-layer*)
    (clone-show icard4 *piccolo-layer*)
    (clone-show icard4 *piccolo-layer*)
    
    (let [slips-from-icard1 (@*icard-to-slip-map* icard1)
	  slips-from-icard4 (@*icard-to-slip-map* icard4)]
      (is (= 4 (count (get-icards-in-localDB))))
      (is (= 4 (count @*icard-to-slip-map*)))
      (is (= 7 (count @*slip-attrs*)))
      (is (= 2 (count slips-from-icard1)))
      (is (= 3 (count slips-from-icard4)))))
    (SYSdrop "four-notecards"))


(defn test-ns-hook
  "controls test sequence; NOTE: contains fixed Sedna db & collection names"
  []
  (println "### Did you recompile the test file? ###")

  (let [db-name "brain"
	coll-name "api"
	]
    
    (SYSsetup-InfWb db-name coll-name)
    (display-three-icards)
    (add-new-icards)
    (check-slip-globals)
  ))

