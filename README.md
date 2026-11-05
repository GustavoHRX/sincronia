# SincronIA

App Android de agenda que sincroniza com o **Google Agenda** e o **Google Tasks** e tem **alarmes** para eventos e tarefas. Sem versão web: o app conversa direto com as APIs do Google.

> Status: M2 (tela Hoje) concluído. Próximo: M3 (login e leitura do Google). Veja [`docs/PROGRESSO.md`](docs/PROGRESSO.md).

## O que tem aqui

| Arquivo | Para que serve |
|---|---|
| [`docs/integracao-google-agenda-tasks.md`](docs/integracao-google-agenda-tasks.md) | Especificação do MVP v2 (arquitetura, APIs, marcos M0…) |
| [`docs/prompt-figma.md`](docs/prompt-figma.md) | Prompt do Figma em 3 partes (azul, azul claro, branco) |
| [`docs/PROGRESSO.md`](docs/PROGRESSO.md) | Estado atual e próximos passos |
| [`docs/revisao-achados-parcial.md`](docs/revisao-achados-parcial.md) | Registro da revisão da spec (histórico) |

## Como começar

```bash
git clone https://github.com/GustavoHRX/sincronia.git
cd sincronia
JAVA_HOME=~/Documents/Workspace/android-studio/jbr ./gradlew assembleDebug test lint
```

No build debug, instale o app e toque em **Entrar em modo demonstração**. Agenda e Tarefas mostram dados locais; a conexão com o Google entra no M3. Veja [`docs/TESTES-MANUAIS.md`](docs/TESTES-MANUAIS.md).

## Segurança

Este repositório é público. **Nunca** comite `google-services.json`, `local.properties`, `key.properties`, keystores (`*.jks`, `*.keystore`), `.env` nem `client_secret*.json`. O `.gitignore` já bloqueia esses arquivos.
