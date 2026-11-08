# Test raporu – Uygulama 6–10

Ortam: GitHub Actions `ubuntu-latest`, JDK 17, Gradle 8.14.3, AGP 8.11.1 · Emülatör: Android 15 (API 35), 1080×2400, AOSP (Google Play Hizmetleri yok)
Kaynak çalıştırma: [https://github.com/emeklituran/fpp/actions/runs/42457396458](https://github.com/emeklituran/fpp/actions/runs/42457396458) (commit `5c341be`)

## Kontrol listesi (her uygulama için)
| Kontrol | Yöntem | Sonuç |
|---|---|---|
| Gradle derlemesi | `./gradlew test verifyNoTrackers assembleRelease assembleDebug assembleDebugAndroidTest` | Başarılı |
| APK üretildi mi | `build/outputs/apk/release/*.apk` | Evet |
| Birim testleri | JUnit (JVM) | Tümü geçti |
| INTERNET izni | `aapt2 dump permissions` (birleştirilmiş APK manifesti) | Yok |
| Gereksiz izin | Uygulama başına `permissions.allow` ile birebir karşılaştırma | Fazla izin yok |
| Reklam/analiz/telemetri SDK | `verifyNoTrackers` (bağımlılık ağacı) + APK dex taraması | Yok |
| APK kurulabiliyor mu | `adb install` (emülatör) | Evet |
| Uygulama açılıyor mu | `am start -W` soğuk başlatma + süreç kontrolü | Evet |
| Çökme | `logcat -b crash` | Yok |
| Kapatıp açınca çalışıyor mu | `am force-stop` + yeniden başlatma | Evet |
| Ana özellik ve veri kalıcılığı | Compose cihaz testi (aktivite yeniden oluşturulduktan sonra veri kontrolü) | Geçti |
| Koyu tema / AMOLED | Ortak tasarım sistemi; ayarlardan seçilebilir | Kodlandı (otomatik ekran görüntüsü karşılaştırması yok) |

## Sonuçlar
| No | Uygulama | Paket | APK | Soğuk açılış | Kapat-aç | Çökme | Cihaz testi | Sonuç |
|---|---|---|---|---|---|---|---|---|
| 6 | Alışveriş Listesi | `com.galaxyoffline.shoppinglist` | 2.44 MB | 904 ms | 1147 ms | yok | 1/1 | GEÇTİ |
| 7 | Hatırlatıcı | `com.galaxyoffline.reminder` | 2.58 MB | 769 ms | 1288 ms | yok | 1/1 | GEÇTİ |
| 8 | Takvim | `com.galaxyoffline.calendar` | 2.60 MB | 563 ms | 567 ms | yok | 1/1 | GEÇTİ |
| 9 | Alarm | `com.galaxyoffline.alarm` | 2.44 MB | 1180 ms | 1066 ms | yok | 1/1 | GEÇTİ |
| 10 | Kronometre | `com.galaxyoffline.stopwatch` | 2.34 MB | 770 ms | 530 ms | yok | 1/1 | GEÇTİ |

## Bilinen sınırlar ve dürüst notlar
- Testler **emülatörde** yapıldı; fiziksel Galaxy S25 cihazında test edilmedi. One UI'ya özgü pil optimizasyonu davranışı emülatörde görülmez.
- "Kapat-aç" testi uygulamayı tamamen durdurur (`force-stop`); cihaz testi verinin aktivite yeniden oluşturulduktan sonra durduğunu doğrular. Telefon yeniden başlatma testi otomatik yapılmadı.
- Koyu/AMOLED tema görsel olarak elle doğrulanmadı; ekran görüntüleri CI artefaktında (`galaxy-device-report-*`) mevcuttur.
- CI sırasında emülatör bağlantısının bir kez koptuğu görüldü (altyapı sorunu); test betiği yeniden bağlanma ve iki paralel parçaya bölme ile güçlendirildi.
