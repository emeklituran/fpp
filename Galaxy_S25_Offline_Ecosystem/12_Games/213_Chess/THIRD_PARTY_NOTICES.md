# Üçüncü taraf bileşenler – Satranç (213_Chess)

Bu uygulamanın kendi kaynak kodu **GNU GPL sürüm 3** ile lisanslanmıştır (`LICENSE`).

| Bileşen | Sürüm | Lisans | Kullanım | Durum |
|---|---|---|---|---|
| Stockfish | sf_19 (`edb0d9db`) | GPL-3.0 | Satranç motoru; ayrı süreç olarak UCI ile çalışır | Derleme betiği hazır (`engine/`), uygulamaya henüz eklenmedi |
| Stockfish NNUE ağı | `nn-1a298aa575a0.nnue` | Stockfish ile birlikte dağıtılır | Motorun değerlendirme ağı (ikili dosyaya gömülü) | Derleme betiği hazır |
| chesslib (bhlangonijr) | 1.3.7 (`2e9b4e1a`) | Apache-2.0 | Hamle üretimi, SAN, Zobrist | Değerlendirildi, kabul edildi; kaynağı değiştirilmeden depoya alınacak (Faz 2) |
| Apache Commons Lang | 3.12.0 | Apache-2.0 | chesslib bağımlılığı | Faz 2 |

Apache-2.0 lisanslı bileşenler GPL-3 ile birleştirilebilir. Uygulamanın "Hakkında › Açık kaynak lisansları" ekranında tüm lisans metinleri ve Apache NOTICE dosyaları gösterilecektir.

| cburnett taş seti (Colin M.L. Burnett) | lichess-org/lila `ea908493` | GPLv2+ | Tahtadaki taş görselleri (VectorDrawable'a çevrildi) | Eklendi |
