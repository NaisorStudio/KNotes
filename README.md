# KNotes 📒✨

Aplicación Android de **diarios/cuadernos digitales** escrita en **Kotlin** (100 % original,
arte y contenido generados proceduralmente por la propia app).

## La idea clave que pide la app

> **Los trazos siguen siendo editables y borrables después de guardar, salir del
> cuaderno y volver a entrar.**

Cómo se consigue: cada página almacena **objetos vectoriales** (trazos con lista de
puntos, cajas de texto, stickers-emoji, imágenes y enlaces) serializados en archivos
**JSON** por página (`filesDir/notebooks/<id>/p0.json`). Nada se aplasta en un bitmap,
así que al reabrir el diario puedes:

- **Borrar por zona** — la goma corta los trazos que toca, dejando los pedazos que no tocó.
- **Borrar trazos completos** — goma en modo "trazo".
- **Lazo** — rodea trazos/textos/imagenes y **muévelos**, **escálalos o elimínalos**.
- **Deshacer / rehacer** funcionan sobre cualquier cambio (40 pasos).

## Funciones

- **Cuadernos ilimitados** con portadas procedurales únicas (semilla aleatoria ⇒ cada portada es distinta).
- **Etiquetas** (`todos`, `trabajo`, …) + favoritos ⭐ + buscador.
- **Tipos de página:** rayado (papel crema con línea de margen como la captura), cuadrícula, puntos y en blanco — **con color de papel por página** (crema / blanco / oscuro, serializado en el JSON de cada página).
- **4 medios de trazo:** bolígrafo, rotulador, lápiz suave y **subrayador** translúcido (amarillo como el de la foto, con su paleta completa).
- **Goma** por zona o por trazo, **lazo** para mover/eliminar, **deshacer/rehacer**.
- **Interactividad extra:** texto tecleado, imágenes de la galería, stickers-emoji (Flores/Plantas/Cute/Símbolos/Comida) y **enlaces clicable** (🔗 tócalo para abrir; también copia la URL).
- **Zoom/pan** con dos dedos; indicador de página «1/2»; páginas ilimitadas por cuaderno; duplicar cuadernos, papelera con restaurar o borrar para siempre.
- **Guardado** con un toque (icono 💾) + autoguardado al salir (EditorActivity.onStop); 100 % local, sin internet ni cuentas.
- Mercados y Descubrir con plantillas listas para crear diarios (planners semanales, mensuales, habit tracker… con sticker packs de recuerdos en el editor) funcionando los botones «Usarlo».
- **Se ve bien en móvil y tablet:** rejilla adaptativa (2–4 columnas según ancho), portadas grandes en los diálogos con scroll, y `fitsSystemWindows` para no quedar debajo de la barra del sistema.

### Novedades v1.1

- 🐞 **Trazo visible en tiempo real (bug corregido):** el Path del trazo vivo se cacheara en su primer fotograma (un solo punto) y quedaba congelado hasta salir y volver a entrar. Ahora el trazo vivo nunca usa el caché (`strokePath` en `canvas/CanvasPageView.kt`).
- 🧰 **Barra del editor compacta:** herramientas siempre visibles (el menú lateral de acciones pasó a ⋮: nueva página, ver páginas, tipo de página, color del papel). Iconos 46dp con desplazamiento.
- 🧭 **Brújula del raíl = plantillas externas:** galería con 24 plantillas (vista previa detallada por tipo de página + color de papel), botón **Descargar** que crea el diario directamente, e **Importar archivo** (.json) para plantillas o respaldos de terceros.
- 💾 **Icono de la bolsa = respaldo:** crea respaldo de TODOS los diarios **en orden de creación**, eligiendo destino con el selector de Archivos (Descargas o Google Drive) — **JSON restaurable** (metas + páginas + imágenes en base64), TXT y HTML legibles. Además botón **Restaurar** que importa el .json sin pisar tus diarios existentes (los repetidos entran como «(restaurado)»).
- 📤 **Exportar notas** (menú ⋮ del editor): página actual a **PNG** y **JPG**, diario completo a **PDF** (A4, una hoja por página), **TXT** y **Word (.docx mínimo sin librerías)**.
- 🖥 Interfaz del editor con `fitsSystemWindows`; pantalla completa de página sin tapar.

### Novedades v1.3

- 🎨 **Paleta AMPLIA con feedback de elección:** rejilla de 36 colores + círculo de vista previa + animación de pulso al elegir, y **color personalizado** con deslizadores R/G/B (todo en el diálogo del lápiz).
- 🧽 **La goma borra TODO y sin crash:** trazos por zona o completos, y también **stickers, textos, imágenes y enlaces** si los toca. Reescrita para operar por objeto (adiós a los índices corridos que tumbaban la app al borrar mucho) y hacer UNA instantánea de deshacer por arrastre completo.
- ☀️🌙 **Modo claro y oscuro:** se elige manualmente (Automático/Claro/Oscuro) en Ajustes, se recuerda aunque se cierre la app, y «Automático» sigue al sistema; la barra de estado toma el color y el contraste correctos.
- 👩👨 **Modo Mujer / Hombre:** al abrir por primera vez pregunta «¿Cómo usarás KNotes?» y «¿Cómo te llamas?» (ambos quedan guardados). Mujer = acentos rosa y biblioteca romántica; Hombre = acentos AZUL, cuaderno NEGRO con AZUL como ícono del lanzador y biblioteca gym/fútbol/retos/gaming. Se cambia en Ajustes ⇒ «Cambiar sexo» (dos íconos alternativos vía activity-alias).
- 📔 **Título personalizado:** «Todos los diarios de ⟨tu nombre⟩».
- 🔖 **Fecha de creación en cada tarjeta**, debajo del nombre.
- ✂️ **Selección múltiple** (mantener pulsado un diario): barra con contador y acciones **Eliminar (papelera)**, **Mover** (favorito/duplicar/papelera), **Compartir** (genera PDF+PNG+JPG+TXT y abre el selector para compartir) e **Información** (creado / última edición de cada diario seleccionado).
- 🔔 **Actualizaciones (`io/Actualizaciones.kt`)**: marcado «Aqui va tu gist» — pega la URL RAW de un gist JSON `{"version":N,"name":"…","apk":"…"}` en `GIST_URL_HERE` y la app avisa y descarga por DownloadManager. Funciona igual de bien si no se configura.

### Novedades v1.4

- 👩👨 **«Elije tu Sexo» a PANTALLA COMPLETA:** el primer arranque (y ahora también Ajustes ⇒ «Cambiar sexo») muestra dos mitades — rosa 👩 con Mujer y azul 👨 con Hombre, con descripción de lo que incluye cada modo. Un toque y listo.
- ⚡ **Cambio de sexo en tiempo real:** al elegir una mitad desde Ajustes, TODA la app (raíl, barra inferior, ícono del lanzador, plantillas) se re-pinta en el instante.
- ⚙️ **Ajustes imposible de perder:** botón de engranaje nuevo en el raíl izquierdo (siempre visible) y la barra de abajo ahora muestra ETIQUETAS («Ajustes», «Diarios»…).
- 💙 **Interior 100 % adaptado al modo Hombre:** logo del raíl azul con ícono blanco, botón 🎁 azul suave, botón flotante «+» azul intenso, pestañas de Descubrir/Mercado con indicador azul, «Vaciar» de la papelera azul, flechas y botón 💾 del editor azules, y **portadas procedurales sin rosa** (los 3 estilos rosados se intercambian por noche/azul/mostaza en modo hombre).
- 🔵 Azul más intenso (`accent_blue_deep #4A90D9`) para que los íconos y FAB del modo hombre destaquen de verdad.

### Novedades v1.5.4 (cursor con manos largas: agrandar · achicar · girar)

- 🤏 **Nueva barra del cursor:** cuando tienes algo seleccionado en el papel aparecen 4 botones nuevos — **➕ agranda**, **➖ achica** (cada toque escala 15%, pica tantas veces como necesites) y **↻ / ↺ gira** de 15° en 15° hacia cualquier lado.
- 🎯 Funciona con TODO lo que puedas seleccionar: **textos, fotos, stickers, enlaces y trozos del dibujo (trazos)**. Los trazos se giran/escalan punto por punto (de verdad, no un embellecimiento) y el grosor escala con el dibujo.
- 🖼 Las **fotos mantienen su proporción exacta** al agrandarlas o achicarlas; los enlaces ahora también escalan su chip; los stickers llegan hasta 4× su tamaño original.
- 💾 Todo se **guarda con el elemento**: el giro y el tamaño siguen igual cuando cierras y abres el cuaderno (compatible con tus páginas viejas, sin cambios raros).

### Novedades v1.5.3 (límites del papel + imágenes a prueba de todo)

- ✉️ **El ícono «Crear respaldo (JSON)» pasó a ser UN SOBRE** (el título conserva el 💾 que marcaste).
- 📏 **El papel es EL LÍMITE:** ya no puedes escribir ni dibujar fuera del papel — si tocas fuera de la hoja, no pasa nada; si el trazo llega al borde, se detiene justito ahí (stickers, textos, enlaces y las goma también respetan el límite).
- 🌠 **Importación de imágenes 2× más fuerte:** usa el descodificador moderno del sistema (Android 9+) en adelante: **JPG más caprichoso, PNG, WEBP, HEIC/HEIF (fotos de iPhone/cámara), AVIF, BMP y GIF**. Si una decodificación falla, reintenta por el camino clásico. Además:
  - las fotos de cámara **giran solas** (EXIF) para verse derechas,
  - los PNG con transparencia ya **no se ponen negros** al convertirlos (fondo blanco),
  - y el mensaje de error ahora avisa los formatos soportados.

### Novedades v1.5.2 (arreglos de escritura/imagen/sticker)

- ✍️ **Textos ahora caen BIEN sobre las líneas:** la línea base del texto se ancla EXACTO al renglón del papel (antes la fuente de respaldo de algunos caracteres movía el renglón: uno arriba, otro abajo). Empezarán justo donde tocas y TODOS alineados por igual. Tus textos viejos guardados se ven igual que antes (compatibles).
- 🖼 **La imagen SÍ aparece:** ahora se coloca EN EL CENTRO DE LO QUE ESTÁS VIENDO del papel (antes iba al centro absoluto de la página y parecía que "no cargaba" porque quedaba fuera de la vista). Y si un archivo falla (formato raro) avisa con un mensaje en vez de quedarse callado.
- 🐱 **Modo sticker repetible:** elijes el sticker y luego tocas el papel TODAS las veces que quieras (cada toque pone uno). Para cambiar de sticker o apagar el modo: contacto CORTO en el botón 🖊 lo apaga, y MANTENER PRESIONADO el botón 🖊 vuelve a abrir el selector.

### Novedades v1.5.1 (ajustes de la primera prueba)

- 💾 **Ícono del respaldo → diskette** (el que marcaste con el círculo rojo en el diálogo, como el clásico 💾).
- ℹ️ **Información del diario COMPLETA y legible:** nombre, fecha de creación, última edición, **páginas** y **cuánto pesa** (KB/MB). De paso, el texto blanco-invisible de los diálogos en modo oscuro quedó arreglado a nivel global.
- 🖊 **Lápiz flotante mejor educado:** ARRASTRAR solo lo mueve (no abre nada), **mantener presionado NO abre nada**, y solo un **toque rápido** abre las herramientas (rueda de color, tipo de trazo y grosor).
- 🏷 **Título completo de inicio:** «Todos los diarios de …» ahora vive en su PROPIA fila debajo de los botones 🔍 🎁 ⋮ — ya no se recorta ni lo tapan.
- 🔗 **Enlaces con su nombre:** el chip muestra el NOMBRE que escribiste, centrado y con subrayado; con el CURSOR lo seleccionas para **borrarlo**.
- 🎛 **Rueda de color fuera del papel:** antes flotaba abajo tapando el diario; ahora vive **dentro de la configuración del lápiz** (toque rápido en el lápiz flotante), junto al **"Color elegido"** con pelotita + HEX que cambian AL INSTANTE al elegir color en la rueda o en la paleta.
- 📜 **Barra de texto con scroll:** en textos largos ya ves lo de arriba (campo con scroll interno) y el teclado ya no tapa la pantalla (`adjustResize`).

### Novedades v1.5

- 🚑 **Crash de Descubrir resuelto:** las tarjetas del feed usaban vistas nulas según el layout → ahora Descubrir carga secciones, plantillas y stickers sin cerrarse.
- ⚔️ **Corazón ↔ Espada:** el logo del raíl muestra ♥ dorado en modo Mujer y **ESPADA blanca** en modo Hombre (con los colores del modo).
- ✋🖊 **Palma/dedos NO manchan al escribir con lápiz:** si se detecta un STYLUS/táctil-lápiz, los toques de dedo/palma no dibujan nada (la pUNTA TRASERA del lápiz funciona como goma automática). Sin lápiz, dibujas con uno o varios dedos y el zoom de 2 dedos sigue igual.
- 🖱 **Herramienta CURSOR:** toca texto, trazo, imagen, sticker o enlace y muévelo con libertad (con manija para escalar); si tocas el vacío, habilita el lazo para marcar zona.
- 📝 **Texto que SIGUE las líneas:** la «T» abre una barra de escritura abajo; al confirmar, el texto se ENCAJA en el renglón del papel más cercano y continúa renglón por renglón al escribir salto de línea.
- 🎨 **Rueda de colores en vivo:** sub-barra con disco 12 colores × 4 intensidades (borde intenso → centro pastel), pulso al elegir y «Color elegido» con su HEX actualizándose AL INSTANTE (vale para texto y plumas).
- ✚ **Stickers con «Míos»:** pestaña nueva para AGREGAR los emojis que quieras (se guardan) y **CARGAR IMAGEN** (PNG/JPG) como sticker. Las celdas ya se ven siempre (antes quedaban en blanco por el tema del sistema).
- ✉️ **Respaldo con sobre y DRIVE:** el 💾 pasó a ✉️ y agregamos «☁️ Subir respaldo a Drive (Google)» — genera el JSON restaurable y lo comparte directo a tu Drive.
- 🖊 **Lápiz flotante reubicable:** muévelo donde no estorbe (recuerda la posición) y mantenlo presionado para el acceso rápido a plumón/color/grosor.
- 🏷 **Título y «⋮» robustos:** con nombre largo el título se recorta con «…» y los tres puntos, la lupa y el 🎁 NO desaparecen. Toca el título para SUBIR la rejilla («dame una opción de subir»).
- 📜 **Nombre en Ajustes en tiempo real:** al guardar «Tu nombre» el título cambia al instante (recreación rápida de la actividad).
- 🧽 **Texto blanco invisible corregido** en: campo de nombre, celdas de stickers y los banners «Plantillas y stickers» (colores oscuros fijos sobre papel claro).
- ✅ **Selección múltiple desde 1 diario:** los 4 botones (papelera/mover/compartir/info) operan con UNA tarjeta; al seleccionar, los banners bajos se esconden para ver bien, y la rejilla guarda espacio para el scroll inferior.

## Estructura del código

```
app/src/main/java/com/knotes/app/
├── KNotesApp.kt            # init de persistencia
├── MainActivity.kt         # raíl lateral + barra inferior + launchers de respaldo/importación
├── EditorActivity.kt       # barra de herramientas del editor + guardado + exportaciones
├── data/
│   ├── Model.kt            # Notebook / KPage(+paper) / Stroke / Text/Imagen/Link/Sticker
│   └── Store.kt            # JSON en filesDir + imágenes importadas + adoptNotebook
├── io/
│   └── ExportUtil.kt       # PNG/JPG/PDF/TXT/DOCX con destino SAF
├── canvas/
│   ├── Geo.kt              # distancias, polígono del lazo
│   └── CanvasPageView.kt   # papel interactivo (dibujo, goma, lazo, zoom, render para exportar)
└── ui/
    ├── HomeFragment.kt     # "Todos los diarios"
    ├── FeedFragment.kt     # Descubrir / Mercado
    ├── Catalog.kt          # plantillas + sticker packs (locales)
    ├── NotebookCoverView.kt# portadas procedurales
    ├── ProceduralCardView.kt# miniaturas de tarjetas
    ├── MiscFragments.kt    # Ajustes + Papelera
    ├── BackupUtil.kt       # respaldo JSON/TXT/HTML + restauración (ord. de creación)
    ├── TemplatesLibrary.kt # brújula: 24 plantillas con vista previa + descargar
    └── Panels.kt           # diálogos del editor (lápiz, texto, enlace…)
```

## Cómo compilar

```bash
# Requisitos: JDK 17+, Android SDK 36
gradle assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
```

El APK entregado (`app-debug.apk`) está firmado con la llave de depuración,
listo para instalar en el móvil (activa «instalar apps de fuentes desconocidas»).

## Nota de licencia/creación

Todo el código y el arte visual (portadas, miniaturas, iconos) son creaciones
originales generadas por Canvas/procedural — no se copió ningún recurso de otras apps.
