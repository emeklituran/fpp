# Satranç

Çevrimdışı satranç: bilgisayara karşı ve iki kişilik oyun, Stockfish 19 ile canlı analiz (en iyi 3 hamle ve varyantlar), konum düzenleyici, FEN.

| | |
|---|---|
| Katalog no | 213 |
| Paket adı | `com.galaxyoffline.chess` |
| Sürüm | 1.0.0 (1) |
| Gradle modülü | `:chess` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | APK oluşturuldu |

## Özellikler
- Dokunarak hamle; terfi seçimi, rok, geçerken alma; tahtayı çevirme; hamle geçmişinde geri/ileri
- Bilgisayara karşı (seviye ayarlı) ve aynı cihazda iki kişilik oyun
- Canlı analiz: en iyi 3 hamle, puan, devam varyantı; hamle yapılınca otomatik güncellenir; varyant önizleme
- Konum düzenleyici: taş yerleştirme, sıra, rok ve geçerken alma; olanaksız konumlar nedeniyle reddedilir
- FEN içe/dışa aktarma; oyunlar cihazda kaydedilir
- FIDE beraberlik kuralları: pat, ölü pozisyon, 5 tekrar ve 75 hamle otomatik; 3 tekrar ve 50 hamle talep ile
- Motor testi ekranı (cihazda hız ölçümü)

## İzinler
| İzin | Neden |
|---|---|
| — | Bu uygulama hiçbir izin istemez. |

## Bilinen kısıtlamalar
- Sürükleyerek hamle bu sürümde yok (dokunarak hamle var).
- PGN, açılış adları, oyun sonu hata raporu ve Türkçe açıklamalar sonraki sürümlerde (1.1, 1.2).
- Motor dosyası büyük: uygulama cihazda yaklaşık 190 MB yer kaplar.

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :chess:assembleRelease      # APK: 12_Games/213_Chess/build/outputs/apk/release/
./gradlew :chess:testDebugUnitTest     # birim testleri
./gradlew :chess:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
