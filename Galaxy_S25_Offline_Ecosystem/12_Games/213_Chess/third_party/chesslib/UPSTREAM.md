# chesslib (üçüncü taraf, değiştirilmemiş kaynak)

- Kaynak: https://github.com/bhlangonijr/chesslib
- Sürüm: **1.3.7** – etiket commit'i `2e9b4e1a71a84d4fe147e762cf39c82a8cd4c251`
- Lisans: Apache-2.0 (`LICENSE`)
- Kapsam: yalnızca `src/main` (testler dahil edilmedi); **hiçbir dosya değiştirilmedi**.
- Neden depoya alındı: chesslib yalnızca JitPack'ten dağıtılıyor. JitPack eklemek ortak depo ayarını
  (`FAIL_ON_PROJECT_REPOS`) değiştirmeyi gerektirir ve derlemeyi üçüncü tarafın sunucuda ürettiği bir ikili dosyaya bağlar.
- Bağımlılık: `org.apache.commons:commons-lang3:3.12.0` (Maven Central, Apache-2.0).
- Değişmediğinin doğrulanması: `git -C <chesslib> checkout 1.3.7 && diff -r src/main third_party/chesslib/src/main`
