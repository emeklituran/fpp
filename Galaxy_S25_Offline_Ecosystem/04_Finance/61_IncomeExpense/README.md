# Gelir Gider

Kategorili gelir-gider kaydı, aylık özet, bütçe ve grafiklerle çevrimdışı para takibi.

| | |
|---|---|
| Katalog no | 61 |
| Paket adı | `com.galaxyoffline.incomeexpense` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:incomeexpense` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Gelir ve gider kaydı: tutar, kategori, tarih, not, ödeme yöntemi
- Aylık özet: toplam gelir, gider, net; önceki aya göre değişim
- Kategori bazlı harcama grafiği ve aylık bütçe sınırları (aşım uyarısı)
- Ay ay gezinme, düzenleme ve geri alınabilir silme
- CSV dışa aktarma (Excel uyumlu, UTF-8)

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Banka hesaplarıyla senkronizasyon yoktur (internet yok); kayıtlar elle girilir.
- Tek para birimi (₺).

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :incomeexpense:assembleRelease      # APK: 04_Finance/61_IncomeExpense/build/outputs/apk/release/
./gradlew :incomeexpense:testDebugUnitTest     # birim testleri
./gradlew :incomeexpense:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
