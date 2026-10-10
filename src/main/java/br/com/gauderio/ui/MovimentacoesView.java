package br.com.gauderio.ui;

import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.ExportadorCsv;
import br.com.gauderio.util.Formatador;
import br.com.gauderio.util.IconeUtil;
import br.com.gauderio.util.UiUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Movimentações: registrar entradas e saídas (agora ou futuras), pesquisar e consultar o histórico completo.
 */
public class MovimentacoesView extends BorderPane implements Refreshable {

    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final Runnable onDadosAlterados;

    private final FormularioMovimentacao form = new FormularioMovimentacao();
    private final Label lblTituloForm = new Label("Nova movimentação");
    private final TableView<Transacao> tabela = new TableView<>();
    private final TabPane abas = new TabPane();
    private final Tab abaRegistrar = new Tab("Registrar");
    private final Tab abaHistorico = new Tab("Histórico");

    private final ComboBox<String> filtroTipo = new ComboBox<>(
            FXCollections.observableArrayList("Todas", "Entrada (Receita)", "Saída (Despesa)"));
    private final ComboBox<String> filtroPeriodo = new ComboBox<>(
            FXCollections.observableArrayList("Todos", "Este mês", "Mês anterior", "Últimos 12 meses"));
    private final TextField campoBusca = new TextField();

    // Labels do resumo no rodapé da tabela de histórico
    private final Label lblQtdLancamentos = new Label("0 lançamentos");
    private final Label lblTotalEntradas = new Label("Entradas: R$ 0,00");
    private final Label lblTotalSaidas = new Label("Saídas: R$ 0,00");
    private final Label lblSaldoPeriodo = new Label("Saldo: R$ 0,00");

    private final Button btnMarcar = new Button("Marcar como concluída");
    private int idEmEdicao = -1;

    public MovimentacoesView() {
        this(() -> {
        });
    }

    public MovimentacoesView(Runnable onDadosAlterados) {
        this.onDadosAlterados = onDadosAlterados == null ? () -> {
        } : onDadosAlterados;
        setPadding(new Insets(24));
        getStyleClass().add("content");

        setTop(criarCabecalho());
        setCenter(criarAbas());

        filtroTipo.setValue("Todas");
        filtroPeriodo.setValue("Todos");
        filtroTipo.valueProperty().addListener((obs, a, b) -> aplicarFiltros());
        filtroPeriodo.valueProperty().addListener((obs, a, b) -> aplicarFiltros());
        campoBusca.textProperty().addListener((obs, a, b) -> aplicarFiltros());

        btnMarcar.setDisable(true);
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> atualizarBotaoMarcar());
        abas.getSelectionModel().select(abaRegistrar);
        aplicarFiltros();
    }

    @Override
    public void refresh() {
        form.recarregarContas();
        aplicarFiltros();
    }

    /** Abre a aba de registro já com o tipo escolhido (Entrada = true, Saída = false). */
    public void mostrarRegistro(boolean entrada) {
        idEmEdicao = -1;
        lblTituloForm.setText("Nova movimentação");
        form.limpar();
        form.setTipoPreferido(entrada);
        abas.getSelectionModel().select(abaRegistrar);
    }

    /** Abre a aba de histórico. */
    public void mostrarHistorico() {
        abas.getSelectionModel().select(abaHistorico);
        aplicarFiltros();
    }

    // =========================================================
    // CABEÇALHO
    // =========================================================
    private BorderPane criarCabecalho() {
        Button btnEntrada = new Button("+ Registrar entrada", IconeUtil.iconeBotao(IconeUtil.PATH_ENTRADA, "#ffffff"));
        btnEntrada.getStyleClass().addAll("btn", "btn-green");
        btnEntrada.setOnAction(e -> mostrarRegistro(true));

        Button btnSaida = new Button("- Registrar saída", IconeUtil.iconeBotao(IconeUtil.PATH_SAIDA, "#ffffff"));
        btnSaida.getStyleClass().addAll("btn", "btn-red");
        btnSaida.setOnAction(e -> mostrarRegistro(false));

        return UiUtil.cabecalhoComAcoes(
                "Movimentações",
                "Registre todo dinheiro que entra ou sai da empresa e consulte seu histórico.",
                btnEntrada, btnSaida);
    }

    // =========================================================
    // ABAS: REGISTRAR E HISTÓRICO
    // =========================================================
    private TabPane criarAbas() {
        abas.getStyleClass().add("conta-tabs");
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        abas.getTabs().addAll(abaRegistrar, abaHistorico);
        abaRegistrar.setContent(criarAbaRegistrar());
        abaHistorico.setContent(criarAbaHistorico());
        VBox.setVgrow(abas, Priority.ALWAYS);
        return abas;
    }

    private VBox criarAbaRegistrar() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        lblTituloForm.getStyleClass().add("form-title");

        Button salvar = new Button("Salvar", IconeUtil.iconeBotao(IconeUtil.PATH_SALVAR, "#ffffff"));
        salvar.getStyleClass().addAll("btn", "btn-green");
        salvar.setOnAction(e -> salvar());

        Button limpar = new Button("Limpar", IconeUtil.iconeBotao(IconeUtil.PATH_LIMPAR, "#41505c"));
        limpar.getStyleClass().addAll("btn", "btn-outline");
        limpar.setOnAction(e -> {
            idEmEdicao = -1;
            lblTituloForm.setText("Nova movimentação");
            form.limpar();
        });

        HBox acoes = new HBox(10, salvar, limpar);
        acoes.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(lblTituloForm, form, acoes);
        return card;
    }

    private VBox criarAbaHistorico() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label titulo = new Label("Histórico de movimentações");
        titulo.getStyleClass().add("card-label");

        campoBusca.setPromptText("Buscar descrição, categoria ou conta...");
        campoBusca.setPrefWidth(260);

        Button btnExportar = new Button("Exportar (CSV)", IconeUtil.iconeBotao(IconeUtil.PATH_EXPORTAR, "#41505c"));
        btnExportar.getStyleClass().addAll("btn", "btn-outline");
        btnExportar.setOnAction(e -> exportarCsv());

        Region espacoFiltro = new Region();
        HBox.setHgrow(espacoFiltro, Priority.ALWAYS);

        HBox filtros = new HBox(10,
                new Label("Buscar:"), campoBusca,
                new Label("Tipo:"), filtroTipo,
                new Label("Período:"), filtroPeriodo,
                espacoFiltro,
                btnExportar);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.getStyleClass().add("filter-bar");

        configurarColunasTabela();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        btnMarcar.getStyleClass().addAll("btn", "btn-green");
        btnMarcar.setGraphic(IconeUtil.iconeBotao(IconeUtil.PATH_CHECK_CIRCLE, "#ffffff"));
        btnMarcar.setOnAction(e -> marcarSelecionado());

        Button btnEditar = new Button("Editar", IconeUtil.iconeBotao(IconeUtil.PATH_EDITAR, "#41505c"));
        btnEditar.getStyleClass().addAll("btn", "btn-outline");
        btnEditar.setOnAction(e -> editarSelecionado());

        Button btnExcluir = new Button("Excluir", IconeUtil.iconeBotao(IconeUtil.PATH_EXCLUIR, "#C8102E"));
        btnExcluir.getStyleClass().addAll("btn", "btn-outline-red");
        btnExcluir.setOnAction(e -> excluirSelecionado());

        HBox acoes = new HBox(10, btnMarcar, btnEditar, btnExcluir);
        acoes.setAlignment(Pos.CENTER_LEFT);

        HBox barraTotais = criarBarraTotais();

        card.getChildren().addAll(titulo, filtros, tabela, barraTotais, acoes);
        return card;
    }

    private HBox criarBarraTotais() {
        lblQtdLancamentos.getStyleClass().add("summary-count");
        lblTotalEntradas.getStyleClass().add("summary-income");
        lblTotalSaidas.getStyleClass().add("summary-expense");
        lblSaldoPeriodo.getStyleClass().add("summary-balance");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        HBox valores = new HBox(18, lblTotalEntradas, lblTotalSaidas, lblSaldoPeriodo);
        valores.setAlignment(Pos.CENTER_RIGHT);

        HBox barra = new HBox(12, lblQtdLancamentos, espaco, valores);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("history-summary-bar");
        barra.setPadding(new Insets(8, 12, 8, 12));
        return barra;
    }

    // =========================================================
    // COLUNAS DA TABELA DE HISTÓRICO
    // =========================================================
    private void configurarColunasTabela() {
        tabela.getColumns().clear();
        tabela.getColumns().addAll(List.of(
                colunaData(),
                colunaTipo(),
                colunaTexto("Descrição", "descricao", 220),
                colunaTexto("Categoria", "categoria", 140),
                colunaTexto("Conta", "conta", 130),
                colunaValor(),
                colunaVencimento(),
                colunaSituacao(),
                colunaRecorrente()));
    }

    private TableColumn<Transacao, LocalDate> colunaData() {
        TableColumn<Transacao, LocalDate> col = new TableColumn<>("Data");
        col.setCellValueFactory(new PropertyValueFactory<>("data"));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Formatador.data(item));
            }
        });
        col.setMinWidth(95);
        return col;
    }

    private TableColumn<Transacao, String> colunaTipo() {
        TableColumn<Transacao, String> col = new TableColumn<>("Tipo");
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTipoTexto()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    boolean entrada = "Entrada".equalsIgnoreCase(item);
                    setText(entrada ? "▲ Entrada" : "▼ Saída");
                    setStyle("-fx-text-fill:" + (entrada ? "#0E7A3C" : "#C8102E")
                            + "; -fx-font-weight:bold;");
                }
            }
        });
        col.setMinWidth(95);
        return col;
    }

    private TableColumn<Transacao, String> colunaTexto(String rotulo, String prop, double largura) {
        TableColumn<Transacao, String> col = new TableColumn<>(rotulo);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setMinWidth(largura);
        return col;
    }

    private TableColumn<Transacao, Double> colunaValor() {
        TableColumn<Transacao, Double> col = new TableColumn<>("Valor");
        col.setCellValueFactory(d -> new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getValor()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                Transacao t = getTableRow() == null ? null : getTableRow().getItem();
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(Formatador.moeda(item));
                    boolean entrada = t != null && t.isReceita();
                    setStyle("-fx-text-fill:" + (entrada ? "#0E7A3C" : "#C8102E")
                            + "; -fx-font-weight:bold;");
                }
            }
        });
        col.setMinWidth(115);
        return col;
    }

    private TableColumn<Transacao, String> colunaVencimento() {
        TableColumn<Transacao, String> col = new TableColumn<>("Vencimento");
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getVencimento() == null ? "—" : Formatador.data(d.getValue().getVencimento())));
        col.setMinWidth(95);
        return col;
    }

    private TableColumn<Transacao, String> colunaSituacao() {
        TableColumn<Transacao, String> col = new TableColumn<>("Situação");
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStatusTexto()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                    return;
                }
                setText(item);
                String cor = switch (item) {
                    case "Paga", "Recebida" -> "#0E7A3C";
                    case "Vencida" -> "#C8102E";
                    default -> "#B7791F";
                };
                String bg = switch (item) {
                    case "Paga", "Recebida" -> "#eafaf1";
                    case "Vencida" -> "#fde8e8";
                    default -> "#fef5e7";
                };
                setStyle("-fx-text-fill:" + cor
                        + "; -fx-background-color:" + bg
                        + "; -fx-background-radius: 12px; -fx-padding: 3px 8px; -fx-font-weight:bold; -fx-alignment: center;");
            }
        });
        col.setMinWidth(100);
        return col;
    }

    private TableColumn<Transacao, String> colunaRecorrente() {
        TableColumn<Transacao, String> col = new TableColumn<>("Recorrente");
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().isRecorrente() ? "Sim" : "—"));
        col.setMinWidth(80);
        return col;
    }

    // =========================================================
    // AÇÕES
    // =========================================================
    private void salvar() {
        Optional<String> erro = form.validar();
        if (erro.isPresent()) {
            alerta(erro.get(), Alert.AlertType.WARNING);
            return;
        }
        if (idEmEdicao > 0) {
            Transacao t = form.construirBase();
            t.setId(idEmEdicao);
            transacaoDAO.update(t);
            alerta("Movimentação atualizada com sucesso.", Alert.AlertType.INFORMATION);
            idEmEdicao = -1;
            lblTituloForm.setText("Nova movimentação");
            form.limpar();
        } else {
            List<Transacao> lista = form.gerarLancamentos();
            if (lista.size() > 1) {
                transacaoDAO.insertAll(lista);
                alerta(lista.size() + " lançamentos criados com sucesso.", Alert.AlertType.INFORMATION);
            } else {
                transacaoDAO.insert(lista.get(0));
                boolean entrada = Transacao.TIPO_RECEITA.equals(lista.get(0).getTipo());
                alerta(entrada ? "Entrada registrada com sucesso." : "Saída registrada com sucesso.",
                        Alert.AlertType.INFORMATION);
            }
            form.limpar();
        }
        abas.getSelectionModel().select(abaHistorico);
        aplicarFiltros();
        onDadosAlterados.run();
    }

    private void atualizarBotaoMarcar() {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            btnMarcar.setDisable(true);
            btnMarcar.setText("Marcar como concluída");
            return;
        }
        boolean receita = t.isReceita();
        btnMarcar.setText(receita ? "Marcar como recebida" : "Marcar como paga");
        btnMarcar.setDisable(Transacao.STATUS_PAGO.equals(t.getStatus()));
    }

    private void marcarSelecionado() {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione uma movimentação na lista.", Alert.AlertType.WARNING);
            return;
        }
        if (Transacao.STATUS_PAGO.equals(t.getStatus())) {
            alerta("Essa movimentação já foi " + (t.isReceita() ? "recebida." : "paga."),
                    Alert.AlertType.WARNING);
            return;
        }
        dialogDataPagamento(t, dataPagamento -> {
            transacaoDAO.marcarPaga(t.getId(), dataPagamento);
            alerta(t.isReceita() ? "Conta marcada como recebida." : "Conta marcada como paga.",
                    Alert.AlertType.INFORMATION);
            aplicarFiltros();
            onDadosAlterados.run();
        });
    }

    private void dialogDataPagamento(Transacao t, java.util.function.Consumer<LocalDate> aoConfirmar) {
        boolean receita = t.isReceita();
        Dialog<LocalDate> diag = new Dialog<>();
        diag.setTitle(receita ? "Marcar como recebida" : "Marcar como paga");
        diag.setHeaderText("Confirmar a liquidação de \"" + t.getDescricao() + "\"?");

        DatePicker data = new DatePicker(LocalDate.now());
        Label ajuda = new Label("Informe a data em que o dinheiro " + (receita ? "entrou" : "saiu")
                + ". O saldo da conta bancária será atualizado automaticamente.");
        ajuda.getStyleClass().add("form-hint");
        ajuda.setWrapText(true);
        VBox conteudo = new VBox(10, new Label("Data do pagamento:"), data, ajuda);
        conteudo.setPadding(new Insets(10));
        diag.getDialogPane().setContent(conteudo);

        ButtonType confirmar = new ButtonType("Confirmar", ButtonBar.ButtonData.OK_DONE);
        diag.getDialogPane().getButtonTypes().addAll(confirmar, ButtonType.CANCEL);
        diag.setResultConverter(b -> b == confirmar ? data.getValue() : null);

        diag.showAndWait().ifPresent(dataPagamento -> {
            if (dataPagamento != null) {
                aoConfirmar.accept(dataPagamento);
            }
        });
    }

    private void editarSelecionado() {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione uma movimentação na lista.", Alert.AlertType.WARNING);
            return;
        }
        idEmEdicao = t.getId();
        lblTituloForm.setText("Editando movimentação #" + t.getId());
        form.limpar();
        form.preencherParaEdicao(t);
        abas.getSelectionModel().select(abaRegistrar);
    }

    private void excluirSelecionado() {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione uma movimentação na lista.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir movimentação");
        confirm.setHeaderText(null);
        confirm.setContentText("Excluir definitivamente \"" + t.getDescricao() + "\"? Esta ação não pode ser desfeita.");
        ButtonType excluirBtn = new ButtonType("Sim, excluir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(excluirBtn, cancelarBtn);
        confirm.showAndWait().filter(b -> b == excluirBtn).ifPresent(b -> {
            transacaoDAO.delete(t.getId());
            alerta("Movimentação excluída.", Alert.AlertType.INFORMATION);
            aplicarFiltros();
            onDadosAlterados.run();
        });
    }

    private void exportarCsv() {
        List<Transacao> lista = tabela.getItems();
        if (lista == null || lista.isEmpty()) {
            alerta("Não há movimentações para exportar na lista atual.", Alert.AlertType.WARNING);
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar movimentações em CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Planilha CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName("movimentacoes-" + LocalDate.now() + ".csv");
        File arquivo = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (arquivo != null) {
            try {
                ExportadorCsv.exportarTransacoes(lista, arquivo);
                alerta("Arquivo CSV exportado com sucesso em:\n" + arquivo.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                alerta("Erro ao exportar arquivo: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void aplicarFiltros() {
        List<Transacao> todos = transacaoDAO.findAll();
        String tipo = filtroTipo.getValue();
        String periodo = filtroPeriodo.getValue();
        String termo = campoBusca.getText() == null ? "" : campoBusca.getText().trim().toLowerCase();

        LocalDate hoje = LocalDate.now();
        LocalDate inicio = null;
        LocalDate fim = null;

        if ("Este mês".equals(periodo)) {
            inicio = hoje.withDayOfMonth(1);
            fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
        } else if ("Mês anterior".equals(periodo)) {
            inicio = hoje.withDayOfMonth(1).minusMonths(1);
            fim = inicio.withDayOfMonth(inicio.lengthOfMonth());
        } else if ("Últimos 12 meses".equals(periodo)) {
            inicio = hoje.minusMonths(11).withDayOfMonth(1);
            fim = hoje;
        }

        List<Transacao> resultado = new ArrayList<>();
        double totalEntradas = 0;
        double totalSaidas = 0;

        for (Transacao t : todos) {
            if (tipo != null && !"Todas".equals(tipo)) {
                String tipoFiltro = tipo.startsWith("Entrada") ? Transacao.TIPO_RECEITA : Transacao.TIPO_DESPESA;
                if (!tipoFiltro.equals(t.getTipo())) {
                    continue;
                }
            }
            if (inicio != null && (t.getData().isBefore(inicio) || t.getData().isAfter(fim))) {
                continue;
            }
            if (!termo.isEmpty()) {
                String desc = t.getDescricao() == null ? "" : t.getDescricao().toLowerCase();
                String cat = t.getCategoria() == null ? "" : t.getCategoria().toLowerCase();
                String conta = t.getConta() == null ? "" : t.getConta().toLowerCase();
                if (!desc.contains(termo) && !cat.contains(termo) && !conta.contains(termo)) {
                    continue;
                }
            }
            resultado.add(t);
            if (t.isReceita()) {
                totalEntradas += t.getValor();
            } else {
                totalSaidas += t.getValor();
            }
        }

        tabela.getItems().setAll(resultado);
        tabela.refresh();

        // Atualiza a barra de resumo de totais
        lblQtdLancamentos.setText(resultado.size() + (resultado.size() == 1 ? " lançamento" : " lançamentos"));
        lblTotalEntradas.setText("Entradas: " + Formatador.moeda(totalEntradas));
        lblTotalSaidas.setText("Saídas: " + Formatador.moeda(totalSaidas));
        double saldo = totalEntradas - totalSaidas;
        lblSaldoPeriodo.setText("Saldo: " + Formatador.moeda(saldo));
        lblSaldoPeriodo.setStyle("-fx-text-fill:" + (saldo >= 0 ? "#0E7A3C" : "#C8102E") + "; -fx-font-weight: bold;");
    }

    private void alerta(String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
