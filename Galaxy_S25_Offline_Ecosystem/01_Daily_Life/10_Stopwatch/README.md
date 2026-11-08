# Kronometre

Turlu, uygulama kapansa bile süreyi koruyan, oturum geçmişli kronometre.

| | |
|---|---|
| Katalog no | 10 |
| Paket adı | `com.galaxyoffline.stopwatch` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:stopwatch` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | Kodlandı |

## Özellikler
- Salise hassasiyeti; başlat, duraklat, tur, sıfırla
- Tur listesi: en hızlı ve en yavaş tur vurgusu
- Uygulama kapatılsa veya telefon yeniden başlatılsa bile süre korunur
- Oturumları kaydetme ve geçmiş (Room)
- Ekranı açık tutma ve titreşimli geri bildirim ayarları

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Çalışan kronometre için kalıcı bildirim yoktur (bildirim izni istememek için).

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :stopwatch:assembleRelease      # APK: 01_Daily_Life/10_Stopwatch/build/outputs/apk/release/
./gradlew :stopwatch:testDebugUnitTest     # birim testleri
./gradlew :stopwatch:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
