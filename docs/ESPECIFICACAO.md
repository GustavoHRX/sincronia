# SincronIA · Especificação do MVP (Android)

> **Para o Claude Code:** este arquivo é a especificação completa do MVP. Leia tudo antes de começar e siga os marcos da seção 8 em ordem, respeitando as regras da seção 11. Trabalhe na pasta do projeto que o Gustavo criou no Android Studio (seção 3.1); se ainda não houver repositório git, rode `git init` (branch `main`). Não crie repositório remoto nem faça push sem o Gustavo pedir. Se algo aqui estiver ambíguo, escolha a opção mais simples, registre a decisão em `docs/DECISOES.md` e siga em frente.
>
> Versão 2 · 30/09/2026 · revisada contra a documentação oficial do Google e do Android · Dono do produto: Gustavo

---

## 1. O produto em uma frase

**SincronIA** é um app **Android** de agenda com **alarme de verdade** (tela cheia, toca mesmo no silencioso) que sincroniza nos dois sentidos com **Google Agenda** (eventos) e **Google Tasks** (tarefas).

## 2. Decisões fixas (não rediscutir)

| Tema | Decisão |
|---|---|
| Plataforma | **Somente Android.** Nada de iOS |
| Web | **Não existe** site nem painel web |
| Backend | **Não existe no MVP.** O app fala direto com as APIs do Google |
| Uso | Pessoal agora; código pronto para virar **SaaS** depois (seção 10) |
| Idioma | Interface em **português do Brasil** (`strings.xml`); código e nomes técnicos em inglês |
| Nomes | Para o usuário: **SincronIA** (`app_name`). No código: `Sincronia` (classes) e `sincronia` (pacote `com.sincronia.app`). A aba de configurações se chama **Ajustes** na interface e `settings` no código |
| Criação | Igual ao Google Agenda: o usuário escolhe **Evento** ou **Tarefa** |
| Evento | Salvo no Google Agenda (Calendar API v3), só na agenda `primary` |
| Tarefa | Salva no **Google Tasks** (Tasks API v1). A API guarda **só a data**; a **hora do alarme fica no banco local** e é copiada para as notas da tarefa (seção 7.3) |
| Recorrência | Eventos recorrentes vindos do Google aparecem e têm alarme, mas são **somente leitura** no MVP (só o alarme local é editável) |
| Conflito | Alteração local ainda não enviada **vence** (seção 7.2) |
| Fonte da verdade no app | **Room** (offline-first); a UI nunca lê direto da rede |

---

## 3. Pré-requisitos (feitos pelo Gustavo, fora do código)

O Claude Code **não consegue** fazer esta parte. Enquanto ela não estiver pronta, ele trabalha no **modo demonstração** (seção 6.3) e segue com os marcos que não dependem do Google.

### 3.1 Ambiente de desenvolvimento (uma vez)
- [ ] **Android Studio** (versão estável) instalado. Ele traz o JDK 17+ e o Android SDK. Coloque o `adb` no PATH (pasta `platform-tools` do SDK).
- [ ] **Celular Android** com *Depuração USB* ligada (Opções do desenvolvedor) **ou** emulador com imagem **"Google Play"** (o login Google não funciona em imagens sem Play).
- [ ] No Android Studio: **New Project → Empty Activity**, nome `SincronIA`, pacote `com.sincronia.app`, Minimum SDK **API 26**, Build configuration language **Kotlin DSL**. Rode o app uma vez. Isso gera `gradlew`, `local.properties` e `gradle/libs.versions.toml`, e o Claude Code parte desse esqueleto.

### 3.2 Google Cloud (menu **Google Auth Platform**)
- [ ] Projeto `SincronIA` criado em <https://console.cloud.google.com/>
- [ ] **APIs e serviços → Biblioteca**: ativar **Google Calendar API** e **Google Tasks API**
- [ ] **Branding**: nome SincronIA e seu e-mail de suporte
- [ ] **Público-alvo**: tipo **Externo**; seu Gmail em **Usuários de teste**
- [ ] **Acesso a dados**: adicionar só os escopos `https://www.googleapis.com/auth/calendar.events` e `https://www.googleapis.com/auth/tasks`
- [ ] **Clientes → Criar cliente → Android**: pacote `com.sincronia.app` + **SHA-1 de debug** (rode `./gradlew signingReport` na pasta do projeto)
- [ ] **Clientes → Criar cliente → Aplicativo da Web**: nome "SincronIA login", sem URIs. **Não é site nem servidor**: o login do Android (Credential Manager) exige o ID desse cliente. Copie **só o Client ID** (termina em `.apps.googleusercontent.com`) e cole no `local.properties` do projeto:
  ```properties
  GOOGLE_WEB_CLIENT_ID=123456789-abc.apps.googleusercontent.com
  ```
  Não baixe nem use o *client secret*.
- [ ] (Quando gerar o APK de release) outro cliente **Android** com o **SHA-1 da keystore de release**
- [ ] (Para uso diário) **Público-alvo → Publicar app** ("Em produção"), sem pedir verificação. No modo *Teste* a autorização expira a cada 7 dias; em produção sem verificação aparece o aviso "O Google não verificou este app" (toque em *Avançado → Acessar SincronIA*) e o limite é de 100 usuários.

**Se o login falhar** com `GetCredentialException` (Credential Manager) ou `ApiException` com status 10 / `DEVELOPER_ERROR` (AuthorizationClient), a causa quase sempre é configuração: pacote ou SHA-1 do cliente Android, ou `GOOGLE_WEB_CLIENT_ID` errado. Confira também se o build não usa `applicationIdSuffix` (seção 11, regra 5).

---

## 4. Escopo do MVP

### ✅ Dentro
| ID | Funcionalidade |
|---|---|
| F1 | Entrar com conta Google e autorizar Agenda + Tasks |
| F2 | Sincronizar os eventos da agenda **principal** (`primary`), dos últimos 7 dias até os próximos 60, com a janela andando junto com o calendário |
| F3 | Sincronizar tarefas de **todas as listas** do Google Tasks |
| F4 | Tela **Hoje** (dia local do aparelho), de cima para baixo: saudação ("Bom dia/Boa tarde/Boa noite, <primeiro nome da conta>"); **Atrasadas** (tarefas não concluídas com data antes de hoje); **Dia inteiro** (eventos de dia inteiro que cobrem hoje, inclusive os de vários dias, e tarefas de hoje sem hora de alarme); **linha do tempo** (eventos com hora que cruzam hoje, inclusive ocorrências de recorrentes e eventos que começaram ontem, e tarefas de hoje com hora de alarme, em ordem de horário, com a linha "agora"); por último, tarefas concluídas hoje, riscadas |
| F5 | Tela **Agenda**: faixa com os 7 dias da semana (deslizar troca a semana; dia atual destacado; ponto nos dias com itens) e, abaixo, a **lista** do dia escolhido no mesmo formato da Hoje. Tela **Tarefas**: chips por lista; subtarefas vindas do Google recuadas sob a tarefa-mãe (ordem por `position`); seção recolhível "Concluídas" (últimos 7 dias) |
| F6 | **Criar / editar / excluir** Evento ou Tarefa, com envio ao Google (regras de somente leitura na seção 7.3) |
| F7 | Concluir e desfazer a conclusão de tarefa (com "Desfazer" na snackbar) |
| F8 | **Alarme**: tela cheia com o celular bloqueado; notificação com **Soneca** e **Dispensar** com o celular em uso; som mesmo no silencioso |
| F9 | Reagendar alarmes depois de reiniciar o celular, atualizar o app, mudar hora ou fuso e a cada sync |
| F10 | **Ajustes**: conta (foto, e-mail, **Desconectar**), alarme padrão para novos eventos (10 min antes), lembrete diário das tarefas sem hora (desligado; 09:00), soneca (5/10/15 min), som do alarme, sincronizar a cada (15/30/60 min; padrão 30), "Sincronizar agora" com o horário da última sync, permissões, atalho para os ajustes de bateria, "Exportar diagnóstico" |
| F11 | Funcionar **offline**: alterações ficam na fila e sobem sozinhas quando a internet voltar |
| F12 | **Modo demonstração** (só no build debug): dados de exemplo, sem login (seção 6.3) |

### ❌ Fora do MVP
iOS · web · backend · várias contas Google · agendas além da `primary` · criar ou editar eventos **recorrentes** · grade de horários semanal · criar subtarefas ou reordenar tarefas · convidados · widgets · IA · pagamentos · sync incremental por `syncToken` e notificações push da Agenda · alarmes antes do primeiro desbloqueio depois de reiniciar (Direct Boot, seção 7.4) · ajuste fino do tema escuro além do esquema gerado a partir da cor azul.

---

## 5. Stack

Parta das versões do template do Android Studio (seção 3.1) e use só versões **estáveis** (nada de alpha, beta ou RC; a documentação às vezes mostra exemplos com versões alfa). Kotlin, KSP, AGP, o plugin do Compose, Hilt e Room precisam ser compatíveis entre si: no **AGP 9** o Kotlin é embutido (não aplicar `org.jetbrains.kotlin.android`) e o **Hilt 2.59+ exige AGP 9 e Gradle 9.1+**. Se o build quebrar por versão, volte à última combinação que compilava. Fixe tudo em `gradle/libs.versions.toml` e registre as versões em `docs/DECISOES.md`. Não adicione bibliotecas fora desta lista sem registrar o motivo.

| Camada | Tecnologia |
|---|---|
| Linguagem / build | Kotlin 2.x, Gradle Kotlin DSL, **version catalog**, JDK 17 |
| SDK | `minSdk 26`; `compileSdk`/`targetSdk` = o mais recente estável que o template trouxer (**no mínimo 36**, exigido pelo Google Play desde 31/08/2026) |
| UI | Jetpack Compose + **Material 3** + Navigation Compose |
| DI | Hilt + `androidx.hilt:hilt-work` (workers com `@HiltWorker` e `@AssistedInject`) |
| Banco | **Room 2.x** com o plugin `androidx.room` e KSP, `exportSchema = true`. **Não** usar `androidx.room3` (ainda não é estável) |
| Preferências | DataStore |
| Rede | Retrofit + OkHttp + kotlinx.serialization (REST direto; **sem** as bibliotecas `google-api-services-*`) |
| Login | **Credential Manager**: `androidx.credentials:credentials` + `androidx.credentials:credentials-play-services-auth` + `com.google.android.libraries.identity.googleid:googleid` |
| Autorização de escopos | **`AuthorizationClient`** de `com.google.android.gms:play-services-auth` **22.0.0 ou mais novo** (tem `revokeAccess`) |
| Segundo plano | WorkManager |
| Alarme | `AlarmManager.setAlarmClock()` + foreground service tocando o som (seção 7.4) |
| Datas | `java.time` |
| Testes | JUnit, kotlinx-coroutines-test, Turbine, MockWebServer, Room in-memory, Robolectric (opcional, para receivers) |

⚠️ **Não usar** `GoogleSignInClient` / `GoogleSignInOptions` (descontinuados).

---

## 6. Arquitetura

### 6.1 Pastas
```
app/src/main/java/com/sincronia/app/
 ├─ SincroniaApp.kt   (Application: Hilt, canais de notificação, Configuration.Provider com HiltWorkerFactory)
 ├─ core/
 │   ├─ auth/        GoogleAuthManager (Credential Manager + AuthorizationClient), AuthState
 │   ├─ network/     OkHttp: interceptor do token, Authenticator do 401, backoff
 │   ├─ database/    AppDatabase, DAOs, entidades, TypeConverters, migrações
 │   ├─ log/         AppLogger (Logcat + arquivo local), Analytics (NoOp)
 │   └─ time/        utilitários de data e fuso (sempre ZoneId.systemDefault(); nunca fixar fuso no código)
 ├─ data/
 │   ├─ calendar/    CalendarApi (Retrofit), GoogleCalendarSource, EventRepository
 │   ├─ tasks/       TasksApi (Retrofit), GoogleTaskSource, TaskRepository
 │   ├─ demo/        FakeCalendarSource, FakeTaskSource, DemoData
 │   ├─ settings/    SettingsRepository (DataStore)
 │   └─ sync/        SyncWorker, OutboxProcessor, SyncScheduler
 ├─ alarm/           AlarmScheduler, AlarmReceiver, AlarmService, AlarmActivity, BootReceiver
 ├─ ui/
 │   ├─ theme/       cores, tipografia, formas (seção 9)
 │   ├─ onboarding/  boas-vindas, login, permissões
 │   ├─ today/       Hoje
 │   ├─ agenda/      semana + lista do dia
 │   ├─ tasks/       listas e tarefas
 │   ├─ editor/      criar/editar (Evento | Tarefa)
 │   └─ settings/    Ajustes
 └─ navigation/
```

### 6.2 Padrões
- **MVVM + Repository + UDF**: ViewModels expõem `StateFlow<UiState>`; telas só falam com ViewModels; ViewModels só com Repositories; Repositories leem e escrevem no Room e enfileiram as mudanças no **outbox**.
- `CalendarSource` e `TaskSource` são **interfaces** que isolam o Google. Há duas implementações (Google e demonstração); no SaaS pode entrar uma terceira (backend próprio) sem mexer na UI.
- No manifesto, remover o inicializador padrão do WorkManager (obrigatório ao usar `HiltWorkerFactory`):
  ```xml
  <provider android:name="androidx.startup.InitializationProvider"
      android:authorities="${applicationId}.androidx-startup"
      android:exported="false" tools:node="merge">
      <meta-data android:name="androidx.work.WorkManagerInitializer"
          android:value="androidx.startup" tools:node="remove" />
  </provider>
  ```

### 6.3 Modo demonstração (só no build debug)
- `FakeCalendarSource` e `FakeTaskSource` com dados relativos a "hoje": eventos de hoje e da semana, 1 ocorrência de evento recorrente, 1 evento de dia inteiro, 1 evento que começou ontem e termina hoje, tarefas em 2 listas (com 1 subtarefa), 1 tarefa atrasada e 1 concluída hoje.
- No onboarding do debug, o botão **"Entrar em modo demonstração"** grava uma flag no DataStore; o app passa a usar as fontes falsas, com `accountId = "demo"`, e pula o login.
- Se `GOOGLE_WEB_CLIENT_ID` estiver vazio no `local.properties`, o botão "Entrar com Google" fica desativado com o aviso "Configure o GOOGLE_WEB_CLIENT_ID (especificação, seção 3.2)". O modo demonstração não existe no release.

### 6.4 Login e autorização (sem backend)
1. **Login**: botão "Entrar com Google" → Credential Manager com `GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)` (o valor vem do `local.properties`). Do `GoogleIdTokenCredential`, guardar no DataStore: e-mail (`id`), nome, foto e `accountId` = claim `sub` do `idToken` (decodificar o payload do JWT localmente; o `sub` é estável, o e-mail pode mudar).
2. **Autorização**: `AuthorizationClient.authorize()` com `setAccount(Account(email, "com.google"))` e os 2 escopos pela **URL completa**: `Scope("https://www.googleapis.com/auth/calendar.events")` e `Scope("https://www.googleapis.com/auth/tasks")`. Se vier `hasResolution()`, abrir o `PendingIntent` de consentimento a partir da Activity.
3. **Conferir os escopos concedidos** (`grantedScopes` do resultado): o Google pode mostrar uma caixa por escopo e o usuário pode desmarcar um. Se faltar Agenda ou Tasks, explicar e pedir de novo; não sincronizar a parte sem permissão. `403 insufficientPermissions` recebe o mesmo tratamento.
4. **Token**: o access token (vale cerca de 1 h) fica **só em memória**. Chamar `authorize()` no **início de cada sync** (é silencioso e devolve o token em cache ou renovado). Um interceptor do OkHttp coloca `Authorization: Bearer <token>`.
5. **401**: um `okhttp3.Authenticator` chama `clearToken(ClearTokenRequest.builder().setToken(tokenAntigo).build())`, depois `authorize()`, e repete a requisição **uma única vez**. Se vier 401 de novo, tratar como autorização perdida (passo 6).
6. **Consentimento em segundo plano**: se `authorize()` pedir interação (`hasResolution()`) fora de uma Activity (ex.: no `SyncWorker`), **não** apagar nada: gravar `authState = NEEDS_REAUTH`, pausar o sync, manter os alarmes locais, mostrar a notificação e a faixa "Reconecte sua conta Google" e terminar o worker com `Result.success()`. O onboarding só aparece se o usuário recusar.
7. **Não** guardar refresh token no aparelho (sem backend, não é necessário).
8. **Desconectar**, nesta ordem: `revokeAccess(RevokeAccessRequest.builder().setAccount(account).setScopes(escopos).build())` (revoga todos os escopos) → `CredentialManager.clearCredentialState(ClearCredentialStateRequest())` → cancelar os trabalhos do WorkManager → `AlarmScheduler.cancelAll()` (e parar o `AlarmService` se estiver tocando) → apagar Room e DataStore → onboarding. Se o revoke falhar por falta de internet, desconectar localmente mesmo assim e registrar no log.

---

## 7. Dados, sync e alarmes

### 7.1 Entidades Room
| Entidade | Campos |
|---|---|
| `EventEntity` | `localId` (PK, UUID), `accountId`, `googleId`, `calendarId`, `title`, `description`, `location`, `allDay`, `startAt`/`endAt` (Instant; nulos se dia inteiro), `startDate`/`endDate` (LocalDate; só dia inteiro; `endDate` **exclusivo**, igual ao Google), `timeZone`, `status`, `eventType`, `recurringEventId`, `originalStartTime`, `organizerSelf`, `guestsCanModify`, `selfResponseStatus`, `reminderUseDefault`, `reminderMinutes` (lista, só lembretes `popup`), `colorId`, `htmlLink`, `etag`, `remoteUpdated`, `updatedAt`, `deleted`, `syncState` |
| `TaskEntity` | `localId` (PK, UUID), `accountId`, `googleId` (nulo até subir), `taskListId`, `remoteTaskListId` (lista em que o Google a conhece), `title`, `notes` (sem a marca de hora), `dueDate` (LocalDate), `status` (`needsAction`/`completed`), `completedAt`, `parentId`, `position`, `hidden`, `webViewLink`, `etag`, `remoteUpdated`, `updatedAt`, `deleted`, `syncState` |
| `TaskListEntity` | PK (`accountId`, `googleId`), `title`, `isDefault`, `etag`, `remoteUpdated` |
| `AlarmEntity` | `id` (PK autoincremento; é o `requestCode`), `accountId`, `itemType` (`EVENT`/`TASK`/`DAILY_SUMMARY`), `itemLocalId`, `mode` (`AUTO` = segue os lembretes do Google; `OFFSET` = X min antes, escolhido no app; `TIME` = hora local fixa, para tarefa e evento de dia inteiro; `OFF` = desligado pelo usuário), `offsetMinutes`, `localTime`, `triggerAt` (**derivado**, sempre recalculado), `scheduled` (está no AlarmManager?), `snoozedUntil`, `dismissedTriggerAt`, `soundUri`, `vibrate` |
| `OutboxEntity` | `id`, `accountId`, `itemType` (`EVENT`/`TASK`), `itemLocalId`, `operation` (`CREATE`/`UPDATE`/`DELETE`), `changedFields` (lista), `state` (`PENDING`/`FAILED`), `attempts`, `nextAttemptAt`, `lastError`, `createdAt` |
| `SyncMetaEntity` | `key` (sempre prefixada por `<accountId>:`, ex.: `<accountId>:calendar:primary:timeZone`), `value` |

- `syncState`: `SYNCED`, `PENDING`, `FAILED`.
- **Índices**: únicos em (`accountId`, `googleId`) de Event e Task; (`accountId`, `startAt`), (`accountId`, `startDate`), (`accountId`, `taskListId`, `dueDate`); únicos em (`itemType`, `itemLocalId`) de Outbox e Alarm; `triggerAt` em Alarm.
- Listas (ex.: `reminderMinutes`, `changedFields`) via `TypeConverter` em JSON. Toda consulta de UI filtra `deleted = 0`.
- `exportSchema = true`, com `room { schemaDirectory("$projectDir/schemas") }`; a pasta `app/schemas/` vai para o git.

### 7.2 Sincronização

| Etapa | Regra |
|---|---|
| Uma por vez | Toda sync roda como trabalho único, `WorkManager.enqueueUniqueWork("sync", ExistingWorkPolicy.KEEP, …)` com `NetworkType.CONNECTED`, e um `Mutex` no repositório. Ao terminar, se ainda houver itens `PENDING` no outbox com `nextAttemptAt <= agora`, enviar de novo antes de sair |
| Ordem | 1) autorizar (6.4; pular no modo demonstração) → 2) enviar o **outbox** (7.2.1) → 3) baixar Agenda e Tasks → 4) recalcular e reagendar os alarmes (7.4) |
| Agenda | A cada sync, carregar a janela inteira: `GET calendars/primary/events?singleEvents=true&timeMin=<início do dia local −7 dias>&timeMax=<início do dia local +61 dias>&maxResults=250`, paginando com `pageToken` até o fim. Guardar `timeZone` e `defaultReminders` da resposta em `SyncMeta`. **Só depois de receber todas as páginas**, numa transação: gravar ou atualizar o que veio e apagar (cancelando o alarme) todo evento `SYNCED` da conta que não veio. Itens `PENDING`/`FAILED` nunca são apagados nem sobrescritos. Se uma página falhar, não apagar nada. Uma agenda pessoal cabe em 1 a 3 páginas. **Não** usar `syncToken` no MVP: ele não aceita `timeMin`/`timeMax`, então a janela não andaria e os eventos futuros deixariam de chegar |
| Tasks, listas | `GET users/@me/lists?maxResults=100`, paginando. Lista nova → gravar. Lista que sumiu (com a listagem completa) → apagar a lista, suas tarefas e seus alarmes locais. Descobrir a lista padrão uma vez com `GET users/@me/lists/@default` (se falhar, usar a primeira lista retornada) e marcar `isDefault` |
| Tasks, tarefas | Para cada lista: `GET lists/{id}/tasks?maxResults=100&showCompleted=true&showHidden=true&showDeleted=true[&updatedMin=X]`, **paginando com `nextPageToken` até o fim** (o padrão da API é só 20 por página). `X` = maior `updated` recebido na sync anterior **menos 2 minutos** (relógio do Google, nunca o do celular); omitir na primeira vez; gravar o novo `X` só depois de todas as páginas, na chave `<accountId>:tasks:<listId>:updatedMin`. Gravar por (`accountId`, `googleId`); se o `googleId` já existir em outra lista, atualizar `taskListId` e `remoteTaskListId`. `deleted = true` → apagar local e cancelar o alarme |
| Conflito | **Alteração local pendente vence**: o download nunca sobrescreve item com entrada no outbox, e o `PATCH` envia **só os campos alterados** (`changedFields`), então edições em campos diferentes no celular e no Google sobrevivem. Registrar essa política em `docs/DECISOES.md` |
| Quando | Ao abrir o app; ao puxar para atualizar; ao gravar algo no outbox; e periódico com `enqueueUniquePeriodicWork("sync-periodic", ExistingPeriodicWorkPolicy.UPDATE, …)` a cada 15, 30 ou 60 min (padrão 30; 15 é o mínimo do WorkManager), com rede |
| Erros transitórios | Falha de rede, `5xx`, `429`, `403` com `rateLimitExceeded`/`userRateLimitExceeded` → repetir a requisição até 3 vezes (1 s, 2 s, 4 s) e depois deixar para a próxima sync |
| Erros definitivos | `400`, `403` com outro motivo (`quotaExceeded`, `forbiddenForNonOrganizer`, `insufficientPermissions`), `409` fora do caso da 7.2.1 → não repetir (ver 7.2.1). Ler o motivo em `error.errors[0].reason` do corpo |
| 410 | Nunca tratar 410 de forma global no interceptor: `410 deleted` num `DELETE` significa "já estava apagado" (sucesso) |

#### 7.2.1 Outbox (fila de envio)
- **Uma entrada por item** (índice único). Ao enfileirar: CREATE + UPDATE continua CREATE; UPDATE + UPDATE vira um UPDATE (juntando `changedFields`); CREATE + DELETE apaga a entrada e o item local, sem ir à rede; UPDATE + DELETE vira DELETE. Concluir ou desfazer a conclusão é um UPDATE de `status`.
- O envio **lê o estado atual** da entidade no Room (o outbox não guarda cópia), inclusive o `googleId` atual.
- **Evento novo**: o app gera o id do Google, que é o `localId` (UUID) sem hífens e em minúsculas (hexadecimal é base32hex válido, com 32 caracteres), e grava em `googleId` na criação. Se uma nova tentativa do CREATE responder `409 duplicate`, buscar o evento por esse id e considerá-lo criado.
- **Tarefa nova** (a API não aceita id do cliente): se uma tentativa anterior terminou sem resposta (timeout depois do envio), antes de reenviar listar a lista com `updatedMin = createdAt − 2 min` e procurar tarefa com mesmo título e data; se achar, adotar o `googleId` dela.
- **Tarefa mudou de lista**: se `taskListId ≠ remoteTaskListId`, antes do PATCH chamar `POST lists/{remoteTaskListId}/tasks/{id}/move?destinationTasklist={taskListId}` (o PATCH não muda lista, pai nem posição). Tarefa recorrente não pode mudar de lista: mostrar aviso no editor.
- **Sucesso**: gravar `googleId`, `etag`, `remoteUpdated` e remover a linha do outbox **na mesma transação**.
- `DELETE` com `404` ou `410` = sucesso. `PATCH` com `404`/`410`: fazer `GET` do item; se o `GET` também der `404`/`410` (ou vier `status = cancelled` / `deleted = true`), o item foi apagado no Google: apagar local e cancelar o alarme. Senão, tratar como erro transitório (logo depois de criar, o Google pode responder `404` por alguns instantes).
- **Falhas**: erro transitório → `attempts + 1` e `nextAttemptAt` (1, 5, 15, 60 min); depois de 5 tentativas, ou em erro definitivo → `state = FAILED` e `syncState = FAILED`, sem bloquear os outros itens. A UI mostra "Não foi possível sincronizar" com **Tentar de novo** e **Descartar alteração** (descartar = baixar de novo a versão do Google).

### 7.3 Evento x Tarefa na API
| | Evento | Tarefa |
|---|---|---|
| Criar | `POST https://www.googleapis.com/calendar/v3/calendars/primary/events` (com o `id` gerado no app) | `POST https://tasks.googleapis.com/tasks/v1/lists/{lista}/tasks` |
| Editar | `PATCH …/events/{id}` só com os campos alterados | `PATCH …/lists/{lista}/tasks/{id}` só com os campos alterados |
| Excluir | `DELETE …/events/{id}` | `DELETE …/lists/{lista}/tasks/{id}` |
| Concluir / desfazer | — | `PATCH` com `status: "completed"` / com `status: "needsAction"` e `completed: null` |
| Mudar de lista | — | `POST …/lists/{origem}/tasks/{id}/move?destinationTasklist={destino}` |
| Data e hora | `start`/`end` com `dateTime` + `timeZone`; dia inteiro com `date` e `end.date` **exclusivo** (evento só em 29/09 → `end.date = 2026-09-30`) | `due` = `YYYY-MM-DDT00:00:00.000Z` (**só data**) |
| Lembrete | Alarme "X min antes": `reminders: {"useDefault": false, "overrides": [{"method": "popup", "minutes": X}]}`; "Padrão da agenda": `{"useDefault": true}` | — (alarme só local) |
| Lista padrão | `primary` | a lista marcada `isDefault` (ID real; nunca gravar `@default` no banco) |

**Datas (atenção: é o erro mais provável do app)**
- `due` da Tasks API: ler **só os 10 primeiros caracteres** como data (`LocalDate.parse(due.take(10))`) e **nunca** converter fuso. Ao gravar: `"${dueDate}T00:00:00.000Z"`. Se for lido como `Instant` e convertido para o fuso de São Paulo, toda tarefa cai no dia anterior.
- Evento de dia inteiro aparece no dia D se `startDate <= D < endDate`. Evento com hora aparece no dia D se começa antes do fim de D e termina depois do início de D (cobre eventos que passam da meia-noite).

**O que pode ser editado**
- Só eventos com `eventType = default` são editáveis. `birthday`, `fromGmail`, `focusTime`, `outOfOffice` e `workingLocation` aparecem como **somente leitura** (a API restringe o que muda neles).
- Ocorrência de evento recorrente (`recurringEventId` preenchido): somente leitura, com o aviso "Evento recorrente: edite no Google Agenda". Nunca enviar o campo `recurrence`.
- Convite em que o usuário não é o organizador (`organizerSelf = false`) e `guestsCanModify = false`: somente leitura (o Google responde `403 forbiddenForNonOrganizer`).
- Em todos os casos de somente leitura, o **alarme local** continua editável.
- Subtarefas: o app mostra as que vêm do Google, mas não cria nem reordena.

**Hora das tarefas (a API não guarda)**
- A API **não** lê nem grava a hora de uma tarefa, nem a que o usuário escolheu no app Google Agenda. A hora do alarme de tarefa fica em `AlarmEntity` (`mode = TIME`, `localTime`).
- **Marca de hora nas notas** (feita no M5): ao salvar tarefa com hora no SincronIA, a última linha de `notes` no Google passa a ser exatamente `⏰ HH:mm` (regex `\n?⏰ ([01]\d|2[0-3]):[0-5]\d\s*$`), substituindo a marca anterior e nunca acrescentando outra. Ao ler do Google: tirar a marca de `notes` antes de gravar no Room e de mostrar no editor; se ainda não existir `AlarmEntity` para a tarefa, criar uma com essa hora. Alarme desligado no app: remover a marca no Google e manter a `AlarmEntity` com `mode = OFF` (assim a marca não ressuscita o alarme). Isso preserva a hora se o app for reinstalado.
- Tarefa vinda do Google sem marca chega **sem hora**: fica na faixa "Dia inteiro" e o cartão mostra "Sem hora definida".

### 7.4 Alarmes

**Quando cada item tem alarme**
| Item | Regra |
|---|---|
| Evento com hora vindo do Google (`mode = AUTO`) | Alarme em `início − N min`. Se `reminderUseDefault = true`: N = maior `minutes` entre os `defaultReminders` do tipo `popup` guardados em `SyncMeta`. Se `false` e houver `overrides` do tipo `popup`: N = o maior (o lembrete mais cedo). Se `false` e **sem** overrides, o usuário removeu os lembretes: **sem alarme** |
| Evento criado no SincronIA | Alarme padrão "10 min antes" (Ajustes) com `mode = OFFSET`; o mesmo lembrete vai para o Google na criação (7.3) |
| Mudar o alarme depois | Sempre **só local** (muda `mode`/`offsetMinutes`/`localTime`); a sync nunca muda `mode` |
| Evento de dia inteiro | Sem alarme automático; o usuário pode definir uma hora (`mode = TIME`) |
| Sem alarme automático | `eventType` diferente de `default`, convites recusados (`selfResponseStatus = declined`) e eventos cancelados |
| Tarefa | Alarme na `dueDate` + `localTime` (`mode = TIME`); sem data não há alarme (o editor exige data para pôr hora) |
| Lembrete diário das tarefas sem hora | Desligado por padrão. Se ligado: **uma** notificação comum (sem tela cheia) na hora escolhida (padrão 09:00) listando as tarefas de hoje sem hora (`itemType = DAILY_SUMMARY`), agendada com `setAndAllowWhileIdle` (não precisa ser exata nem mostrar o ícone de despertador) |

**Como agendar**
| Regra | Detalhe |
|---|---|
| Cálculo | `triggerAt` é sempre recalculado a partir de `mode` + dados atuais do item: `TIME` → `data.atTime(localTime).atZone(ZoneId.systemDefault())`; `AUTO`/`OFFSET` → `startAt − minutos`. Se `triggerAt` mudou, zerar `dismissedTriggerAt` |
| Quais | Só `triggerAt` em `(agora, agora + 7 dias]`, não `OFF`, não dispensados, de itens não excluídos nem concluídos. No máximo os **50** mais próximos no AlarmManager; os outros entram nas próximas execuções |
| Horário que já passou | **Nunca** chamar `setAlarmClock` com `triggerAt <= agora` (o Android dispara na hora). Se o horário do alarme passou mas o evento ainda não começou, mostrar uma notificação comum uma única vez (marcar `dismissedTriggerAt`) |
| Idempotência | `AlarmScheduler.rescheduleAll()` compara o que está agendado (`scheduled`) com o desejado: cancela os que saíram e agenda os novos ou alterados. `PendingIntent` com intent **explícito** para `AlarmReceiver`, `requestCode = AlarmEntity.id` e `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT`; o mesmo `requestCode` cancela. `AlarmClockInfo.showIntent` abre o item no app |
| Quando rodar | Depois de cada sync, ao abrir o app, depois de cada disparo/soneca/dispensa, no `BootReceiver` e num `PeriodicWork` diário **sem** restrição de rede (completa a janela de 7 dias mesmo offline) |

**Como tocar**
| Parte | Regra |
|---|---|
| Disparo | `AlarmReceiver` (`exported=false`) → `ContextCompat.startForegroundService(AlarmService)` (o disparo de alarme exato permite iniciar foreground service em segundo plano) |
| Serviço | `AlarmService` com `android:foregroundServiceType="mediaPlayback"`: em até 5 s chama `ServiceCompat.startForeground(…, FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)` com a notificação do alarme. **O som e a vibração ficam no serviço, nunca na Activity**: `MediaPlayer` em loop com `AudioAttributes.USAGE_ALARM` + `CONTENT_TYPE_SONIFICATION` e `setWakeMode(PARTIAL_WAKE_LOCK)`; vibração com `VibrationAttributes.USAGE_ALARM` (API 33+). Sem interação, parar depois de **10 min** e trocar por notificação "Alarme perdido: <título>". Não usar `shortService` (limite de cerca de 3 min; no Android 17 o áudio em segundo plano exige Activity visível ou foreground service que não seja `shortService`, e a falha é silenciosa) |
| Notificação | Canal `alarm`: `IMPORTANCE_HIGH`, `setSound(null, null)` (o som vem do serviço), vibração do canal desligada, `lockscreenVisibility = VISIBILITY_PUBLIC`. Notificação com `setCategory(CATEGORY_ALARM)`, `PRIORITY_MAX`, `setOngoing(true)`, `setFullScreenIntent(AlarmActivity, true)` e ações **Soneca** e **Dispensar**. Com o celular **desbloqueado e em uso**, o Android mostra essa notificação (heads-up) em vez da tela cheia; por isso as ações ficam nela. Outros canais (`sync`, `geral`): `IMPORTANCE_LOW`/`DEFAULT` |
| Tela cheia | `AlarmActivity` no manifesto com `showWhenLocked="true"`, `turnScreenOn="true"`, `excludeFromRecents="true"`, `launchMode="singleInstance"`, `taskAffinity=""`, `exported="false"`. Na API 26 (esses atributos só existem a partir da 27): `window.addFlags(FLAG_SHOW_WHEN_LOCKED or FLAG_TURN_SCREEN_ON)` no `onCreate`. Sempre `FLAG_KEEP_SCREEN_ON` e layout edge-to-edge com insets. Só mostra hora, título, tipo e os botões; envia `ACTION_SNOOZE`/`ACTION_DISMISS` ao serviço |
| Soneca | Padrão 10 min (Ajustes: 5/10/15). Grava `snoozedUntil`, que vale também depois de reiniciar |
| Dois ao mesmo tempo | O novo alarme assume a tela e o som; o anterior vira notificação comum |
| Reiniciar, atualizar, hora e fuso | `BootReceiver` para `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`, `TIMEZONE_CHANGED` e `AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`: usar `goAsync()` + corrotina (o Room não roda na main thread) e chamar `rescheduleAll()`. **Nunca** iniciar o `AlarmService` a partir do `BOOT_COMPLETED` (o Android 15 proíbe foreground service `mediaPlayback` nesse caso): alarme cujo horário passou com o celular desligado vira notificação "Alarme perdido" |
| Limitação conhecida | O `BOOT_COMPLETED` só chega depois do **primeiro desbloqueio** após reiniciar (até lá o Room fica em armazenamento criptografado). Um alarme que cairia antes desse desbloqueio não toca. Registrar em `docs/DECISOES.md`. A solução (Direct Boot: `LOCKED_BOOT_COMPLETED`, componentes `directBootAware` e cópia dos próximos alarmes em `createDeviceProtectedStorageContext()`) fica para depois do MVP |

### 7.5 Permissões e onboarding
**AndroidManifest**: `INTERNET`, `POST_NOTIFICATIONS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` com `android:maxSdkVersion="32"` (o Android 12/12L não conhece `USE_EXACT_ALARM`), `USE_FULL_SCREEN_INTENT`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`. **Não** declarar `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

| Item | Como checar | Se negado |
|---|---|---|
| Notificações (Android 13+) | permissão `POST_NOTIFICATIONS` | **Sem ela o alarme não aparece.** Faixa fixa na Hoje: "Alarmes desativados: permita notificações", com botão para os ajustes do app |
| Alarmes e lembretes (Android 12/12L) | `AlarmManager.canScheduleExactAlarms()` | Abrir `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`; enquanto negado, faixa na Hoje. `setAlarmClock` sempre dentro de `try/catch (SecurityException)` |
| Tela cheia (Android 14+) | `NotificationManager.canUseFullScreenIntent()` | Abrir `Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` com `package:<pacote>`. Sem ela, o alarme aparece como notificação heads-up com Soneca, Dispensar e som |
| Otimização de bateria | opcional | Fora do onboarding (`setAlarmClock` toca mesmo no modo Doze). Em Ajustes, só um atalho que abre `Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, útil para o sync em Xiaomi, Samsung e Motorola |

Onboarding: Boas-vindas → Entrar com Google (ou modo demonstração no debug) → Permissões (Notificações; Alarmes em tela cheia, só no Android 14+; Alarmes e lembretes, só no Android 12/12L; cada item mostra "Permitido" ou o botão "Permitir") → Hoje. Rechecar tudo no `onResume` da Hoje.

---

## 8. Marcos (fazer em ordem, um commit por marco)

| Marco | Entrega | Pronto quando… |
|---|---|---|
| **M0 · Base** | Projeto a partir do esqueleto da 3.1, version catalog, Hilt (+ hilt-work e `Configuration.Provider`), tema da seção 9, navegação com Hoje / Agenda / Tarefas / Ajustes vazias, `AppLogger`, `.gitignore`, `README.md`, `CLAUDE.md`, `docs/ESPECIFICACAO.md` (cópia deste arquivo), `docs/DECISOES.md`, `docs/PROGRESSO.md`, `docs/TESTES-MANUAIS.md` | `./gradlew assembleDebug test lint` passa; o app abre e navega entre as 4 abas |
| **M1 · Dados locais e demonstração** | Entidades, DAOs, TypeConverters e índices da 7.1; `CalendarSource`/`TaskSource` + fontes falsas; modo demonstração; `SyncWorker` já com as 4 etapas da 7.2 (outbox e alarmes ainda sem efeito); telas Agenda e Tarefas | No modo demonstração, Agenda e Tarefas mostram os dados de exemplo; os testes dos DAOs passam |
| **M2 · Hoje** | Tela Hoje conforme o F4, puxar para atualizar | No modo demonstração, a Hoje mostra Atrasadas, Dia inteiro, linha do tempo (com o evento que começou ontem) e concluídas. Nesta fase todas as tarefas ficam em "Dia inteiro" (a hora chega no M5) |
| **M3 · Login e leitura do Google** | Login e autorização (6.4), APIs Retrofit, `GoogleCalendarSource`/`GoogleTaskSource`, sync de leitura da 7.2, WorkManager periódico, "Desconectar" (o cancelamento de alarmes entra no M5) | Testes com MockWebServer passam (paginação, janela, reconciliação). Manual (depois da seção 3): o Gustavo entra, autoriza e vê seus eventos e tarefas; sem internet, tudo continua aparecendo |
| **M4 · Escrita** | Editor Evento/Tarefa, regras de somente leitura (7.3), outbox (7.2.1), excluir, concluir/desfazer, mudar tarefa de lista | Testes da junção do outbox passam. Manual: criar evento no SincronIA → aparece no Google Agenda; criar tarefa → aparece no Google Tasks e, se tiver data, no Google Agenda; offline entra na fila e sobe quando a internet volta |
| **M5 · Alarmes** | `SettingsRepository` com os padrões (alarme de novo evento 10 min, lembrete diário desligado às 09:00, soneca 10 min); `AlarmScheduler`, `AlarmReceiver`, `AlarmService`, `AlarmActivity`, `BootReceiver`; marca `⏰` nas notas; permissões e onboarding (7.5); "Desconectar" passa a cancelar os alarmes; no debug, "Testar alarme em 10 s" em Ajustes | Testes do cálculo de alarme passam. Manual: o alarme toca com a tela bloqueada, no silencioso e no "Não perturbe" (com alarmes permitidos); com o celular em uso aparece a notificação com Soneca/Dispensar e o som toca; depois de reiniciar e desbloquear, os alarmes continuam. Tarefas com hora aparecem na linha do tempo |
| **M6 · Ajustes** | Tela Ajustes completa (F10), editando o `SettingsRepository` do M5; "Exportar diagnóstico" | As mudanças ficam salvas e valem na próxima sync e no próximo alarme |
| **M7 · Acabamento** | Estados vazios, de carregamento e de erro; faixas de permissão e "Reconecte sua conta Google"; ícone; splash; APK de release assinado | `./gradlew assembleRelease` gera APK instalável; checklist da seção 12 completo |

**Verificação de cada marco**
- *Automática* (Claude Code): `./gradlew assembleDebug test lint` passa e, no modo demonstração, a tela mostra o esperado.
- *Manual* (Gustavo, no celular): passos que o Claude Code escreve em `docs/TESTES-MANUAIS.md`, com comandos prontos (ex.: `adb shell dumpsys alarm | grep -i sincronia`, `adb reboot`). Se o teste manual depender de algo que ainda não está pronto (ex.: seção 3), registrar como pendente em `docs/PROGRESSO.md` e seguir para o próximo marco.

**Testes obrigatórios** (além dos de ViewModel e DAO)
- Com fuso `America/Sao_Paulo`, a tarefa com `due=2026-09-29T00:00:00.000Z` aparece em 29/09.
- Evento de dia inteiro em 29/09 (`end.date` 30/09) não aparece em 30/09.
- Janela da Agenda: simulando 61 dias depois da primeira sync, um evento do dia +62 aparece; um evento apagado no Google some do Room.
- Tasks: 3 páginas de 100 itens são todas lidas.
- Outbox: CREATE + UPDATE vira um CREATE; CREATE + DELETE não chama a rede.
- Primeira sync às 15h, com eventos passados e tarefas de hoje, **não** dispara nenhum alarme.
- Agendar 3 alarmes gera 3 `PendingIntent` distintos; `rescheduleAll()` duas vezes seguidas não duplica.
- Marca `⏰ 14:30`: ler, reescrever e remover sem duplicar.

---

## 9. Identidade visual

Paleta **azul, azul claro e branco**, Material 3, cantos arredondados, visual limpo. Os mesmos tokens são usados no Figma (`guias/prompt-figma.md`). Contrastes conferidos pelo WCAG (texto ≥ 4,5:1; ícones e bordas ≥ 3:1).

| Token | Hex | Uso | Contraste |
|---|---|---|---|
| `primary` | `#1565C0` | Botões principais, FAB, aba ativa, barra do cartão de evento | 5,75:1 com branco |
| `onPrimary` | `#FFFFFF` | Texto e ícone sobre o azul | — |
| `primaryContainer` | `#D6E6FF` | Cartões de evento | — |
| `onPrimaryContainer` | `#0A2A5E` | Texto sobre cartão de evento | 11,0:1 |
| `secondary` | `#0277BD` | Azul claro "forte": checkbox de tarefa, ícones de tarefa, linha "agora" | 4,8:1 com branco |
| `onSecondary` | `#FFFFFF` | Texto sobre `secondary` | 4,8:1 |
| `secondaryContainer` | `#E3F2FD` | Cartões de tarefa | — |
| `onSecondaryContainer` | `#0D1B2A` | Texto sobre cartão de tarefa | 15,2:1 |
| `tertiary` (acento) | `#42A5F5` | Azul claro da marca, **só em preenchimentos** com texto `#0A2A5E` (chip e segmento selecionados, ilustrações, logo). Nunca como texto, ícone ou borda sobre branco (2,65:1) | 5,3:1 com `#0A2A5E` |
| `background` / `surface` | `#FFFFFF` | Fundo das telas | — |
| `surfaceVariant` | `#F4F8FD` | Seções, campos de texto | — |
| `onSurface` | `#0D1B2A` | Texto principal | 17,4:1 |
| `onSurfaceVariant` | `#505F73` | Texto secundário (horários, locais) | ≥ 5,1:1 em todas as superfícies |
| `outline` | `#6B7785` | Borda de campos e de checkbox desmarcado | 4,6:1 |
| `outlineVariant` | `#C9D6E6` | Só divisórias decorativas | — |
| `error` | `#D32F2F` | Erros e excluir | 5,0:1 |
| Alarme (fundo) | gradiente `#0D47A1` → `#1565C0` | Tela cheia do alarme, com texto branco | ≥ 5,75:1 |

- **Desligar as cores dinâmicas** (`dynamicColor = false` no `Theme.kt` do template); senão, no Android 12+ as cores vêm do papel de parede e a paleta azul some.
- **Tema escuro**: gerar o esquema escuro a partir da cor-semente `#1565C0` no Material Theme Builder (<https://material-foundation.github.io/material-theme-builder/>) e colocá-lo em `Color.kt`. Não usar o esquema escuro padrão do Material 3, que é roxo.
- Tipografia **Inter** (ou Roboto padrão). Cantos: 16 dp em cartões, 28 dp no FAB e em diálogos. Espaçamento base 8 dp. Área de toque mínima 48 dp; `contentDescription` nos ícones.
- Evento = cartão `primaryContainer` com barra lateral `primary`. Tarefa = cartão `secondaryContainer` com checkbox circular `secondary`. Concluída = texto riscado em `onSurfaceVariant`.

---

## 10. Preparação para SaaS

| Já no MVP | Por quê |
|---|---|
| Pacote definitivo `com.sincronia.app` | Não pode mudar depois de publicado |
| `accountId` em todas as entidades e chaves | Várias contas e usuários sem migração dolorosa |
| `CalendarSource` / `TaskSource` como interfaces | Trocar para backend próprio depois |
| Só 2 escopos (`calendar.events`, `tasks`) | Facilita a verificação do Google |
| Textos em `strings.xml` | Tradução futura |
| Room com `exportSchema` e `app/schemas/` no git; testes com `MigrationTestHelper` a partir da versão 2; nunca `fallbackToDestructiveMigration` no release | Migrar dados sem perda |
| `AppLogger` (Logcat + arquivo local rotativo de 1 MB, **sem** tokens, títulos ou e-mails) + "Exportar diagnóstico" | Investigar alarme que não tocou; trocar por Crashlytics no SaaS |
| `Analytics` como interface com implementação vazia (`NoOpAnalytics`) | Métricas no SaaS sem mexer nas telas |

**Fica para depois**: backend (troca do *server auth code* via `requestOfflineAccess(GOOGLE_WEB_CLIENT_ID)` e refresh tokens no servidor; sync incremental com `syncToken` e push `events.watch`; push FCM) · alarmes em Direct Boot · verificação do app no Google (política de privacidade, domínio, vídeo) · Play App Signing + o SHA-1 dele no Google Cloud · declarações no Play Console: `USE_EXACT_ALARM` e `USE_FULL_SCREEN_INTENT` (função principal de alarme/agenda) e o tipo de foreground service (`mediaPlayback`; `systemExempted` é alternativa documentada para apps com permissão de alarme exato) · LGPD · Google Play Billing.

---

## 11. Regras para o Claude Code

1. **Memória entre sessões**: no M0, crie um `CLAUDE.md` curto (até 60 linhas) com o resumo do produto, os comandos (`./gradlew assembleDebug test lint`), estas regras e a linha "Especificação completa: `docs/ESPECIFICACAO.md`; leia a seção do marco atual antes de começar". Mantenha `docs/PROGRESSO.md` com o marco atual e as pendências manuais, atualizado a cada commit.
2. Siga os marcos **em ordem**; ao fim de cada um, rode `./gradlew assembleDebug test lint` e faça **um commit** com mensagem em português (ex.: `M3: login e leitura do Google`).
3. **Nunca** comitar: `*.jks`, `*.keystore`, `key.properties`, `local.properties`, `.env`, `client_secret*.json`, `google-services.json`.
4. **Não** usar API key, client secret nem escopos além de `https://www.googleapis.com/auth/calendar.events` e `https://www.googleapis.com/auth/tasks` (sempre a URL completa em `Scope(...)`). **Não** chamar `calendars.get` nem `calendarList` (exigem outros escopos): fuso e lembretes padrão vêm na resposta de `events.list`.
5. **Não** usar `applicationIdSuffix` no build debug: o cliente OAuth Android está registrado para `com.sincronia.app`.
6. **Não** usar `GoogleSignInClient`, `androidx.room3` nem versões alpha/beta/RC. **Não** adicionar backend, iOS ou web.
7. Toda string visível ao usuário vai em `res/values/strings.xml`, em pt-BR.
8. Registre decisões e desvios desta especificação em `docs/DECISOES.md` (data + decisão + motivo).
9. Quando algo depender do Gustavo (Google Cloud, SHA-1, testar no celular), diga **exatamente** o que ele precisa fazer, com o comando pronto, e **continue no que não depende dele** (o modo demonstração existe para isso).
10. Prefira a solução mais simples que cumpra o critério "Pronto quando…" do marco. Não faça push nem crie repositório remoto sem o Gustavo pedir.

---

## 12. Checklist de segurança e qualidade

- [ ] `.gitignore` cobre todos os itens da regra 3
- [ ] Access token só em memória; nenhum token, e-mail ou título de evento em log
- [ ] "Desconectar" segue a ordem do passo 8 da seção 6.4 (revoga, limpa credenciais, cancela trabalhos e alarmes, apaga Room + DataStore)
- [ ] `401` renova o token uma vez; autorização perdida em segundo plano mostra "Reconecte sua conta Google" sem apagar dados
- [ ] Escopos concedidos conferidos depois da autorização
- [ ] Nenhum dado sai do celular além das chamadas às APIs do Google e do "Exportar diagnóstico" feito pelo usuário
- [ ] Alarme testado: tela bloqueada, celular em uso, modo silencioso, "Não perturbe", depois de reiniciar e desbloquear
- [ ] Permissões negadas mostram faixa com o caminho para liberar
- [ ] Testado sem internet (criar, editar, concluir e depois sincronizar)
- [ ] Keystore de release com backup fora do repositório

---

## 13. Links oficiais

| Tema | Link |
|---|---|
| Google Cloud Console | <https://console.cloud.google.com/> |
| Calendar: `events.list` | <https://developers.google.com/workspace/calendar/api/v3/reference/events/list> |
| Calendar: recurso Event | <https://developers.google.com/workspace/calendar/api/v3/reference/events> |
| Calendar: erros | <https://developers.google.com/workspace/calendar/api/guides/errors> |
| Tasks: recurso Task | <https://developers.google.com/workspace/tasks/reference/rest/v1/tasks> |
| Tasks: `tasks.list` | <https://developers.google.com/workspace/tasks/reference/rest/v1/tasks/list> |
| Tasks: `tasks.move` | <https://developers.google.com/workspace/tasks/reference/rest/v1/tasks/move> |
| Escopos OAuth | <https://developers.google.com/identity/protocols/oauth2/scopes> |
| Consentimento por escopo | <https://developers.google.com/identity/protocols/oauth2/resources/granular-permissions> |
| Android: login (Credential Manager) | <https://developer.android.com/identity/sign-in/credential-manager-siwg-implementation> |
| Android: autorização (AuthorizationClient) | <https://developer.android.com/identity/authorization> |
| Android: alarmes exatos | <https://developer.android.com/develop/background-work/services/alarms/schedule> |
| Android: notificações urgentes | <https://developer.android.com/develop/ui/views/notifications/time-sensitive> |
| Android: tipos de foreground service | <https://developer.android.com/develop/background-work/services/fgs/service-types> |
| Android 17: áudio em segundo plano | <https://developer.android.com/about/versions/17/changes/bg-audio> |
| Android: Doze e bateria | <https://developer.android.com/training/monitoring-device-state/doze-standby> |
| Android: Direct Boot | <https://developer.android.com/training/articles/direct-boot> |
| Hilt + WorkManager | <https://developer.android.com/training/dependency-injection/hilt-jetpack> |
| Room: migrações e schema | <https://developer.android.com/training/data-storage/room/migrating-db-versions> |
| Google Play: target SDK | <https://developer.android.com/google/play/requirements/target-sdk> |
| Política de dados do usuário Google | <https://developers.google.com/terms/api-services-user-data-policy> |

---

**Histórico**: v1 (29/09/2026) primeira versão · v2 (30/09/2026) revisada contra a documentação oficial: cliente OAuth Web para o login, sync da Agenda por janela, paginação do Tasks, datas sem fuso, outbox, alarmes (som no serviço, permissão do Android 12, horizonte de 7 dias), permissões negadas, modo demonstração e marcos reordenados. Detalhes em `guias/PROGRESSO.md`.
