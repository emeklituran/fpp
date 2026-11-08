# Derleme ve kurulum

## Android Studio
1. Android Studio (Narwhal 2025.1 veya yenisi) → **Open** → `Galaxy_S25_Offline_Ecosystem` klasörü.
2. Gradle eşitlemesi bittikten sonra çalıştırma yapılandırmasından bir uygulama modülü seçin (ör. `smartcalculator`).
3. APK: **Build → Generate App Bundles or APKs → Generate APKs**. Çıktı: `<uygulama klasörü>/build/outputs/apk/release/`.

## Komut satırı
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew test verifyNoTrackers assembleRelease     # tüm uygulamalar
./gradlew :notes:assembleRelease                    # tek uygulama
```

## Kalıcı imza anahtarı (güncellemeler için önemli)
Anahtar tanımlanmazsa APK'lar debug anahtarıyla imzalanır ve kurulabilir; ancak farklı bilgisayarda/CI çalıştırmasında
derlenen yeni sürüm eskisinin üzerine **güncelleme olarak kurulamaz**. Bir kez anahtar oluşturun:
```bash
keytool -genkeypair -v -keystore galaxy-offline.jks -alias galaxyoffline -keyalg RSA -keysize 4096 -validity 10000
cp keystore.properties.example keystore.properties   # storeFile=galaxy-offline.jks ve şifreleri yazın
```
GitHub Actions için depo gizli anahtarları: `KEYSTORE_BASE64` (`base64 -w0 galaxy-offline.jks`), `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD`.

## Galaxy S25'e kurulum
1. APK'yı telefona aktarın (USB, Quick Share).
2. Dosyalarım → APK → *Bu kaynaktan yüklemeye izin ver* → **Yükle**.
3. **Otomatik Engelleyici** açıksa (Ayarlar → Güvenlik ve gizlilik) yükleme sırasında geçici olarak kapatın.
4. Alarm/Hatırlatıcı için: Ayarlar → Pil → Arka plan kullanım sınırları → uygulamayı "Hiçbir zaman uyutulmayan
   uygulamalar" listesine ekleyin.
