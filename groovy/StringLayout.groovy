package com.iteego.glasir.util

/**
 * A simple grid based layout manager for text strings
 * 
 * <p>Essentially this class does for terminal string output
 * what the html table element does for formatting on 
 * an html page. It formats string data into columns 
 * allocating appropriate space so that the widest element 
 * given in the indata can fit into each column.</p>
 *
 * <p>Example usage: 
 * <pre> 
 * // Indata can be a string with rows separated by \n
 * // and words separated by spaces _or_ a List&lt;List&lt;String&gt;&gt;
 * // both of which represent 'rows' of data to be aligned in columns. 
 * def rows = "a b\naa bb"
 * // create a layout with two columns and a space in between
 * def layout = new StringLayout(width: 80, layoutString: '[L] [R]')
 * def formatted = layout.layout(rows)
 * assert formatted == ["a   b", "aa bb"]
 * </pre>
 * where one of the three spaces between the a and the b on the "a   b" row 
 * is there because of the space in the middle of the format 
 * string '[L] [R]' and the two other spaces on the "a   b" row
 * are there because the other row "aa bb" had wider elements and 
 * 'a' was left aligned and 'b' was right aligned. 
 * </p>
 *
 * <p>The layout manager currently supports the following layout 
 * string syntax elements: 
 * <ul> 
 *   <li> Column layout specification using either 'L' or 'R' for 
 *        left or right justification. Column layout specifiers 
 *        are enclosed in square brackets in the layout string</li>
 *   <li> Before, after, and in between the column layout specifications
 *        you can enter either characters of your choice (which will 
 *        be included verbatim) or a single instance of the special 
 *        keyword 'fill'. Verbatim characters would typically be one 
 *        or more spaces, but separating the grid with characters 
 *        other than white space is equally valid</li>
 * </ul>
 * </p>
 *
 * <p>An example of a more complex layout string: 
 * <pre> 
 * [L] [L] [L]fill[R] [R]
 * </pre>
 * which would create a five column layout with the three leftmost columns
 * being left aligned and the two rightmost columns being right aligned. All
 * extra space (that is, space left over in 'width' once the largest item in each column 
 * has been fitted in) is given to the gap between the third and the fourth 
 * column. 
 * </p>
 *
 * <p>In the example above, the gaps between the three first and the two 
 * last columns are always one space at a minimum (as there is one space 
 * between them in the layout string). We say at a minimum because there 
 * will be one space between those elements on the rows where the elements
 * are the widest.</p>
 * 
 * Created by mbjarland on 30/04/15.
 */
class StringLayout {
  /**
   * Enumeration for supported column alignment types 
   */
  enum Align {
    LEFT, RIGHT
    public static Align parse(String s) {
      switch (s) {
        case ['l','L']:
          LEFT
          break
        case ['r','R']:
          RIGHT
          break
        default:
          throw new RuntimeException("Unsupported layout specifier '${s}' encountered for StringLayout")
      }
    }
  }

  /**
   * The layout string to use when formatting a row of data
   */
  String layoutString

  /**
   * The maximum width of a row. When using a 'fill' specifier in the 
   * layout string, the fill gap will be expanded until the row is
   * 'width' characters wide. Defaul: 80
   */
  int width = 80

  /**
   * Character to use when aligning columns. Default: one space
   */
  String alignChar = ' '

  /**
   * Layout a string of data representing rows. This method is a 
   * shorthand for calling the method which takes a List&lt;List&lt;String&gt;&gt;
   * where this method defaults to splitting rows on '\n' and splitting
   * words on a line on ' ' (space).
   * 
   * @param rows The string data to format
   * @return A list of formatted rows represented as strings
   */
  public List<String> layout(String rows) {
    layout(rows.tokenize("\n").collect { it.tokenize(' ') } )
  }

  /**
   * Layout a list of rows into a grid according to the configured layout 
   * string. 
   * 
   * @param rows a list of 'rows' where each row is a list of words or 
   *        elements to be formatted. 
   * @return A list of formatted rows represented as strings. 
   */
  public List<String> layout(List<List<String>> rows) {
    if (rows.size() == 0) return []

    def (aligns, spaces) = layoutStringParsed

    def colWidths = rows.transpose().collect { List<String> col ->
      col.max { String word -> word.length() }.length()
    }

    def align = { String word, int wi ->
      switch(aligns[wi]) {
        case Align.LEFT:  return word.padRight(colWidths[wi], alignChar)
        case Align.RIGHT: return word.padLeft(colWidths[wi], alignChar)
        default: throw new RuntimeException("Unsupported alignment operation '${aligns[wi]}' encountered, index: $wi, aligns: $aligns!")
      }
    }


    int fillWidth = width - (colWidths.sum() + spaces.collect { s -> s == 'fill' ? 0 : s.length() }.sum())
    if (fillWidth < 0) fillWidth = 0

    def space = { int wi ->
      spaces[wi] == 'fill' ? (alignChar*fillWidth) : spaces[wi]
    }

    rows.collect { List<String> row ->
      // can not use
      // row.indexed().inject(space(0)) { rowResult, wi, String word
      // as indexed() is a groovy 2.4.x feature
      def wi = -1
      row.inject(space(0)) { rowResult, String word -> 
        wi++
        rowResult << align(word, wi) << space(wi+1)
      } as String
    }
  }

  private void validateRows(List<List<String>> rows) {
    def size = rows.first().size()

    rows.each { row ->
      if (row.size() != size) throw new RuntimeException("Invalid row set encountered, all rows must have the same number of elements on them!")
    }
  }

  List getLayoutStringParsed() {
    if (!layoutString) throw IllegalStateException("No layoutString specified!")

    def aligns = []
    def spaces = ['']

    boolean inBrace = false
    layoutString.each { c ->
      if (c == '[') return inBrace = true
      if (c == ']') {
        inBrace = false
        spaces << ''
        return
      }
      if (inBrace)  return aligns << Align.parse(c)

      spaces[-1] += c

    }

    [aligns, spaces.collect { it.toLowerCase() == 'fill' ? 'fill' : it }]
  }
}

