# Birim Dönüştürücü

Uzunluk, ağırlık, sıcaklık, hacim, alan, hız, veri, süre, enerji, basınç ve mutfak ölçüleri (katalog 37, 39–45, 56).

| | |
|---|---|
| Katalog no | 37 |
| Paket adı | `com.galaxyoffline.unitconverter` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:unitconverter` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- 13 kategori, 100+ birim (uzunluk, ağırlık, sıcaklık, hacim, alan/dönüm, hız, veri, zaman, enerji, basınç, güç, açı, mutfak); anında dönüşüm
- Tek girişte tüm birimlerin listesi
- Her kategoride son seçilen birim hatırlanır
- Türkçe sayı biçimi, sonuca dokunarak kopyalama

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Para birimi dönüşümü bu uygulamada yoktur (canlı kur internet gerektirir; manuel kur uygulaması ayrıca planlanmıştır).

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :unitconverter:assembleRelease      # APK: 02_Calculators/37_UnitConverter/build/outputs/apk/release/
./gradlew :unitconverter:testDebugUnitTest     # birim testleri
./gradlew :unitconverter:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
