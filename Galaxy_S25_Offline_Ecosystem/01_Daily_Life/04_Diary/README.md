# Kişisel Günlük

Ruh hali kayıtlı, aranabilir ve istatistikli kişisel günlük.

| | |
|---|---|
| Katalog no | 4 |
| Paket adı | `com.galaxyoffline.diary` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:diary` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Tarihli günlük kayıtları, 5 seviyeli ruh hali
- Aya göre gruplanmış liste, arama
- İstatistikler: toplam kayıt, bu ay, ardışık yazma serisi, ruh hali dağılımı
- Tüm günlüğü .txt olarak dışa aktarma (Depolama Erişim Çerçevesi, izin gerektirmez)

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Uygulama kilidi (biyometrik) bu sürümde yoktur; kayıtlar şifrelenmez.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :diary:assembleRelease      # APK: 01_Daily_Life/04_Diary/build/outputs/apk/release/
./gradlew :diary:testDebugUnitTest     # birim testleri
./gradlew :diary:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
