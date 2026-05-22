(ns ^:no-doc clj-string-layout.impl.error)

(set! *warn-on-reflection* true)

(defn layout-error
  ([message]
   (layout-error message {}))
  ([message data]
   (throw (ex-info message data))))

(defn parse-options!
  "Validates that options is a flat key/value sequence and returns it as a map.

  Throws layout-error with the supplied error-data (plus an :options key holding
  the offending sequence) when the count is odd. Used by both config and parser
  validation paths so they share one definition of the rule."
  [options error-data]
  (when (odd? (count options))
    (layout-error "Layout specification options must be key/value pairs"
                  (assoc error-data :options options)))
  (apply hash-map options))
