# Alarm

Haftanın günlerine göre tekrarlı, ertelemeli, tam ekran çalan alarm saati.

| | |
|---|---|
| Katalog no | 9 |
| Paket adı | `com.galaxyoffline.alarm` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:alarm` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Saat seçici, etiket, haftanın günleri, titreşim ve erteleme süresi
- Sistem alarm saati API'si (setAlarmClock): durum çubuğunda sonraki alarm görünür
- Kilit ekranında tam ekran çalma, Ertele / Kapat; 10 dakika sonra otomatik susma
- Sonraki alarma kalan süre; yeniden başlatma ve saat değişiminde yeniden kurma
- Önizleme: alarm ekranını ve sesini anında deneme

## İzinler
| İzin | Neden |
|---|---|
| `android.permission.USE_EXACT_ALARM` | Alarmı tam dakikasında çaldırmak için (Android 13+, alarm uygulamalarına otomatik verilir). |
| `android.permission.SCHEDULE_EXACT_ALARM` | Android 12'de tam zamanlı alarm için (yalnızca API 31-32). |
| `android.permission.POST_NOTIFICATIONS` | Çalan alarmı ve yaklaşan alarmı bildirimde göstermek için. |
| `android.permission.USE_FULL_SCREEN_INTENT` | Kilit ekranında tam ekran alarm göstermek için. |
| `android.permission.FOREGROUND_SERVICE` | Alarm sesi çalarken uygulamanın sistem tarafından kapatılmaması için. |
| `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Ön plan hizmetinin alarm sesi çalma türü (Android 14+). |
| `android.permission.VIBRATE` | Alarm titreşimi için. |
| `android.permission.WAKE_LOCK` | Alarm çalarken işlemcinin uykuya geçmemesi için. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Yeniden başlatmadan sonra alarmları yeniden kurmak için. |

## Bilinen kısıtlamalar
- Alarm sesi olarak cihazın varsayılan alarm tonu kullanılır (özel ses seçimi sonraki sürümde).
- Android 14+ cihazlarda tam ekran gösterim izni kapatılmışsa alarm bildirim olarak çalar.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :alarm:assembleRelease      # APK: 01_Daily_Life/09_Alarm/build/outputs/apk/release/
./gradlew :alarm:testDebugUnitTest     # birim testleri
./gradlew :alarm:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
