(ns clj-string-layout.width
  "Display-width helpers for use with the :display-width layout option."
  (:require [clojure.string :as str]))

(def ^:private ansi-escape-pattern
  ;; Covers common color/style CSI sequences, OSC links, and single-byte ESC codes.
  (re-pattern (str "\u001B(?:"
                   "\\[[0-?]*[ -/]*[@-~]"
                   "|\\][^\u0007]*(?:\u0007|\u001B\\\\)"
                   "|[@-_]"
                   ")")))

(defn strip-ansi
  "Removes ANSI escape sequences from value and returns a string.

  This covers common CSI color/style sequences, OSC sequences such as terminal
  hyperlinks, and single-byte ESC sequences. It does not interpret Unicode
  grapheme width; use a Unicode-aware width function when wide glyphs are
  present."
  [value]
  (str/replace (str value) ansi-escape-pattern ""))

(defn ansi-width
  "Returns the display width of value after removing ANSI escape sequences.

  Use as `:display-width width/ansi-width` for colored terminal output where SGR
  color/style codes should not count as visible columns."
  [value]
  (count (strip-ansi value)))
