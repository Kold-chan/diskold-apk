# 📱 Diskold APK — Instrucciones

## ¿Qué hace este APK diferente a la PWA?

| Función | PWA/Web | APK |
|---|---|---|
| Burbuja sobre otras apps | ❌ No es posible | ✅ Igual a Discord |
| Notificación con botón Colgar | ❌ | ✅ |
| Micrófono en segundo plano | ⚠️ Depende del dispositivo | ✅ Garantizado |
| Wake lock real (CPU despierta) | ⚠️ Solo pantalla | ✅ CPU completa |
| Funciona offline (página de error) | ❌ | ✅ |

---

## Paso 1 — Cambiar la URL del servidor

Abre este archivo y cambia la URL por la de tu servidor:

```
android-app/src/main/java/com/diskold/app/MainActivity.java
```

Línea 17:
```java
public static final String SERVER_URL = "https://TU-APP.onrender.com";
//                                              ↑ CAMBIA ESTO
```

---

## Paso 2 — Obtener el APK con GitHub Actions (recomendado, sin instalar nada)

### 2a. Sube el proyecto a GitHub

1. Ve a [github.com/new](https://github.com/new)
2. Crea un repositorio **privado** llamado `diskold-apk`
3. Sube los archivos de esta carpeta:

```bash
cd diskold-apk-project
git init
git add .
git commit -m "Diskold APK v4.4"
git remote add origin https://github.com/TU_USUARIO/diskold-apk.git
git push -u origin main
```

### 2b. GitHub compila el APK automáticamente

- Ve a tu repositorio → pestaña **Actions**
- Verás el workflow "Build Diskold APK" corriendo
- Espera ~3-5 minutos
- Cuando termine, haz clic en el workflow → **Artifacts** → descarga **Diskold-APK**

### 2c. Instala el APK en tu Android

1. Transfiere el `.apk` a tu teléfono
2. En Ajustes → Seguridad → activa **"Instalar apps de fuentes desconocidas"**
3. Toca el archivo `.apk` para instalar

---

## Paso 3 — Conceder permiso de burbuja flotante

Al abrir la app por primera vez, Diskold pedirá permiso de **"Mostrar sobre otras apps"**.

También puedes hacerlo manualmente:
- Ajustes → Apps → Diskold → Permisos especiales → Mostrar sobre otras apps → ✅ Activar

---

## Cómo funciona la burbuja

Cuando estás en una llamada de voz y cambias de app:

```
┌─────────────────────────────────────────┐
│ YouTube / Instagram / cualquier app     │
│                                         │
│                              ┌────┐     │
│                              │ 🎤 │ ←── burbuja de Diskold  │
│                              └────┘     │
│              (arrastra para moverla)    │
└─────────────────────────────────────────┘

Toca la burbuja para ver:
┌──────────────────┐
│ 🟢 EN LLAMADA    │
│ # general        │
│ 🎤 Silenciar     │
│ 📵 Colgar        │
│ ↩ Abrir Diskold  │
└──────────────────┘
```

---

## Compilar localmente (opcional, sin GitHub)

Requisitos: Android Studio o Android SDK + Java 17

```bash
cd diskold-apk-project

# Download gradle wrapper
mkdir -p gradle/wrapper
curl -sL "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar" \
  -o gradle/wrapper/gradle-wrapper.jar

chmod +x gradlew
./gradlew :android-app:assembleDebug

# APK generado en:
# android-app/build/outputs/apk/debug/android-app-debug.apk
```

---

## Estructura del proyecto

```
diskold-apk-project/
├── .github/workflows/build-apk.yml   ← GitHub Actions (compila el APK)
├── android-app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml       ← Permisos (SYSTEM_ALERT_WINDOW, etc.)
│   │   ├── java/com/diskold/app/
│   │   │   ├── MainActivity.java     ← WebView principal ← EDITA LA URL AQUÍ
│   │   │   ├── BubbleService.java    ← Burbuja flotante nativa
│   │   │   ├── VoiceService.java     ← Foreground service de voz
│   │   │   └── DiskoldBridge.java    ← Bridge JS ↔ Android
│   │   └── res/
│   │       ├── layout/activity_main.xml
│   │       ├── values/styles.xml
│   │       └── mipmap-*/ic_launcher.png
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── gradlew
```
