# Fatura ve Abonelik

Fatura ve abonelik takibi: son ödeme hatırlatması, ödendi işareti, aylık/yıllık maliyet özeti (katalog 52 ve 53).

| | |
|---|---|
| Katalog no | 52 |
| Paket adı | `com.galaxyoffline.billreminder` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:billreminder` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Fatura/abonelik: ad, tutar, kategori, tekrar (aylık, 3 aylık, 6 aylık, yıllık, tek seferlik), son ödeme günü
- "Ödendi" işareti ve ödeme geçmişi; bir sonraki dönem otomatik oluşur
- Geciken ve yaklaşan ödemeler; aylık ve yıllık toplam maliyet
- Günlük yerel kontrol ile X gün önce bildirim

## İzinler
| İzin | Neden |
|---|---|
| `android.permission.POST_NOTIFICATIONS` | Son ödeme günü yaklaşınca bildirim göstermek için. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Telefon yeniden başlayınca günlük kontrolü yeniden kurmak için. |

## Bilinen kısıtlamalar
- Bildirimler günde bir kez yapılan kontrolle gönderilir (tam dakika hassasiyeti gerekmez, ek izin istenmez).

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :billreminder:assembleRelease      # APK: 03_Home_Life/52_BillReminder/build/outputs/apk/release/
./gradlew :billreminder:testDebugUnitTest     # birim testleri
./gradlew :billreminder:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
