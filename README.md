# SincronIA

App Android de agenda que sincroniza com o **Google Agenda** e o **Google Tasks** e tem **alarmes** para eventos e tarefas. Sem versão web: o app conversa direto com as APIs do Google.

> Status: M0 (base do projeto) em andamento. Veja [`docs/PROGRESSO.md`](docs/PROGRESSO.md).

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
claude
```

Aí é só dizer ao Claude Code:

```
Leia docs/integracao-google-agenda-tasks.md e comece pelo Marco M0
```

## Segurança

Este repositório é público. **Nunca** comite `google-services.json`, `local.properties`, `key.properties`, keystores (`*.jks`, `*.keystore`), `.env` nem `client_secret*.json`. O `.gitignore` já bloqueia esses arquivos.
