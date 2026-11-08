package com.galaxyoffline.unitconverter.data

/** Bir birim: temel birime çevirme (toBase) ve temelden çevirme (fromBase). Doğrusal birimlerde yalnızca katsayı. */
data class UnitDef(val id: String, val label: String, val symbol: String, val toBase: (Double) -> Double, val fromBase: (Double) -> Double) {
    companion object {
        fun linear(id: String, label: String, symbol: String, factor: Double) = UnitDef(id, label, symbol, { it * factor }, { it / factor })
    }
}

data class UnitCategory(val id: String, val label: String, val emoji: String, val units: List<UnitDef>, val note: String? = null) {
    fun unit(id: String?) = units.firstOrNull { it.id == id } ?: units.first()
    fun convert(value: Double, from: UnitDef, to: UnitDef): Double = to.fromBase(from.toBase(value))
}

private fun L(id: String, label: String, sym: String, f: Double) = UnitDef.linear(id, label, sym, f)

object Units {
    val categories: List<UnitCategory> = listOf(
        UnitCategory("length", "Uzunluk", "📏", listOf(
            L("km", "Kilometre", "km", 1000.0), L("m", "Metre", "m", 1.0), L("cm", "Santimetre", "cm", 0.01), L("mm", "Milimetre", "mm", 0.001),
            L("um", "Mikrometre", "µm", 1e-6), L("nm", "Nanometre", "nm", 1e-9), L("mi", "Mil", "mi", 1609.344), L("yd", "Yarda", "yd", 0.9144),
            L("ft", "Fit", "ft", 0.3048), L("in", "İnç", "in", 0.0254), L("nmi", "Deniz mili", "nmi", 1852.0))),
        UnitCategory("mass", "Ağırlık", "⚖️", listOf(
            L("t", "Ton", "t", 1000.0), L("kg", "Kilogram", "kg", 1.0), L("g", "Gram", "g", 0.001), L("mg", "Miligram", "mg", 1e-6),
            L("ug", "Mikrogram", "µg", 1e-9), L("lb", "Libre", "lb", 0.45359237), L("oz", "Ons", "oz", 0.028349523125),
            L("ct", "Karat", "ct", 0.0002), L("st", "Stone", "st", 6.35029318))),
        UnitCategory("temp", "Sıcaklık", "🌡️", listOf(
            UnitDef("c", "Santigrat", "°C", { it }, { it }),
            UnitDef("f", "Fahrenhayt", "°F", { (it - 32) * 5 / 9 }, { it * 9 / 5 + 32 }),
            UnitDef("k", "Kelvin", "K", { it - 273.15 }, { it + 273.15 }))),
        UnitCategory("volume", "Hacim", "🧪", listOf(
            L("m3", "Metreküp", "m³", 1000.0), L("l", "Litre", "L", 1.0), L("dl", "Desilitre", "dL", 0.1), L("cl", "Santilitre", "cL", 0.01),
            L("ml", "Mililitre", "mL", 0.001), L("cm3", "Santimetreküp", "cm³", 0.001), L("galus", "Galon (ABD)", "gal", 3.785411784),
            L("galuk", "Galon (İngiliz)", "gal", 4.54609), L("qt", "Kuart (ABD)", "qt", 0.946352946), L("pt", "Pint (ABD)", "pt", 0.473176473),
            L("floz", "Sıvı ons (ABD)", "fl oz", 0.0295735295625), L("ft3", "Fitküp", "ft³", 28.316846592), L("in3", "İnçküp", "in³", 0.016387064))),
        UnitCategory("area", "Alan", "🗺️", listOf(
            L("km2", "Kilometrekare", "km²", 1e6), L("ha", "Hektar", "ha", 10_000.0), L("donum", "Dönüm", "dönüm", 1000.0), L("ar", "Ar", "a", 100.0),
            L("m2", "Metrekare", "m²", 1.0), L("cm2", "Santimetrekare", "cm²", 1e-4), L("mm2", "Milimetrekare", "mm²", 1e-6),
            L("mi2", "Milkare", "mi²", 2_589_988.110336), L("ac", "Akre", "ac", 4046.8564224), L("ft2", "Fitkare", "ft²", 0.09290304),
            L("in2", "İnçkare", "in²", 0.00064516)), note = "Dönüm: Türkiye'de kullanılan 1.000 m²'lik dönüm."),
        UnitCategory("speed", "Hız", "🚀", listOf(
            L("ms", "Metre/saniye", "m/s", 1.0), L("kmh", "Kilometre/saat", "km/sa", 1 / 3.6), L("mph", "Mil/saat", "mph", 0.44704),
            L("kn", "Knot (deniz mili/saat)", "kn", 1852.0 / 3600), L("fts", "Fit/saniye", "ft/s", 0.3048), L("mach", "Mach (20 °C hava)", "Ma", 343.0))),
        UnitCategory("data", "Veri boyutu", "💾", listOf(
            L("bit", "Bit", "bit", 0.125), L("B", "Bayt", "B", 1.0), L("KB", "Kilobayt", "KB", 1e3), L("MB", "Megabayt", "MB", 1e6),
            L("GB", "Gigabayt", "GB", 1e9), L("TB", "Terabayt", "TB", 1e12), L("KiB", "Kibibayt", "KiB", 1024.0), L("MiB", "Mebibayt", "MiB", 1024.0 * 1024),
            L("GiB", "Gibibayt", "GiB", 1024.0 * 1024 * 1024), L("TiB", "Tebibayt", "TiB", 1024.0 * 1024 * 1024 * 1024)),
            note = "KB = 1.000 bayt (SI); KiB = 1.024 bayt. Telefonlar depolamayı genellikle 1.000 tabanıyla gösterir."),
        UnitCategory("time", "Süre", "⏱️", listOf(
            L("ns", "Nanosaniye", "ns", 1e-9), L("us", "Mikrosaniye", "µs", 1e-6), L("msec", "Milisaniye", "ms", 1e-3), L("s", "Saniye", "sn", 1.0),
            L("min", "Dakika", "dk", 60.0), L("h", "Saat", "sa", 3600.0), L("d", "Gün", "gün", 86_400.0), L("w", "Hafta", "hafta", 604_800.0),
            L("mo", "Ay (ortalama)", "ay", 2_629_746.0), L("y", "Yıl (ortalama)", "yıl", 31_556_952.0))),
        UnitCategory("energy", "Enerji", "⚡", listOf(
            L("j", "Joule", "J", 1.0), L("kj", "Kilojoule", "kJ", 1000.0), L("cal", "Kalori", "cal", 4.184), L("kcal", "Kilokalori", "kcal", 4184.0),
            L("wh", "Watt-saat", "Wh", 3600.0), L("kwh", "Kilowatt-saat", "kWh", 3.6e6), L("ev", "Elektronvolt", "eV", 1.602176634e-19),
            L("btu", "BTU", "BTU", 1055.05585262))),
        UnitCategory("pressure", "Basınç", "🎈", listOf(
            L("pa", "Pascal", "Pa", 1.0), L("kpa", "Kilopascal", "kPa", 1000.0), L("bar", "Bar", "bar", 1e5), L("atm", "Atmosfer", "atm", 101_325.0),
            L("mmhg", "Milimetre cıva", "mmHg", 133.322387415), L("psi", "Psi", "psi", 6894.757293168))),
        UnitCategory("power", "Güç", "🔌", listOf(
            L("w", "Watt", "W", 1.0), L("kw", "Kilowatt", "kW", 1000.0), L("mw", "Megawatt", "MW", 1e6),
            L("hp", "Beygir gücü (metrik)", "BG", 735.49875), L("hpi", "Beygir gücü (mekanik, hp)", "hp", 745.69987158227022))),
        UnitCategory("angle", "Açı", "📐", listOf(
            L("deg", "Derece", "°", 1.0), L("rad", "Radyan", "rad", 180 / Math.PI), L("grad", "Grad", "gon", 0.9), L("turn", "Tam tur", "tur", 360.0))),
        UnitCategory("kitchen", "Mutfak ölçüleri", "🥄", listOf(
            L("subardagi", "Su bardağı", "su b.", 200.0), L("caybardagi", "Çay bardağı", "çay b.", 100.0), L("fincan", "Kahve fincanı", "fincan", 70.0),
            L("yk", "Yemek kaşığı", "y.k.", 15.0), L("tk", "Tatlı kaşığı", "t.k.", 10.0), L("ck", "Çay kaşığı", "ç.k.", 5.0),
            L("cup", "Cup (ABD)", "cup", 236.5882365), L("ml_k", "Mililitre", "mL", 1.0), L("l_k", "Litre", "L", 1000.0)),
            note = "Mutfak ölçüleri hacimdir ve yaklaşık değerlerdir (su bardağı 200 mL). Un, şeker gibi malzemelerin ağırlığı yoğunluğa göre değişir."),
    )

    fun category(id: String?) = categories.firstOrNull { it.id == id } ?: categories.first()
}
