# Test raporu – Uygulama 11–15 (Dalga 2: Şifre Kasası, Doğrulayıcı, Gelir Gider, QR Kod, PDF Okuyucu)

Ortam: GitHub Actions `ubuntu-latest`, JDK 17, Gradle 8.14.3, AGP 8.11.1 · Emülatör: Android 15 (API 35), 1080×2400, AOSP (Google Play Hizmetleri yok), 4 paralel emülatör
Kaynak çalıştırma: CI #19 [https://github.com/emeklituran/fpp/actions/runs/42480565013](https://github.com/emeklituran/fpp/actions/runs/42480565013) (commit `c9809bf`)

## Kontrol listesi (her uygulama için)
| Kontrol | Yöntem | Sonuç |
|---|---|---|
| Gradle derlemesi | `./gradlew test verifyNoTrackers assembleRelease assembleDebug assembleDebugAndroidTest` | Başarılı |
| APK üretildi mi | `build/outputs/apk/release/*.apk` | Evet |
| Birim testleri | JUnit (JVM) | Tümü geçti |
| INTERNET izni | `aapt2 dump permissions` (birleştirilmiş APK manifesti) | Yok |
| Gereksiz izin | Uygulama başına `permissions.allow` ile birebir karşılaştırma | Fazla izin yok |
| Reklam/analiz/telemetri SDK | `verifyNoTrackers` (bağımlılık ağacı) + APK dex taraması | Yok |
| APK kurulabiliyor mu | `adb install -g` (emülatör) | Evet |
| Uygulama açılıyor mu | `am start -W` soğuk başlatma + süreç kontrolü | Evet |
| Çökme | `logcat -b crash` | Yok |
| Kapatıp açınca çalışıyor mu | `am force-stop` + yeniden başlatma | Evet |
| Ana özellik | Compose cihaz testi (aşağıda uygulama bazında) | Geçti |

## Sonuçlar
| No | Uygulama | Paket | APK | Soğuk açılış | Kapat-aç | Çökme | Cihaz testi | Sonuç |
|---|---|---|---|---|---|---|---|---|
| 81 | Şifre Kasası | `com.galaxyoffline.passwordvault` | 3.06 MB | 641 ms | 1191 ms | yok | 1/1 | GEÇTİ |
| 88 | Doğrulayıcı | `com.galaxyoffline.authenticator` | 3.78 MB | 1342 ms | 962 ms | yok | 1/1 | GEÇTİ |
| 61 | Gelir Gider | `com.galaxyoffline.incomeexpense` | 2.56 MB | 613 ms | 849 ms | yok | 1/1 | GEÇTİ |
| 159 | QR Kod | `com.galaxyoffline.qrcode` | 3.63 MB | 646 ms | 653 ms | yok | 1/1 | GEÇTİ |
| 107 | PDF Okuyucu | `com.galaxyoffline.pdfreader` | 2.37 MB | 583 ms | 498 ms | yok | 1/1 | GEÇTİ |

## Uygulama bazında neyin test edildiği
| No | Birim testleri | Cihaz (emülatör) testi |
|---|---|---|
| 81 Şifre Kasası | BackupTest; CryptoTest (core:crypto) | Ana şifre kurulumu → kayıt ekleme → kilitleme → ana şifreyle açma; kayıt şifre çözülerek görünür |
| 88 Doğrulayıcı | CryptoTest (core:crypto; RFC 6238 test vektörleri dahil) | Elle hesap ekleme (Base32 anahtar) → listede görünme → aktivite yeniden oluşturulduktan sonra Keystore ile çözülüp tekrar görünme |
| 61 Gelir Gider | MoneyLogicTest | Gider ekleme (250,50 ₺) → aylık özetin güncellenmesi → yeniden oluşturma sonrası verinin kalması |
| 159 QR Kod | QrParserTest | Oluştur sekmesinde metinden QR görseli üretme |
| 107 PDF Okuyucu | — (yalnızca cihaz testi) | Test içinde üretilen 3 sayfalık PDF dosyasını açma ve sayfa göstergesini doğrulama |

## Test edilmeyenler ve dürüst notlar
- Testler **emülatörde** yapıldı; fiziksel Galaxy S25 cihazında test edilmedi.
- Şifre Kasası: Biyometrik açma emülatörde test edilmedi (kayıtlı parmak izi yok).
- Doğrulayıcı: Kamerayla QR tarama emülatörde test edilmedi (emülatör kamerası kapalı); elle ekleme test edildi.
- Doğrulayıcı: Biyometrik/ekran kilidi ile uygulama kilidi emülatörde test edilmedi.
- QR Kod: Kamerayla tarama emülatörde test edilmedi (emülatör kamerası kapalı); QR çözümleme mantığı QrParserTest ile, üretim cihaz testiyle doğrulandı.
- Release APK’lar testte izinler önceden verilerek (`-g`) kuruldu. Gerçek kullanımda izinler ilk ihtiyaç anında sorulur; izin reddedilme akışı otomatik test edilmedi.
- CI emülatörü (Android Emulator 37.2.13, yazılımsal GPU) bazı çalıştırmalarda kendiliğinden çöktü. Bu durumda etkilenen uygulamalar yeni bir emülatörde (farklı GPU modu: `swangle_indirect`) yeniden test edilir. CI #19’da parça 2/4’te Bilimsel Hesap Makinesi ve Yapılacaklar bu şekilde tekrar test edilip geçti; bu raporda listelenen uygulamaların hiçbiri tekrar denemeye kalmadı.
- Koyu/AMOLED tema görsel olarak elle doğrulanmadı.
