# Project TODO

Open follow-ups from the May 2026 review, refreshed against the 1.1.0 release
that added Markdown alignment formats and Unicode box aliases. Items 1-17 are
review findings; the "User-facing features" section captures additional ideas
surfaced in the same review.

## Review findings

- [ ] 1. Collapse the duplicate table-format layout generators in `table.clj` and reuse `presets.clj` as the single source of truth. The 1.1.0 box/markdown alias additions now flow through the same hand-built generators, so the duplication has grown.
- [ ] 2. Stop bypassing the layout engine for `:html`; route through a preset so `:width`, `:display-width`, and `:raw?` apply uniformly.
- [ ] 3. Fix the `psql` and `org` preset rule rows so the separator length matches the data row width.
- [ ] 4. Make `table.clj/align-token` throw on unknown align values instead of silently defaulting to left.
- [ ] 5. Only emit the Markdown header rule row when a header is actually present.
- [ ] 6. Wire up CLI `--width` and document `--from`/`--to` (or remove the dead `:display-width` branch).
- [ ] 7. Trim or programmatically generate `clj-string-layout.layout` so the compatibility shim is not 226 lines of hand-written forwarders.
- [ ] 8. Replace the positional boolean argument in `parse-layout`/`explain-layout` with explicit `parse-row-layout`/`explain-row-layout` helpers.
- [ ] 9. Remove duplicated `option-map`/`spec-vector?` helpers between `impl/config.clj` and `impl/parser.clj`.
- [ ] 10. Make `merge-adjacent-text` and any other accidentally-public render helpers private.
- [ ] 11. Rename the parser's transient `:column*` IR type to something less footgun-shaped.
- [ ] 12. Slim the README by moving the preset catalog and full CLI reference into dedicated docs. Reflect the 1.1.0 alias formats in whatever survives.
- [ ] 13. Fix the `default-layout-config` and README docs so the `:fill-char` precedence (defaults to `:align-char`) is accurate.
- [ ] 14. Decide and document `[Vf]` behavior; either error or document that fills are ignored on verbatim columns.
- [ ] 15. Document the center-alignment bias (extra column to the left) in the layout language reference; the new `:markdown-center` test shape makes the bias publicly visible.
- [ ] 16. Fix the unescaped `|` in the literal-text row of `doc/layout-language.md` so the table renders.
- [ ] 17. Replace this TODO file's stale checked-off list with the active follow-ups (this commit).

## User-facing features

- [ ] 18. Add `:title` / caption support to the table API, rendered as a header line above the table.
- [ ] 19. Add `:footers` to the table API, mirroring `:headers` for totals and trailing rows.
- [ ] 20. Add a `:cell-fn` decoration callback for ANSI-colored cells without requiring `:raw? true` post-processing.
- [ ] 21. Honor `:width` for table presets via fill-aware variants so width truly expands the table.
- [ ] 22. Add `layout-into!` / `table-into!` writer sinks that stream output to a `java.io.Writer`.
