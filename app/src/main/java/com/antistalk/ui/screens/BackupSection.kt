package com.antistalk.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antistalk.ui.MainViewModel
import com.antistalk.ui.theme.BrandMossGreenLight
import com.antistalk.ui.theme.LocalIsDarkTheme
import com.antistalk.ui.theme.WarmSage
import com.antistalk.ui.theme.WarmSageDark
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
    val isDark = LocalIsDarkTheme.current
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Dữ liệu",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Sao lưu từ khóa và danh sách né để chuyển sang máy mới.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // 1. Primary action: Nền xanh lá đậm (khuyên dùng / hành động chính)
        Button(
            onClick = { jsonLauncher.launch("antistalk-sao-luu-$stamp.json") },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDark) BrandMossGreenLight else WarmSageDark,
                contentColor = Color.White
            )
        ) {
            Text("Xuất sao lưu JSON", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }

        // 2. Secondary action: Viền / outline cho tác vụ phụ
        if (showRestore) {
            OutlinedButton(
                onClick = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(
                    1.5.dp,
                    if (isDark) BrandMossGreenLight.copy(alpha = 0.7f) else WarmSage.copy(alpha = 0.8f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDark) BrandMossGreenLight else WarmSageDark
                )
            ) {
                Text("Khôi phục từ file JSON", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // 3. Ghost / Text action: Tác vụ ít dùng
        TextButton(
            onClick = { apkLauncher.launch("antistalk-$stamp.apk") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                "Lưu bản cài đặt APK dự phòng",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
