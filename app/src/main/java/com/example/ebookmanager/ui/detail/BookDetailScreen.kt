package com.example.ebookmanager.ui.detail

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ebookmanager.core.Formatters
import com.example.ebookmanager.data.Book
import com.example.ebookmanager.data.SafPaths
import com.example.ebookmanager.ui.AppScaffold
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BookDetailScreen(book: Book, onBack: () -> Unit) {
    val context = LocalContext.current
    var fileExists by remember(book.uri) { mutableStateOf<Boolean?>(null) }
    var checkKey by remember(book.uri) { mutableStateOf(0) }
    var showFolderHelp by remember(book.uri) { mutableStateOf(false) }

    LaunchedEffect(book.uri, checkKey) {
        fileExists = withContext(Dispatchers.IO) {
            documentExists(context.contentResolver, Uri.parse(book.uri))
        }
    }

    val fullPath = SafPaths.fullPath(book)

    AppScaffold(title = "书籍详情", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(book.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "${book.format.label} · ${Formatters.fileSize(book.size)} · 修改于 " +
                    Formatters.dateTime(book.lastModified),
                style = MaterialTheme.typography.bodyMedium,
            )

            DetailRow("文件名", book.displayName)
            DetailRow("所在文件夹", book.folder)
            DetailRow("完整路径", fullPath)

            Text(
                text = when (fileExists) {
                    null -> "正在检查文件…"
                    true -> "文件存在"
                    false -> "文件不存在或无法访问（记录会保留，不会删除任何文件）"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when (fileExists) {
                    true -> MaterialTheme.colorScheme.primary
                    false -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )

            Button(
                onClick = {
                    if (!openWithReader(context, book)) {
                        Toast.makeText(
                            context,
                            "没有找到能打开 ${book.format.label} 的应用",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("用阅读器打开")
            }

            OutlinedButton(
                onClick = {
                    copyText(context, fullPath)
                    Toast.makeText(context, "完整路径已复制", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("复制完整路径")
            }

            OutlinedButton(
                onClick = {
                    if (!tryOpenFolder(context, book)) {
                        copyText(context, fullPath)
                        showFolderHelp = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("打开所在目录")
            }

            TextButton(onClick = { checkKey++ }) {
                Text("重新检查文件")
            }
        }
    }

    if (showFolderHelp) {
        AlertDialog(
            onDismissRequest = { showFolderHelp = false },
            title = { Text("无法直接打开文件夹") },
            text = {
                Text(
                    "系统文件管理器通常不支持从其他应用直接定位到某个文件夹。" +
                        "完整路径已复制，可以粘贴到文件管理器的搜索或地址栏中。",
                )
            },
            confirmButton = {
                TextButton(onClick = { showFolderHelp = false }) {
                    Text("知道了")
                }
            },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        SelectionContainer {
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun documentExists(resolver: ContentResolver, uri: Uri): Boolean = runCatching {
    resolver.query(
        uri,
        arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
        null,
        null,
        null,
    )?.use { it.moveToFirst() } ?: false
}.getOrDefault(false)

private fun openWithReader(context: Context, book: Book): Boolean {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse(book.uri), book.format.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}

/** 尽力打开父目录；多数系统文件管理器不支持，失败时由界面给出替代方案。 */
private fun tryOpenFolder(context: Context, book: Book): Boolean {
    val documentUri = Uri.parse(book.uri)
    val documentId = runCatching { DocumentsContract.getDocumentId(documentUri) }.getOrNull()
        ?: return false
    val parentId = documentId.substringBeforeLast('/', "")
    if (parentId.isEmpty() || parentId == documentId) return false

    val parentUri = DocumentsContract.buildDocumentUriUsingTree(
        Uri.parse(book.treeUri),
        parentId,
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(parentUri, DocumentsContract.Document.MIME_TYPE_DIR)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("电子书路径", text))
}
