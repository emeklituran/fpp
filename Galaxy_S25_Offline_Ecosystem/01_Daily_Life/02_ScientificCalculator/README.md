# Bilimsel Hesap Makinesi

Trigonometri, logaritma, üs, kök, faktöriyel ve sabitlerle bilimsel hesap makinesi.

| | |
|---|---|
| Katalog no | 2 |
| Paket adı | `com.galaxyoffline.scientificcalculator` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:scientificcalculator` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | Test edildi |

## Özellikler
- sin, cos, tan ve tersleri (2nd), hiperbolik fonksiyonlar
- DEG / RAD açı modu (kalıcı)
- ln, log, log₂, eˣ, 10ˣ, x², xʸ, √, ∛, 1/x, |x|, x!
- π ve e sabitleri, Ans (son sonuç), bilimsel gösterim (E)
- Eksik parantezleri otomatik tamamlama, canlı önizleme
- Hesap geçmişi (Room) ve sonuç kopyalama

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Karmaşık sayılar, matris ve grafik çizimi desteklenmez.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :scientificcalculator:assembleRelease      # APK: 01_Daily_Life/02_ScientificCalculator/build/outputs/apk/release/
./gradlew :scientificcalculator:testDebugUnitTest     # birim testleri
./gradlew :scientificcalculator:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
