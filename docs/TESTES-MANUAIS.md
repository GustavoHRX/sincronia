# Testes manuais

Passos para o Gustavo executar no celular ou emulador. Cada marco acrescenta a sua seção; o que depender da seção 3 da especificação e ainda não estiver pronto fica como pendente em `docs/PROGRESSO.md`.

## M0 · Base

Pré-requisito: celular com Depuração USB ligada ou emulador com imagem "Google Play".

```bash
./gradlew assembleDebug test lint
./gradlew installDebug
adb shell am start -n com.sincronia.app/.MainActivity
```

- [ ] O app abre sem travar.
- [ ] A barra inferior tem 4 abas: Hoje, Agenda, Tarefas, Ajustes.
- [ ] Tocar em cada aba mostra a tela (vazia) correspondente e destaca a aba ativa em azul.
- [ ] Cores: azul `#1565C0`, sem as cores dinâmicas do papel de parede (Android 12+).

## M1 · Dados locais e demonstração

No Mjolnir, com o emulador `Medium_Phone_API_37.0` ou um celular conectado:

```bash
JAVA_HOME=~/Documents/Workspace/android-studio/jbr ./gradlew assembleDebug test lint
JAVA_HOME=~/Documents/Workspace/android-studio/jbr ./gradlew installDebug
~/Android/Sdk/platform-tools/adb shell am start -n com.sincronia.app/.MainActivity
```

- [x] No build debug, o botão **Entrar em modo demonstração** aparece. O login Google fica desativado.
- [x] Depois de entrar, **Agenda** mostra semana, dia selecionado, evento de dia inteiro, evento que começou ontem, evento com hora e tarefas de hoje; mudar o dia e a semana mostra os itens correspondentes.
- [x] **Tarefas** mostra as listas Pessoal e Trabalho, uma subtarefa recuada, uma tarefa atrasada e a seção recolhível Concluídas.
- [x] Fechar e reabrir o app: a demonstração continua ativa e os itens não duplicam (emulador API 37, 02/11).
- [ ] Conferir os dados com o aparelho sem internet (a UI lê o Room).
- [ ] Em uma instalação de release, o botão de demonstração não aparece. A configuração do Google Cloud fica para M3.

Os itens marcados foram verificados no emulador API 37 em 02/11/2026 por captura de tela e hierarquia da UI. As demais verificações manuais ficam para o Gustavo.

## M2 · Hoje

No Mjolnir, com o emulador `Medium_Phone_API_37.0` ou um celular conectado, já no modo demonstração:

```bash
JAVA_HOME=~/Documents/Workspace/android-studio/jbr ./gradlew installDebug
~/Android/Sdk/platform-tools/adb shell am start -n com.sincronia.app/.MainActivity
```

- [x] A aba **Hoje** abre primeiro, com "Bom dia", "Boa tarde" ou "Boa noite" (sem nome no modo demonstração) e a data de hoje.
- [x] **Atrasadas** mostra "Pagar conta atrasada" com "Venceu ontem".
- [x] **Dia inteiro** mostra "Semana de provas" (com "até" o dia de amanhã), "Dia de estudos", "Comprar mantimentos" e "Comprar leite", estas duas com "Sem hora definida".
- [x] **Linha do tempo** começa com "Viagem noturna" ("ontem, 22:00 – 01:00"), depois "Reunião de planejamento" (10:00 – 11:00), com a linha azul "Agora HH:mm" no lugar do horário atual.
- [x] **Concluídas hoje** mostra "Organizar documentos" riscada e marcada.
- [ ] Puxar a tela para baixo mostra o indicador de atualização, que some sozinho; os itens não duplicam.
- [ ] Deixar o app aberto até virar o minuto: o horário da linha "agora" muda junto com o relógio do celular.
- [x] Na **Agenda**, os cartões têm o mesmo formato da Hoje; "Semana de provas" aparece de ontem até amanhã.

Os itens marcados foram verificados no emulador API 37 em 05/11/2026 por captura de tela e hierarquia da UI. Puxar para atualizar rodou sem duplicar itens, mas o indicador não foi visto, porque a sync da demonstração dura menos de 1 s.
