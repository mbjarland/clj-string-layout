# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

## [2.1.1] - 2026-05-22

### Changed

- Added a "Large data" section to `doc/cli.md`, `doc/table-api.md`, and
  `doc/recipes.md`. Each section is angled at that guide's audience but
  shares the same underlying facts: `table/table`, `table/table-str`,
  `table/table-seq`, `table/table-into!`, and the CLI are all eager and
  top out somewhere around a million rows; for genuinely huge inputs use
  `core/layout-seq` with explicit `:col-widths` and `core/layout-into!`
  to a `java.io.Writer`. Recipes carries a measured-numbers table
  showing the eager paths OOM at `-Xmx 512m` on a 1 M-row CSV fixture
  while the streaming path runs in 280 MB / 2 s on the same input.

No library behavior changes; documentation only.

## [2.1.0] - 2026-05-22

### Added

- `--fill` flag on the CLI. Pair it with `--width N` to expand the
  rendered table toward that target width on any fill-aware format
  (`plain`, the markdown variants, the box variants, `ascii-grid`).
  Previously `--width` was silently ineffective for those formats
  because `:fill?` wasn't reachable from the command line.
- `clj-string-layout.cli/render` now accepts `:fill?` alongside the
  existing `:width` and `:display-width` keys for programmatic
  callers.

### Changed

- The CLI guide (`doc/cli.md`) grew from a 95-line options reference
  to a 303-line guide with a man-page-style synopsis, per-input-format
  examples, an output-format gallery, width/fill semantics, piping
  recipes, exit codes, a Babashka-vs-JVM startup comparison, expanded
  programmatic-use notes, and a troubleshooting table.
- README restructured: the multi-format showcase now leads the body,
  the "Two layers" section comes after it (framed as "now that you've
  seen it work, here's how it's organised") and is slimmed
  dramatically. The description of `clj-string-layout.core/layout` is
  broadened — it's the workhorse anywhere text needs alignment, not a
  niche fallback.
- The CSV input example in the CLI guide now leads with two clean
  RFC 4180 cases (quoted comma, doubled-quote escape) so the lead
  rendering looks correct. The embedded-newline limitation is now its
  own bold-flagged paragraph that names which formats handle newlines
  in cells via their per-format escaper.

No breaking changes; `--fill` is purely additive.

## [2.0.2] - 2026-05-22

### Changed

- README restructured around a single two-layer mental model (table
  API on top, layout DSL underneath, with `clj-string-layout.presets`
  as a shortcut catalog into the DSL instead of being framed as a
  third "layer"). The DSL layer now gets one focused teaser instead
  of ~250 lines of reference content that already lived in
  `doc/layout-language.md`. Net README size: 667 → 271 lines.
- Reworded the Layout DSL example so `[L]f[R]` is described
  accurately (one layout string with two column markers and a fill
  region between them, not a "single column layout").

No library behavior changes; documentation only.

## [2.0.1] - 2026-05-22

### Changed

- Reworked the README opener: the same data is now rendered through
  `:box`, `:markdown`, `:html`, and `:csv` so the breadth of the named
  formats is visible at a glance. The dense reference paragraph that
  followed it is replaced with four labelled subsections (Rows, Columns,
  Format, Extras).
- Release workflow now POSTs to `cljdoc.org/api/request-build2` after a
  Clojars deploy succeeds, so new releases trigger documentation
  indexing immediately.
- README gains a cljdoc badge.
- `CONTRIBUTING.md` documents the docs pipeline from release to cljdoc.

No library behavior changes; documentation and release-automation only.

## [2.0.0] - 2026-05-22

Breaking redesign of the high-level table column-spec API. Reading and
writing column definitions is now an exercise in saying what you mean.

### Changed (breaking)

- Renamed column-spec keys in `clj-string-layout.table/table`:
  - `:key`     → **`:from`** ("from the row's `:foo` key")
  - `:title`   → **`:as`** ("as the header label \"Foo\"")
  - `:format`  → **`:formatter`** (a 1-arg fn applied to the value)
- `:format` at the spec level is unchanged — it still names the output
  style (`:markdown`, `:box`, ...). The two roles no longer share a name.
- Dropped the integer-`:from` shortcut for vector rows. Vector rows now
  use **position-implicit** columns: omit `:from` and the column's source
  is its position in `:columns`.
- A column entry must now be either a bare keyword (defaults) or a map.
  Vector shorthand forms (`[:qty "Qty"]`, `[:qty "Qty" :right]`, etc.)
  are gone.

### Added

- New `:invalid-column-spec` error type with `:column` and `:reason`
  fields for malformed column entries.

### Migration

```clojure
;; before
{:columns [{:key :name :title "Name"}
           {:key :qty  :title "Qty" :align :right}
           {:key :price :title "Price" :align :right
            :format #(format "$%.2f" %)}]}

;; after
{:columns [{:from :name  :as "Name"}
           {:from :qty   :as "Qty"   :align :right}
           {:from :price :as "Price" :align :right
            :formatter   #(format "$%.2f" %)}]}

;; or, for columns that only need defaults
{:columns [:name :qty :price]}

;; vector rows with per-column options now omit :from
{:columns [{:as "Name"}
           {:as "Qty" :align :right}]
 :rows    [["apple" 12] ["pear" 4]]}
```

A `sed`-style migration on a project that didn't use the `:format` fn
form is essentially `:key` → `:from`, `:title` → `:as`. If you used
`:format` inside a column spec, also rename it to `:formatter`.

## [1.2.0] - 2026-05-22

### Added

- Added `:title` caption support to the high-level table API, rendered as a centered banner for text formats and as a `<caption>` element for `:html`.
- Added `:footers` to the table API, mirroring `:headers` for totals and trailing rows.
- Added a `:cell-fn` decoration callback so per-cell styling (such as ANSI color wrapping) is possible without `:raw? true` post-processing.
- Added `:fill?` to the table API so generated formats honor `:width` via fill markers in cell padding.
- Added `layout-into!` and `table-into!` writer sinks that stream rendered output to a `java.io.Writer`.
- Added `parse-row-layout` and `explain-row-layout` so callers no longer have to pass a positional boolean.
- Added a dedicated preset catalog (`doc/presets.md`) and CLI reference (`doc/cli.md`).
- Added a Babashka-native `bb bb-format` task that runs the CLI in tens of milliseconds with no JVM startup.
- Added a Babashka test runner (`bb bb-test`) that exercises every namespace bb supports.
- Added a `--width` CLI flag wired through to fill-aware output formats.

### Changed

- Replaced the Instaparse-based grammar with a hand-rolled recursive descent parser. The library now has zero third-party Clojure dependencies and can be `(require ...)`d directly from a `bb` script.
- Consolidated box-drawing character sets into `clj-string-layout.impl.box` so `presets` and the high-level table API share one source of truth for each box style.
- Programmatically generate the `clj-string-layout.layout` compatibility shim from `predicates` and `presets`, shrinking the file from 226 lines to 57 while keeping all public aliases.
- Slimmed the README; preset catalog moved to `doc/presets.md`, CLI reference to `doc/cli.md`.
- Bumped runtime and tooling dependencies: Clojure 1.12.5, test.check 1.1.3, deps-deploy 0.2.4.
- Pinned newer GitHub Actions (checkout v6, setup-java v5, cache v5, setup-clojure 13.6.1).

### Fixed

- Fixed the `psql` and `org` preset rule rows so the separator length matches the data row width.
- Markdown table output now only emits the header rule when a header is present, so headerless `:markdown` no longer treats the first data row as a header.
- `table/table` now throws `:invalid-table-column` for unknown `:align` values instead of silently defaulting to left.
- `table/table` now throws `:empty-table-spec` up front when neither `:rows`, `:headers`, nor `:columns` is supplied.
- `:html` output now honors `:raw?`. `:width` and `:display-width` are documented as intentionally not applying to structural HTML.
- Cleared all reflection and primitive-boxing warnings; CI now fails on regression.

### Removed

- Removed the Instaparse runtime dependency.

## [1.1.0] - 2026-05-05

### Added

- Added high-level `clj-string-layout.table` APIs with named formats, column specs, map-row support, and overflow policies.
- Added high-level `:ascii-box` and `:ascii-double-box` table formats with Unicode box-drawing borders.
- Added high-level Markdown alignment formats and clearer Unicode box aliases.
- Added `clj-string-layout.width/ansi-width` and `strip-ansi` helpers for ANSI-styled terminal output.
- Added Unicode-aware `codepoint-width`, `unicode-width`, and `terminal-width` display-width helpers.
- Added TSV, Org mode, reStructuredText, and log-safe escaping helpers.
- Added a command-line formatter for CSV, TSV, and whitespace-separated input.
- Added Babashka `bb.edn` tasks for formatting, testing, linting, and jar builds.
- Added an examples gallery comparing built-in table output styles side by side.
- Added a repeatable `:bench` alias and benchmark runner for common layout paths.
- Added structured error and `ex-data` reference documentation.
- Expanded the examples gallery with backing layout definitions for each high-level table format.
- Added a dedicated layout language reference with structured examples and troubleshooting notes.
- Added plain column, TSV, CSV, pipe-separated, ASCII grid, psql, Org mode, and reStructuredText preset layouts.
- Added `clj-string-layout.escape/csv-cell` for CSV cell escaping.

## [1.0.4] - 2026-05-04

### Added

- Added `layout-str`, `layout-seq`, `parse-layout`, and `explain-layout` public helpers.
- Added escaped layout literals for reserved delimiter characters such as `\f`, `\F`, `\{`, and `\}`.
- Added configurable `:display-width`, explicit `:col-widths`, and `:row-count` options for wide glyphs and large data sets.
- Added HTML and Markdown cell escaping helpers in `clj-string-layout.escape`.
- Added a GitHub-readable recipe book with common layout examples.
- Added deterministic randomized and property-based test coverage for layout invariants.
- Added a tag-triggered GitHub Actions release workflow for Clojars and GitHub Releases.

### Changed

- Migrated the project from Leiningen to Clojure CLI, `deps.edn`, and `tools.build`.
- Updated runtime dependencies to Clojure 1.12.4 and Instaparse 1.5.0.
- Replaced Midje tests with `clojure.test`.
- Removed the Specter runtime dependency by rewriting the internal transformations with standard Clojure data operations.
- Reworked CI to run linting, tests, and jar builds on Java 11, 17, and 21.
- Expanded README documentation for the layout language, predicates, built-in layouts, development commands, and release flow.
- Expanded public API docstrings and marked internal implementation namespaces as hidden from generated docs.
- Split the internals into parser, config, render, error, and predicate namespaces with a tagged internal layout representation.
- Added `:repeat-for` as the clearer key for repeat-group column predicates while keeping column-layout `:apply-for` compatibility.
- Moved the release version to `version.edn` and kept deploy-only dependencies out of normal jar builds.
- Changed the published Maven coordinate from `com.github.mbjarland/clj-string-layout` to `io.github.mbjarland/clj-string-layout`.

### Fixed

- Removed parser debug output from normal library execution.
- Fixed the built-in HTML table layouts to emit `</table>`.

### Removed

- Removed old Leiningen, IDE, Groovy prototype, scratch, and commented-out source files from the public artifact.

## [1.0.2]

- Previous published release.

[Unreleased]: https://github.com/mbjarland/clj-string-layout/compare/v2.1.1...HEAD
[2.1.1]: https://github.com/mbjarland/clj-string-layout/releases/tag/v2.1.1
[2.1.0]: https://github.com/mbjarland/clj-string-layout/releases/tag/v2.1.0
[2.0.2]: https://github.com/mbjarland/clj-string-layout/releases/tag/v2.0.2
[2.0.1]: https://github.com/mbjarland/clj-string-layout/releases/tag/v2.0.1
[2.0.0]: https://github.com/mbjarland/clj-string-layout/releases/tag/v2.0.0
[1.2.0]: https://github.com/mbjarland/clj-string-layout/releases/tag/v1.2.0
[1.1.0]: https://github.com/mbjarland/clj-string-layout/releases/tag/v1.1.0
[1.0.4]: https://github.com/mbjarland/clj-string-layout/releases/tag/v1.0.4
[1.0.2]: https://github.com/mbjarland/clj-string-layout/releases/tag/1.0.2
