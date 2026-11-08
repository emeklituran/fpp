# Mimari

## Hedef
Samsung Galaxy S25 için **her biri ayrı APK** olan, reklamsız, internetsiz, ücretsiz ve Türkçe uygulamalar.
Her uygulamanın kendi `applicationId`'si, simgesi, açılış ekranı, ayarları, veritabanı ve sürüm numarası vardır.
Bir uygulamayı kaldırmak diğerlerini etkilemez; uygulamalar çalışma zamanında birbirine bağımlı değildir.

## Tek depo, çok APK
Kod tek bir Gradle projesinde tutulur; ortak kod **derleme zamanında** her APK'nın içine gömülür.
Böylece ortak tasarım ve hata düzeltmeleri tek yerden yapılır ama son kullanıcıya bağımsız APK'lar teslim edilir.

```
Galaxy_S25_Offline_Ecosystem/
├── build-logic/convention/      Gradle kural eklentileri (tüm APK'larda ortak ayarlar)
├── core/
│   ├── common/                  Saf Kotlin: Türkçe sayı/tarih/süre biçimleri (JVM testli)
│   ├── math/                    Saf Kotlin: hesap makinesi ifade motoru ve düzenleyici (JVM testli)
│   ├── designsystem/            Compose: tema (açık/koyu/AMOLED/dinamik), bileşenler,
│   │                            ortak Ayarlar ve Hakkında ekranları, yerel hata kaydı
│   └── calculator/              Hesap makinesi uygulamalarının ortak ViewModel, geçmiş (Room) ve arayüzü
├── 01_Daily_Life/<NN_Uygulama>/ Her klasör bağımsız bir APK modülü
├── ... 12_Games/, 13_App_Center/
├── 14_Release_APKs/             Teslim edilen APK'ların konumu ve sağlama toplamları
├── 15_Test_Reports/             Test ve teslimat raporları
└── tools/                       Katalog, iskelet üretici, APK denetimi, emülatör testleri
```

## Gradle kural eklentileri
| Eklenti | Görev |
|---|---|
| `galaxy.android.application` | compileSdk 36, minSdk 31, targetSdk 36, Java 17, Compose, R8 küçültme + kaynak daraltma, imzalama, yalnızca Türkçe kaynaklar, `dependenciesInfo` kapalı, `verifyNoTrackers` görevi |
| `galaxy.android.library` | Ortak Android kütüphaneleri |
| `galaxy.android.room` | KSP + Room + şema dışa aktarımı (migration için) |
| `galaxy.jvm.library` | Saf Kotlin modülleri + JUnit |

## Uygulama içi katmanlar (MVVM + Repository)
- **UI**: Jetpack Compose + Material 3, Navigation Compose. Ekran durumu `StateFlow` ile ViewModel'dan gelir.
- **ViewModel**: iş mantığı; tüm yazma işlemleri `launchSafe` ile (beklenmeyen hata çökme yerine mesaj olur).
- **Veri**: Room (kayıtlar), DataStore (ayarlar). Her uygulamanın verisi kendi özel klasöründedir.
- **Bağımlılık enjeksiyonu**: Elle (uygulama başına küçük bir `AppContainer`/singleton). Hilt/Dagger gibi
  ek çalışma zamanı bağımlılıkları APK boyutunu ve açılış süresini artıracağından kullanılmadı.

## Neden minSdk 31?
Galaxy S25 Android 15 ile çıktı ve Android 16'ya güncellendi. API 31 (Android 12) tabanı: dinamik renk,
sistem açılış ekranı API'si ve tam zamanlı alarm izin modeli yerleşiktir; son yıllardaki Samsung cihazlarını da kapsar.

## Güvenlik ve gizlilik ilkeleri
- `INTERNET` izni hiçbir uygulamada yok; ayrıca manifest birleştirmede `tools:node="remove"` ile engellenir.
- `allowBackup=false` ve `data_extraction_rules.xml`: veriler buluta yedeklenmez.
- Reklam/analiz/telemetri SDK'ları derlemede `verifyNoTrackers` ile, APK'da dex taramasıyla engellenir.
- Gerekli izinler uygulama klasöründeki `permissions.allow` dosyasında listelenir; listede olmayan izin CI'yı kırar.
- Hata kayıtları yalnızca cihazda tutulur (Hakkında > Son hata kaydı).
