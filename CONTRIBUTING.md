# Contributing

Thanks for taking the time to look at `clj-string-layout`. This guide covers
the workflow expected for changes that ship.

## Getting set up

The project uses the Clojure CLI plus Babashka. Both are needed to fully
exercise CI locally.

```sh
brew install clojure/tools/clojure babashka
```

A JDK ≥ 11 is required for the JVM test suite.

## Running the checks

```sh
clojure -M:test     # JVM test suite (60 tests, includes test.check)
bb bb-test          # Babashka subset (skips the property tests)
clojure -M:lint     # clj-kondo
clojure -M:bench    # parse + render benchmarks on the JVM
bb bb-bench         # the same benchmarks under Babashka
```

CI runs all of these on every push and pull request against `master` across
Java 11, 17, and 21. The `Reflection check` CI step loads every source
namespace with `*warn-on-reflection*` true and fails on any warning; each
source file also sets the flag itself so REPL-loaded changes catch
reflection regressions immediately.

## Code expectations

- **No third-party Clojure dependencies on the runtime classpath.** The
  library is required to run under Babashka without a pod, so every new
  feature must work under both Clojure and SCI. Add tests in
  `clj-string-layout.bb-test-runner` if a feature has a Babashka angle.
- **Type-hint Java interop call sites.** The CI reflection step is strict;
  prefer narrowing the hint over disabling it.
- **Keep public docstrings descriptive.** Every public var should explain
  what it returns, what its arguments mean, and any non-obvious caveats.
- **Use structured errors.** New error paths should throw `ex-info` with a
  `:type` key listed in `doc/errors.md`.

## Releasing

Releases are tag-driven. Update `version.edn`, fold the Unreleased section
of `CHANGELOG.md` into a new dated entry, push a `v<version>` tag, and the
release workflow handles the rest:

```sh
git tag -a vX.Y.Z -m "Release vX.Y.Z"
git push origin vX.Y.Z
```

The workflow re-runs the full Java 11/17/21 matrix plus the Babashka
checks, verifies the tag matches `version.edn`, builds the jar, deploys
to [Clojars](https://clojars.org/io.github.mbjarland/clj-string-layout),
POSTs to `cljdoc.org/api/request-build2` so the documentation is
indexed immediately, and creates a GitHub Release with the jar attached.

Documentation lives at
[cljdoc.org](https://cljdoc.org/d/io.github.mbjarland/clj-string-layout/CURRENT).
The nav tree is `doc/cljdoc.edn`; the jar bundles README, CHANGELOG,
LICENSE, and every file in `doc/` so cljdoc can render them.

## Reporting bugs

Open an issue with:
- The shortest layout config or table spec that reproduces the problem.
- The expected output and the actual output (or stack trace).
- Whether the problem reproduces under both `clojure -M:test` and `bb`.

## License

By contributing you agree that your contribution will be licensed under the
Eclipse Public License 1.0, the same license as the rest of the project.
