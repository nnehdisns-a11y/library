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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ebookmanager.data.SafPaths
import com.example.ebookmanager.ui.AppScaffold

@Composable
fun ScanScreen(
    onBack: () -> Unit,
    onOpenLibrary: () -> Unit,
) {
    val viewModel: ScanViewModel = viewModel()
    val roots by viewModel.roots.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

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

            if (roots.isEmpty()) {
                Text(
                    text = "还没有授权目录。点击下方按钮选择手机里的文件夹，应用只会读取文件信息，" +
                        "不会修改或删除任何文件。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                roots.forEach { tree ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = SafPaths.rootLabel(tree),
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            TextButton(
                                onClick = { viewModel.removeTree(tree) },
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
                enabled = roots.isNotEmpty() && !state.isScanning,
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
