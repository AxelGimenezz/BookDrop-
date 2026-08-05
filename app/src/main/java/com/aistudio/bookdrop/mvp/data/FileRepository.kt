package com.aistudio.bookdrop.mvp.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileRepository(private val context: Context) {

    val sharedDir: File = File(context.filesDir, "shared").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    suspend fun copyUrisToShared(uris: List<Uri>): List<File> = withContext(Dispatchers.IO) {
        val copiedFiles = mutableListOf<File>()
        for (uri in uris) {
            val originalName = sanitizeFileName(getDisplayName(uri) ?: "archivo")
            val uniqueName = generateUniqueFileName(originalName)
            val destFile = File(sharedDir, uniqueName)

            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) {
                    copiedFiles.add(destFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (destFile.exists()) {
                    destFile.delete()
                }
            }
        }
        copiedFiles
    }

    private fun getDisplayName(uri: Uri): String? {
        var displayName: String? = null
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (columnIndex != -1) {
                        displayName = cursor.getString(columnIndex)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (displayName.isNullOrBlank()) {
            displayName = uri.lastPathSegment
        }
        return displayName?.takeIf { it.isNotBlank() }
    }

    private fun sanitizeFileName(value: String): String {
        val cleaned = value
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[\r\n\t\u0000-\u001F]"), "_")
            .trim()
        return cleaned.takeIf { it.isNotBlank() && it != "." && it != ".." } ?: "archivo"
    }

    private fun generateUniqueFileName(originalName: String): String {
        val file = File(sharedDir, originalName)
        if (!file.exists()) {
            return originalName
        }

        val dotIndex = originalName.lastIndexOf('.')
        val baseName = if (dotIndex > 0) originalName.substring(0, dotIndex) else originalName
        val extension = if (dotIndex > 0) originalName.substring(dotIndex) else ""

        var counter = 1
        while (true) {
            val candidateName = "${baseName}_$counter$extension"
            if (!File(sharedDir, candidateName).exists()) {
                return candidateName
            }
            counter++
        }
    }

    @Synchronized
    fun getSharedFiles(): List<File> {
        return sharedDir.listFiles()?.filter { it.isFile }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    @Synchronized
    fun deleteFile(fileName: String): Boolean {
        val file = getFileByName(fileName) ?: return false
        return try {
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @Synchronized
    fun getFileByName(fileName: String): File? {
        val targetFile = File(sharedDir, fileName)
        return try {
            val canonicalShared = sharedDir.canonicalPath
            val canonicalTarget = targetFile.canonicalPath
            val isInsideSharedDirectory = canonicalTarget.startsWith(canonicalShared + File.separator)
            if (targetFile.exists() && targetFile.isFile && isInsideSharedDirectory) {
                targetFile
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
