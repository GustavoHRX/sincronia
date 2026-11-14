<div align="center">

<img src="docs/assets/banner.svg" alt="SincronIA: real alarms for Google Calendar and Google Tasks. Built with Claude." width="100%">

**An Android calendar with real alarms, in two-way sync with Google Calendar and Google Tasks.**

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-1565C0)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-0277BD)

🌐 **English** · [中文](README.zh-CN.md) · [हिन्दी](README.hi.md) · [Español](README.es.md) · [العربية](README.ar.md) · [Português](README.pt-BR.md)

</div>

---

Google Calendar reminders are easy to miss: a notification sound, a vibration, and it's gone. SincronIA turns your events and tasks into **alarms that actually wake you up**: full screen over the lock screen, ringing even in silent mode and Do Not Disturb, with Snooze and Dismiss. Everything you create or edit in the app goes back to Google, and everything you change on Google shows up in the app.

> **Note:** the app's interface is currently in Brazilian Portuguese. Translating it is a welcome contribution!

## Features

- **Today** screen: greeting, overdue tasks, all-day items, a timeline with a "now" line, and tasks completed today.
- **Agenda** (week strip + day list) and **Tasks** (all your Google Tasks lists, with subtasks).
- **Create, edit, delete** events and tasks, with two-way sync to Google.
- **Real alarms**: full screen when the phone is locked, a notification with Snooze/Dismiss when it's in use, sound in silent mode; alarms survive reboots, app updates and time zone changes.
- Turn off the alarm of a **whole recurring series** at once.
- **Offline first**: changes wait in a queue and go up by themselves when the internet is back.
- Settings: default alarm for new events, snooze length, alarm sound, daily reminder of untimed tasks, sync interval, permission shortcuts and a privacy-safe diagnostics export.

## Privacy

- **No backend.** The app talks directly to the Google Calendar and Google Tasks APIs; there is no SincronIA server.
- Only two scopes: `calendar.events` and `tasks`. No API key, no client secret.
- Your data stays on your phone (Room + DataStore). Android backup is turned off, and logs never contain tokens, e-mails or event titles.
- "Disconnect" revokes the access, cancels alarms and wipes the local data.

## Coming soon to Google Play

> 🚀 We're planning to release SincronIA on Google Play, free for everyone. Until then, you can build it yourself (see below).

## How to use

1. Open the app and **sign in with your Google account**, allowing access to Calendar and Tasks.
2. Allow **notifications** and **full-screen alarms** when the app asks. If something is missing, a banner on **Today** shows the shortcut.
3. Your events and tasks show up on **Today**, **Agenda** and **Tasks**. Pull down on Today to sync right away.
4. Tap **+** to create an event or a task and choose when the alarm rings: at the start or some minutes before (for tasks, a time).
5. When the alarm rings, tap **Snooze** or **Dismiss**. In **Settings** you change the default alarm, snooze length, sound and sync interval.

> 💡 For alarms and sync never to be delayed, set the app's battery usage to **Unrestricted** in your phone's settings.

## Build it yourself

Requirements: Android Studio (or JDK 17+) and the Android SDK (compileSdk 37). The app runs on Android 8.0 (API 26) or newer.

```bash
git clone https://github.com/GustavoHRX/sincronia.git
cd sincronia
./gradlew assembleDebug test lint
```

- **Demo mode (no Google account needed):** install the debug build and tap **"Entrar em modo demonstração"**. The app fills itself with sample events and tasks.
- **Sign in with Google:** create your own OAuth clients in Google Cloud (an *Android* client with your debug SHA-1 and a *Web* client), enable the Calendar and Tasks APIs, and put the Web client ID in `local.properties`:

  ```properties
  GOOGLE_WEB_CLIENT_ID=123456789-abc.apps.googleusercontent.com
  ```

  The step-by-step guide is in section 3.2 of [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md). Get your SHA-1 with `./gradlew signingReport`.
- **Release build:** create a `key.properties` at the project root pointing to your own keystore (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Without it, `assembleRelease` produces an unsigned APK.

> Never commit `local.properties`, `key.properties`, keystores, `google-services.json` or `client_secret*.json`. The `.gitignore` already blocks them.

## Tech stack

Kotlin · Jetpack Compose + Material 3 · Hilt · Room · DataStore · WorkManager · Retrofit + OkHttp + kotlinx.serialization · Credential Manager + `AuthorizationClient` · `AlarmManager.setAlarmClock()`

```
app/src/main/java/com/sincronia/app/
├── alarm/       alarm receivers, foreground service, full-screen activity, notifications
├── core/        auth, database (Room), network (Google APIs), alarm planning, logging
├── data/        repositories, sync pipeline (outbox + download), settings, demo data
├── navigation/  app navigation
└── ui/          Today, Agenda, Tasks, editor, settings, onboarding, permissions
```

## Documentation (in Portuguese)

| File | What it is |
|---|---|
| [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md) | Full specification: architecture, sync rules, alarms |
| [`docs/DECISOES.md`](docs/DECISOES.md) | Decisions and deviations from the spec, with dates and reasons |

## Contributing

Issues and pull requests are welcome. See [`CONTRIBUTING.md`](CONTRIBUTING.md).

## License

[MIT](LICENSE) © GustavoHRX
