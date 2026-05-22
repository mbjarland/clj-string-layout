# Command Line Interface

`clj-string-layout` ships a command-line formatter that reads CSV, TSV, or
whitespace-separated input from stdin or a file and writes a formatted table
to stdout. The same entry point is exposed as a Babashka task for fast
startup.

## Quick Examples

```sh
clojure -M:cli -- --input csv --format markdown --headers data.csv
clojure -M:cli -- --input tsv --format ascii-grid < data.tsv
clojure -M:cli -- --from csv --to box --headers data.csv
```

Babashka users have two options. `bb format` shells out to the JVM CLI and
mirrors the `clojure -M:cli` flags:

```sh
bb format -- --input csv --format markdown --headers data.csv
bb format -- --input tsv --format ascii-grid < data.tsv
```

`bb bb-format` runs the same logic natively under Babashka with no JVM
startup, so it returns in tens of milliseconds instead of half a second:

```sh
bb bb-format --from csv --to box --headers < data.csv
```

The library has no third-party Clojure dependencies, so requiring it from a
Babashka script also works directly:

```clojure
#!/usr/bin/env bb
(require '[clj-string-layout.table :as table])
(println (table/table-str {:format :box
                           :headers ["Name" "Qty"]
                           :rows [["apple" 12]]}))
```

`bb test` runs the full JVM test suite, `bb bb-test` runs the Babashka-only
subset (skipping the test.check property tests). `bb lint`, `bb bench`, and
`bb jar` are also available.

## Options

| Option | Meaning |
| --- | --- |
| `--input FORMAT`, `--from FORMAT` | Input format. One of `csv`, `tsv`, `whitespace`. Defaults to `csv`. |
| `--format FORMAT`, `--to FORMAT` | Output format. Any value returned by `clj-string-layout.table/formats`. Defaults to `plain`. |
| `--headers` | Treat the first input row as headers. |
| `--no-headers` | Treat every input row as data. |
| `--no-escape` | Disable output-format escaping (assume the input is already safe). |
| `--width N` | Target width for fill-aware output formats. |
| `-h`, `--help` | Show built-in help. |

Run `clojure -M:cli -- --help` for the auto-generated help that always
reflects the currently registered table formats.

## Supported Input Formats

| Format | Behavior |
| --- | --- |
| `csv` | RFC 4180-style parser with quoted fields, doubled quotes, CR/LF row separators, and embedded line breaks. Lenient about text after closing quotes. |
| `tsv` | Tab-separated values, one row per line. |
| `whitespace` | Whitespace-separated tokens, one row per non-blank line. |

## Supported Output Formats

The CLI accepts every value returned by `clj-string-layout.table/formats`. As
of this writing that includes `plain`, `markdown`, `markdown-left`,
`markdown-center`, `markdown-right`, `box`, `unicode-box`, `ascii-box`,
`double-box`, `unicode-double-box`, `ascii-double-box`, `ascii-grid`, `csv`,
`tsv`, `pipe`, `psql`, `org`, `rst`, and `html`. See
[the table API guide](table-api.md) for what each format produces.

## Programmatic Use

`cli/render` is the same entry point invoked by `-main`, but returns a vector
of output lines instead of printing or exiting. It accepts every key the
command-line flags produce, plus optional `:width` (integer) and
`:display-width` (a function from string to display width, useful for ANSI or
wide-glyph data):

```clojure
(require '[clj-string-layout.cli :as cli])

(cli/render {:input :csv :format :markdown :headers? true}
            "Name,Note\nalice,\"a,b\"\n")
;; => ["| Name  | Note |"
;;     "|:----- |:---- |"
;;     "| alice | a,b  |"]
```
