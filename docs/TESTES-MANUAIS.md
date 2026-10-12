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
