# Ses Kaydedici

Yüksek kaliteli AAC ses kaydı, oynatma, yeniden adlandırma ve paylaşma; kayıtlar cihazda kalır.

| | |
|---|---|
| Katalog no | 103 |
| Paket adı | `com.galaxyoffline.voicerecorder` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:voicerecorder` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Ön plan hizmetiyle kesintisiz kayıt (ekran kapalıyken de), duraklat/devam
- Canlı ses seviyesi göstergesi ve süre
- Kayıt listesi: oynat, ileri/geri sar, yeniden adlandır, sil, paylaş, Müzik klasörüne dışa aktar
- Kalite seçimi (64 / 128 / 192 kbps)

## İzinler
| İzin | Neden |
|---|---|
| `android.permission.RECORD_AUDIO` | Ses kaydı yapmak için. |
| `android.permission.FOREGROUND_SERVICE` | Ekran kapalıyken kaydın kesilmemesi için. |
| `android.permission.FOREGROUND_SERVICE_MICROPHONE` | Ön plan hizmetinin mikrofon türü (Android 14+). |
| `android.permission.POST_NOTIFICATIONS` | Kayıt sürerken bildirimde durum ve durdurma düğmesi göstermek için. |

## Bilinen kısıtlamalar
- Telefon görüşmeleri kaydedilemez (Android kısıtlaması).
- Kayıtlar uygulamanın özel klasöründedir; uygulama kaldırılırsa silinir (saklamak için Müzik klasörüne aktarın veya paylaşın).

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :voicerecorder:assembleRelease      # APK: 06_Media/103_VoiceRecorder/build/outputs/apk/release/
./gradlew :voicerecorder:testDebugUnitTest     # birim testleri
./gradlew :voicerecorder:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
