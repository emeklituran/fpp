package com.galaxyoffline.core.math

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpressionEngineTest {
    private fun v(e: String, angle: AngleMode = AngleMode.DEG, ans: Double = 0.0): Double {
        val r = ExpressionEngine.evaluate(e, angle, ans)
        assertTrue("'$e' -> $r", r is CalcResult.Value)
        return (r as CalcResult.Value).value
    }
    private fun err(e: String) = assertTrue(e, ExpressionEngine.evaluate(e) is CalcResult.Error)

    @Test fun precedenceAndAssociativity() {
        assertEquals(14.0, v("2+3×4"), 1e-12)
        assertEquals(20.0, v("(2+3)×4"), 1e-12)
        assertEquals(-4.0, v("−2^2"), 1e-12)
        assertEquals(512.0, v("2^3^2"), 1e-9)
        assertEquals(0.5, v("2^−1"), 1e-12)
        assertEquals(2.5, v("10÷4"), 1e-12)
        assertEquals(-15.0, v("5×−3"), 1e-12)
    }

    @Test fun smartPercent() {
        assertEquals(220.0, v("200+10%"), 1e-9)
        assertEquals(180.0, v("200−10%"), 1e-9)
        assertEquals(20.0, v("200×10%"), 1e-9)
        assertEquals(0.5, v("50%"), 1e-12)
    }

    @Test fun functionsAndConstants() {
        assertEquals(0.5, v("sin(30)"), 1e-12)
        assertEquals(0.0, v("cos(90)"), 1e-12)
        assertEquals(1.0, v("tan(45)"), 1e-12)
        assertEquals(30.0, v("asin(0.5)"), 1e-9)
        assertEquals(1.0, v("sin(π÷2)", AngleMode.RAD), 1e-12)
        assertEquals(3.0, v("√9"), 1e-12)
        assertEquals(3.0, v("√(9)"), 1e-12)
        assertEquals(2.0, v("log(100)"), 1e-12)
        assertEquals(1.0, v("ln(e)"), 1e-12)
        assertEquals(120.0, v("5!"), 1e-12)
        assertEquals(2 * Math.PI, v("2π"), 1e-12)
        assertEquals(12.0, v("3(4)"), 1e-12)
        assertEquals(1500.0, v("1.5E3"), 1e-9)
        assertEquals(10.0, v("Ans×2", ans = 5.0), 1e-12)
        assertEquals(9.0, v("(1+2)×(2+1"), 1e-12) // eksik parantez
    }

    @Test fun errors() {
        err("5÷0")
        err("√(−4)")
        err("ln(0)")
        err("2.5!")
        assertTrue(ExpressionEngine.evaluate("tan(90)", AngleMode.DEG) is CalcResult.Error)
        err("1..2")
        err("×")
        err("")
    }

    @Test fun formatting() {
        assertEquals("0,3", CalcFormat.result(v("0.1+0.2")))
        assertEquals("1.234.567,89", CalcFormat.result(1234567.89))
        assertEquals("1234567,89", CalcFormat.result(1234567.89, grouping = false))
        assertEquals("1E20", CalcFormat.result(1e20))
        assertEquals("−5", CalcFormat.result(-5.0))
        assertEquals("1.234×5,5", CalcFormat.expression("1234×5.5"))
        assertEquals("−12.5", CalcFormat.literal(-12.5))
    }

    @Test fun preview() {
        assertEquals(5.0, ExpressionEngine.preview("2+3+")!!, 1e-12)
        assertEquals(null, ExpressionEngine.preview("12"))
    }
}
