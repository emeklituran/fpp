# Finans Asistanı (Android)

Depodaki Streamlit uygulamasının (`app.py`) **tamamen çevrimdışı**, reklamsız ve izinsiz Android sürümü.
Kotlin + Jetpack Compose + Material 3 ile yazılmıştır; Samsung Galaxy S25 (Android 15/16) hedeflenmiştir.

## Gizlilik ve güvenlik

| Özellik | Durum |
|---|---|
| İnternet izni (`INTERNET`) | **Yok** – manifestte `tools:node="remove"` ile ayrıca engellenir |
| Herhangi bir izin | **Yok** – CI her derlemede `aapt2 dump permissions` ile doğrular |
| Reklam / analiz / telemetri SDK'sı | **Yok** |
| Google Play Hizmetleri | Gerekmez |
| Hesap / üyelik / giriş | Yok |
| Veri saklama | Cihazda: Room (SQLite) + DataStore |
| Bulut yedeği | Kapalı (`allowBackup=false`, `data_extraction_rules.xml`) |
| Elle yedek | JSON dışa/içe aktarma (Storage Access Framework, izin gerektirmez) |

## Özellikler

- **Özet**: Net kullanılabilir fazla, gelir dağılımı çubuğu (sabit gider / asgari ödeme / birikim / ekstra güç),
  toplam borç, kart limitleri ve uygulanan asgari oranı, en büyük 5 borç, hızlı ekleme düğmeleri.
- **Kayıtlar**: Gelir, Borç, Gider, Birikim sekmeleri. Dokununca düzenleme, menüden silme, silmede **Geri al**.
  - Gelir: maaş (yılda 0/1/2/4 zam + zam oranı), düzenli zamsız gelir, tek seferlik gelir.
  - Borç: kredi kartı (limit → %20 / %40 asgari), tüketici/konut kredisi, KMH (asgari = aylık faiz),
    sabit taksitli borç (taksit × adet, ilk ödeme tarihi), diğer.
  - Birikim: sabit tutar veya net fazlanın yüzdesi.
- **Plan (simülasyon)**: Çığ / Kartopu yöntemi, borçsuz tarih, toplam faiz ve ödeme, iki yöntemin karşılaştırması,
  borç azalış grafiği (harici kütüphane yok), borçların kapanma sırası, ay ay ödeme tablosu.
  Veriler simülasyondan sonra değişirse uyarı gösterilir.
- **Ayarlar**: Sistem/aydınlık/karanlık tema, Material You dinamik renk, yedek al/geri yükle, tüm verileri sil,
  yerel hata kaydı görüntüleme.
- **Yardım**: Kullanım kılavuzu ve hesaplama varsayımları.

### Python sürümüne göre düzeltilen hesaplama hataları
- Tek seferlik gelir artık her ay tekrar eden gelir gibi sayılmıyor; ilk ay borca aktarılıyor.
- Hedef borç kapandığında artan ekstra para bir sonraki borca devrediliyor (gerçek kartopu etkisi).
- Henüz başlamamış taksitli borç hedef seçilip ekstra paranın boşa gitmesi engellendi.
- Tüm birikim hedefleri hesaba katılıyor (Python yalnızca ilkini kullanıyordu).
- Simülasyon 600 ayla sınırlı; sonsuz döngü veya çökme olmaz.

## Proje yapısı

```
FinansAsistani/
├── core/   Saf Kotlin hesaplama motoru (Android bağımsız, JUnit testli)
│   └── Models, FinanceCalculator, DebtSimulator, Money (TR biçimlendirme/ayrıştırma)
└── app/    Android uygulaması
    ├── data/        Room varlıkları/DAO, depo, DataStore ayarları, JSON yedekleme
    ├── viewmodel/   Finance, Simulation, Settings ViewModel'ları
    ├── ui/          Tema, bileşenler, ekranlar, düzenleyiciler, gezinme
    └── util/        Güvenli coroutine başlatıcı, mesaj veriyolu, yerel hata kaydı
```

Teknik: minSdk 26 (Android 8.0), target/compileSdk 36, AGP 8.11.1, Kotlin 2.2.0, Compose BOM 2025.07.00,
Room 2.7.2, DataStore 1.1.7, Navigation 2.9.2. R8 küçültme ve kaynak daraltma açık.

Çökme önleme: tüm veritabanı/dosya işlemleri `CoroutineExceptionHandler` ile sarılı, hatalar Snackbar ile gösterilir;
girişler doğrulanır (Türkçe `1.500,75` ve `1500.75` biçimleri), yedek dosyaları boyut/biçim kontrolünden geçer,
bozuk veri değerleri hesaplamada 0'a çekilir.

## APK oluşturma

### A) Android Studio ile
1. Android Studio'yu (Narwhal 2025.1 veya daha yeni) açın → **Open** → `apps/FinansAsistani` klasörünü seçin.
2. Gradle eşitlemesi bitince (ilk seferde bağımlılıklar indirilir) üst menüden
   **Build → Generate App Bundles or APKs → Generate APKs**.
3. APK: `app/build/outputs/apk/release/app-release.apk` (veya debug için `.../debug/app-debug.apk`).
4. Kalıcı imza için: **Build → Generate Signed App Bundle or APK → APK → Create new...** ile bir `.jks` oluşturun,
   ya da `keystore.properties.example` dosyasını `keystore.properties` olarak kopyalayıp doldurun.
   İmza anahtarınızı saklayın: güncellemeler aynı anahtarla imzalanmalıdır.

### B) Komut satırı
```bash
cd apps/FinansAsistani
./gradlew :core:test :app:assembleRelease
```

### C) GitHub Actions (bilgisayar gerekmez)
Her push'ta `.github/workflows/finans-asistani-apk.yml` çalışır: testleri koşar, APK'yı derler, APK'da hiç izin
olmadığını doğrular ve APK'yı **Artifacts** olarak yükler (Actions → ilgili çalışma → *FinansAsistani-...*).
Kalıcı imza için depo gizli anahtarları ekleyin: `KEYSTORE_BASE64` (`base64 -w0 anahtar.jks`),
`KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

### Telefona kurulum (Galaxy S25)
1. APK'yı telefona kopyalayın (USB, Quick Share vb.).
2. **Dosyalarım** ile APK'ya dokunun → istenirse *Bu kaynaktan yüklemeye izin ver*'i açın → **Yükle**.
3. **Otomatik Engelleyici** açıksa (Ayarlar → Güvenlik ve gizlilik → Otomatik Engelleyici) yüklemeden önce geçici olarak kapatın.
