package com.evgeniich.flashread.core.importdoc

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipException
import java.util.zip.ZipInputStream

/**
 * Copies a picked document to a temp file, detects the format, and returns
 * UTF-8 paragraph-per-line text. [ExtractedBook.title] comes from book metadata
 * when present; callers should fall back to the file display name.
 */
object BookTextExtractor {
    const val UNSUPPORTED_FORMAT_MESSAGE =
        "This file format is not supported. Import .txt, .fb2, or .epub."
    const val DAMAGED_FILE_MESSAGE =
        "Could not read this book. The file may be damaged."
    const val UNABLE_TO_READ_MESSAGE = "Unable to read selected file."

    fun extract(
        contentResolver: ContentResolver,
        uri: Uri,
        cacheDir: File,
        fileName: String? = queryDisplayName(contentResolver, uri),
        mimeType: String? = contentResolver.getType(uri),
    ): ExtractedBook {
        val temp = try {
            copyUriToTempFile(contentResolver, uri, cacheDir)
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: Exception) {
            throw IllegalStateException(UNABLE_TO_READ_MESSAGE, error)
        }
        try {
            return extract(file = temp, fileName = fileName, mimeType = mimeType)
        } finally {
            temp.delete()
        }
    }

    fun extract(
        file: File,
        fileName: String? = file.name,
        mimeType: String? = null,
    ): ExtractedBook {
        try {
            return extractDetected(file, fileName, mimeType)
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: XmlPullParserException) {
            throw IllegalStateException(DAMAGED_FILE_MESSAGE, error)
        } catch (error: ZipException) {
            throw IllegalStateException(DAMAGED_FILE_MESSAGE, error)
        } catch (error: IOException) {
            throw IllegalStateException(DAMAGED_FILE_MESSAGE, error)
        }
    }
}

internal class BookExtractException(
    val format: BookFormat,
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

private const val HEADER_BYTES = 1024

private val HTML_EXTENSIONS = setOf("html", "htm")
private val HTML_MIMES = setOf("text/html", "application/xhtml+xml")

private fun extractDetected(
    file: File,
    fileName: String?,
    mimeType: String?,
): ExtractedBook {
    val header = file.readHeader(HEADER_BYTES)
    val zipEntries = if (BookFormat.isZipSignature(header)) {
        runCatching { listZipEntryNames(file) }.getOrNull()
    } else {
        null
    }
    val format = BookFormat.detect(
        fileName = fileName,
        mimeType = mimeType,
        headerBytes = header,
        zipEntryNames = zipEntries,
    )
    val extracted = try {
        when (format) {
            BookFormat.Text -> extractText(file, fileName, mimeType, header)
            BookFormat.Fb2 -> parseOrDamaged(file, Fb2TextExtractor::extract)
            BookFormat.Epub -> parseOrDamaged(file, EpubTextExtractor::extract)
            BookFormat.Unknown -> error(BookTextExtractor.UNSUPPORTED_FORMAT_MESSAGE)
        }
    } catch (error: IllegalStateException) {
        throw BookExtractException(
            format = format,
            message = error.message ?: BookTextExtractor.DAMAGED_FILE_MESSAGE,
            cause = error,
        )
    }
    return extracted.copy(format = format)
}

private fun parseOrDamaged(
    file: File,
    parser: (InputStream) -> ExtractedBook,
): ExtractedBook {
    return try {
        file.inputStream().buffered().use(parser)
    } catch (error: IllegalStateException) {
        throw IllegalStateException(BookTextExtractor.DAMAGED_FILE_MESSAGE, error)
    }
}

private fun extractText(
    file: File,
    fileName: String?,
    mimeType: String?,
    header: ByteArray,
): ExtractedBook {
    val raw = file.readText(Charsets.UTF_8)
    val content = if (isHtmlSource(fileName, mimeType, header)) {
        HtmlToPlainText.convert(raw)
    } else {
        raw.replace("\r\n", "\n").replace('\r', '\n')
    }
    return ExtractedBook(title = null, content = content)
}

private fun isHtmlSource(fileName: String?, mimeType: String?, header: ByteArray): Boolean {
    val name = fileName?.substringAfterLast('/')?.substringAfterLast('\\')?.lowercase().orEmpty()
    val extension = name.substringAfterLast('.', missingDelimiterValue = "")
    if (extension in HTML_EXTENSIONS) return true
    val mime = mimeType?.substringBefore(';')?.trim()?.lowercase().orEmpty()
    if (mime in HTML_MIMES) return true
    return BookFormat.detect(headerBytes = header) == BookFormat.Text
}

private fun copyUriToTempFile(
    contentResolver: ContentResolver,
    uri: Uri,
    cacheDir: File,
): File {
    val temp = File.createTempFile("flashread-import-", ".tmp", cacheDir)
    try {
        val input = contentResolver.openInputStream(uri)
            ?: error(BookTextExtractor.UNABLE_TO_READ_MESSAGE)
        input.use { stream ->
            temp.outputStream().buffered().use { output ->
                stream.copyTo(output)
            }
        }
        return temp
    } catch (error: Throwable) {
        temp.delete()
        throw error
    }
}

private fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
    contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return null
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index < 0) return null
        return cursor.getString(index)
    }
    return null
}

private fun File.readHeader(maxBytes: Int): ByteArray {
    inputStream().use { input ->
        val buffer = ByteArray(maxBytes)
        var offset = 0
        while (offset < maxBytes) {
            val read = input.read(buffer, offset, maxBytes - offset)
            if (read <= 0) break
            offset += read
        }
        return if (offset == maxBytes) buffer else buffer.copyOf(offset)
    }
}

private fun listZipEntryNames(file: File): List<String> {
    val names = ArrayList<String>()
    ZipInputStream(file.inputStream().buffered()).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (!entry.isDirectory) names.add(entry.name)
            zip.closeEntry()
        }
    }
    return names
}
