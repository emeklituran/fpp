#!/usr/bin/env python3
"""Ekosistem aracı.

  python3 tools/ecosystem.py scaffold   # eksik uygulama dosyalarını oluşturur (var olanlara dokunmaz)
  python3 tools/ecosystem.py docs       # uygulama README'leri ve PROJECT_STATUS.md'yi üretir
  python3 tools/ecosystem.py modules    # settings.gradle.kts için modül listesini yazdırır
"""
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
from catalog import CATALOG, CATEGORIES, DUPLICATES  # noqa: E402
from apps import APPS, GLYPHS  # noqa: E402

STATUS_FILE = os.path.join(ROOT, "tools", "status.json")
STATUS_STEPS = ["Planlandı", "Geliştiriliyor", "Kodlandı", "Derlendi", "Test edildi", "APK oluşturuldu", "Teslim edildi"]

# Uygulamaların istemediği ama kütüphanelerin ekleyebileceği izinler birleştirilmiş manifestten silinir.
REMOVABLE = [
    "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE", "android.permission.ACCESS_WIFI_STATE",
    "android.permission.WAKE_LOCK", "android.permission.RECEIVE_BOOT_COMPLETED", "android.permission.FOREGROUND_SERVICE",
    "com.google.android.gms.permission.AD_ID", "android.permission.ACCESS_ADSERVICES_AD_ID",
    "android.permission.ACCESS_ADSERVICES_ATTRIBUTION", "android.permission.ACCESS_ADSERVICES_TOPICS",
]


def write(path, content, overwrite=False):
    full = os.path.join(ROOT, path)
    if os.path.exists(full) and not overwrite:
        return False
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        f.write(content)
    return True


def full_perm(p):
    return p if "." in p else "android.permission." + p


def manifest_removals(app):
    allowed = {full_perm(p) for p, _ in app["permissions"]}
    lines = [f'    <uses-permission android:name="{p}" tools:node="remove" />' for p in REMOVABLE if p not in allowed]
    lines.append('    <permission android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" tools:node="remove" />')
    lines.append('    <uses-permission android:name="${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION" tools:node="remove" />')
    return "\n".join(lines)


def icon_xml(glyph):
    paths = []
    for kind, data, width in GLYPHS[glyph]:
        if kind == "fill":
            paths.append(f'    <path android:fillColor="#FFFFFFFF" android:pathData="{data}" />')
        else:
            paths.append(
                f'    <path android:fillColor="#00000000" android:strokeColor="#FFFFFFFF" android:strokeWidth="{width}"\n'
                f'        android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{data}" />')
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">\n'
            + "\n".join(paths) + "\n</vector>\n")


def scaffold(no, app):
    d = app["dir"]
    res = f"{d}/src/main/res"
    light, dark = app["accent"]
    created = []
    if write(f"{d}/build.gradle.kts", f'''plugins {{
    id("galaxy.android.application")
{'    id("galaxy.android.room")' + chr(10) if app["room"] else ''}}}

android {{
    namespace = "{app["package"]}"
    defaultConfig {{
        applicationId = "{app["package"]}"
        versionCode = {app["version"][1]}
        versionName = "{app["version"][0]}"
    }}
}}

dependencies {{
    implementation(project(":core:designsystem"))
}}
'''):
        created.append("build.gradle.kts")
    write(f"{d}/src/main/AndroidManifest.xml", f'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Gizlilik: istenmeyen izinler (internet dahil) birleştirilmiş manifestten silinir. -->
{manifest_removals(app)}

    <application
        android:name=".App"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:enableOnBackInvokedCallback="true"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:localeConfig="@xml/locales_config"
        android:supportsRtl="true"
        android:theme="@style/Theme.App"
        tools:targetApi="36">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
''')
    write(f"{res}/values/strings.xml", f'<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <string name="app_name">{app["name"]}</string>\n</resources>\n')
    write(f"{res}/values/colors.xml", f'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">{light.replace('#', '#FF')}</color>
    <color name="window_background">#FFF8F9FB</color>
</resources>
''')
    write(f"{res}/values-night/colors.xml", '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n    <color name="window_background">#FF111318</color>\n</resources>\n')
    for folder, parent in (("values", "android:Theme.Material.Light.NoActionBar"), ("values-night", "android:Theme.Material.NoActionBar")):
        write(f"{res}/{folder}/themes.xml", f'''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Android 12+ açılış ekranı: uygulama simgesi ve kendi rengi. -->
    <style name="Theme.App" parent="{parent}">
        <item name="android:windowBackground">@color/window_background</item>
        <item name="android:windowSplashScreenBackground">@color/window_background</item>
        <item name="android:windowSplashScreenIconBackgroundColor">@color/ic_launcher_background</item>
    </style>
</resources>
''')
    write(f"{res}/drawable/ic_launcher_foreground.xml", icon_xml(app["glyph"]))
    write(f"{res}/mipmap-anydpi-v26/ic_launcher.xml", '''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
''')
    write(f"{res}/xml/locales_config.xml", '<?xml version="1.0" encoding="utf-8"?>\n<locale-config xmlns:android="http://schemas.android.com/apk/res/android">\n    <locale android:name="tr" />\n</locale-config>\n')
    write(f"{res}/xml/data_extraction_rules.xml", '''<?xml version="1.0" encoding="utf-8"?>
<!-- Veriler buluta yedeklenmez; yalnızca yerel cihazdan cihaza aktarım (ör. Smart Switch) mümkündür. -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" />
        <exclude domain="file" />
        <exclude domain="database" />
        <exclude domain="sharedpref" />
        <exclude domain="external" />
    </cloud-backup>
    <device-transfer>
        <include domain="database" />
        <include domain="file" />
    </device-transfer>
</data-extraction-rules>
''')
    write(f"{d}/permissions.allow", "".join(full_perm(p) + "\n" for p, _ in app["permissions"]), overwrite=True)
    return created


def load_status():
    if os.path.exists(STATUS_FILE):
        with open(STATUS_FILE, encoding="utf-8") as f:
            return {int(k): v for k, v in json.load(f).items()}
    return {}


def docs():
    status = load_status()
    for no, app in APPS.items():
        perms = "\n".join(f"| `{full_perm(p)}` | {r} |" for p, r in app["permissions"]) or "| — | Bu uygulama hiçbir izin istemez. |"
        st = status.get(no, {})
        write(f'{app["dir"]}/README.md', f'''# {app["name"]}

{app["summary"]}

| | |
|---|---|
| Katalog no | {no} |
| Paket adı | `{app["package"]}` |
| Sürüm | {app["version"][0]} ({app["version"][1]}) |
| Gradle modülü | `:{app["module"]}` |
| Minimum Android | 12 (API 31) · Hedef: Android 16 (API 36) |
| İnternet | Yok (INTERNET izni yok, CI ile doğrulanır) |
| Durum | {st.get("state", "Planlandı")} |

## Özellikler
{chr(10).join("- " + f for f in app["features"])}

## İzinler
| İzin | Neden |
|---|---|
{perms}

## Bilinen kısıtlamalar
{chr(10).join("- " + l for l in app["limitations"]) or "- Yok"}

## Derleme
```bash
cd Galaxy_S25_Offline_Ecosystem
./gradlew :{app["module"]}:assembleRelease      # APK: {app["dir"]}/build/outputs/apk/release/
./gradlew :{app["module"]}:testDebugUnitTest     # birim testleri
./gradlew :{app["module"]}:connectedDebugAndroidTest   # cihaz/emülatör testleri
```
''', overwrite=True)

    lines = ["# PROJECT_STATUS", "",
             "Galaxy S25 Offline Ecosystem – uygulama bazında durum takibi. Bu dosya `tools/ecosystem.py docs` ile üretilir;",
             "durumlar `tools/status.json` içinde tutulur ve yalnızca gerçekten doğrulanan adımlar işaretlenir.", "",
             "Durum sırası: " + " → ".join(STATUS_STEPS), ""]
    done = sum(1 for v in status.values() if v.get("state") in ("APK oluşturuldu", "Teslim edildi"))
    lines += [f"**Özet:** {len(CATALOG)} katalog uygulaması + App Center · APK'sı oluşturulan: {done}", ""]
    for cat, (folder, title) in CATEGORIES.items():
        lines += [f"## {cat}. {title} (`{folder}`)", "", "| No | Uygulama | Paket | Durum | Not |", "|---|---|---|---|---|"]
        for no, c, name in CATALOG:
            if c != cat:
                continue
            app = APPS.get(no)
            st = status.get(no, {})
            pkg = f'`{app["package"]}`' if app else "—"
            note = st.get("note", "")
            if no in DUPLICATES:
                note = f"No {DUPLICATES[no]} ile aynı uygulama (tek APK)"
            lines.append(f"| {no} | {app['name'] if app else name} | {pkg} | {st.get('state', 'Planlandı')} | {note} |")
        lines.append("")
    lines += ["## 13. Galaxy Offline App Center (`13_App_Center`)", "", "| Uygulama | Durum |", "|---|---|",
              f"| Galaxy Offline App Center | {status.get(0, {}).get('state', 'Planlandı')} |", ""]
    write("PROJECT_STATUS.md", "\n".join(lines), overwrite=True)


def modules():
    for no, app in sorted(APPS.items()):
        print(f'    ":{app["module"]}" to "{app["dir"]}",')


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "docs"
    if cmd == "scaffold":
        for no, app in APPS.items():
            scaffold(no, app)
        print("scaffold tamam")
    elif cmd == "docs":
        docs()
        print("docs tamam")
    elif cmd == "modules":
        modules()
