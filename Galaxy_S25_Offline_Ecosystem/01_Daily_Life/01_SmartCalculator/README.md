# Akıllı Hesap Makinesi

Canlı sonuç önizlemeli, akıllı yüzde ve bellek tuşlu günlük hesap makinesi.

| | |
|---|---|
| Katalog no | 1 |
| Paket adı | `com.galaxyoffline.calculator` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:smartcalculator` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | Kodlandı |

## Özellikler
- Yazarken canlı sonuç önizlemesi; işlem önceliği ve parantez desteği
- Akıllı yüzde: 200 + %10 = 220, 200 − %10 = 180, 200 × %10 = 20
- Tek tuşla akıllı parantez, ± işaret değiştirme, geri silme
- Bellek tuşları (MC, MR, M+, M−); bellek kalıcıdır
- Hesap geçmişi (Room): dokunarak yeniden kullanma, tek tek veya tümden silme
- Sonucu uzun basarak kopyalama; Türkçe biçim (1.234,56)
- Ayarlar: titreşimli geri bildirim, binlik ayırıcı, tema (açık/koyu/AMOLED)

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Hesaplamalar 12 anlamlı basamakla gösterilir (çift duyarlıklı kayan nokta).

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :smartcalculator:assembleRelease      # APK: 01_Daily_Life/01_SmartCalculator/build/outputs/apk/release/
./gradlew :smartcalculator:testDebugUnitTest     # birim testleri
./gradlew :smartcalculator:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
