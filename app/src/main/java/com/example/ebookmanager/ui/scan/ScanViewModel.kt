package com.example.ebookmanager.ui.scan

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebookmanager.data.Book
import com.example.ebookmanager.data.BookRepository
import com.example.ebookmanager.data.SafScanner
import com.example.ebookmanager.data.TreeStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScanUiState(
    val isScanning: Boolean = false,
    val scannedDirs: Int = 0,
    val foundBooks: Int = 0,
    val message: String? = null,
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val treeStore = TreeStore(application)
    private val scanner = SafScanner(application.contentResolver)

    private val _roots = MutableStateFlow(treeStore.roots())
    val roots: StateFlow<List<Uri>> = _roots.asStateFlow()

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    fun addTree(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (ignored: SecurityException) {
                _state.update { it.copy(message = "无法保存该目录的访问权限") }
            }
        }
        treeStore.add(uri)
        _roots.value = treeStore.roots()
    }

    fun removeTree(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        runCatching {
            resolver.releasePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }.onFailure {
            runCatching {
                resolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        treeStore.remove(uri)
        _roots.value = treeStore.roots()
        // 只移除数据库中的记录，不触碰任何文件
        BookRepository.removeTree(uri.toString())
    }

    fun startScan() {
        if (scanJob?.isActive == true) return
        val trees = _roots.value
        if (trees.isEmpty()) {
            _state.update { it.copy(message = "请先添加授权目录") }
            return
        }

        _state.value = ScanUiState(isScanning = true, message = "正在扫描…")
        scanJob = viewModelScope.launch {
            var totalDirs = 0
            var totalBooks = 0
            val allBooks = ArrayList<Book>()
            try {
                for (tree in trees) {
                    val result = scanner.scan(tree) { dirs, books ->
                        _state.update {
                            it.copy(
                                scannedDirs = totalDirs + dirs,
                                foundBooks = totalBooks + books,
                            )
                        }
                    }
                    totalDirs += result.dirs
                    totalBooks += result.books.size
                    allBooks += result.books
                }
                BookRepository.merge(allBooks)
                _state.update {
                    it.copy(
                        isScanning = false,
                        scannedDirs = totalDirs,
                        foundBooks = totalBooks,
                        message = "扫描完成：发现 $totalBooks 本电子书",
                    )
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(isScanning = false, message = "扫描已取消") }
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isScanning = false,
                        message = "扫描出错：${e.message ?: e.javaClass.simpleName}",
                    )
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
    }
}
