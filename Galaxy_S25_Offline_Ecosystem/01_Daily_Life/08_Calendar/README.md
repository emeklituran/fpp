# Takvim

Ay görünümü, ajanda ve tekrarlı etkinliklerle yerel takvim.

| | |
|---|---|
| Katalog no | 8 |
| Paket adı | `com.galaxyoffline.calendar` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:calendar` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | Planlandı |

## Özellikler
- Pazartesi ile başlayan ay görünümü, bugün vurgusu, etkinlik noktaları
- Seçili günün etkinlikleri ve 60 günlük ajanda
- Etkinlik: başlık, not, tüm gün veya saat aralığı, renk, tekrar (haftalık/aylık/yıllık)
- Türkiye'nin sabit tarihli resmi tatilleri

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Sistem takvimi (Google/Samsung hesapları) okunmaz; etkinlikler yalnızca bu uygulamada tutulur.
- Dini bayramlar her yıl değiştiği ve doğrulanmış çevrimdışı veri olmadığı için gösterilmez.
- Etkinlik bildirimi yoktur; zamanlı uyarı için Hatırlatıcı uygulamasını kullanın.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :calendar:assembleRelease      # APK: 01_Daily_Life/08_Calendar/build/outputs/apk/release/
./gradlew :calendar:testDebugUnitTest     # birim testleri
./gradlew :calendar:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
