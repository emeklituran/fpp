# Alışkanlık Takipçisi

Günlük ve haftalık alışkanlıklar, seri takibi, takvim ısı haritası ve hatırlatmalar.

| | |
|---|---|
| Katalog no | 19 |
| Paket adı | `com.galaxyoffline.habittracker` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:habittracker` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Alışkanlık: ad, emoji simge, haftanın hangi günleri yapılacağı
- Bugün ekranında tek dokunuşla işaretleme
- Güncel ve en uzun seri, son 30 gün tamamlanma oranı, son 20 haftanın ısı haritası (geçmiş günleri düzeltme)
- İsteğe bağlı günlük hatırlatma saati; bildirimden "Yaptım" ile işaretleme
- Arşivleme ve silme

## İzinler
| İzin | Neden |
|---|---|
| `android.permission.POST_NOTIFICATIONS` | İsteğe bağlı günlük hatırlatma için. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Yeniden başlatmadan sonra hatırlatmaları kurmak için. |

## Bilinen kısıtlamalar
- Hatırlatmalar Android'in pil optimizasyonu nedeniyle birkaç dakika gecikebilir.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :habittracker:assembleRelease      # APK: 01_Daily_Life/19_HabitTracker/build/outputs/apk/release/
./gradlew :habittracker:testDebugUnitTest     # birim testleri
./gradlew :habittracker:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
