<div align="center">

<img src="docs/assets/banner.svg" alt="SincronIA: alarmas de verdad para Google Calendar y Google Tasks. Hecho con Claude." width="100%">

**Una agenda Android con alarmas de verdad, sincronizada en ambos sentidos con Google Calendar y Google Tasks.**

[![Licencia: MIT](https://img.shields.io/badge/licencia-MIT-blue.svg)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-1565C0)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-0277BD)

🌐 [English](README.md) · [中文](README.zh-CN.md) · [हिन्दी](README.hi.md) · **Español** · [العربية](README.ar.md) · [Português](README.pt-BR.md)

</div>

---

Los recordatorios de Google Calendar pasan desapercibidos: un sonido de notificación, una vibración y listo. SincronIA convierte tus eventos y tareas en **alarmas que de verdad te despiertan**: a pantalla completa sobre la pantalla de bloqueo, sonando incluso en modo silencio y en "No molestar", con Posponer y Descartar. Todo lo que creas o editas en la app vuelve a Google, y todo lo que cambias en Google aparece en la app.

> **Nota:** por ahora la interfaz de la app está en portugués de Brasil. ¡Traducirla es una contribución muy bienvenida!

## Funciones

- Pantalla **Hoy**: saludo, tareas atrasadas, elementos de todo el día, una línea de tiempo con la marca "ahora" y las tareas completadas del día.
- **Agenda** (franja de la semana + lista del día) y **Tareas** (todas tus listas de Google Tasks, con subtareas).
- **Crear, editar y eliminar** eventos y tareas, con sincronización en ambos sentidos.
- **Alarmas de verdad**: pantalla completa con el teléfono bloqueado, notificación con Posponer/Descartar cuando lo estás usando, sonido en modo silencio; las alarmas siguen después de reiniciar, actualizar la app o cambiar de zona horaria.
- Desactivar la alarma de una **serie recurrente completa** de una sola vez.
- **Funciona sin conexión**: los cambios esperan en una cola y se suben solos cuando vuelve internet.
- Ajustes: alarma predeterminada para eventos nuevos, duración de la pospuesta, sonido de la alarma, recordatorio diario de tareas sin hora, intervalo de sincronización, accesos directos a permisos y un diagnóstico exportable sin datos personales.

## Privacidad

- **Sin servidor.** La app habla directamente con las API de Google Calendar y Google Tasks; no existe un servidor de SincronIA.
- Solo dos permisos (scopes): `calendar.events` y `tasks`. Sin API key ni client secret.
- Tus datos se quedan en tu teléfono (Room + DataStore). La copia de seguridad de Android está desactivada, y los registros nunca contienen tokens, correos ni títulos de eventos.
- "Desconectar" revoca el acceso, cancela las alarmas y borra los datos locales.

## Muy pronto en Google Play

> 🚀 Estamos planeando lanzar SincronIA en Google Play, gratis para todos. Mientras tanto, puedes compilarlo tú mismo (ver abajo).

## Cómo usarlo

1. Abre la app e **inicia sesión con tu cuenta de Google**, permitiendo el acceso a Calendar y Tasks.
2. Permite las **notificaciones** y las **alarmas a pantalla completa** cuando la app lo pida. Si falta alguna, una franja en **Hoy** muestra el acceso directo.
3. Tus eventos y tareas aparecen en **Hoy**, **Agenda** y **Tareas**. Desliza hacia abajo en Hoy para sincronizar al instante.
4. Toca **+** para crear un evento o una tarea y elige cuándo suena la alarma: al inicio o unos minutos antes (en las tareas, una hora).
5. Cuando suene la alarma, toca **Posponer** o **Descartar**. En **Ajustes** cambias la alarma predeterminada, la pospuesta, el sonido y el intervalo de sincronización.

> 💡 Para que las alarmas y la sincronización nunca se retrasen, configura el uso de batería de la app como **Sin restricciones** en los ajustes del teléfono.

## Compilarlo tú mismo

Requisitos: Android Studio (o JDK 17+) y el Android SDK (compileSdk 37). La app funciona en Android 8.0 (API 26) o superior.

```bash
git clone https://github.com/GustavoHRX/sincronia.git
cd sincronia
./gradlew assembleDebug test lint
```

- **Modo demostración (sin cuenta de Google):** instala la versión debug y toca **"Entrar em modo demonstração"**. La app se llena de eventos y tareas de ejemplo.
- **Iniciar sesión con Google:** crea tus propios clientes OAuth en Google Cloud (un cliente *Android* con tu SHA-1 de debug y un cliente *Web*), activa las API de Calendar y Tasks y pon el ID del cliente Web en `local.properties`:

  ```properties
  GOOGLE_WEB_CLIENT_ID=123456789-abc.apps.googleusercontent.com
  ```

  La guía paso a paso está en la sección 3.2 de [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md). Obtén tu SHA-1 con `./gradlew signingReport`.
- **Versión release:** crea un `key.properties` en la raíz del proyecto que apunte a tu propio keystore (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Sin él, `assembleRelease` genera un APK sin firmar.

> Nunca hagas commit de `local.properties`, `key.properties`, keystores, `google-services.json` ni `client_secret*.json`. El `.gitignore` ya los bloquea.

## Tecnologías

Kotlin · Jetpack Compose + Material 3 · Hilt · Room · DataStore · WorkManager · Retrofit + OkHttp + kotlinx.serialization · Credential Manager + `AuthorizationClient` · `AlarmManager.setAlarmClock()`

```
app/src/main/java/com/sincronia/app/
├── alarm/       receptores de alarma, servicio en primer plano, pantalla completa, notificaciones
├── core/        inicio de sesión, base de datos (Room), red (API de Google), cálculo de alarmas, registros
├── data/        repositorios, sincronización (cola + descarga), ajustes, datos de demostración
├── navigation/  navegación
└── ui/          Hoy, Agenda, Tareas, editor, ajustes, onboarding, permisos
```

## Documentación (en portugués)

| Archivo | Qué es |
|---|---|
| [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md) | Especificación completa: arquitectura, reglas de sincronización, alarmas |
| [`docs/DECISOES.md`](docs/DECISOES.md) | Decisiones y desvíos de la especificación, con fecha y motivo |

## Contribuir

Los issues y pull requests son bienvenidos. Consulta [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Licencia

[MIT](LICENSE) © GustavoHRX
