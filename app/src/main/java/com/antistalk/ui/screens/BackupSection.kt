package com.antistalk.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antistalk.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val APK_MIME = "application/vnd.android.package-archive"

/**
 * Export / restore buttons backed by SAF, so nothing depends on a path the user
 * has to type. The same composable is embedded in the migration dialog — after
 * an uninstall the app's own storage is gone, so the JSON has to be written
 * somewhere outside it first.
 */
@Composable
fun BackupSection(vm: MainViewModel, modifier: Modifier = Modifier, showRestore: Boolean = true) {
    val ctx = LocalContext.current
    val stamp = remember {
        SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(Date())
    }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { vm.exportBackupTo(ctx, it) } }
    val apkLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(APK_MIME)
    ) { uri -> uri?.let { vm.saveUpdateApkTo(ctx, it) } }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importBackupFrom(ctx, it) } }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Dữ liệu",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Xuất file JSON trước khi gỡ app hoặc đổi máy — cài lại xong thì Khôi phục để lấy người né và từ khóa về.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = { jsonLauncher.launch("antistalk-sao-luu-$stamp.json") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("XUẤT SAO LƯU JSON", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = { apkLauncher.launch("antistalk-$stamp.apk") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("LƯU BẢN CÀI ĐẶT APK", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        if (showRestore) {
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("KHÔI PHỤC TỪ FILE", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
