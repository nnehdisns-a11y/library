package com.example.ebookmanager.ui.library

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ebookmanager.core.EbookFormat
import com.example.ebookmanager.core.Formatters
import com.example.ebookmanager.data.Book
import com.example.ebookmanager.data.BookRepository
import com.example.ebookmanager.data.BookSort
import com.example.ebookmanager.data.FolderKey
import com.example.ebookmanager.data.FolderSummary
import com.example.ebookmanager.data.LibraryQuery
import com.example.ebookmanager.data.SafPaths
import com.example.ebookmanager.ui.AppScaffold

@Composable
fun LibraryScreen(
    onBack: () -> Unit,
    onScan: () -> Unit,
    onOpenBook: (Book) -> Unit,
) {
    val viewModel: LibraryViewModel = viewModel()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val allBooks by BookRepository.books.collectAsStateWithLifecycle()
    val visibleBooks = remember(allBooks, filters) { LibraryQuery.filterAndSort(allBooks, filters) }
    val folders = remember(allBooks) { LibraryQuery.folders(allBooks) }

    val context = LocalContext.current
    var showFolderPicker by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // 授权目录被移除或记录清空后，自动退出已失效的文件夹筛选
    LaunchedEffect(folders, filters.folder) {
        val folder = filters.folder
        if (folder != null && folders.none { it.key == folder }) {
            viewModel.setFolder(null)
        }
    }

    AppScaffold(title = "书库", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (allBooks.isEmpty()) {
                EmptyLibrary(onScan = onScan, modifier = Modifier.fillMaxSize())
            } else {
                OutlinedTextField(
                    value = filters.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    singleLine = true,
                    placeholder = { Text("搜索书名、文件名或文件夹") },
                    trailingIcon = {
                        if (filters.query.isNotEmpty()) {
                            TextButton(onClick = { viewModel.setQuery("") }) {
                                Text("清空")
                            }
                        }
                    },
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box {
                        OutlinedButton(onClick = { showSortMenu = true }) {
                            Text("排序：${filters.sort.label}")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            BookSort.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text("${sort.label}（${sort.hint}）") },
                                    onClick = {
                                        viewModel.setSort(sort)
                                        showSortMenu = false
                                    },
                                )
                            }
                        }
                    }

                    OutlinedButton(onClick = { showFolderPicker = true }) {
                        Text(if (filters.folder == null) "文件夹筛选" else "文件夹 · 已筛选")
                    }
                }

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        FilterChip(
                            selected = filters.format == null,
                            onClick = { viewModel.setFormat(null) },
                            label = { Text("全部格式") },
                        )
                    }
                    items(EbookFormat.entries) { format ->
                        FilterChip(
                            selected = filters.format == format,
                            onClick = { viewModel.setFormat(format) },
                            label = { Text(format.label) },
                        )
                    }
                }

                filters.folder?.let { folder ->
                    FolderFilterHeader(
                        folder = folder,
                        onCopy = {
                            copyText(context, SafPaths.folderPath(folder))
                            Toast.makeText(context, "文件夹路径已复制", Toast.LENGTH_SHORT).show()
                        },
                        onClear = { viewModel.setFolder(null) },
                    )
                }

                if (visibleBooks.isEmpty()) {
                    EmptyResult(
                        modifier = Modifier.weight(1f),
                        onClearAll = viewModel::clearAll,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            Text(
                                text = "共 ${visibleBooks.size} 本",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        items(visibleBooks, key = { it.uri }) { book ->
                            BookRow(
                                book = book,
                                onClick = { onOpenBook(book) },
                                onFolderClick = {
                                    viewModel.setFolder(FolderKey(book.treeUri, book.folder))
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            folders = folders,
            selected = filters.folder,
            totalBooks = allBooks.size,
            onSelect = viewModel::setFolder,
            onDismiss = { showFolderPicker = false },
        )
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier, onScan: () -> Unit) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("书库还是空的", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text("先选择一个目录并扫描。", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onScan) { Text("去扫描") }
    }
}

@Composable
private fun EmptyResult(modifier: Modifier, onClearAll: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("没有找到匹配的书籍", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "试试其他关键词，或清除筛选条件。",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onClearAll) { Text("清除全部条件") }
    }
}

@Composable
private fun FolderFilterHeader(
    folder: FolderKey,
    onCopy: () -> Unit,
    onClear: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = "当前文件夹",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = SafPaths.folderPath(folder),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onCopy) { Text("复制文件夹路径") }
                TextButton(onClick = onClear) { Text("返回全部书库") }
            }
        }
    }
}

@Composable
private fun FolderPickerDialog(
    folders: List<FolderSummary>,
    selected: FolderKey?,
    totalBooks: Int,
    onSelect: (FolderKey?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("按文件夹筛选") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                item {
                    FolderOptionRow(
                        label = "全部文件夹",
                        count = totalBooks,
                        selected = selected == null,
                        onClick = {
                            onSelect(null)
                            onDismiss()
                        },
                    )
                }
                items(folders, key = { it.key.treeUri + "|" + it.key.relativeDir }) { summary ->
                    FolderOptionRow(
                        label = SafPaths.folderPath(summary.key),
                        count = summary.count,
                        selected = summary.key == selected,
                        onClick = {
                            onSelect(summary.key)
                            onDismiss()
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun FolderOptionRow(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$count 本",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BookRow(
    book: Book,
    onClick: () -> Unit,
    onFolderClick: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 封面解析留到后续阶段，先用格式色块占位
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(book.format.label, style = MaterialTheme.typography.labelMedium)
            }

            Column(Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${book.format.label} · ${Formatters.fileSize(book.size)} · " +
                        Formatters.dateTime(book.lastModified),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "文件夹：${book.folder}",
                    modifier = Modifier.clickable(onClick = onFolderClick),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "路径：${SafPaths.fullPath(book)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("文件夹路径", text))
}
