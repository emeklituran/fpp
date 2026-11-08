# PROJECT_STATUS

Galaxy S25 Offline Ecosystem – uygulama bazında durum takibi. Bu dosya `tools/ecosystem.py docs` ile üretilir;
durumlar `tools/status.json` içinde tutulur ve yalnızca gerçekten doğrulanan adımlar işaretlenir.

Durum sırası: Planlandı → Geliştiriliyor → Kodlandı → Derlendi → Test edildi → APK oluşturuldu → Teslim edildi

**Özet:** 215 katalog uygulaması + App Center · APK'sı oluşturulan: 20

## 1. Günlük Yaşam (`01_Daily_Life`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 1 | Akıllı Hesap Makinesi | `com.galaxyoffline.calculator` | APK oluşturuldu | CI #7: derleme, denetim, emülatör ve cihaz testleri geçti |
| 2 | Bilimsel Hesap Makinesi | `com.galaxyoffline.scientificcalculator` | APK oluşturuldu | CI #7: derleme, denetim, emülatör ve cihaz testleri geçti |
| 3 | Not Defteri | `com.galaxyoffline.notes` | APK oluşturuldu | CI #7: derleme, denetim, emülatör ve cihaz testleri geçti |
| 4 | Kişisel Günlük | `com.galaxyoffline.diary` | APK oluşturuldu | CI #7: derleme, denetim, emülatör ve cihaz testleri geçti |
| 5 | Yapılacaklar | `com.galaxyoffline.todo` | APK oluşturuldu | CI #7: derleme, denetim, emülatör ve cihaz testleri geçti |
| 6 | Alışveriş Listesi | `com.galaxyoffline.shoppinglist` | APK oluşturuldu | CI #7: derleme, denetim, emülatör ve cihaz testleri geçti |
| 7 | Hatırlatıcı | `com.galaxyoffline.reminder` | APK oluşturuldu | CI #7: derleme, denetim, emülatör ve cihaz testleri geçti |
| 8 | Takvim | `com.galaxyoffline.calendar` | APK oluşturuldu | CI #7: derleme, denetim, emülatör ve cihaz testleri geçti |
| 9 | Alarm | `com.galaxyoffline.alarm` | APK oluşturuldu | CI #7: derleme, denetim, emülatör ve cihaz testleri geçti |
| 10 | Kronometre | `com.galaxyoffline.stopwatch` | APK oluşturuldu | CI #7: derleme, denetim, emülatör ve cihaz testleri geçti |
| 11 | Zamanlayıcı | `com.galaxyoffline.timer` | APK oluşturuldu | CI #19: derleme, izin denetimi, emülatörde kurulum/açılış ve cihaz testi geçti |
| 12 | Dünya saati | — | Planlandı |  |
| 13 | Tarih hesaplayıcı | — | Planlandı |  |
| 14 | Yaş hesaplayıcı | — | Planlandı |  |
| 15 | Gün sayacı | — | Planlandı |  |
| 16 | Özel gün takipçisi | — | Planlandı |  |
| 17 | Doğum günü takipçisi | — | Planlandı |  |
| 18 | Hedef takipçisi | — | Planlandı |  |
| 19 | Alışkanlık Takipçisi | `com.galaxyoffline.habittracker` | APK oluşturuldu | CI #19: derleme, izin denetimi, emülatörde kurulum/açılış ve cihaz testi geçti |
| 20 | Kişisel ajanda | — | Planlandı |  |

## 2. Akıllı Hesaplama Araçları (`02_Calculators`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 21 | Yüzde hesaplayıcı | — | Planlandı |  |
| 22 | KDV hesaplayıcı | — | Planlandı |  |
| 23 | İndirim hesaplayıcı | — | Planlandı |  |
| 24 | Bahşiş hesaplayıcı | — | Planlandı |  |
| 25 | Taksit hesaplayıcı | — | Planlandı |  |
| 26 | Kredi hesaplayıcı | — | Planlandı |  |
| 27 | Faiz hesaplayıcı | — | Planlandı |  |
| 28 | Bileşik faiz hesaplayıcı | — | Planlandı |  |
| 29 | Maaş hesaplayıcı | — | Planlandı |  |
| 30 | Mesai hesaplayıcı | — | Planlandı |  |
| 31 | İzin günü hesaplayıcı | — | Planlandı |  |
| 32 | Yakıt maliyeti hesaplayıcı | — | Planlandı |  |
| 33 | Elektrik faturası tahmin hesaplayıcı | — | Planlandı |  |
| 34 | Su tüketim hesaplayıcı | — | Planlandı |  |
| 35 | Ortalama hesaplayıcı | — | Planlandı |  |
| 36 | Not ortalaması hesaplayıcı | — | Planlandı |  |
| 37 | Birim Dönüştürücü | `com.galaxyoffline.unitconverter` | APK oluşturuldu | CI #19: derleme, izin denetimi, emülatörde kurulum/açılış ve cihaz testi geçti |
| 38 | Para birimi dönüştürücü (manuel kur) | — | Planlandı |  |
| 39 | Uzunluk dönüştürücü | — | APK oluşturuldu | No 37 (Birim Dönüştürücü) kapsamında |
| 40 | Ağırlık dönüştürücü | — | APK oluşturuldu | No 37 (Birim Dönüştürücü) kapsamında |
| 41 | Sıcaklık dönüştürücü | — | APK oluşturuldu | No 37 (Birim Dönüştürücü) kapsamında |
| 42 | Hacim dönüştürücü | — | APK oluşturuldu | No 37 (Birim Dönüştürücü) kapsamında |
| 43 | Alan dönüştürücü | — | APK oluşturuldu | No 37 (Birim Dönüştürücü) kapsamında |
| 44 | Hız dönüştürücü | — | APK oluşturuldu | No 37 (Birim Dönüştürücü) kapsamında |
| 45 | Veri boyutu dönüştürücü | — | APK oluşturuldu | No 37 (Birim Dönüştürücü) kapsamında |

## 3. Ev ve Yaşam Yönetimi (`03_Home_Life`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 46 | Ev bütçesi takipçisi | — | Planlandı |  |
| 47 | Ev işleri planlayıcısı | — | Planlandı |  |
| 48 | Temizlik takvimi | — | Planlandı |  |
| 49 | Market alışverişi yöneticisi | — | Planlandı |  |
| 50 | Ev envanter takipçisi | — | Planlandı |  |
| 51 | Garanti süresi takipçisi | — | Planlandı |  |
| 52 | Fatura ve Abonelik | `com.galaxyoffline.billreminder` | APK oluşturuldu | CI #19: derleme, izin denetimi, emülatörde kurulum/açılış ve cihaz testi geçti |
| 53 | Abonelik ve ödeme hatırlatıcısı | — | APK oluşturuldu | No 52 (Fatura ve Abonelik) kapsamında |
| 54 | Yemek planlayıcısı | — | Planlandı |  |
| 55 | Tarif defteri | — | Planlandı |  |
| 56 | Mutfak ölçü dönüştürücü | — | APK oluşturuldu | No 37 (Birim Dönüştürücü) kapsamında |
| 57 | Porsiyon hesaplayıcı | — | Planlandı |  |
| 58 | Son kullanma tarihi takipçisi | — | Planlandı |  |
| 59 | Buzdolabı envanteri | — | Planlandı |  |
| 60 | Ev bakım takvimi | — | Planlandı |  |

## 4. Finans ve Para Yönetimi (`04_Finance`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 61 | Gelir Gider | `com.galaxyoffline.incomeexpense` | APK oluşturuldu | CI #19: derleme, izin denetimi, emülatörde kurulum/açılış ve cihaz testi geçti |
| 62 | Kişisel finans yöneticisi | — | Planlandı |  |
| 63 | Aylık bütçe | — | Planlandı |  |
| 64 | Borç alacak takipçisi | — | Planlandı |  |
| 65 | Harcama analiz aracı | — | Planlandı |  |
| 66 | Tasarruf hedefi | — | Planlandı |  |
| 67 | Birikim takipçisi | — | Planlandı |  |
| 68 | Nakit kasa defteri | — | Planlandı |  |
| 69 | Harcama kategorileri | — | Planlandı |  |
| 70 | Finansal grafikler | — | Planlandı |  |
| 71 | Aile bütçesi | — | Planlandı |  |
| 72 | Alışveriş fiyat karşılaştırma defteri | — | Planlandı |  |
| 73 | Kredi geri ödeme hesaplayıcı | — | Planlandı |  |
| 74 | Yatırım getiri hesaplayıcı | — | Planlandı |  |
| 75 | Enflasyon hesaplayıcı (manuel veri) | — | Planlandı |  |
| 76 | Net varlık hesaplayıcı | — | Planlandı |  |
| 77 | Vergi hesaplama yardımcı aracı | — | Planlandı |  |
| 78 | Altın değer hesaplayıcı (manuel fiyat) | — | Planlandı |  |
| 79 | Döviz hesaplayıcı (manuel fiyat) | — | Planlandı |  |
| 80 | Finansal hedef planlayıcı | — | Planlandı |  |

## 5. Güvenlik ve Gizlilik (`05_Security`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 81 | Şifre Kasası | `com.galaxyoffline.passwordvault` | APK oluşturuldu | CI #19: derleme, izin denetimi, emülatörde kurulum/açılış ve cihaz testi geçti |
| 82 | Güçlü şifre üretici | — | Planlandı |  |
| 83 | Gizli not kasası | — | Planlandı |  |
| 84 | Güvenli fotoğraf kasası | — | Planlandı |  |
| 85 | Güvenli dosya kasası | — | Planlandı |  |
| 86 | Dosya şifreleme | — | Planlandı |  |
| 87 | Dosya şifre çözme | — | Planlandı |  |
| 88 | Doğrulayıcı | `com.galaxyoffline.authenticator` | APK oluşturuldu | CI #19: derleme, izin denetimi, emülatörde kurulum/açılış ve cihaz testi geçti |
| 89 | Yerel güvenlik kontrol aracı | — | Planlandı |  |
| 90 | Şifre gücü analiz aracı | — | Planlandı |  |
| 91 | Güvenli kimlik bilgisi saklama | — | Planlandı |  |
| 92 | Yerel yedekleme aracı | — | Planlandı |  |
| 93 | Şifreli yedekleme aracı | — | Planlandı |  |
| 94 | Dosya bütünlük kontrol aracı | — | Planlandı |  |
| 95 | SHA-256 hesaplayıcı | — | Planlandı |  |

## 6. Medya ve Eğlence (`06_Media`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 96 | Müzik çalar | — | Planlandı |  |
| 97 | Video oynatıcı | — | Planlandı |  |
| 98 | Fotoğraf galerisi | — | Planlandı |  |
| 99 | Fotoğraf görüntüleyici | — | Planlandı |  |
| 100 | Fotoğraf sıkıştırıcı | — | Planlandı |  |
| 101 | Fotoğraf boyutlandırıcı | — | Planlandı |  |
| 102 | Fotoğraf kırpma aracı | — | Planlandı |  |
| 103 | Ses Kaydedici | `com.galaxyoffline.voicerecorder` | APK oluşturuldu | CI #19: derleme, izin denetimi, emülatörde kurulum/açılış ve cihaz testi geçti |
| 104 | Ses oynatıcı | — | Planlandı |  |
| 105 | Ses kesme aracı | — | Planlandı |  |
| 106 | Video kesme aracı | — | Planlandı |  |
| 107 | PDF Okuyucu | `com.galaxyoffline.pdfreader` | APK oluşturuldu | CI #19: derleme, izin denetimi, emülatörde kurulum/açılış ve cihaz testi geçti |
| 108 | PDF birleştirme | — | Planlandı |  |
| 109 | PDF sayfa ayırma | — | Planlandı |  |
| 110 | E-kitap okuyucu | — | Planlandı |  |
| 111 | EPUB okuyucu | — | Planlandı |  |
| 112 | Yerel dosya görüntüleyici | — | Planlandı |  |
| 113 | ZIP arşiv yöneticisi | — | Planlandı |  |
| 114 | Dosya sıkıştırıcı | — | Planlandı |  |
| 115 | Dosya karşılaştırıcı | — | Planlandı |  |

## 7. Sağlık ve Spor (`07_Health_Fitness`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 116 | Adım sayar | — | Planlandı |  |
| 117 | Kalori hesaplayıcı | — | Planlandı |  |
| 118 | Vücut kitle indeksi hesaplayıcı | — | Planlandı |  |
| 119 | Su içme takipçisi | — | Planlandı |  |
| 120 | Antrenman takipçisi | — | Planlandı |  |
| 121 | Egzersiz planlayıcısı | — | Planlandı |  |
| 122 | Yürüyüş takipçisi | — | Planlandı |  |
| 123 | Koşu takipçisi | — | Planlandı |  |
| 124 | Uyku günlüğü | — | Planlandı |  |
| 125 | Nefes egzersizi | — | Planlandı |  |
| 126 | Meditasyon zamanlayıcısı | — | Planlandı |  |
| 127 | Kişisel sağlık günlüğü | — | Planlandı |  |
| 128 | Kilo takipçisi | — | Planlandı |  |
| 129 | Günlük aktivite takipçisi | — | Planlandı |  |
| 130 | Esneme egzersizi hatırlatıcısı | — | Planlandı |  |

## 8. Telefon ve Donanım Araçları (`08_Device_Tools`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 131 | Pusula | — | Planlandı |  |
| 132 | Su terazisi | — | Planlandı |  |
| 133 | Desibel ölçer | — | Planlandı |  |
| 134 | Metronom | — | Planlandı |  |
| 135 | Sensör görüntüleyici | — | Planlandı |  |
| 136 | Pil bilgi ekranı | — | Planlandı |  |
| 137 | Depolama analiz aracı | — | Planlandı |  |
| 138 | RAM bilgi ekranı | — | Planlandı |  |
| 139 | Ekran piksel testi | — | Planlandı |  |
| 140 | Ekran renk testi | — | Planlandı |  |
| 141 | Dokunmatik ekran test aracı | — | Planlandı |  |
| 142 | Titreşim test aracı | — | Planlandı |  |
| 143 | Telefon donanım bilgi aracı | — | Planlandı |  |
| 144 | Ekran parlaklığı yardımcı aracı | — | Planlandı |  |
| 145 | Kamera test aracı | — | Planlandı |  |
| 146 | Mikrofon test aracı | — | Planlandı |  |
| 147 | Hoparlör test aracı | — | Planlandı |  |
| 148 | Ekran yenileme hızı bilgi aracı | — | Planlandı |  |
| 149 | Telefon sıcaklık bilgisi | — | Planlandı |  |
| 150 | Cihaz tanılama paneli | — | Planlandı |  |

## 9. Metin ve Belge Araçları (`09_Text_Documents`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 151 | Metin düzenleyici | — | Planlandı |  |
| 152 | Markdown editörü | — | Planlandı |  |
| 153 | Kelime sayacı | — | Planlandı |  |
| 154 | Karakter sayacı | — | Planlandı |  |
| 155 | Metin karşılaştırıcı | — | Planlandı |  |
| 156 | Metin biçimlendirici | — | Planlandı |  |
| 157 | OCR metin tanıma | — | Planlandı |  |
| 158 | Fotoğraftan yazı çıkarma | — | Planlandı |  |
| 159 | QR Kod | `com.galaxyoffline.qrcode` | APK oluşturuldu | CI #19: derleme, izin denetimi, emülatörde kurulum/açılış ve cihaz testi geçti |
| 160 | QR kod okuyucu | — | APK oluşturuldu | No 159 (QR Kod) kapsamında |
| 161 | Barkod okuyucu | — | Planlandı |  |
| 162 | JSON görüntüleyici | — | Planlandı |  |
| 163 | JSON düzenleyici | — | Planlandı |  |
| 164 | XML görüntüleyici | — | Planlandı |  |
| 165 | Kod editörü | — | Planlandı |  |
| 166 | Renk kodu dönüştürücü | — | Planlandı |  |
| 167 | HEX RGB dönüştürücü | — | Planlandı |  |
| 168 | Metin şifreleme aracı | — | Planlandı |  |
| 169 | Dosya hash hesaplayıcı | — | Planlandı |  |
| 170 | Yerel belge tarayıcı | — | Planlandı |  |

## 10. Eğitim ve Kişisel Gelişim (`10_Education`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 171 | Kelime ezberleme | — | Planlandı |  |
| 172 | Flashcard uygulaması | — | Planlandı |  |
| 173 | Ders çalışma zamanlayıcısı | — | Planlandı |  |
| 174 | Pomodoro zamanlayıcısı | — | APK oluşturuldu | No 11 (Zamanlayıcı) kapsamında |
| 175 | Sınav geri sayımı | — | Planlandı |  |
| 176 | Öğrenci not takipçisi | — | Planlandı |  |
| 177 | Ders programı | — | Planlandı |  |
| 178 | Kişisel okuma günlüğü | — | Planlandı |  |
| 179 | Kitap takipçisi | — | Planlandı |  |
| 180 | Hedef ve gelişim takipçisi | — | Planlandı |  |
| 181 | Çevrimdışı sözlük | — | Planlandı |  |
| 182 | Kelime kartları | — | Planlandı |  |
| 183 | Matematik alıştırmaları | — | Planlandı |  |
| 184 | Zihin egzersizleri | — | Planlandı |  |
| 185 | Hafıza oyunları | — | Planlandı |  |

## 11. Seyahat ve Dış Mekân (`11_Travel`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 186 | Seyahat planlayıcısı | — | Planlandı |  |
| 187 | Bavul hazırlama listesi | — | Planlandı |  |
| 188 | Yolculuk masraf hesaplayıcı | — | Planlandı |  |
| 189 | Mesafe hesaplayıcı | — | Planlandı |  |
| 190 | GPS koordinat görüntüleyici | — | Planlandı |  |
| 191 | Pusula (seyahat) | — | Planlandı | No 131 ile aynı uygulama (tek APK) |
| 192 | Rakım ölçer | — | Planlandı |  |
| 193 | Hız göstergesi | — | Planlandı |  |
| 194 | Seyahat günlüğü | — | Planlandı |  |
| 195 | Çevrimdışı gezi notları | — | Planlandı |  |
| 196 | Park yeri hatırlatıcısı | — | Planlandı |  |
| 197 | Araç kilometre takipçisi | — | Planlandı |  |
| 198 | Araç bakım takipçisi | — | Planlandı |  |
| 199 | Yakıt tüketim takipçisi | — | Planlandı |  |
| 200 | Araç masraf defteri | — | Planlandı |  |

## 12. Oyun ve Eğlence Araçları (`12_Games`)

| No | Uygulama | Paket | Durum | Not |
|---|---|---|---|---|
| 201 | Sudoku | — | Planlandı |  |
| 202 | Kelime bulmaca | — | Planlandı |  |
| 203 | Hafıza oyunu | — | Planlandı |  |
| 204 | Mantık bulmacaları | — | Planlandı |  |
| 205 | Zar uygulaması | — | Planlandı |  |
| 206 | Rastgele sayı üretici | — | Planlandı |  |
| 207 | Şans çarkı | — | Planlandı |  |
| 208 | Rastgele isim seçici | — | Planlandı |  |
| 209 | Taş kağıt makas | — | Planlandı |  |
| 210 | Dikkat geliştirme oyunu | — | Planlandı |  |
| 211 | Matematik oyunu | — | Planlandı |  |
| 212 | Kelime oyunu | — | Planlandı |  |
| 213 | Satranç (çevrimdışı) | — | Planlandı |  |
| 214 | Basit dama | — | Planlandı |  |
| 215 | Günlük beyin egzersizi | — | Planlandı |  |

## 13. Galaxy Offline App Center (`13_App_Center`)

| Uygulama | Durum |
|---|---|
| Galaxy Offline App Center | Planlandı |
