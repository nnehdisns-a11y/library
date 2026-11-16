package com.example.ebookmanager.core

/**
 * 只按扩展名识别，不做内容嗅探。
 * 注意：TXT 与普通文本无法区分，扫描时另加了最小体积阈值。
 */
enum class EbookFormat(
    val label: String,
    val mimeType: String,
    val extensions: Set<String>,
) {
    EPUB("EPUB", "application/epub+zip", setOf("epub")),
    PDF("PDF", "application/pdf", setOf("pdf")),
    TXT("TXT", "text/plain", setOf("txt")),
    MOBI("MOBI", "application/x-mobipocket-ebook", setOf("mobi")),
    AZW("AZW", "application/vnd.amazon.ebook", setOf("azw")),
    AZW3("AZW3", "application/vnd.amazon.ebook", setOf("azw3")),
    CBZ("CBZ", "application/vnd.comicbook+zip", setOf("cbz")),
    CBR("CBR", "application/vnd.comicbook-rar", setOf("cbr"));

    companion object {
        private val byExtension: Map<String, EbookFormat> =
            EbookFormat.entries.flatMap { format -> format.extensions.map { it to format } }.toMap()

        fun fromFileName(fileName: String): EbookFormat? {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension.isEmpty()) return null
            return byExtension[extension]
        }
    }
}
