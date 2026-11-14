<div align="center">

<img src="docs/assets/banner.svg" alt="SincronIA: alarmes de verdade para o Google Agenda e o Google Tasks. Feito com o Claude." width="100%">

**Agenda Android com alarme de verdade, sincronizada nos dois sentidos com o Google Agenda e o Google Tasks.**

[![Licença: MIT](https://img.shields.io/badge/licen%C3%A7a-MIT-blue.svg)](LICENSE)
![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-1565C0)
![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-0277BD)

🌐 [English](README.md) · [中文](README.zh-CN.md) · [हिन्दी](README.hi.md) · [Español](README.es.md) · [العربية](README.ar.md) · **Português**

</div>

---

Os lembretes do Google Agenda passam despercebidos: um som de notificação, uma vibração, e acabou. O SincronIA transforma eventos e tarefas em **alarmes que acordam de verdade**: tela cheia sobre a tela bloqueada, tocando até no silencioso e no "Não perturbe", com Soneca e Dispensar. Tudo o que você cria ou edita no app vai para o Google, e tudo o que muda no Google aparece no app.

## Funcionalidades

- Tela **Hoje**: saudação, tarefas atrasadas, itens de dia inteiro, linha do tempo com a marca "agora" e tarefas concluídas no dia.
- **Agenda** (faixa da semana + lista do dia) e **Tarefas** (todas as listas do Google Tasks, com subtarefas).
- **Criar, editar e excluir** eventos e tarefas, com sincronização nos dois sentidos.
- **Alarme de verdade**: tela cheia com o celular bloqueado, notificação com Soneca/Dispensar com o celular em uso, som no silencioso; os alarmes continuam depois de reiniciar, atualizar o app ou mudar de fuso.
- Desligar o alarme de uma **série recorrente inteira** de uma vez.
- **Funciona offline**: as mudanças ficam numa fila e sobem sozinhas quando a internet volta.
- Ajustes: alarme padrão para novos eventos, soneca, som do alarme, lembrete diário das tarefas sem hora, intervalo de sincronização, atalhos de permissão e um diagnóstico exportável sem dados pessoais.

## Privacidade

- **Sem servidor.** O app fala direto com as APIs do Google Agenda e do Google Tasks; não existe servidor do SincronIA.
- Só dois escopos: `calendar.events` e `tasks`. Sem API key e sem client secret.
- Os dados ficam no celular (Room + DataStore). O backup do Android está desligado, e os logs nunca têm token, e-mail ou título de evento.
- "Desconectar" revoga o acesso, cancela os alarmes e apaga os dados locais.

## Em breve na Google Play

> 🚀 Estamos planejando lançar o SincronIA na Google Play, de graça para todos. Até lá, você pode compilar o app (veja abaixo).

## Como usar

1. Abra o app e **entre com a sua conta Google**, permitindo o acesso à Agenda e às Tarefas.
2. Permita as **notificações** e os **alarmes em tela cheia** quando o app pedir. Se faltar alguma, uma faixa na **Hoje** mostra o atalho.
3. Seus eventos e tarefas aparecem em **Hoje**, **Agenda** e **Tarefas**. Puxe a Hoje para baixo para sincronizar na hora.
4. Toque em **+** para criar um evento ou uma tarefa e escolha quando o alarme toca: na hora ou alguns minutos antes (na tarefa, um horário).
5. Quando o alarme tocar, toque em **Soneca** ou **Dispensar**. Em **Ajustes** você muda o alarme padrão, a soneca, o som e o intervalo de sincronização.

> 💡 Para os alarmes e a sincronização nunca atrasarem, deixe o uso de bateria do app como **Sem restrições** nas configurações do celular.

## Como compilar

Requisitos: Android Studio (ou JDK 17+) e o Android SDK (compileSdk 37). O app roda no Android 8.0 (API 26) ou mais novo.

```bash
git clone https://github.com/GustavoHRX/sincronia.git
cd sincronia
./gradlew assembleDebug test lint
```

- **Modo demonstração (sem conta Google):** instale o build debug e toque em **"Entrar em modo demonstração"**. O app se enche de eventos e tarefas de exemplo.
- **Entrar com o Google:** crie seus próprios clientes OAuth no Google Cloud (um cliente *Android* com o seu SHA-1 de debug e um cliente *Web*), ative as APIs do Calendar e do Tasks e coloque o ID do cliente Web no `local.properties`:

  ```properties
  GOOGLE_WEB_CLIENT_ID=123456789-abc.apps.googleusercontent.com
  ```

  O passo a passo está na seção 3.2 de [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md). O SHA-1 sai com `./gradlew signingReport`.
- **Build de release:** crie um `key.properties` na raiz do projeto apontando para a sua keystore (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`). Sem ele, o `assembleRelease` gera um APK sem assinatura.

> Nunca comite `local.properties`, `key.properties`, keystores, `google-services.json` nem `client_secret*.json`. O `.gitignore` já bloqueia esses arquivos.

## Tecnologias

Kotlin · Jetpack Compose + Material 3 · Hilt · Room · DataStore · WorkManager · Retrofit + OkHttp + kotlinx.serialization · Credential Manager + `AuthorizationClient` · `AlarmManager.setAlarmClock()`

```
app/src/main/java/com/sincronia/app/
├── alarm/       receivers do alarme, serviço em primeiro plano, tela cheia, notificações
├── core/        login, banco (Room), rede (APIs do Google), cálculo dos alarmes, log
├── data/        repositórios, sincronização (fila + download), ajustes, dados de demonstração
├── navigation/  navegação
└── ui/          Hoje, Agenda, Tarefas, editor, ajustes, onboarding, permissões
```

## Documentação

| Arquivo | O que é |
|---|---|
| [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md) | Especificação completa: arquitetura, regras de sync, alarmes |
| [`docs/DECISOES.md`](docs/DECISOES.md) | Decisões e desvios da especificação, com data e motivo |

## Como contribuir

Issues e pull requests são bem-vindos. Veja o [`CONTRIBUTING.md`](CONTRIBUTING.md).

## Licença

[MIT](LICENSE) © GustavoHRX
