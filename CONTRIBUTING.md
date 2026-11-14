# Contributing to SincronIA

Thanks for your interest! Issues and pull requests are welcome, in English or Portuguese.

## Reporting a bug

Open an issue with:
- what you did, what you expected and what happened;
- phone model and Android version;
- if possible, the file from **Ajustes → Exportar diagnóstico** (it has no e-mail, token or event title).

## Pull requests

1. Fork the repository and create a branch from `main`.
2. Read the relevant part of [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md), including the rules in section 11 (stack, scopes, security).
3. Keep the change focused. Code and comments in English; every user-facing string in `app/src/main/res/values/strings.xml`, in Brazilian Portuguese.
4. Run the full check before opening the PR:

   ```bash
   ./gradlew assembleDebug test lint
   ```

5. If you change a behavior described in the spec, add a dated entry to [`docs/DECISOES.md`](docs/DECISOES.md) explaining why.

## Translations

- The README exists in English, Chinese, Hindi, Spanish, Arabic and Portuguese. The translations were written with AI help: corrections by native speakers are very welcome.
- The app's interface is only in Brazilian Portuguese for now. A translation means adding `app/src/main/res/values-<language>/strings.xml`.

## Ground rules

- No backend, no extra Google scopes beyond `calendar.events` and `tasks`, no API keys or client secrets.
- Never commit `local.properties`, `key.properties`, keystores, `google-services.json` or `client_secret*.json`.
- Logs must never contain tokens, e-mails or event titles.
- Only stable library versions (no alpha, beta or RC).

By contributing, you agree that your contributions are licensed under the [MIT License](LICENSE).
