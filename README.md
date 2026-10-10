# Gauderio-ERP

Sistema financeiro desktop para organizar as finanças de um pequeno negócio
diretamente no computador. Os dados ficam armazenados localmente em um banco
SQLite e não são enviados para a internet.

O Gauderio-ERP permite:

- acompanhar o saldo das contas bancárias;
- registrar entradas e saídas realizadas;
- controlar contas a pagar e a receber;
- criar lançamentos recorrentes;
- consultar vencimentos e movimentações atrasadas;
- analisar receitas, despesas e resultado por período;
- exportar informações para CSV, compatível com Excel;
- criar e restaurar cópias de segurança.

> **Importante:** o sistema ajuda no controle financeiro interno, mas não
> substitui a contabilidade, os extratos bancários nem as obrigações fiscais da
> empresa. A qualidade dos indicadores depende do registro correto e frequente
> das movimentações.

## Sumário

1. [Instalação no computador do usuário](#1-instalação-no-computador-do-usuário)
2. [Como o sistema organiza os valores](#2-como-o-sistema-organiza-os-valores)
3. [Primeira configuração](#3-primeira-configuração)
4. [Registrar entradas e saídas](#4-registrar-entradas-e-saídas)
5. [Controlar contas a pagar](#5-controlar-contas-a-pagar)
6. [Controlar contas a receber](#6-controlar-contas-a-receber)
7. [Consultar e corrigir o histórico](#7-consultar-e-corrigir-o-histórico)
8. [Conferir saldos bancários](#8-conferir-saldos-bancários)
9. [Usar o Dashboard](#9-usar-o-dashboard)
10. [Gerar relatórios](#10-gerar-relatórios)
11. [Exportar dados](#11-exportar-dados)
12. [Fazer backup e restaurar dados](#12-fazer-backup-e-restaurar-dados)
13. [Rotina recomendada de organização](#13-rotina-recomendada-de-organização)
14. [Boas práticas](#14-boas-práticas)
15. [Solução de problemas](#15-solução-de-problemas)
16. [Desenvolvimento e construção do instalador](#16-desenvolvimento-e-construção-do-instalador)

## 1. Instalação no computador do usuário

O cliente precisa apenas de um computador com Windows 10 ou Windows 11 de 64
bits. Java, Maven e SQLite já estão incluídos no aplicativo instalado.

1. Copie o arquivo `Gauderio-ERP-1.0.0.exe` para o computador.
2. Dê dois cliques no instalador.
3. Se o Windows exibir o aviso do SmartScreen, confira se o arquivo veio de uma
   fonte confiável. Como o instalador ainda não possui assinatura digital, pode
   ser necessário clicar em **Mais informações** e **Executar assim mesmo**.
4. Escolha a pasta de instalação, se desejar.
5. Mantenha habilitada a criação dos atalhos.
6. Conclua a instalação e abra o **Gauderio-ERP** pelo Menu Iniciar ou pela Área
   de Trabalho.

Na primeira abertura, o sistema cria automaticamente o banco de dados e as
categorias iniciais. Nenhuma conexão com servidor é necessária.

## 2. Como o sistema organiza os valores

Antes de começar, entenda estes quatro conceitos:

| Conceito | Uso no sistema |
| --- | --- |
| **Entrada / Receita** | Dinheiro que entra no negócio, como vendas, serviços e rendimentos. |
| **Saída / Despesa** | Dinheiro que sai do negócio, como aluguel, fornecedores e impostos. |
| **Realizada agora** | Já foi recebida ou paga. Afeta imediatamente o saldo bancário e os relatórios. |
| **Para depois** | Ainda será recebida ou paga. Fica pendente até sua liquidação e aparece em Contas a pagar/receber. |

O saldo de cada conta é calculado a partir do saldo inicial, das entradas e
saídas já realizadas e dos ajustes manuais. Uma conta pendente não altera o
saldo disponível até ser marcada como paga ou recebida.

## 3. Primeira configuração

Faça esta preparação uma única vez antes de registrar a rotina do negócio.

### 3.1 Escolha uma data de início

Defina uma data a partir da qual o controle será feito no sistema, por exemplo,
o primeiro dia do mês atual. Os saldos iniciais devem representar exatamente o
valor existente nas contas nessa data.

Evite cadastrar um saldo inicial atual e, ao mesmo tempo, registrar novamente
todas as movimentações antigas que já formavam esse saldo. Isso duplicaria os
valores.

### 3.2 Revise as categorias

1. No menu lateral, abra **Categorias**.
2. Confira as categorias de receita e despesa já disponíveis.
3. Para criar uma categoria, informe um nome claro, escolha **Receita** ou
   **Despesa** e clique em **Salvar**.
4. Para alterar uma categoria, selecione-a na tabela, edite os campos e salve.
5. Use **Nova** para limpar o formulário antes de outro cadastro.

Exemplos úteis:

- receitas: Vendas, Serviços, Mensalidades e Rendimentos;
- despesas: Fornecedores, Aluguel, Energia, Internet, Impostos, Folha de
  pagamento, Fretes e Manutenção.

Prefira poucas categorias consistentes. Por exemplo, não misture “Combustível”,
“Gasolina” e “Posto” se todas representam a mesma finalidade.

### 3.3 Cadastre as contas bancárias e o caixa

1. Abra **Contas bancárias**.
2. Em **Dados da conta**, informe um nome único e fácil de reconhecer, como
   `Conta corrente Banrisul` ou `Caixa da loja`.
3. Preencha banco, agência e número quando forem aplicáveis.
4. Em **Saldo inicial**, informe quanto havia disponível na data escolhida para
   começar o controle.
5. Clique em **Salvar**.
6. Repita o processo para cada conta usada pelo negócio.

Cadastre somente contas da empresa. Misturar valores pessoais e empresariais
dificulta a conferência e torna os relatórios menos confiáveis.

### 3.4 Faça o primeiro backup

Depois da configuração inicial:

1. Abra **Backup & Dados**.
2. Clique em **Criar cópia de segurança (Backup)**.
3. Salve o arquivo `.db` em uma pasta externa, pendrive ou serviço de nuvem.

## 4. Registrar entradas e saídas

### 4.1 Registrar uma entrada já recebida

Use este fluxo quando o dinheiro já entrou no caixa ou na conta bancária.

1. Clique em **Registrar entrada**.
2. Confirme o tipo **Entrada (Receita)**.
3. Escreva uma descrição identificável, como `Venda pedido 1048`.
4. Informe o valor. São aceitos formatos como `1500`, `1500,50` ou `1.500,50`.
5. Selecione a categoria correta.
6. Selecione a conta em que o dinheiro entrou.
7. Em **Quando acontece?**, escolha **Recebida agora**.
8. Clique em **Salvar**.

O valor passa a compor o saldo da conta, as receitas do mês e o resultado do
período.

### 4.2 Registrar uma saída já paga

1. Clique em **Registrar saída**.
2. Confirme o tipo **Saída (Despesa)**.
3. Informe descrição, valor, categoria e conta bancária.
4. Em **Quando acontece?**, escolha **Paga agora**.
5. Clique em **Salvar**.

O valor será descontado do saldo da conta e incluído nas despesas do período.

### 4.3 Registrar um valor futuro

Use este fluxo para boletos, compras a prazo, vendas ainda não recebidas e
outros compromissos futuros.

1. Abra **Registrar entrada** ou **Registrar saída**.
2. Preencha descrição, valor, categoria e conta prevista.
3. Escolha **A receber depois** ou **A pagar depois**.
4. Informe a data de vencimento ou recebimento.
5. Clique em **Salvar**.

O lançamento ficará como **Pendente**. Quando a data passar sem liquidação, o
sistema exibirá a situação **Vencida**.

### 4.4 Criar lançamentos recorrentes

Para aluguel, mensalidades, contratos ou outras repetições:

1. Preencha normalmente o lançamento.
2. Marque **Repetir este lançamento**.
3. Escolha a periodicidade: semanal, mensal, bimestral, trimestral, semestral
   ou anual.
4. Informe o número de ocorrências.
5. Clique em **Salvar**.

O sistema cria as ocorrências futuras automaticamente. Cada ocorrência é um
lançamento independente; editar ou excluir uma delas não altera as demais.

### 4.5 Registrar transferências entre contas próprias

Uma transferência entre duas contas da empresa não é receita nem despesa. Para
não distorcer o resultado:

1. Abra **Contas bancárias** e selecione a conta de origem.
2. Em **Ajuste de saldo**, registre uma **Saída** com a descrição
   `Transferência para [nome da conta]`.
3. Selecione a conta de destino.
4. Registre uma **Entrada** do mesmo valor com a descrição
   `Transferência recebida de [nome da conta]`.

Assim, o saldo de cada conta fica correto e os relatórios de receitas e despesas
não são alterados.

## 5. Controlar contas a pagar

1. Abra **Contas a pagar**.
2. Observe os cartões com o total pendente, o total vencido e os vencimentos dos
   próximos sete dias.
3. Use **+ Nova conta a pagar** para cadastrar um compromisso futuro.
4. Use a busca para localizar uma descrição, categoria ou conta bancária.
5. Filtre por **Pendentes**, **Vencidas**, **Pagas** ou **Todas**.
6. Quando pagar uma conta, selecione-a e clique em **Marcar como paga**.
7. Informe a data real em que o dinheiro saiu e confirme.

É possível selecionar várias linhas para liquidar ou excluir em conjunto.
Depois da liquidação, o valor passa a reduzir o saldo da conta escolhida e entra
nas despesas da data informada.

## 6. Controlar contas a receber

1. Abra **Contas a receber**.
2. Confira o total pendente, o total vencido e os recebimentos próximos.
3. Use **+ Nova conta a receber** para registrar uma venda ou cobrança futura.
4. Localize lançamentos pela busca ou pelo filtro de situação.
5. Quando receber, selecione o lançamento e clique em **Marcar como recebida**.
6. Informe a data real da entrada do dinheiro e confirme.

Depois da confirmação, o valor aumenta o saldo da conta e passa a fazer parte
das receitas do período correspondente.

## 7. Consultar e corrigir o histórico

1. Abra **Histórico**, dentro de Movimentações.
2. Use a busca para procurar por descrição, categoria ou conta.
3. Filtre por tipo: todas, entradas ou saídas.
4. Filtre por período: todos, mês atual, mês anterior ou últimos 12 meses.
5. Confira no rodapé a quantidade, o total de entradas, o total de saídas e o
   saldo da lista filtrada.

Com um lançamento selecionado, você pode:

- marcar uma pendência como paga ou recebida;
- editar informações incorretas;
- excluir definitivamente o registro;
- exportar a lista atualmente filtrada para CSV.

Antes de excluir, confirme se o correto não seria apenas editar. Exclusões não
podem ser desfeitas sem restaurar um backup anterior.

## 8. Conferir saldos bancários

Abra **Contas bancárias** para comparar o **Saldo atual** do sistema com o saldo
real no extrato ou no caixa.

O saldo apresentado considera:

```text
saldo inicial
+ entradas já recebidas
- saídas já pagas
+ ajustes manuais de entrada
- ajustes manuais de saída
```

Se houver diferença:

1. Procure primeiro lançamentos ausentes, duplicados ou associados à conta
   errada.
2. Confira se pagamentos e recebimentos continuam indevidamente pendentes.
3. Corrija os registros encontrados.
4. Use **Ajuste de saldo** apenas quando a origem da diferença não puder ser
   representada por uma movimentação normal.
5. Descreva claramente o motivo do ajuste.

Alterar o saldo inicial depois que o uso já começou modifica todo o saldo atual.
Faça isso somente para corrigir um cadastro inicial incorreto.

## 9. Usar o Dashboard

O **Dashboard** é a visão rápida da situação do negócio:

- **Saldo disponível:** soma dos saldos atuais de todas as contas;
- **Receitas do mês:** entradas já recebidas no mês atual;
- **Despesas do mês:** saídas já pagas no mês atual;
- **Resultado do mês:** receitas menos despesas;
- **A receber:** receitas ainda pendentes;
- **A pagar:** despesas ainda pendentes;
- **Atrasadas:** contas a pagar pendentes com vencimento passado;
- **Próximos vencimentos:** compromissos que exigem atenção em breve;
- **Últimas movimentações:** registros recentes.

Um resultado positivo significa que as receitas realizadas no período superam
as despesas realizadas. Isso não significa necessariamente que todas as contas
futuras estão cobertas; confira também os valores a pagar e a receber.

## 10. Gerar relatórios

1. Abra **Relatórios**.
2. Escolha o período: ano completo, primeiro semestre ou segundo semestre.
3. Escolha o ano.
4. Escolha o tipo: todas, receitas ou despesas.
5. Clique em **Gerar relatório**.

A tela apresenta:

- total de receitas, despesas e resultado líquido;
- comparação mensal entre receitas e despesas;
- evolução do resultado acumulado;
- distribuição por categoria;
- tabela detalhada mês a mês.

Os relatórios consideram lançamentos efetivamente pagos ou recebidos. Valores
pendentes aparecem nas telas de contas a pagar e receber, mas ainda não entram
no resultado realizado.

## 11. Exportar dados

Os arquivos CSV usam ponto e vírgula como separador e codificação compatível com
o Excel em português.

Há quatro formas principais de exportação:

- **Histórico > Exportar (CSV):** exporta somente a lista filtrada;
- **Contas a pagar/receber > Exportar (CSV):** exporta a visão filtrada atual;
- **Relatórios > Exportar relatório (CSV):** exporta o demonstrativo mensal;
- **Backup & Dados > Exportar todos os lançamentos:** exporta o histórico
  completo.

O CSV é indicado para análise, conferência e envio ao contador. Ele não
substitui o backup do banco, pois não contém toda a estrutura necessária para
restaurar o sistema.

## 12. Fazer backup e restaurar dados

### 12.1 Criar uma cópia de segurança

1. Abra **Backup & Dados**.
2. Confira o local do banco e as estatísticas exibidas.
3. Clique em **Criar cópia de segurança (Backup)**.
4. Escolha uma pasta segura e salve o arquivo `.db`.
5. Mantenha pelo menos uma cópia fora do computador principal.

Uma estratégia simples é conservar três arquivos: o backup mais recente, o da
semana anterior e o do fechamento do mês anterior.

### 12.2 Restaurar um backup

> **Atenção:** a restauração substitui todos os dados atuais pelo conteúdo do
> arquivo escolhido.

1. Antes de restaurar, crie um backup do estado atual.
2. Clique em **Restaurar cópia de segurança**.
3. Leia o aviso e clique em **Continuar**.
4. Selecione o arquivo `.db` correto.
5. Aguarde a confirmação e confira Dashboard, contas e histórico.

Nunca renomeie um arquivo CSV para `.db`: somente backups criados pelo próprio
sistema devem ser usados na restauração.

### 12.3 Onde os dados ficam

No Windows, o banco principal fica em:

```text
%LOCALAPPDATA%\Gauderio-ERP\gauderio.db
```

A pasta pode ser aberta pelo botão **Abrir pasta de dados no Windows**. Não
edite, mova ou apague o arquivo enquanto o aplicativo estiver aberto.

Desinstalar ou atualizar o programa não deve remover essa pasta, mas um backup
externo continua sendo indispensável contra defeito do computador, exclusão
acidental ou perda do disco.

## 13. Rotina recomendada de organização

### Todos os dias

1. Registre cada entrada e saída realizada.
2. Cadastre novas contas futuras assim que forem conhecidas.
3. Marque pagamentos e recebimentos concluídos com a data real.
4. Observe vencimentos e atrasos no Dashboard.

### Uma vez por semana

1. Compare os saldos do sistema com os extratos bancários e o caixa.
2. Verifique contas vencidas e os próximos sete dias.
3. Corrija duplicidades, categorias e contas incorretas.
4. Crie um backup fora do computador.

### No fechamento do mês

1. Confirme que todas as movimentações do mês foram registradas.
2. Confira se nenhuma conta quitada continua pendente.
3. Concilie cada saldo bancário.
4. Gere o relatório do período.
5. Analise resultado, principais despesas e categorias fora do padrão.
6. Exporte o CSV para arquivo ou contador, se necessário.
7. Crie um backup identificado com o mês fechado.

## 14. Boas práticas

- Registre as movimentações na data em que acontecerem; não dependa da memória.
- Use descrições pesquisáveis, como número do pedido, fornecedor ou cliente.
- Selecione sempre a conta bancária correta, pois ela determina o saldo afetado.
- Não registre a mesma operação como realizada e também como pendente.
- Não trate empréstimos, aportes, retiradas de sócios ou transferências como
  vendas/despesas sem alinhar a classificação com o contador.
- Evite categorias genéricas em excesso, como “Outros”.
- Não compartilhe o mesmo arquivo de banco simultaneamente entre computadores.
- Faça backup antes de exclusões grandes, restaurações ou fechamento mensal.
- Proteja os backups, pois eles contêm informações financeiras do negócio.

## 15. Solução de problemas

### O saldo do sistema não corresponde ao banco

Confira saldo inicial, conta associada, duplicidades, lançamentos ausentes,
pendências já quitadas e ajustes manuais. Use um novo ajuste somente depois de
investigar essas possibilidades.

### Uma conta vencida não aparece nas despesas do mês

Isso é esperado enquanto ela estiver pendente. Marque-a como paga e informe a
data real do pagamento.

### Um recebimento não aumentou o saldo

Confira se foi marcado como recebido e se uma conta bancária foi selecionada no
lançamento.

### O CSV não abre em colunas no Excel

Importe o arquivo escolhendo codificação UTF-8 e delimitador ponto e vírgula
(`;`). Em instalações brasileiras do Excel, normalmente a abertura direta já
faz essa identificação.

### O aplicativo não abre ou os dados parecem indisponíveis

1. Reinicie o aplicativo.
2. Abra **Backup & Dados** e confira o caminho do banco.
3. Não crie manualmente outro arquivo `gauderio.db`.
4. Se o banco estiver danificado, restaure o backup válido mais recente.

## 16. Desenvolvimento e construção do instalador

Esta seção é destinada a desenvolvedores e responsáveis pela distribuição.

### Requisitos de desenvolvimento

- Windows 10 ou 11 de 64 bits;
- JDK 26 completo, com `java`, `javac` e `jpackage` no `PATH`;
- Apache Maven 3.9 ou superior no `PATH`;
- WiX Toolset 3.14 ou 4 para gerar `.exe` ou `.msi`.

Confirme o ambiente no PowerShell:

```powershell
java --version
jpackage --version
mvn --version
candle -?
light -?
```

Com WiX 4, use `wix --version` no lugar dos dois últimos comandos.

### Rodar o código

Na raiz do projeto:

```powershell
mvn clean javafx:run
```

Para validar os testes e executar o JAR:

```powershell
mvn clean package
java --enable-native-access=ALL-UNNAMED -jar .\target\gauderio-erp-1.0.0.jar
```

### Construir o instalador para o cliente

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-installer.ps1
```

O instalador será criado em:

```text
target\installer\Gauderio-ERP-1.0.0.exe
```

Formatos alternativos:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-installer.ps1 -Type msi
powershell -ExecutionPolicy Bypass -File .\scripts\build-installer.ps1 -Type app-image
```

O tipo `app-image` não exige WiX e serve para testar o aplicativo empacotado.
Antes de uma distribuição pública, recomenda-se assinar digitalmente o
instalador para reduzir alertas do Microsoft SmartScreen.
