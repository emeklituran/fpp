# Alışveriş Listesi

Kategorili, fiyat tahminli ve öneri destekli alışveriş listeleri.

| | |
|---|---|
| Katalog no | 6 |
| Paket adı | `com.galaxyoffline.shoppinglist` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:shoppinglist` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | Kodlandı |

## Özellikler
- Birden çok liste, ilerleme göstergesi
- Ürün: miktar, birim, kategori, tahmini birim fiyat
- Kategoriye göre gruplama, alınanlar en altta
- Daha önce eklenen ürünlerden otomatik öneri
- Tahmini toplam tutar, alınanları temizleme, listeyi metin olarak paylaşma

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Fiyatlar kullanıcı tarafından girilen tahmini değerlerdir; mağaza fiyatı çekilmez.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :shoppinglist:assembleRelease      # APK: 01_Daily_Life/06_ShoppingList/build/outputs/apk/release/
./gradlew :shoppinglist:testDebugUnitTest     # birim testleri
./gradlew :shoppinglist:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
