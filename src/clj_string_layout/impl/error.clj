(ns ^:no-doc clj-string-layout.impl.error)

(defn layout-error
  ([message]
   (layout-error message {}))
  ([message data]
   (throw (ex-info message data))))
