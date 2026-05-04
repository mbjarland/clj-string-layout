# Project TODO

This roadmap tracks user-facing improvements that would make
`clj-string-layout` easier to adopt, more useful in production, and more visible
to users outside the core Clojure library audience.

## User-Facing API

- [ ] Add high-level `table`, `table-str`, and `table-seq` APIs.
- [ ] Add a named format registry for formats such as `:markdown`, `:ascii-grid`, `:csv`, and `:html`.
- [ ] Add column specs for map rows, titles, alignment, fixed widths, and per-column formatting.
- [ ] Add overflow policies such as `:clip`, `:ellipsis`, `:wrap`, and `:error`.

## Width And Escaping

- [ ] Add ANSI-aware display-width helpers for colored terminal output.
- [ ] Add Unicode display-width helpers for wide glyphs.
- [ ] Add more escaping helpers for TSV, Org mode, reStructuredText, and log-safe output.

## Tools And Reach

- [ ] Add a command-line tool for formatting CSV/TSV/stdin data.
- [ ] Add a Babashka-compatible entry point and `bb.edn` tasks.
- [ ] Add an examples gallery that shows output styles side-by-side.
- [ ] Add performance benchmarks and a repeatable `:bench` alias.
- [ ] Document structured `ex-data` validation and parse error shapes.
