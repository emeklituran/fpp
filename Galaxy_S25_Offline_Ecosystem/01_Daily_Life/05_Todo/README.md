# Yapılacaklar

Listeler, son tarih, öncelik ve akıllı filtrelerle yapılacaklar uygulaması.

| | |
|---|---|
| Katalog no | 5 |
| Paket adı | `com.galaxyoffline.todo` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:todo` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Birden çok liste (ekle, yeniden adlandır, sil)
- Görev: başlık, not, son tarih, öncelik (düşük/normal/yüksek)
- Akıllı filtreler: Bugün, Yaklaşan, Tümü, Tamamlanan; geciken görev vurgusu
- Sıralama (tarih, öncelik, oluşturma), tamamlananları temizleme, geri al

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Görev bildirimleri yoktur (izin gerektirmemek için); zamanlı uyarı için Hatırlatıcı uygulamasını kullanın.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :todo:assembleRelease      # APK: 01_Daily_Life/05_Todo/build/outputs/apk/release/
./gradlew :todo:testDebugUnitTest     # birim testleri
./gradlew :todo:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
