package br.com.gauderio.ui;

import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.Formatador;
import br.com.gauderio.util.UiUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Contas a pagar / contas a receber: visões filtradas das movimentações (transacoes).
 * Uma única classe atende os dois casos: receber = true monta a tela de contas a receber.
 * Substitui a antiga "Central de boletos".
 */
public class ContasParaView extends BorderPane implements Refreshable {

    private final boolean receber;
    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final Runnable onDadosAlterados;

    private final Label lblTotal = new Label();
    private final Label lblVencido = new Label();
    private final Label lblProximos = new Label();
    private final ComboBox<String> filtroSituacao = new ComboBox<>();
    private final TableView<Transacao> tabela = new TableView<>();
    private final Button btnMarcar = new Button();
    private final Button btnNovo = new Button();

    private final String verboConcluido; // "Pagas" ou "Recebidas"
    private final String verboAcao;      // "Marcar como paga" ou "Marcar como recebida"
    private final String verboFeedback;  // "Conta marcada como paga." ou "Conta marcada como recebida."

    public ContasParaView(boolean receber, Runnable onDadosAlterados) {
        this.receber = receber;
        this.onDadosAlterados = onDadosAlterados == null ? () -> {
        } : onDadosAlterados;
        this.verboConcluido = receber ? "Recebidas" : "Pagas";
        this.verboAcao = receber ? "Marcar como recebida" : "Marcar como paga";
        this.verboFeedback = receber ? "Conta marcada como recebida." : "Conta marcada como paga.";

        setPadding(new Insets(24));
        getStyleClass().add("content");

        setTop(criarCabecalho());
        setCenter(criarArea());

        filtroSituacao.setItems(javafx.collections.FXCollections.observableArrayList(
                "Pendentes", "Vencidas", verboConcluido, "Todas"));
        filtroSituacao.setValue("Pendentes");
        filtroSituacao.valueProperty().addListener((obs, a, b) -> atualizarTabela());

        tabela.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> atualizarBotaoMarcar());

        atualizarTudo();
    }

    @Override
    public void refresh() {
        atualizarTudo();
    }

    private String tipo() {
        return receber ? Transacao.TIPO_RECEITA : Transacao.TIPO_DESPESA;
    }

    private BorderPane criarCabecalho() {
        String titulo = receber ? "Contas a receber" : "Contas a pagar";
        String subtitulo = receber ? "Veja o que a empresa ainda precisa receber."
                : "Veja o que a empresa ainda precisa pagar.";
        return UiUtil.cabecalhoComAcoes(titulo, subtitulo);
    }

    // =========================================================
    // ÁREA PRINCIPAL (resumo + lista)
    // =========================================================
    private VBox criarArea() {
        VBox area = new VBox(16, criarResumo(), criarFiltroETabela());
        area.setPadding(new Insets(18, 0, 0, 0));
        return area;
    }

    private FlowPane criarResumo() {
        String rotuloTotal = receber ? "Total a receber" : "Total a pagar";
        FlowPane resumo = new FlowPane(14, 14,
                criarCardResumo(rotuloTotal, lblTotal, receber ? "green" : "red"),
                criarCardResumo("Total vencido", lblVencido, "yellow"),
                criarCardResumo("Próximos vencimentos", lblProximos, "blue"));
        resumo.setAlignment(Pos.CENTER_LEFT);
        return resumo;
    }

    private VBox criarCardResumo(String rotulo, Label lValor, String cor) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));
        card.setPrefWidth(230);

        Region faixa = new Region();
        faixa.setPrefHeight(6);
        faixa.setMaxWidth(Double.MAX_VALUE);
        faixa.getStyleClass().add("accent-" + cor);

        Label l = new Label(rotulo.toUpperCase());
        l.getStyleClass().add("card-label");
        l.setPadding(new Insets(10, 12, 0, 12));
        lValor.getStyleClass().add("card-value");
        lValor.setPadding(new Insets(0, 12, 12, 12));

        card.getChildren().addAll(faixa, l, lValor);
        return card;
    }

    private VBox criarFiltroETabela() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        HBox filtros = new HBox(8, new Label("Situação:"), filtroSituacao);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.getStyleClass().add("filter-bar");

        tabela.getColumns().addAll(
                colunaVencimento(),
                colunaTexto("Descrição", "descricao", 240),
                colunaTexto("Categoria", "categoria", 150),
                colunaTexto("Conta", "conta", 130),
                colunaValor(),
                colunaSituacao());
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        btnNovo.setText(receber ? "+ Nova conta a receber" : "+ Nova conta a pagar");
        btnNovo.getStyleClass().addAll("btn", receber ? "btn-green" : "btn-red");
        btnNovo.setOnAction(e -> novaConta());

        btnMarcar.getStyleClass().addAll("btn", "btn-outline");
        btnMarcar.setText(verboAcao);
        btnMarcar.setOnAction(e -> marcarSelecionadas());

        Button btnEditar = new Button("Editar");
        btnEditar.getStyleClass().addAll("btn", "btn-outline");
        btnEditar.setOnAction(e -> editarSelecionada());

        Button btnExcluir = new Button("Excluir");
        btnExcluir.getStyleClass().addAll("btn", "btn-outline-red");
        btnExcluir.setOnAction(e -> excluirSelecionadas());

        HBox acoes = new HBox(10, btnNovo, btnMarcar, btnEditar, btnExcluir);
        acoes.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(filtros, tabela, acoes);
        return card;
    }

    // =========================================================
    // COLUNAS DA TABELA
    // =========================================================
    private TableColumn<Transacao, String> colunaVencimento() {
        TableColumn<Transacao, String> col = new TableColumn<>("Vencimento");
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getVencimento() == null ? "—" : Formatador.data(d.getValue().getVencimento())));
        col.setMinWidth(110);
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
                setText(empty || item == null ? "" : Formatador.moeda(item));
            }
        });
        col.setMinWidth(120);
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
        col.setMinWidth(110);
        return col;
    }

    // =========================================================
    // AÇÕES: NOVA, MARCAR, EDITAR, EXCLUIR
    // =========================================================
    private void atualizarBotaoMarcar() {
        List<Transacao> sel = new ArrayList<>(tabela.getSelectionModel().getSelectedItems());
        if (sel.isEmpty()) {
            btnMarcar.setDisable(true);
            return;
        }
        boolean algumConcluido = sel.stream().anyMatch(t -> Transacao.STATUS_PAGO.equals(t.getStatus()));
        btnMarcar.setDisable(algumConcluido);
    }

    private void novaConta() {
        FormularioMovimentacao form = new FormularioMovimentacao();
        form.setTipoPreferido(receber);
        form.selecionarDepois();

        Dialog<FormularioMovimentacao> diag = new Dialog<>();
        diag.setTitle(receber ? "Nova conta a receber" : "Nova conta a pagar");
        diag.setHeaderText(receber ? "O que a empresa ainda vai receber?"
                : "O que a empresa ainda precisa pagar?");

        ScrollPane rolagem = new ScrollPane(form);
        rolagem.setFitToWidth(true);
        rolagem.setPrefViewportHeight(400);
        diag.getDialogPane().setContent(rolagem);

        ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        diag.getDialogPane().getButtonTypes().addAll(salvar, ButtonType.CANCEL);
        diag.setResultConverter(b -> b == salvar ? form : null);

        diag.showAndWait().ifPresent(f -> {
            Optional<String> erro = f.validar();
            if (erro.isPresent()) {
                alerta(erro.get(), Alert.AlertType.WARNING);
                return;
            }
            List<Transacao> lista = f.gerarLancamentos();
            if (lista.size() > 1) {
                transacaoDAO.insertAll(lista);
                alerta(lista.size() + " lançamentos criados com sucesso.", Alert.AlertType.INFORMATION);
            } else {
                transacaoDAO.insert(lista.get(0));
                alerta(receber ? "Conta a receber registrada com sucesso."
                        : "Conta a pagar registrada com sucesso.", Alert.AlertType.INFORMATION);
            }
            atualizarTudo();
            onDadosAlterados.run();
        });
    }

    // Marcar como paga/recebida com data do pagamento
    private void marcarSelecionadas() {
        List<Transacao> selecionadas = new ArrayList<>(tabela.getSelectionModel().getSelectedItems());
        selecionadas.removeIf(t -> Transacao.STATUS_PAGO.equals(t.getStatus()));
        if (selecionadas.isEmpty()) {
            alerta("Selecione contas pendentes na lista.", Alert.AlertType.WARNING);
            return;
        }
        Dialog<LocalDate> diag = new Dialog<>();
        diag.setTitle(verboAcao);
        diag.setHeaderText(selecionadas.size() == 1
                ? "Confirmar: \"" + selecionadas.get(0).getDescricao() + "\"?  (" + verboAcao + ")"
                : verboAcao + " " + selecionadas.size() + " contas selecionadas?");

        DatePicker data = new DatePicker(LocalDate.now());
        Label ajuda = new Label("Informe a data em que o dinheiro " + (receber ? "entrou" : "saiu")
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
            if (dataPagamento == null) {
                return;
            }
            selecionadas.forEach(t -> transacaoDAO.marcarPaga(t.getId(), dataPagamento));
            alerta(selecionadas.size() == 1 ? verboFeedback
                            : selecionadas.size() + " contas " + (receber ? "recebidas." : "pagas."),
                    Alert.AlertType.INFORMATION);
            atualizarTudo();
            onDadosAlterados.run();
        });
    }

    private void editarSelecionada() {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione uma conta na lista.", Alert.AlertType.WARNING);
            return;
        }
        if (tabela.getSelectionModel().getSelectedItems().size() > 1) {
            alerta("Selecione apenas uma conta para editar.", Alert.AlertType.WARNING);
            return;
        }
        FormularioMovimentacao form = new FormularioMovimentacao();
        form.preencherParaEdicao(t);

        Dialog<FormularioMovimentacao> diag = new Dialog<>();
        diag.setTitle("Editar conta");
        diag.setHeaderText("Altere os dados de \"" + t.getDescricao() + "\".");
        ScrollPane rolagem = new ScrollPane(form);
        rolagem.setFitToWidth(true);
        rolagem.setPrefViewportHeight(400);
        diag.getDialogPane().setContent(rolagem);

        ButtonType salvar = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        diag.getDialogPane().getButtonTypes().addAll(salvar, ButtonType.CANCEL);
        diag.setResultConverter(b -> b == salvar ? form : null);

        diag.showAndWait().ifPresent(f -> {
            Optional<String> erro = f.validar();
            if (erro.isPresent()) {
                alerta(erro.get(), Alert.AlertType.WARNING);
                return;
            }
            Transacao atualizada = f.construirBase();
            atualizada.setId(t.getId());
            transacaoDAO.update(atualizada);
            alerta("Conta atualizada com sucesso.", Alert.AlertType.INFORMATION);
            atualizarTudo();
            onDadosAlterados.run();
        });
    }

    // Exclusão com confirmação
    private void excluirSelecionadas() {
        List<Transacao> selecionadas = new ArrayList<>(tabela.getSelectionModel().getSelectedItems());
        if (selecionadas.isEmpty()) {
            alerta("Selecione ao menos uma conta na lista.", Alert.AlertType.WARNING);
            return;
        }
        String mensagem = selecionadas.size() == 1
                ? "Excluir \"" + selecionadas.get(0).getDescricao() + "\"? Esta ação não pode ser desfeita."
                : "Excluir " + selecionadas.size() + " contas selecionadas? Esta ação não pode ser desfeita.";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText(mensagem);
        ButtonType excluirBtn = new ButtonType("Excluir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(excluirBtn, cancelarBtn);
        confirm.showAndWait().filter(b -> b == excluirBtn).ifPresent(b -> {
            selecionadas.forEach(t -> transacaoDAO.delete(t.getId()));
            alerta("Conta(s) excluída(s).", Alert.AlertType.INFORMATION);
            atualizarTudo();
            onDadosAlterados.run();
        });
    }

    // =========================================================
    // DADOS (resumo e tabela)
    // =========================================================
    private void atualizarTudo() {
        atualizarResumo();
        atualizarTabela();
        atualizarBotaoMarcar();
    }

    private void atualizarResumo() {
        lblTotal.setText(Formatador.moeda(transacaoDAO.somaPendente(tipo())));
        lblVencido.setText(Formatador.moeda(transacaoDAO.somaVencido(tipo())));
        long proximos = transacaoDAO.pendentes().stream()
                .filter(t -> tipo().equals(t.getTipo()))
                .filter(t -> !t.isVencida())
                .filter(t -> !t.getDataReferencia().isAfter(LocalDate.now().plusDays(7)))
                .count();
        lblProximos.setText(proximos + (proximos == 1 ? " conta" : " contas") + " em até 7 dias");
    }

    private void atualizarTabela() {
        String situacao = filtroSituacao.getValue() == null ? "Pendentes" : filtroSituacao.getValue();
        List<Transacao> todas = transacaoDAO.porTipo(tipo());
        List<Transacao> resultado = new ArrayList<>();
        for (Transacao t : todas) {
            switch (situacao) {
                case "Pendentes" -> {
                    if (!Transacao.STATUS_PAGO.equals(t.getStatus()) && !t.isVencida()) {
                        resultado.add(t);
                    }
                }
                case "Vencidas" -> {
                    if (t.isVencida()) {
                        resultado.add(t);
                    }
                }
                default -> {
                    if (verboConcluido.equals(situacao) && Transacao.STATUS_PAGO.equals(t.getStatus())) {
                        resultado.add(t);
                    } else if ("Todas".equals(situacao)) {
                        resultado.add(t);
                    }
                }
            }
        }
        Comparator<Transacao> cmp = Transacao.STATUS_PAGO.equals(estadoPrincipal(situacao))
                ? Comparator.comparing(Transacao::getData).reversed()
                : Comparator.comparing(Transacao::getDataReferencia);
        resultado.sort(cmp);
        tabela.getItems().setAll(resultado);
        tabela.refresh();
    }

    private String estadoPrincipal(String situacao) {
        return verboConcluido.equals(situacao) ? Transacao.STATUS_PAGO : Transacao.STATUS_PENDENTE;
    }

    private void alerta(String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}