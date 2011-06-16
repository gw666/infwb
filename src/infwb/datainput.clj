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
;; displayed onscreen
(def mf (MainFrame.))
(def model (NotecardModel.))
(def infomlFile (Main. mf))
(def ib (InfocardBuilder. infomlFile model))
(def jframe (JFrame. "Big Whoop"))
(def nd (NotecardDialog. jframe "nd Title" model ib mf (Boolean. true)))

