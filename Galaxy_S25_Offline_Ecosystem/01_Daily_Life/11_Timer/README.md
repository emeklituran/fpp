# Zamanlayıcı

Geri sayım zamanlayıcı ve Pomodoro: hazır süreler, kilit ekranında uyarı (katalog 11 ve 174).

| | |
|---|---|
| Katalog no | 11 |
| Paket adı | `com.galaxyoffline.timer` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:timer` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Saat/dakika/saniye seçimi ve kayıtlı hazır süreler
- Uygulama kapalıyken de doğru süre; bitince sesli ve titreşimli uyarı
- +1 dk ekleme, duraklat/devam, sıfırla
- Pomodoro modu: çalışma / kısa mola / uzun mola döngüsü ve tamamlanan oturum sayısı

## İzinler
| İzin | Neden |
|---|---|
| `android.permission.USE_EXACT_ALARM` | Süre dolduğunda tam zamanında çalmak için (zamanlayıcı uygulamalarına otomatik verilir). |
| `android.permission.SCHEDULE_EXACT_ALARM` | Android 12'de tam zamanlı alarm için (yalnızca API 31-32). |
| `android.permission.POST_NOTIFICATIONS` | Süre dolunca bildirim ve tam ekran uyarı göstermek için. |
| `android.permission.USE_FULL_SCREEN_INTENT` | Kilit ekranında süre bitti uyarısı için. |
| `android.permission.FOREGROUND_SERVICE` | Uyarı sesi çalarken uygulamanın kapanmaması için. |
| `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Ön plan hizmetinin ses çalma türü (Android 14+). |
| `android.permission.VIBRATE` | Uyarı titreşimi için. |
| `android.permission.WAKE_LOCK` | Uyarı çalarken işlemcinin uyumaması için. |

## Bilinen kısıtlamalar
- Aynı anda tek zamanlayıcı çalışır.
- Telefon yeniden başlatılırsa süren zamanlayıcı iptal olur (uygulama açılınca süre dolmuşsa uyarı verilir).
- Uyarı sesi 2 dakika sonra kendiliğinden susar.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :timer:assembleRelease      # APK: 01_Daily_Life/11_Timer/build/outputs/apk/release/
./gradlew :timer:testDebugUnitTest     # birim testleri
./gradlew :timer:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
