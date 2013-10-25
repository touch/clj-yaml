(ns clj-yaml.core
  (:require [flatland.ordered.map :refer (ordered-map)]
            [flatland.ordered.set :refer (ordered-set)])
  (:import (org.yaml.snakeyaml Yaml DumperOptions DumperOptions$FlowStyle)
           (java.util LinkedHashMap)))

(def ^{:dynamic true} *keywordize* true)

(def flow-styles
  {:auto DumperOptions$FlowStyle/AUTO
   :block DumperOptions$FlowStyle/BLOCK
   :flow DumperOptions$FlowStyle/FLOW})

(defn make-dumper-options
  [& {:keys [flow-style]}]
  (doto (DumperOptions.)
    (.setDefaultFlowStyle (flow-styles flow-style))))

(defn make-yaml
  [& {:keys [dumper-options]}]
  (if dumper-options
    (Yaml. (apply make-dumper-options
                  (mapcat (juxt key val)
                          dumper-options)))
    (Yaml.)))

(defprotocol YAMLCodec
  (encode [data])
  (decode [data]))

(defn decode-key [k]
  (if *keywordize* (keyword k) k))

(extend-protocol YAMLCodec

  clojure.lang.IPersistentMap
  (encode [data]
    (let [lhm (LinkedHashMap.)]
      (doseq [[k v] data]
        (.put lhm (encode k) (encode v)))
      lhm))

  clojure.lang.IPersistentCollection
  (encode [data]
    (map encode data))

  clojure.lang.Keyword
  (encode [data]
    (name data))

  java.util.LinkedHashMap
  (decode [data]
    (into (ordered-map)
          (for [[k v] data]
            [(decode-key k) (decode v)])))

  java.util.LinkedHashSet
  (decode [data]
    (into (ordered-set) data))

  java.util.ArrayList
  (decode [data]
    (map decode data))

  Object
  (encode [data] data)
  (decode [data] data)

  nil
  (encode [data] data)
  (decode [data] data))

(defn generate-string [data & opts]
  (.dump (apply make-yaml opts)
         (encode data)))

(defn parse-string
  ([string keywordize]
     (binding [*keywordize* keywordize]
       (parse-string string)))
  ([string]
     (decode (.load (make-yaml) string))))
