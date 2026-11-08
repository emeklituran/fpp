# Geliştirme standartları (her uygulama için kontrol listesi)

- [ ] Kendi paket adı (`com.galaxyoffline.<ad>`), simgesi (uyarlanabilir + tek renkli tema simgesi), açılış ekranı rengi
- [ ] Ana ekran, gezinme, gerçekten çalışan tüm düğmeler; sahte/boş özellik yok
- [ ] Boş durum ekranları, anlaşılır Türkçe hata mesajları, silmede onay veya geri al
- [ ] Ayarlar (ortak tema bölümü: Sistem / Açık / Koyu / AMOLED + dinamik renk) ve Hakkında ekranı
- [ ] Veriler Room/DataStore ile kalıcı; uygulama kapatılıp açılınca korunur
- [ ] Erişilebilirlik: içerik açıklamaları, en az 48 dp dokunma alanı, yeterli kontrast
- [ ] Birim testleri (iş mantığı) ve en az bir cihaz üstü Compose testi (ana akış)
- [ ] `permissions.allow` güncel; her izin Hakkında ekranında gerekçesiyle listelenir
- [ ] README (tools/ecosystem.py docs ile üretilir) ve PROJECT_STATUS.md güncel
- [ ] Demo veri yok; örnek veri gösterilecekse açıkça "örnek" olarak işaretlenir

## Yeni uygulama ekleme
1. `tools/apps.py` içine uygulamayı ekleyin (paket, renk, glif, izinler, özellikler).
2. `python3 tools/ecosystem.py scaffold` – manifest, simge, tema, yedekleme kuralları oluşur.
3. Kaynak kodunu yazın (`App.kt`, `MainActivity.kt`, ekranlar, veri katmanı, testler).
4. `settings.gradle.kts` içindeki `apps` listesine modülü ekleyin.
5. `python3 tools/ecosystem.py docs` – README ve PROJECT_STATUS.md güncellenir.
