package com.galaxyoffline.unitconverter

import com.galaxyoffline.unitconverter.data.Units
import org.junit.Assert.assertEquals
import org.junit.Test

class UnitsTest {
    private fun conv(cat: String, v: Double, from: String, to: String): Double { val c = Units.category(cat); return c.convert(v, c.unit(from), c.unit(to)) }

    @Test fun conversions() {
        assertEquals(100_000.0, conv("length", 1.0, "km", "cm"), 1e-6)
        assertEquals(2.54, conv("length", 1.0, "in", "cm"), 1e-12)
        assertEquals(212.0, conv("temp", 100.0, "c", "f"), 1e-9)
        assertEquals(-40.0, conv("temp", -40.0, "f", "c"), 1e-9)
        assertEquals(0.0, conv("temp", 273.15, "k", "c"), 1e-9)
        assertEquals(2.20462262, conv("mass", 1.0, "kg", "lb"), 1e-8)
        assertEquals(10.0, conv("area", 1.0, "ha", "donum"), 1e-9)
        assertEquals(100.0, conv("speed", 27.7777777778, "ms", "kmh"), 1e-6)
        assertEquals(1024.0, conv("data", 1.0, "MiB", "KiB"), 1e-9)
        assertEquals(1.0, conv("energy", 3600.0, "kj", "kwh"), 1e-12)
        assertEquals(1.01325, conv("pressure", 1.0, "atm", "bar"), 1e-12)
        assertEquals(Math.PI, conv("angle", 180.0, "deg", "rad"), 1e-12)
        assertEquals(40.0, conv("kitchen", 2.0, "subardagi", "ck").let { it / 2 }, 1e-9)
    }

    @Test fun roundTripEveryUnit() {
        Units.categories.forEach { c -> c.units.forEach { u -> c.units.forEach { v ->
            val x = 123.456
            assertEquals("${c.id} ${u.id}->${v.id}", x, c.convert(c.convert(x, u, v), v, u), 1e-6 * x)
        } } }
    }
}
