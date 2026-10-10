# Gauderio-ERP · Status do Projeto e Melhorias Realizadas

## 1. Correções Críticas de Lógica de Negócios
- [x] **Inversão de Entrada e Saída corrigida:**
  - `Transacao.java`: Corrigido `getTipoTexto()` para retornar `"Entrada"` quando `TIPO_RECEITA` e `"Saída"` quando `TIPO_DESPESA`. Adicionados métodos auxiliares `isReceita()` e `isDespesa()`.
  - `FormularioMovimentacao.java`: Corrigido o mapeamento do `tipoCombo` ("Entrada (Receita)" -> `TIPO_RECEITA`, "Saída (Despesa)" -> `TIPO_DESPESA`).
  - `ContasView.java`: Corrigido o ComboBox de ajuste manual de saldo ("Entrada (aumenta o saldo)" e "Saída (diminui o saldo)") e a exibição do tipo na tabela de ajustes.
  - `DashboardView.java` e `MovimentacoesView.java`: Corrigidos botões de ação e mensagens ("+ Registrar entrada" em verde e "- Registrar saída" em vermelho).
  - `Main.java`: Corrigida rota dos botões no menu lateral para abrir o tipo correspondente.

## 2. Salvamento, Segurança de Dados e Exportação
- [x] **Sistema de Backup e Restauração (`BackupService.java`):**
  - Criação de backup instantâneo do banco de dados SQLite (`.db`) com integridade (flush/checkpoint).
  - Restauração segura de backup com confirmação prévia e reinicialização automática das conexões.
  - Exibição de estatísticas do banco de dados (tamanho em disco, total de transações, contas e categorias).
  - Acesso com um clique para abrir a pasta de dados local no Windows Explorer.
- [x] **Exportação para Excel/CSV (`ExportadorCsv.java`):**
  - Exportação de lançamentos e contas filtradas com cabeçalho, formato moeda brasileiro e codificação UTF-8 com BOM.
  - Exportação de relatório financeiro anual/semestral detalhado mês a mês.

## 3. Melhorias de UI / UX e Facilidade de Uso
- [x] **Barra de Busca e Filtros em Tempo Real:**
  - Histórico de Movimentações: busca textual instantânea por descrição, categoria ou conta bancária.
  - Contas a Pagar e a Receber: filtro textual e cálculo dinâmico de prazos.
- [x] **Barras de Resumo e Totais nas Tabelas:**
  - Rodapé dinâmico no histórico exibindo quantidade de lançamentos, total de entradas, total de saídas e saldo líquido do período filtrado.
  - Rodapé em Contas a Pagar e a Receber com quantidade de contas e total listado.
- [x] **Badges e Indicadores Visuais:**
  - Coluna de Situação formatada com pills arredondadas coloridas ("Paga/Recebida" em verde, "Vencida" em vermelho, "Pendente" em amarelo/âmbar).
  - Coluna Tipo com indicadores `▲ Entrada` (verde) e `▼ Saída` (vermelho).
  - Coluna de Prazo calculando dias até o vencimento ("Hoje", "Em X dias", "X dias de atraso").
- [x] **Nova aba no painel de Configurações:**
  - Aba "Backup & Dados" integrada à navegação e com atalho direto no menu lateral.

## 4. Ícones Vetoriais e Identidade Visual
- [x] **Utilitário de Ícones SVG (`IconeUtil.java`):**
  - Ícones vetoriais nítidos em alta definição adicionados em todos os botões de ação (Salvar, Limpar, Novo, Editar, Excluir, Liquidar, Exportar, Backup).
  - Ícones temáticos revisados e padronizados no menu lateral (`style.css`).
- [x] **Ícone da Aplicação:**
  - Gerado ícone oficial `icon.png` (com as cores da bandeira gaúcha e identidade financeira) aplicado à barra de título da janela principal (`Stage`).

## 5. Qualidade Técnica e Testes Automatizados
- [x] Adicionado JUnit 5 (`junit-jupiter`) no `pom.xml`.
- [x] Criada suite de testes automatizados `RegrasNegocioTest.java` validando tipos, cálculo de saldo bancário, rotinas de backup, exportação CSV e formatação.
- [x] Corrigido uso do construtor obsoleto de `Locale` para `Locale.of("pt", "BR")`.
- [x] Testes compilados e executados com 100% de aprovação.