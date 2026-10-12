# SincronIA

App **Android** de agenda com **alarme de verdade** (tela cheia, toca no silencioso) que sincroniza nos dois sentidos com **Google Agenda** (eventos) e **Google Tasks** (tarefas). Sem backend, sem web, sem iOS: o app fala direto com as APIs do Google. Interface em pt-BR; código em inglês; pacote `com.sincronia.app`.

Especificação completa: `docs/ESPECIFICACAO.md`; leia a seção do marco atual antes de começar.
Estado atual e pendências manuais: `docs/PROGRESSO.md`. Decisões e desvios: `docs/DECISOES.md`.

## Comandos
```bash
./gradlew assembleDebug test lint   # verificação de cada marco
./gradlew signingReport             # SHA-1 para o Google Cloud
```

## Stack
Kotlin, Compose + Material 3, Hilt (+ hilt-work), Room 2.x (não `room3`), DataStore, Retrofit/OkHttp/kotlinx.serialization, Credential Manager + `AuthorizationClient`, WorkManager, `AlarmManager.setAlarmClock()`. Versões em `gradle/libs.versions.toml`; só estáveis.

## Regras
1. Siga os marcos M0…M7 **em ordem**; ao fim de cada um rode `./gradlew assembleDebug test lint` e faça **um commit** em português (ex.: `M3: login e leitura do Google`).
2. Atualize `docs/PROGRESSO.md` (marco atual + pendências manuais) a cada commit.
3. **Nunca** comitar `*.jks`, `*.keystore`, `key.properties`, `local.properties`, `.env`, `client_secret*.json`, `google-services.json`. O repositório é público.
4. Só os escopos `https://www.googleapis.com/auth/calendar.events` e `https://www.googleapis.com/auth/tasks` (URL completa). Sem API key nem client secret. Não chamar `calendars.get` nem `calendarList`.
5. **Não** usar `applicationIdSuffix` no debug (o cliente OAuth Android é de `com.sincronia.app`).
6. **Não** usar `GoogleSignInClient`, `androidx.room3`, versões alpha/beta/RC, backend, iOS ou web.
7. Toda string visível ao usuário em `res/values/strings.xml`, em pt-BR.
8. Registre decisões e desvios da especificação em `docs/DECISOES.md` (data + decisão + motivo).
9. Quando algo depender do Gustavo (Google Cloud, SHA-1, testar no celular), diga **exatamente** o que ele precisa fazer, com o comando pronto, e continue no que não depende dele (modo demonstração).
10. Prefira a solução mais simples que cumpra o "Pronto quando…" do marco. **Não** faça push nem crie repositório remoto sem o Gustavo pedir.
11. Nenhum token, e-mail ou título de evento em log.
