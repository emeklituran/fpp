"""Galaxy S25 Offline Ecosystem – tam uygulama kataloğu (master prompt sırasıyla)."""

CATEGORIES = {
    1: ("01_Daily_Life", "Günlük Yaşam"),
    2: ("02_Calculators", "Akıllı Hesaplama Araçları"),
    3: ("03_Home_Life", "Ev ve Yaşam Yönetimi"),
    4: ("04_Finance", "Finans ve Para Yönetimi"),
    5: ("05_Security", "Güvenlik ve Gizlilik"),
    6: ("06_Media", "Medya ve Eğlence"),
    7: ("07_Health_Fitness", "Sağlık ve Spor"),
    8: ("08_Device_Tools", "Telefon ve Donanım Araçları"),
    9: ("09_Text_Documents", "Metin ve Belge Araçları"),
    10: ("10_Education", "Eğitim ve Kişisel Gelişim"),
    11: ("11_Travel", "Seyahat ve Dış Mekân"),
    12: ("12_Games", "Oyun ve Eğlence Araçları"),
}

_NAMES = {
    1: ["Akıllı hesap makinesi", "Bilimsel hesap makinesi", "Gelişmiş not defteri", "Kişisel günlük",
        "Yapılacaklar listesi", "Alışveriş listesi", "Hatırlatıcı", "Takvim", "Alarm", "Kronometre",
        "Geri sayım", "Dünya saati", "Tarih hesaplayıcı", "Yaş hesaplayıcı", "Gün sayacı",
        "Özel gün takipçisi", "Doğum günü takipçisi", "Hedef takipçisi", "Alışkanlık takipçisi", "Kişisel ajanda"],
    2: ["Yüzde hesaplayıcı", "KDV hesaplayıcı", "İndirim hesaplayıcı", "Bahşiş hesaplayıcı", "Taksit hesaplayıcı",
        "Kredi hesaplayıcı", "Faiz hesaplayıcı", "Bileşik faiz hesaplayıcı", "Maaş hesaplayıcı", "Mesai hesaplayıcı",
        "İzin günü hesaplayıcı", "Yakıt maliyeti hesaplayıcı", "Elektrik faturası tahmin hesaplayıcı",
        "Su tüketim hesaplayıcı", "Ortalama hesaplayıcı", "Not ortalaması hesaplayıcı", "Birim dönüştürücü",
        "Para birimi dönüştürücü (manuel kur)", "Uzunluk dönüştürücü", "Ağırlık dönüştürücü", "Sıcaklık dönüştürücü",
        "Hacim dönüştürücü", "Alan dönüştürücü", "Hız dönüştürücü", "Veri boyutu dönüştürücü"],
    3: ["Ev bütçesi takipçisi", "Ev işleri planlayıcısı", "Temizlik takvimi", "Market alışverişi yöneticisi",
        "Ev envanter takipçisi", "Garanti süresi takipçisi", "Fatura takipçisi", "Abonelik ve ödeme hatırlatıcısı",
        "Yemek planlayıcısı", "Tarif defteri", "Mutfak ölçü dönüştürücü", "Porsiyon hesaplayıcı",
        "Son kullanma tarihi takipçisi", "Buzdolabı envanteri", "Ev bakım takvimi"],
    4: ["Gelir gider takipçisi", "Kişisel finans yöneticisi", "Aylık bütçe", "Borç alacak takipçisi",
        "Harcama analiz aracı", "Tasarruf hedefi", "Birikim takipçisi", "Nakit kasa defteri", "Harcama kategorileri",
        "Finansal grafikler", "Aile bütçesi", "Alışveriş fiyat karşılaştırma defteri", "Kredi geri ödeme hesaplayıcı",
        "Yatırım getiri hesaplayıcı", "Enflasyon hesaplayıcı (manuel veri)", "Net varlık hesaplayıcı",
        "Vergi hesaplama yardımcı aracı", "Altın değer hesaplayıcı (manuel fiyat)", "Döviz hesaplayıcı (manuel fiyat)",
        "Finansal hedef planlayıcı"],
    5: ["Şifre kasası", "Güçlü şifre üretici", "Gizli not kasası", "Güvenli fotoğraf kasası", "Güvenli dosya kasası",
        "Dosya şifreleme", "Dosya şifre çözme", "TOTP doğrulama uygulaması", "Yerel güvenlik kontrol aracı",
        "Şifre gücü analiz aracı", "Güvenli kimlik bilgisi saklama", "Yerel yedekleme aracı", "Şifreli yedekleme aracı",
        "Dosya bütünlük kontrol aracı", "SHA-256 hesaplayıcı"],
    6: ["Müzik çalar", "Video oynatıcı", "Fotoğraf galerisi", "Fotoğraf görüntüleyici", "Fotoğraf sıkıştırıcı",
        "Fotoğraf boyutlandırıcı", "Fotoğraf kırpma aracı", "Ses kayıt cihazı", "Ses oynatıcı", "Ses kesme aracı",
        "Video kesme aracı", "PDF okuyucu", "PDF birleştirme", "PDF sayfa ayırma", "E-kitap okuyucu", "EPUB okuyucu",
        "Yerel dosya görüntüleyici", "ZIP arşiv yöneticisi", "Dosya sıkıştırıcı", "Dosya karşılaştırıcı"],
    7: ["Adım sayar", "Kalori hesaplayıcı", "Vücut kitle indeksi hesaplayıcı", "Su içme takipçisi",
        "Antrenman takipçisi", "Egzersiz planlayıcısı", "Yürüyüş takipçisi", "Koşu takipçisi", "Uyku günlüğü",
        "Nefes egzersizi", "Meditasyon zamanlayıcısı", "Kişisel sağlık günlüğü", "Kilo takipçisi",
        "Günlük aktivite takipçisi", "Esneme egzersizi hatırlatıcısı"],
    8: ["Pusula", "Su terazisi", "Desibel ölçer", "Metronom", "Sensör görüntüleyici", "Pil bilgi ekranı",
        "Depolama analiz aracı", "RAM bilgi ekranı", "Ekran piksel testi", "Ekran renk testi",
        "Dokunmatik ekran test aracı", "Titreşim test aracı", "Telefon donanım bilgi aracı",
        "Ekran parlaklığı yardımcı aracı", "Kamera test aracı", "Mikrofon test aracı", "Hoparlör test aracı",
        "Ekran yenileme hızı bilgi aracı", "Telefon sıcaklık bilgisi", "Cihaz tanılama paneli"],
    9: ["Metin düzenleyici", "Markdown editörü", "Kelime sayacı", "Karakter sayacı", "Metin karşılaştırıcı",
        "Metin biçimlendirici", "OCR metin tanıma", "Fotoğraftan yazı çıkarma", "QR kod üretici", "QR kod okuyucu",
        "Barkod okuyucu", "JSON görüntüleyici", "JSON düzenleyici", "XML görüntüleyici", "Kod editörü",
        "Renk kodu dönüştürücü", "HEX RGB dönüştürücü", "Metin şifreleme aracı", "Dosya hash hesaplayıcı",
        "Yerel belge tarayıcı"],
    10: ["Kelime ezberleme", "Flashcard uygulaması", "Ders çalışma zamanlayıcısı", "Pomodoro zamanlayıcısı",
         "Sınav geri sayımı", "Öğrenci not takipçisi", "Ders programı", "Kişisel okuma günlüğü", "Kitap takipçisi",
         "Hedef ve gelişim takipçisi", "Çevrimdışı sözlük", "Kelime kartları", "Matematik alıştırmaları",
         "Zihin egzersizleri", "Hafıza oyunları"],
    11: ["Seyahat planlayıcısı", "Bavul hazırlama listesi", "Yolculuk masraf hesaplayıcı", "Mesafe hesaplayıcı",
         "GPS koordinat görüntüleyici", "Pusula (seyahat)", "Rakım ölçer", "Hız göstergesi", "Seyahat günlüğü",
         "Çevrimdışı gezi notları", "Park yeri hatırlatıcısı", "Araç kilometre takipçisi", "Araç bakım takipçisi",
         "Yakıt tüketim takipçisi", "Araç masraf defteri"],
    12: ["Sudoku", "Kelime bulmaca", "Hafıza oyunu", "Mantık bulmacaları", "Zar uygulaması",
         "Rastgele sayı üretici", "Şans çarkı", "Rastgele isim seçici", "Taş kağıt makas", "Dikkat geliştirme oyunu",
         "Matematik oyunu", "Kelime oyunu", "Satranç (çevrimdışı)", "Basit dama", "Günlük beyin egzersizi"],
}

CATALOG = []  # (no, category, name)
_n = 0
for _cat in sorted(_NAMES):
    for _name in _NAMES[_cat]:
        _n += 1
        CATALOG.append((_n, _cat, _name))
assert len(CATALOG) == 215, len(CATALOG)

# Katalogdaki örtüşmeler: aynı uygulama bir kez geliştirilip iki yerde listelenir.
DUPLICATES = {191: 131}

# Başka bir uygulamanın kapsamında geliştirilenler: no -> kapsayan uygulama no
COVERED_BY = {160: 159, 39: 37, 40: 37, 41: 37, 42: 37, 43: 37, 44: 37, 45: 37, 56: 37, 53: 52, 174: 11}
