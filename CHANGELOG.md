# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Added

- Added high-level `clj-string-layout.table` APIs with named formats, column specs, map-row support, and overflow policies.
- Added `clj-string-layout.width/ansi-width` and `strip-ansi` helpers for ANSI-styled terminal output.
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

[Unreleased]: https://github.com/mbjarland/clj-string-layout/compare/v1.0.4...HEAD
[1.0.4]: https://github.com/mbjarland/clj-string-layout/releases/tag/v1.0.4
[1.0.2]: https://github.com/mbjarland/clj-string-layout/releases/tag/1.0.2
