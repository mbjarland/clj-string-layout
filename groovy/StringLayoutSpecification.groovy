package com.iteego.glasir.util

import spock.lang.Unroll
import spock.lang.Specification

/**
 * http://code.google.com/p/spock/wiki/SpockBasics
 */
class StringLayoutSpecification extends Specification {
  @Unroll
  def "Expect rows #rows, layoutString #layoutString and width #width to result in #expectedResult"() {
    setup:
      def layout = new StringLayout(width: width, layoutString: layoutString)
      def formatted = layout.layout(rows)

    expect:
      expectedResult == formatted
      if ('fill' in layoutString ) formatted.collect { it.length() } == [width] * formatted.size()

    where:
      rows                   | layoutString   | width || expectedResult
      "a b"                  | '[L] [R]'      | 20    || ["a b"]
      "a b"                  | '[L] [R]'      |  0    || ["a b"]
      "a b\naa bb"           | '[L] [R]'      | 20    || ["a   b", "aa bb"]
      "a b\naa bb"           | '[L] [R]'      |  0    || ["a   b", "aa bb"]
      "a b\naa bb"           | '[L]  [R]'     | 20    || ["a    b", "aa  bb"]
      "a b\naa bb"           | '[L]  [R]'     |  0    || ["a    b", "aa  bb"]
      "a b"                  | '[L]fill[R]'   | 20    || ["a                  b"]
      "a b"                  | '[L]fill[R]'   |  0    || ["ab"]
      "a b\naa bb"           | '[L]fill[R]'   | 10    || ["a        b", "aa      bb"]
      "a b\naa bb"           | '[L]fill[R]'   |  0    || ["a  b", "aabb"]
      "a b\naa bb"           | 'fill[R] [R]'  | 10    || ["      a  b", "     aa bb"]
      "a b\naa bb"           | 'fill[R] [R]'  |  0    || [" a  b", "aa bb"]
      "a b\naa bb"           | '[R] [R]fill'  | 10    || [" a  b     ", "aa bb     "]
      "a b\naa bb"           | '[R] [R]fill'  |  0    || [" a  b", "aa bb"]
  }

  //> glasir.build 1.4.0-SNAPSHOT 6e0c87                built 2015.Apr.29 16:15:27
  //> glasir.core 1.15-groovy-2.3-SNAPSHOT 41d8c8       built 2015.Apr.29 11:30:00
  def "Test glasir.build version logging alignment"() {
    setup:
      def rows = [
          ['glasir.build', '1.4.0-SNAPSHOT',           '6e0c87', 'built 2015.Apr.29 16:15:27'],
          ['glasir.core',  '1.15-groovy-2.3-SNAPSHOT', '41d8c8', 'built 2015.Apr.29 11:30:00'],

      ]
      def layout = new StringLayout(layoutString: "[L] [L] [L]fill[R]", width: 80)

    when:
      def result = layout.layout(rows)

    then:
      result.first() == "glasir.build 1.4.0-SNAPSHOT           6e0c87          built 2015.Apr.29 16:15:27"
      result.last()  == "glasir.core  1.15-groovy-2.3-SNAPSHOT 41d8c8          built 2015.Apr.29 11:30:00"
  }
}