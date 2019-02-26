(ns clj-string-layout.example)


(comment
  ; Filosophy - col layout lays out the horizontal alignment
  ;           - row layout lays out
  ;
  ; row indexes can be read as "the 'virtual' row above data row x"
  ; we always have an implicit first and last row index. So for a
  ; zero row data, we still have virtual row indexes zero and one
  ;
  ;
  ; ROWS
  ; data size 0
  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; └───────────────┴───────────────┴───────┘ ← 1
  ;
  ; data size 1
  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │ Are           │ Cool  │
  ; └───────────────┴───────────────┴───────┘ ← 1
  ;
  ; data size 2 (..n)
  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │ Are           │ Cool  │
  ; ├───────────────┼───────────────┼───────┤ ← 1
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; └───────────────┴───────────────┴───────┘ ← 2 (n)
  ;

  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │ Are           │ Cool  │
  ; ├───────────────┼───────────────┼───────┤ ← 1
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; ├───────────────┼───────────────┼───────┤ ← 2
  ; │ col 2 is      │   centered    │   $12 │
  ; ├───────────────┼───────────────┼───────┤ ← 3
  ; │ zebra stripes │ are neat      │    $1 │
  ; └───────────────┴───────────────┴───────┘ ← 4
  {:layout {:cols   "│ [L] │ [C] │ [R] │"
            :rows [["┌─[─]─┬─[─]─┬─[─]─┐" :apply-for first-row?]
                   ["├─[─]─┼─[─]─┼─[─]─┤" :apply-for interior-row?]
                   ["└─[─]─┴─[─]─┴─[─]─┘" :apply-for last-row?]]}}

  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │ Are           │ Cool  │
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; │ col 2 is      │   centered    │   $12 │
  ; │ zebra stripes │ are neat      │    $1 │
  ; └───────────────┴───────────────┴───────┘ ← 4
  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐"] :apply-for first-row?
                ["└─[─]─┴─[─]─┴─[─]─┘"] :apply-for last-row?]}


  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │ Are           │ Cool  │
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; │ col 2 is      │   centered    │   $12 │
  ; │ zebra stripes │ are neat      │    $1 │
  ; └───────────────┴───────────────┴───────┘ ← 4
  {:col-layout "│ [L] │f[C]f│ [R] │"
   :row-layout [["┌─[─]─┬f[─]f┬─[─]─┐" :apply-for first-row? :fill-char \-]
                ["└─[─]─┴f[─]f┴─[─]─┘" :apply-for last-row?]]}

  ; ↓             ↓               ↓       ↓
  ; ┌─────────────────────────────────────┐ ← 0
  ; │ Tables         Are            Cool  │
  ; │ col 3 is       right-aligned  $1600 │
  ; │ col 2 is         centered       $12 │
  ; │ zebra stripes  are neat          $1 │
  ; └─────────────────────────────────────┘ ← 4

  {:col-layout   "│ [L]f[C]f[R] │"
   :row-layout [["┌─[─]f[─]f[─]─┐" :apply-for first-row? :fill-chars [\- \-]]
                ["└─[─]f[─]f[─]─┘" :apply-for last-row?]]}

  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │ Are           │ Cool  │
  ; ├───────────────┼───────────────┼───────┤ ← 1
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; │ col 2 is      │   centered    │   $12 │
  ; │ zebra stripes │ are neat      │    $1 │
  ; └───────────────┴───────────────┴───────┘ ← 4

  {:col-layout "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐" :apply-for first-row?]
                ["├─[─]─┼─[─]─┼─[─]─┤" :apply-for second-row?]
                ["└─[─]─┴─[─]─┴─[─]─┘" :apply-for last-row?]]}

  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │     Are       │  Cool │
  ; ┝━━━━━━━━━━━━━━━┿━━━━━━━━━━━━━━━┿━━━━━━━┥ ← 1
  ; │ col 3 is      │   centered    │ $1600 │
  ; ├───────────────┼───────────────┼───────┤ ← 2
  ; │ col 2 is      │   centered    │   $12 │
  ; ┝━━━━━━━━━━━━━━━┿━━━━━━━━━━━━━━━┿━━━━━━━┥ ← 3
  ; │ col 2 is      │   centered    │   $12 │
  ; ├───────────────┼───────────────┼───────┤ ← 4
  ; │ zebra stripes │   are neat    │    $1 │
  ; └───────────────┴───────────────┴───────┘
  {:col-layout "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐" :apply-for first-row?]
                ["┝━[━]━┿━[━]━┿━[━]━┥" :apply-for (every-pred interior-row? even-row?)]
                ["├─[─]─┼─[─]─┼─[─]─┤" :apply-for (every-pred not-last-row? odd-row?)]
                ["└─[─]─┴─[─]─┴─[─]─┘" :apply-for last-row?]]}

  ; | Tables        | Are           | Cool  |
  ; | ------------- |:-------------:| -----:|
  ; | col 3 is      | right-aligned | $1600 |
  ; | col 2 is      | centered      |   $12 |
  ; | zebra stripes | are neat      |    $1 |

  {:col-layout "│ [L] │ [C] │ [R] │"
   :row-layout [["| [-] |:[-]:| [-]:|" :apply-for second-row?]]}

  ; | Tables        | Are           | Cool  |
  ; | ------------- |:-------------:| -----:|
  ; | col 3 is      | right-aligned | $1600 |
  ; | col 2 is      | centered      |   $12 |
  ; | zebra stripes | are neat      |    $1 |

  {:col-layout "│ [L] │ f[C]f │ [R] │"
   :row-layout [["| [-] |:f[-]f:| [-]:|" :apply-for second-row? :fill-char \-]]}

  )
