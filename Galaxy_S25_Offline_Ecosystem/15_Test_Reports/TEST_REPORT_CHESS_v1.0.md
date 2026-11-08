# Test raporu – Satranç v1.0 (`com.galaxyoffline.chess`)

Ortam: GitHub Actions `ubuntu-latest`, JDK 17, Gradle 8.14.3 · Emülatör: Android 15 (API 35), x86_64, 1080×2400, yazılımsal GPU, 2 çekirdek
Kaynak çalıştırma: CI #26 [https://github.com/emeklituran/fpp/actions/runs/42506178532](https://github.com/emeklituran/fpp/actions/runs/42506178532) (commit `4e833b3`)
Motor: Stockfish 19 (`sf_19`, commit `edb0d9d`), ağ `nn-1a298aa575a0.nnue`, NDK 27.2 ile statik derleme

## Özet
| Kontrol | Sonuç |
|---|---|
| Release APK (yalnızca arm64-v8a) | Oluştu · **79.62 MB** |
| İzinler | **Hiç yok** (INTERNET yok) |
| Reklam/analiz/telemetri SDK | Yok (`verifyNoTrackers` + APK dex taraması) |
| Stockfish ikilisi SHA-256 doğrulaması (`verifyStockfish`, derleme öncesi) | Geçti |
| Stockfish bench imzası (2497913) – sunucu, arm64 (qemu), x86_64 | Geçti (chess-engine iş akışı #1 ve CI #24 derlemesi) |
| Kural birimi testleri (rules) | 38/38 geçti |
| Motor oturumu birimi testleri (engine, sahte motor ile) | 13/13 geçti |
| Emülatörde kurulum / soğuk açılış / kapat-aç / çökme | Debug APK ile: TAMAM / 917 ms / 1135 ms / yok |
| Cihaz (instrumentation) testleri – gerçek Stockfish ile | **14/14 geçti** |
| Galaxy S25'te çalıştırma ve performans | **Ölçülmedi** (cihaz erişimim yok) |

Release APK yalnızca arm64-v8a içerdiği için x86_64 emülatöre kurulamaz (INSTALL_FAILED_NO_MATCHING_ABIS). Emülatör testleri aynı kaynak koddan derlenen **debug** APK (arm64 + x86_64) ile yapıldı. `tools/device_tests.sh` bunu raporda açıkça "release yalnızca arm64-v8a → debug APK ile" olarak yazar. Release APK'nın kendisi gerçek bir arm64 cihazda henüz kurulmadı.

## Birim testleri
| Modül | Test | Ne doğrulanır |
|---|---|---|
| rules | PerftTest (11) | Stockfish `perft.sh` konumları + 14 zor konum (rok, geçerken alma, terfi, açmaz). Derin set (başlangıç d7 = 3 195 901 860) ayrıca `-PdeepPerft` ile yerelde geçti (248 sn); CI'da hızlı set çalışır |
| rules | FenTest (4) | Katı FEN ayrıştırma, alan bazında Türkçe hata mesajları, yazma–okuma döngüsü |
| rules | ValidatorTest (3) | Olanaksız konumlar: şah sayısı, piyon/taş sayısı, 1./8. sırada piyon, rok ve geçerken alma tutarlılığı, sırası olmayan tarafın şahta olması, 3+ şah çeken taş |
| rules | EqualityTest (5) | Metin FEN eşitliği ≠ konum eşitliği (yalnızca yasal geçerken alma) ≠ tam durum eşitliği |
| rules | DrawRulesTest (9) | Mat, pat, 5 tekrar ve 150 yarım hamle otomatik; 3 tekrar ve 100 yarım hamle yalnızca talep; mat 75 hamle kuralından önce gelir; KN–KN ve KB–KN otomatik beraberlik **değil**; süre bitiminde mat yapamama ayrı fonksiyon |
| rules | RandomPlayoutTest (1) | 300 rastgele oyunda chesslib ile tutarlılık |
| rules | GameTest (5) | SAN, geri alma, yeniden oynatma, durum |
| engine | UciTest (5) | `info` / `bestmove` ayrıştırma, sınır satırlarının ve `info string`in atlanması |
| engine | EngineSessionTest (8) | Yeni istek eski aramanın sonuçlarını asla iletmez; takılan motorda stop → öldür → yeniden başlat; çöken motor hata bildirir ve sonraki istekte yeniden başlar; 125 ms güncelleme sınırı |

## Cihaz (emülatör) testleri – 14/14
Motor testleri `nativeLibraryDir/libstockfish.so` ikilisini gerçek süreç olarak çalıştırır; sahte motor kullanılmaz.

| Test | Doğrulanan |
|---|---|
| EngineDeviceTest.binaryIsExtractedAndExecutable | İkili kurulumda çıkarılmış ve çalıştırılabilir; motor kendini "Stockfish 19" olarak tanıtır |
| EngineDeviceTest.findsMateInOne | `6k1/5ppp/8/8/8/8/8/R5K1 w` → en iyi hamle `a1a8`, puan "mat 1" |
| EngineDeviceTest.threeLinesFromStart | MultiPV 3: üç ayrı ilk hamleli satır |
| EngineDeviceTest.killedProcessReportsErrorThenRestarts | Analiz sırasında süreç öldürülünce hata bildirilir; sonraki istekte motor yeniden başlar ve doğru sonuç verir |
| ChessFlowTest.foolsMateEndsGame | f3 e5 g4 Vh4# → "Mat – Siyah kazandı" |
| ChessFlowTest.illegalTapDoesNotMove | Kuraldışı hedefe dokunmak konumu değiştirmez |
| ChessFlowTest.promotionToKnight | a7–a8 terfi penceresi, at seçimi → FEN `N7/7k/…` |
| ChessFlowTest.invalidFenShowsErrorAndKeepsGame | Hatalı FEN hata gösterir, oyun değişmez; yalnız şahlar konumu otomatik "Berabere – ölü pozisyon" |
| ChessFlowTest.fenRoundTripAndPersistenceAfterRecreate | 1.e4 c5 sonrası FEN tam olarak doğru; etkinlik yeniden oluşturulunca aynı oyun geri gelir |
| ChessFlowTest.liveAnalysisThreeLinesAndUpdatesAfterMove | Analiz modunda 3 satır gelir; hamleden sonra yeni konum için yeniden hesaplanır |
| ChessFlowTest.variationPreviewDoesNotChangeGame | Varyant önizlemesi ilerletilip kapatılınca oyunun FEN'i değişmez |
| ChessFlowTest.computerReplies | Bilgisayara karşı modda 1.e4 sonrası bilgisayar hamle yapar (sıra yeniden beyazda, 2. hamle) |
| ChessFlowTest.editorRejectsIllegalAndAcceptsLegal | Boş tahta (şah yok) reddedilir ve sorunlar listelenir; K+V–K konumu kabul edilip analize açılır |
| ChessFlowTest.licensesScreenShowsGpl | Ayarlar → Açık kaynak lisansları: GPL-3 tam metni ve Stockfish bildirimi görünür |

Bu sonuca ulaşmadan önceki CI çalıştırmalarında düzeltilenler (hepsi bu raporda görünür kalsın diye):
- CI #23: derleme hatası (modüller arası akıllı dönüşüm); kod düzeltildi.
- CI #24: 14 testten 3'ü kaldı:
  - 2'si benim test beklentim yanlıştı:
    - `assertTextContains` varsayılan olarak tam metni karşılaştırır.
    - Oyun FEN'i, standart gösterime uygun olarak iki kare piyon hamlesinden sonra `c6` karesini yazar.
  - 1'i düzenleyici testindeki zamanlama sorunuydu.
- CI #25: düzenleyici testi gezinme sırasında "Temizle"ye dokundu. Tıklama kaybolunca geçerli konum analize gönderildi. Test artık boş tahta FEN'ini bekliyor. Uygulama kodunda değişiklik gerekmedi; boş tahtanın reddedildiği birim testinde zaten doğrulanıyordu.

## Kabul ölçütleri (v1.0)
| # | Ölçüt | Durum | Kanıt |
|---|---|---|---|
| 1 | Kurallar doğru (perft, özel hamleler) | Geçti | PerftTest, RandomPlayoutTest |
| 2 | Katı FEN içe/dışa aktarma, alan bazında hata | Geçti | FenTest, invalidFen… cihaz testi |
| 3 | Olanaksız konumların reddi (düzenleyici dahil) | Geçti | ValidatorTest, editor… cihaz testi |
| 4 | FIDE otomatik/talep ayrımı, yanlış otomatik beraberlik yok | Geçti | DrawRulesTest |
| 5 | Üç düzeyli eşitlik | Geçti | EqualityTest |
| 6 | Gerçek motorla analiz, 3 satır | Geçti (emülatör) | threeLinesFromStart, liveAnalysis… |
| 7 | Hamleden sonra eski sonuç gösterilmez | Geçti | EngineSessionTest (nesil numarası) + liveAnalysis… |
| 8 | Motor takılır/çökerse kurtarma | Geçti | EngineSessionTest (sahte), killedProcess… (gerçek) |
| 9 | Varyant önizleme oyunu değiştirmez | Geçti | variationPreview… |
| 10 | Bilgisayara karşı ve iki kişilik oyun | Geçti | computerReplies, foolsMate… |
| 11 | Uygulama yeniden açılınca oyun korunur | Geçti | fenRoundTripAndPersistenceAfterRecreate |
| 12 | İzin yok, INTERNET yok, takipçi yok | Geçti | APK denetimi |
| 13 | S25'te performans (ilk sonuç hedefi 300 ms, nps, iş parçacığı ölçeklemesi) | **Ölçülmedi** | Uygulamadaki "Motor testi" ekranı ile kullanıcı tarafından yapılmalı |
| 14 | GPL-3 yükümlülükleri: lisans metni, bildirimler, karşılık gelen kaynak | Geçti | licenses… cihaz testi; kaynak paketi APK'larla aynı artefaktta |

## Test edilmeyenler ve dürüst notlar
- **Galaxy S25'te hiç çalıştırılmadı.** Bütün cihaz testleri x86_64 emülatörde debug APK ile yapıldı. arm64 ikilisi yalnızca qemu altında bench imzasıyla doğrulandı.
- **Performans ölçülmedi.** "300 ms'de ilk sonuç" bir hedeftir, ölçülmüş bir değer değildir. Emülatör (2 çekirdek, yazılımsal) gerçek hız hakkında bilgi vermez. İş parçacığı ve hash varsayılanları (çekirdek−1, 64 MB) S25 ölçümü gelene kadar geçicidir.
- Mat bulma yalnızca mat-1 konumunda otomatik test edildi. Mat-2 ve mat-3 konumları "Motor testi" ekranında var ama otomatik testte çalıştırılmadı. Motorun her matı bulacağı garanti edilmez.
- Uygulama arka plana geçince motorun durması (60 sn sonra süreç kapatılır) otomatik test edilmedi.
- Kayıtlı oyunlar ekranı (Room) cihaz testinde kullanılmadı. Kalıcılık testi etkin oyun deposunu doğrular.
- Bilgisayar seviyeleri (Elo sınırlaması) güç açısından doğrulanmadı. Yalnızca hamle yapıldığı test edildi.
- Koyu/AMOLED tema ve küçük ekran düzeni görsel olarak elle incelenmedi.
- Cihazda kaplanan alan ölçülmedi. Hesap: APK 79.62 MB + çıkarılmış motor ≈103 MB ⇒ yaklaşık 185 MB.
