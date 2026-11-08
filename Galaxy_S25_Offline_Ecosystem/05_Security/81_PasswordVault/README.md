# Şifre Kasası

Ana şifre ve AES-256-GCM ile şifrelenmiş, isteğe bağlı biyometrik kilitli çevrimdışı şifre kasası.

| | |
|---|---|
| Katalog no | 81 |
| Paket adı | `com.galaxyoffline.passwordvault` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:passwordvault` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Ana şifreden PBKDF2-HMAC-SHA256 (310.000 tur) ile anahtar türetme; kayıtlar AES-256-GCM ile şifreli
- İsteğe bağlı biyometrik açma: veri anahtarı Android Keystore'da donanım destekli, biyometriye bağlı anahtarla sarılır
- Kayıt türleri: giriş, banka kartı, güvenli not, Wi-Fi, diğer; favoriler ve arama
- Güçlü şifre üretici ve şifre gücü ölçer
- Panoya kopyalama: hassas olarak işaretlenir, süre sonunda otomatik temizlenir
- Otomatik kilit (arka planda süre), ekran görüntüsü engeli (FLAG_SECURE), hatalı denemede bekleme
- Şifreli yedek dışa/içe aktarma (.gvault, ana şifre ile)

## İzinler
| İzin | Neden |
|---|---|
| `android.permission.USE_BIOMETRIC` | İsteğe bağlı: kasayı parmak izi veya yüz tanıma ile açmak için. |

## Bilinen kısıtlamalar
- Ana şifre unutulursa veriler kurtarılamaz (arka kapı yoktur).
- Tarayıcı/diğer uygulamalarda otomatik doldurma (Autofill) bu sürümde yok.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :passwordvault:assembleRelease      # APK: 05_Security/81_PasswordVault/build/outputs/apk/release/
./gradlew :passwordvault:testDebugUnitTest     # birim testleri
./gradlew :passwordvault:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
