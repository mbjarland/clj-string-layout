# Project TODO

This roadmap tracks user-facing improvements that would make
`clj-string-layout` easier to adopt, more useful in production, and more visible
to users outside the core Clojure library audience.

## User-Facing API

- [x] Add high-level `table`, `table-str`, and `table-seq` APIs.
- [x] Add a named format registry for formats such as `:markdown`, `:ascii-grid`, `:csv`, and `:html`.
- [x] Add column specs for map rows, titles, alignment, fixed widths, and per-column formatting.
- [x] Add overflow policies such as `:clip`, `:ellipsis`, `:wrap`, and `:error`.

## Width And Escaping

- [x] Add ANSI-aware display-width helpers for colored terminal output.
- [x] Add Unicode display-width helpers for wide glyphs.
- [x] Add more escaping helpers for TSV, Org mode, reStructuredText, and log-safe output.

## Tools And Reach

- [x] Add a command-line tool for formatting CSV/TSV/stdin data.
- [x] Add a Babashka-compatible entry point and `bb.edn` tasks.
- [ ] Add an examples gallery that shows output styles side-by-side.
- [ ] Add performance benchmarks and a repeatable `:bench` alias.
- [ ] Document structured `ex-data` validation and parse error shapes.
