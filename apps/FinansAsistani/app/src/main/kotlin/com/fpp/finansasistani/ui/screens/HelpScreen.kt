package com.fpp.finansasistani.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

private data class HelpSection(val icon: ImageVector, val title: String, val points: List<String>)

private val sections = listOf(
    HelpSection(
        Icons.Outlined.Flag,
        "Uygulamanın amacı",
        listOf(
            "Netlik: Tüm gelir, gider ve borçlarınızı tek yerde görerek finansal fotoğrafınızı netleştirin.",
            "Strateji: Çığ ve Kartopu yöntemleriyle borçlarınızı en verimli şekilde nasıl kapatacağınızı keşfedin.",
            "Gelecek: Borçlarınızın ne zaman biteceğini ve ne kadar faiz ödeyeceğinizi ay ay görün.",
        ),
    ),
    HelpSection(
        Icons.AutoMirrored.Outlined.ListAlt,
        "Adım adım kullanım",
        listOf(
            "1. Kayıtlar sekmesinden gelirlerinizi ekleyin. Maaşınız için yılda kaç zam beklediğinizi ve zam oranını girin.",
            "2. Borçlarınızı ekleyin. Kredi kartında kart limitini, KMH'de faizi, sabit taksitli borçta taksit tutarı, " +
                "sayısı ve ilk ödeme tarihini girmeniz yeterlidir.",
            "3. Sabit giderlerinizi (kira, faturalar, abonelikler) ve birikim hedeflerinizi ekleyin.",
            "4. Özet sekmesinde nakit akışınızı inceleyin. Kayıtlara dokunarak düzenleyebilir, menüden silebilirsiniz.",
            "5. Plan sekmesinde yönteminizi seçip simülasyonu çalıştırın; grafik, kapanma sırası ve ay ay tabloyu inceleyin.",
        ),
    ),
    HelpSection(
        Icons.AutoMirrored.Outlined.TrendingDown,
        "Çığ ve Kartopu yöntemleri",
        listOf(
            "Her iki yöntemde de tüm borçların asgari ödemeleri yapılır; kalan ekstra para tek bir hedef borca yığılır.",
            "Çığ yöntemi: Hedef, en yüksek faizli borçtur. Matematiksel olarak en az faizi ödersiniz.",
            "Kartopu yöntemi: Hedef, en küçük bakiyeli borçtur. Borçlar tek tek hızlıca kapandığı için motivasyon sağlar.",
            "Bir borç kapandığında onun asgari ödemesi de ekstra güce eklenir ve bir sonraki borca aktarılır (kartopu etkisi).",
        ),
    ),
    HelpSection(
        Icons.Outlined.Calculate,
        "Hesaplama varsayımları",
        listOf(
            "Faiz aylık olarak işletilir: yıllık oran / 12. Kartın aylık faizini biliyorsanız 12 ile çarparak girin.",
            "Kredi kartı asgari ödemesi: toplam kart limiti 50.000 ₺ ve altındaysa bakiyenin %20'si, üzerindeyse %40'ı.",
            "KMH asgari ödemesi: o ayın faizi kadardır.",
            "Sabit taksitli borçlar faizsizdir; ilk ödeme tarihinden itibaren takvimine göre ödenir ve erken kapatılmaz.",
            "Maaş zamları belirttiğiniz aralıklarla uygulanır; zamdan gelen artışın tamamı borç ödemesine aktarılır.",
            "Tek seferlik gelirler simülasyonun ilk ayında borçlara aktarılır.",
            "Yüzdesel birikim, gelirden sabit giderler, asgari ödemeler ve sabit tutarlı birikimler düşüldükten sonra kalan tutar üzerinden hesaplanır.",
            "Simülasyon en fazla 50 yıl (600 ay) ileriye bakar. Sonuçlar tahmindir; bankanızın gerçek hesaplamaları farklı olabilir.",
        ),
    ),
    HelpSection(
        Icons.Outlined.Shield,
        "Gizlilik ve güvenlik",
        listOf(
            "Uygulama internet izni istemez ve hiçbir veriyi cihaz dışına göndermez.",
            "Reklam, analiz, telemetri, hesap veya üyelik yoktur. Google Play Hizmetleri gerekmez.",
            "Veriler buluta yedeklenmez. Yedek almak için Ayarlar > Yedeği dışa aktar seçeneğini kullanın ve dosyayı güvenli bir yerde saklayın.",
        ),
    ),
    HelpSection(
        Icons.Outlined.Lightbulb,
        "İpuçları",
        listOf(
            "Bir kaydı yanlışlıkla sildiyseniz alttaki bildirimde \"Geri al\"a dokunun.",
            "Veri girerken 1.500,75 veya 1500.75 biçimlerinin ikisi de kabul edilir.",
            "Simülasyondan sonra verileri değiştirirseniz Plan ekranı sizi yeniden çalıştırmanız için uyarır.",
        ),
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Yardım") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(sections, key = { it.title }) { section ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(section.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Text(section.title, style = MaterialTheme.typography.titleMedium)
                        }
                        section.points.forEach { point ->
                            Text(point, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
