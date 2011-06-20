; project: github/gw666/infwb
; file: src/infwb/datainput.clj

(ns infwb.cardmaker
  (:gen-class)
  (:import
   (javax.swing   JFrame)
   (org.infoml.infocardOrganizer   InfocardBuilder Main MainFrame
				   NotecardDialog  NotecardModel)
	   
   ))

;; These statements, when executed, end with a notecard dialog being
;; displayed onscreen; but record and infoml file don't get stored
(def model (NotecardModel.))
(def mainFr (MainFrame.))
(def infomlFile (Main. mainFr))
(def ib (InfocardBuilder. infomlFile model))
(def nd (NotecardDialog. mainFr "New Notecard" model ib mainFr (Boolean. true)))

;; add content to notecard dialog, then execute the following:

(.createContent nd)
(def sm   MainFrame/setupModel)
(.setupInf sm)
(.doSave mainFr)

;; experiments

(let [model (NotecardModel.)
      mainFr (MainFrame.)
      infomlFile (Main. mainFr)
      ib (InfocardBuilder. infomlFile model)
      nd (NotecardDialog. mainFr "New Notecard" model ib
			  mainFr (Boolean. true))
      ]
  (println (.panel mainFr)))