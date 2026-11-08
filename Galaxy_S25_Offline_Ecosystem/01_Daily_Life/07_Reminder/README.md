# Hatırlatıcı

Tekrarlı, ertelenebilir ve yeniden başlatmaya dayanıklı yerel hatırlatıcı.

| | |
|---|---|
| Katalog no | 7 |
| Paket adı | `com.galaxyoffline.reminder` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:reminder` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | Kodlandı |

## Özellikler
- Tarih ve saatli hatırlatmalar; tekrar: yok, her gün, her hafta, her ay, her yıl
- Bildirimde Tamamlandı ve 10 dk ertele eylemleri
- Yeniden başlatma, saat ve saat dilimi değişiminde otomatik yeniden kurma
- Bildirim ve tam zamanlı alarm izni için yönlendirmeli uyarılar

## İzinler
| İzin | Neden |
|---|---|
| `android.permission.POST_NOTIFICATIONS` | Hatırlatma zamanı geldiğinde bildirim göstermek için (Android 13+, sizden istenir). |
| `android.permission.SCHEDULE_EXACT_ALARM` | Hatırlatmayı tam dakikasında tetiklemek için. Verilmezse Android birkaç dakika gecikebilir. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Telefon yeniden başlatıldığında hatırlatmaları yeniden kurmak için. |

## Bilinen kısıtlamalar
- Samsung pil optimizasyonu (Uyku modundaki uygulamalar) bildirimleri geciktirebilir; uygulamayı listeden çıkarın.
- Tam zamanlı alarm izni verilmezse Android hatırlatmayı birkaç dakika geciktirebilir.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :reminder:assembleRelease      # APK: 01_Daily_Life/07_Reminder/build/outputs/apk/release/
./gradlew :reminder:testDebugUnitTest     # birim testleri
./gradlew :reminder:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
