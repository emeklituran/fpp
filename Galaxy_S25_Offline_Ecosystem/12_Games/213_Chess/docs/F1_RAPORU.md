# Satranç – Faz 1 raporu: depo incelemesi, kurallar kütüphanesi, Stockfish CI hazırlığı

Tarih: 2026-11-08 · Kapsam: kod yazımı yok; inceleme, değerlendirme ve derleme altyapısı.

## 1. Depo ve ortak altyapı incelemesi

| Konu | Bulgu | Satranç uygulamasına etkisi |
|---|---|---|
| Depo çözümleme | `settings.gradle.kts`: `FAIL_ON_PROJECT_REPOS`; depolar: Google, Maven Central aynası, Maven Central | Proje bazında depo eklenemez. JitPack kullanmak ortak dosyayı değiştirmek demektir → kaçınıldı (bkz. §2) |
| Ortak kurallar | `galaxy.android.application` (R8, imza, `verifyNoTrackers`, yalnızca `tr` kaynakları), `galaxy.jvm.library` (JVM 17), lint `abortOnError` | Satranç modülleri bunları değiştirmeden kullanacak. Yerel kütüphane paketleme ayarı yalnızca satranç modülünün kendi `build.gradle.kts` dosyasında yapılacak |
| İzin denetimi | `tools/audit_apks.sh`: `*/permissions.allow` bulunan her modülün release APK'sı | Satranç: boş izin listesi |
| Emülatör testi | `tools/device_tests.sh`: release APK x86_64 emülatöre kurulur | **Sorun:** release yalnızca arm64 yerel kod içereceği için x86_64 emülatöre kurulamaz (`INSTALL_FAILED_NO_MATCHING_ABIS`). Faz 2'de bu betiğe yalnızca bu durumu tanıyan küçük bir istisna eklenmesi gerekecek; değişiklik ayrıca bildirilecek. Önceki planda "release emülatörde motorsuz açılır" demiştim; bu yanlıştı, düzeltildi |
| CI | Ana iş akışı tüm modülleri derler | Stockfish derlemesi ana iş akışına eklenmedi; ayrı `chess-engine.yml` iş akışı kuruldu |

## 2. Kurallar kütüphanesi: chesslib değerlendirmesi

**Doğrulanan bilgiler (kaynak koddan, tahmin değil):**
- Lisans: Apache-2.0 (`LICENSE`), GPL-3 ile birleştirilebilir.
- Güncel sürüm: **1.3.7** (etiket commit'i `2e9b4e1a`, 30 Haziran 2026); ana dalda bu sürümden sonra 10 commit var.
- Dağıtım kanalı: **yalnızca JitPack.** Maven Central'da (`com.github.bhlangonijr…` ve `io.github.bhlangonijr…`) yok (404). JitPack bu ortamdan erişilemiyor.
- Bağımlılık: `commons-lang3` 3.12.0 (Apache-2.0, Maven Central'da).
- 49 Java dosyası; Java 11 hedefli.

**Ölçülen sonuçlar** (yerel JVM, chesslib 1.3.7 kaynağından derlenmiş; test kodu `docs/f1-eval/`):

| Test | Kaynak | Sonuç |
|---|---|---|
| Hızlı perft: başlangıç d1–5, Kiwipete d1–4, konum 3 d1–6, konum 4 ve aynası d1–4, konum 5 d1–4, konum 6 d1–4 | Chess Programming Wiki | **31/31 eşleşti** |
| Derin perft: Stockfish `tests/perft.sh` standart konumlarının tamamı (başlangıç d7 = 3.195.901.860 dahil, toplam ≈4,66 milyar düğüm) | Stockfish sf_19 | **8/8 eşleşti** |
| Zor durumlar (geçerken alma açmazları, rok şah çeker, alt terfi, kendini pat vb.) | Martin Sedlak listesi (değerler bellekten yazıldı) | **14/14 eşleşti** – iki bağımsız kaynağın uyumu; CI'da Stockfish ile ayrıca doğrulanacak |
| SAN: terfi, alt terfi, ayrım, rok, geçerken alma, şah/mat işareti | – | Doğru |
| Hız | – | ≈20 milyon düğüm/sn (JVM, tek çekirdek) |

**Tespit edilen sorunlar ve kararlar:**

| Sorun | Ölçüm | Karar |
|---|---|---|
| `isInsufficientMaterial()` | Şah+at – şah+at ve şah+fil – şah+at için "yetersiz" diyor; bu konumlarda mat mümkün (FIDE'ye göre ölü pozisyon değil). Şah+iki at – şah için doğru (yetersiz demiyor) | **Kullanılmayacak.** Ölü pozisyon (otomatik beraberlik) ve süre bitiminde "mat edemez" kontrolü ayrı ayrı bizim katmanımızda yazılacak; yalnızca kesin durumlar otomatik |
| `isDraw()` | 100 yarım hamleyi (50 hamle) ve 3 tekrarı otomatik beraberlik sayıyor | **Kullanılmayacak.** Otomatik: 150 yarım hamle (75 hamle; son hamle matsa mat geçerli), 5 tekrar, pat, ölü pozisyon. Talep: 100 yarım hamle, 3 tekrar |
| `loadFromFen()` doğrulama yapmıyor | Hamle sırası `x`, şahsız tahta, eksik alanlar, 9 kareli sıra kabul ediliyor; bozuk metin istisna fırlatıyor | **Kendi katı FEN çözümleyicimiz** (alan alan hata mesajı) + konum doğrulayıcı; chesslib'e yalnızca doğrulanmış FEN verilecek |
| Tekrar tespiti ve geçerken alma | Yakalanamayan geçerken alma karesi tekrarı bozmadı (doğru) | Yine de FIDE tanımlı kendi konum anahtarımız yazılıp chesslib ile çapraz test edilecek |

**Dağıtım kararı:** JitPack eklemek ortak depo ayarını değiştirmeyi ve derlemeyi üçüncü tarafın sunucuda ürettiği bir ikili dosyaya bağlamayı gerektirir. Bunun yerine chesslib **1.3.7 etiketinin kaynağı değiştirilmeden** `213_Chess/third_party/chesslib` altına alınacak (Apache-2.0 izin verir; LICENSE korunur, commit özeti kayıtlı). `commons-lang3` mevcut Maven Central deposundan gelir. Yeni depo eklenmez.

**Sonuç:** chesslib hamle üretimi, SAN ve özetleme için **kabul edildi**. FEN çözümleme, beraberlik kuralları ve konum kimliği bizim katmanımızda.

## 3. GPL-3 lisans doğrulaması

Varsayım yapılmadı: Stockfish'in ayrı süreçte çalışması yükümlülükleri ortadan kaldırmaz. Yükümlülükler dağıtılan ikili dosyaya (APK içindeki `libstockfish.so`) bağlıdır.

- **Kararınız:** `213_Chess` kodunun tamamı GPL-3 (`LICENSE` = GPL-3 tam metni, Stockfish'in `Copying.txt` dosyasıyla birebir aynı). Böylece "ayrı program mı, birleşik eser mi" sorusu belirleyici olmaktan çıkar; her iki durumda da GPL-3 koşulları sağlanır.
- **Nesne kodu dağıtımı (GPL-3 §6):** APK ile birlikte, aynı yerden, karşılık gelen kaynak sunulacak. Derleme betiği her derlemede `stockfish-sf_19-corresponding-source.tar.gz` üretir: Stockfish kaynağı (aynı commit) + NNUE ağ dosyası + derlemeyi yöneten betikler (§1'deki "derlemeyi kontrol eden betikler" gereği). Uygulamanın kendi kaynağı zaten bu depodadır.
- **Kurulum bilgisi (§6, tüketici ürünü):** Kullanıcı kaynaktan kendi APK'sını derleyip kendi anahtarıyla kurabilir; adımlar `BUILD_AND_INSTALL.md` dosyasında yazılı ve satranç için genişletilecek. Ek kısıtlama (DRM vb.) yok.
- **Bildirimler:** Uygulamada GPL-3 tam metni, Stockfish telif bildirimi, Apache-2.0 metinleri ve `commons-lang3` NOTICE dosyası gösterilecek (`THIRD_PARTY_NOTICES.md`).
- **Lisans uyumu:** Apache-2.0 (chesslib, commons-lang3, AndroidX/Compose, Kotlin) → GPL-3 ile uyumlu. İleride eklenecek Lichess açılış verisi CC0 (kamu malı).
- **Diğer uygulamalar:** Ayrı APK'lar, Stockfish koduyla bağlantıları yok → etkilenmez.
- **Ad kullanımı:** Uygulama adı "Stockfish" içermeyecek; yalnızca "motor: Stockfish 19" atfı.
- Bu bir hukuki görüş değildir.

## 4. Stockfish CI hazırlığı

- `engine/stockfish.env`: sürüm sabitleme (etiket, commit, bench imzası, ağ adı, NDK sürümü).
- `engine/build_stockfish.sh`:
  1. Kaynak alınır, commit doğrulanır.
  2. Ağ indirilir, SHA-256 önekiyle doğrulanır.
  3. Üç derleme yapılır: Linux sunucu (referans), Android arm64 (`arm64-universal`: armv8 + armv8-dotprod, çalışma anında seçim; statik), Android x86_64 (yalnızca debug).
  4. Bench imzaları beklenen değerle karşılaştırılır. arm64 ikilisi qemu altında çalıştırılır; bu yöntemi Stockfish'in kendi CI'ı da kullanıyor.
  5. Stockfish'in `perft.sh` testi ve bizim zor durumlar setimiz çalıştırılır.
  6. Paketleme: SHA256SUMS, BUILDINFO, karşılık gelen kaynak.
- `.github/workflows/chess-engine.yml`: ayrı iş akışı; yalnızca `213_Chess/engine/**` değişince çalışır.

**CI sonucu – Chess Engine #1 (başarılı):** https://github.com/emeklituran/fpp/actions/runs/42495424789
Artefakt: `stockfish-android` (ikili dosyalar, SHA256SUMS, BUILDINFO, karşılık gelen kaynak paketi).

| Kontrol | Sonuç |
|---|---|
| Kaynak commit doğrulaması | `edb0d9db…` eşleşti |
| NNUE ağı | `nn-1a298aa575a0.nnue`, SHA-256 `1a298aa575a0…` (önek eşleşti) |
| NDK | 27.2.12479018 (r27c), statik bağlantı |
| Bench imzası (beklenen 2497913) | Linux sunucu **2497913** · Android arm64 (qemu altında) **2497913** · Android x86_64 **2497913** |
| Stockfish `tests/perft.sh` (standart + Chess960, başlangıç d7 dahil) | Tümü OK |
| Zor durumlar perft seti (14 konum) | **14/14** Stockfish ile eşleşti; bellekten yazdığım değerler artık Stockfish ile de doğrulandı |
| İkili dosya boyutu | arm64 **103,4 MB**, x86_64 **102,5 MB** (ağ gömülü; ağın kendisi ≈100 MB) |

Not: CI, ikili dosyaların Android'de **çalıştığını** değil, aynı makine kodunun doğru sonuç ürettiğini doğrular. arm64 ikilisi qemu ile, x86_64 ikilisi doğrudan çalıştırıldı. Gerçek Android sürecinde çalışma Faz 2'de emülatörde (x86_64) ve S25'te (arm64) doğrulanacak.

## 5. Ortaya çıkan karar: boyut ve motorun çalıştırılma biçimi

Ağ dosyası tek başına yaklaşık 100 MB tutuyor. Bu, önceki planda tahmin ettiğim "50–90 MB APK" değerini aşıyor. CI artefaktındaki sıkıştırma oranına göre ağ yalnızca yaklaşık %20 sıkışıyor.

| Seçenek | APK indirme (tahmin) | Cihazda kaplanan alan (tahmin) | Artı / eksi |
|---|---|---|---|
| **A. Ayrı süreç** (önerilen, mevcut plan) | ≈85 MB | ≈190 MB (APK + çalıştırılabilir dosyanın çıkarılmış kopyası) | Motor çökerse uygulama ayakta kalır; zaman aşımında süreç öldürülüp yeniden başlatılabilir |
| B. Aynı süreçte (JNI) | ≈100 MB | ≈100 MB (sıkıştırılmadan APK içinde, çıkarılmaz) | Yarı alan; motor hatası uygulamayı çökertir; Stockfish'in iç değişikliklerini gerektirir |
| C. Ağ ayrı dosya (varlık) + küçük motor | ≈85 MB | ≈190 MB (ağ ilk açılışta kopyalanır) | A'ya göre kazanç yok, ilk açılış yavaş |

Önerim **A**: güvenilirlik hedefine en uygun seçenek; S25'in depolama alanı için 190 MB kabul edilebilir. Debug APK (arm64 + x86_64) yaklaşık iki kat büyük olacak; yalnızca CI ve emülatör içindir.

## 6. Ana CI'a etkisi

Faz 1'de ana iş akışına ve ortak dosyalara hiçbir değişiklik yapılmadı. Bu commit ile ana CI (#20) da çalıştı:
- Derleme ve izin denetimi: geçti.
- Alışkanlık Takipçisi cihaz testi: başarısız. Neden: testteki bir yarış durumu ("Kitap oku" metni açık formda da bulunduğu için bekleme erken bitiyordu). Bu commit'le ilgisi yok; aynı kod CI #19'da geçmişti.
- Test düzeltildi (`5c9d574`): artık listedeki satır bekleniyor. **Doğrulandı:** CI #21 (https://github.com/emeklituran/fpp/actions/runs/42496382486) – 20 uygulamanın tamamı derleme, denetim ve emülatör testlerinden geçti.

## 7. Sonraki adım (Faz 2)

1. chesslib 1.3.7 kaynağı değiştirilmeden `third_party/chesslib` altına alınır.
2. `rules` modülü yazılır:
   - Katı FEN çözümleyici ve doğrulayıcı.
   - Üç düzeyli eşitlik (metinsel, konum kimliği, tam oyun durumu).
   - Beraberlik kuralları: ölü pozisyon ile süre bitiminde "mat edemez" ayrı; 150 yarım hamle; 5 tekrar; talepler.
3. `docs/f1-eval` testleri modül testlerine dönüştürülür. Hızlı perft seti her derlemede çalışır; derin set ayrı.
4. Motor entegrasyonundan önce §5'teki boyut kararının onaylanması gerekiyor.


## 8. Faz 2 ilk adımı (tamamlandı): chesslib ve `rules` modülü

- `third_party/chesslib`: 1.3.7 kaynağı, değiştirilmeden (`diff -r` ile birebir doğrulandı).
- `rules` modülü (saf Kotlin, JVM):
  - Katı FEN çözümleyici: 6 alan, alan bazında Türkçe hata mesajı.
  - Konum doğrulayıcı: şah sayısı, 1./8. sırada piyon, taş ve piyon sayıları, terfiyle açıklanabilirlik, rok tutarlılığı, geçerken alma tutarlılığı, sırası olmayan tarafın şah altında olmaması, en fazla iki şah çeken taş.
  - Üç eşitlik düzeyi: metinsel FEN, FIDE konum kimliği (yasal geçerken alma ile), tam oyun durumu.
  - Beraberlik kuralları:
    - Otomatik: mat, pat, 5 tekrar, 150 yarım hamle (son hamle matsa mat geçerli), kesin ölü pozisyonlar.
    - Talep: 3 tekrar, 100 yarım hamle.
    - Süre bitiminde "mat edemez" ayrı fonksiyon.
- Ortak dosyalarda yalnızca ekleme yapıldı: `settings.gradle.kts` (2 modül satırı), `libs.versions.toml` (commons-lang3).

**Testler:**

| Test sınıfı | Test sayısı | Yerel | CI #22 |
|---|---|---|---|
| PerftTest (hızlı set, zor durumlar, oyun katmanı tutarlılığı) | 11 (+1 derin) | Geçti (3,4 sn) | Geçti |
| PerftTest derin set (≈4,66 milyar düğüm) | 1 | Geçti (248 sn) | Ayrı çalıştırılacak (her derlemede değil) |
| FenTest | 4 | Geçti | Geçti |
| ValidatorTest | 3 (≈30 konum) | Geçti | Geçti |
| EqualityTest | 5 | Geçti | Geçti |
| DrawRulesTest | 9 | Geçti | Geçti |
| RandomPlayoutTest (300 rastgele oyun, >30.000 konum) | 1 | Geçti | Geçti |

- CI #22: https://github.com/emeklituran/fpp/actions/runs/42497536624
  - Ana iş akışının tamamı başarılı: birim testleri (`./gradlew test`, `:chess-rules:test` dahil), derleme, denetim ve 20 uygulamanın emülatör testleri.
  - Diğer uygulamalar etkilenmedi.
- **Not:** Yerel testlerde iki hata çıktı; ikisi de benim test kodumdaki yanlış beklentiydi (sıra numarası ve şah altında olan bir "yasal" örnek konum). Kod doğru davranıyordu; testler düzeltildi.
