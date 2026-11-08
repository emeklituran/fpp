"""Geliştirilen uygulamaların ayrıntıları. Tek gerçek kaynak: scaffold, README, PROJECT_STATUS ve katalog buradan üretilir."""

# Simge glifleri: 108x108 tuvalde beyaz çizgiler (güvenli alan ~ 30..78).
S = "stroke"
GLYPHS = {
    "calculator": [
        (S, "M38,28 h32 a6,6 0 0 1 6,6 v40 a6,6 0 0 1 -6,6 h-32 a6,6 0 0 1 -6,-6 v-40 a6,6 0 0 1 6,-6 z", 4),
        (S, "M40,38 h28", 4), (S, "M44,52 v10 M39,57 h10", 4), (S, "M59,57 h10", 4), (S, "M59,69 h10 M59,74 h10", 3),
    ],
    "scientific": [
        (S, "M26,54 h56", 3), (S, "M54,28 v52", 3),
        (S, "M28,54 C34,32 42,32 47,54 S60,76 66,54 S76,36 80,40", 5),
    ],
    "notes": [
        (S, "M36,26 h26 l14,14 v40 a4,4 0 0 1 -4,4 h-36 a4,4 0 0 1 -4,-4 v-50 a4,4 0 0 1 4,-4 z", 4),
        (S, "M62,26 v14 h14", 4), (S, "M42,52 h24 M42,62 h24 M42,72 h16", 4),
    ],
    "diary": [
        (S, "M34,28 h36 a4,4 0 0 1 4,4 v48 a4,4 0 0 1 -4,4 h-36 z", 4), (S, "M34,28 v56", 6),
        (S, "M58,28 v18 l5,-4 l5,4 v-18", 3), (S, "M44,58 h20 M44,66 h14", 3),
    ],
    "todo": [
        (S, "M30,34 h12 v12 h-12 z", 4), (S, "M32,40 l3,3 l6,-7", 3), (S, "M50,40 h28", 4),
        (S, "M30,60 h12 v12 h-12 z", 4), (S, "M50,66 h28", 4),
    ],
    "shopping": [
        (S, "M26,32 h9 l7,30 h30 l6,-22 h-40", 4),
        ("fill", "M47,72 m-4,0 a4,4 0 1 1 8,0 a4,4 0 1 1 -8,0", 0),
        ("fill", "M69,72 m-4,0 a4,4 0 1 1 8,0 a4,4 0 1 1 -8,0", 0),
    ],
    "reminder": [
        (S, "M54,28 c-11,0 -18,8 -18,20 v12 l-6,8 h48 l-6,-8 v-12 c0,-12 -7,-20 -18,-20 z", 4),
        (S, "M48,74 a6,6 0 0 0 12,0", 4), (S, "M54,22 v6", 4),
    ],
    "calendar": [
        (S, "M32,32 h44 a4,4 0 0 1 4,4 v40 a4,4 0 0 1 -4,4 h-44 a4,4 0 0 1 -4,-4 v-40 a4,4 0 0 1 4,-4 z", 4),
        (S, "M28,44 h52", 4), (S, "M42,26 v10 M66,26 v10", 4),
        ("fill", "M40,56 h7 v7 h-7 z M51,56 h7 v7 h-7 z M62,56 h7 v7 h-7 z M40,67 h7 v7 h-7 z M51,67 h7 v7 h-7 z", 0),
    ],
    "alarm": [
        (S, "M54,58 m-20,0 a20,20 0 1 1 40,0 a20,20 0 1 1 -40,0", 4),
        (S, "M54,46 v12 l8,6", 4), (S, "M30,36 l8,-8 M78,36 l-8,-8", 4), (S, "M40,78 l-4,5 M68,78 l4,5", 4),
    ],
    "stopwatch": [
        (S, "M54,60 m-20,0 a20,20 0 1 1 40,0 a20,20 0 1 1 -40,0", 4),
        (S, "M54,60 l9,-11", 4), (S, "M48,28 h12 M54,28 v12", 4), (S, "M72,38 l4,-4", 4),
    ],
}

NO_PERMISSIONS = []

APPS = {
    1: dict(
        module="smartcalculator", dir="01_Daily_Life/01_SmartCalculator", package="com.galaxyoffline.calculator",
        name="Akıllı Hesap Makinesi", short="Hesap", accent=("#1565C0", "#9CC9FF"), glyph="calculator",
        version=("1.0.0", 1), room=True, permissions=NO_PERMISSIONS,
        summary="Canlı sonuç önizlemeli, akıllı yüzde ve bellek tuşlu günlük hesap makinesi.",
        features=[
            "Yazarken canlı sonuç önizlemesi; işlem önceliği ve parantez desteği",
            "Akıllı yüzde: 200 + %10 = 220, 200 − %10 = 180, 200 × %10 = 20",
            "Tek tuşla akıllı parantez, ± işaret değiştirme, geri silme",
            "Bellek tuşları (MC, MR, M+, M−); bellek kalıcıdır",
            "Hesap geçmişi (Room): dokunarak yeniden kullanma, tek tek veya tümden silme",
            "Sonucu uzun basarak kopyalama; Türkçe biçim (1.234,56)",
            "Ayarlar: titreşimli geri bildirim, binlik ayırıcı, tema (açık/koyu/AMOLED)",
        ],
        limitations=["Hesaplamalar 12 anlamlı basamakla gösterilir (çift duyarlıklı kayan nokta)."],
    ),
    2: dict(
        module="scientificcalculator", dir="01_Daily_Life/02_ScientificCalculator", package="com.galaxyoffline.scientificcalculator",
        name="Bilimsel Hesap Makinesi", short="Bilimsel", accent=("#3949AB", "#BAC3FF"), glyph="scientific",
        version=("1.0.0", 1), room=True, permissions=NO_PERMISSIONS,
        summary="Trigonometri, logaritma, üs, kök, faktöriyel ve sabitlerle bilimsel hesap makinesi.",
        features=[
            "sin, cos, tan ve tersleri (2nd), hiperbolik fonksiyonlar",
            "DEG / RAD açı modu (kalıcı)",
            "ln, log, log₂, eˣ, 10ˣ, x², xʸ, √, ∛, 1/x, |x|, x!",
            "π ve e sabitleri, Ans (son sonuç), bilimsel gösterim (E)",
            "Eksik parantezleri otomatik tamamlama, canlı önizleme",
            "Hesap geçmişi (Room) ve sonuç kopyalama",
        ],
        limitations=["Karmaşık sayılar, matris ve grafik çizimi desteklenmez."],
    ),
    3: dict(
        module="notes", dir="01_Daily_Life/03_Notes", package="com.galaxyoffline.notes",
        name="Not Defteri", short="Notlar", accent=("#A86200", "#FFC46B"), glyph="notes",
        version=("1.0.0", 1), room=True, permissions=NO_PERMISSIONS,
        summary="Sabitleme, renk, arşiv, çöp kutusu ve aramalı gelişmiş not defteri.",
        features=[
            "Otomatik kaydetme; başlık ve içerik, kelime/karakter sayısı",
            "Sabitleme, 8 not rengi, arşiv ve 30 günlük çöp kutusu (geri yükleme)",
            "Tam metin arama, sıralama (düzenleme, oluşturma, başlık), liste/ızgara görünümü",
            "Silmede geri al; notu paylaşma (Android paylaşım menüsü) ve .txt olarak dışa aktarma",
        ],
        limitations=["Notlar şifrelenmez; hassas bilgiler için Güvenlik kategorisindeki gizli not kasası planlanmıştır."],
    ),
    4: dict(
        module="diary", dir="01_Daily_Life/04_Diary", package="com.galaxyoffline.diary",
        name="Kişisel Günlük", short="Günlük", accent=("#AD1457", "#FFB1C8"), glyph="diary",
        version=("1.0.0", 1), room=True, permissions=NO_PERMISSIONS,
        summary="Ruh hali kayıtlı, aranabilir ve istatistikli kişisel günlük.",
        features=[
            "Tarihli günlük kayıtları, 5 seviyeli ruh hali",
            "Aya göre gruplanmış liste, arama",
            "İstatistikler: toplam kayıt, bu ay, ardışık yazma serisi, ruh hali dağılımı",
            "Tüm günlüğü .txt olarak dışa aktarma (Depolama Erişim Çerçevesi, izin gerektirmez)",
        ],
        limitations=["Uygulama kilidi (biyometrik) bu sürümde yoktur; kayıtlar şifrelenmez."],
    ),
    5: dict(
        module="todo", dir="01_Daily_Life/05_Todo", package="com.galaxyoffline.todo",
        name="Yapılacaklar", short="Görevler", accent=("#2E7D32", "#94D88E"), glyph="todo",
        version=("1.0.0", 1), room=True, permissions=NO_PERMISSIONS,
        summary="Listeler, son tarih, öncelik ve akıllı filtrelerle yapılacaklar uygulaması.",
        features=[
            "Birden çok liste (ekle, yeniden adlandır, sil)",
            "Görev: başlık, not, son tarih, öncelik (düşük/normal/yüksek)",
            "Akıllı filtreler: Bugün, Yaklaşan, Tümü, Tamamlanan; geciken görev vurgusu",
            "Sıralama (tarih, öncelik, oluşturma), tamamlananları temizleme, geri al",
        ],
        limitations=["Görev bildirimleri yoktur (izin gerektirmemek için); zamanlı uyarı için Hatırlatıcı uygulamasını kullanın."],
    ),
    6: dict(
        module="shoppinglist", dir="01_Daily_Life/06_ShoppingList", package="com.galaxyoffline.shoppinglist",
        name="Alışveriş Listesi", short="Alışveriş", accent=("#D84315", "#FFB59E"), glyph="shopping",
        version=("1.0.0", 1), room=True, permissions=NO_PERMISSIONS,
        summary="Kategorili, fiyat tahminli ve öneri destekli alışveriş listeleri.",
        features=[
            "Birden çok liste, ilerleme göstergesi",
            "Ürün: miktar, birim, kategori, tahmini birim fiyat",
            "Kategoriye göre gruplama, alınanlar en altta",
            "Daha önce eklenen ürünlerden otomatik öneri",
            "Tahmini toplam tutar, alınanları temizleme, listeyi metin olarak paylaşma",
        ],
        limitations=["Fiyatlar kullanıcı tarafından girilen tahmini değerlerdir; mağaza fiyatı çekilmez."],
    ),
    7: dict(
        module="reminder", dir="01_Daily_Life/07_Reminder", package="com.galaxyoffline.reminder",
        name="Hatırlatıcı", short="Hatırlatıcı", accent=("#00838F", "#7FD6E0"), glyph="reminder",
        version=("1.0.0", 1), room=True,
        permissions=[
            ("POST_NOTIFICATIONS", "Hatırlatma zamanı geldiğinde bildirim göstermek için (Android 13+, sizden istenir)."),
            ("SCHEDULE_EXACT_ALARM", "Hatırlatmayı tam dakikasında tetiklemek için. Verilmezse Android birkaç dakika gecikebilir."),
            ("RECEIVE_BOOT_COMPLETED", "Telefon yeniden başlatıldığında hatırlatmaları yeniden kurmak için."),
        ],
        summary="Tekrarlı, ertelenebilir ve yeniden başlatmaya dayanıklı yerel hatırlatıcı.",
        features=[
            "Tarih ve saatli hatırlatmalar; tekrar: yok, her gün, her hafta, her ay, her yıl",
            "Bildirimde Tamamlandı ve 10 dk ertele eylemleri",
            "Yeniden başlatma, saat ve saat dilimi değişiminde otomatik yeniden kurma",
            "Bildirim ve tam zamanlı alarm izni için yönlendirmeli uyarılar",
        ],
        limitations=[
            "Samsung pil optimizasyonu (Uyku modundaki uygulamalar) bildirimleri geciktirebilir; uygulamayı listeden çıkarın.",
            "Tam zamanlı alarm izni verilmezse Android hatırlatmayı birkaç dakika geciktirebilir.",
        ],
    ),
    8: dict(
        module="calendar", dir="01_Daily_Life/08_Calendar", package="com.galaxyoffline.calendar",
        name="Takvim", short="Takvim", accent=("#C62828", "#FFB4AB"), glyph="calendar",
        version=("1.0.0", 1), room=True, permissions=NO_PERMISSIONS,
        summary="Ay görünümü, ajanda ve tekrarlı etkinliklerle yerel takvim.",
        features=[
            "Pazartesi ile başlayan ay görünümü, bugün vurgusu, etkinlik noktaları",
            "Seçili günün etkinlikleri ve 60 günlük ajanda",
            "Etkinlik: başlık, not, tüm gün veya saat aralığı, renk, tekrar (haftalık/aylık/yıllık)",
            "Türkiye'nin sabit tarihli resmi tatilleri",
        ],
        limitations=[
            "Sistem takvimi (Google/Samsung hesapları) okunmaz; etkinlikler yalnızca bu uygulamada tutulur.",
            "Dini bayramlar her yıl değiştiği ve doğrulanmış çevrimdışı veri olmadığı için gösterilmez.",
            "Etkinlik bildirimi yoktur; zamanlı uyarı için Hatırlatıcı uygulamasını kullanın.",
        ],
    ),
    9: dict(
        module="alarm", dir="01_Daily_Life/09_Alarm", package="com.galaxyoffline.alarm",
        name="Alarm", short="Alarm", accent=("#6A1B9A", "#E5B5FF"), glyph="alarm",
        version=("1.0.0", 1), room=True,
        permissions=[
            ("USE_EXACT_ALARM", "Alarmı tam dakikasında çaldırmak için (Android 13+, alarm uygulamalarına otomatik verilir)."),
            ("SCHEDULE_EXACT_ALARM", "Android 12'de tam zamanlı alarm için (yalnızca API 31-32)."),
            ("POST_NOTIFICATIONS", "Çalan alarmı ve yaklaşan alarmı bildirimde göstermek için."),
            ("USE_FULL_SCREEN_INTENT", "Kilit ekranında tam ekran alarm göstermek için."),
            ("FOREGROUND_SERVICE", "Alarm sesi çalarken uygulamanın sistem tarafından kapatılmaması için."),
            ("FOREGROUND_SERVICE_MEDIA_PLAYBACK", "Ön plan hizmetinin alarm sesi çalma türü (Android 14+)."),
            ("VIBRATE", "Alarm titreşimi için."),
            ("WAKE_LOCK", "Alarm çalarken işlemcinin uykuya geçmemesi için."),
            ("RECEIVE_BOOT_COMPLETED", "Yeniden başlatmadan sonra alarmları yeniden kurmak için."),
        ],
        summary="Haftanın günlerine göre tekrarlı, ertelemeli, tam ekran çalan alarm saati.",
        features=[
            "Saat seçici, etiket, haftanın günleri, titreşim ve erteleme süresi",
            "Sistem alarm saati API'si (setAlarmClock): durum çubuğunda sonraki alarm görünür",
            "Kilit ekranında tam ekran çalma, Ertele / Kapat; 10 dakika sonra otomatik susma",
            "Sonraki alarma kalan süre; yeniden başlatma ve saat değişiminde yeniden kurma",
            "Önizleme: alarm ekranını ve sesini anında deneme",
        ],
        limitations=[
            "Alarm sesi olarak cihazın varsayılan alarm tonu kullanılır (özel ses seçimi sonraki sürümde).",
            "Android 14+ cihazlarda tam ekran gösterim izni kapatılmışsa alarm bildirim olarak çalar.",
        ],
    ),
    10: dict(
        module="stopwatch", dir="01_Daily_Life/10_Stopwatch", package="com.galaxyoffline.stopwatch",
        name="Kronometre", short="Kronometre", accent=("#455A64", "#B8CAD4"), glyph="stopwatch",
        version=("1.0.0", 1), room=True, permissions=NO_PERMISSIONS,
        summary="Turlu, uygulama kapansa bile süreyi koruyan, oturum geçmişli kronometre.",
        features=[
            "Salise hassasiyeti; başlat, duraklat, tur, sıfırla",
            "Tur listesi: en hızlı ve en yavaş tur vurgusu",
            "Uygulama kapatılsa veya telefon yeniden başlatılsa bile süre korunur",
            "Oturumları kaydetme ve geçmiş (Room)",
            "Ekranı açık tutma ve titreşimli geri bildirim ayarları",
        ],
        limitations=["Çalışan kronometre için kalıcı bildirim yoktur (bildirim izni istememek için)."],
    ),
}
