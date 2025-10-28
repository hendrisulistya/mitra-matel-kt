package app.mitra.matel.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kebijakan Privasi") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        PrivacyPolicyContent(
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun PrivacyPolicyContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Kebijakan Privasi",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Text(
            text = "Terakhir diperbarui: September 2025",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Divider()

        PrivacySectionCard(
            title = "1. Informasi yang Kami Kumpulkan",
            content = "Kami mengumpulkan informasi yang Anda berikan secara langsung kepada kami, termasuk:\n\n• Informasi akun (nama, email, nomor telepon)\n• Data kendaraan yang Anda cari atau kelola\n• Informasi perangkat dan penggunaan aplikasi\n• Data lokasi (jika diizinkan)\n• Log aktivitas dan preferensi pengguna"
        )

        PrivacySectionCard(
            title = "2. Bagaimana Kami Menggunakan Informasi",
            content = "Informasi yang kami kumpulkan digunakan untuk:\n\n• Menyediakan dan meningkatkan layanan aplikasi\n• Memproses permintaan pencarian kendaraan\n• Mengirim notifikasi dan pembaruan penting\n• Menganalisis penggunaan untuk perbaikan layanan\n• Mematuhi kewajiban hukum\n• Mencegah penipuan dan penyalahgunaan"
        )

        PrivacySectionCard(
            title = "3. Berbagi Informasi",
            content = "Kami tidak menjual atau menyewakan informasi pribadi Anda. Kami dapat membagikan informasi dalam situasi berikut:\n\n• Dengan persetujuan eksplisit Anda\n• Untuk mematuhi kewajiban hukum\n• Dengan penyedia layanan tepercaya\n• Dalam kasus merger atau akuisisi\n• Untuk melindungi hak dan keamanan"
        )

        PrivacySectionCard(
            title = "4. Keamanan Data",
            content = "Kami menerapkan langkah-langkah keamanan yang sesuai untuk melindungi informasi Anda:\n\n• Enkripsi data saat transmisi dan penyimpanan\n• Kontrol akses yang ketat\n• Pemantauan keamanan berkelanjutan\n• Audit keamanan reguler\n• Pelatihan keamanan untuk karyawan"
        )

        PrivacySectionCard(
            title = "5. Penyimpanan Data",
            content = "Data Anda disimpan selama diperlukan untuk menyediakan layanan atau sesuai dengan kewajiban hukum. Kami akan menghapus atau menganonimkan data pribadi Anda ketika tidak lagi diperlukan."
        )

        PrivacySectionCard(
            title = "6. Hak Anda",
            content = "Anda memiliki hak untuk:\n\n• Mengakses data pribadi Anda\n• Memperbaiki informasi yang tidak akurat\n• Menghapus data pribadi Anda\n• Membatasi pemrosesan data\n• Portabilitas data\n• Menarik persetujuan kapan saja"
        )

        PrivacySectionCard(
            title = "7. Cookies dan Teknologi Pelacakan",
            content = "Kami menggunakan cookies dan teknologi serupa untuk:\n\n• Mengingat preferensi Anda\n• Menganalisis penggunaan aplikasi\n• Meningkatkan pengalaman pengguna\n• Menyediakan konten yang relevan\n\nAnda dapat mengatur preferensi cookies melalui pengaturan aplikasi."
        )

        PrivacySectionCard(
            title = "8. Layanan Pihak Ketiga",
            content = "Aplikasi kami dapat mengintegrasikan layanan pihak ketiga yang memiliki kebijakan privasi sendiri. Kami mendorong Anda untuk membaca kebijakan privasi mereka."
        )

        PrivacySectionCard(
            title = "9. Perubahan Kebijakan",
            content = "Kami dapat memperbarui kebijakan privasi ini dari waktu ke waktu. Perubahan material akan diberitahukan melalui aplikasi atau email. Penggunaan berkelanjutan setelah perubahan berarti Anda menyetujui kebijakan yang diperbarui."
        )

        PrivacySectionCard(
            title = "10. Hubungi Kami",
            content = "Jika Anda memiliki pertanyaan tentang kebijakan privasi ini atau ingin menggunakan hak privasi Anda, silakan hubungi kami:\n\nEmail: privacy@mitra-matel.com\nTelepon: +62 21 1234 5678\nAlamat: Jakarta, Indonesia"
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PrivacySectionCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify
            )
        }
    }
}

@Preview
@Composable
fun PrivacyPolicyScreenPreview() {
    MaterialTheme {
        PrivacyPolicyScreen()
    }
}