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

(defn- between? [low high value]
  (<= low value high))

(defn- zero-width-codepoint? [codepoint]
  (or (zero? codepoint)
      (< codepoint 32)
      (between? 0x7f 0x9f codepoint)
      (let [type (Character/getType codepoint)]
        (or (= Character/NON_SPACING_MARK type)
            (= Character/ENCLOSING_MARK type)
            (= Character/COMBINING_SPACING_MARK type)
            (= Character/FORMAT type)))))

(defn- wide-codepoint? [codepoint]
  (and (>= codepoint 0x1100)
       (or (between? 0x1100 0x115f codepoint)
           (= 0x2329 codepoint)
           (= 0x232a codepoint)
           (and (between? 0x2e80 0xa4cf codepoint)
                (not= 0x303f codepoint))
           (between? 0xac00 0xd7a3 codepoint)
           (between? 0xf900 0xfaff codepoint)
           (between? 0xfe10 0xfe19 codepoint)
           (between? 0xfe30 0xfe6f codepoint)
           (between? 0xff00 0xff60 codepoint)
           (between? 0xffe0 0xffe6 codepoint)
           (between? 0x20000 0x2fffd codepoint)
           (between? 0x30000 0x3fffd codepoint))))

(defn codepoint-width
  "Returns a terminal-style display width for one Unicode code point.

  Control, combining, enclosing, spacing-combining, and format code points are
  treated as zero-width. East Asian wide/fullwidth code points are treated as two
  columns. Other code points are treated as one column."
  [codepoint]
  (cond
    (zero-width-codepoint? codepoint) 0
    (wide-codepoint? codepoint) 2
    :else 1))

(defn unicode-width
  "Returns a terminal-style display width for Unicode text.

  This is a code-point based helper for common monospace terminal output. It
  handles combining marks and East Asian wide/fullwidth characters, but does not
  attempt full grapheme-cluster shaping for emoji ZWJ sequences."
  [value]
  (let [text (str value)
        len (.length text)]
    (loop [idx 0
           total 0]
      (if (< idx len)
        (let [codepoint (.codePointAt text idx)]
          (recur (+ idx (Character/charCount codepoint))
                 (+ total (codepoint-width codepoint))))
        total))))

(defn terminal-width
  "Returns Unicode display width after removing ANSI escape sequences.

  Use as `:display-width width/terminal-width` when terminal output may contain
  both ANSI styling and wide Unicode glyphs."
  [value]
  (unicode-width (strip-ansi value)))
