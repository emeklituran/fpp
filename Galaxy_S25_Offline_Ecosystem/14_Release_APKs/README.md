# 14_Release_APKs – APK teslim konumu

APK'lar bu ortamda yerel olarak derlenemiyor (Android SDK indirme sunucusu engelli). Bu yüzden derleme **GitHub Actions** üzerinde yapılıyor: `.github/workflows/galaxy-ecosystem.yml`.

APK dosyaları bu klasöre konmaz, iki nedenle:
- İkili dosyalar git deposunu şişirir.
- İmza anahtarı her çalıştırmada değişebilir.

## APK'ları indirme
1. GitHub → **Actions** → **Galaxy Offline Ecosystem** → en son başarılı çalıştırma.
2. Sayfanın altındaki **Artifacts** bölümünden `galaxy-release-apks` dosyasını indirin.
3. Zip içinde şunlar var:
   - Her uygulama için `<paket>-<sürüm>.apk` (örn. `com.galaxyoffline.notes-1.0.0.apk`)
   - `SHA256SUMS.txt` (bütünlük kontrolü)
   - `audit.md` (izin ve güvenlik denetimi)

GitHub, Actions dosyalarını 90 gün saklar. Kalıcı arşiv için indirdiğiniz APK'ları kendiniz saklayın veya bir GitHub Release oluşturun.

## Bütünlük kontrolü
```bash
sha256sum -c SHA256SUMS.txt
```

## İmza
- Depoya `KEYSTORE_*` gizli anahtarları tanımlanmamışsa APK'lar CI'ın geçici debug anahtarıyla imzalanır.
- Bu APK'lar kurulabilir.
- Ancak sonraki bir CI çalıştırmasından gelen sürüm, eskisinin üzerine **güncelleme olarak kurulamaz**: önce eski sürümü kaldırmanız gerekir (veriler silinir).
- Kalıcı anahtar kurulumu için `00_Project_Documentation/BUILD_AND_INSTALL.md` dosyasına bakın.

Teslim edilen sürümlerin ayrıntılı listesi: `DELIVERY_REPORT_01-10.md`.
