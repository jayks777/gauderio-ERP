package br.com.gauderio.ui;

import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.Formatador;
import br.com.gauderio.util.UiUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Movimentações: registrar entradas e saídas (agora ou futuras) e consultar o histórico.
 * Substitui o antigo "Financeiro · Receitas e Despesas" com linguagem simples.
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
            FXCollections.observableArrayList("Todas", "Entrada", "Saída"));
    private final ComboBox<String> filtroPeriodo = new ComboBox<>(
            FXCollections.observableArrayList("Todos", "Este mês", "Mês anterior", "Últimos 12 meses"));

    private final Button btnMarcar = new Button("Marcar como paga");
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

    /** Abre a aba de registro já com o tipo escolhido (Entrada = true). */
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
        Button btnEntrada = new Button("+ Registrar entrada");
        btnEntrada.getStyleClass().addAll("btn", "btn-green");
        btnEntrada.setOnAction(e -> mostrarRegistro(true));

        Button btnSaida = new Button("- Registrar saída");
        btnSaida.getStyleClass().addAll("btn", "btn-red");
        btnSaida.setOnAction(e -> mostrarRegistro(false));

        return UiUtil.cabecalhoComAcoes(
                "Movimentações",
                "Registre todo dinheiro que entra ou sai da empresa.",
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

        Button salvar = new Button("Salvar");
        salvar.getStyleClass().addAll("btn", "btn-green");
        salvar.setOnAction(e -> salvar());

        Button limpar = new Button("Limpar");
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

        HBox filtros = new HBox(8, new Label("Tipo:"), filtroTipo, new Label("Período:"), filtroPeriodo);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.getStyleClass().add("filter-bar");

        tabela.getColumns().addAll(
                colunaData(),
                colunaTipo(),
                colunaTexto("Descrição", "descricao", 220),
                colunaTexto("Categoria", "categoria", 140),
                colunaTexto("Conta", "conta", 130),
                colunaValor(),
                colunaVencimento(),
                colunaSituacao(),
                colunaRecorrente());
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        btnMarcar.getStyleClass().addAll("btn", "btn-green");
        btnMarcar.setOnAction(e -> marcarSelecionado());

        Button btnEditar = new Button("Editar");
        btnEditar.getStyleClass().addAll("btn", "btn-outline");
        btnEditar.setOnAction(e -> editarSelecionado());

        Button btnExcluir = new Button("Excluir");
        btnExcluir.getStyleClass().addAll("btn", "btn-outline-red");
        btnExcluir.setOnAction(e -> excluirSelecionado());

        HBox acoes = new HBox(10, btnMarcar, btnEditar, btnExcluir);
        acoes.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(titulo, filtros, tabela, acoes);
        return card;
    }

    // =========================================================
    // COLUNAS DA TABELA DE HISTÓRICO
    // =========================================================
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
                    setText(item);
                    setStyle("-fx-text-fill:" + ("Entrada".equals(item) ? "#0E7A3C" : "#C8102E")
                            + "; -fx-font-weight:bold;");
                }
            }
        });
        col.setMinWidth(80);
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
                    boolean entrada = t != null && Transacao.TIPO_RECEITA.equals(t.getTipo());
                    setStyle("-fx-text-fill:" + (entrada ? "#0E7A3C" : "#C8102E") + ";");
                }
            }
        });
        col.setMinWidth(110);
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
                    default -> "#9A6B00";
                };
                setStyle("-fx-text-fill:" + cor + "; -fx-font-weight:bold;");
            }
        });
        col.setMinWidth(95);
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
            btnMarcar.setText("Marcar como paga");
            return;
        }
        boolean receita = Transacao.TIPO_RECEITA.equals(t.getTipo());
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
            alerta("Essa movimentação já foi " + (Transacao.TIPO_RECEITA.equals(t.getTipo()) ? "recebida." : "paga."),
                    Alert.AlertType.WARNING);
            return;
        }
        dialogDataPagamento(t, dataPagamento -> {
            transacaoDAO.marcarPaga(t.getId(), dataPagamento);
            boolean receita = Transacao.TIPO_RECEITA.equals(t.getTipo());
            alerta(receita ? "Conta marcada como recebida." : "Conta marcada como paga.",
                    Alert.AlertType.INFORMATION);
            aplicarFiltros();
            onDadosAlterados.run();
        });
    }

    /** Diálogo que pede a data em que a conta foi paga/recebida antes de concluir. */
    private void dialogDataPagamento(Transacao t, java.util.function.Consumer<LocalDate> aoConfirmar) {
        boolean receita = Transacao.TIPO_RECEITA.equals(t.getTipo());
        Dialog<LocalDate> diag = new Dialog<>();
        diag.setTitle(receita ? "Marcar como recebida" : "Marcar como paga");
        diag.setHeaderText("Confirmar o pagamento de \"" + t.getDescricao() + "\"?");

        DatePicker data = new DatePicker(LocalDate.now());
        Label ajuda = new Label("Informe a data em que o dinheiro " + (receita ? "entrou" : "saiu")
                + ". O saldo da conta será atualizado.");
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

    // Edição, exclusão, filtros e avisos
    private void editarSelecionado() {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione uma movimentação na lista.", Alert.AlertType.WARNING);
            return;
        }
        idEmEdicao = t.getId();
        lblTituloForm.setText("Editando movimentação");
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
        confirm.setHeaderText(null);
        confirm.setContentText("Excluir \"" + t.getDescricao() + "\"? Esta ação não pode ser desfeita.");
        ButtonType excluirBtn = new ButtonType("Excluir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(excluirBtn, cancelarBtn);
        confirm.showAndWait().filter(b -> b == excluirBtn).ifPresent(b -> {
            transacaoDAO.delete(t.getId());
            alerta("Movimentação excluída.", Alert.AlertType.INFORMATION);
            aplicarFiltros();
            onDadosAlterados.run();
        });
    }

    private void aplicarFiltros() {
        List<Transacao> todos = transacaoDAO.findAll();
        String tipo = filtroTipo.getValue();
        String periodo = filtroPeriodo.getValue();

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
        for (Transacao t : todos) {
            if (!"Todas".equals(tipo)) {
                String tipoFiltro = "Entrada".equals(tipo) ? Transacao.TIPO_RECEITA : Transacao.TIPO_DESPESA;
                if (!tipoFiltro.equals(t.getTipo())) {
                    continue;
                }
            }
            if (inicio != null && (t.getData().isBefore(inicio) || t.getData().isAfter(fim))) {
                continue;
            }
            resultado.add(t);
        }
        tabela.getItems().setAll(resultado);
        tabela.refresh();
    }

    private void alerta(String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
