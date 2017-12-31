(ns string-layout.example)

(defn first-row? [[idx _]] (zero? idx))
(def not-first? (complement first-row?))
(defn second? [[idx _]] (= idx 1))

(defn last-row? [[idx cnt]]
  (= idx cnt))
(def not-last-row? (complement last-row?))
(def interior-row? (complement last-row?))
(defn always? [[_ _]] true)


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
  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐" :apply-when first-row?]
                ["├─[─]─┼─[─]─┼─[─]─┤" :apply-when not-last-row?]
                ["└─[─]─┴─[─]─┴─[─]─┘" :apply-when last-row?]]}

  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │ Are           │ Cool  │
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; │ col 2 is      │   centered    │   $12 │
  ; │ zebra stripes │ are neat      │    $1 │
  ; └───────────────┴───────────────┴───────┘ ← 4
  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐"] :apply-when first-row?
                ["└─[─]─┴─[─]─┴─[─]─┘"] :apply-when last-row?]}


  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │ Are           │ Cool  │
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; │ col 2 is      │   centered    │   $12 │
  ; │ zebra stripes │ are neat      │    $1 │
  ; └───────────────┴───────────────┴───────┘ ← 4
  {:col-layout   "│ [L] │f[C]f│ [R] │"
   :row-layout [["┌─[─]─┬f[─]f┬─[─]─┐" :apply-when first-row? :fill-char \-]
                ["└─[─]─┴f[─]f┴─[─]─┘" :apply-when last-row?]]}

  ; ↓             ↓               ↓       ↓
  ; ┌─────────────────────────────────────┐ ← 0
  ; │ Tables         Are            Cool  │
  ; │ col 3 is       right-aligned  $1600 │
  ; │ col 2 is         centered       $12 │
  ; │ zebra stripes  are neat          $1 │
  ; └─────────────────────────────────────┘ ← 4

  {:col-layout   "│ [L]f[C]f[R] │"
   :row-layout [["┌─[─]f[─]f[─]─┐" :apply-when first-row? :fill-chars [\- \-]]
                ["└─[─]f[─]f[─]─┘" :apply-when last-row?]]}

  ; ↓               ↓               ↓       ↓
  ; ┌───────────────┬───────────────┬───────┐ ← 0
  ; │ Tables        │ Are           │ Cool  │
  ; ├───────────────┼───────────────┼───────┤ ← 1
  ; │ col 3 is      │ right-aligned │ $1600 │
  ; │ col 2 is      │   centered    │   $12 │
  ; │ zebra stripes │ are neat      │    $1 │
  ; └───────────────┴───────────────┴───────┘ ← 4

  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐" :apply-when first-row?]
                ["├─[─]─┼─[─]─┼─[─]─┤" :apply-when second-row?]
                ["└─[─]─┴─[─]─┴─[─]─┘" :apply-when last-row?]]}

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
  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [["┌─[─]─┬─[─]─┬─[─]─┐" :apply-when first-row?]
                ["┝━[━]━┿━[━]━┿━[━]━┥" :apply-when (every-pred interior-row? even-row?)]
                ["├─[─]─┼─[─]─┼─[─]─┤" :apply-when (every-pred not-last-row? odd-row?)]
                ["└─[─]─┴─[─]─┴─[─]─┘" :apply-when last-row?]]}

  ; | Tables        | Are           | Cool  |
  ; | ------------- |:-------------:| -----:|
  ; | col 3 is      | right-aligned | $1600 |
  ; | col 2 is      | centered      |   $12 |
  ; | zebra stripes | are neat      |    $1 |

  {:col-layout   "│ [L] │ [C] │ [R] │"
   :row-layout [["| [-] |:[-]:| [-]:|" :apply-when second-row?]]}

  ; | Tables        | Are           | Cool  |
  ; | ------------- |:-------------:| -----:|
  ; | col 3 is      | right-aligned | $1600 |
  ; | col 2 is      | centered      |   $12 |
  ; | zebra stripes | are neat      |    $1 |

  {:col-layout   "│ [L] │ f[C]f │ [R] │"
   :row-layout [["| [-] |:f[-]f:| [-]:|" :apply-when second-row? :fill-char \-]]}



  )
