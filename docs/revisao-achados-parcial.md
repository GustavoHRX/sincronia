# SincronIA · Achados da revisão (verificados)

> Revisão feita em 29/09/2026 por 4 revisores automáticos (APIs Google, Android, Sync e dados, Escopo MVP / Claude Code). Em 30/09/2026 os achados de alta prioridade foram conferidos na documentação oficial e as correções entraram na **especificação v2** (`integracao-google-agenda-tasks.md`). O prompt do Figma foi revisado à parte (ver `PROGRESSO.md`).
> Os textos de cada achado vieram dos revisores (alguns em inglês). Este arquivo fica como registro; a fonte da verdade agora é a especificação.

## Resultado da verificação (30/09/2026)

Nenhum achado de alta prioridade foi refutado. Em alguns casos a especificação usa uma solução mais simples que a sugerida, e 3 detalhes não puderam ser confirmados na documentação (indicados abaixo).

| Achados | Tema | Veredito | Conferido em | Como ficou na spec v2 |
|---|---|---|---|---|
| 1, 4, 6 | Cliente OAuth Web obrigatório no login | ✅ Confirmado | Credential Manager: "The webClientId is the Web Client ID you set up for OAuth"; a página de autorização manda criar os clientes Android e "Web application" | 3.2, 6.3, 6.4 e regra 5. O texto exato do erro "[28444]" não foi confirmado, então a spec cita só o tipo de exceção |
| 2 | Som do alarme no serviço | ✅ Confirmado | Notificações urgentes: heads-up "while the user actively uses the device"; Android 17: áudio em segundo plano exige Activity visível ou FGS que não seja `shortService`, e a falha é silenciosa | 7.4 ("Como tocar"), tipo `mediaPlayback` |
| 3, 11 | `SCHEDULE_EXACT_ALARM` no Android 12/12L e permissões negadas | ✅ Confirmado | Alarmes exatos (API 31–32 exige `SCHEDULE_EXACT_ALARM`); limites de tela cheia (sem a permissão, heads-up por 60 s); Doze (Play proíbe o pedido direto de isenção; `setAlarmClock` toca no Doze) | 7.5. O `maxSdkVersion="32"` vem da referência de `USE_EXACT_ALARM` citada pelo revisor; a página de alarmes não fala disso |
| 5, 7, 12 | Janela da Agenda não anda com `syncToken` | ✅ Confirmado | `events.list`: `syncToken` não aceita `timeMin`/`timeMax` | 7.2: carga da janela inteira a cada sync, com reconciliação e sem `syncToken` no MVP (a solução mais simples, do achado 7) |
| 8, 14 | Tela Hoje indefinida; datas com fuso | ✅ Confirmado | Tasks: "Only date information is recorded"; Event: fim exclusivo | F4, 7.3 ("Datas") e testes obrigatórios |
| 9 | Sem modo demonstração, o agente trava | ✅ Confirmado (consistência interna) | — | 6.3, marcos reordenados (M1 e M2 não dependem do Google) e regra 9 |
| 10 | Ambiente e versões sem âncora | ✅ Confirmado, com ajuste | AGP 9 (Kotlin embutido, Gradle 9.1+, JDK 17); Dagger 2.59 (Hilt exige AGP 9); Room 2.8.5 estável e `room3` sem versão estável; `play-services-auth` 22.0.0 com `revokeAccess` | 3.1 e 5. Target SDK: no mínimo 36 (exigência do Play desde 31/08/2026), sem fixar o 37 |
| 13 | Alarmes no passado, horizonte e limite | ✅ Confirmado | Comportamento documentado do AlarmManager (horário passado dispara na hora). O limite de 500 alarmes por app não é oficial | 7.4: janela de 7 dias, no máximo 50 alarmes |
| 15 | Outbox: ordem e idempotência | ✅ Confirmado | `events.insert` aceita id do cliente (base32hex); a Tasks API não aceita | 7.2.1 |
| 16 | Conflito comparando relógios diferentes | ✅ Confirmado, com solução simplificada | Recurso Event (`updated`) | 7.2: alteração local pendente vence e o PATCH envia só os campos alterados, sem comparar relógios nem usar `If-Match` no MVP |
| 17 | Paginação e `updatedMin` do Tasks | ✅ Confirmado | `tasks.list`: "The default is 20 (max allowed: 100)"; `showHidden`/`showDeleted` desligados por padrão | 7.2 |

**Média e baixa prioridade**: aplicados na spec v2, com estas diferenças: 19 (Direct Boot ficou para depois do MVP, como limitação conhecida), 23 (sem fixar o target 37), 34 e 35 (política de conflito simplificada, como acima), 38 (o arquivo manteve o nome; o Claude Code copia a spec para `docs/ESPECIFICACAO.md` e cria o `CLAUDE.md`), 46 (ocorrências de recorrentes ficaram somente leitura, em vez de editáveis) e 47 (sem a checagem do pacote no Play).

**Total: 48 achados** · 🔴 17 alta · 🟠 27 média · 🟡 4 baixa

| # | Severidade | Frente | Achado | Seção |
|---|---|---|---|---|
| 1 | 🔴 Alta | Android | `web-client-id-obrigatorio` | 3. Pré-requisitos / 6. Fluxo de autorização passo 1 / 10. Fi |
| 2 | 🔴 Alta | Android | `som-do-alarme-no-servico` | 7.4 Regras de alarme (linha Disparo) e 7.5 Permissões |
| 3 | 🔴 Alta | Android | `schedule-exact-alarm-api31-32` | 7.5 Permissões (AndroidManifest) |
| 4 | 🔴 Alta | APIs Google | `web-client-id-credential-manager` | 3. Pré-requisitos; 6. Fluxo de autorização (passo 1); 10. Pr |
| 5 | 🔴 Alta | APIs Google | `calendar-sync-window-slides` | 7.2 Regras de sync (linhas 'Agenda, primeira vez' / 'Agenda, |
| 6 | 🔴 Alta | Escopo MVP / Claude Code | `credential-manager-exige-web-client-id` | 3. Pré-requisitos / 5. Stack / 10. Preparação para SaaS |
| 7 | 🔴 Alta | Escopo MVP / Claude Code | `janela-sync-nao-desliza-recorrentes` | 7.2 Regras de sync / 4. Fora do MVP |
| 8 | 🔴 Alta | Escopo MVP / Claude Code | `hoje-indefinido-e-due-date-fuso` | 4. F4 / 7.3 Evento x Tarefa / 8. M3 / 6. core/time |
| 9 | 🔴 Alta | Escopo MVP / Claude Code | `sem-modo-demo-agente-trava` | 6. Arquitetura / 8. Marcos / 11. Regra 7 |
| 10 | 🔴 Alta | Escopo MVP / Claude Code | `ambiente-e-versoes-nao-ancorados` | 3. Pré-requisitos / 5. Stack |
| 11 | 🔴 Alta | Escopo MVP / Claude Code | `alarme-exato-android-12-e-permissoes-negadas` | 7.5 Permissões (e tela 2 do prompt do Figma) |
| 12 | 🔴 Alta | Sync e dados | `agenda-sync-incremental-e-janela` | 7.2 Regras de sync (linhas 'Agenda, primeira vez' e 'Agenda, |
| 13 | 🔴 Alta | Sync e dados | `alarmes-passado-horizonte-limite` | 7.4 Regras de alarme (Reboot / Sempre após sync) + F9 |
| 14 | 🔴 Alta | Sync e dados | `datas-sem-hora-fuso` | 7.1 EventEntity/TaskEntity + 7.3 (linha 'Hora') |
| 15 | 🔴 Alta | Sync e dados | `outbox-ordem-idempotencia` | 7.1 OutboxEntity + 7.2 (Ordem/Erros) |
| 16 | 🔴 Alta | Sync e dados | `conflito-lww-relogio` | 7.2 Regras de sync (linha 'Conflito') |
| 17 | 🔴 Alta | Sync e dados | `tasks-paginacao-updatedmin` | 7.2 Regras de sync (linha 'Tasks') + 7.1 SyncMetaEntity |
| 18 | 🟠 Média | Android | `canal-categoria-fsi-dnd` | 7.4 Regras de alarme / 7.5 onboarding / 6 (SincroniaApp: can |
| 19 | 🟠 Média | Android | `boot-directboot-goasync` | 7.4 Regras de alarme (linha Reboot / troca de hora) |
| 20 | 🟠 Média | Android | `alarmactivity-lockscreen-pendingintent` | 7.4 Regras de alarme / 6. Arquitetura (alarm/) |
| 21 | 🟠 Média | Android | `401-cleartoken-reconsent` | 6. Fluxo de autorização, passos 5 e 6 |
| 22 | 🟠 Média | Android | `desconectar-cancelar-alarmes` | 12. Checklist (Desconectar) / M1 |
| 23 | 🟠 Média | Android | `versoes-agp9-hilt-target` | 5. Stack |
| 24 | 🟠 Média | Android | `hilt-work-initializer` | 5. Stack (Segundo plano) / 6. Arquitetura (SincroniaApp) / 7 |
| 25 | 🟠 Média | Android | `otimizacao-bateria-politica` | TELAS 2. Permissões (e spec 7.5 / F10) |
| 26 | 🟠 Média | APIs Google | `tasks-pagination-updatedmin-lists` | 7.2 Regras de sync (linha 'Tasks') |
| 27 | 🟠 Média | APIs Google | `tasks-due-utc-date-shift` | 7.3 Evento x Tarefa na API (linha 'Hora') + 8. Marcos (M4) |
| 28 | 🟠 Média | APIs Google | `error-taxonomy-410-meanings` | 7.2 Regras de sync (linhas 'Erros' e 'Agenda, depois') |
| 29 | 🟠 Média | APIs Google | `token-lifecycle-cleartoken-background` | 6. Fluxo de autorização (passos 3–7) + 12. Checklist |
| 30 | 🟠 Média | APIs Google | `scopes-minimal-granular-consent` | 3. Pré-requisitos (escopos); 4. Fora do MVP; 6. passo 2; 11. |
| 31 | 🟠 Média | APIs Google | `reminders-usedefault` | 7.4 Regras de alarme (linha 'Evento com hora') + 7.3 |
| 32 | 🟠 Média | APIs Google | `event-types-readonly-declined` | 7.3 Observações + 7.4 Regras de alarme |
| 33 | 🟠 Média | APIs Google | `tasks-move-default-uncomplete` | 7.3 Evento x Tarefa na API |
| 34 | 🟠 Média | APIs Google | `conflict-rule-unimplementable` | 7.2 Regras de sync (linhas 'Ordem' e 'Conflito') |
| 35 | 🟠 Média | Escopo MVP / Claude Code | `outbox-e-conflito-subespecificados` | 7.1 Entidades / 7.2 Regras de sync |
| 36 | 🟠 Média | Escopo MVP / Claude Code | `escopo-grade-semanal-e-cortes` | 4. Escopo do MVP (F3, F5, Fora) / 3 e 11 (escopos) / Figma t |
| 37 | 🟠 Média | Escopo MVP / Claude Code | `dados-so-locais-e-saas-barato` | 7.3 Observações / 10. Preparação para SaaS / 12. Checklist |
| 38 | 🟠 Média | Escopo MVP / Claude Code | `claude-md-arquivo-e-progresso` | Cabeçalho / 8. M0 / 11. Regras para o Claude Code |
| 39 | 🟠 Média | Escopo MVP / Claude Code | `tema-dynamic-color-e-escuro-roxo` | 9. Identidade visual / 4. Fora do MVP |
| 40 | 🟠 Média | Sync e dados | `lembretes-usedefault` | 7.4 Regras de alarme (linha 'Evento com hora') |
| 41 | 🟠 Média | Sync e dados | `alarmentity-derivado` | 7.1 AlarmEntity + 7.4 (Tarefa / Reboot / Sempre após sync) |
| 42 | 🟠 Média | Sync e dados | `tasks-mover-lista-hierarquia` | 7.3 Evento x Tarefa na API + 7.1 OutboxEntity (operation) |
| 43 | 🟠 Média | Sync e dados | `marcador-notes-alarme` | 7.3 Observações (marca '⏰ HH:mm' em notes) + 'alarme automát |
| 44 | 🟠 Média | Sync e dados | `entidades-campos-indices` | 7.1 Entidades Room |
| 45 | 🟡 Baixa | Android | `figma-alarme-headsup` | TELAS 7. Alarme em tela cheia / 8. Ajustes |
| 46 | 🟡 Baixa | APIs Google | `recurring-occurrence-and-allday-mapping` | 4. Fora do MVP (texto sobre recorrência) + 7.3 |
| 47 | 🟡 Baixa | Escopo MVP / Claude Code | `nomes-e-textos-inconsistentes` | 2. Decisões fixas / 4. F10 / 8. M0 e M6 / Figma telas 3 e 8 |
| 48 | 🟡 Baixa | Sync e dados | `ordem-marcos-dependencias` | 8 Marcos + 12 Checklist |

---

## 1. 🔴 Alta · `web-client-id-obrigatorio` (Android)

**Arquivo/seção:** spec · 3. Pré-requisitos / 6. Fluxo de autorização passo 1 / 10. Fica para depois / 5. Stack (Login)

**Problema:** A spec manda usar Credential Manager + googleid, mas só pede para criar o cliente OAuth Android e diz na seção 10 que o 'cliente OAuth Web' fica para depois. O Sign in with Google via Credential Manager exige setServerClientId(WEB_CLIENT_ID) em GetGoogleIdOption/GetSignInWithGoogleOption. A stack também não cita androidx.credentials:credentials nem credentials-play-services-auth (este último é necessário no Android 13 e anteriores). E a frase 'retorna DEVELOPER_ERROR (código 10)' só vale para o AuthorizationClient (ApiException). O Credential Manager falha com GetCredentialException/NoCredentialException.

**Por quê:** No M1, o Claude Code vai precisar de um WEB_CLIENT_ID que a spec manda não criar. Ele trava ou inventa um valor, e o login falha sempre, com um erro que não bate com o que a spec descreve. Para o AuthorizationClient sozinho (sem requestOfflineAccess), o cliente Android basta. O que exige o cliente Web é o Credential Manager.

**Correção sugerida:**

Seção 3, adicionar depois do item do cliente Android:
"- [ ] **Cliente OAuth Web** (tipo 'Aplicativo da Web', sem URIs, sem usar client secret) no mesmo projeto. O Client ID dele é o `WEB_CLIENT_ID` exigido por `GetGoogleIdOption.Builder().setServerClientId(...)`. Não é site nem backend. Guardar em `local.properties` como `SINCRONIA_WEB_CLIENT_ID` e expor via `BuildConfig.WEB_CLIENT_ID`."
Trocar a linha 41 por:
"Sem esses passos o login falha: o Credential Manager lança `GetCredentialException`/`NoCredentialException` (ex.: '[28444] Developer console is not set up correctly') e o `AuthorizationClient` devolve `ApiException` com status 10 (`DEVELOPER_ERROR`). A causa é quase sempre pacote/SHA-1 do cliente Android ou `WEB_CLIENT_ID` errados. Isso **não é bug de código**."
Seção 6, passo 1:
"1. Credential Manager (`GetGoogleIdOption` com `setServerClientId(BuildConfig.WEB_CLIENT_ID)` e `setFilterByAuthorizedAccounts(false)`) → usuário escolhe a conta → guardar no DataStore o e-mail (`GoogleIdTokenCredential.id`), nome, foto e `accountId` = claim `sub` do ID token (decodificar o payload JWT localmente; o `sub` é estável, o e-mail pode mudar)."
Seção 5, linha Login: "**Credential Manager** (`androidx.credentials:credentials` + `androidx.credentials:credentials-play-services-auth`) + `com.google.android.libraries.identity.googleid`".
Seção 10, em 'Fica para depois', trocar 'cliente OAuth Web (credencial de servidor, não é site)' por '`requestOfflineAccess(WEB_CLIENT_ID)` + troca do server auth code no backend'.

**Evidência:** https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation : "Set the setServerClientId parameter. The webClientId is the Web Client ID you set up for OAuth in your Google Cloud Project" (os dois exemplos usam setServerClientId(WEB_CLIENT_ID)); a mesma página lista as dependências credentials + credentials-play-services-auth + googleid. https://developer.android.com/identity/authorization : manda criar um Android client ID e também um 'Web application' client ID. A mensagem [28444] e a distinção DEVELOPER_ERROR/ApiException: unverified (from knowledge).

---

## 2. 🔴 Alta · `som-do-alarme-no-servico` (Android)

**Arquivo/seção:** spec · 7.4 Regras de alarme (linha Disparo) e 7.5 Permissões

**Problema:** A spec coloca o som e a vibração na AlarmActivity e trata o foreground service como opcional ('se tocar o som por serviço'). Com o celular desbloqueado e em uso, o fullScreenIntent não abre a Activity: o sistema mostra uma notificação heads-up. Nesse caso, nada toca. Além disso, no Android 17 (estável desde 16/06/2026), qualquer app que toque áudio em segundo plano precisa de Activity visível ou de um FGS que não seja shortService. Fora disso, a reprodução falha em silêncio.

**Por quê:** O alarme fica mudo sempre que o usuário estiver usando o celular, ou quando a Activity sair da frente (botão Home). Isso quebra a função principal do app (F8). Sem timeout, um alarme também poderia tocar para sempre se ninguém interagir.

**Correção sugerida:**

Seção 7.4, trocar a linha Disparo por:
"| Disparo | `AlarmReceiver` (exported=false) → `ContextCompat.startForegroundService(AlarmService)` (permitido: disparo de alarme exato). `AlarmService` (`android:foregroundServiceType=\"mediaPlayback\"`) chama `ServiceCompat.startForeground(..., ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)` em até 5 s com a notificação do alarme (canal `alarm`, `CATEGORY_ALARM`, `setOngoing(true)`, `setFullScreenIntent(AlarmActivity, true)` e ações **Soneca** e **Dispensar**). **Som e vibração ficam no serviço, nunca na Activity**: `MediaPlayer` em loop com `AudioAttributes.USAGE_ALARM` + `CONTENT_TYPE_SONIFICATION` e `setWakeMode(PARTIAL_WAKE_LOCK)`, vibração com `VibrationAttributes.USAGE_ALARM` (API 33+). A `AlarmActivity` só mostra a UI e envia `ACTION_SNOOZE`/`ACTION_DISMISS` ao serviço. Sem interação, parar sozinho após 10 min e registrar como 'perdido'. Não usar `shortService` (limite de 3 min e bloqueado para áudio em segundo plano no Android 17). |"
Seção 7.5, trocar o trecho do serviço por: "`FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (obrigatório: o som do alarme toca no `AlarmService`)".
Marco M5, no 'Pronto quando', acrescentar: "...e também com o celular desbloqueado em uso (aparece a notificação com Soneca/Dispensar e o som toca)".

**Evidência:** https://developer.android.com/develop/ui/views/notifications/build-notification#urgent-message : "When the user's device is unlocked, the notification appears in an expanded form". https://developer.android.com/develop/ui/views/notifications/time-sensitive : "the system UI displays a heads-up notification instead while the user actively uses the device". https://developer.android.com/about/versions/17/changes/bg-audio : "All apps running on Android 17 that have these background audio interactions must have a visible activity or must be running a foreground service that is not of type SHORT_SERVICE. This applies whether or not the app targets API level 37"; para target 37 a exigência WIU é dispensada se o app "has been granted the exact alarm permission, and it is making changes to audio streams that have the USAGE_ALARM attribute"; as falhas são silenciosas. https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start : exceção "Your app invokes an exact alarm to complete an action that the user requests". https://developer.android.com/develop/background-work/services/fgs/service-types : shortService tem limite de cerca de 3 minutos. https://developer.android.com/blog/posts/android-17-is-here : Android 17 lançado em 16/06/2026.

---

## 3. 🔴 Alta · `schedule-exact-alarm-api31-32` (Android)

**Arquivo/seção:** spec · 7.5 Permissões (AndroidManifest)

**Problema:** A spec declara só USE_EXACT_ALARM com minSdk 26. USE_EXACT_ALARM só existe a partir da API 33. No Android 12/12L (API 31–32), o app fica sem permissão de alarme exato e setAlarmClock() lança SecurityException. Também não há verificação de canScheduleExactAlarms() nem tratamento de revogação na API 31–32.

**Por quê:** O app quebra ao agendar alarme em aparelhos Android 12/12L, que ainda estão dentro do minSdk.

**Correção sugerida:**

Seção 7.5, trocar a lista por:
"`INTERNET`, `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` com `android:maxSdkVersion=\"32\"` (Android 12/12L não conhecem `USE_EXACT_ALARM`), `USE_FULL_SCREEN_INTENT`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
No `AlarmScheduler`: em API 31–32, checar `alarmManager.canScheduleExactAlarms()` antes de agendar. Se for falso, mostrar aviso que abre `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. Registrar receiver para `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` que reagenda tudo. Envolver `setAlarmClock` em `try/catch (SecurityException)`."
Não declarar SCHEDULE_EXACT_ALARM sem maxSdkVersion: só uma das duas deve valer em cada aparelho.

**Evidência:** https://developer.android.com/reference/android/Manifest.permission#USE_EXACT_ALARM : "Apps need to target API TIRAMISU or above to be able to request this permission. Note that only one of USE_EXACT_ALARM or SCHEDULE_EXACT_ALARM should be requested on a device. If your app is already using SCHEDULE_EXACT_ALARM on older SDKs but needs USE_EXACT_ALARM on SDK 33 and above, then SCHEDULE_EXACT_ALARM should be declared with a max-sdk attribute, like: <uses-permission android:name=\"android.permission.SCHEDULE_EXACT_ALARM\" android:maxSdkVersion=\"32\" />". https://developer.android.com/develop/background-work/services/alarms : Android 12 (API 31) → declarar SCHEDULE_EXACT_ALARM; usar canScheduleExactAlarms() e ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED para reagendar.

---

## 4. 🔴 Alta · `web-client-id-credential-manager` (APIs Google)

**Arquivo/seção:** spec · 3. Pré-requisitos; 6. Fluxo de autorização (passo 1); 10. Preparação para SaaS

**Problema:** The spec has Claude Code use Credential Manager + `googleid` for login, but the prerequisites only create an Android OAuth client. Section 10 also postpones the Web OAuth client to 'depois'. Credential Manager's `GetGoogleIdOption` / `GetSignInWithGoogleOption` requires `setServerClientId(...)`, and that value must be the Client ID of a Web application OAuth client, not the Android one.

**Por quê:** M1 cannot pass as specified. Claude Code will either pass the Android client ID, which fails at runtime with GetCredentialException '[28444] Developer console is not set up correctly' (not the `DEVELOPER_ERROR (código 10)` the spec tells Gustavo to expect), or it will get stuck because rule 3 and section 10 appear to forbid a Web client. Only the Web client's ID is needed. Its secret is never used, so the no-backend decision still holds. The console is also now called 'Google Auth Platform' (Branding / Público-alvo / Clientes / Acesso a dados), so the 'Tela de consentimento OAuth' wording will confuse Gustavo.

**Correção sugerida:**

Seção 3, adicionar logo após o item do cliente Android:
"- [ ] **Cliente OAuth do tipo \"Aplicativo da Web\"** (Google Auth Platform → Clientes → Criar cliente → Aplicativo da Web, sem URIs). Copiar **só o Client ID** (termina em `.apps.googleusercontent.com`) para `local.properties` como `GOOGLE_WEB_CLIENT_ID=...` (lido via `BuildConfig`). O Credential Manager exige esse ID em `GetGoogleIdOption.Builder().setServerClientId(...)`. **Não** baixar nem usar o client secret."
Seção 3, trocar a última linha por: "Sem esses passos, o login falha com `GetCredentialException` \"[28444] Developer console is not set up correctly\" (Credential Manager) ou `ApiException` 10 / `DEVELOPER_ERROR` (AuthorizationClient). Isso **não é bug de código**."
Seção 6, passo 1: "1. Credential Manager (`GetGoogleIdOption` com `setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)` e `setFilterByAuthorizedAccounts(false)`) → usuário escolhe a conta → guardar no DataStore o e-mail (`GoogleIdTokenCredential.id`) e, como `accountId`, o `sub` lido do payload do `idToken` (ID estável; o e-mail pode mudar)."
Seção 10, trocar "cliente OAuth Web (credencial de servidor, não é site)" por "uso do **client secret** do cliente Web (só com backend)".

**Evidência:** https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation: "Set the setServerClientId parameter. The webClientId is the Web Client ID you set up for OAuth in your Google Cloud Project". https://developer.android.com/identity/authorization recommends Credential Manager for sign-in before AuthorizationClient. https://support.google.com/cloud/answer/15549945 documents the Google Auth Platform sections (Audience, Branding, Clients, Data Access). The exact 28444 error text is unverified (from knowledge).

---

## 5. 🔴 Alta · `calendar-sync-window-slides` (APIs Google)

**Arquivo/seção:** spec · 7.2 Regras de sync (linhas 'Agenda, primeira vez' / 'Agenda, depois' / 'Excluídos')

**Problema:** The first sync limits the window with timeMin=-7d/timeMax=+60d. After that the app only uses `syncToken`, which cannot be combined with timeMin/timeMax and returns only entries that changed. So an event that already existed beyond +60d, including every future instance of an unchanged weekly meeting, never arrives as the window moves forward. The reverse also happens: with singleEvents=true, one edit to a recurring series makes the incremental sync return every expanded instance, years back and forward. On 410, the spec only says 'apagar token e refazer' and does not reconcile, so events deleted in the meantime stay in Room with their alarms.

**Por quê:** After a few weeks, events near the end of the window are missing. After about 60 days with no change to a recurring series, its instances are gone entirely and their alarms never fire. For an alarm app this is a silent correctness failure. Ghost events and alarms also survive a 410.

**Correção sugerida:**

Substituir as linhas da Agenda em 7.2 por:
"| Agenda, sync completo | `GET events?singleEvents=true&timeMin=<hoje−7d>&timeMax=<hoje+60d>&maxResults=2500`, paginando por `nextPageToken`; o `nextSyncToken` vem **só na última página**. Ao terminar, apagar do Room os eventos `SYNCED` da `primary` que não vieram na resposta (manter os `PENDING_UPLOAD`) e cancelar os alarmes deles. Guardar `calendar_primary_syncToken` e `calendar_primary_lastFullSync` (data local) |
| Agenda, incremental | `GET events?singleEvents=true&syncToken=...` — **sem** `timeMin`, `timeMax`, `updatedMin`, `orderBy`, `q` (a API rejeita). Os demais parâmetros iguais aos do sync completo. Itens fora da janela: ignorar (não gravar) ou apagar se já existirem |
| Janela deslizante | O `syncToken` **não** traz eventos antigos que entram na janela com o passar dos dias. Refazer o sync completo **1x por dia** (quando a data local for diferente de `calendar_primary_lastFullSync`) |
| Agenda, 410 | `GET events?syncToken=...` com `410` (`reason: fullSyncRequired`) → apagar o token e fazer o sync completo **com a reconciliação acima** |"

**Evidência:** https://developers.google.com/workspace/calendar/api/v3/reference/events/list: the params not allowed with syncToken are iCalUID, orderBy, privateExtendedProperty, q, sharedExtendedProperty, timeMin, timeMax, updatedMin. syncToken "makes the result of this list request contain only entries that have changed since then". nextSyncToken appears only when nextPageToken is omitted. maxResults max is 2500. https://developers.google.com/workspace/calendar/api/guides/sync: "Each list request should use the same set of query parameters" and "[410] should trigger a full wipe of the client's store and a new full sync". Third-party corroboration: https://github.com/garrytan/gbrain/pull/5511 ("With singleEvents=true, one change to a recurring series ... returns every expanded instance of that series, years back and forward").

---

## 6. 🔴 Alta · `credential-manager-exige-web-client-id` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 3. Pré-requisitos / 5. Stack / 10. Preparação para SaaS

**Problema:** A seção 5 manda usar Credential Manager + `googleid` (Sign in with Google), e isso exige `GetGoogleIdOption.setServerClientId(WEB_CLIENT_ID)`, ou seja, o ID de um cliente OAuth do tipo “Aplicativo da Web”. A seção 3 pede só o cliente Android, e a seção 10 põe “cliente OAuth Web” em “Fica para depois”. Nada impede o agente de usar `applicationIdSuffix ".debug"`, que é padrão comum e muda o pacote para `com.sincronia.app.debug`, o que quebra o cliente Android registrado para `com.sincronia.app`.

**Por quê:** O M1 não fecha. O agente não recebe o ID de que precisa, e a spec diz em outra seção que o cliente Web fica para depois. Aí ele inventa um valor, tira o Credential Manager (desvio de stack) ou para. Com o sufixo .debug o login dá DEVELOPER_ERROR, e a seção 3 diz que isso “não é bug de código”, o que atrapalha o diagnóstico.

**Correção sugerida:**

Na seção 3, logo depois da linha do cliente Android, acrescentar:
“- [ ] **Cliente OAuth do tipo “Aplicativo da Web”**, criado só para obter o *Web Client ID* (não é site nem servidor). O Credential Manager exige esse ID em `GetGoogleIdOption.setServerClientId()`. O ID não é segredo: pode ficar em `app/build.gradle.kts` como `buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", ...)`. Nunca baixar nem usar o *client secret*.”
Na seção 10, trocar “cliente OAuth Web (credencial de servidor, não é site)” por “troca de *auth code* por refresh token no servidor (`requestOfflineAccess` com o Web Client ID)”.
Na seção 11, acrescentar a regra: “9. Não usar `applicationIdSuffix` no build debug. O cliente OAuth Android está registrado para `com.sincronia.app`. Para ter debug e release instalados juntos, registre antes um segundo cliente Android para `com.sincronia.app.debug`.”

**Evidência:** https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation (“Set the server client ID… The webClientId is the Web Client ID you set up for OAuth in your Google Cloud Project”). https://developer.android.com/identity/authorization (para autorização client-side basta o cliente Android; revokeAccess no exemplo com play-services-auth 22.0.0). Spec §3 linha 37, §10 linha 245.

---

## 7. 🔴 Alta · `janela-sync-nao-desliza-recorrentes` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 7.2 Regras de sync / 4. Fora do MVP

**Problema:** A primeira sincronização usa `timeMin=-7d&timeMax=+60d&singleEvents=true` e as seguintes usam só `syncToken`. A Calendar API proíbe `timeMin`/`timeMax` junto com `syncToken`, então a janela nunca avança: ocorrências de eventos recorrentes além dos +60 dias iniciais só chegam se forem alteradas. A doc de sync ainda exige os mesmos parâmetros na requisição inicial e nas incrementais. Além disso, a seção 4 se contradiz sobre recorrência: “editar só a ocorrência” e, no mesmo parêntese, “recorrências só são exibidas”.

**Por quê:** Uns 2 meses depois da instalação, a reunião semanal some de Hoje e da Agenda e perde o alarme, sem erro nenhum. É exatamente o caso de uso central (alarme de compromisso). Com a contradição, o agente pode fazer um editor de ocorrência (PATCH na instância) ou deixar tudo como somente leitura, ao acaso.

**Correção sugerida:**

Na seção 7.2, trocar as linhas “Agenda, primeira vez” e “Agenda, depois” por uma só:
“| Agenda | A cada sync: `GET events?timeMin=<hoje−7d>&timeMax=<hoje+60d>&singleEvents=true&maxResults=250`, paginando até o fim. Gravar/atualizar o que voltar. Apagar do Room (e cancelar o alarme) os eventos `SYNCED` da janela que não voltaram. Nunca apagar itens com alteração pendente no outbox. Uma agenda pessoal cabe em 1 a 3 páginas; `syncToken` fica para depois do MVP. |”
Tirar `calendar_primary_syncToken` do exemplo de `SyncMetaEntity`.
Na seção 4, “Fora do MVP”, trocar “editar **série** de evento recorrente (no MVP, editar só a ocorrência; recorrências só são exibidas)” por:
“criar ou editar eventos recorrentes. No MVP, as ocorrências de eventos recorrentes são **somente leitura** (o editor mostra “Evento recorrente: edite no Google Agenda”). O usuário só pode ligar/desligar e mudar o **alarme local** daquela ocorrência.”

**Evidência:** https://developers.google.com/calendar/api/v3/reference/events/list (syncToken não pode ser usado com iCalUID, orderBy, privateExtendedProperty, q, sharedExtendedProperty, timeMin, timeMax, updatedMin). https://developers.google.com/calendar/api/guides/sync (“Each list request should use the same set of query parameters, including the initial request.”) Spec §7.2 linhas 151-152, §4 linha 63.

---

## 8. 🔴 Alta · `hoje-indefinido-e-due-date-fuso` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 4. F4 / 7.3 Evento x Tarefa / 8. M3 / 6. core/time

**Problema:** O F4 e o M3 não dizem o que entra em Hoje: eventos de dia inteiro e de vários dias, eventos que começaram ontem, tarefas atrasadas, tarefas concluídas hoje. Também não dizem se uma tarefa com alarme automático das 09:00 vai para “Dia inteiro” ou para a linha do tempo. O `due` do Tasks vem como `YYYY-MM-DDT00:00:00.000Z` e a spec não diz como interpretar: convertido para o fuso de São Paulo, vira 21:00 do dia anterior. O `end.date` de dia inteiro é exclusivo. A seção 6 diz “America/Sao_Paulo via ZoneId.systemDefault()”, o que é ambíguo (fixar ou usar o do sistema?). O “alarme automático em tarefas” não tem valor padrão definido e cria um alarme em tela cheia por tarefa na mesma hora.

**Por quê:** A tela principal do app vai sair diferente do esperado. Parsear o `due` como Instant (o que o agente tende a fazer) mostra todas as tarefas um dia antes em Hoje e agenda o alarme no dia errado. Com 10 tarefas no dia, o alarme automático dispara 10 telas cheias às 09:00.

**Correção sugerida:**

Trocar o F4 por:
“| F4 | Tela **Hoje** (dia local do aparelho): (1) **Atrasadas**: tarefas não concluídas com `dueDate` antes de hoje; (2) **Dia inteiro**: eventos de dia inteiro que cobrem hoje (inclusive os de vários dias; `end.date` é exclusivo) e tarefas de hoje sem hora escolhida pelo usuário; (3) **linha do tempo**: eventos com hora que cruzam hoje (inclusive ocorrências de recorrentes e eventos que começaram ontem) e tarefas de hoje com hora de alarme, em ordem de horário; (4) tarefas concluídas hoje aparecem riscadas no fim. |”
Nas observações da seção 7.3, acrescentar:
“- O `due` da Tasks API é só data: ler os 10 primeiros caracteres (`YYYY-MM-DD`) como `LocalDate`, **sem converter fuso**. Ao gravar, enviar `YYYY-MM-DDT00:00:00.000Z`. Ter teste unitário disso.
- Eventos de dia inteiro usam `start.date`/`end.date` como `LocalDate`, com fim exclusivo.
- “Alarme automático em tarefas” vem **desligado** por padrão. Se ligado, dispara **um único alarme** na hora padrão listando as tarefas do dia sem hora. Essas tarefas continuam na faixa “Dia inteiro”.”
Na seção 6, trocar “utilitários de data/fuso (America/Sao_Paulo via ZoneId.systemDefault())” por “utilitários de data/fuso; sempre `ZoneId.systemDefault()` (no celular do Gustavo, America/Sao_Paulo); nunca fixar fuso no código”.
Acrescentar o interruptor “Alarme automático em tarefas” no F10, no M6 e na tela 8 do Figma.

**Evidência:** https://developers.google.com/tasks/reference/rest/v1/tasks (due: “The due date only records date information; the time portion of the timestamp is discarded… It isn't possible to read or write the time that a task is due via the API.”). Fim exclusivo de dia inteiro: https://developers.google.com/calendar/api/v3/reference/events (unverified, from knowledge). Spec §4 linha 53, §7.3 linhas 166/171, §8 linha 199.

---

## 9. 🔴 Alta · `sem-modo-demo-agente-trava` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 6. Arquitetura / 8. Marcos / 11. Regra 7

**Problema:** Do M1 em diante, todo “Pronto quando…” depende da configuração do Google Cloud feita pelo Gustavo e de um celular real. O M5 exige tela bloqueada, silencioso e reinício, coisas que o agente não verifica pelo terminal. A regra 7 manda o agente parar. Não há fonte de dados falsa: as interfaces `CalendarSource`/`TaskSource` existem, mas nada diz para ter uma implementação fake.

**Por quê:** O Claude Code trava no M1 (ou declara o marco pronto sem verificar), e as UIs dos M2/M3/M5 não podem ser vistas nem testadas até o Gustavo terminar a seção 3. É o maior risco de o trabalho parar logo no início.

**Correção sugerida:**

Na seção 6, depois do parágrafo das interfaces, acrescentar:
“**Modo demonstração (só no build debug):** implementar `FakeCalendarSource` e `FakeTaskSource` com dados de exemplo: eventos de hoje e da semana, 1 evento recorrente, 1 evento de dia inteiro, tarefas em 2 listas e 1 tarefa atrasada. No onboarding do build debug, o botão “Entrar em modo demonstração” liga uma flag no DataStore que faz o app usar as fontes falsas e pula o login. Assim os marcos M2 a M6 andam mesmo sem os pré-requisitos da seção 3. O modo demonstração não existe no release.”
Na seção 8, depois da tabela, acrescentar:
“**Verificação de cada marco:** (a) *automática*, feita pelo Claude Code: `./gradlew assembleDebug test lint` passa e, no modo demonstração, a tela mostra o esperado; (b) *manual*, feita pelo Gustavo no celular, seguindo os passos que o Claude Code escreve em `docs/TESTES-MANUAIS.md`. Se (b) depender de algo que ainda não está pronto, registre como pendente em `docs/DECISOES.md` e siga para o próximo marco.”
No M5, acrescentar: “No build debug, Configurações tem “Testar alarme em 10 s”. Comandos para o Gustavo: `adb shell dumpsys alarm | grep -i sincronia` e `adb reboot`.”
Na regra 7, trocar “**pare e diga exatamente o que ele precisa fazer**, com o comando pronto” por “diga exatamente o que ele precisa fazer, com o comando pronto, e **continue no que não depende dele**”.

**Evidência:** Interno: Spec §8 linhas 197-201 (critérios M1/M2/M5 exigem Google real e celular), §11 regra 7 linha 257, §6 linha 119 (interfaces sem fake). Nada a verificar externamente.

---

## 10. 🔴 Alta · `ambiente-e-versoes-nao-ancorados` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 3. Pré-requisitos / 5. Stack

**Problema:** A seção 3 só lista o Google Cloud. Falta o ambiente de desenvolvimento: Android Studio/JDK 17+, Android SDK, `adb`, e um celular com depuração USB ou emulador com imagem Google Play (o login Google precisa disso). O M0 exige `./gradlew`, mas o agente não gera o wrapper sem Gradle instalado. “Versão estável mais recente” não tem âncora: as versões que o agente conhece podem estar velhas ou não bater entre si (Kotlin/KSP/AGP/plugin do Compose/Hilt). A doc oficial do Room já mostra o plugin `androidx.room3` (alfa), enquanto o estável é o Room 2.8.5. `revokeAccess()`, usado no checklist da seção 12, só existe a partir de play-services-auth 22.0.0.

**Por quê:** O M0 pode travar antes da primeira linha útil (sem SDK/wrapper) ou entrar num loop de ajustar versões. Se o agente copiar exemplos atuais da doc, pode puxar o Room 3 alfa. Com play-services-auth antigo, o “Desconectar” não compila.

**Correção sugerida:**

Acrescentar a seção “3.1 Ambiente de desenvolvimento (Gustavo, uma vez)”:
“- [ ] Android Studio instalado (traz JDK 17+ e o Android SDK); `adb` no PATH
- [ ] Celular Android com depuração USB ligada **ou** emulador com imagem “Google Play”
- [ ] No Android Studio: *New Project → Empty Activity*, nome `SincronIA`, pacote `com.sincronia.app`, minSdk 26, Kotlin DSL. Abrir uma vez para gerar `gradlew`, `local.properties` e `gradle/libs.versions.toml`. O Claude Code parte desse esqueleto.”
Na seção 5, trocar “Use sempre a **versão estável mais recente** de cada item.” por:
“Parta das versões do template do Android Studio e atualize só para versões **estáveis** (nada de alpha/beta/RC). Kotlin, KSP, AGP, o plugin do Compose e o Hilt têm de ser compatíveis entre si; se o build quebrar por versão, volte à última combinação que funcionava. Mínimos: `play-services-auth` 22.0.0 (tem `revokeAccess`) e Room **2.x** com o plugin `androidx.room`. Não usar `androidx.room3`, que ainda é alfa. Registre as versões escolhidas em `docs/DECISOES.md`.”

**Evidência:** https://developer.android.com/jetpack/androidx/releases/room (Room 2.8.5 estável em 09/09/2026; room3 sem versão estável). https://developer.android.com/training/data-storage/room/migrating-db-versions (exemplos já usam `id("androidx.room3")`). https://developer.android.com/identity/authorization (revokeAccess com `play-services-auth:22.0.0`).

---

## 11. 🔴 Alta · `alarme-exato-android-12-e-permissoes-negadas` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 7.5 Permissões (e tela 2 do prompt do Figma)

**Problema:** Com minSdk 26, o manifesto lista só `USE_EXACT_ALARM`, que não existe no Android 12/12L (API 31-32). Nessas versões, `setAlarmClock()` exige `SCHEDULE_EXACT_ALARM`. A spec não diz o que acontece quando o usuário nega notificações (API 33+), nega/revoga o full-screen intent (Android 14+) ou o acesso a alarmes exatos. A tela 2 do Figma põe um botão “Permitir” para “Ignorar otimização de bateria”, o que sugere pedir a isenção direto. Isso não é necessário (`setAlarmClock` toca mesmo em Doze) e o Play só permite o pedido direto em casos específicos.

**Por quê:** No Android 12 o alarme não é agendado (SecurityException). Com notificação negada, o alarme falha em silêncio, que é a pior falha possível para um app de alarme. O pedido direto de isenção de bateria viraria problema de política quando o app for para o Play (SaaS).

**Correção sugerida:**

Na seção 7.5, trocar a lista por:
“`INTERNET`, `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` com `android:maxSdkVersion="32"` (o Android 12/12L não conhece `USE_EXACT_ALARM`), `USE_FULL_SCREEN_INTENT`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (se tocar o som por serviço). Não declarar `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
| Item | Como checar | Se negado |
| Notificações (13+) | `POST_NOTIFICATIONS` | Faixa em Hoje: “Alarmes desativados: permita notificações”, com botão para as configurações do app |
| Alarmes e lembretes (12/12L) | `AlarmManager.canScheduleExactAlarms()` | Abrir `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; enquanto negado, avisar em Hoje |
| Tela cheia (14+) | `NotificationManager.canUseFullScreenIntent()` | Abrir `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`; o alarme ainda toca como notificação de alta prioridade com som |
| Otimização de bateria | opcional | Botão “Abrir configurações” com `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (só ajuda o sync; o alarme toca mesmo em Doze) |
Rechecar tudo no `onResume` da tela Hoje.”
No Figma, tela 2, trocar o item por: “2. Permissões: lista com Notificações, Alarmes e lembretes e Alarmes em tela cheia (cada um com o status “Permitido” em verde ou o botão “Permitir”), e um item opcional “Otimização de bateria” com o botão “Abrir configurações”. Rodapé: botão “Continuar” e link “Fazer depois”.”

**Evidência:** https://developer.android.com/develop/background-work/services/alarms/schedule (USE_EXACT_ALARM só para quem tem target 33+; API 31-32 usa SCHEDULE_EXACT_ALARM). https://developer.android.com/training/monitoring-device-state/doze-standby (setAlarmClock “continue to fire normally”; pedido direto de isenção restrito pelo Play). https://support.google.com/googleplay/android-developer/answer/13392821 (FSI concedido por padrão só a apps de alarme/chamada). https://support.google.com/googleplay/android-developer/answer/9888170 (USE_EXACT_ALARM aceito para app de agenda que notifica eventos). maxSdkVersion=32: unverified (from knowledge).

---

## 12. 🔴 Alta · `agenda-sync-incremental-e-janela` (Sync e dados)

**Arquivo/seção:** spec · 7.2 Regras de sync (linhas 'Agenda, primeira vez' e 'Agenda, depois') + F2

**Problema:** A linha 'Agenda, depois' diz só `GET events?syncToken=...`: não tem `singleEvents=true` nem paginação. A janela de −7d a +60d só é aplicada na primeira carga completa. No erro 410 o texto diz apenas 'apagar token e refazer a sincronização completa', sem reconciliar o que já está no Room.

**Por quê:** (1) A documentação exige os mesmos parâmetros da carga inicial. Sem `singleEvents=true` a API devolve o evento-mestre recorrente (com `recurrence`) em vez das ocorrências, e o Claude Code, copiando a linha ao pé da letra, grava esse mestre como um evento único na data da 1ª ocorrência. (2) `timeMin`/`timeMax` não podem ser usados junto com `syncToken`, então a janela nunca avança. Eventos não editados que estavam além de +60d na primeira carga nunca chegam: uma consulta marcada há meses para daqui a 70 dias, ou as ocorrências de uma reunião semanal depois de +60d. Em uns 2 meses a Agenda futura fica vazia e os alarmes param. Ao mesmo tempo, a sync incremental traz mudanças de qualquer data, inclusive ocorrências muito distantes de uma série editada. (3) No 410 a documentação manda apagar os dados locais. Se o app só baixar de novo e fizer upsert, eventos apagados no Google continuam no Room e o alarme deles toca.

**Correção sugerida:**

Trocar as duas linhas da Agenda na tabela 7.2 por:
| Agenda, carga completa | `GET events?timeMin=<hoje-7d>&timeMax=<hoje+60d>&singleEvents=true&maxResults=250`, paginando com `pageToken` até vir `nextSyncToken`. Gravar o token e `<accountId>:calendar:primary:windowEnd = hoje+60d` **só depois** de processar todas as páginas, na mesma transação. Ao terminar, apagar localmente (e cancelar o alarme de) todo evento `SYNCED` do `accountId` que não veio na carga; itens `PENDING_UPLOAD` ficam. |
| Agenda, incremental | `GET events?syncToken=...&singleEvents=true&maxResults=250` (**mesmos parâmetros da carga completa**, exceto `timeMin`/`timeMax`, que são proibidos com `syncToken`), paginando até vir `nextSyncToken`. Ignorar (não gravar) eventos que começam fora de `[hoje-7d, windowEnd)`. |
| Janela móvel | Em cada sync, se `hoje+60d > windowEnd`, descartar o token e fazer a **carga completa** de novo (na prática 1 vez por dia; numa agenda pessoal são poucas requisições). Apagar localmente os eventos que terminaram antes de `hoje-7d`. |
| Erro 410 | Mesma coisa: descartar o token e fazer a carga completa com a reconciliação acima. |
Teste no M2: MockWebServer simulando 61 dias depois; um evento do dia +62 precisa aparecer.

**Evidência:** events.list: syncToken 'cannot be specified together with ... timeMin, timeMax, updatedMin' and 'All other query parameters should be the same as for the initial synchronization to avoid undefined behavior'; singleEvents default False returns 'the underlying recurring events' https://developers.google.com/calendar/api/v3/reference/events/list . Sync guide: 410 should trigger 'a full wipe of the client's store and a new full sync' https://developers.google.com/calendar/api/guides/sync

---

## 13. 🔴 Alta · `alarmes-passado-horizonte-limite` (Sync e dados)

**Arquivo/seção:** spec · 7.4 Regras de alarme (Reboot / Sempre após sync) + F9

**Problema:** A spec manda reagendar 'tudo' a partir do Room depois de cada sync e de cada boot, mas não exige que `triggerAt` esteja no futuro e não define horizonte nem limite de alarmes.

**Por quê:** O AlarmManager dispara na hora qualquer alarme com horário no passado. Na primeira sync entram os eventos dos últimos 7 dias (início − 10 min já passou) e as tarefas de hoje e atrasadas com a hora padrão 09:00. Resultado: dezenas de alarmes em tela cheia de uma vez no primeiro login, e de novo a cada reboot e a cada sync. Além disso, o Android limita um app a 500 alarmes simultâneos (IllegalStateException), número fácil de atingir com 67 dias de um evento recorrente diário mais as tarefas. Por fim, BOOT_COMPLETED só chega depois do primeiro desbloqueio, então um reboot de madrugada pode fazer o alarme da manhã não tocar.

**Correção sugerida:**

Acrescentar à tabela 7.4:
| Quais agendar | Só alarmes com `triggerAt` em `(agora, agora + 7 dias]`, `enabled`, não dispensados, de itens não excluídos nem concluídos. No máximo **50** no AlarmManager; o restante entra nas próximas execuções. |
| Horário já passou | **Nunca** chamar `setAlarmClock` com `triggerAt <= agora`. Se o evento ainda não começou, mostrar notificação normal (sem tela cheia). Tarefa atrasada não gera alarme. |
| Idempotência | `AlarmScheduler.rescheduleAll()` cancela o `PendingIntent` de todos os alarmes agendados antes (guardados no Room) e agenda os novos. `requestCode = AlarmEntity.id`. |
| Quando rodar | Depois de cada sync, no `BootReceiver`, ao abrir o app, no `AlarmReceiver` após cada disparo e num `PeriodicWork` diário **sem** restrição de rede (para completar a janela de 7 dias mesmo offline). |
| Reboot antes do desbloqueio | Limitação conhecida do MVP (o `BootReceiver` só roda após o 1º desbloqueio); registrar em `docs/DECISOES.md`. |
Teste: primeira sync às 15h com eventos passados e tarefas de hoje não dispara nenhum alarme.

**Evidência:** AlarmManager.set: 'If the stated trigger time is in the past, the alarm will be triggered immediately'; alarms 'will be cleared if it is turned off and rebooted' https://developer.android.com/reference/android/app/AlarmManager . The 500-per-uid cap is not in the official docs; it is reported as 'Maximum limit of concurrent alarms 500 reached for uid', e.g. https://github.com/MaikuB/flutter_local_notifications/issues/1914 (unofficial). BOOT_COMPLETED after unlock (direct boot): unverified (from knowledge).

---

## 14. 🔴 Alta · `datas-sem-hora-fuso` (Sync e dados)

**Arquivo/seção:** spec · 7.1 EventEntity/TaskEntity + 7.3 (linha 'Hora')

**Problema:** A spec guarda eventos de dia inteiro em `startAt`/`endAt` (instantes) e diz que o `due` da tarefa tem o formato `YYYY-MM-DDT00:00:00.000Z`, mas não explica como ler esse valor. Também não diz que `end.date` é exclusivo.

**Por quê:** O código Kotlin natural é `Instant.parse(due).atZone(ZoneId.systemDefault()).toLocalDate()`. Em America/Sao_Paulo (UTC−3) isso cai no dia anterior: uma tarefa para 29/09 aparece em 28/09 e o alarme das 09:00 toca um dia antes. Com `start.date` convertido para meia-noite UTC acontece o mesmo. Um evento de dia inteiro só em 29/09 vem com `end.date = 30/09`; se o fim for tratado como inclusivo, ele aparece em dois dias. E criar um evento com `end.date = start.date` gera um intervalo vazio.

**Correção sugerida:**

Acrescentar ao fim da seção 7.3:
- `due` da Tasks API: ler **só a data** (`LocalDate.parse(due.take(10))`), **nunca** converter de fuso. Ao gravar: `dueDate.toString() + "T00:00:00.000Z"`.
- Evento de dia inteiro: `EventEntity` ganha `startDate`/`endDate` (`LocalDate`, com `endDate` **exclusivo**, igual ao Google); `startAt`/`endAt` ficam nulos. Ele aparece no dia D se `startDate <= D < endDate`. Ao criar: `end.date = start.date + 1 dia`.
- Evento com hora aparece no dia D se `startAt < fimDoDia(D)` e `endAt > inícioDoDia(D)` (cobre eventos que passam da meia-noite).
- Testes obrigatórios: com fuso `America/Sao_Paulo`, a tarefa com `due=2026-09-29T00:00:00.000Z` aparece em 29/09; um evento de dia inteiro em 29/09 (end 30/09) não aparece em 30/09.

**Evidência:** Events resource: end = 'The (exclusive) end time of the event'; start.date 'yyyy-mm-dd, if this is an all-day event' https://developers.google.com/calendar/api/v3/reference/events . Tasks resource due: 'the time portion of the timestamp is discarded when setting the due date' https://developers.google.com/tasks/reference/rest/v1/tasks

---

## 15. 🔴 Alta · `outbox-ordem-idempotencia` (Sync e dados)

**Arquivo/seção:** spec · 7.1 OutboxEntity + 7.2 (Ordem/Erros)

**Problema:** O outbox é descrito apenas como linhas com `payloadJson` e `attempts`. Faltam: coalescência de operações, como resolver o `googleId` no envio, idempotência do CREATE, exclusão mútua entre syncs e um estado final para falhas.

**Por quê:** (a) Se o usuário cria e edita offline, o UPDATE é enfileirado sem `googleId`; com o payload como snapshot vira `PATCH .../events/null`. (b) Se o POST tem sucesso no servidor mas a resposta se perde (timeout), a nova tentativa cria um evento ou tarefa duplicado. (c) Abrir o app, puxar para atualizar e o Worker periódico podem processar a mesma linha ao mesmo tempo e duplicar. (d) Não diz o que fazer com DELETE de item que nunca subiu, nem com 404/410. (e) Após 5 tentativas o item fica em `PENDING_UPLOAD` para sempre, e um 400 pode travar a fila. (f) 'Sobe quando voltar a internet' depende só do periódico de 30 min.

**Correção sugerida:**

Acrescentar a subseção '7.2.1 Outbox':
- **Uma operação pendente por item** (índice único `itemType+itemLocalId`). Ao enfileirar: CREATE+UPDATE continua CREATE; CREATE+DELETE apaga as duas e o registro local, sem ir à rede; UPDATE+UPDATE vira um só; qualquer operação + DELETE vira DELETE.
- O payload **não** é snapshot: no envio, ler o estado atual da entidade e o `googleId` atual no Room.
- **Evento:** `localId` é UUID; no CREATE enviar `id = localId sem hífens` (hexadecimal é base32hex válido). Se a nova tentativa responder **409**, fazer `GET` do evento e considerá-lo criado.
- **Tarefa** (a API não aceita id do cliente): antes de reenviar um CREATE com resultado incerto (timeout depois do envio), listar a lista com `updatedMin = createdAt − 2 min` e procurar tarefa com mesmo título e `due`; se achar, adotar o `googleId` dela.
- Gravar `googleId`, `etag` e `updated` da resposta **na mesma transação** que remove a linha do outbox.
- Uma sync por vez: todas as origens usam `WorkManager.enqueueUniqueWork("sync", ExistingWorkPolicy.KEEP, ...)` com restrição `NetworkType.CONNECTED`, mais um `Mutex` no repositório. Enfileirar esse trabalho único sempre que algo entra no outbox.
- DELETE que responde 404/410 conta como sucesso. PATCH que responde 404/410: o item foi apagado no Google; apagar local e cancelar o alarme.
- Erro 4xx (exceto 401/408/409/412/429) não é retentado: `syncState = FAILED`, aviso na UI, e não bloqueia os outros itens.
- `OutboxEntity` ganha `accountId`, `nextAttemptAt`, `lastError`.

**Evidência:** Calendar events: client-supplied id uses base32hex 'lowercase letters a-v and digits 0-9', length 5–1024, UUID recommended https://developers.google.com/calendar/api/v3/reference/events/insert . Tasks resource has no client id field https://developers.google.com/tasks/reference/rest/v1/tasks

---

## 16. 🔴 Alta · `conflito-lww-relogio` (Sync e dados)

**Arquivo/seção:** spec · 7.2 Regras de sync (linha 'Conflito')

**Problema:** A regra 'último a alterar vence (`updated` do Google x `updatedAt` local)' não funciona com a ordem 'outbox antes do download', compara relógios diferentes e não protege edições locais pendentes. `TaskEntity` não tem `etag` nem o `updated` do servidor.

**Por quê:** Como o outbox roda primeiro com um PATCH sem condição, o celular sempre vence, mesmo sendo mais antigo. Exemplo: edição offline no celular às 10h, edição no PC às 11h, celular volta às 12h e a edição do PC se perde. Se o envio de um item falhar, o download seguinte sobrescreve a edição local que ainda não subiu. `updatedAt` usa o relógio do celular e `updated` usa o do Google, então qualquer diferença de relógio decide errado. E o `updated` do evento não muda quando só os lembretes mudam, então uma mudança de lembrete feita no Google não recalcula o alarme.

**Correção sugerida:**

Substituir a linha 'Conflito' por:
| Conflito | Cada entidade guarda `etag` e `remoteUpdated` (o `updated` do Google, relógio do servidor), além do `updatedAt` local. **Evento:** `PATCH` com cabeçalho `If-Match: <etag>`. Se vier **412**, fazer `GET`: se `remote.updated > updatedAt local + offsetServidor`, o Google vence (descartar a mudança local e registrar em log); senão, reenviar o PATCH com o novo etag. **Tarefa** (If-Match não documentado): `GET` antes do `PATCH`; se `remote.updated == remoteUpdated guardado`, enviar; senão, aplicar a mesma comparação. `offsetServidor` = cabeçalho HTTP `Date` menos o relógio do celular, medido a cada sync. O PATCH envia **só os campos alterados**. No download, **nunca sobrescrever** item com `syncState = PENDING_UPLOAD`. Um item conta como alterado quando o `etag` muda (o `updated` do evento não muda quando só os lembretes mudam); nesse caso, recalcular o alarme. |

**Evidência:** Calendar If-Match/412: 'you can specify an If-Match header that contains the value of the etag ... Otherwise, you will get a 412 (Precondition failed)' https://developers.google.com/calendar/api/guides/version-resources . Event.updated: 'Updating event reminders will not cause this to change' https://developers.google.com/calendar/api/v3/reference/events . tasks.patch documents no If-Match https://developers.google.com/tasks/reference/rest/v1/tasks/patch

---

## 17. 🔴 Alta · `tasks-paginacao-updatedmin` (Sync e dados)

**Arquivo/seção:** spec · 7.2 Regras de sync (linha 'Tasks') + 7.1 SyncMetaEntity

**Problema:** A linha de Tasks não fala em paginação nem em `maxResults`. O `updatedMin=<último sync>` vem do relógio do celular. E não há regra para listas criadas ou apagadas no Google.

**Por quê:** `tasks.list` devolve 20 itens por padrão (máximo 100). Com `showCompleted`, `showHidden` e `showDeleted` ligados, as primeiras 20 podem ser quase todas concluídas ou apagadas, e as tarefas pendentes simplesmente não aparecem. Usar o relógio do celular (ou gravar o horário no fim da sync) perde mudanças feitas durante a sync ou quando o relógio está adiantado. A doc não diz se o limite é inclusivo. Uma lista apagada no Google devolve 404 e deixa lista, tarefas e alarmes fantasmas. A chave `tasks_<lista>_updatedMin` também não tem `accountId`.

**Correção sugerida:**

Substituir a linha 'Tasks' por:
| Tasks | 1) `GET users/@me/lists?maxResults=100`, paginando. Lista nova: carga completa dela. Lista que sumiu (ou tarefa que responde 404): apagar lista, tarefas e alarmes locais. 2) Para cada lista: `GET lists/{id}/tasks?maxResults=100&showCompleted=true&showHidden=true&showDeleted=true[&updatedMin=X]`, **paginando com `nextPageToken` até acabar**. `X` = maior `updated` recebido na sync anterior **menos 2 minutos** (relógio do servidor, nunca `System.currentTimeMillis()`). Gravar o novo `X` só depois de todas as páginas. Upsert idempotente por `(accountId, googleId)`: se o `googleId` já existir em outra lista, atualizar `taskListId`. Chave em `SyncMeta`: `<accountId>:tasks:<listId>:updatedMin`. |

**Evidência:** tasks.list: maxResults 'The default is 20 (max allowed: 100)'; showHidden default False; showDeleted default False; response has nextPageToken https://developers.google.com/tasks/reference/rest/v1/tasks/list . updatedMin inclusivity is not documented there.

---

## 18. 🟠 Média · `canal-categoria-fsi-dnd` (Android)

**Arquivo/seção:** spec · 7.4 Regras de alarme / 7.5 onboarding / 6 (SincroniaApp: canais de notificação)

**Problema:** A spec não define o canal nem a categoria da notificação do alarme. Sem setCategory(CATEGORY_ALARM), o 'Não perturbe' pode suprimir a notificação e o full-screen intent, mesmo com 'Alarmes' permitidos. Se o canal tiver som padrão, toca um som de notificação duplicado, que também é silenciado no modo silencioso. A spec só diz 'checar canUseFullScreenIntent()', sem dizer o que fazer quando é falso. E trata POST_NOTIFICATIONS como um pedido comum, mas se for negado a notificação do alarme (e com ela a tela cheia) nunca aparece.

**Por quê:** O alarme pode não aparecer no 'Não perturbe' (item do checklist 12) ou quando a permissão de notificação for negada, e o usuário não teria como dispensar pela UI.

**Correção sugerida:**

Acrescentar na seção 7.4:
"| Canais | `alarm`: `IMPORTANCE_HIGH`, `setSound(null, null)` (o som vem do `AlarmService`), vibração do canal desligada, `lockscreenVisibility = VISIBILITY_PUBLIC`. `sync`/geral: `IMPORTANCE_LOW`. Notificação do alarme com `setCategory(NotificationCompat.CATEGORY_ALARM)` (é o que deixa passar no 'Não perturbe' com Alarmes permitidos), `PRIORITY_MAX`, `setOngoing(true)`. |"
Seção 7.5, onboarding:
"Notificações são obrigatórias para o alarme aparecer. Se negadas, mostrar faixa fixa na Hoje com botão para os ajustes do app. Android 14+: se `NotificationManager.canUseFullScreenIntent()` for falso, abrir `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` com `Uri.parse(\"package:$packageName\")`. Sem essa permissão o alarme vira notificação heads-up, por isso ela precisa ter as ações Soneca/Dispensar. (Play, no futuro: declarar em 'Conteúdo do app' que a função principal é alarme, para USE_FULL_SCREEN_INTENT e USE_EXACT_ALARM.)"

**Evidência:** https://source.android.com/docs/core/permissions/fsi-limits : a Play Store revoga FSI de apps sem função de chamada/alarme; apps de outras fontes mantêm; se negado "the system displays a persistent heads-up notification instead"; configuração via ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT. https://developer.android.com/about/versions/14/behavior-changes-14 : canUseFullScreenIntent + ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT. https://support.google.com/googleplay/android-developer/answer/13392821 : declaração de função principal no Play Console (App content). https://support.google.com/googleplay/android-developer/answer/13986130 : USE_EXACT_ALARM aceito para "calendar app that shows event notifications". Comportamento de CATEGORY_ALARM no DND e som duplicado do canal: unverified (from knowledge).

---

## 19. 🟠 Média · `boot-directboot-goasync` (Android)

**Arquivo/seção:** spec · 7.4 Regras de alarme (linha Reboot / troca de hora)

**Problema:** O BootReceiver só escuta BOOT_COMPLETED, TIME_SET e TIMEZONE_CHANGED. Depois de um reboot, BOOT_COMPLETED só chega quando o usuário desbloqueia o celular (Direct Boot). Se o aparelho reiniciar de madrugada (atualização automática), o alarme da manhã não é reagendado. O Room fica no armazenamento criptografado por credencial, indisponível antes do desbloqueio. Faltam também MY_PACKAGE_REPLACED e o uso de goAsync() (Room não roda na main thread). E no Android 15+ é proibido iniciar FGS mediaPlayback a partir de BOOT_COMPLETED, então tocar alarme perdido no boot quebra.

**Por quê:** Perder o despertador depois de uma reinicialização noturna é exatamente a falha que o M5 quer evitar. Uma consulta ao Room dentro do onReceive trava ou gera ANR.

**Correção sugerida:**

Trocar a linha por:
"| Reboot / troca de hora / atualização | `BootReceiver` (`android:directBootAware=\"true\"`) para `LOCKED_BOOT_COMPLETED`, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED`. Usar `goAsync()` + corrotina (ou enfileirar um `OneTimeWorkRequest` expedited). O `AlarmScheduler` mantém um espelho dos próximos alarmes (id, triggerAt, título, tipo) em arquivo de `context.createDeviceProtectedStorageContext()`. No `LOCKED_BOOT_COMPLETED`, reagendar a partir do espelho. No `BOOT_COMPLETED`, reagendar a partir do Room. `AlarmReceiver`, `AlarmService` e `AlarmActivity` também `directBootAware` e sem acessar o Room antes do desbloqueio (usar o espelho). Alarme cujo horário passou durante o desligamento: **não tocar** no boot (Android 15 proíbe FGS `mediaPlayback` a partir de `BOOT_COMPLETED`). Mostrar notificação 'Alarme perdido'. |"
Se for complexo demais para o MVP, registrar a escolha em docs/DECISOES.md e ao menos implementar MY_PACKAGE_REPLACED + goAsync.

**Evidência:** https://developer.android.com/training/articles/direct-boot : credential encrypted storage "is NOT available (requires user unlock)"; LOCKED_BOOT_COMPLETED + android:directBootAware; createDeviceProtectedStorageContext(); caso de uso citado: "Apps that have scheduled notifications, such as alarm clock apps". https://developer.android.com/about/versions/15/behavior-changes-15 : apps com target 15 não podem iniciar FGS dos tipos dataSync, camera, mediaPlayback... a partir de BOOT_COMPLETED (ForegroundServiceStartNotAllowedException).

---

## 20. 🟠 Média · `alarmactivity-lockscreen-pendingintent` (Android)

**Arquivo/seção:** spec · 7.4 Regras de alarme / 6. Arquitetura (alarm/)

**Problema:** A spec não define os atributos da AlarmActivity para aparecer sobre a tela de bloqueio e ligar a tela, nem as regras dos PendingIntents. setShowWhenLocked/setTurnScreenOn e os atributos de manifesto só existem a partir da API 27, e o minSdk é 26. Também não exige requestCode único por alarme. Com requestCode repetido e FLAG_UPDATE_CURRENT, cada novo alarme sobrescreve o anterior e só o último toca.

**Por quê:** Sem showWhenLocked, a Activity abre atrás do bloqueio e o critério do M5 ('toca com a tela bloqueada') falha. Com requestCode fixo, a maior parte dos alarmes some sem nenhum erro.

**Correção sugerida:**

Acrescentar na seção 7.4:
"| AlarmActivity | No manifesto: `android:showWhenLocked=\"true\" android:turnScreenOn=\"true\" android:excludeFromRecents=\"true\" android:launchMode=\"singleInstance\" android:taskAffinity=\"\" android:exported=\"false\"`. Na API 26, em `onCreate`: `window.addFlags(FLAG_SHOW_WHEN_LOCKED or FLAG_TURN_SCREEN_ON)`. Em todas: `FLAG_KEEP_SCREEN_ON`. Não chamar `requestDismissKeyguard` ao Dispensar. Layout edge-to-edge com insets (padrão no target 35+). |
| PendingIntent | Intent explícito para `AlarmReceiver`, `requestCode` único e estável = `AlarmEntity.id` (Int), `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`. O mesmo requestCode é usado para cancelar. O `showIntent` do `AlarmClockInfo` abre o item no app. Teste unitário: agendar 3 alarmes e verificar 3 PendingIntents distintos. |"

**Evidência:** https://developer.android.com/about/versions/15/behavior-changes-15 : edge-to-edge é padrão para target 35. setShowWhenLocked/setTurnScreenOn/atributos de manifesto adicionados na API 27 e a semântica de requestCode/FLAG_UPDATE_CURRENT: unverified (from knowledge).

---

## 21. 🟠 Média · `401-cleartoken-reconsent` (Android)

**Arquivo/seção:** spec · 6. Fluxo de autorização, passos 5 e 6

**Problema:** No 401, a spec manda chamar authorize() de novo e repetir. Mas o AuthorizationClient guarda o token em cache: sem clearToken(token antigo), authorize() pode devolver o mesmo token inválido. O passo 6 também não diz o que fazer quando authorize() pede interação (hasResolution) dentro do SyncWorker, onde não há Activity para mostrar o consentimento.

**Por quê:** Pode entrar em loop de 401 até o token expirar, ou o app pode mandar o usuário para o onboarding e apagar dados por um problema de consentimento temporário descoberto em segundo plano.

**Correção sugerida:**

Trocar os passos 5 e 6 por:
"5. Tratar `401` num `okhttp3.Authenticator`: chamar `Identity.getAuthorizationClient(context).clearToken(ClearTokenRequest.builder().setToken(tokenAntigo).build())`, depois `authorize()` silencioso, e repetir a requisição **uma vez** (se ela já foi repetida, desistir).
6. Se `authorize()` devolver `hasResolution()` fora de uma Activity (ex.: no `SyncWorker`), **não** apagar dados: marcar `authState = NEEDS_CONSENT` no DataStore, pausar o sync, manter os alarmes locais e mostrar notificação/faixa 'Reconecte sua conta Google' que abre o `PendingIntent` de consentimento no app. Só ir para o onboarding se o usuário recusar."

**Evidência:** https://developer.android.com/identity/authorization : documenta `clearToken(ClearTokenRequest.builder().setToken(invalidAccessToken).build())` para "make sure that the next authorization request for an access token goes to the OAuth server"; authorize() devolve accessToken via authorizationResult.accessToken e hasResolution() para consentimento.

---

## 22. 🟠 Média · `desconectar-cancelar-alarmes` (Android)

**Arquivo/seção:** spec · 12. Checklist (Desconectar) / M1

**Problema:** 'Desconectar' só revoga o acesso e apaga Room + DataStore. Os PendingIntents já registrados no AlarmManager e o WorkManager periódico continuam ativos: alarmes de dados apagados ainda tocam e o SyncWorker continua rodando. Falta também clearCredentialState() do Credential Manager. E revokeAccess exige um Account e revoga todos os escopos, detalhes que a spec não cita.

**Por quê:** Depois de desconectar, o usuário recebe alarmes 'fantasmas' que abrem itens que não existem mais, e o worker fica falhando em segundo plano.

**Correção sugerida:**

Trocar o item do checklist por:
"- [ ] 'Desconectar', nesta ordem: `AuthorizationClient.revokeAccess(RevokeAccessRequest.builder().setAccount(Account(email, \"com.google\")).setScopes(escopos).build())` (revoga **todos** os escopos do app) → `CredentialManager.clearCredentialState(ClearCredentialStateRequest())` → `WorkManager.cancelUniqueWork(\"sync\")` → `AlarmScheduler.cancelAll()` (cancelar cada PendingIntent no AlarmManager e parar o `AlarmService` se estiver tocando) → apagar Room + DataStore + espelho de alarmes. Se o revoke falhar por estar offline, desconectar localmente mesmo assim e registrar no log."

**Evidência:** https://developer.android.com/identity/authorization : RevokeAccessRequest.builder().setAccount(account).setScopes(requestedScopes) e "Calling revokeAccess() revokes all scopes previously granted to the user for your application". https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation : "When a user signs out of your app, call the API clearCredentialState()".

---

## 23. 🟠 Média · `versoes-agp9-hilt-target` (Android)

**Arquivo/seção:** spec · 5. Stack

**Problema:** 'Use sempre a versão estável mais recente' fica ambíguo e arriscado. Hoje isso significa AGP 9.x (Kotlin embutido: não se aplica mais org.jetbrains.kotlin.android, Gradle ≥ 9.1) e Hilt ≥ 2.59, que exige AGP 9. Se o Claude Code, pelo que aprendeu no treino, montar AGP 8 + kotlin-android com o Hilt mais recente (ou o contrário), o build quebra no M0. O targetSdk 'mais recente estável' hoje é 37 (Android 17), e o Play exige ≥ 36 desde 31/08/2026. Com target 37 entra o endurecimento de áudio em segundo plano, já coberto na correção do som no serviço.

**Por quê:** Há risco real de o Claude Code travar no M0 com erros de compatibilidade entre plugins e de escolher um targetSdk que ele 'conhece' (34/35) em vez do atual.

**Correção sugerida:**

Seção 5, trocar a linha SDK e acrescentar uma linha de build:
"| Build | AGP 9.x com Kotlin embutido (**não** aplicar `org.jetbrains.kotlin.android`), Gradle ≥ 9.1, JDK 17, KSP compatível com a versão do Kotlin, Hilt ≥ 2.59 (exige AGP 9). Antes do M0, confirmar as versões atuais e fixar tudo em `libs.versions.toml`. Se alguma combinação não compilar, fixar a última que compila e registrar em `docs/DECISOES.md`. |
| SDK | `minSdk 26`, `compileSdk`/`targetSdk 37` (Android 17, estável desde jun/2026; o Play exige ≥ 36 desde 31/08/2026). Se algum plugin ainda não suportar 37, usar 36 e registrar. |"

**Evidência:** https://developer.android.com/build/releases/agp-9-0-0-release-notes : Kotlin embutido ativado por padrão ("You no longer need to apply the org.jetbrains.kotlin.android plugin"), Gradle mínimo 9.1.0, KGP mínimo 2.2.10, KSP 2.2.10-2.0.2. https://github.com/google/dagger/releases/tag/dagger-2.59 : "if you use the Hilt Gradle Plugin, AGP 9 is now a requirement along with AGP 9's own requirements like Gradle 9.1+". https://developer.android.com/google/play/requirements/target-sdk : a partir de 31/08/2026, apps novos e atualizações precisam de API 36+. https://developer.android.com/blog/posts/android-17-is-here : Android 17 em 16/06/2026.

---

## 24. 🟠 Média · `hilt-work-initializer` (Android)

**Arquivo/seção:** spec · 5. Stack (Segundo plano) / 6. Arquitetura (SincroniaApp) / 7.2 Quando

**Problema:** O SyncWorker vai precisar de repositórios injetados pelo Hilt, mas a stack não inclui androidx.hilt:hilt-work, e a regra da seção 5 proíbe adicionar bibliotecas fora da lista. A spec também não diz que é preciso implementar Configuration.Provider e remover o inicializador padrão do WorkManager no manifesto. Sem isso, o worker falha em tempo de execução ao ser instanciado. E o 'intervalo de sync' configurável não tem limite mínimo (o WorkManager usa 15 min como piso) nem política de atualização do trabalho periódico.

**Por quê:** Esse é um ponto clássico de travamento no M2, e a própria regra da spec atrapalha a correção.

**Correção sugerida:**

Seção 5, linha Segundo plano: "WorkManager + `androidx.hilt:hilt-work` + `ksp(\"androidx.hilt:hilt-compiler\")` (workers com `@HiltWorker`/`@AssistedInject`)".
Seção 6, linha SincroniaApp: "(Application, Hilt, canais de notificação, implementa `Configuration.Provider` com `HiltWorkerFactory`)". Acrescentar: "No manifesto, remover o inicializador padrão: `<provider android:name=\"androidx.startup.InitializationProvider\" android:authorities=\"${applicationId}.androidx-startup\" android:exported=\"false\" tools:node=\"merge\"><meta-data android:name=\"androidx.work.WorkManagerInitializer\" android:value=\"androidx.startup\" tools:node=\"remove\"/></provider>`".
Seção 7.2, linha Quando: "...WorkManager periódico via `enqueueUniquePeriodicWork(\"sync\", ExistingPeriodicWorkPolicy.UPDATE, ...)`, padrão 30 min, opções 15/30/60 min (15 é o mínimo do WorkManager), com rede".

**Evidência:** https://developer.android.com/training/dependency-injection/hilt-jetpack : dependências `androidx.hilt:hilt-work` + `ksp(\"androidx.hilt:hilt-compiler\")`, @HiltWorker, Application implementando Configuration.Provider com HiltWorkerFactory, e "you also must remove the default initializer from the AndroidManifest.xml". Intervalo mínimo de 15 min do PeriodicWorkRequest: unverified (from knowledge).

---

## 25. 🟠 Média · `otimizacao-bateria-politica` (Android)

**Arquivo/seção:** figma · TELAS 2. Permissões (e spec 7.5 / F10)

**Problema:** O Figma mostra 'Ignorar otimização de bateria' com um botão 'Permitir', o que leva o Claude Code a usar ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS + a permissão REQUEST_IGNORE_BATTERY_OPTIMIZATIONS. A política do Google Play proíbe o pedido direto, a menos que a função principal seja prejudicada, e setAlarmClock() já dispara durante o Doze. A spec (7.5) também coloca isso no onboarding sem dizer qual intent usar. Além disso, a tela de permissões não mostra 'Alarmes e lembretes' (necessário no Android 12/12L) e não indica que 'tela cheia' só aparece no Android 14+.

**Por quê:** Quando o app for para o Play (plano de SaaS), isso gera risco de rejeição. E o pedido é desnecessário para o alarme funcionar, o que só acrescenta atrito no onboarding.

**Correção sugerida:**

Figma, trocar o item 2 por:
"2. Permissões: itens com ícone, texto curto e botão: Notificações ('Permitir', obrigatório para o alarme aparecer); Alarmes em tela cheia ('Abrir ajustes', só Android 14+); Alarmes e lembretes ('Abrir ajustes', só Android 12). Abaixo, um cartão discreto e opcional: 'Se os alarmes atrasarem no seu celular, ajuste a bateria', com o link 'Abrir ajustes de bateria'."
Spec 7.5, trocar a última frase por:
"No onboarding: pedir notificações e checar `canUseFullScreenIntent()` (Android 14+). Otimização de bateria **não** faz parte do onboarding: `setAlarmClock()` já dispara no Doze. Em Configurações, só um atalho opcional que abre `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`. **Não** declarar `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` nem usar `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`."

**Evidência:** https://developer.android.com/training/monitoring-device-state/doze-standby : "Google Play policies prohibit apps from requesting direct exemption from Power Management features—Doze and App Standby—in Android 6.0 and above unless the core function of the app is adversely affected"; "Most apps can invoke an intent that contains the ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS"; "Alarms set with setAlarmClock() continue to fire normally. The system exits Doze shortly before those alarms fire." A lista de casos aceitáveis (mensagens, VOIP, segurança, automação, dispositivos acompanhantes) não inclui calendário/alarme.

---

## 26. 🟠 Média · `tasks-pagination-updatedmin-lists` (APIs Google)

**Arquivo/seção:** spec · 7.2 Regras de sync (linha 'Tasks')

**Problema:** The Tasks rule does not require pagination, but tasks.list returns only 20 items per page by default (max 100). With showCompleted, showHidden and showDeleted all true, the first sync exceeds 20 almost immediately. `updatedMin=<último sync>` suggests using the phone's clock. Nothing says to re-list the task lists, so new lists are never picked up and deleted lists (TaskList has no deleted flag) leave orphan tasks and alarms.

**Por quê:** Tasks would be silently missing from the first sync. Device clock skew can skip changes forever. Deleting a list in Google Tasks would leave ringing alarms for tasks that no longer exist.

**Correção sugerida:**

Substituir a linha 'Tasks' de 7.2 por:
"| Tasks, listas | A cada sync: `GET https://tasks.googleapis.com/tasks/v1/users/@me/lists` (paginar por `nextPageToken`). Lista nova → inserir. Lista que sumiu (a API não marca lista excluída) → apagar localmente a lista, suas tarefas e seus alarmes |
| Tasks, tarefas | Para cada lista: `GET lists/{id}/tasks?showCompleted=true&showHidden=true&showDeleted=true&maxResults=100` + `updatedMin` (omitir na primeira vez), **paginando** por `nextPageToken` até o fim (o padrão da API é só 20 por página). Guardar em `tasks_<lista>_updatedMin` o **maior `updated` recebido do Google** (relógio do servidor, nunca `Instant.now()` do celular) |"

**Evidência:** https://developers.google.com/workspace/tasks/reference/rest/v1/tasks/list: maxResults "Default: 20 (max: 100)". showHidden and showDeleted default False. updatedMin is "Lower bound for a task's last modification time". https://developers.google.com/workspace/tasks/reference/rest/v1/tasklists/list: tasklists paginate with nextPageToken. That TaskList has no deleted flag is unverified (from knowledge; the resource has id/title/updated/etag/selfLink).

---

## 27. 🟠 Média · `tasks-due-utc-date-shift` (APIs Google)

**Arquivo/seção:** spec · 7.3 Evento x Tarefa na API (linha 'Hora') + 8. Marcos (M4)

**Problema:** `due` comes back as `YYYY-MM-DDT00:00:00.000Z`. If Claude Code parses it as an Instant and converts it to ZoneId.systemDefault() (America/Sao_Paulo, UTC-3), which the spec's `core/time` utilities encourage, the result is 21:00 on the previous day. Every Google task then shows on the wrong day and its default 09:00 alarm fires a day early. The M4 check also says a created task 'aparece no Google Agenda', which only happens when the task has a date.

**Por quê:** This is a one-line pitfall that silently corrupts every task date and alarm for a Brazilian user. It is the most likely time-zone bug in the app.

**Correção sugerida:**

Na linha 'Hora' da tabela 7.3, coluna Tarefa, trocar por: "`due` = `YYYY-MM-DDT00:00:00.000Z` (**só data**). **Ler** pegando só os 10 primeiros caracteres (`LocalDate.parse(due.substring(0, 10))`); **nunca** converter para fuso. **Escrever** como `\"${date}T00:00:00.000Z\"`. Teste obrigatório: `due=2026-09-29T00:00:00.000Z` com fuso `America/Sao_Paulo` → 29/09."
No M4, trocar "criar tarefa → aparece no Google Tasks e no Google Agenda" por "criar tarefa → aparece no Google Tasks e, se tiver data, no Google Agenda".

**Evidência:** https://developers.google.com/workspace/tasks/reference/rest/v1/tasks: due: "Only date information is recorded; the time portion of the timestamp is discarded when setting this field. It isn't possible to read or write the time that a task is scheduled for using the API." This also confirms that no newer field exposes a time set in the Calendar UI, so the spec's local-alarm-time decision is correct. The UTC→São Paulo previous-day shift is unverified (from knowledge; standard java.time behavior).

---

## 28. 🟠 Média · `error-taxonomy-410-meanings` (APIs Google)

**Arquivo/seção:** spec · 7.2 Regras de sync (linhas 'Erros' e 'Agenda, depois')

**Problema:** The only 410 handling in the spec is 'apagar token e refazer a sincronização completa', and the only errors covered are retryable ones. Calendar returns 410 for three different reasons: fullSyncRequired, updatedMinTooLongAgo, and `deleted`. `deleted` is what you get when DELETE targets an event already deleted on Google, which is common when an offline delete is replayed. The spec also says nothing about permanent errors (400, 403 forbiddenForNonOrganizer / insufficientPermissions, 404, 409, 412). It lists 403 rateLimitExceeded but not userRateLimitExceeded or quotaExceeded.

**Por quê:** If Claude Code handles 410 globally (for example in the interceptor), a replayed delete triggers a full re-sync every run and the outbox item never clears. Permanent 4xx errors will be retried forever or dropped silently, and the item stays PENDING_UPLOAD.

**Correção sugerida:**

Substituir a linha 'Erros' de 7.2 por:
"| Erros transitórios | `429`, `403` com `reason` `rateLimitExceeded`/`userRateLimitExceeded`/`quotaExceeded`, `5xx` e falha de rede → backoff exponencial (1s, 2s, 4s, 8s, máx. 5 tentativas); o item continua no outbox |
| Erros definitivos | `400`, `403` com outro `reason` (ex.: `forbiddenForNonOrganizer`, `insufficientPermissions`), `404` em UPDATE, `409` → **não repetir**: tirar do outbox, marcar `syncState=CONFLICT`, baixar a versão do Google e avisar o usuário |
| Excluir algo que já sumiu | `DELETE` que retorna `404` ou `410` (`reason: deleted`) = **sucesso**: remover do outbox e do Room |
| 410 na listagem | Só `GET events?syncToken=...` com `410` dispara o sync completo. Ler o `error.errors[0].reason` do corpo; **nunca** tratar 410 de forma global no interceptor |"

**Evidência:** https://developers.google.com/workspace/calendar/api/guides/errors: 410 reasons are fullSyncRequired ("Sync token is no longer valid"), updatedMinTooLongAgo, and deleted ("Resource has been deleted", e.g. deleting an already-deleted event). 403 reasons are userRateLimitExceeded, rateLimitExceeded, quotaExceeded, forbiddenForNonOrganizer. 409 duplicate. 412 conditionNotMet. 429 rateLimitExceeded.

---

## 29. 🟠 Média · `token-lifecycle-cleartoken-background` (APIs Google)

**Arquivo/seção:** spec · 6. Fluxo de autorização (passos 3–7) + 12. Checklist

**Problema:** Step 5 says to call authorize() again on 401. Play Services caches access tokens, though, so without `clearToken()` the retry can return the same bad token. The spec also keeps one in-memory token for the whole session, even though tokens last about 1 h. It does not say what the SyncWorker should do when authorize() returns hasResolution() without an Activity. That is exactly how the 7-day Testing-mode expiry and granular un-grant show up, not as a 'revogação'. `revokeAccess()` needs a RevokeAccessRequest with account and scopes, and the Credential Manager state is never cleared.

**Por quê:** Without these steps the app can loop on 401 and incorrectly kick Gustavo to onboarding. Background syncs can also fail silently once the Testing grant expires. 'Desconectar' may not compile or may leave the account selected.

**Correção sugerida:**

Substituir os passos 3 a 7 da seção 6 por:
"3. O resultado traz o **access token** (vale ~1 h). Guardar **só em memória** e chamar `authorize()` de novo **no início de cada sync** (é silencioso; o Play Services devolve o token em cache ou renovado).
4. Interceptor do OkHttp coloca `Authorization: Bearer <token>`.
5. Resposta `401` → `clearToken(ClearTokenRequest.builder().setToken(tokenRuim).build())`, depois `authorize()` e repetir a requisição **uma vez**. Se vier 401 de novo, tratar como autorização perdida.
6. Se `authorize()` retornar `hasResolution()` fora de uma Activity (ex.: no `SyncWorker`), não abrir nada: salvar estado `NEEDS_REAUTH`, mostrar notificação \"Toque para reconectar sua conta Google\" e encerrar o worker com `Result.success()`. Isso acontece, por exemplo, quando o app está em \"Testing\" e a autorização expira em 7 dias.
7. **Não** guardar refresh token no aparelho (não é necessário).
8. Desconectar: `revokeAccess(RevokeAccessRequest.builder().setAccount(Account(email, \"com.google\")).setScopes(escopos).build())` + `CredentialManager.clearCredentialState()` + apagar Room e DataStore + cancelar alarmes e trabalhos do WorkManager."

**Evidência:** https://developer.android.com/identity/authorization documents `clearToken(ClearTokenRequest.builder().setToken(invalidAccessToken).build())` ("clear the local cache to make sure that the next authorization request for an access token goes to the OAuth server"), shows RevokeAccessRequest.builder().setAccount(account).setScopes(...), and says revokeAccess "revokes all scopes ... [and] clears any locally cached tokens". https://developers.google.com/identity/protocols/oauth2 confirms the Testing-status refresh token expires in 7 days, so the spec's '7 dias' claim is correct. Calling clearToken on HTTP 401 specifically is unverified (from knowledge; the docs phrase it as an invalid-token case).

---

## 30. 🟠 Média · `scopes-minimal-granular-consent` (APIs Google)

**Arquivo/seção:** spec · 3. Pré-requisitos (escopos); 4. Fora do MVP; 6. passo 2; 11. regra 3

**Problema:** (a) `calendar.calendarlist.readonly` is requested only 'para o futuro'. The app uses the `primary` alias, and the calendar's timeZone and defaultReminders come back in every events.list response under calendar.events. That breaks the spec's own 'Escopos mínimos' rule and adds a scope to verify later. (b) Scopes are written as short names, but `Scope(...)` needs the full URL. (c) Google shows granular consent on Android when more than one non-sign-in scope is requested, so the user can uncheck Tasks or Calendar. The spec never checks `AuthorizationResult.getGrantedScopes()`, and 403 insufficientPermissions would reach the error handling with no plan.

**Por quê:** Claude Code may pass 'calendar.events' as the scope string and fail, or later call calendars.get / calendarList.get, which calendar.events does not authorize. A partial grant would leave half the sync failing with no explanation.

**Correção sugerida:**

Seção 3: "- [ ] Escopos (Google Auth Platform → Acesso a dados): `https://www.googleapis.com/auth/calendar.events` e `https://www.googleapis.com/auth/tasks`".
Regra 3 (seção 11): "**Não** usar API key, client secret ou escopos além de `https://www.googleapis.com/auth/calendar.events` e `https://www.googleapis.com/auth/tasks` (sempre a URL completa em `Scope(...)`)."
Seção 4, Fora do MVP: trocar "várias agendas além da `primary` (só leitura da lista para o futuro)" por "várias agendas além da `primary`".
Seção 6, passo 2: "`AuthorizationClient.authorize()` com os 2 escopos. Se vier `hasResolution()`, abrir o `PendingIntent` de consentimento. Depois conferir `result.grantedScopes`: se faltar Agenda ou Tasks (o usuário pode desmarcar um deles), mostrar explicação e pedir de novo; não sincronizar a parte sem permissão. `403 insufficientPermissions` → mesmo tratamento. Fuso e lembretes padrão da agenda vêm na resposta de `events.list` (`timeZone`, `defaultReminders`); **não** chamar `calendars.get` nem `calendarList` (exigem outros escopos)."

**Evidência:** https://developers.google.com/workspace/calendar/api/auth: "choose the most narrowly focused scope possible and avoid requesting scopes that your app doesn't require". https://developers.google.com/workspace/calendar/api/v3/reference/calendars/get and https://developers.google.com/workspace/calendar/api/v3/reference/calendarList/get: neither accepts calendar.events. https://developers.google.com/workspace/calendar/api/v3/reference/events/list: the response has defaultReminders and timeZone, and calendar.events is an accepted scope. https://developers.google.com/identity/protocols/oauth2/resources/granular-permissions: granular consent applies when requesting "more than one non-Sign-In scope" and covers Android ("Your app should always check which scopes were granted"). AuthorizationResult.getGrantedScopes() exists: https://developers.google.com/android/reference/com/google/android/gms/auth/api/identity/AuthorizationResult. Scope sensitivity levels (both believed sensitive, not restricted) are unverified (from knowledge).

---

## 31. 🟠 Média · `reminders-usedefault` (APIs Google)

**Arquivo/seção:** spec · 7.4 Regras de alarme (linha 'Evento com hora') + 7.3

**Problema:** For the event alarm, the spec takes 'o primeiro lembrete popup' and otherwise falls back to 10 min. Most Google events have `reminders.useDefault=true` and no overrides. Their real reminder is the calendar's `defaultReminders`, which comes at the top level of the events.list response, and the spec never tells Claude Code to store it. 'Primeiro' is also not necessarily the earliest reminder. The spec does not define the write format for the editor's 'lembrete/alarme' field either.

**Por quê:** Alarms would ring 10 min before events for which Gustavo set 30 min or 1 h in Google as his calendar default. That is the core feature behaving differently from Google. Without a write format, events created in the app may get unexpected reminders on Google's side.

**Correção sugerida:**

Linha 'Evento com hora' em 7.4: "| Evento com hora | Alarme em `startAt − N` min, onde N = **maior** `minutes` entre os lembretes `method=popup` do evento (o mais cedo). Se `reminders.useDefault=true`, usar os `defaultReminders` da agenda, que vêm no topo de toda resposta de `events.list` (guardar em `SyncMetaEntity` como `calendar_primary_defaultReminders`, junto com `calendar_primary_timeZone`). Se mesmo assim não houver `popup`, usar o padrão das Configurações (10 min) |"
Acrescentar linha em 7.3: "| Lembrete | `reminders: {\"useDefault\": false, \"overrides\": [{\"method\": \"popup\", \"minutes\": N}]}` (máx. 5 overrides); se o usuário não mexer no lembrete, enviar `{\"useDefault\": true}` | — (alarme só local) |"

**Evidência:** https://developers.google.com/workspace/calendar/api/v3/reference/events: reminders.useDefault is "Whether the default reminders of the calendar apply to the event". overrides: "The maximum number of override reminders is 5". Methods are email and popup. https://developers.google.com/workspace/calendar/api/v3/reference/events/list response: defaultReminders are "The default reminders on the calendar for the authenticated user. These reminders apply to all events on this calendar that do not explicitly override them."

---

## 32. 🟠 Média · `event-types-readonly-declined` (APIs Google)

**Arquivo/seção:** spec · 7.3 Observações + 7.4 Regras de alarme

**Problema:** The primary calendar returns several eventType values (default, birthday, fromGmail, focusTime, outOfOffice, workingLocation), plus invitations where Gustavo is only a guest. The spec treats every event as fully editable and alarmable. The API restricts which fields of fromGmail and birthday events can change, and an attendee without guestsCanModify gets 403 forbiddenForNonOrganizer on time or title edits. Declined invitations and working-location blocks would also ring full-screen alarms.

**Por quê:** Edits through the editor would produce permanent outbox errors. Unwanted full-screen alarms, such as 'Escritório 09:00' or meetings Gustavo declined, undermine trust in the alarm.

**Correção sugerida:**

Acrescentar às Observações de 7.3:
"- Tipos de evento (`eventType`): `default`, `birthday`, `fromGmail`, `focusTime`, `outOfOffice`, `workingLocation`. No MVP, só `default` é editável no app; os demais aparecem como **somente leitura** (o editor só permite mudar o alarme local).
- Se o usuário não for o organizador (`organizer.self != true`) e `guestsCanModify` for falso, abrir o editor como somente leitura (PATCH de título/horário retorna `403 forbiddenForNonOrganizer`)."
Acrescentar linha em 7.4: "| Sem alarme automático | Eventos `workingLocation` e eventos em que o usuário recusou (`attendees[].self=true` com `responseStatus=\"declined\"`) |"

**Evidência:** https://developers.google.com/workspace/calendar/api/v3/reference/events: eventType values are birthday, default, focusTime, fromGmail, outOfOffice, workingLocation. guestsCanModify "default is False". attendees[].self and responseStatus "declined". https://developers.google.com/workspace/calendar/api/guides/event-types: fromGmail events "can only be updated for specific properties like colorId, reminders, visibility, transparency, status, attendees, and extended properties", and birthday events have fixed constraints. https://developers.google.com/workspace/calendar/api/guides/errors: 403 forbiddenForNonOrganizer.

---

## 33. 🟠 Média · `tasks-move-default-uncomplete` (APIs Google)

**Arquivo/seção:** spec · 7.3 Evento x Tarefa na API

**Problema:** The editor has a 'lista' field (Figma screen 6: 'Em Tarefa: título, data, hora do alarme, lista, notas'), but the spec maps editing only to PATCH. PATCH cannot move a task between lists; that needs `tasks.move` with `destinationTasklist`, and recurring tasks cannot move lists at all. Creating in `@default` without resolving the real list ID means the next sync returns the same task under the real list ID, which can duplicate it or confuse `TaskEntity.taskListId`. Un-completing a task is not specified either.

**Por quê:** Claude Code will PATCH a different list ID in the URL or body, which fails or does nothing, and then likely work around it by delete + create, which loses the ID and history. The @default mismatch causes duplicate tasks after sync.

**Correção sugerida:**

Acrescentar linhas na tabela 7.3 (coluna Tarefa):
"| Mudar de lista | — | `POST .../lists/{origem}/tasks/{id}/move?destinationTasklist={destino}` (PATCH não move; tarefa recorrente não pode mudar de lista → mostrar aviso) |
| Desfazer conclusão | — | `PATCH` com `{\"status\":\"needsAction\",\"completed\":null}` |"
Trocar a linha 'Lista padrão', coluna Tarefa, por: "`@default`, mas resolver para o ID real na primeira sync (`GET users/@me/lists/@default`) e **sempre** gravar o ID real em `TaskEntity.taskListId`".

**Evidência:** https://developers.google.com/workspace/tasks/reference/rest/v1/tasks/move: destinationTasklist "If set, the task is moved from tasklist to the destinationTasklist list". "Recurrent tasks cannot currently be moved between lists." https://developers.google.com/workspace/tasks/reference/rest/v1/tasks: parent is "Output only ... Use the 'move' method". The `@default` tasklists.get alias and `completed:null` for un-completing are unverified (from knowledge).

---

## 34. 🟠 Média · `conflict-rule-unimplementable` (APIs Google)

**Arquivo/seção:** spec · 7.2 Regras de sync (linhas 'Ordem' e 'Conflito')

**Problema:** The rule 'Último a alterar vence (updated do Google x updatedAt local)' cannot happen with the stated order, which pushes the outbox first. A PATCH/DELETE is sent blindly and always overwrites Google, so the comparison never runs. `EventEntity.etag` is stored but never used, and the comparison mixes the server's clock with the phone's.

**Por quê:** Claude Code will implement 'local always wins' while believing it implemented last-writer-wins. Edits made in Google Calendar while the phone was offline get overwritten silently, and CONFLICT is never set.

**Correção sugerida:**

Substituir a linha 'Conflito' de 7.2 por:
"| Conflito | Evento: `PATCH`/`DELETE` com cabeçalho `If-Match: <etag salvo>`. Se vier `412` (`conditionNotMet`), buscar o evento no Google e comparar o `updated` do Google com o `updatedAt` local (ambos em UTC): o mais recente vence; se o local vencer, reenviar sem `If-Match`; se o Google vencer, descartar o item do outbox e gravar a versão do Google. Tarefa: antes do `PATCH`, fazer `GET` da tarefa e aplicar a mesma comparação. Registrar em log |"

**Evidência:** https://developers.google.com/workspace/calendar/api/guides/errors: "412: Precondition Failed – conditionNotMet – ETag in If-Match header no longer valid". Spec 7.2 'Ordem': "1) processar outbox (subir alterações locais) → 2) baixar mudanças". If-Match support in the Tasks API is unverified (from knowledge), hence GET-then-compare for tasks.

---

## 35. 🟠 Média · `outbox-e-conflito-subespecificados` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 7.1 Entidades / 7.2 Regras de sync

**Problema:** O outbox tem 4 operações e o estado `CONFLICT`, mas não diz: como juntar operações do mesmo item (criar offline e depois editar ou excluir antes de subir), como gravar o `googleId` depois do CREATE, o que fazer após as 5 tentativas nem com 404/400. “Último a alterar vence” compara o `updated` do Google com o relógio do celular (`updatedAt`), mas como o outbox sobe antes do download, a alteração local sempre sobrescreve de qualquer forma. `CONFLICT` não tem quem o produza nem tela que o resolva.

**Por quê:** É a parte mais sutil do MVP. Sem regras, o agente ou inventa semânticas que duplicam itens no Google (CREATE repetido) ou faz um motor de conflito grande demais para um desenvolvedor solo. Item preso no outbox bloqueia a fila para sempre.

**Correção sugerida:**

Na seção 7.2, trocar a linha “Conflito” por:
“| Outbox | No máximo **uma** entrada por item. Ao enfileirar: CREATE + UPDATE → fica CREATE com o payload novo; CREATE + DELETE → apagar a entrada e o item local (nunca subiu); UPDATE + UPDATE → um UPDATE com o payload novo; UPDATE + DELETE → DELETE; COMPLETE é um UPDATE de `status`. Depois de um CREATE bem-sucedido, gravar o `googleId` retornado. 404 em UPDATE/DELETE → o item já não existe no Google: apagar localmente. Outro 4xx (menos 401/429) ou 5 falhas seguidas → `syncState = ERROR`, sair da fila e mostrar “Não foi possível sincronizar” com a opção “Descartar alteração”. |
| Conflito | Sem comparar relógios no MVP: como o outbox sobe **antes** do download, a alteração local pendente vence; no download, itens com entrada no outbox não são sobrescritos. |”
Na seção 7.1, trocar “`syncState`: `SYNCED`, `PENDING_UPLOAD`, `CONFLICT`.” por “`syncState`: `SYNCED`, `PENDING_UPLOAD`, `ERROR`.”

**Evidência:** Interno: Spec §7.1 linhas 141/144 e §7.2 linhas 150/154. Nada a verificar externamente.

---

## 36. 🟠 Média · `escopo-grade-semanal-e-cortes` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 4. Escopo do MVP (F3, F5, Fora) / 3 e 11 (escopos) / Figma tela 4

**Problema:** O F5 (“visão de semana/dia”) e a tela 4 do Figma (“grade de horários com blocos de evento”) pedem uma grade de horários própria: layout de eventos sobrepostos, rolagem de 24 h, faixa de dia inteiro e linha “agora”. O Compose não tem isso pronto, e é provavelmente a maior tarefa de UI do projeto, com pouco ganho sobre o próprio Google Agenda. O F3 traz `parentId`/`position` (subtarefas e ordem) sem dizer se há hierarquia ou reordenação. O escopo `calendar.calendarlist.readonly` é pedido mas não é usado no MVP (só `primary`), o que contradiz “Escopos mínimos” da seção 10.

**Por quê:** Para um desenvolvedor solo, a grade semanal pode custar mais que M2+M3 juntos e atrasar o que importa (sync e alarme). Um escopo sem uso é mais um item na tela de consentimento e mais uma justificativa na verificação do Google depois.

**Correção sugerida:**

Trocar o F5 por:
“| F5 | Tela **Agenda**: faixa com os 7 dias da semana (deslizar troca a semana; dia atual em círculo azul; ponto nos dias com itens) e, abaixo, a **lista** do dia selecionado no mesmo formato da tela Hoje. Tela **Tarefas**: chips por lista; subtarefas recuadas abaixo da tarefa-mãe; sem reordenar. |”
Em “Fora do MVP”, acrescentar: “grade de horários semanal · reordenar tarefas · criar subtarefas”.
Na seção 3, na regra 3 e onde mais aparecer, usar só `calendar.events` e `tasks`. Acrescentar em “Fora do MVP”: “`calendar.calendarlist.readonly` (pedir por autorização incremental quando houver várias agendas)”.
No Figma, trocar a tela 4 por: “4. Agenda: faixa com os 7 dias da semana no topo (dia atual em círculo azul, ponto nos dias com compromissos) e, abaixo, a lista do dia selecionado com cartões de evento e de tarefa, igual à tela Hoje.”

**Evidência:** Spec §4 linha 54, §7.1 linha 138, §3 linha 36; Figma tela 4. Escopo: https://developers.google.com/calendar/api/auth (calendarlist.readonly = “See the list of Google calendars you're subscribed to”, desnecessário para `primary`). https://developer.android.com/identity/authorization (autorização incremental).

---

## 37. 🟠 Média · `dados-so-locais-e-saas-barato` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 7.3 Observações / 10. Preparação para SaaS / 12. Checklist

**Problema:** A hora do alarme de tarefa existe só no Room. O checklist manda “Desconectar” apagar o Room, e reinstalar ou trocar de celular também perde tudo. O backup `⏰ HH:mm` nas notas está marcado como “Opcional”, então o agente pode não fazer. A seção 10 fala de migrações versionadas, mas não manda exportar o schema do Room (que a AutoMigration e o `MigrationTestHelper` exigem) nem diz quando a versão 1 é congelada. Não há log nem diagnóstico: com “nenhum dado sai do celular”, quando um alarme não tocar não haverá nada para investigar.

**Por quê:** A perda silenciosa das horas de alarme é o dado mais valioso do app que o Google não guarda. Exportar o schema e ter uma interface de log custa minutos agora, e refazer isso quando o app virar SaaS custa muito mais.

**Correção sugerida:**

Na seção 7.3, trocar a observação “Opcional: …” por:
“- **Obrigatório:** ao salvar uma tarefa com hora no SincronIA, acrescentar a linha `⏰ HH:mm` no fim de `notes`. Ao ler do Google, se não houver alarme local, recriar a partir da marca. Se a hora mudar no app, reescrever a marca. Esconder a marca no campo de notas da UI.”
Na seção 10, acrescentar as linhas:
“| Room com `exportSchema = true` e o plugin `androidx.room` (`schemaDirectory("$projectDir/schemas")`); a pasta `app/schemas/` vai para o git; teste com `MigrationTestHelper` a partir da versão 2 | Migrar dados sem perda |
| Interface `AppLogger` (Logcat + arquivo local rotativo de 1 MB, sem tokens nem títulos de eventos) e o botão “Exportar diagnóstico” em Configurações | Investigar alarme que não tocou; trocar por Crashlytics no SaaS sem mexer no resto |
| Interface `Analytics` com implementação vazia (`NoOpAnalytics`) | Métricas no SaaS sem refatorar telas |”

**Evidência:** https://developer.android.com/training/data-storage/room/migrating-db-versions (“Store these files in your version control system so that you can recreate lower versions of the database for testing and support automated migration generation.”). Spec §7.3 linha 172, §12 linha 266.

---

## 38. 🟠 Média · `claude-md-arquivo-e-progresso` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · Cabeçalho / 8. M0 / 11. Regras para o Claude Code

**Problema:** A spec fica em `guias/integracao-google-agenda-tasks.md`, fora do repositório, e o nome não bate mais com o conteúdo (é a spec do MVP, não um guia de integração). O Claude Code começa cada sessão com o contexto vazio e só carrega sozinho o `CLAUDE.md`. Em 8 marcos e várias sessões, as regras da seção 11 se perdem, a menos que o Gustavo cole a spec de novo toda vez. A spec também não diz em que pasta criar o repositório, se deve rodar `git init`, onde registrar o marco atual, nem se pode fazer push ou CI.

**Por quê:** Sem `CLAUDE.md`, a partir da 2ª sessão o agente pode ignorar regras críticas (não usar GoogleSignInClient, não comitar keystore, strings em pt-BR) ou refazer marcos. Um nome de arquivo enganoso dificulta achar o brief certo.

**Correção sugerida:**

Renomear o arquivo para `SincronIA-especificacao-MVP.md`.
No M0, acrescentar às entregas: “copiar esta especificação para `docs/ESPECIFICACAO.md`; criar um `CLAUDE.md` curto (até 60 linhas) com um resumo do produto, os comandos (`./gradlew assembleDebug test lint`), as regras da seção 11 e a linha “Especificação completa: docs/ESPECIFICACAO.md; leia a seção do marco atual antes de começar”; criar `docs/PROGRESSO.md` com o marco atual e as pendências manuais, atualizado a cada commit.”
No cabeçalho “Para o Claude Code”, acrescentar: “Trabalhe na pasta do projeto criada pelo Gustavo no Android Studio (`git init` se ainda não houver repositório, branch `main`). Não crie repositório remoto nem faça push sem o Gustavo pedir.”
Opcional no M7: “se houver repositório no GitHub, `.github/workflows/android.yml` rodando `./gradlew assembleDebug test lint` a cada push”.

**Evidência:** https://code.claude.com/docs/en/memory (“Each Claude Code session begins with a fresh context window”; CLAUDE.md “Loaded into: Every session”; “target under 200 lines per CLAUDE.md file”; “Project-root CLAUDE.md survives compaction”).

---

## 39. 🟠 Média · `tema-dynamic-color-e-escuro-roxo` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 9. Identidade visual / 4. Fora do MVP

**Problema:** O `Theme.kt` do template Compose/Material 3 vem com `dynamicColor = true`: no Android 12+, as cores saem do papel de parede e a paleta azul é ignorada. A seção 4 diz “usar o dark padrão do Material 3”, e o esquema escuro padrão (baseline) do M3 é roxo (primary `#D0BCFF`), não azul. A seção 9 não dá tokens escuros.

**Por quê:** Seguindo a spec ao pé da letra, o agente entrega um app que no celular do Gustavo não é azul no modo claro (cor dinâmica) e é roxo no modo escuro, o que contradiz a identidade visual que é requisito.

**Correção sugerida:**

Na seção 9, depois da tabela, acrescentar:
“Desligar as cores dinâmicas (`dynamicColor = false`); a paleta acima vale em todos os aparelhos. Tema escuro: gerar o esquema escuro a partir da cor-semente `#1565C0` (Material Theme Builder) e colocá-lo em `Color.kt`. Não usar o esquema escuro padrão do Material 3, que é roxo.”
Na seção 4, “Fora do MVP”, trocar “temas escuros customizados (usar o dark padrão do Material 3)” por “ajuste fino do tema escuro além do esquema gerado da semente azul”.

**Evidência:** unverified (from knowledge): o template Empty Activity do Android Studio gera `dynamicColor: Boolean = true` em ui/theme/Theme.kt; o esquema baseline escuro do M3 tem primary #D0BCFF (https://m3.material.io/styles/color/static/baseline).

---

## 40. 🟠 Média · `lembretes-usedefault` (Sync e dados)

**Arquivo/seção:** spec · 7.4 Regras de alarme (linha 'Evento com hora')

**Problema:** A regra é 'usa o primeiro lembrete popup; se não houver, 10 min das Configurações'. Ela ignora `reminders.useDefault` e trata 'sem lembrete' como 'usar o padrão'.

**Por quê:** A maioria dos eventos do Google vem com `useDefault=true` e sem `overrides`, então todos cairiam nos 10 min do app, e não no padrão real da agenda (ex.: 30 min). Por outro lado, `useDefault=false` sem `overrides` significa que o usuário removeu os lembretes, e mesmo assim tocaria um alarme em tela cheia. 'Primeiro popup' é ambíguo (podem existir até 5). Aniversários, local de trabalho, foco e convites recusados também ganhariam alarme. E o app Google Agenda no mesmo celular continua mandando a notificação dele.

**Correção sugerida:**

Substituir a linha por:
| Evento com hora | Se `reminders.useDefault = true`: usar os `defaultReminders` que vêm na resposta de `events.list` (guardar em `SyncMeta` `<accountId>:calendar:primary:defaultReminders`). Se `useDefault = false` e existem `overrides` com `method = popup`: usar o **mais cedo** (maior `minutes`). Se `useDefault = false` e **não** há `overrides`: **sem alarme** automático. Os 10 min das Configurações valem só para eventos criados no SincronIA. Sem alarme automático para `eventType` diferente de `default` nem para convite recusado (`attendees[self=true].responseStatus = declined`). Registrar em `docs/DECISOES.md` que a notificação do app Google Agenda continua ativa. |
Na entidade: `reminderUseDefault` (bool) + `reminderMinutes` (lista só de popups).

**Evidência:** events.list response defaultReminders: 'apply to all events on this calendar that do not explicitly override them' https://developers.google.com/calendar/api/v3/reference/events/list . reminders.overrides: 'if not set, indicates that no reminders are set for this event'; max 5 overrides https://developers.google.com/calendar/api/v3/reference/events . eventTypes values (birthday, focusTime, outOfOffice, workingLocation) listed on events.list.

---

## 41. 🟠 Média · `alarmentity-derivado` (Sync e dados)

**Arquivo/seção:** spec · 7.1 AlarmEntity + 7.4 (Tarefa / Reboot / Sempre após sync)

**Problema:** `AlarmEntity` guarda só um `triggerAt` absoluto. Não tem modo (automático, personalizado ou desligado), nem índice único por item, nem estado de soneca ou de dispensado.

**Por quê:** A hora do alarme da tarefa existe só neste `triggerAt`. Se a data da tarefa mudar no Google, ou o fuso mudar (`TIMEZONE_CHANGED`), não há como recalcular 'mesma hora, novo dia' e a hora se perde. O recálculo depois da sync sobrescreve a escolha do usuário: um alarme manual num evento de dia inteiro, ou um alarme desligado, volta ao padrão. A soneca não sobrevive ao reboot, e um alarme já dispensado toca de novo após o reagendamento. Sem índice único, cada sync pode inserir outra linha, deixando `PendingIntent`s antigos vivos que tocam para eventos que mudaram de hora.

**Correção sugerida:**

Substituir a linha `AlarmEntity` por:
| `AlarmEntity` | `id` (PK autoincremento, usado como `requestCode`), `accountId`, `itemType`, `itemLocalId` (**índice único** `itemType+itemLocalId`), `mode` (`AUTO` = calculado do lembrete/padrão; `OFFSET` = X min antes, escolhido pelo usuário; `TIME` = hora local fixa, para tarefas e eventos de dia inteiro; `OFF` = desligado pelo usuário), `offsetMinutes`, `localTime` (`HH:mm`), `triggerAt` (**derivado**), `snoozedUntil`, `dismissedTriggerAt`, `soundUri`, `vibrate` |
E em 7.4: 'Recalcular `triggerAt` sempre a partir de `mode` + dados atuais do item: tarefa/dia inteiro = `date.atTime(localTime).atZone(ZoneId.systemDefault())`; evento = `startAt − offset`. A sync nunca muda `mode`, só `triggerAt`. Se `triggerAt` mudou, zerar `dismissedTriggerAt`. `snoozedUntil` vale depois de reboot. Item excluído ou concluído: apagar a linha e cancelar o `PendingIntent`.'

**Evidência:** AlarmManager: alarms 'will be cleared if it is turned off and rebooted'; cancel matches 'as defined by Intent.filterEquals' https://developer.android.com/reference/android/app/AlarmManager

---

## 42. 🟠 Média · `tasks-mover-lista-hierarquia` (Sync e dados)

**Arquivo/seção:** spec · 7.3 Evento x Tarefa na API + 7.1 OutboxEntity (operation)

**Problema:** O editor de tarefa tem o campo 'Lista' (Figma, tela 6), mas a spec só prevê `PATCH`. `parent` e `position` são somente leitura, e o enum do outbox não tem MOVE. Também não diz como desconcluir, nem como ordenar ou tratar subtarefas.

**Por quê:** Trocar a lista de uma tarefa com PATCH não funciona: a tarefa fica na lista antiga, ou o Claude Code improvisa um DELETE + POST que perde o id e o alarme. Subtarefas vindas do Google precisam de `parent`/`position` para aparecer na ordem certa. Criar uma subtarefa exige `insert?parent=`, o que depende do `googleId` do pai (problema quando o pai foi criado offline).

**Correção sugerida:**

Acrescentar à tabela 7.3:
| Mudar de lista | — | `POST .../lists/{origem}/tasks/{id}/move?destinationTasklist={destino}` (o `PATCH` não muda lista, pai nem posição; `parent` e `position` são somente leitura) |
| Desconcluir | — | `PATCH` com `status: "needsAction"` e `completed: null` |
E nas observações: 'No MVP o app não cria subtarefas. Subtarefas vindas do Google aparecem recuadas sob o pai, ordenadas por `position` (comparação de string). Adicionar `MOVE` ao enum `operation` do outbox; o `googleId` da tarefa não muda ao mover.'

**Evidência:** Tasks resource: parent and position are 'Output only ... Use the move method' https://developers.google.com/tasks/reference/rest/v1/tasks ; tasks.move has 'destinationTasklist: ... the task is moved from tasklist to the destinationTasklist list' https://developers.google.com/tasks/reference/rest/v1/tasks/move . That `completed: null` is required to un-complete: unverified (from knowledge).

---

## 43. 🟠 Média · `marcador-notes-alarme` (Sync e dados)

**Arquivo/seção:** spec · 7.3 Observações (marca '⏰ HH:mm' em notes) + 'alarme automático em tarefas'

**Problema:** A marca é 'opcional' e vaga: não define formato exato, se a marca aparece nas notas ao editar, se é substituída ou acrescentada, nem o que acontece quando o usuário desliga o alarme. A regra 'se existir a marca e não houver alarme local, recriar' ressuscita alarmes desligados.

**Por quê:** Do jeito que está, vai sair `⏰ 09:00 ⏰ 10:00` depois de algumas edições. A marca aparece no editor e no Google Agenda. Um alarme desligado no app volta na próxima sync, porque a linha em `AlarmEntity` some e a marca continua. A API também descarta a hora da tarefa, então uma tarefa que o usuário marcou para 14h no Google Agenda chega sem hora e, com o 'alarme automático' ligado, toca às 09:00.

**Correção sugerida:**

Substituir o terceiro item das observações de 7.3 por:
'- Marca de hora (fazer no M5): última linha de `notes` no formato exato `⏰ HH:mm` (regex `\n?⏰ ([01]\d|2[0-3]):[0-5]\d\s*$`). Ao ler: tirar a marca de `notes` antes de gravar no Room e de mostrar no editor, e guardar a hora em `AlarmEntity.localTime`. Ao gravar: `notesSemMarca + "\n⏰ HH:mm"` (substitui, nunca acrescenta outra). Alarme desligado no app: remover a marca no Google. A marca só cria alarme se **não existir** linha em `AlarmEntity` para a tarefa (inclusive `mode = OFF`).
- A API não informa a hora que o usuário escolheu no app Google Agenda/Tasks. Por isso 'alarme automático em tarefas' vem **desligado** por padrão, e o cartão mostra "Sem hora definida no SincronIA".'

**Evidência:** Tasks resource due: 'the time portion of the timestamp is discarded when setting the due date' https://developers.google.com/tasks/reference/rest/v1/tasks

---

## 44. 🟠 Média · `entidades-campos-indices` (Sync e dados)

**Arquivo/seção:** spec · 7.1 Entidades Room

**Problema:** Faltam campos que a sync e os alarmes precisam, e não há nenhum índice. As chaves de `SyncMeta` e o `OutboxEntity` não têm `accountId`, o que contradiz 'Todo registro tem accountId'.

**Por quê:** Sem índice único em `(accountId, googleId)`, o upsert do download e a resposta de um CREATE geram linhas duplicadas, e cada linha ganha seu próprio alarme. Sem `status`, `eventType` e `selfResponseStatus` não dá para filtrar os alarmes. `htmlLink` ('abrir no Google') e `colorId` custam quase nada agora e exigiriam uma migração depois. As consultas da tela Hoje por data ficam lentas sem índice.

**Correção sugerida:**

Na tabela 7.1:
| `EventEntity` | + `remoteUpdated`, `status`, `eventType`, `selfResponseStatus`, `startDate`/`endDate` (dia inteiro), `originalStartTime`, `reminderUseDefault`, `colorId`, `htmlLink` |
| `TaskEntity` | + `etag`, `remoteUpdated`, `completedAt`, `hidden`, `webViewLink` |
| `TaskListEntity` | PK `(accountId, googleId)`, + `etag` |
| `SyncMetaEntity` | `key` sempre prefixada com `<accountId>:` (ex.: `<accountId>:calendar:primary:syncToken`) |
E abaixo da tabela: 'Índices: únicos em `(accountId, googleId)` de Event, Task e TaskList; `(accountId, startAt)`, `(accountId, startDate)`, `(accountId, taskListId, dueDate)`; único `(itemType, itemLocalId)` em Outbox e Alarm; `triggerAt` em Alarm. Listas (`reminderMinutes`) via `TypeConverter` em JSON. Toda query de UI filtra `deleted = 0`.'

**Evidência:** Field names from the Events resource (colorId, htmlLink, originalStartTime, eventType, status) https://developers.google.com/calendar/api/v3/reference/events and the Task resource (etag, updated, completed, hidden, webViewLink) https://developers.google.com/tasks/reference/rest/v1/tasks

---

## 45. 🟡 Baixa · `figma-alarme-headsup` (Android)

**Arquivo/seção:** figma · TELAS 7. Alarme em tela cheia / 8. Ajustes

**Problema:** O Figma só desenha o alarme em tela cheia. Na prática, com o celular desbloqueado ou sem a permissão de tela cheia, o Android mostra o alarme como notificação heads-up, e esse estado não tem design. O item 'Sincronizar a cada (30 min)' também não mostra as opções válidas (mínimo 15 min).

**Por quê:** Sem esse frame, a UI mais comum do alarme no dia a dia (celular em uso) fica sem referência visual, e a notificação sai genérica e sem as ações.

**Correção sugerida:**

Acrescentar depois do item 7:
"7b. Alarme como notificação (celular desbloqueado ou sem permissão de tela cheia): notificação expandida no topo da tela, com ícone de despertador azul #1565C0, título do compromisso, hora (14:30), tipo (Evento/Tarefa) e dois botões de ação 'Soneca 10 min' e 'Dispensar'. Mostrar também a versão recolhida."
No item 8, trocar 'Sincronizar a cada (30 min)' por 'Sincronizar a cada (15, 30 ou 60 min; padrão 30)'.

**Evidência:** https://developer.android.com/develop/ui/views/notifications/time-sensitive : "the system UI displays a heads-up notification instead while the user actively uses the device". https://source.android.com/docs/core/permissions/fsi-limits : sem a permissão de FSI, "persistent heads-up notification" por 60 s.

---

## 46. 🟡 Baixa · `recurring-occurrence-and-allday-mapping` (APIs Google)

**Arquivo/seção:** spec · 4. Fora do MVP (texto sobre recorrência) + 7.3

**Problema:** 'editar série ... (no MVP, editar só a ocorrência; recorrências só são exibidas)' contradicts itself: can occurrences be edited or not? The spec also never says how to address an occurrence, or that all-day events use start.date/end.date with an exclusive end. 7.3 mentions only start.dateTime.

**Por quê:** Claude Code may block editing recurring items, or send `recurrence` or the series ID and change the whole series. It may also render one-day all-day events across two days in Hoje and Agenda.

**Correção sugerida:**

Seção 4, trocar o trecho por: "criar evento recorrente ou editar a **série** inteira. No MVP, um evento com `recurringEventId` pode ser editado/excluído **só nesta ocorrência**: `PATCH`/`DELETE` em `events/{id da instância}` (o `id` que vem com `singleEvents=true`), sem nunca enviar o campo `recurrence`; o editor mostra o aviso \"Alterações valem só para esta ocorrência\"".
Acrescentar às Observações de 7.3: "- Evento dia inteiro: `start.date`/`end.date` (`yyyy-MM-dd`), com `end.date` **exclusivo** (evento só em 29/09 tem `end.date = 2026-09-30`). Guardar `allDay=true` e exibir até `end.date − 1 dia`."

**Evidência:** https://developers.google.com/workspace/calendar/api/v3/reference/events: end is "The (exclusive) end time of the event". start.date is "The date, in the format 'yyyy-mm-dd', if this is an all-day event". recurringEventId is "For an instance of a recurring event, this is the id of the recurring event". https://developers.google.com/workspace/calendar/api/v3/reference/events/list: singleEvents=true returns "instances of recurring events, but not the underlying recurring events themselves".

---

## 47. 🟡 Baixa · `nomes-e-textos-inconsistentes` (Escopo MVP / Claude Code)

**Arquivo/seção:** spec · 2. Decisões fixas / 4. F10 / 8. M0 e M6 / Figma telas 3 e 8

**Problema:** A aba se chama “Ajustes” no Figma e “Configurações” na spec (F10, M0, M6, 7.5), então o agente vai usar um nome e o design outro. A marca aparece como “SincronIA”, mas a classe é `SincroniaApp`, e não há regra de onde usar cada forma. A saudação “Bom dia, Gustavo” no Figma pode ser copiada fixa no código. O pacote `com.sincronia.app` é tratado como “definitivo” sem verificar se está livre no Play.

**Por quê:** Pequenas divergências entre o Figma e o código geram retrabalho. Um nome fixo no código quebra o requisito de SaaS. Descobrir tarde que o ID de pacote está ocupado obriga a trocar o cliente OAuth.

**Correção sugerida:**

Na seção 2, acrescentar a linha: “| Nome | Para o usuário: **SincronIA** (`app_name`). No código: `Sincronia` (classes) e `sincronia` (pacote). Aba e tela de ajustes: **“Ajustes”** na navegação (cabe na barra de 4 abas), `settings` no código. |”, e trocar “Configurações” por “Ajustes” nos textos de UI citados no F10, M0 e M6.
No F4, acrescentar: “Saudação “Bom dia/Boa tarde/Boa noite, <primeiro nome da conta Google>”, conforme a hora.”
Na seção 3, acrescentar: “- [ ] Confirmar que `com.sincronia.app` não está em uso no Play (hoje não há app público com esse ID) antes de criar o cliente OAuth Android.”

**Evidência:** Figma linhas 17 e 28 (“Ajustes”) x Spec §4 linha 59, §8 linhas 196/202 (“Configurações”). https://play.google.com/store/apps/details?id=com.sincronia.app devolveu 404 em 29/09/2026 (não há app público com esse ID; pode existir reserva não pública).

---

## 48. 🟡 Baixa · `ordem-marcos-dependencias` (Sync e dados)

**Arquivo/seção:** spec · 8 Marcos + 12 Checklist

**Problema:** Alguns marcos dependem de coisas feitas depois. O M5 usa 'o padrão das Configurações' (10 min, 09:00, alarme automático em tarefas), que só nasce no M6. O M1 exige que 'Desconectar' apague o Room, criado só no M2. O M3 promete tarefas 'em ordem de horário', mas a hora da tarefa só existe a partir do M4/M5. O M4 exige 'offline sobe depois' sem um gatilho de rede.

**Por quê:** O Claude Code segue os marcos em ordem e para no critério 'Pronto quando'. Sem ajuste, ele coloca valores fixos no M5 e depois tem que refatorar, ou trava tentando cumprir um critério que depende de um marco futuro. 'Desconectar' também precisa cancelar os alarmes, e isso não está no checklist.

**Correção sugerida:**

Ajustes na seção 8:
- M2: 'SyncWorker já com as 3 etapas (outbox → download → alarmes); outbox e alarmes ficam como no-op até M4/M5.'
- M3, 'Pronto quando': '... em ordem de horário; nesta fase todas as tarefas aparecem em "Dia inteiro" (a hora de alarme chega no M5).'
- M4, entrega: '+ ao gravar no outbox, enfileirar o trabalho único de sync com restrição `NetworkType.CONNECTED`.'
- M5, entrega: '+ `SettingsRepository` (DataStore) com os padrões: lembrete 10 min, hora de tarefa 09:00, alarme automático em tarefas desligado, soneca 10 min.' Critério extra: 'tarefas com alarme aparecem na linha do tempo do Hoje na hora do alarme.'
- M6, entrega: 'Telas que editam os valores do `SettingsRepository` criado no M5.'
- Seção 12: '"Desconectar" revoga o acesso, **cancela todos os alarmes** e apaga Room + DataStore (no M1 só o DataStore).'

**Evidência:** Internal consistency of the spec (sections 7.3, 7.4, 8, 12); no external fact involved.
