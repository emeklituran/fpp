package com.evserver.panel;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.net.HttpURLConnection;
import java.net.URL;

/**
 * evserver Kontrol Paneli için ince bir kabuk.
 * Açılışta önce ev adresini, olmazsa Tailscale adresini dener; paneli WebView içinde gösterir.
 * Panel dışındaki adresler (Immich, FileBrowser, APK sayfası...) telefonun tarayıcısında açılır.
 */
public class MainActivity extends Activity {

    private static final String[] ADRESLER = {
            "http://192.168.1.12:8099",    // ev ağı
            "http://100.113.200.36:8099",  // Tailscale
    };

    private WebView web;
    private SharedPreferences ayar;
    private String taban;          // şu an kullanılan adres
    private boolean kayitliDenendi;
    private boolean hataSayfasinda;
    private boolean araniyor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ayar = getSharedPreferences("panel", MODE_PRIVATE);

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0F1115"));
        web.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setSupportMultipleWindows(false);   // target=_blank aynı görünümde açılmaya çalışır, aşağıda tarayıcıya yönlendirilir
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.setWebViewClient(new Istemci());
        web.setWebChromeClient(new WebChromeClient()); // alert / confirm / prompt pencereleri için
        setContentView(web);

        if (savedInstanceState != null && savedInstanceState.getString("taban") != null) {
            taban = savedInstanceState.getString("taban");
            web.restoreState(savedInstanceState);
        } else {
            baglan(true);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("taban", taban);
        web.saveState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Evden çıkınca / eve gelince doğru adrese geçmek için her dönüşte kontrol et
        if (taban != null || hataSayfasinda) baglan(false);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    /** Ulaşılabilen ilk adresi bulur. ilkAcilis=false ise adres değişmediyse sayfayı yeniden yüklemez. */
    private void baglan(final boolean ilkAcilis) {
        if (araniyor) return;
        araniyor = true;
        if (ilkAcilis) bilgiSayfasi("Sunucu aranıyor…", "Ev ağı ve Tailscale deneniyor.", false);
        new Thread(() -> {
            String bulunan = null;
            for (String a : ADRESLER) {
                if (ulasilabilir(a)) { bulunan = a; break; }
            }
            final String sonuc = bulunan;
            runOnUiThread(() -> {
                araniyor = false;
                if (sonuc == null) {
                    if (ilkAcilis || hataSayfasinda || taban != null) {
                        taban = null;
                        bilgiSayfasi("Sunucuya ulaşılamadı",
                                "Evde değilsen telefonda <b>Tailscale</b>'in açık olduğundan emin ol.<br><br>" +
                                "Evdeysen sunucu kapalı ya da yeniden başlıyor olabilir; 1–2 dakika bekle.", true);
                    }
                    return;
                }
                if (!sonuc.equals(taban) || hataSayfasinda) {
                    taban = sonuc;
                    hataSayfasinda = false;
                    kayitliDenendi = false;
                    web.loadUrl(taban + "/");
                }
            });
        }).start();
    }

    private static boolean ulasilabilir(String adres) {
        HttpURLConnection c = null;
        try {
            c = (HttpURLConnection) new URL(adres + "/api/ozet").openConnection();
            c.setConnectTimeout(1500);
            c.setReadTimeout(2500);
            c.setInstanceFollowRedirects(false);
            int kod = c.getResponseCode();
            return kod == 200 || kod == 401;   // 401 = panel orada, şifre istiyor
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }

    private void bilgiSayfasi(String baslik, String metin, boolean tekrarDugmesi) {
        hataSayfasinda = tekrarDugmesi;
        String html = "<!doctype html><html><head><meta name=viewport content='width=device-width,initial-scale=1'>" +
                "<style>body{margin:0;background:#0F1115;color:#E8EAEE;font:16px/1.5 sans-serif;display:flex;" +
                "align-items:center;justify-content:center;min-height:100vh;text-align:center;padding:24px;box-sizing:border-box}" +
                "h1{font-size:20px;margin:16px 0 8px}p{color:#9AA1AD;margin:0 0 24px}" +
                "a{display:inline-block;background:#34C38F;color:#fff;text-decoration:none;font-weight:600;padding:12px 22px;border-radius:10px}" +
                ".logo{width:56px;height:56px;border-radius:14px;background:#123227;display:inline-grid;place-items:center}" +
                ".donen{width:22px;height:22px;border:3px solid #2A2F38;border-top-color:#34C38F;border-radius:50%;animation:d .8s linear infinite;margin:0 auto}" +
                "@keyframes d{to{transform:rotate(360deg)}}</style></head><body><div>" +
                "<div class=logo><svg width=28 height=28 viewBox='0 0 24 24' fill=none stroke='#34C38F' stroke-width=2 stroke-linecap=round>" +
                "<rect x=3 y=4 width=18 height=7 rx=1.5 /><rect x=3 y=13 width=18 height=7 rx=1.5 /><path d='M7 7.5h.01M7 16.5h.01'/></svg></div>" +
                "<h1>" + baslik + "</h1><p>" + metin + "</p>" +
                (tekrarDugmesi ? "<a href='evpanel://yeniden'>Tekrar dene</a>" : "<div class=donen></div>") +
                "</div></body></html>";
        web.loadDataWithBaseURL("about:blank", html, "text/html", "utf-8", null);
    }

    private void girisPenceresi(final HttpAuthHandler handler, boolean hatali) {
        LinearLayout kutu = new LinearLayout(this);
        kutu.setOrientation(LinearLayout.VERTICAL);
        int p = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        kutu.setPadding(p, p / 2, p, 0);

        final EditText kullanici = new EditText(this);
        kullanici.setHint("Kullanıcı adı");
        kullanici.setSingleLine();
        kullanici.setText(ayar.getString("kullanici", "turan"));
        final EditText sifre = new EditText(this);
        sifre.setHint("Panel şifresi");
        sifre.setSingleLine();
        sifre.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        final CheckBox hatirla = new CheckBox(this);
        hatirla.setText("Beni hatırla");
        hatirla.setChecked(true);
        kutu.addView(kullanici);
        kutu.addView(sifre);
        kutu.addView(hatirla);

        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle(hatali ? "Şifre yanlış, tekrar dene" : "evserver paneline giriş")
                .setView(kutu)
                .setCancelable(false)
                .setPositiveButton("Giriş", (d, w) -> {
                    String k = kullanici.getText().toString().trim();
                    String s = sifre.getText().toString();
                    SharedPreferences.Editor e = ayar.edit().putString("kullanici", k);
                    if (hatirla.isChecked()) e.putString("sifre", s); else e.remove("sifre");
                    e.apply();
                    handler.proceed(k, s);
                })
                .setNegativeButton("Vazgeç", (d, w) -> {
                    handler.cancel();
                    bilgiSayfasi("Giriş yapılmadı", "Panel şifreyle korunuyor.", true);
                })
                .show();
        sifre.requestFocus();
    }

    private void tarayicidaAc(Uri u) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Bu bağlantıyı açacak uygulama yok", Toast.LENGTH_SHORT).show();
        }
    }

    private class Istemci extends WebViewClient {
        private boolean sorulduMu;

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
            Uri u = req.getUrl();
            String adres = u.toString();
            if ("evpanel".equals(u.getScheme())) {
                hataSayfasinda = true;
                baglan(true);
                return true;
            }
            if (taban != null && adres.startsWith(taban)) return false;   // panelin kendi sayfası
            tarayicidaAc(u);   // Immich, FileBrowser, APK sayfası vb.
            return true;
        }

        @Override
        public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
            String k = ayar.getString("kullanici", null);
            String s = ayar.getString("sifre", null);
            if (!kayitliDenendi && k != null && s != null) {
                kayitliDenendi = true;
                handler.proceed(k, s);
                return;
            }
            boolean hatali = kayitliDenendi || sorulduMu;
            if (hatali) ayar.edit().remove("sifre").apply();
            sorulduMu = true;
            girisPenceresi(handler, hatali);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (taban != null && url.startsWith(taban)) {
                // giriş başarılıysa bir sonraki 401'de kayıtlı şifre yeniden denenebilsin
                kayitliDenendi = false;
                sorulduMu = false;
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest req, WebResourceError err) {
            if (req.isForMainFrame() && taban != null) {
                taban = null;
                bilgiSayfasi("Bağlantı koptu", "Sunucuya şu an ulaşılamıyor. Wi-Fi ya da Tailscale bağlantını kontrol et.", true);
            }
        }
    }
}
