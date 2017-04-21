
def (aligns, spaces) = getLayoutStringParsed(args[0])

println "aligns: [${aligns.join('|')}]"
println "spaces: [${spaces.join('|')}]"

List getLayoutStringParsed(layoutString) {
  if (!layoutString) throw new Exception("No layoutString specified!")

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
