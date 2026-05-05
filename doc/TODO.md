# Project TODO

All items from the May 2026 review have been addressed against the 1.1.0
release. Add new follow-ups below as they come up.

## Completed (May 2026 review)

- [x] 1. Collapse the duplicate table-format layout generators in `table.clj`; share box-drawing character sets via `impl/box.clj`.
- [x] 2. Honor `:raw?` for `:html` output and document explicitly which spec keys are intentionally ignored.
- [x] 3. Fix the `psql` and `org` preset rule rows so the separator length matches the data row width.
- [x] 4. Make `table.clj/align-token` throw `:invalid-table-column` instead of silently defaulting to left.
- [x] 5. Only emit the Markdown header rule row when a header is actually present.
- [x] 6. Wire `--width` into the CLI and document `--from`/`--to`.
- [x] 7. Generate `clj-string-layout.layout` aliases programmatically (226 → 57 lines).
- [x] 8. Add `parse-row-layout` / `explain-row-layout` helpers (positional boolean kept for compat).
- [x] 9. Share option-pair validation between `impl/config.clj` and `impl/parser.clj` via `parse-options!`.
- [x] 10. Make `merge-adjacent-text` private.
- [x] 11. Rename the parser's transient `:column*` IR type to `:column-raw`.
- [x] 12. Slim the README; preset catalog moved to `doc/presets.md`, CLI reference to `doc/cli.md`.
- [x] 13. Document `:fill-char` defaulting to `:align-char`.
- [x] 14. Document that `[Vf]` is accepted but has no rendered effect on verbatim columns.
- [x] 15. Document the center-alignment left bias in the layout language reference.
- [x] 16. Escape the literal `|` inside the syntax-summary table in `doc/layout-language.md`.
- [x] 17. Replace the stale checked-off list with the active follow-ups.

## Completed (user-facing features)

- [x] 18. Add `:title` caption support, rendered above text tables and as `<caption>` for `:html`.
- [x] 19. Add `:footers` to the table API.
- [x] 20. Add `:cell-fn` decoration callback receiving `{:section :row :col :column :value}`.
- [x] 21. Honor `:width` for generated table formats via `:fill?`.
- [x] 22. Add `layout-into!` / `table-into!` writer sinks for streaming output.

## Future ideas

- Add `<thead>`, `<tbody>`, `<tfoot>` wrapping for `:html` output as an opt-in spec key.
- Add a `:row-fn` callback for whole-row decoration (e.g. striped backgrounds).
- Add a `cli/main` adapter that lets external scripts invoke the CLI without spawning a subprocess.
- Add an Org/RST/markdown-fill-aware preset that mirrors the box-fill variants.
