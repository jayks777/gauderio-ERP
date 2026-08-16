package br.com.gauderio.ui;

import br.com.gauderio.dao.CategoriaDAO;
import br.com.gauderio.dao.ContaDAO;
import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.model.Categoria;
import br.com.gauderio.model.ContaBancaria;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.Formatador;
import br.com.gauderio.util.UiUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Central de boletos: despesas (boletos) a pagar e receitas (vendas) a receber. */
public class BoletosView extends BorderPane implements Refreshable {

    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final ContaDAO contaDAO = new ContaDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final Map<String, TableView<Transacao>> tabelasPendentes = new HashMap<>();

    private final Label lblAPagar = new Label();
    private final Label lblAReceber = new Label();
    private final Label lblVencidos = new Label();

    public BoletosView() {
        setPadding(new Insets(24));
        getStyleClass().add("content");

        VBox cabecalho = UiUtil.cabecalho("Central de boletos · a pagar e a receber",
                "Acompanhe boletos de despesas e vendas futuras. Marque como pago/recebido quando o dinheiro transacionar.");

        HBox resumo = criarResumo();

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(
                criarTab("Boletos a pagar", Transacao.TIPO_DESPESA),
                criarTab("Vendas / a receber", Transacao.TIPO_RECEITA));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        VBox area = new VBox(16, cabecalho, resumo, tabs);
        area.setPadding(new Insets(0, 0, 0, 0));
        setCenter(area);

        refreshResumo();
    }

    @Override
    public void refresh() {
        tabelasPendentes.forEach((tipo, tabela) -> popularPendentes(tabela, tipo));
        refreshResumo();
    }

    private HBox criarResumo() {
        return new HBox(14,
                criarCardResumo("Total a pagar", lblAPagar, "red"),
                criarCardResumo("Total a receber", lblAReceber, "green"),
                criarCardResumo("Pendências vencidas", lblVencidos, "yellow"));
    }

    private VBox criarCardResumo(String rotulo, Label lValor, String cor) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(220);
        Region faixa = new Region();
        faixa.setPrefHeight(6);
        faixa.setMaxWidth(Double.MAX_VALUE);
        faixa.getStyleClass().add("accent-" + cor);

        Label l = new Label(rotulo.toUpperCase());
        l.getStyleClass().add("card-label");
        lValor.getStyleClass().add("card-value");

        card.getChildren().addAll(faixa, l, lValor);
        return card;
    }

    private void refreshResumo() {
        lblAPagar.setText(Formatador.moeda(transacaoDAO.somaPendente(Transacao.TIPO_DESPESA)));
        lblAReceber.setText(Formatador.moeda(transacaoDAO.somaPendente(Transacao.TIPO_RECEITA)));
        long vencidos = transacaoDAO.pendentes().stream()
                .filter(t -> t.getDataReferencia().isBefore(LocalDate.now()))
                .count();
        lblVencidos.setText(String.valueOf(vencidos));
    }

    private Tab criarTab(String nome, String tipo) {
        Tab ab = new Tab(nome);

        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        TableView<Transacao> tabela = criarTabelaPendentes(tipo);
        tabelasPendentes.put(tipo, tabela);

        Button btnNovo = new Button(tipo.equals(Transacao.TIPO_DESPESA)
                ? "+ Novo boleto" : "+ Nova venda futura");
        btnNovo.getStyleClass().addAll("btn", tipo.equals(Transacao.TIPO_DESPESA) ? "btn-red" : "btn-green");
        btnNovo.setOnAction(e -> adicionar(tipo, tabela));

        Button btnPago = new Button("Marcar como pago/recebido");
        btnPago.getStyleClass().addAll("btn", "btn-green");
        btnPago.setOnAction(e -> marcarPago(tipo, tabela));

        Button btnExcluir = new Button("Excluir");
        btnExcluir.getStyleClass().addAll("btn", "btn-outline-red");
        btnExcluir.setOnAction(e -> excluir(tipo, tabela));

        HBox acoes = new HBox(10, btnNovo, btnPago, btnExcluir);
        acoes.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(tabela, Priority.ALWAYS);
        card.getChildren().addAll(tabela, acoes);
        ab.setContent(card);

        return ab;
    }

    private TableView<Transacao> criarTabelaPendentes(String tipo) {
        TableView<Transacao> tabela = new TableView<>();

        TableColumn<Transacao, String> cVenc = new TableColumn<>("Vencimento");
        cVenc.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(
                Formatador.data(d.getValue().getDataReferencia())));
        cVenc.setMinWidth(110);

        TableColumn<Transacao, String> cDesc = new TableColumn<>("Descrição");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        cDesc.setMinWidth(240);

        TableColumn<Transacao, String> cCat = new TableColumn<>("Categoria");
        cCat.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        cCat.setMinWidth(150);

        TableColumn<Transacao, String> cConta = new TableColumn<>("Conta");
        cConta.setCellValueFactory(new PropertyValueFactory<>("conta"));
        cConta.setMinWidth(130);

        TableColumn<Transacao, Double> cValor = new TableColumn<>("Valor");
        cValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        cValor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Formatador.moeda(item));
            }
        });
        cValor.setMinWidth(110);

        TableColumn<Transacao, String> cStatus = new TableColumn<>("Status");
        cStatus.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(
                d.getValue().getStatusExibido()));
        cStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("");
                    setStyle("");
                    return;
                }
                setText(status);
                if ("VENCIDO".equals(status)) {
                    setStyle("-fx-text-fill:#C8102E; -fx-font-weight:bold;");
                } else {
                    setStyle("-fx-text-fill:#9A6B00; -fx-font-weight:bold;");
                }
            }
        });
        cStatus.setMinWidth(100);

        tabela.getColumns().add(cVenc);
        tabela.getColumns().add(cDesc);
        tabela.getColumns().add(cCat);
        tabela.getColumns().add(cConta);
        tabela.getColumns().add(cValor);
        tabela.getColumns().add(cStatus);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        popularPendentes(tabela, tipo);
        return tabela;
    }

    private void popularPendentes(TableView<Transacao> tabela, String tipo) {
        tabela.getItems().setAll(transacaoDAO.pendentes().stream()
                .filter(t -> tipo.equals(t.getTipo()))
                .toList());
        tabela.refresh();
    }

    private void adicionar(String tipo, TableView<Transacao> tabela) {
        Dialog<Transacao> diag = new Dialog<>();
        diag.setTitle(tipo.equals(Transacao.TIPO_DESPESA) ? "Novo boleto a pagar" : "Nova venda a receber");
        diag.setHeaderText("Preencha os dados da pendência");

        ButtonType salvar = new ButtonType("Salvar", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        diag.getDialogPane().getButtonTypes().addAll(salvar, ButtonType.CANCEL);

        TextField descricao = new TextField();
        descricao.setPromptText("Ex.: Parcela do veículo / Venda de produtos");
        descricao.setPrefWidth(320);

        TextField valor = new TextField();
        valor.setPromptText("0,00");

        ComboBox<Categoria> categoria = new ComboBox<>();
        categoria.getItems().setAll(categoriaDAO.porTipo(tipo));
        if (!categoria.getItems().isEmpty()) {
            categoria.setValue(categoria.getItems().get(0));
        }

        ComboBox<ContaBancaria> conta = new ComboBox<>();
        conta.getItems().setAll(contaDAO.findAll());

        DatePicker vencimento = new DatePicker(LocalDate.now().plusDays(7));

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.add(new Label("Descrição"), 0, 0);
        grid.add(descricao, 0, 1, 2, 1);
        grid.add(new Label("Valor (R$)"), 0, 2);
        grid.add(valor, 0, 3);
        grid.add(new Label("Vencimento"), 1, 2);
        grid.add(vencimento, 1, 3);
        grid.add(new Label("Categoria"), 0, 4);
        grid.add(categoria, 0, 5);
        grid.add(new Label("Conta"), 1, 4);
        grid.add(conta, 1, 5);
        diag.getDialogPane().setContent(grid);

        diag.setResultConverter(b -> b == salvar ? new Transacao() /* placeholder */ : null);
        // A conversão real acontece no ifPresent abaixo para validação amigável.
        diag.showAndWait().ifPresent(ignorado -> {
            String desc = descricao.getText() == null || descricao.getText().isBlank()
                    ? (tipo.equals(Transacao.TIPO_DESPESA) ? "Boleto (sem descrição)" : "Venda (sem descrição)")
                    : descricao.getText().trim();
            String contaNome = conta.getValue() == null ? "" : conta.getValue().getNome();
            double v;
            try {
                v = Formatador.parseValor(valor.getText());
            } catch (NumberFormatException ex) {
                alerta("Valor inválido. Use o formato 123,45.", Alert.AlertType.ERROR);
                return;
            }
            if (v <= 0) {
                alerta("O valor deve ser maior que zero.", Alert.AlertType.WARNING);
                return;
            }
            Transacao t = new Transacao(
                    tipo,
                    desc,
                    v,
                    categoria.getValue() == null ? "Sem categoria" : categoria.getValue().getNome(),
                    contaNome,
                    LocalDate.now(),
                    vencimento.getValue() == null ? LocalDate.now().plusDays(7) : vencimento.getValue(),
                    Transacao.STATUS_PENDENTE,
                    false,
                    null);
            transacaoDAO.insert(t);
            popularPendentes(tabela, tipo);
            refreshResumo();
            alerta("Pendência registrada na central!", Alert.AlertType.INFORMATION);
        });
    }

    private void marcarPago(String tipo, TableView<Transacao> tabela) {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione uma pendência na lista.", Alert.AlertType.WARNING);
            return;
        }
        transacaoDAO.setStatus(t.getId(), Transacao.STATUS_PAGO);
        popularPendentes(tabela, tipo);
        refreshResumo();
        alerta("Pendência marcada como " + (t.getTipo().equals(Transacao.TIPO_DESPESA) ? "paga" : "recebida")
                + ". O saldo da conta foi atualizado.", Alert.AlertType.INFORMATION);
    }

    private void excluir(String tipo, TableView<Transacao> tabela) {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione uma pendência na lista.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir \"" + t.getDescricao() + "\"?");
        confirm.setHeaderText("Confirmar exclusão");
        confirm.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(b -> {
                    transacaoDAO.delete(t.getId());
                    popularPendentes(tabela, tipo);
                    refreshResumo();
                });
    }

    private void alerta(String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}