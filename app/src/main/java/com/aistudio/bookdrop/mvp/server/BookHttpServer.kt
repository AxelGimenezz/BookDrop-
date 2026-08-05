package com.aistudio.bookdrop.mvp.server

import com.aistudio.bookdrop.mvp.data.FileRepository
import fi.iki.elonen.NanoHTTPD
import java.io.FileInputStream
import java.net.URLDecoder
import java.net.URLEncoder

class BookHttpServer(
    hostname: String,
    port: Int = 8080,
    private val fileRepository: FileRepository
) : NanoHTTPD(hostname, port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        if (Method.GET != method) {
            return newFixedLengthResponse(
                Response.Status.METHOD_NOT_ALLOWED,
                MIME_PLAINTEXT,
                "Método no permitido"
            )
        }

        return try {
            when {
                uri == "/" || uri == "/index.html" -> handleIndex()
                uri == "/health" -> newFixedLengthResponse(
                    Response.Status.OK,
                    MIME_PLAINTEXT,
                    "BookDrop LAN OK"
                )
                uri.startsWith("/files/") -> {
                    val encodedId = uri.removePrefix("/files/")
                    val fileName = URLDecoder.decode(encodedId, "UTF-8")
                    handleDownload(fileName)
                }
                else -> newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    MIME_PLAINTEXT,
                    "404 No Encontrado"
                )
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Error interno del servidor"
            )
        }
    }

    private fun handleIndex(): Response {
        val files = fileRepository.getSharedFiles()
        val htmlBuilder = StringBuilder()
        htmlBuilder.append("<!doctype html>\n")
        htmlBuilder.append("<html>\n<head>\n")
        htmlBuilder.append("    <meta charset=\"utf-8\">\n")
        htmlBuilder.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
        htmlBuilder.append("    <title>BookDrop</title>\n")
        htmlBuilder.append("</head>\n<body>\n")
        htmlBuilder.append("    <h1>BookDrop</h1>\n")
        if (files.isEmpty()) {
            htmlBuilder.append("    <p>No hay archivos disponibles en este momento.</p>\n")
        } else {
            htmlBuilder.append("    <ul>\n")
            for (file in files) {
                val encodedName = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")
                val escapedDisplayName = escapeHtml(file.name)
                htmlBuilder.append("        <li><a href=\"/files/$encodedName\">$escapedDisplayName</a></li>\n")
            }
            htmlBuilder.append("    </ul>\n")
        }
        htmlBuilder.append("</body>\n</html>")

        return newFixedLengthResponse(
            Response.Status.OK,
            "text/html; charset=utf-8",
            htmlBuilder.toString()
        ).apply {
            addHeader("Cache-Control", "no-store")
            addHeader("X-Content-Type-Options", "nosniff")
        }
    }

    private fun handleDownload(fileName: String): Response {
        val file = fileRepository.getFileByName(fileName)
            ?: return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                MIME_PLAINTEXT,
                "404 Archivo no encontrado"
            )

        val mimeType = getMimeType(file.name)
        val fileLength = file.length()
        val fis = FileInputStream(file)

        val response = newFixedLengthResponse(
            Response.Status.OK,
            mimeType,
            fis,
            fileLength
        )

        val safeFilename = file.name.replace("\"", "\\\"")
        val encodedFilename = URLEncoder.encode(file.name, "UTF-8").replace("+", "%20")

        response.addHeader(
            "Content-Disposition",
            "attachment; filename=\"$safeFilename\"; filename*=UTF-8''$encodedFilename"
        )
        response.addHeader("Cache-Control", "no-store")
        response.addHeader("X-Content-Type-Options", "nosniff")
        return response
    }

    private fun getMimeType(fileName: String): String {
        val lower = fileName.lowercase()
        return when {
            lower.endsWith(".epub") -> "application/epub+zip"
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".mobi") -> "application/x-mobipocket-ebook"
            lower.endsWith(".azw3") -> "application/x-mobi8-ebook"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".cbz") -> "application/x-cbz"
            lower.endsWith(".cbr") -> "application/x-cbr"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".png") -> "image/png"
            else -> "application/octet-stream"
        }
    }

    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
