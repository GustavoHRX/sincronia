# Decisões e desvios da especificação

Formato: data · decisão · motivo.

## Decisões da especificação (registro exigido por ela)

- **30/09/2026 · Conflito de sync: alteração local pendente vence.** O download nunca sobrescreve item com entrada no outbox, e o `PATCH` envia só os campos alterados (`changedFields`). Motivo: edições em campos diferentes no celular e no Google sobrevivem (spec 7.2).
- **30/09/2026 · Limitação conhecida de alarmes: Direct Boot fica fora do MVP.** O `BOOT_COMPLETED` só chega depois do primeiro desbloqueio após reiniciar; um alarme que cairia antes desse desbloqueio não toca. Solução futura: `LOCKED_BOOT_COMPLETED`, componentes `directBootAware` e cópia dos próximos alarmes em `createDeviceProtectedStorageContext()` (spec 7.4).

## Ambiente e versões

- **30/09/2026 · Projeto esqueleto ainda não existe (M0 bloqueado no passo 3.1).** O repositório foi clonado só com a documentação; não há `gradlew`, Android SDK nem Android Studio nesta máquina (há JDK 25 do sistema e IntelliJ IDEA). A spec manda partir do esqueleto gerado pelo Android Studio. Enquanto isso, só a documentação do M0 é preparada.
- **12/10/2026 · Esqueleto criado** no Android Studio Rabbit 1 (2026.2.1.8), Empty Activity, `com.sincronia.app`, minSdk 26. O template trouxe `compileSdk`/`targetSdk` 37.
- **12/10/2026 · Versões do M0** (`gradle/libs.versions.toml`; as do template foram mantidas, as bibliotecas desatualizadas subiram para a última estável no Google Maven / Maven Central nesta data):
  Gradle 9.6.0 · AGP 9.4.1 (Kotlin embutido) · Kotlin/plugin Compose 2.2.10 · KSP 2.3.12 · Compose BOM 2026.09.00 · Navigation Compose 2.10.2 · Hilt 2.60.1 · androidx.hilt (hilt-work) 1.4.0 · WorkManager 2.12.0 · core-ktx 1.19.1 · lifecycle 2.11.0 · activity-compose 1.13.0. Room, DataStore, Retrofit etc. entram nos marcos em que forem usados.
- **12/10/2026 · Build com o JDK do Android Studio** (`android-studio/jbr`). Motivo: o Java do sistema é o 25; usar o mesmo JDK do Studio evita um segundo daemon do Gradle. No terminal: `JAVA_HOME=<pasta do Android Studio>/jbr ./gradlew …`.
- **12/10/2026 · `compileOptions` em Java 17** (o template trouxe 11). Motivo: a spec pede JDK 17.

## Interface

- **12/10/2026 · Tema escuro e tons não fixados pela seção 9 gerados com `material-color-utilities` 0.3.0** (`SchemeTonalSpot`, semente `#1565C0`), o mesmo algoritmo do Material Theme Builder. **Desvio:** o `tertiary` gerado é rosado (`#DCBCE1` no escuro, container `#F9D8FD` no claro); foi trocado pela paleta tonal de `#42A5F5` (tons 80/20/30/90 no escuro, 90/10 no container claro). Motivo: a paleta da marca é só azul.
- **12/10/2026 · Ícones das abas como vetores em `res/drawable`** (Material Icons, Apache 2.0) em vez da biblioteca `material-icons`. Motivo: a biblioteca não está mais no BOM do Compose e seriam 4 ícones.
- **12/10/2026 · Rotas das abas em texto** (`today`, `agenda`, `tasks`, `settings`) em vez de rotas tipadas com kotlinx.serialization. Motivo: solução mais simples para o M0; dá para migrar quando houver telas com argumentos (editor, M4).
- **12/10/2026 · `AppLogger` grava em `files/logs/app.log` com rotação em 512 KB** (um backup `app.log.1`, no máximo ~1 MB no aparelho), numa thread própria. Motivo: o "Exportar diagnóstico" (F10) precisa de um arquivo, sem ocupar espaço sem limite.

## M1 · Dados locais e demonstração

- **02/11/2026 · Room 2.8.4, DataStore Preferences 1.1.7, kotlinx.serialization JSON 1.9.0 e Robolectric 4.16.1.** Room usa o plugin `androidx.room`, KSP e schema exportado em `app/schemas/`; eventos e tarefas têm chave composta `(accountId, localId)`, e testes de DAO rodam em banco in-memory com Robolectric. Motivo: versões estáveis compatíveis com o build do M0; separar contas nas chaves e persistir a flag do modo demonstração sem depender do Google.
- **02/11/2026 · Fontes falsas só são usadas se a flag do DataStore estiver ativa em debug.** O release nunca oferece nem ativa a demonstração; login real, rede, envio do outbox e agendamento de alarmes entram em M3/M4/M5. O worker já percorre a ordem das quatro etapas e grava o download em transação no Room. Motivo: cumprir M1 sem antecipar o login nem simular envio ao Google.
- **02/11/2026 · Chaves dos itens de demonstração são estáveis e as datas são relativas ao dia local.** A importação substitui os dados sincronizados e marca itens que sumiram como excluídos, mas preserva itens com alterações pendentes no outbox. Motivo: reabrir o app sem duplicar itens e manter o comportamento offline-first.

## M2 · Hoje

- **05/11/2026 · Regras da Hoje num `TodayPlanner` puro**, testado com JUnit sem Android (seções, ordem, linha "agora", saudação). O `TodayViewModel` só junta os `Flow` do Room e um relógio que bate na virada de cada minuto, que move a linha "agora" e troca o dia depois da meia-noite. Motivo: a spec pede testes de ViewModel, mas o projeto não tem `kotlinx-coroutines-test`; testar a lógica fora do ViewModel cobre as regras sem biblioteca nova.
- **05/11/2026 · Saudação: "Bom dia" das 05:00 às 11:59, "Boa tarde" das 12:00 às 17:59, "Boa noite" das 18:00 às 04:59.** No modo demonstração não há conta, então a saudação sai sem nome; o primeiro nome entra no M3 com o login. Motivo: a spec não define os horários nem o caso sem conta.
- **05/11/2026 · Linha "agora" depois de todo evento que já começou** (inclusive o que está acontecendo) e só quando há eventos com hora. Motivo: o evento em andamento fica acima da linha, como no Google Agenda; sem eventos, a linha sozinha não informa nada.
- **05/11/2026 · Puxar para atualizar chama o `SyncCoordinator` direto no ViewModel**, o mesmo pipeline do `SyncWorker`, protegido pelo mesmo `Mutex`. A sync ao abrir o app continua no WorkManager. Motivo: o indicador precisa saber quando a sync termina; com o WorkManager seria preciso observar o `WorkInfo`. Rever no M3, quando houver rede e erros a mostrar.
- **05/11/2026 · "Concluídas hoje" = `status = completed` com `completedAt` no dia local, sem as tarefas ocultas (`hidden`)** (ocultas são as que o usuário limpou no Google). Atrasadas = não concluídas com `dueDate` antes de hoje; tarefa sem data nunca fica atrasada.
- **05/11/2026 · Cartões de evento e de tarefa compartilhados entre Hoje e Agenda** (`ui/components/ItemCards.kt`), com horário relativo ao dia ("ontem, 22:00 – 01:00") e "Dia inteiro, até d/M" nos eventos de vários dias. Motivo: a F5 pede a lista da Agenda no mesmo formato da Hoje.
- **05/11/2026 · Demonstração ganhou "Semana de provas"**, evento de dia inteiro de ontem até amanhã. Motivo: a F4 pede eventos de dia inteiro de vários dias, e a demonstração não tinha nenhum.
- **05/11/2026 · Limitação conhecida (do M1): `Instant` é gravado como texto ISO e comparado como texto no SQL.** Dois horários no mesmo segundo, um com fração e outro sem, podem ficar na ordem errada (erro de no máximo 1 s nas bordas do dia). Corrigir antes do M3, com horários do Google, gravando em milissegundos (migração do schema v1 para v2).
- **11/11/2026 · `Instant` passou a ser gravado em milissegundos (schema v2), o que resolve a limitação acima.** A migração `MIGRATION_1_2` recria as cinco tabelas com colunas `INTEGER` e converte cada texto ISO com `Instant.parse`, em Kotlin, porque o SQLite não troca o tipo de uma coluna e o `strftime` perderia a fração de microssegundos. Precisão de milissegundo, a mesma das APIs do Google. Testes em `MigrationTest` (dados preservados e ordem correta no mesmo segundo).
- **11/11/2026 · `googleid` 1.2.0, não 1.2.1.** A 1.2.1 foi compilada com Kotlin 2.4 e o projeto usa Kotlin 2.2.10; o compilador recusa os metadados. Credential Manager 1.6.0 e play-services-auth 22.0.0 (já tem `clearToken` e `revokeAccess`).
- **11/11/2026 · Escopo desmarcado no primeiro login: o app fica no onboarding** com a explicação e o botão "Pedir de novo", em vez de entrar com só metade da sync. Motivo: solução mais simples para o M3; a sync parcial pode vir depois se fizer falta.
- **11/11/2026 · O `idToken` só é decodificado localmente para ler o `sub`**, sem verificar a assinatura: veio direto do Credential Manager e não há backend que precise confiar nele.
- **12/11/2026 · Item apagado no Google é apagado de verdade do Room no fluxo Google** (o M1 da demonstração só marcava `deleted`). Motivo: a 7.2 diz "apagar", e as linhas marcadas acumulariam a cada sync da janela.
- **12/11/2026 · `parentId` das tarefas guarda o id local da tarefa-mãe**, traduzido do `parent` do Google na sync, porque as telas agrupam pelo id local.
- **12/11/2026 · Retrofit 3.0.0 + OkHttp 5.5.1**, com o plugin `kotlin.plugin.serialization` na versão do Kotlin do projeto. O `Authenticator` do OkHttp usa `runBlocking` para renovar o token: roda na thread de rede do OkHttp, nunca na principal.
- **12/11/2026 · Erro definitivo encerra o `SyncWorker` com `Result.success()`** e fica para a próxima sync; só erro transitório usa o `Result.retry()` do WorkManager.
- **12/11/2026 · Modo Teste do Google Cloud exige a conta em "Usuários de teste".** Sem isso o Google responde `403 access_denied` antes da tela de consentimento. Vale para qualquer conta nova enquanto o app estiver em modo Teste.
- **12/11/2026 · Escrita (M4) sem as repetições de 1/2/4 s da 7.2**: o outbox usa só as novas tentativas de 1/5/15/60 min da 7.2.1, para não repetir um POST que pode ter dado certo.
- **12/11/2026 · 401 no envio conta como erro transitório** (a autorização pode voltar na próxima sync), não como falha definitiva do item.
- **12/11/2026 · Modo demonstração não envia o outbox**: as edições ficam PENDING no aparelho e a sync da demonstração não as sobrescreve.
- **12/11/2026 · Editor de evento: data única com início e fim**; fim antes do início passa da meia-noite. Dia inteiro tem "Até" (inclusivo na tela, exclusivo no Google). Horários não tocados mantêm os instantes originais.
- **12/11/2026 · Limitação conhecida (7.4): `BOOT_COMPLETED` só chega depois do primeiro desbloqueio** após reiniciar; um alarme que cairia antes disso não toca. Direct Boot fica para depois do MVP.
- **12/11/2026 · Marca `⏰` vinda do Google cria o alarme da tarefa só se ainda não houver alarme local**; alarme desligado no app fica `OFF` e a marca é removida no envio, para não ressuscitar.
- **12/11/2026 · Alarme de evento AUTO cria a linha `AlarmEntity` só quando entra na janela**; o id dela é o `requestCode`.
- **12/11/2026 · Notificações só depois de conferir `POST_NOTIFICATIONS`** (Android 13+); sem a permissão, a Hoje mostra a faixa "Alarmes desativados".
- **12/11/2026 · Acréscimo ao escopo do M6: "Desligar alarme da série inteira"** para eventos recorrentes. Motivo: séries diárias geram um alarme por ocorrência, e desligar uma a uma não é prático. A escolha é local, como todo alarme (7.4).
- **12/11/2026 · Desligar a série vale só para ocorrências em AUTO**; uma ocorrência com alarme próprio (minutos antes ou hora escolhida) continua tocando, sem perguntar. Motivo: o alarme próprio é uma escolha explícita mais específica que a da série.
- **12/11/2026 · Foto da conta sem biblioteca de imagens**: baixada uma vez por URL (`URLConnection` + `BitmapFactory`, só na memória) a partir do endereço do Google que veio no login; sem foto ou sem internet aparece a inicial. Motivo: evitar Coil/Glide fora da lista da seção 5 por uma imagem só.
- **12/11/2026 · Som do alarme pelo seletor do sistema (`ACTION_RINGTONE_PICKER`, tipo alarme)**; "Padrão" grava nada e segue o padrão do celular. Se o som escolhido sumir ou não tocar (ex.: arquivo do armazenamento sem permissão de mídia), o `AlarmService` toca o padrão. Motivo: o alarme nunca pode ficar mudo.
- **12/11/2026 · Hora da última sync no DataStore de ajustes** (`last_sync_at`), gravada só quando a sync chega ao Google (ou à demonstração); apagada no "Desconectar" junto com os ajustes.
- **12/11/2026 · Alarme padrão de evento novo com opções fixas** (na hora, 5, 10, 15, 30, 60 min), sem "desligado": para não ter alarme, basta desligar no editor.
- **12/11/2026 · "Exportar diagnóstico" filtra o log de novo antes de sair do aparelho** (e-mails, `Bearer` e tokens `ya29.`), além da regra do `AppLogger`. O arquivo fica em `cache/diagnostics`, apagado a cada exportação, e sai só pelo compartilhamento escolhido pelo usuário.
- **12/11/2026 · Alarme padrão de evento novo em chips (`FilterChip` + `FlowRow`)**, não em botões segmentados: seis opções numa fileira cortavam os rótulos no celular.
- **13/11/2026 · Depois de reiniciar, a tabela `alarms` não vale como espelho do `AlarmManager`**: o `BootReceiver` zera `scheduled` antes de reagendar, em toda ação dele (registrar de novo com o mesmo `requestCode` só substitui). Motivo: num celular de teste os alarmes sumiam depois do reinício até o app ser aberto.
- **13/11/2026 · Conta ativa em segundo plano sem limite de tempo** (`ActiveAccount.current()`): `null` só quando não há conta salva. Motivo: com o limite de 2 s, uma leitura lenta do DataStore logo depois de ligar o celular parecia "desconectado" e cancelaria todos os alarmes.
- **13/11/2026 · Aviso de alarme no passado no editor** (M7): só para item novo ou quando a hora/o alarme mudou; AUTO fica de fora (segue os lembretes do Google). Um segundo "Salvar" salva assim mesmo. Motivo: no celular, um evento salvo segundos depois da hora do alarme ficou sem alarme e sem aviso.
- **13/11/2026 · Falha de sync guardada no DataStore** (`last_sync_failed`), não só na memória, para a faixa aparecer também depois de uma sync em segundo plano. Uma faixa só, para offline e para erro do Google: nos dois casos as mudanças ficam no celular.
- **13/11/2026 · Notificação de reconexão uma vez por queda** (quando o estado passa a `NEEDS_REAUTH`), não a cada sync; some ao reconectar.
- **13/11/2026 · Backup do Android desligado** (`allowBackup=false`, regras de nuvem e de transferência excluindo tudo). Motivo: seção 12, nenhum dado sai do celular; depois de restaurar, basta entrar de novo.
- **13/11/2026 · Chave de release fora do repositório**, com senha aleatória longa (`key.properties` e keystore nunca são versionados). Motivo: sem a chave não há como atualizar o app instalado, então ela fica guardada à parte.
- **13/11/2026 · Ícone sem texto**: calendário branco com sino sobre `#1565C0`; a mesma camada branca serve de ícone monocromático.
