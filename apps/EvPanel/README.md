# evserver Panel (Android)

Ev sunucusundaki kontrol panelini (port 8099) uygulama olarak açan ince bir WebView kabuğu.

- Açılışta önce ev adresini (`192.168.1.12`), olmazsa Tailscale adresini (`100.113.200.36`) dener.
- Panel şifresini ilk girişte sorar, istenirse hatırlar.
- Panel dışındaki adresler (Immich, FileBrowser, APK sayfası) telefonun tarayıcısında açılır.

Derleme `.github/workflows/evpanel-apk.yml` ile GitHub Actions'ta yapılır; son APK
`evpanel-son` sürümüne (Releases) yüklenir.

`app/evpanel.jks` sadece bu kişisel uygulamaya ayrılmış imza anahtarıdır; her derlemenin aynı
anahtarla imzalanması, yeni sürümün eskisinin üstüne kurulabilmesi içindir.
