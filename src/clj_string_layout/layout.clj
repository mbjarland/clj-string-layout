(ns clj-string-layout.layout
  "Compatibility namespace that re-exports the historical public API.

  Earlier versions exposed both predicates and built-in layout maps from this
  namespace. New code should require clj-string-layout.predicates and
  clj-string-layout.presets directly for clearer intent. Existing code can
  continue to use the aliases below.

  Each alias is generated programmatically from the corresponding source var so
  the file stays small as new presets are added."
  (:require clj-string-layout.predicates
            clj-string-layout.presets))

(defn- copy-var!
  "Interns a copy of source-var into the current namespace under sym.

  Carries metadata across (docstring, :arglists, :dynamic) and prefixes the
  docstring with a one-line note pointing at the canonical source."
  [sym source-var]
  (let [origin (str (symbol source-var))
        meta* (-> (meta source-var)
                  (dissoc :ns :name :file :line :column :end-line :end-column)
                  (update :doc (fn [doc]
                                 (str "Compatibility alias for " origin "."
                                      (when doc (str "\n\n" doc))))))]
    (intern *ns* (with-meta sym meta*) @source-var)))

(def ^:private predicate-aliases
  '[first-row? not-first-row? second-row? last-row? not-last-row?
    interior-row? not-interior-row? all-rows?
    first-col? not-first-col? second-col? last-col? not-last-col?
    interior-col? not-interior-col? all-cols?])

(def ^:private preset-aliases
  '[layout-plain-left layout-plain-center layout-plain-right
    layout-tsv layout-csv layout-pipe-separated
    layout-ascii-box-left layout-ascii-box-center layout-ascii-box-right
    layout-ascii-box-fill-left layout-ascii-box-fill-center layout-ascii-box-fill-right
    layout-norton-commander-left layout-norton-commander-center
    layout-norton-commander-right
    layout-norton-commander-fill-left layout-norton-commander-fill-center
    layout-norton-commander-fill-right
    layout-ascii-grid-left layout-ascii-grid-center layout-ascii-grid-right
    layout-ascii-grid-fill-left layout-ascii-grid-fill-center
    layout-ascii-grid-fill-right
    layout-psql-left layout-psql-right
    layout-rst-simple
    layout-org-left layout-org-right
    layout-markdown-left layout-markdown-center layout-markdown-right
    layout-markdown-fill-left layout-markdown-fill-center layout-markdown-fill-right
    layout-html-table layout-html-table-readable])

(doseq [sym predicate-aliases]
  (copy-var! sym (ns-resolve 'clj-string-layout.predicates sym)))

(doseq [sym preset-aliases]
  (copy-var! sym (ns-resolve 'clj-string-layout.presets sym)))
