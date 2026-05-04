(ns clj-string-layout.error)

(defn layout-error
  ([message]
   (layout-error message {}))
  ([message data]
   (throw (ex-info message data))))
