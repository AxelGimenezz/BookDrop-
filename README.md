<h1 align="center">BookDrop</h1>

<p align="center">
  <strong>Del celular a un ebook viejo, sin cables, nube ni vueltas.</strong>
</p>

<p align="center">
  <img alt="Estado: MVP funcional" src="https://img.shields.io/badge/estado-MVP%20funcional-2ea44f">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-1.9.24-7F52FF?logo=kotlin&logoColor=white">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack-Compose-4285F4">
  <img alt="Licencia MIT" src="https://img.shields.io/badge/licencia-MIT-0A7EA4">
</p>

---

BookDrop es una aplicación Android que convierte temporalmente el teléfono en un servidor HTTP local. Permite compartir libros y otros archivos con cualquier dispositivo que tenga navegador y esté conectado a la misma red Wi-Fi.

## El problema

El problema era simple, pero resolverlo se había vuelto innecesariamente complicado: el libro ya estaba en mi celular, pero para pasarlo al ebook tenía que buscar un adaptador para la Mac, conectar un cable micro-USB y usar la computadora como intermediaria. Demasiadas vueltas para mover un EPUB.

El ebook quedó viejo: no tiene Bluetooth, su tienda ya no sirve y conectarlo por cable es cada vez menos práctico. Pero todavía tiene Wi-Fi y navegador.

Ahí apareció la solución. En lugar de seguir acumulando adaptadores o jubilar un aparato que todavía funciona, hice que el celular le mostrara una página web local con los archivos disponibles.

## Cómo se usa

1. Seleccionás uno o varios archivos desde Android.
2. Tocás **Iniciar servidor**.
3. BookDrop detecta la red y muestra una dirección como:

   ```text
   http://192.168.1.34:8080
   ```

4. Abrís esa dirección desde el ebook u otro dispositivo.
5. Tocás el archivo y lo descargás.

No requiere cuentas, cables, almacenamiento en la nube ni instalar nada en el dispositivo receptor.

## Qué incluye

- Selección múltiple mediante el explorador de Android.
- Servidor HTTP dentro de la red local.
- Detección automática de una dirección IPv4 LAN.
- Copia rápida de la URL.
- Descarga de archivos mediante streaming.
- Interfaz web sin JavaScript, compatible con navegadores antiguos.
- Eliminación individual de archivos compartidos.
- Soporte para EPUB, PDF, MOBI, AZW3, imágenes y otros formatos.

## Cómo está hecho

| Componente | Implementación |
|---|---|
| Aplicación Android | Kotlin y Jetpack Compose |
| Gestión de archivos | Storage Access Framework y almacenamiento privado |
| Detección de red | Selección de IPv4 LAN y descarte de VPN, datos móviles y loopback |
| Servidor local | NanoHTTPD |
| Transferencia | Streaming de archivos sin cargarlos completos en memoria |
| Interfaz receptora | HTML mínimo, sin frameworks ni recursos externos |

El alcance se mantuvo deliberadamente chico: resolver bien la transferencia local antes de agregar cuentas, sincronización o funciones que el problema original no necesitaba.

## Descargar

[**Descargar BookDrop v1.1.0**](APK/BookDrop-v1.1.0-debug.apk)

El APK disponible es una compilación de prueba. Android puede solicitar autorización para instalar aplicaciones desde el navegador o el gestor de archivos.

> [!IMPORTANT]
> BookDrop está pensado para redes domésticas o confiables. Esta versión no implementa autenticación ni HTTPS. Conviene detener el servidor después de transferir los archivos.

## Compilar

Requiere Android Studio y JDK 17.

```bash
./gradlew :app:assembleDebug
```

El APK se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Estado actual

El MVP fue compilado, instalado y probado entre un teléfono Android y un ebook antiguo dentro de una red Wi-Fi real.

La función principal está resuelta. Menos cables, menos vueltas y Lovecraft otra vez en el ebook.

## Desarrollo asistido por IA

Construí BookDrop con apoyo de Codex y OpenCode como asistentes de programación. Los utilicé para acelerar la implementación y revisar alternativas, mientras que la definición del problema, el alcance del producto, las decisiones técnicas, la compilación, la depuración de la red LAN y las pruebas en dispositivos físicos formaron parte de mi trabajo.

El criterio detrás del proyecto fue simple: detectar una fricción concreta, reducirla a un MVP útil y comprobar que funcionara fuera del editor.

## Licencia

BookDrop se distribuye bajo la [licencia MIT](LICENSE).
