# Errors And `ex-data`

Public entry points throw `clojure.lang.ExceptionInfo` for validation, parsing,
and rendering failures. The exception message is intended for humans. The
`ex-data` map is intended for callers that want to branch on error categories or
surface structured diagnostics.

The most stable key is `:type`. Additional keys give context and may grow over
time as diagnostics improve.

```clojure
(try
  (layout [["a" "b"]]
          {:col-widths [1]
           :layout {:cols ["[L]"]}})
  (catch clojure.lang.ExceptionInfo e
    (case (:type (ex-data e))
      :invalid-rows "bad rows"
      :invalid-layout-config "bad layout config"
      "other layout error")))
```

## Parse Diagnostics

Use `explain-layout` when you want parse diagnostics without throwing:

```clojure
(explain-layout "[x]")
;; => {:valid? false
;;     :message "..."
;;     :data {:type :layout-parse-error
;;            :layout-string "[x]"
;;            :failure ...}}
```

Use `parse-layout` when invalid layout syntax should throw:

```clojure
(try
  (parse-layout "[x]")
  (catch clojure.lang.ExceptionInfo e
    (ex-data e)))
;; => {:type :layout-parse-error, :layout-string "[x]", :failure ...}
```

## Error Types

| `:type` | Raised By | Common Keys | Meaning |
| --- | --- | --- | --- |
| `:layout-parse-error` | `parse-layout`, `layout`, `layout-seq` | `:layout-string`, `:failure` | A layout DSL string did not match the grammar. |
| `:invalid-layout-spec` | Layout parsing | `:layout-string`, `:options` | A layout spec vector was malformed before config validation. |
| `:invalid-layout-config` | Config validation or layout expansion | `:path`, `:value`, `:layout`, `:expected`, `:actual` | Layout options, predicates, repeat groups, fill options, or column counts are invalid. |
| `:invalid-rows` | Row normalization or lazy rendering | `:path`, `:value`, `:row-count`, `:actual` | Input rows are missing, not strings, have too many cells, or do not match `:row-count`. |
| `:invalid-layout-ir` | Parser normalization | `:entry`, `:parts` | Parsed layout data had an impossible internal shape. This usually indicates a library bug. |
| `:invalid-layout-state` | Rendering | `:entry`, `:needed`, `:remaining` | Rendering reached an impossible internal state. This usually indicates a library bug or inconsistent config. |
| `:unknown-table-format` | `clj-string-layout.table/table` | `:format`, `:available-formats` | The high-level table API received an unsupported `:format`. |
| `:invalid-column-spec` | `clj-string-layout.table/table` | `:column`, `:reason` | A `:columns` entry was not a keyword, shorthand vector, or a map with `:from`. |
| `:invalid-table-column` | `clj-string-layout.table/table` | `:align`, `:allowed` | An `:align` value was not one of the supported keywords. |
| `:empty-table-spec` | `clj-string-layout.table/table` | — | The table spec had no `:rows`, `:headers`, or `:columns`. |
| `:table-cell-overflow` | `clj-string-layout.table/table` | `:value`, `:width`, `:overflow` | A table column used `:overflow :error` and a value exceeded the configured width. |
| `:cli-argument-error` | `clj-string-layout.cli/parse-args` | `:option`, `:value`, `:allowed`, `:file` | CLI arguments were missing, unsupported, or ambiguous. |
| `:cli-input-error` | `clj-string-layout.cli/render` | `:input` | CLI input parsing produced no rows. |

## Common Validation Examples

Missing layout config:

```clojure
(ex-data
  (try
    (layout [["a"]] {})
    (catch clojure.lang.ExceptionInfo e e)))
;; => {:type :invalid-layout-config
;;     :path [:layout :cols]
;;     :value nil}
```

Invalid rows:

```clojure
(ex-data
  (try
    (layout [["a" 1]] {:layout {:cols ["[L] [L]"]}})
    (catch clojure.lang.ExceptionInfo e e)))
;; => {:type :invalid-rows
;;     :path [0 1]
;;     :value 1}
```

Unknown table format:

```clojure
(ex-data
  (try
    (table/table {:format :unknown :rows [["x"]]})
    (catch clojure.lang.ExceptionInfo e e)))
;; => {:type :unknown-table-format
;;     :format :unknown
;;     :available-formats #{...}}
```

Overflow-as-error:

```clojure
(ex-data
  (try
    (table/table {:format :plain
                  :columns [{:as "Text"
                             :width 3
                             :overflow :error}]
                  :rows [["abcdef"]]})
    (catch clojure.lang.ExceptionInfo e e)))
;; => {:type :table-cell-overflow
;;     :value "abcdef"
;;     :width 3
;;     :overflow :error}
```

## Recommendations

Branch on `(:type (ex-data e))`, not on the human-readable exception message.
Use `:path` when present to highlight invalid user input. Treat
`:invalid-layout-ir` and `:invalid-layout-state` as reportable defects unless
you are intentionally constructing internal layout data.
