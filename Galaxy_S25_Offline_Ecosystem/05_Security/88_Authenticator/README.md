# Doğrulayıcı

RFC 6238 uyumlu, çevrimdışı iki adımlı doğrulama (TOTP) kod üretici.

| | |
|---|---|
| Katalog no | 88 |
| Paket adı | `com.galaxyoffline.authenticator` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:authenticator` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- QR kod tarama, galeriden QR görseli, otpauth bağlantısı yapıştırma veya elle ekleme
- SHA1/SHA256/SHA512, 6–8 basamak, 30/60 sn periyot
- Gizli anahtarlar Android Keystore anahtarıyla şifreli saklanır
- Kodlar saniye saniye yenilenir; dokununca kopyalanır (pano otomatik temizlenir)
- İsteğe bağlı uygulama kilidi (biyometri / ekran kilidi), ekran görüntüsü engeli
- Şifreli yedek dışa/içe aktarma (parola ile)

## İzinler
| İzin | Neden |
|---|---|
| `android.permission.CAMERA` | İsteğe bağlı: hesap eklerken QR kodu taramak için. |
| `android.permission.USE_BIOMETRIC` | İsteğe bağlı uygulama kilidi için. |

## Bilinen kısıtlamalar
- Kodlar telefon saatine dayanır; saat yanlışsa kodlar geçersiz olur (Ayarlar > Tarih ve saat > Otomatik).
- Sayaç tabanlı (HOTP) hesaplar desteklenmez.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :authenticator:assembleRelease      # APK: 05_Security/88_Authenticator/build/outputs/apk/release/
./gradlew :authenticator:testDebugUnitTest     # birim testleri
./gradlew :authenticator:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
