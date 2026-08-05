package com.aistudio.bookdrop.mvp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.bookdrop.mvp.data.FileRepository
import com.aistudio.bookdrop.mvp.network.NetworkUtils
import com.aistudio.bookdrop.mvp.server.BookHttpServer
import com.aistudio.bookdrop.mvp.ui.theme.BookDropTheme
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.launch
import java.io.File

private const val SERVER_PORT = 8080

data class ServerConnection(
    val url: String,
    val networkLabel: String
)

class MainActivity : ComponentActivity() {

    private lateinit var fileRepository: FileRepository
    private var server: BookHttpServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fileRepository = FileRepository(applicationContext)

        setContent {
            BookDropTheme {
                BookDropApp(
                    fileRepository = fileRepository,
                    onStartServer = { startLanServer() },
                    onStopServer = { stopServer() },
                    isServerRunning = { server?.isAlive == true }
                )
            }
        }
    }

    private fun startLanServer(): Result<ServerConnection> {
        if (server?.isAlive == true) {
            return Result.failure(IllegalStateException("El servidor ya está activo"))
        }

        // La URL debe provenir de Wi-Fi/Ethernet, nunca de datos móviles ni de una VPN.
        val lanAddress = NetworkUtils.getLanAddress(applicationContext)
            ?: return Result.failure(
                IllegalStateException(
                    "No se encontró una red LAN. Conectá este teléfono y el otro dispositivo a la misma red Wi-Fi."
                )
            )

        return try {
            val newServer = BookHttpServer(
                hostname = lanAddress.ip,
                port = SERVER_PORT,
                fileRepository = fileRepository
            )
            // El socket queda enlazado únicamente a la interfaz LAN seleccionada.
            newServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            server = newServer

            Result.success(
                ServerConnection(
                    url = "http://${lanAddress.ip}:$SERVER_PORT",
                    networkLabel = "${lanAddress.transportName} · ${lanAddress.interfaceName}"
                )
            )
        } catch (exception: Exception) {
            server?.stop()
            server = null
            Result.failure(
                IllegalStateException(
                    "No se pudo abrir el puerto $SERVER_PORT: ${exception.message ?: "error desconocido"}",
                    exception
                )
            )
        }
    }

    private fun stopServer() {
        try {
            server?.stop()
        } finally {
            server = null
        }
    }

    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDropApp(
    fileRepository: FileRepository,
    onStartServer: () -> Result<ServerConnection>,
    onStopServer: () -> Unit,
    isServerRunning: () -> Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var serverActive by remember { mutableStateOf(isServerRunning()) }
    var connection by remember { mutableStateOf<ServerConnection?>(null) }
    var fileList by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoadingFiles by remember { mutableStateOf(false) }

    fun refreshFiles() {
        fileList = fileRepository.getSharedFiles()
    }

    fun copyUrl(url: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("URL BookDrop LAN", url))
        Toast.makeText(context, "URL copiada", Toast.LENGTH_SHORT).show()
    }

    LaunchedEffect(Unit) {
        refreshFiles()
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isLoadingFiles = true
            scope.launch {
                val copied = fileRepository.copyUrisToShared(uris)
                isLoadingFiles = false
                refreshFiles()
                snackbarHostState.showSnackbar(
                    if (copied.isNotEmpty()) {
                        "Se agregaron ${copied.size} archivo(s)"
                    } else {
                        "No se pudieron copiar los archivos"
                    }
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ServerCard(
                serverActive = serverActive,
                connection = connection,
                onCopyUrl = ::copyUrl,
                onStart = {
                    onStartServer()
                        .onSuccess {
                            serverActive = true
                            connection = it
                        }
                        .onFailure { exception ->
                            serverActive = false
                            connection = null
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    exception.message ?: "Error al iniciar el servidor"
                                )
                            }
                        }
                },
                onStop = {
                    onStopServer()
                    serverActive = false
                    connection = null
                }
            )

            Button(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("add_files_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Agregar archivos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isLoadingFiles) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copiando archivos...")
                }
            }

            Text(
                text = "Archivos compartidos (${fileList.size})",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (fileList.isEmpty() && !isLoadingFiles) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay archivos agregados.",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(fileList, key = { it.name }) { file ->
                        FileItemRow(
                            file = file,
                            onDelete = {
                                if (fileRepository.deleteFile(file.name)) {
                                    refreshFiles()
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("No se pudo eliminar el archivo")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerCard(
    serverActive: Boolean,
    connection: ServerConnection?,
    onCopyUrl: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (serverActive) {
                                androidx.compose.ui.graphics.Color(0xFF22C55E)
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                            shape = CircleShape
                        )
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (serverActive) "Servidor LAN activo" else "Servidor detenido",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("server_status_text")
                )
            }

            if (serverActive && connection != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "URL LAN",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = connection.url,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("local_url_text")
                            )
                            IconButton(
                                onClick = { onCopyUrl(connection.url) },
                                modifier = Modifier.testTag("copy_url_button")
                            ) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "Copiar URL"
                                )
                            }
                        }
                        Text(
                            text = connection.networkLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "Conectá ambos dispositivos a la misma red Wi-Fi. La app no publicará una IP de datos móviles ni de VPN.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onStart,
                    enabled = !serverActive,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("start_server_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Iniciar", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onStop,
                    enabled = serverActive,
                    shape = RoundedCornerShape(100.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("stop_server_button")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Detener", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun FileItemRow(
    file: File,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = file.name,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatFileSize(file.length()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_file_${file.name}")
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar archivo",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val group = (Math.log10(bytes.toDouble()) / Math.log10(1024.0))
        .toInt()
        .coerceIn(0, units.lastIndex)
    return String.format(
        "%.1f %s",
        bytes / Math.pow(1024.0, group.toDouble()),
        units[group]
    )
}
