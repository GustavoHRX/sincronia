# SincronIA · Prompt para o Figma (v2)

**Como usar:** no **Figma Make** (ou no First Draft do Figma Design), cole o **Prompt 1**. Quando terminar, na mesma conversa, cole o **Prompt 2** e depois o **Prompt 3**. Pedir tudo de uma vez costuma gerar telas incompletas ou com estilos diferentes. Se a ferramenta não mantiver a conversa, cole o Prompt 1 junto com cada um dos outros.

As cores e os componentes são os mesmos da seção 9 da especificação (`integracao-google-agenda-tasks.md`), então o Figma e o app vão bater. Os contrastes foram conferidos pelo WCAG.

---

## Prompt 1 · Design System + Hoje + Criar

Crie o design de um app Android chamado **SincronIA**: agenda com alarme que sincroniza com Google Agenda e Google Tasks. Estilo Material Design 3, limpo, moderno e arejado. Frames **Android Compact (412 × 917)**. Todos os textos em português do Brasil. Não use emoji na interface.

PALETA (use só estas cores e crie como variáveis do Figma):
- primary #1565C0: botões, FAB, aba ativa, barra do cartão de evento; texto branco sobre ele
- primaryContainer #D6E6FF: cartão de evento, com texto #0A2A5E
- secondary #0277BD: checkbox e ícones de tarefa, linha "agora"
- secondaryContainer #E3F2FD: cartão de tarefa, com texto #0D1B2A
- tertiary #42A5F5: azul claro da marca, só em preenchimentos com texto #0A2A5E (chip e segmento selecionados, ilustrações, logo). Nunca como texto, ícone ou borda sobre fundo branco
- background #FFFFFF; surfaceVariant #F4F8FD (seções e campos)
- texto principal #0D1B2A; texto secundário #505F73
- outline #6B7785 (borda de campos e checkbox desmarcado); divisórias #C9D6E6
- error #D32F2F (só excluir e erro)

TIPOGRAFIA E FORMAS: fonte Inter; títulos em semibold; cantos de 16 px nos cartões e 28 px no botão flutuante e nos diálogos; espaçamento base de 8 px; sombras suaves; área de toque mínima de 48 px.

COMPONENTES (reutilizáveis, com variantes):
- Cartão de EVENTO: fundo #D6E6FF, barra vertical #1565C0 à esquerda, título, horário (09:00 – 10:00), local e ícone de sino quando tem alarme. Variante "somente leitura" com um pequeno ícone de cadeado.
- Cartão de TAREFA: fundo #E3F2FD, checkbox circular #0277BD, título, data e, se tiver, ícone de despertador com a hora (14:30). Variantes: sem hora (texto secundário "Sem hora definida"), concluída (título riscado) e atrasada (data em #D32F2F).
- Barra de navegação inferior com 4 abas: Hoje, Agenda, Tarefas, Ajustes.
- Botão flutuante (FAB) azul #1565C0 com ícone "+".
- Botões (preenchido, contorno, texto), chips, seletor segmentado, campos de texto, switch, snackbar ("Tarefa concluída" com a ação "Desfazer") e faixa de aviso (ícone, texto e botão).

Primeiro, crie uma página "Design System" com as variáveis de cor, os estilos de texto e os componentes acima. Depois, crie estas telas:

1. **Hoje**: saudação "Bom dia, Gustavo" e a data "Terça, 29 de setembro"; seção "Atrasadas" com 1 tarefa; faixa "Dia inteiro" com 1 evento de dia inteiro e 1 tarefa sem hora; linha do tempo com eventos e tarefas misturados por horário e uma linha horizontal #0277BD marcando a hora atual; no fim, "Concluídas hoje" com 1 tarefa riscada. FAB "+" e navegação inferior.
2. **Criar**: bottom sheet com o seletor segmentado "Evento | Tarefa" no topo. Em Evento: título, "Dia inteiro" (switch), data e hora de início e de fim, local, descrição e "Alarme" (Padrão da agenda / 10 min antes / Sem alarme). Em Tarefa: título, data, "Hora do alarme" (opcional), lista e notas. Botão "Salvar".

---

## Prompt 2 · Boas-vindas, Permissões, Agenda, Tarefas, Ajustes

Continue o app **SincronIA** no mesmo estilo, com as mesmas variáveis de cor e os mesmos componentes. Crie estas telas:

3. **Boas-vindas**: logo SincronIA (calendário com relógio, em azul), a frase "Sua agenda e suas tarefas, sincronizadas e com alarme" e o botão "Entrar com Google".
4. **Permissões**: lista com Notificações ("Obrigatório para o alarme aparecer"), Alarmes em tela cheia e Alarmes e lembretes. Cada item tem ícone, texto curto e, à direita, o status "Permitido" ou o botão "Permitir". No rodapé, o botão "Continuar" e o link "Fazer depois".
5. **Agenda**: faixa com os 7 dias da semana no topo (dia atual em círculo #1565C0 com número branco e um ponto embaixo dos dias com compromissos) e, abaixo, a lista do dia escolhido com cartões de evento e de tarefa, no mesmo formato da tela Hoje. Sem grade de horas.
6. **Tarefas**: chips com as listas (Minhas tarefas, Trabalho, Casa), com o chip selecionado em #42A5F5 e texto #0A2A5E; tarefas pendentes, uma delas com 1 subtarefa recuada; seção recolhível "Concluídas".
7. **Ajustes**: conta Google (foto, nome, e-mail e botão "Desconectar"); Alarme padrão para novos eventos (10 min antes); Lembrete diário das tarefas sem hora (switch desligado, 09:00); Soneca (10 min); Som do alarme; Sincronizar a cada (15, 30 ou 60 min, com 30 selecionado); "Sincronizar agora" com "Última sincronização: 10:42"; Permissões; Ajustes de bateria; Exportar diagnóstico.

---

## Prompt 3 · Alarme, estados e tema escuro

Continue o app **SincronIA** no mesmo estilo. Crie:

8. **Alarme em tela cheia**: fundo em gradiente #0D47A1 → #1565C0, hora grande em branco (14:30), título do compromisso, o tipo (Evento ou Tarefa) e dois botões grandes arredondados: "Soneca 10 min" (contorno branco) e "Dispensar" (fundo branco com texto #1565C0).
9. **Alarme como notificação** (celular desbloqueado e em uso): notificação expandida no topo da tela, com ícone de despertador #1565C0, título, hora, tipo e os botões "Soneca 10 min" e "Dispensar". Mostre também a versão recolhida.
10. **Estados da tela Hoje**: vazia ("Nada para hoje", com uma ilustração simples em azul claro); sem conexão (faixa discreta "Sem conexão, mostrando dados salvos"); faixa "Reconecte sua conta Google" com botão; faixa "Alarmes desativados: permita notificações" com botão; carregando (esqueleto dos cartões).
11. **Editor somente leitura** de um evento recorrente: aviso "Evento recorrente: edite no Google Agenda", campos desativados e só o campo "Alarme" editável.
12. **Erro de sincronização** num item: cartão com ícone de alerta e as ações "Tentar de novo" e "Descartar alteração".
13. **Versões escuras** da tela Hoje e do Alarme em tela cheia, com um esquema escuro gerado a partir do azul #1565C0 (sem roxo).
