# PDF Okuyucu

Android'in yerleşik PDF motoruyla hızlı, izinsiz ve çevrimdışı PDF okuyucu.

| | |
|---|---|
| Katalog no | 107 |
| Paket adı | `com.galaxyoffline.pdfreader` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:pdfreader` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Dosya seçici ile PDF açma (depolama izni gerekmez); diğer uygulamalardan "Birlikte aç"
- Sayfa sayfa kaydırma, iki parmakla yakınlaştırma, sayfaya git
- Son açılan dosyalar ve kaldığı sayfayı hatırlama
- Gece modu (renkleri ters çevirme)
- Paylaşma

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Metin seçme/arama ve form doldurma desteklenmez (yerleşik PDF motoru görüntü üretir).
- Parola korumalı PDF'ler açılamaz.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :pdfreader:assembleRelease      # APK: 06_Media/107_PdfReader/build/outputs/apk/release/
./gradlew :pdfreader:testDebugUnitTest     # birim testleri
./gradlew :pdfreader:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
