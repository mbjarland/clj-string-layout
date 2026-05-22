# Command Line Interface

`clj-string-layout` ships a command-line formatter that reads CSV, TSV, or
whitespace-separated data from stdin or a file and writes a formatted table
to stdout. The same entry point is exposed three ways: through the Clojure
CLI, through a Babashka task that shells out to the JVM, and through a
Babashka task that runs natively under SCI with no JVM startup at all.

## Synopsis

```text
clojure -M:cli -- [options] [file]
bb format     -- [options] [file]
bb bb-format     [options] [file]

  Reads input from FILE, "-", or stdin. Writes a formatted table to stdout.
  Exits 0 on success, 2 on argument or input error.
```

## Quick examples

```sh
# CSV with a header row, rendered as Markdown
clojure -M:cli -- --from csv --to markdown --headers data.csv

# Tab-separated input, ASCII-grid output, via Babashka (no JVM)
bb bb-format --from tsv --to ascii-grid < data.tsv

# Whitespace-separated input, box-drawing output, expanded to 60 columns
bb bb-format --from whitespace --to box --headers --width 60 --fill < data.txt

# Pipe through a Unix tool, then back through the formatter
printf 'item,qty\npear,4\napple,12\nkiwi,8\n' \
  | bb bb-format --from csv --to csv --headers \
  | sort \
  | bb bb-format --from csv --to box
```

## Options

| Option | Meaning |
| --- | --- |
| `--input FORMAT`, `--from FORMAT` | Input format. One of `csv`, `tsv`, `whitespace`. Defaults to `csv`. |
| `--format FORMAT`, `--to FORMAT` | Output format. Any value returned by `clj-string-layout.table/formats`. Defaults to `plain`. |
| `--headers` | Treat the first input row as headers. |
| `--no-headers` | Treat every input row as data (the default). |
| `--no-escape` | Disable output-format escaping. Use this if the input is already escaped for the target format. |
| `--width N` | Target total width. Only takes effect when paired with `--fill` on a generated format (`plain`, the markdown variants, the box variants, `ascii-grid`). |
| `--fill` | Expand cell padding to consume `--width`. Has no effect without `--width`. |
| `-h`, `--help` | Print built-in help and exit. |

`--help` is generated from the live `cli/formats` registry, so it always
reflects the formats currently available.

## Inputs

### Source

| Form | Behaviour |
| --- | --- |
| `clojure -M:cli -- data.csv` | Read `data.csv`. |
| `clojure -M:cli -- -` | Read stdin (the `-` is explicit). |
| `clojure -M:cli --` | No file given — read stdin. |
| `clojure -M:cli -- a.csv b.csv` | Error — only one input file is supported. |

The same rules apply to `bb format` and `bb bb-format`.

### CSV (`--from csv`)

RFC 4180-style parser. Handles quoted fields, doubled quotes inside quoted
fields, `CR`/`LF`/`CRLF` row separators, and embedded line breaks inside
quoted fields. The parser is intentionally lenient about text after a
closing quote, so imperfect CSV exports usually still load.

```sh
$ printf 'name,note\nalice,"a, b"\nbob,"line1\nline2"\n' \
    | bb bb-format --from csv --to box --headers
┌───────┬─────────────┐
│ name  │ note        │
├───────┼─────────────┤
│ alice │ a, b        │
├───────┼─────────────┤
│ bob   │ line1
line2 │
└───────┴─────────────┘
```

Note: embedded newlines inside cells stay literal. The formatter will not
split them into additional table rows; the output line breaks inside the
cell. For multi-line cell content that you want laid out as multiple rows,
preprocess the input.

### TSV (`--from tsv`)

Tab-separated values. One row per line. No quoting — every tab is a
separator. Use `--no-escape` if your data already contains backslash
escape sequences that match the TSV escaper's output.

```sh
$ printf 'name\tqty\tprice\napple\t12\t1.50\npear\t4\t2.00\n' \
    | bb bb-format --from tsv --to markdown --headers
| name  | qty | price |
|:----- |:--- |:----- |
| apple | 12  | 1.50  |
| pear  | 4   | 2.00  |
```

### Whitespace (`--from whitespace`)

Splits each non-blank line on runs of whitespace. Trims leading and
trailing whitespace. Blank lines are skipped. Useful for reformatting
output from `ps`, `df`, `ls -l`, or similar.

```sh
$ printf '  name   qty   price\n  apple  12    1.50\n  pear    4   2.00\n' \
    | bb bb-format --from whitespace --to ascii-grid --headers
+-------+-----+-------+
| name  | qty | price |
+-------+-----+-------+
| apple | 12  | 1.50  |
+-------+-----+-------+
| pear  | 4   | 2.00  |
+-------+-----+-------+
```

## Outputs

The CLI accepts every keyword in `clj-string-layout.table/formats`. Same
input, four representative outputs:

```sh
$ INPUT='item,qty,price\napple,12,1.50\npear,4,2.00\n'

$ printf "$INPUT" | bb bb-format --headers       # --to plain (default)
item   qty  price
apple  12   1.50
pear   4    2.00

$ printf "$INPUT" | bb bb-format --to markdown --headers
| item  | qty | price |
|:----- |:--- |:----- |
| apple | 12  | 1.50  |
| pear  | 4   | 2.00  |

$ printf "$INPUT" | bb bb-format --to psql --headers
 item   | qty | price
--------+-----+------
 apple  | 12  | 1.50
 pear   | 4   | 2.00

$ printf "$INPUT" | bb bb-format --to box --headers
┌───────┬─────┬───────┐
│ item  │ qty │ price │
├───────┼─────┼───────┤
│ apple │ 12  │ 1.50  │
├───────┼─────┼───────┤
│ pear  │ 4   │ 2.00  │
└───────┴─────┴───────┘
```

The other available output formats are `markdown-left`, `markdown-center`,
`markdown-right`, `double-box`, `unicode-box`, `unicode-double-box`,
`ascii-box`, `ascii-double-box`, `ascii-grid`, `csv`, `tsv`, `pipe`, `org`,
`rst`, and `html`. See the [examples gallery](examples-gallery.md) for the
same data rendered through every named format.

## Width and fill

By default, every named format auto-sizes to its content. To produce
fixed-width output, pair `--width` with `--fill`:

```sh
$ printf 'name,qty\napple,12\n' | bb bb-format --to box --headers --width 40 --fill
┌───────────────────┬──────────────────┐
│ name              │ qty              │
├───────────────────┼──────────────────┤
│ apple             │ 12               │
└───────────────────┴──────────────────┘
```

`--fill` is what makes the columns expand. Without it `--width` is silently
ignored for the box/markdown/ascii-grid/plain formats (they have no fill
markers to expand into). For formats that have no width semantics at all
(`csv`, `tsv`, `pipe`, `html`), `--width` and `--fill` are both ignored.

## Piping

The CLI is a well-behaved Unix filter: stdin in, stdout out, exit 0 on
success. That makes composition straightforward.

```sh
# pipe from curl
curl -s https://example.com/data.csv | bb bb-format --to box --headers

# reformat then sort by the first column, then re-render as a box
printf 'item,qty\npear,4\napple,12\nkiwi,8\n' \
  | bb bb-format --to csv --headers \
  | sort \
  | bb bb-format --to box

# read CSV, emit Markdown, copy to the clipboard (macOS)
bb bb-format --to markdown --headers data.csv | pbcopy
```

The `--to csv` round-trip is especially handy: it normalises the input,
escapes CSV-unsafe characters, and gives you a known-good intermediate
representation for downstream Unix tools.

## Exit codes

| Code | Meaning |
| --- | --- |
| `0` | Success. Output written to stdout. |
| `2` | Argument error or input error. Diagnostic on stderr. |

Examples of `2`:

```sh
$ bb bb-format --bogus
Unsupported CLI option
$ echo $?
2

$ echo 'a,b' | bb bb-format --to notarealformat
Unsupported --to value
$ echo $?
2
```

The CLI never throws an uncaught exception in normal operation — any
`ex-info` with a known `:type` is rendered as a one-line diagnostic and
the process exits 2.

## Babashka vs JVM startup

| Path | Cold start | Setup |
| --- | --- | --- |
| `clojure -M:cli --` | ~700 ms | requires JVM and Clojure deps cache |
| `bb format --` | ~700 ms | identical to above; just shells through bb |
| `bb bb-format` | ~50 ms | runs natively under SCI, no JVM |

For one-off pipes the difference is the cost of inhaling a JVM. For
repeated runs (e.g. tight shell loops) it dominates everything else, so
prefer `bb bb-format`. The output is identical.

## Programmatic use

`cli/render` is the same entry point invoked by `-main`, but returns a
vector of output lines instead of printing or exiting. It accepts every
key the command-line flags produce, plus optional `:width` (integer),
`:fill?` (boolean), and `:display-width` (a function from string to
display width, useful when the caller passes ANSI- or wide-glyph data
that the layout engine needs to measure correctly):

```clojure
(require '[clj-string-layout.cli :as cli])

(cli/render {:input :csv :format :markdown :headers? true}
            "Name,Note\nalice,\"a,b\"\n")
;; => ["| Name  | Note |"
;;     "|:----- |:---- |"
;;     "| alice | a,b  |"]

(cli/render {:input :csv :format :box :headers? true
             :width 40 :fill? true}
            "item,qty\napple,12\n")
;; => ["┌───────────────────┬──────────────────┐"
;;     "│ item              │ qty              │"
;;     "├───────────────────┼──────────────────┤"
;;     "│ apple             │ 12               │"
;;     "└───────────────────┴──────────────────┘"]
```

`cli/parse-args` parses a string sequence into the same options map and
is useful when you want CLI-flavoured argument parsing inside your own
program:

```clojure
(cli/parse-args ["--from" "tsv" "--to" "box" "--headers" "--width" "60" "--fill"])
;; => {:input :tsv :format :box :headers? true :escape? true
;;     :width 60 :fill? true}
```

`cli/parse-input` exposes the CSV/TSV/whitespace parsers on their own so
you can reuse them outside the rendering path:

```clojure
(cli/parse-input :csv "name,note\nalice,\"a,b\"\n")
;; => [["name" "note"] ["alice" "a,b"]]
```

## Troubleshooting

| Symptom | Likely cause |
| --- | --- |
| `Input contains no rows` (exit 2) | File or stdin produced zero rows. Check the input format actually matches the data. |
| `Unsupported --to value` (exit 2) | Typo in the output format name. Run `--help` for the live list. |
| `Unsupported --from value` (exit 2) | Same, for input format. |
| `--width must be a non-negative integer` (exit 2) | Pass a positive integer, e.g. `--width 60`. |
| `Only one input file may be supplied` (exit 2) | Pass exactly one file path, or `-` for stdin. |
| Numeric columns end up left-aligned | CSV/TSV/whitespace input is always read as strings; the CLI has no per-column alignment knob. For aligned numerics, use the table API programmatically with `:columns [{:from :qty :align :right}]`. |
| Embedded newlines split a cell across lines | Quoted-CSV newlines stay literal in the output. Preprocess to strip or replace them if your renderer can't handle a multi-line cell. |
| `--width 30` does nothing | Pair it with `--fill`. Without `--fill`, only the explicitly fill-aware presets respect `:width`, and the CLI's default formats are not those presets. |
