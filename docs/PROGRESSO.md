# SincronIA · Progresso e como continuar

> Última atualização: 12/10/2026

## Marco atual: M1 · Dados locais e demonstração (próximo)

## M0 · Base (concluído em 12/10/2026)

- [x] Documentação do M0: `CLAUDE.md`, `docs/ESPECIFICACAO.md`, `docs/DECISOES.md`, `docs/TESTES-MANUAIS.md`, este arquivo; `.gitignore` e `README.md` já existiam.
- [x] (06/10) Android Studio Rabbit 1 e Android SDK (platform 37, emulador `Medium_Phone_API_37.0` com Google Play) instalados.
- [x] (12/10) Esqueleto criado pelo Gustavo na spec 3.1 (`com.sincronia.app`, minSdk 26, Kotlin DSL).
- [x] (12/10) Version catalog com versões estáveis (lista em `DECISOES.md`), Hilt + hilt-work com `Configuration.Provider` e o inicializador padrão do WorkManager removido, tema da seção 9 (claro e escuro azul, sem cores dinâmicas), navegação com Hoje / Agenda / Tarefas / Ajustes vazias, `AppLogger` (Logcat + arquivo com rotação) com 3 testes.
- [x] (12/10) `./gradlew assembleDebug test lint` passou (3 testes; lint só com 2 avisos de versão mais nova: Gradle 9.8.1 e Kotlin 2.4.21, mantidos os do template).
- [x] (12/10) Teste manual no emulador (API 37): o app abre e navega entre as 4 abas, cada uma com sua descrição. Tema escuro ainda não conferido no aparelho.
- [ ] Pendência manual: seção 3.2 (Google Cloud), só necessária a partir do M3.

### Como rodar a verificação no terminal
```bash
JAVA_HOME=~/Documents/Workspace/android-studio/jbr ./gradlew assembleDebug test lint
```
Usa o JDK do Android Studio (o Java do sistema é o 25) e reaproveita o daemon do Gradle.

## Arquivos do projeto (pasta `guias/`)

| Arquivo | O que é | Status |
|---|---|---|
| `integracao-google-agenda-tasks.md` | **Especificação do MVP v2** para o Claude Code (escopo, stack, arquitetura, sync, alarmes, marcos M0–M7, identidade visual, regras) | ✅ Pronta para entregar ao Claude Code |
| `prompt-figma.md` | **Prompt do Figma v2**, em 3 partes (Design System + 13 telas e estados) | ✅ Pronto |
| `revisao-achados-parcial.md` | Os 48 achados da revisão + o resultado da verificação | ✅ Registro (a especificação é a fonte da verdade) |
| `PROGRESSO.md` | Este arquivo | — |

## Decisões (fixas)
- Só **Android**, sem web e sem backend no MVP. Uso pessoal agora, código pronto para virar **SaaS**.
- Criação igual ao Google Agenda: **Evento** (Calendar API) ou **Tarefa** (Tasks API). A Tasks API só guarda a data: a hora do alarme fica no banco local e numa linha `⏰ HH:mm` nas notas da tarefa.
- Login com Credential Manager, que exige o **ID de um cliente OAuth "Aplicativo da Web"** (só o ID, sem segredo; não é site).
- Só 2 escopos: `calendar.events` e `tasks`.
- Agenda sincronizada pela **janela** (−7 a +60 dias) a cada sync, sem `syncToken` no MVP.
- Conflito: a alteração local ainda não enviada vence; o PATCH envia só os campos alterados.
- Eventos recorrentes: somente leitura (só o alarme local é editável).
- Alarmes: som num foreground service, horizonte de 7 dias, no máximo 50 agendados. Alarmes antes do primeiro desbloqueio depois de reiniciar (Direct Boot) ficam para depois do MVP.
- Paleta: azul `#1565C0`, azul claro `#0277BD` (ícones e checkbox) e `#42A5F5` (só preenchimentos), containers `#D6E6FF`/`#E3F2FD`, branco. Cores dinâmicas desligadas; tema escuro gerado do azul.

## O que foi feito
1. ✅ (29/09) Guia de APIs Google (Agenda + Tasks), depois focado em Android.
2. ✅ (29/09) Guia transformado em especificação MVP para o Claude Code.
3. ✅ (29/09) Prompt do Figma.
4. ✅ (29/09) Revisão em 4 frentes: 48 achados.
5. ✅ (30/09) **Verificação** dos 17 achados de alta prioridade na documentação oficial do Google e do Android: **todos procedem** (detalhes no topo de `revisao-achados-parcial.md`).
6. ✅ (30/09) **Revisão do prompt do Figma**.
7. ✅ (30/09) **Especificação v2** e **prompt do Figma v2** com as correções.

## O que mudou na especificação (v1 → v2)
- **Pré-requisitos**: ambiente de desenvolvimento (Android Studio, celular ou emulador com Google Play, projeto esqueleto) e o **cliente OAuth Web** com o `GOOGLE_WEB_CLIENT_ID` no `local.properties`. Escopo `calendarlist.readonly` removido.
- **Modo demonstração** no build debug: o Claude Code avança sem depender do Google Cloud. Marcos reordenados: M1 dados locais, M2 Hoje, M3 login e leitura do Google.
- **Sync da Agenda**: janela inteira a cada sync, com reconciliação (antes, os eventos futuros parariam de chegar em cerca de 2 meses).
- **Sync do Tasks**: paginação (a API devolve só 20 por página), `updatedMin` pelo relógio do Google, listas novas e apagadas.
- **Datas**: `due` da tarefa lido sem fuso (antes cairia no dia anterior em São Paulo); fim de dia inteiro exclusivo; tela Hoje definida item por item.
- **Outbox**: junção de operações, id do evento gerado no app (evita duplicar), tratamento de erros pela documentação.
- **Alarmes**: som no foreground service (senão não toca com o celular em uso nem no Android 17), permissão do Android 12/12L, nunca agendar horário passado, horizonte de 7 dias, `requestCode` único, lembretes padrão da agenda (`useDefault`), sem alarme em convite recusado.
- **Permissões negadas**: faixas com o caminho para liberar; otimização de bateria só como atalho (o Play proíbe o pedido direto).
- **Login**: `clearToken` no 401, "Reconecte sua conta Google" em segundo plano sem apagar dados, conferência dos escopos concedidos, "Desconectar" completo.
- **Tema**: cores dinâmicas desligadas, tema escuro azul (o padrão do Material 3 é roxo), cores com contraste corrigido.
- **Claude Code**: `CLAUDE.md` e `docs/PROGRESSO.md` criados no M0 para não perder as regras entre sessões; testes obrigatórios listados.

## O que mudou no prompt do Figma (v1 → v2)
- **Contraste** (conferido pelo WCAG): o azul claro `#42A5F5` tinha 2,65:1 sobre branco no checkbox e na linha "agora" (mínimo 3:1) → trocado por `#0277BD`; o fim do gradiente do alarme tinha 3,68:1 com texto branco → `#1565C0`; o texto secundário tinha 4,31:1 no cartão de evento → `#505F73`; a borda dos campos tinha 1,47:1 → `outline #6B7785`.
- Dividido em **3 prompts** (pedir tudo de uma vez costuma gerar telas incompletas).
- Agenda **sem grade de horas** (semana + lista do dia), tela de permissões nova, **alarme como notificação** (celular em uso), estados (vazio, sem conexão, reconectar, permissão negada, carregando, erro de sync), editor somente leitura e **versões escuras**.
- Frame nomeado como "Android Compact (412 × 917)", sem emoji na interface.

## Próximos passos
1. [ ] **Gustavo**: seção 3.1 da especificação (Android Studio e o projeto esqueleto `com.sincronia.app`).
2. [ ] **Gustavo**: seção 3.2 (Google Cloud), incluindo o **cliente Web** e o `GOOGLE_WEB_CLIENT_ID` no `local.properties`. Pode ser feito em paralelo: o Claude Code começa sem isso, no modo demonstração.
3. [ ] Copiar `integracao-google-agenda-tasks.md` para a pasta do projeto e dizer ao Claude Code do terminal: `Leia integracao-google-agenda-tasks.md e comece pelo Marco M0.`
4. [ ] Figma: colar o Prompt 1, depois o 2 e o 3 de `prompt-figma.md`.
5. [ ] Depois do MVP: alarmes em Direct Boot, backend com `syncToken`/push, verificação do app no Google e publicação no Play.

## Para retomar (copie e cole na thread)
> Claude, continue o SincronIA a partir de `guias/PROGRESSO.md`.
