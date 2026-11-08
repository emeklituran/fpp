# Not Defteri

Sabitleme, renk, arşiv, çöp kutusu ve aramalı gelişmiş not defteri.

| | |
|---|---|
| Katalog no | 3 |
| Paket adı | `com.galaxyoffline.notes` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:notes` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | Planlandı |

## Özellikler
- Otomatik kaydetme; başlık ve içerik, kelime/karakter sayısı
- Sabitleme, 8 not rengi, arşiv ve 30 günlük çöp kutusu (geri yükleme)
- Tam metin arama, sıralama (düzenleme, oluşturma, başlık), liste/ızgara görünümü
- Silmede geri al; notu paylaşma (Android paylaşım menüsü) ve .txt olarak dışa aktarma

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Notlar şifrelenmez; hassas bilgiler için Güvenlik kategorisindeki gizli not kasası planlanmıştır.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :notes:assembleRelease      # APK: 01_Daily_Life/03_Notes/build/outputs/apk/release/
./gradlew :notes:testDebugUnitTest     # birim testleri
./gradlew :notes:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
