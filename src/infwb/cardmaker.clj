; project: github/gw666/infwb
; file: src/infwb/infocard.clj

(ns infwb.cardmaker
  (:gen-class)
  (:import
   ; for creating infocards
   (org.infoml.jaxb     ContainerType
			ContentAgentContainerLocationType
			InfomlFile  InfomlType  ObjectFactory  PType
			RichTextWithExactType
			SelectorsType
			SimpleRichTextType)

   (java.io  ByteArrayOutputStream IOException)

   (javax.xml.bind  JAXBContext  JAXBException  Marshaller
		    Unmarshaller)
;   (my.numberaddition NumberAdditionUI)
   ))

(defn make-icard
  "returns an infocard string created from the given inputs"
  [iid title p-seq tag-seq]
  (let [out (ByteArrayOutputStream.)
	objFactory (ObjectFactory.)
	myInfomlFile (.createInfomlFile objFactory)
	infomlTypeList (.getInfoml myInfomlFile)
	infocard (InfomlType.)
	
	selectors (SelectorsType.)
	tagList (.getTag selectors)
	
	data (ContentAgentContainerLocationType.)
	mySRTType (SimpleRichTextType.)
	titleContainer (.getContent mySRTType)

	myRTWEType (RichTextWithExactType.)
	contentContainer (.getPOrQuotationOrPoem myRTWEType)
	]
    
    (.setCardId infocard iid)
    (.setEncoding infocard "UTF-8")
    (.setVersion infocard (BigDecimal. "1.0"))

    (.add infomlTypeList infocard)
    (.setSelectors infocard selectors)
    (doseq [tag tag-seq]
      (.add tagList tag))

    (.setData infocard data)
    (.setTitle data mySRTType)
    (.add titleContainer title) ;plain title text--no styling

    (.setContent data myRTWEType)
    (doseq [p-text p-seq]
      (let [this-p (SimpleRichTextType.)
	    this-pContainer (.getContent this-p)]
	(.add contentContainer this-p)
	(.add this-pContainer p-text)))
	    

    (try
    (let [jc (JAXBContext/newInstance "org.infoml.jaxb")
	  m (.createMarshaller jc)]

; currently not needed because infomlFile info is not used
;      (. m setProperty  Marshaller/JAXB_SCHEMA_LOCATION
;	 "http://infoml.org/infomlFile     http://infoml.org/s/infomlFile.xsd")
      (. m setProperty  Marshaller/JAXB_FORMATTED_OUTPUT Boolean/TRUE)

      (.marshal m myInfomlFile out)

      ; rawOutput is a string containing an infomlFile element with one
      ; infoml element inside it. The code below captures (and returns)
      ; the string of the infoml element only
      (let [raw-output (.toString out)
	    parsed-output
	    (re-matches #"(?s).*(<infoml .*</infoml>).*" raw-output)]
	(nth parsed-output 1))	;matched part is second item in seq
      )  ; end of let
    (catch RuntimeException e (.printStackTrace e)))  ; end of try
    ))
    

(comment   ;forms for creating infocards

  (def out1 (ByteArrayOutputStream.))
  (def objFactory (ObjectFactory.))
  (def myInfomlFile (.createInfomlFile objFactory))
  (.setTitle myInfomlFile "My first infocard file in Clojure")

  (def infomlTypeList (.getInfoml myInfomlFile))
  (def infocard1 (InfomlType.))

  (.add infomlTypeList infocard1)
  (.setCardId infocard1 "card1234")
  (.setEncoding infocard1 "UTF-8")
  (.setVersion infocard1 (BigDecimal. "1.0"))

  (def jc (JAXBContext/newInstance "org.infoml.jaxb"))
  (def m (.createMarshaller jc))
  (.marshal m myInfomlFile out1)
  (.toString out1)   ; it works!
  )
  

