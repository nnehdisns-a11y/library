package com.example.ebookmanager.ui.scan

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ebookmanager.EbookApp
import com.example.ebookmanager.core.SafAuthorization
import com.example.ebookmanager.data.SafPaths
import com.example.ebookmanager.data.SafScanner
import com.example.ebookmanager.data.TreeRoot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TreeItem(
    val root: TreeRoot,
    val authorized: Boolean,
)

data class ScanUiState(
    val isScanning: Boolean = false,
    val scannedDirs: Int = 0,
    val foundBooks: Int = 0,
    val message: String? = null,
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as EbookApp).bookRepository
    private val resolver = application.contentResolver
    private val scanner = SafScanner(resolver)

    private val authorizedUris = MutableStateFlow(currentAuthorizedUris())

    /** 数据库中的目录 + 实时授权状态；授权失效的目录仍保留记录并明确标出。 */
    val trees: StateFlow<List<TreeItem>> =
        combine(repository.trees, authorizedUris) { roots, authorized ->
            roots.map { root ->
                TreeItem(
                    root = root,
                    authorized = SafAuthorization.isAuthorized(root.uri, authorized),
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private var scanJob: Job? = null

    init {
        refreshPermissions()
    }

    /** 页面重新进入时刷新，用户可能在系统设置里撤销了授权。 */
    fun refreshPermissions() {
        authorizedUris.value = currentAuthorizedUris()
    }

    private fun currentAuthorizedUris(): Set<String> =
        resolver.persistedUriPermissions.map { it.uri.toString() }.toSet()

    fun addTree(uri: Uri) {
        val granted = try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
            true
        } catch (e: SecurityException) {
            try {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                true
            } catch (ignored: SecurityException) {
                false
            }
        }

        if (!granted) {
            _state.update { it.copy(message = "无法保存该目录的访问权限，请重新选择") }
            return
        }

        val path = SafPaths.rootLabel(uri)
        val displayName = path.substringAfterLast('/').ifEmpty { path }
        viewModelScope.launch {
            repository.addTree(uri.toString(), displayName, path)
            refreshPermissions()
            _state.update { it.copy(message = "已添加目录：$path") }
        }
    }

    fun removeTree(item: TreeItem) {
        val uri = Uri.parse(item.root.uri)
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

        viewModelScope.launch {
            // 只删除本地数据库记录，不触碰设备上的文件
            repository.removeTree(item.root.uri)
            refreshPermissions()
            _state.update { it.copy(message = "已移除目录记录：${item.root.path}") }
        }
    }

    fun startScan() {
        if (scanJob?.isActive == true) return

        val allTrees = trees.value
        if (allTrees.isEmpty()) {
            _state.update { it.copy(message = "请先添加授权目录") }
            return
        }

        val targets = allTrees.filter { it.authorized }
        val skipped = allTrees.size - targets.size
        if (targets.isEmpty()) {
            _state.update { it.copy(message = "所有目录的授权都已失效，请重新添加目录") }
            return
        }

        _state.value = ScanUiState(
            isScanning = true,
            message = if (skipped > 0) {
                "正在扫描（跳过 $skipped 个授权失效目录）…"
            } else {
                "正在扫描…"
            },
        )

        scanJob = viewModelScope.launch {
            var totalDirs = 0
            var totalBooks = 0
            try {
                for (item in targets) {
                    val result = scanner.scan(Uri.parse(item.root.uri)) { dirs, books ->
                        _state.update { current ->
                            current.copy(
                                scannedDirs = totalDirs + dirs,
                                foundBooks = totalBooks + books,
                            )
                        }
                    }
                    // 只有整个目录扫描成功才同步，取消或失败时保留该目录原有记录
                    repository.syncTree(item.root.uri, result.books)
                    totalDirs += result.dirs
                    totalBooks += result.books.size
                }

                val skippedHint = if (skipped > 0) "（跳过 $skipped 个授权失效目录）" else ""
                _state.update {
                    it.copy(
                        isScanning = false,
                        scannedDirs = totalDirs,
                        foundBooks = totalBooks,
                        message = "扫描完成：发现 $totalBooks 本电子书$skippedHint",
                    )
                }
            } catch (e: CancellationException) {
                _state.update {
                    it.copy(
                        isScanning = false,
                        message = "扫描已取消，未完成的目录保留原有记录",
                    )
                }
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
