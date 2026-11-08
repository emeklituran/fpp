# Test raporu – Uygulama 16–20 (Dalga 2: Birim Dönüştürücü, Fatura ve Abonelik, Alışkanlık, Ses Kaydedici, Zamanlayıcı)

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
| 37 | Birim Dönüştürücü | `com.galaxyoffline.unitconverter` | 2.28 MB | 497 ms | 469 ms | yok | 1/1 | GEÇTİ |
| 52 | Fatura ve Abonelik | `com.galaxyoffline.billreminder` | 2.57 MB | 951 ms | 457 ms | yok | 1/1 | GEÇTİ |
| 19 | Alışkanlık Takipçisi | `com.galaxyoffline.habittracker` | 2.50 MB | 528 ms | 513 ms | yok | 1/1 | GEÇTİ |
| 103 | Ses Kaydedici | `com.galaxyoffline.voicerecorder` | 2.41 MB | 766 ms | 656 ms | yok | 1/1 | GEÇTİ |
| 11 | Zamanlayıcı | `com.galaxyoffline.timer` | 2.34 MB | 835 ms | 1403 ms | yok | 2/2 | GEÇTİ |

## Uygulama bazında neyin test edildiği
| No | Birim testleri | Cihaz (emülatör) testi |
|---|---|---|
| 37 Birim Dönüştürücü | UnitsTest | Sıcaklık: 100 °C → 212 °F |
| 52 Fatura ve Abonelik | BillLogicTest | Fatura ekleme (Elektrik, 450 ₺) → listede ve aylık toplamda görünme. Not: test adı "addBillAndMarkPaid" olsa da "Ödendi" adımı cihaz testinde yok; ödeme sonrası dönem hesabı BillLogicTest ile test edildi |
| 19 Alışkanlık Takipçisi | HabitLogicTest | Alışkanlık ekleme → bugünü işaretleme → "🔥 1 gün seri" gösterimi |
| 103 Ses Kaydedici | RecFilesTest | Kayda başlama (ön plan hizmeti) → ~2 sn → durdurma → kaydın listede görünmesi |
| 11 Zamanlayıcı | TimerEngineTest | 3 dk hızlı başlat → duraklat → +1 dk → sıfırla; Pomodoro 25 dk başlat → sıfırla |

## Test edilmeyenler ve dürüst notlar
- Testler **emülatörde** yapıldı; fiziksel Galaxy S25 cihazında test edilmedi.
- Fatura ve Abonelik: Günlük bildirim kontrolü (09:00) otomatik testte tetiklenmedi.
- Alışkanlık Takipçisi: Hatırlatma bildirimi ve bildirimden "Yaptım" otomatik testte tetiklenmedi.
- Ses Kaydedici: Emülatör sesi kapalıydı (-noaudio); test dosyanın oluşup listelendiğini doğrular, ses kalitesini doğrulamaz.
- Zamanlayıcı: Süre dolunca çalan uyarı (tam ekran, ses) otomatik testte beklenmedi; zamanlama mantığı TimerEngineTest ile doğrulandı.
- Release APK’lar testte izinler önceden verilerek (`-g`) kuruldu. Gerçek kullanımda izinler ilk ihtiyaç anında sorulur; izin reddedilme akışı otomatik test edilmedi.
- CI emülatörü (Android Emulator 37.2.13, yazılımsal GPU) bazı çalıştırmalarda kendiliğinden çöktü. Bu durumda etkilenen uygulamalar yeni bir emülatörde (farklı GPU modu: `swangle_indirect`) yeniden test edilir. CI #19’da parça 2/4’te Bilimsel Hesap Makinesi ve Yapılacaklar bu şekilde tekrar test edilip geçti; bu raporda listelenen uygulamaların hiçbiri tekrar denemeye kalmadı.
- Koyu/AMOLED tema görsel olarak elle doğrulanmadı.
