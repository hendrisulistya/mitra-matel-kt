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
fun TermOfServiceScreen(
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Syarat dan Ketentuan") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        TermOfServiceContent(
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun TermOfServiceContent(
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
            text = "Syarat dan Ketentuan Penggunaan",
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

        SectionCard(
            title = "1. Penerimaan Syarat",
            content = "Dengan mengunduh, menginstal, atau menggunakan aplikasi Mitra Matel, Anda menyetujui untuk terikat oleh syarat dan ketentuan ini. Jika Anda tidak menyetujui syarat ini, mohon untuk tidak menggunakan aplikasi."
        )

        SectionCard(
            title = "2. Deskripsi Layanan",
            content = "Mitra Matel adalah aplikasi yang menyediakan layanan pencarian dan manajemen data kendaraan bermotor. Aplikasi ini membantu pengguna untuk mengakses informasi kendaraan secara cepat dan akurat."
        )

        SectionCard(
            title = "3. Akun Pengguna",
            content = "Untuk menggunakan layanan tertentu, Anda mungkin perlu membuat akun. Anda bertanggung jawab untuk menjaga kerahasiaan informasi akun Anda dan semua aktivitas yang terjadi di bawah akun Anda."
        )

        SectionCard(
            title = "4. Penggunaan yang Diizinkan",
            content = "Anda setuju untuk menggunakan aplikasi hanya untuk tujuan yang sah dan sesuai dengan hukum yang berlaku. Anda tidak diperbolehkan untuk:\n\n• Menggunakan aplikasi untuk tujuan ilegal\n• Mengganggu atau merusak layanan\n• Mengakses data tanpa izin\n• Menyalahgunakan informasi yang diperoleh"
        )

        SectionCard(
            title = "5. Privasi dan Data",
            content = "Penggunaan data pribadi Anda diatur oleh Kebijakan Privasi kami. Dengan menggunakan aplikasi, Anda menyetujui pengumpulan dan penggunaan informasi sesuai dengan kebijakan tersebut."
        )

        SectionCard(
            title = "6. Batasan Tanggung Jawab",
            content = "Aplikasi disediakan 'sebagaimana adanya'. Kami tidak memberikan jaminan bahwa layanan akan selalu tersedia, akurat, atau bebas dari kesalahan. Kami tidak bertanggung jawab atas kerugian yang mungkin timbul dari penggunaan aplikasi."
        )

        SectionCard(
            title = "7. Perubahan Syarat",
            content = "Kami berhak untuk mengubah syarat dan ketentuan ini sewaktu-waktu. Perubahan akan berlaku setelah dipublikasikan dalam aplikasi. Penggunaan berkelanjutan aplikasi setelah perubahan berarti Anda menyetujui syarat yang baru."
        )

        SectionCard(
            title = "8. Penghentian",
            content = "Kami dapat menghentikan atau menangguhkan akses Anda ke aplikasi kapan saja, dengan atau tanpa pemberitahuan, jika Anda melanggar syarat dan ketentuan ini."
        )

        SectionCard(
            title = "9. Hukum yang Berlaku",
            content = "Syarat dan ketentuan ini diatur oleh hukum Republik Indonesia. Setiap sengketa yang timbul akan diselesaikan melalui pengadilan yang berwenang di Jakarta."
        )

        SectionCard(
            title = "10. Kontak",
            content = "Jika Anda memiliki pertanyaan tentang syarat dan ketentuan ini, silakan hubungi kami di:\n\nEmail: support@mitra-matel.com\nTelepon: +62 21 1234 5678"
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SectionCard(
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
fun TermOfServiceScreenPreview() {
    MaterialTheme {
        TermOfServiceScreen()
    }
}