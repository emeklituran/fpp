# QR Kod

Çevrimdışı QR kod ve barkod okuyucu + QR üretici (katalog 159 ve 160).

| | |
|---|---|
| Katalog no | 159 |
| Paket adı | `com.galaxyoffline.qrcode` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:qrcode` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Kamera ile QR ve barkod (EAN, UPC, Code 128/39, Data Matrix, PDF417, Aztec) okuma; fener desteği
- Galeriden görsel seçerek okuma
- İçerik türü tanıma: bağlantı, Wi-Fi, kişi kartı, telefon, e-posta, SMS, konum, metin
- Bağlantıyı açmadan önce adres gösterilir (güvenlik); Wi-Fi şifresi kopyalanabilir
- QR üretme: metin, bağlantı, Wi-Fi, kişi; görsel olarak kaydet veya paylaş
- Tarama geçmişi (Room), silme ve temizleme

## İzinler
| İzin | Neden |
|---|---|
| `android.permission.CAMERA` | QR kod ve barkod taramak için. Görüntüler kaydedilmez, yalnızca cihazda işlenir. |

## Bilinen kısıtlamalar
- Bağlantılar varsayılan tarayıcıda açılır; bu uygulama internete çıkmaz.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :qrcode:assembleRelease      # APK: 09_Text_Documents/159_QrCode/build/outputs/apk/release/
./gradlew :qrcode:testDebugUnitTest     # birim testleri
./gradlew :qrcode:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
