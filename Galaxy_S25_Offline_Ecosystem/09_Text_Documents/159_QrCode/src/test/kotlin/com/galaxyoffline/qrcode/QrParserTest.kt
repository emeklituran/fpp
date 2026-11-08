package com.galaxyoffline.qrcode

import com.galaxyoffline.qrcode.data.QrContent
import com.galaxyoffline.qrcode.data.QrParser
import com.galaxyoffline.qrcode.data.QrPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QrParserTest {
    @Test fun wifiWithEscapes() {
        val w = QrParser.parse("WIFI:T:WPA;S:Ev\\;Ağı;P:şi\\:fre;H:true;;") as QrContent.Wifi
        assertEquals("Ev;Ağı", w.ssid); assertEquals("şi:fre", w.password); assertEquals("WPA", w.security); assertTrue(w.hidden)
        val round = QrParser.parse(QrPayload.wifi("Ofis, 2", "p;a:s", "WPA", false)) as QrContent.Wifi
        assertEquals("Ofis, 2", round.ssid); assertEquals("p;a:s", round.password)
    }

    @Test fun typesDetected() {
        assertTrue(QrParser.parse("https://ornek.com") is QrContent.Url)
        assertTrue(!(QrParser.parse("http://ornek.com") as QrContent.Url).secure)
        assertEquals("05551234567", (QrParser.parse("tel:05551234567") as QrContent.Phone).number)
        val m = QrParser.parse("mailto:a@b.com?subject=Merhaba%20d%C3%BCnya") as QrContent.Email
        assertEquals("Merhaba dünya", m.subject)
        val g = QrParser.parse("geo:41.0082,28.9784") as QrContent.Geo
        assertEquals(41.0082, g.lat, 1e-9)
        assertTrue(QrParser.parse("8690000000000", "EAN barkod") is QrContent.Product)
        assertTrue(QrParser.parse("merhaba") is QrContent.Text)
    }

    @Test fun contacts() {
        val v = QrParser.parse(QrPayload.contact("Ayşe Yılmaz", "+905551112233", "ayse@ornek.com", "ACME")) as QrContent.Contact
        assertEquals("Ayşe Yılmaz", v.name); assertEquals(listOf("+905551112233"), v.phones)
        val me = QrParser.parse("MECARD:N:Yılmaz,Ayşe;TEL:123;EMAIL:a@b.c;;") as QrContent.Contact
        assertEquals("Ayşe Yılmaz", me.name)
    }
}
