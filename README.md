# 本地书库（EbookManager）

轻量、完全本地的 Android 电子书管理工具。通过 SAF（存储访问框架）读取用户授权目录，
扫描 EPUB / PDF / TXT / MOBI / AZW / AZW3 / CBZ / CBR，展示书名、格式、大小、文件夹、
完整可读路径与修改时间。

- 不申请存储权限，不使用 `MANAGE_EXTERNAL_STORAGE`
- 不联网、无账号、无服务器
- 扫描阶段只读，不修改、不移动、不删除任何文件
- 书库实时搜索：书名 / 原始文件名 / 文件夹名称与路径，英文不区分大小写
- 格式与文件夹筛选、文件名 / 大小 / 修改时间排序、文件夹视图与路径复制
- Kotlin + Jetpack Compose + Material3 + Room（KSP）

## 构建

本地（Android Studio / 命令行）：

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

云端：推送到 GitHub `main` 分支，或在 Actions 页手动触发 `Android CI`，
构建完成后下载 `app-debug` artifact。

## 目录

```
app/src/main/java/com/example/ebookmanager/
  core/      扩展名识别、格式化、SAF 授权判断（纯 Kotlin，可单测）
  data/      Book、TreeRoot、SafScanner、BookRepository
  data/db/   Room 实体、DAO、数据库、映射
  ui/        Compose 页面（首页 / 书库 / 扫描 / 详情）
```

## 数据持久化（Room）

数据库文件：`ebook-library.db`，两张表：

| 表 | 主要字段 |
|---|---|
| `books` | `uri`（主键）、`treeUri`、`displayName`、`title`、`folder`、`relativePath`、`format`、`size`、`lastModified`、`addedAt` |
| `trees` | `uri`（主键）、`displayName`、`path`、`addedAt` |

恢复与同步机制：

- App 启动时由 `BookRepository` 观察 Room 的 Flow，界面直接拿到上次扫描结果，**不会自动重扫**。
- 扫描按目录逐个完成，只有整棵目录扫描成功后才在事务中替换该目录的记录；
  取消、失败或授权失效都不会清空其他目录或未完成目录的旧数据。
- 以 document URI 为主键做 `Upsert`，重复扫描不会产生重复记录；`addedAt` 保留首次加入时间。
- 移除目录时在同一事务中删除该目录的书籍记录与目录记录，并释放 SAF 持久化授权，
  **不会删除设备上的实际文件**。
- 启动/进入扫描页时会用 `persistedUriPermissions` 校验授权；失效目录保留记录并标红提示，
  可选择重新添加或移除。

## 测试

- 纯 JVM 测试：格式识别、筛选排序、SAF 授权判断
- Room 测试（Robolectric）：读写、URI 去重、更新、多目录隔离、删除隔离、重启恢复、同步安全

## 已知限制

- SAF 不提供真实文件系统绝对路径，界面展示的是由 documentId 还原的可读路径
- 「打开所在目录」没有标准系统接口，失败时会复制路径作为替代
