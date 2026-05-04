# Changelog

All notable changes to this project are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Changed

- Migrated the project from Leiningen to Clojure CLI, `deps.edn`, and `tools.build`.
- Updated runtime dependencies to Clojure 1.12.4 and Instaparse 1.5.0.
- Replaced Midje tests with `clojure.test`.
- Removed the Specter runtime dependency by rewriting the internal transformations with standard Clojure data operations.
- Reworked CI to run linting, tests, and jar builds on Java 11, 17, and 21.
- Expanded README documentation for the layout language, predicates, built-in layouts, development commands, and release flow.
- Split the internals into parser, config, render, error, and predicate namespaces with a tagged internal layout representation.
- Added `:repeat-for` as the clearer key for repeat-group column predicates while keeping column-layout `:apply-for` compatibility.
- Moved the release version to `version.edn` and kept deploy-only dependencies out of normal jar builds.

### Fixed

- Removed parser debug output from normal library execution.
- Fixed the built-in HTML table layouts to emit `</table>`.

### Removed

- Removed old Leiningen, IDE, Groovy prototype, scratch, and commented-out source files from the public artifact.

## [1.0.2]

- Previous published release.

[Unreleased]: https://github.com/mbjarland/clj-string-layout/compare/1.0.2...HEAD
[1.0.2]: https://github.com/mbjarland/clj-string-layout/releases/tag/1.0.2
