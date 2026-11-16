# 本地书库（EbookManager）

轻量、完全本地的 Android 电子书管理工具。通过 SAF（存储访问框架）读取用户授权目录，
扫描 EPUB / PDF / TXT / MOBI / AZW / AZW3 / CBZ / CBR，展示书名、格式、大小、文件夹、
完整可读路径与修改时间。

- 不申请存储权限，不使用 `MANAGE_EXTERNAL_STORAGE`
- 不联网、无账号、无服务器
- 扫描阶段只读，不修改任何文件
- Kotlin + Jetpack Compose + Material3，阶段一暂不引入 Room

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
  core/    扩展名识别、大小/时间格式化（纯 Kotlin，可单测）
  data/    Book、TreeStore、SafScanner、BookRepository
  ui/      Compose 页面（首页 / 书库 / 扫描 / 详情）
```

## 已知限制

- SAF 不提供真实文件系统绝对路径，界面展示的是由 documentId 还原的可读路径
- 「打开所在目录」没有标准系统接口，失败时会复制路径作为替代
- 阶段一数据仅存内存，App 重启后需重新扫描（阶段二接入 Room）
