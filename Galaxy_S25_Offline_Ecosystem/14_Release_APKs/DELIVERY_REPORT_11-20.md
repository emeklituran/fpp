# APK teslimat raporu – Uygulama 11–20 (Dalga 2)

Tüm APK’lar: **[galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557)** (CI #19, GitHub Actions artefaktı, 6 Şubat 2027’ye kadar saklanır). Bu artefakt 20 uygulamanın tamamını (1–20) içerir; zip içinde `SHA256SUMS.txt` ve `audit.md` bulunur.

İmza: CI debug anahtarı (kalıcı anahtar tanımlanmadı) – kurulabilir, ancak farklı bir CI çalıştırmasından gelen sürüm eskisinin üzerine güncellenemez (önce kaldırmak gerekir; veriler silinir).

Not: Uygulama 1–10’un bu çalıştırmadaki APK’ları da artefaktta vardır ve yeniden test edilip geçmiştir; ayrıntılar `DELIVERY_REPORT_01-10.md`.

## 11. Şifre Kasası (katalog no 81)

| Alan | Değer |
|---|---|
| 1. Uygulama adı | Şifre Kasası |
| 2. Paket adı | `com.galaxyoffline.passwordvault` |
| 3. Sürüm | 1.0.0 (1) |
| 4. APK dosya adı | `com.galaxyoffline.passwordvault-1.0.0.apk` |
| 5. APK boyutu | 3.06 MB |
| 6. Minimum Android | 12 (API 31) · hedef Android 16 (API 36) |
| 7. İzinler | `USE_BIOMETRIC` |
| 8. Çevrimdışı çalışma | Tam (INTERNET izni yok, CI ile doğrulandı) |
| 9. Kütüphaneler | Kotlin 2.2.0, Jetpack Compose (BOM 2025.07.00), Material 3, Navigation Compose 2.9.2, Lifecycle 2.9.2, Room 2.7.2, DataStore 1.1.7, AndroidX Core 1.16.0, AndroidX Biometric 1.1.0, Lifecycle Process 2.9.2 |
| 10. Test sonucu | Birim testleri geçti (BackupTest; CryptoTest (core:crypto)); cihaz testi 1/1 geçti; soğuk açılış 641 ms; çökme yok |
| 11. Bilinen kısıtlamalar | Ana şifre unutulursa veriler kurtarılamaz (arka kapı yoktur). Tarayıcı/diğer uygulamalarda otomatik doldurma (Autofill) bu sürümde yok. Biyometrik açma emülatörde test edilmedi (kayıtlı parmak izi yok). |
| 12. Kaynak kodu | `Galaxy_S25_Offline_Ecosystem/05_Security/81_PasswordVault/` |
| 13. APK gerçek konumu | [galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557) → `com.galaxyoffline.passwordvault-1.0.0.apk` |

## 12. Doğrulayıcı (katalog no 88)

| Alan | Değer |
|---|---|
| 1. Uygulama adı | Doğrulayıcı |
| 2. Paket adı | `com.galaxyoffline.authenticator` |
| 3. Sürüm | 1.0.0 (1) |
| 4. APK dosya adı | `com.galaxyoffline.authenticator-1.0.0.apk` |
| 5. APK boyutu | 3.78 MB |
| 6. Minimum Android | 12 (API 31) · hedef Android 16 (API 36) |
| 7. İzinler | `CAMERA`, `USE_BIOMETRIC` |
| 8. Çevrimdışı çalışma | Tam (INTERNET izni yok, CI ile doğrulandı) |
| 9. Kütüphaneler | Kotlin 2.2.0, Jetpack Compose (BOM 2025.07.00), Material 3, Navigation Compose 2.9.2, Lifecycle 2.9.2, Room 2.7.2, DataStore 1.1.7, AndroidX Core 1.16.0, AndroidX Biometric 1.1.0, Lifecycle Process 2.9.2, CameraX 1.4.2, ZXing core 3.5.3 |
| 10. Test sonucu | Birim testleri geçti (CryptoTest (core:crypto; RFC 6238 test vektörleri dahil)); cihaz testi 1/1 geçti; soğuk açılış 1342 ms; çökme yok |
| 11. Bilinen kısıtlamalar | Kodlar telefon saatine dayanır; saat yanlışsa kodlar geçersiz olur (Ayarlar > Tarih ve saat > Otomatik). Sayaç tabanlı (HOTP) hesaplar desteklenmez. Kamerayla QR tarama emülatörde test edilmedi (emülatör kamerası kapalı); elle ekleme test edildi. Biyometrik/ekran kilidi ile uygulama kilidi emülatörde test edilmedi. |
| 12. Kaynak kodu | `Galaxy_S25_Offline_Ecosystem/05_Security/88_Authenticator/` |
| 13. APK gerçek konumu | [galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557) → `com.galaxyoffline.authenticator-1.0.0.apk` |

## 13. Gelir Gider (katalog no 61)

| Alan | Değer |
|---|---|
| 1. Uygulama adı | Gelir Gider |
| 2. Paket adı | `com.galaxyoffline.incomeexpense` |
| 3. Sürüm | 1.0.0 (1) |
| 4. APK dosya adı | `com.galaxyoffline.incomeexpense-1.0.0.apk` |
| 5. APK boyutu | 2.56 MB |
| 6. Minimum Android | 12 (API 31) · hedef Android 16 (API 36) |
| 7. İzinler | Yok |
| 8. Çevrimdışı çalışma | Tam (INTERNET izni yok, CI ile doğrulandı) |
| 9. Kütüphaneler | Kotlin 2.2.0, Jetpack Compose (BOM 2025.07.00), Material 3, Navigation Compose 2.9.2, Lifecycle 2.9.2, Room 2.7.2, DataStore 1.1.7, AndroidX Core 1.16.0 |
| 10. Test sonucu | Birim testleri geçti (MoneyLogicTest); cihaz testi 1/1 geçti; soğuk açılış 613 ms; çökme yok |
| 11. Bilinen kısıtlamalar | Banka hesaplarıyla senkronizasyon yoktur (internet yok); kayıtlar elle girilir. Tek para birimi (₺). |
| 12. Kaynak kodu | `Galaxy_S25_Offline_Ecosystem/04_Finance/61_IncomeExpense/` |
| 13. APK gerçek konumu | [galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557) → `com.galaxyoffline.incomeexpense-1.0.0.apk` |

## 14. QR Kod (katalog no 159)

| Alan | Değer |
|---|---|
| 1. Uygulama adı | QR Kod |
| 2. Paket adı | `com.galaxyoffline.qrcode` |
| 3. Sürüm | 1.0.0 (1) |
| 4. APK dosya adı | `com.galaxyoffline.qrcode-1.0.0.apk` |
| 5. APK boyutu | 3.63 MB |
| 6. Minimum Android | 12 (API 31) · hedef Android 16 (API 36) |
| 7. İzinler | `CAMERA` |
| 8. Çevrimdışı çalışma | Tam (INTERNET izni yok, CI ile doğrulandı) |
| 9. Kütüphaneler | Kotlin 2.2.0, Jetpack Compose (BOM 2025.07.00), Material 3, Navigation Compose 2.9.2, Lifecycle 2.9.2, Room 2.7.2, DataStore 1.1.7, AndroidX Core 1.16.0, CameraX 1.4.2, ZXing core 3.5.3 |
| 10. Test sonucu | Birim testleri geçti (QrParserTest); cihaz testi 1/1 geçti; soğuk açılış 646 ms; çökme yok |
| 11. Bilinen kısıtlamalar | Bağlantılar varsayılan tarayıcıda açılır; bu uygulama internete çıkmaz. Kamerayla tarama emülatörde test edilmedi (emülatör kamerası kapalı); QR çözümleme mantığı QrParserTest ile, üretim cihaz testiyle doğrulandı. |
| 12. Kaynak kodu | `Galaxy_S25_Offline_Ecosystem/09_Text_Documents/159_QrCode/` |
| 13. APK gerçek konumu | [galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557) → `com.galaxyoffline.qrcode-1.0.0.apk` |

## 15. PDF Okuyucu (katalog no 107)

| Alan | Değer |
|---|---|
| 1. Uygulama adı | PDF Okuyucu |
| 2. Paket adı | `com.galaxyoffline.pdfreader` |
| 3. Sürüm | 1.0.0 (1) |
| 4. APK dosya adı | `com.galaxyoffline.pdfreader-1.0.0.apk` |
| 5. APK boyutu | 2.37 MB |
| 6. Minimum Android | 12 (API 31) · hedef Android 16 (API 36) |
| 7. İzinler | Yok |
| 8. Çevrimdışı çalışma | Tam (INTERNET izni yok, CI ile doğrulandı) |
| 9. Kütüphaneler | Kotlin 2.2.0, Jetpack Compose (BOM 2025.07.00), Material 3, Navigation Compose 2.9.2, Lifecycle 2.9.2, Room 2.7.2, DataStore 1.1.7, AndroidX Core 1.16.0 |
| 10. Test sonucu | Birim testleri geçti (— (yalnızca cihaz testi)); cihaz testi 1/1 geçti; soğuk açılış 583 ms; çökme yok |
| 11. Bilinen kısıtlamalar | Metin seçme/arama ve form doldurma desteklenmez (yerleşik PDF motoru görüntü üretir). Parola korumalı PDF'ler açılamaz. |
| 12. Kaynak kodu | `Galaxy_S25_Offline_Ecosystem/06_Media/107_PdfReader/` |
| 13. APK gerçek konumu | [galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557) → `com.galaxyoffline.pdfreader-1.0.0.apk` |

## 16. Birim Dönüştürücü (katalog no 37)

| Alan | Değer |
|---|---|
| 1. Uygulama adı | Birim Dönüştürücü |
| 2. Paket adı | `com.galaxyoffline.unitconverter` |
| 3. Sürüm | 1.0.0 (1) |
| 4. APK dosya adı | `com.galaxyoffline.unitconverter-1.0.0.apk` |
| 5. APK boyutu | 2.28 MB |
| 6. Minimum Android | 12 (API 31) · hedef Android 16 (API 36) |
| 7. İzinler | Yok |
| 8. Çevrimdışı çalışma | Tam (INTERNET izni yok, CI ile doğrulandı) |
| 9. Kütüphaneler | Kotlin 2.2.0, Jetpack Compose (BOM 2025.07.00), Material 3, Navigation Compose 2.9.2, Lifecycle 2.9.2, Room 2.7.2, DataStore 1.1.7, AndroidX Core 1.16.0 |
| 10. Test sonucu | Birim testleri geçti (UnitsTest); cihaz testi 1/1 geçti; soğuk açılış 497 ms; çökme yok |
| 11. Bilinen kısıtlamalar | Para birimi dönüşümü bu uygulamada yoktur (canlı kur internet gerektirir; manuel kur uygulaması ayrıca planlanmıştır). |
| 12. Kaynak kodu | `Galaxy_S25_Offline_Ecosystem/02_Calculators/37_UnitConverter/` |
| 13. APK gerçek konumu | [galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557) → `com.galaxyoffline.unitconverter-1.0.0.apk` |

## 17. Fatura ve Abonelik (katalog no 52)

| Alan | Değer |
|---|---|
| 1. Uygulama adı | Fatura ve Abonelik |
| 2. Paket adı | `com.galaxyoffline.billreminder` |
| 3. Sürüm | 1.0.0 (1) |
| 4. APK dosya adı | `com.galaxyoffline.billreminder-1.0.0.apk` |
| 5. APK boyutu | 2.57 MB |
| 6. Minimum Android | 12 (API 31) · hedef Android 16 (API 36) |
| 7. İzinler | `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` |
| 8. Çevrimdışı çalışma | Tam (INTERNET izni yok, CI ile doğrulandı) |
| 9. Kütüphaneler | Kotlin 2.2.0, Jetpack Compose (BOM 2025.07.00), Material 3, Navigation Compose 2.9.2, Lifecycle 2.9.2, Room 2.7.2, DataStore 1.1.7, AndroidX Core 1.16.0 |
| 10. Test sonucu | Birim testleri geçti (BillLogicTest); cihaz testi 1/1 geçti; soğuk açılış 951 ms; çökme yok |
| 11. Bilinen kısıtlamalar | Bildirimler günde bir kez yapılan kontrolle gönderilir (tam dakika hassasiyeti gerekmez, ek izin istenmez). Günlük bildirim kontrolü (09:00) otomatik testte tetiklenmedi. |
| 12. Kaynak kodu | `Galaxy_S25_Offline_Ecosystem/03_Home_Life/52_BillReminder/` |
| 13. APK gerçek konumu | [galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557) → `com.galaxyoffline.billreminder-1.0.0.apk` |

## 18. Alışkanlık Takipçisi (katalog no 19)

| Alan | Değer |
|---|---|
| 1. Uygulama adı | Alışkanlık Takipçisi |
| 2. Paket adı | `com.galaxyoffline.habittracker` |
| 3. Sürüm | 1.0.0 (1) |
| 4. APK dosya adı | `com.galaxyoffline.habittracker-1.0.0.apk` |
| 5. APK boyutu | 2.50 MB |
| 6. Minimum Android | 12 (API 31) · hedef Android 16 (API 36) |
| 7. İzinler | `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` |
| 8. Çevrimdışı çalışma | Tam (INTERNET izni yok, CI ile doğrulandı) |
| 9. Kütüphaneler | Kotlin 2.2.0, Jetpack Compose (BOM 2025.07.00), Material 3, Navigation Compose 2.9.2, Lifecycle 2.9.2, Room 2.7.2, DataStore 1.1.7, AndroidX Core 1.16.0 |
| 10. Test sonucu | Birim testleri geçti (HabitLogicTest); cihaz testi 1/1 geçti; soğuk açılış 528 ms; çökme yok |
| 11. Bilinen kısıtlamalar | Hatırlatmalar Android'in pil optimizasyonu nedeniyle birkaç dakika gecikebilir. Hatırlatma bildirimi ve bildirimden "Yaptım" otomatik testte tetiklenmedi. |
| 12. Kaynak kodu | `Galaxy_S25_Offline_Ecosystem/01_Daily_Life/19_HabitTracker/` |
| 13. APK gerçek konumu | [galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557) → `com.galaxyoffline.habittracker-1.0.0.apk` |

## 19. Ses Kaydedici (katalog no 103)

| Alan | Değer |
|---|---|
| 1. Uygulama adı | Ses Kaydedici |
| 2. Paket adı | `com.galaxyoffline.voicerecorder` |
| 3. Sürüm | 1.0.0 (1) |
| 4. APK dosya adı | `com.galaxyoffline.voicerecorder-1.0.0.apk` |
| 5. APK boyutu | 2.41 MB |
| 6. Minimum Android | 12 (API 31) · hedef Android 16 (API 36) |
| 7. İzinler | `RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `POST_NOTIFICATIONS` |
| 8. Çevrimdışı çalışma | Tam (INTERNET izni yok, CI ile doğrulandı) |
| 9. Kütüphaneler | Kotlin 2.2.0, Jetpack Compose (BOM 2025.07.00), Material 3, Navigation Compose 2.9.2, Lifecycle 2.9.2, Room 2.7.2, DataStore 1.1.7, AndroidX Core 1.16.0 |
| 10. Test sonucu | Birim testleri geçti (RecFilesTest); cihaz testi 1/1 geçti; soğuk açılış 766 ms; çökme yok |
| 11. Bilinen kısıtlamalar | Telefon görüşmeleri kaydedilemez (Android kısıtlaması). Kayıtlar uygulamanın özel klasöründedir; uygulama kaldırılırsa silinir (saklamak için Müzik klasörüne aktarın veya paylaşın). Emülatör sesi kapalıydı (-noaudio); test dosyanın oluşup listelendiğini doğrular, ses kalitesini doğrulamaz. |
| 12. Kaynak kodu | `Galaxy_S25_Offline_Ecosystem/06_Media/103_VoiceRecorder/` |
| 13. APK gerçek konumu | [galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557) → `com.galaxyoffline.voicerecorder-1.0.0.apk` |

## 20. Zamanlayıcı (katalog no 11)

| Alan | Değer |
|---|---|
| 1. Uygulama adı | Zamanlayıcı |
| 2. Paket adı | `com.galaxyoffline.timer` |
| 3. Sürüm | 1.0.0 (1) |
| 4. APK dosya adı | `com.galaxyoffline.timer-1.0.0.apk` |
| 5. APK boyutu | 2.34 MB |
| 6. Minimum Android | 12 (API 31) · hedef Android 16 (API 36) |
| 7. İzinler | `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM`, `POST_NOTIFICATIONS`, `USE_FULL_SCREEN_INTENT`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `VIBRATE`, `WAKE_LOCK` |
| 8. Çevrimdışı çalışma | Tam (INTERNET izni yok, CI ile doğrulandı) |
| 9. Kütüphaneler | Kotlin 2.2.0, Jetpack Compose (BOM 2025.07.00), Material 3, Navigation Compose 2.9.2, Lifecycle 2.9.2, Room 2.7.2, DataStore 1.1.7, AndroidX Core 1.16.0 |
| 10. Test sonucu | Birim testleri geçti (TimerEngineTest); cihaz testi 2/2 geçti; soğuk açılış 835 ms; çökme yok |
| 11. Bilinen kısıtlamalar | Aynı anda tek zamanlayıcı çalışır. Telefon yeniden başlatılırsa süren zamanlayıcı iptal olur (uygulama açılınca süre dolmuşsa uyarı verilir). Uyarı sesi 2 dakika sonra kendiliğinden susar. Süre dolunca çalan uyarı (tam ekran, ses) otomatik testte beklenmedi; zamanlama mantığı TimerEngineTest ile doğrulandı. |
| 12. Kaynak kodu | `Galaxy_S25_Offline_Ecosystem/01_Daily_Life/11_Timer/` |
| 13. APK gerçek konumu | [galaxy-release-apks](https://github.com/emeklituran/fpp/actions/runs/42480565013/artifacts/13617805557) → `com.galaxyoffline.timer-1.0.0.apk` |
