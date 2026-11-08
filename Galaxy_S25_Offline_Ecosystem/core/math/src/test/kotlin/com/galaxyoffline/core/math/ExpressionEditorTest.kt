package com.galaxyoffline.core.math

import com.galaxyoffline.core.math.ExpressionEditor as E
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpressionEditorTest {
    @Test fun operatorsReplaceEachOther() {
        assertEquals("5+", E.operator("5×", '+'))
        assertEquals("5×−", E.operator("5×", '−'))
        assertEquals("5+", E.operator("5×−", '+'))
        assertEquals("−", E.operator("", '−'))
        assertEquals("", E.operator("", '×'))
    }

    @Test fun decimalsAndParens() {
        assertEquals("0.", E.decimal(""))
        assertEquals("1.5", E.decimal("1.5"))
        assertEquals("2×0.", E.decimal("2×"))
        assertEquals("(", E.smartParen(""))
        assertEquals("(2)", E.smartParen("(2"))
        assertEquals("(2+(", E.smartParen("(2+"))
        assertEquals("(1+(2))", E.closeAll("(1+(2"))
    }

    @Test fun backspaceAndSign() {
        assertEquals("2+", E.backspace("2+sin("))
        assertEquals("1", E.backspace("12"))
        assertEquals("2+(−5", E.toggleSign("2+5"))
        assertEquals("2+5", E.toggleSign("2+(−5"))
        assertEquals("5%", E.postfix("5", '%'))
        assertEquals("5%", E.postfix("5%", '%'))
        assertEquals("+", E.postfix("+", '!'))
    }

    @Test fun exponentAndImplicitTimes() {
        assertEquals("5×10^(", E.function("5", "10^("))
        assertEquals("sin(", E.function("", "sin("))
        assertEquals("1E", E.exponent("1"))
        assertEquals("1E5", E.exponent("1E5"))
        assertEquals("", E.exponent(""))
        assertEquals("1E−", E.operator("1E", '−'))
    }
}
