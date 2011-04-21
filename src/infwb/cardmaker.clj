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

   (java.io ByteArrayOutputStream  IOException)

   (javax.xml.bind  JAXBContext  JAXBException  Marshaller
		    Unmarshaller))
  )

(defn make-icard
  "returns an infocard string created from the given inputs"
  [id tag1]
  (let [out1 (ByteArrayOutputStream.)
	objFactory (ObjectFactory.)
	myInfomlFile (.createInfomlFile objFactory)
	infomlTypeList (.getInfoml myInfomlFile)
	infocard1 (InfomlType.)
	selectors1 (SelectorsType.)
					;	data1 (ContentAgentContainerLocationType.)
	tagList (.getTag selectors1)
	]
    (.setCardId infocard1 id)
    (.setEncoding infocard1 "UTF-8")
    (.setVersion infocard1 (BigDecimal. "1.0"))

    (.add infomlTypeList infocard1)
    (.setSelectors infocard1 selectors1)
    (.add tagList tag1)

    (let [jc (JAXBContext/newInstance "org.infoml.jaxb")
	  m (.createMarshaller jc)]

	;    (.setData infocard1 data1)

      (. m setProperty  Marshaller/JAXB_FORMATTED_OUTPUT Boolean/TRUE)
      (. m setProperty  Marshaller/JAXB_SCHEMA_LOCATION
	 "http://infoml.org/infomlFile     http://infoml.org/s/infomlFile.xsd")
      (.marshal m myInfomlFile out1)
      (.toString out1)   ; it works!
      )))
    

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
  

