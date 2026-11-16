package com.example.ebookmanager.ui.scan

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ebookmanager.ui.AppScaffold

@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val viewModel: ScanViewModel = viewModel()
    val trees by viewModel.trees.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 进入页面时重新核对系统持久化授权（用户可能已在系统设置中撤销）
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    val pickDirectory = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) viewModel.addTree(uri)
    }

    AppScaffold(title = "扫描目录", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("授权目录", style = MaterialTheme.typography.titleMedium)

            if (trees.isEmpty()) {
                Text(
                    text = "还没有授权目录。点击下方按钮选择手机里的文件夹，应用只会读取文件信息，" +
                        "不会修改或删除任何文件。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                val invalidCount = trees.count { !it.authorized }
                if (invalidCount > 0) {
                    Text(
                        text = "有 $invalidCount 个目录授权已失效。请重新添加或移除对应目录，" +
                            "应用不会因此删除书籍记录。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                trees.forEach { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.root.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = item.root.path,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (!item.authorized) {
                                    Text(
                                        text = "授权已失效，请移除后重新添加",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            TextButton(
                                onClick = { viewModel.removeTree(item) },
                                enabled = !state.isScanning,
                            ) {
                                Text("移除")
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { pickDirectory.launch(null) },
                enabled = !state.isScanning,
            ) {
                Text("添加授权目录")
            }

            HorizontalDivider()

            Button(
                onClick = { viewModel.startScan() },
                enabled = trees.any { it.authorized } && !state.isScanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isScanning) "扫描中…" else "开始扫描")
            }

            if (state.isScanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    text = "已扫描 ${state.scannedDirs} 个文件夹，发现 ${state.foundBooks} 本",
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = { viewModel.cancelScan() }) {
                    Text("取消扫描")
                }
            }

            state.message?.let { message ->
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedButton(
                onClick = onOpenLibrary,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("进入书库")
            }
        }
    }
}
