package br.com.gauderio.ui;

import br.com.gauderio.dao.ContaDAO;
import br.com.gauderio.dao.MovimentacaoDAO;
import br.com.gauderio.model.ContaBancaria;
import br.com.gauderio.model.MovimentacaoSaldo;
import br.com.gauderio.util.Formatador;
import br.com.gauderio.util.UiUtil;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/** Controle das contas do banco, com ajuste manual de saldo quando necessário. */
public class ContasView extends BorderPane implements Refreshable {

    private final ContaDAO contaDAO = new ContaDAO();
    private final MovimentacaoDAO movimentacaoDAO = new MovimentacaoDAO();
    private final Runnable onDadosAlterados;

    private int idContaEmEdicao = -1;

    private final TextField nomeField = new TextField();
    private final TextField bancoField = new TextField();
    private final TextField agenciaField = new TextField();
    private final TextField numeroField = new TextField();
    private final TextField saldoInicialField = new TextField();
    private final Label lblContaMov = new Label("Selecione uma conta na lista");

    private final ComboBox<String> tipoMovCombo = new ComboBox<>(
            FXCollections.observableArrayList("Entrada (aumenta o saldo)", "Saída (diminui o saldo)"));
    private final TextField descMovField = new TextField();
    private final TextField valorMovField = new TextField();

    private final TableView<ContaBancaria> tabelaContas = new TableView<>();
    private final TableView<MovimentacaoSaldo> tabelaMov = new TableView<>();

    public ContasView() {
        this(() -> {
        });
    }

    public ContasView(Runnable onDadosAlterados) {
        this.onDadosAlterados = onDadosAlterados == null ? () -> {
        } : onDadosAlterados;
        setPadding(new Insets(24));
        getStyleClass().add("content");

        VBox cabecalho = UiUtil.cabecalho("Contas bancárias",
                "Cadastre suas contas do banco e acompanhe o saldo de cada uma.");

        VBox listaContas = criarListaContas();
        HBox linhaContas = new HBox(18, criarFormConta(), listaContas);
        linhaContas.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(listaContas, Priority.ALWAYS);

        VBox listaMovimentacoes = criarListaMovimentacoes();
        HBox linhaMov = new HBox(18, criarFormMovimentacao(), listaMovimentacoes);
        linhaMov.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(listaMovimentacoes, Priority.ALWAYS);

        VBox area = new VBox(16, cabecalho, linhaContas, linhaMov);
        setCenter(area);

        tipoMovCombo.setValue("Entrada (aumenta o saldo)");
        atualizarSobre();
    }

    @Override
    public void refresh() {
        atualizarListaContas();
        ContaBancaria selecionada = tabelaContas.getSelectionModel().getSelectedItem();
        if (selecionada != null) {
            carregarMovimentacoes(selecionada);
        }
    }

    private VBox criarFormConta() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(330);

        Label titulo = new Label("Dados da conta");
        titulo.getStyleClass().add("form-title");

        nomeField.setPromptText("Ex.: Conta corrente Banco do Brasil");
        bancoField.setPromptText("Banco (Ex.: Banrisul, BB, Caixa)");
        agenciaField.setPromptText("Agência");
        numeroField.setPromptText("Número da conta");
        saldoInicialField.setPromptText("0,00");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Nome da conta *"), 0, 0);
        grid.add(nomeField, 0, 1);
        grid.add(new Label("Banco"), 0, 2);
        grid.add(bancoField, 0, 3);
        grid.add(new Label("Agência"), 0, 4);
        grid.add(agenciaField, 0, 5);
        grid.add(new Label("Número da conta"), 0, 6);
        grid.add(numeroField, 0, 7);
        grid.add(new Label("Saldo inicial"), 0, 8);
        grid.add(saldoInicialField, 0, 9);

        Label ajudaSaldo = new Label("Informe quanto havia disponível nessa conta quando você começou "
                + "a usar o Gauderio-ERP. Se não souber, deixe 0,00.");
        ajudaSaldo.getStyleClass().add("form-hint");
        ajudaSaldo.setWrapText(true);
        ajudaSaldo.setMaxWidth(290);

        Button salvar = new Button("Salvar");
        salvar.getStyleClass().addAll("btn", "btn-green");
        salvar.setOnAction(e -> salvarConta());

        Button novo = new Button("Nova conta");
        novo.getStyleClass().addAll("btn", "btn-outline");
        novo.setOnAction(e -> novoConta());

        Button excluir = new Button("Excluir conta");
        excluir.getStyleClass().addAll("btn", "btn-outline-red");
        excluir.setOnAction(e -> excluirConta());

        HBox botoes = new HBox(8, salvar, novo, excluir);
        botoes.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(titulo, grid, ajudaSaldo, botoes);
        return card;
    }

    private VBox criarListaContas() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label titulo = new Label("Contas cadastradas");
        titulo.getStyleClass().add("card-label");

        TableColumn<ContaBancaria, String> cNome = new TableColumn<>("Nome");
        cNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        cNome.setMinWidth(170);

        TableColumn<ContaBancaria, String> cBanco = new TableColumn<>("Banco");
        cBanco.setCellValueFactory(new PropertyValueFactory<>("banco"));
        cBanco.setMinWidth(100);

        TableColumn<ContaBancaria, String> cAgencia = new TableColumn<>("Agência");
        cAgencia.setCellValueFactory(new PropertyValueFactory<>("agencia"));

        TableColumn<ContaBancaria, String> cNumero = new TableColumn<>("Número");
        cNumero.setCellValueFactory(new PropertyValueFactory<>("numero"));

        TableColumn<ContaBancaria, Double> cSaldo = new TableColumn<>("Saldo atual");
        cSaldo.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(
                contaDAO.saldoAtual(d.getValue().getNome())));
        cSaldo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Formatador.moeda(item));
                setStyle(empty ? "" : (item != null && item >= 0
                        ? "-fx-text-fill:#0E7A3C; -fx-font-weight:bold;"
                        : "-fx-text-fill:#C8102E; -fx-font-weight:bold;"));
            }
        });
        cSaldo.setMinWidth(110);

        tabelaContas.getColumns().add(cNome);
        tabelaContas.getColumns().add(cBanco);
        tabelaContas.getColumns().add(cAgencia);
        tabelaContas.getColumns().add(cNumero);
        tabelaContas.getColumns().add(cSaldo);
        tabelaContas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tabelaContas, Priority.ALWAYS);

        tabelaContas.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> {
            if (depois != null) {
                preencherFormDepoisSelecao(depois);
                carregarMovimentacoes(depois);
            }
        });

        card.getChildren().addAll(titulo, tabelaContas);
        atualizarListaContas();
        return card;
    }

    private void preencherFormDepoisSelecao(ContaBancaria c) {
        idContaEmEdicao = c.getId();
        nomeField.setText(c.getNome());
        bancoField.setText(c.getBanco());
        agenciaField.setText(c.getAgencia());
        numeroField.setText(c.getNumero());
        saldoInicialField.setText(String.format("%.2f", c.getSaldoInicial()).replace(".", ","));
        lblContaMov.setText("Conta: " + c.getNome() + " · saldo " + Formatador.moeda(contaDAO.saldoAtual(c.getNome())));
    }

    private void novoConta() {
        idContaEmEdicao = -1;
        nomeField.clear();
        bancoField.clear();
        agenciaField.clear();
        numeroField.clear();
        saldoInicialField.clear();
        tabelaContas.getSelectionModel().clearSelection();
    }

    private void salvarConta() {
        String nome = nomeField.getText() == null ? "" : nomeField.getText().trim();
        if (nome.isBlank()) {
            alerta("Informe o nome da conta.", Alert.AlertType.WARNING);
            return;
        }
        double saldoInicial = 0;
        try {
            if (saldoInicialField.getText() != null && !saldoInicialField.getText().isBlank()) {
                saldoInicial = Formatador.parseValor(saldoInicialField.getText());
            }
        } catch (NumberFormatException ex) {
            alerta("Saldo inicial inválido. Use o formato 123,45.", Alert.AlertType.ERROR);
            return;
        }

        ContaBancaria conta = new ContaBancaria(nome, bancoField.getText().trim(), agenciaField.getText().trim(),
                numeroField.getText().trim(), saldoInicial);
        if (idContaEmEdicao > 0) {
            conta.setId(idContaEmEdicao);
            contaDAO.update(conta);
            alerta("Conta atualizada!", Alert.AlertType.INFORMATION);
        } else {
            contaDAO.insert(conta);
            alerta("Conta criada!", Alert.AlertType.INFORMATION);
        }
        atualizarListaContas();
        novoConta();
        atualizarSobre();
        onDadosAlterados.run();
    }

    private void excluirConta() {
        ContaBancaria c = tabelaContas.getSelectionModel().getSelectedItem();
        if (c == null) {
            alerta("Selecione uma conta na lista.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir a conta \"" + c.getNome() + "\"? As movimentações manuais dela também serão apagadas.");
        confirm.setHeaderText("Confirmar exclusão");
        confirm.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(b -> {
                    contaDAO.delete(c.getId());
                    atualizarListaContas();
                    novoConta();
                    tabelaMov.getItems().clear();
                    atualizarSobre();
                    onDadosAlterados.run();
                });
    }

    private VBox criarFormMovimentacao() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(330);

        Label titulo = new Label("Ajuste de saldo");
        titulo.getStyleClass().add("form-title");
        lblContaMov.getStyleClass().add("form-hint");

        Label ajudaAjuste = new Label("Use esta opção somente quando o saldo mostrado pelo sistema for "
                + "diferente do saldo real da conta. Para registrar entradas e saídas normais, use Movimentações.");
        ajudaAjuste.getStyleClass().add("form-hint");
        ajudaAjuste.setWrapText(true);
        ajudaAjuste.setMaxWidth(290);

        tipoMovCombo.setMaxWidth(Double.MAX_VALUE);
        descMovField.setPromptText("Motivo (Ex.: depósito, saque, transferência)");
        valorMovField.setPromptText("0,00");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.add(new Label("Tipo de ajuste"), 0, 0);
        grid.add(tipoMovCombo, 0, 1);
        grid.add(new Label("Descrição"), 0, 2);
        grid.add(descMovField, 0, 3);
        grid.add(new Label("Valor"), 0, 4);
        grid.add(valorMovField, 0, 5);

        Button registrar = new Button("Registrar ajuste");
        registrar.getStyleClass().addAll("btn", "btn-yellow");
        registrar.setOnAction(e -> registrarMovimentacao());

        card.getChildren().addAll(titulo, lblContaMov, grid, ajudaAjuste, registrar);
        return card;
    }

    private VBox criarListaMovimentacoes() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label titulo = new Label("Movimentações manuais da conta");
        titulo.getStyleClass().add("card-label");

        TableColumn<MovimentacaoSaldo, String> cData = new TableColumn<>("Data");
        cData.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(
                Formatador.data(d.getValue().getData())));

        TableColumn<MovimentacaoSaldo, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        cTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null) {
                    setText("");
                    setStyle("");
                    return;
                }
                setText("ENTRADA".equals(tipo) ? "Entrada" : "Saída");
                setStyle("ENTRADA".equals(tipo)
                        ? "-fx-text-fill:#0E7A3C; -fx-font-weight:bold;"
                        : "-fx-text-fill:#C8102E; -fx-font-weight:bold;");
            }
        });

        TableColumn<MovimentacaoSaldo, String> cDesc = new TableColumn<>("Descrição");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));

        TableColumn<MovimentacaoSaldo, Double> cValor = new TableColumn<>("Valor");
        cValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        cValor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Formatador.moeda(item));
            }
        });
        cValor.setMinWidth(110);

        tabelaMov.getColumns().add(cData);
        tabelaMov.getColumns().add(cTipo);
        tabelaMov.getColumns().add(cDesc);
        tabelaMov.getColumns().add(cValor);
        tabelaMov.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tabelaMov, Priority.ALWAYS);

        card.getChildren().addAll(titulo, tabelaMov);
        return card;
    }

    private void registrarMovimentacao() {
        ContaBancaria conta = tabelaContas.getSelectionModel().getSelectedItem();
        if (conta == null) {
            alerta("Selecione uma conta na lista.", Alert.AlertType.WARNING);
            return;
        }
        String descricao = descMovField.getText() == null || descMovField.getText().isBlank()
                ? "Ajuste manual de saldo"
                : descMovField.getText().trim();
        double valor;
        try {
            valor = Formatador.parseValor(valorMovField.getText());
        } catch (NumberFormatException ex) {
            alerta("Valor inválido. Use o formato 123,45.", Alert.AlertType.ERROR);
            return;
        }
        if (valor <= 0) {
            alerta("O valor deve ser maior que zero.", Alert.AlertType.WARNING);
            return;
        }

        String valorCombo = tipoMovCombo.getValue();
        String tipo;
        if ("Saída (diminui o saldo)".equals(valorCombo)) {
            tipo = MovimentacaoSaldo.SAIDA;
        } else {
            tipo = MovimentacaoSaldo.ENTRADA;
        }
        movimentacaoDAO.insert(new MovimentacaoSaldo(conta.getId(), tipo, descricao, valor, LocalDate.now()));

        descMovField.clear();
        valorMovField.clear();
        atualizarListaContas();
        carregarMovimentacoes(conta);
        preencherFormDepoisSelecao(conta);
        alerta("Ajuste registrado! O saldo da conta foi atualizado.", Alert.AlertType.INFORMATION);
        onDadosAlterados.run();
    }

    private void carregarMovimentacoes(ContaBancaria conta) {
        List<MovimentacaoSaldo> lista = movimentacaoDAO.porConta(conta.getId());
        tabelaMov.getItems().setAll(lista);
        tabelaMov.refresh();
    }

    private void atualizarListaContas() {
        List<ContaBancaria> contas = contaDAO.findAll();
        tabelaContas.getItems().setAll(contas);
        tabelaContas.refresh();
        if (!contas.isEmpty() && tabelaContas.getSelectionModel().getSelectedItem() == null) {
            tabelaContas.getSelectionModel().select(0);
        }
    }

    private void atualizarSobre() {
        if (tabelaContas.getSelectionModel().getSelectedItem() == null && !tabelaContas.getItems().isEmpty()) {
            tabelaContas.getSelectionModel().select(0);
        }
    }

    private void alerta(String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}