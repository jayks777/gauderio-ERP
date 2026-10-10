package br.com.gauderio.ui;

import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.ExportadorCsv;
import br.com.gauderio.util.Formatador;
import br.com.gauderio.util.IconeUtil;
import br.com.gauderio.util.UiUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Contas a pagar / contas a receber: visões filtradas e focadas das movimentações.
 * Uma única classe atende os dois casos: receber = true monta a tela de contas a receber.
 */
public class ContasParaView extends BorderPane implements Refreshable {

    private final boolean receber;
    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final Runnable onDadosAlterados;

    private final Label lblTotal = new Label();
    private final Label lblVencido = new Label();
    private final Label lblProximos = new Label();
    private final ComboBox<String> filtroSituacao = new ComboBox<>();
    private final TextField campoBusca = new TextField();
    private final TableView<Transacao> tabela = new TableView<>();
    private final Button btnMarcar = new Button();
    private final Button btnNovo = new Button();

    // Rodapé de resumo da lista filtrada
    private final Label lblQtdFiltrada = new Label("0 contas");
    private final Label lblSomaFiltrada = new Label("Total listado: R$ 0,00");

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
        campoBusca.textProperty().addListener((obs, a, b) -> atualizarTabela());

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
        String subtitulo = receber ? "Acompanhe tudo o que clientes e parceiros têm a pagar para você."
                : "Acompanhe seus boletos, despesas e compromissos futuros a pagar.";

        Button btnTopo = new Button(receber ? "+ Nova conta a receber" : "+ Nova conta a pagar",
                IconeUtil.iconeBotao(IconeUtil.PATH_MAIS, "#ffffff"));
        btnTopo.getStyleClass().addAll("btn", receber ? "btn-green" : "btn-red");
        btnTopo.setOnAction(e -> novaConta());

        return UiUtil.cabecalhoComAcoes(titulo, subtitulo, btnTopo);
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
                criarCardResumo("Total vencido", lblVencido, "red"),
                criarCardResumo("Próximos vencimentos", lblProximos, "blue"));
        resumo.setAlignment(Pos.CENTER_LEFT);
        return resumo;
    }

    private VBox criarCardResumo(String rotulo, Label lValor, String cor) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));
        card.setPrefWidth(240);

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

        campoBusca.setPromptText("Buscar conta por descrição, categoria ou conta...");
        campoBusca.setPrefWidth(280);

        Button btnExportar = new Button("Exportar (CSV)", IconeUtil.iconeBotao(IconeUtil.PATH_EXPORTAR, "#41505c"));
        btnExportar.getStyleClass().addAll("btn", "btn-outline");
        btnExportar.setOnAction(e -> exportarCsv());

        Region espacoFiltro = new Region();
        HBox.setHgrow(espacoFiltro, Priority.ALWAYS);

        HBox filtros = new HBox(10,
                new Label("Buscar:"), campoBusca,
                new Label("Situação:"), filtroSituacao,
                espacoFiltro,
                btnExportar);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.getStyleClass().add("filter-bar");

        configurarColunas();
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        btnNovo.setText(receber ? "+ Nova conta a receber" : "+ Nova conta a pagar");
        btnNovo.setGraphic(IconeUtil.iconeBotao(IconeUtil.PATH_MAIS, "#ffffff"));
        btnNovo.getStyleClass().addAll("btn", receber ? "btn-green" : "btn-red");
        btnNovo.setOnAction(e -> novaConta());

        btnMarcar.getStyleClass().addAll("btn", "btn-green");
        btnMarcar.setGraphic(IconeUtil.iconeBotao(IconeUtil.PATH_CHECK_CIRCLE, "#ffffff"));
        btnMarcar.setText(verboAcao);
        btnMarcar.setOnAction(e -> marcarSelecionadas());

        Button btnEditar = new Button("Editar", IconeUtil.iconeBotao(IconeUtil.PATH_EDITAR, "#41505c"));
        btnEditar.getStyleClass().addAll("btn", "btn-outline");
        btnEditar.setOnAction(e -> editarSelecionada());

        Button btnExcluir = new Button("Excluir", IconeUtil.iconeBotao(IconeUtil.PATH_EXCLUIR, "#C8102E"));
        btnExcluir.getStyleClass().addAll("btn", "btn-outline-red");
        btnExcluir.setOnAction(e -> excluirSelecionadas());

        HBox acoes = new HBox(10, btnNovo, btnMarcar, btnEditar, btnExcluir);
        acoes.setAlignment(Pos.CENTER_LEFT);

        HBox barraResumo = criarBarraResumoTabela();

        card.getChildren().addAll(filtros, tabela, barraResumo, acoes);
        return card;
    }

    private HBox criarBarraResumoTabela() {
        lblQtdFiltrada.getStyleClass().add("summary-count");
        lblSomaFiltrada.getStyleClass().add("summary-balance");

        Region espaco = new Region();
        HBox.setHgrow(espaco, Priority.ALWAYS);

        HBox barra = new HBox(12, lblQtdFiltrada, espaco, lblSomaFiltrada);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("history-summary-bar");
        barra.setPadding(new Insets(8, 12, 8, 12));
        return barra;
    }

    // =========================================================
    // COLUNAS DA TABELA
    // =========================================================
    private void configurarColunas() {
        tabela.getColumns().clear();
        tabela.getColumns().addAll(List.of(
                colunaVencimento(),
                colunaPrazo(),
                colunaTexto("Descrição", "descricao", 230),
                colunaTexto("Categoria", "categoria", 140),
                colunaTexto("Conta Bancária", "conta", 130),
                colunaValor(),
                colunaSituacao()));
    }

    private TableColumn<Transacao, String> colunaVencimento() {
        TableColumn<Transacao, String> col = new TableColumn<>("Vencimento");
        col.setCellValueFactory(d -> new ReadOnlyStringWrapper(
                d.getValue().getVencimento() == null ? Formatador.data(d.getValue().getData()) : Formatador.data(d.getValue().getVencimento())));
        col.setMinWidth(105);
        return col;
    }

    private TableColumn<Transacao, String> colunaPrazo() {
        TableColumn<Transacao, String> col = new TableColumn<>("Prazo");
        col.setCellValueFactory(d -> {
            Transacao t = d.getValue();
            if (Transacao.STATUS_PAGO.equals(t.getStatus())) {
                return new ReadOnlyStringWrapper("Concluída");
            }
            LocalDate ref = t.getDataReferencia();
            LocalDate hoje = LocalDate.now();
            long dias = ChronoUnit.DAYS.between(hoje, ref);
            if (dias == 0) {
                return new ReadOnlyStringWrapper("Vence HOJE");
            } else if (dias > 0) {
                return new ReadOnlyStringWrapper("Em " + dias + (dias == 1 ? " dia" : " dias"));
            } else {
                long atraso = Math.abs(dias);
                return new ReadOnlyStringWrapper(atraso + (atraso == 1 ? " dia de atraso" : " dias de atraso"));
            }
        });
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
                if (item.contains("atraso") || item.equals("Vence HOJE")) {
                    setStyle("-fx-text-fill: #C8102E; -fx-font-weight: bold;");
                } else if ("Concluída".equals(item)) {
                    setStyle("-fx-text-fill: #0E7A3C;");
                } else {
                    setStyle("-fx-text-fill: #5b6c7a;");
                }
            }
        });
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
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(Formatador.moeda(item));
                    setStyle("-fx-text-fill:" + (receber ? "#0E7A3C" : "#C8102E") + "; -fx-font-weight:bold;");
                }
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

    // =========================================================
    // AÇÕES: NOVA, MARCAR, EDITAR, EXCLUIR, EXPORTAR
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
        diag.setHeaderText(receber ? "Cadastrar valor futuro que a empresa vai receber:"
                : "Cadastrar compromisso futuro que a empresa precisa pagar:");

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

    private void marcarSelecionadas() {
        List<Transacao> selecionadas = new ArrayList<>(tabela.getSelectionModel().getSelectedItems());
        selecionadas.removeIf(t -> Transacao.STATUS_PAGO.equals(t.getStatus()));
        if (selecionadas.isEmpty()) {
            alerta("Selecione contas pendentes na lista para dar baixa.", Alert.AlertType.WARNING);
            return;
        }
        Dialog<LocalDate> diag = new Dialog<>();
        diag.setTitle(verboAcao);
        diag.setHeaderText(selecionadas.size() == 1
                ? "Confirmar liquidação: \"" + selecionadas.get(0).getDescricao() + "\"?"
                : "Confirmar liquidação de " + selecionadas.size() + " contas selecionadas?");

        DatePicker data = new DatePicker(LocalDate.now());
        Label ajuda = new Label("Informe a data em que o dinheiro " + (receber ? "entrou" : "saiu")
                + ". O saldo da respectiva conta bancária será atualizado automaticamente.");
        ajuda.getStyleClass().add("form-hint");
        ajuda.setWrapText(true);
        VBox conteudo = new VBox(10, new Label("Data do pagamento/recebimento:"), data, ajuda);
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
                            : selecionadas.size() + " contas " + (receber ? "recebidas com sucesso." : "pagas com sucesso."),
                    Alert.AlertType.INFORMATION);
            atualizarTudo();
            onDadosAlterados.run();
        });
    }

    private void editarSelecionada() {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione uma conta na lista para editar.", Alert.AlertType.WARNING);
            return;
        }
        if (tabela.getSelectionModel().getSelectedItems().size() > 1) {
            alerta("Selecione apenas uma conta por vez para editar.", Alert.AlertType.WARNING);
            return;
        }
        FormularioMovimentacao form = new FormularioMovimentacao();
        form.preencherParaEdicao(t);

        Dialog<FormularioMovimentacao> diag = new Dialog<>();
        diag.setTitle("Editar conta #" + t.getId());
        diag.setHeaderText("Altere os dados de \"" + t.getDescricao() + "\":");
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

    private void excluirSelecionadas() {
        List<Transacao> selecionadas = new ArrayList<>(tabela.getSelectionModel().getSelectedItems());
        if (selecionadas.isEmpty()) {
            alerta("Selecione ao menos uma conta na lista.", Alert.AlertType.WARNING);
            return;
        }
        String mensagem = selecionadas.size() == 1
                ? "Excluir definitivamente \"" + selecionadas.get(0).getDescricao() + "\"? Esta ação não pode ser desfeita."
                : "Excluir definitivamente as " + selecionadas.size() + " contas selecionadas? Esta ação não pode ser desfeita.";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir conta(s)");
        confirm.setHeaderText(null);
        confirm.setContentText(mensagem);
        ButtonType excluirBtn = new ButtonType("Sim, excluir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(excluirBtn, cancelarBtn);
        confirm.showAndWait().filter(b -> b == excluirBtn).ifPresent(b -> {
            selecionadas.forEach(t -> transacaoDAO.delete(t.getId()));
            alerta("Conta(s) excluída(s) com sucesso.", Alert.AlertType.INFORMATION);
            atualizarTudo();
            onDadosAlterados.run();
        });
    }

    private void exportarCsv() {
        List<Transacao> lista = tabela.getItems();
        if (lista == null || lista.isEmpty()) {
            alerta("Não há contas para exportar na lista atual.", Alert.AlertType.WARNING);
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle(receber ? "Exportar contas a receber" : "Exportar contas a pagar");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Planilha CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName((receber ? "contas-a-receber-" : "contas-a-pagar-") + LocalDate.now() + ".csv");
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
        String termo = campoBusca.getText() == null ? "" : campoBusca.getText().trim().toLowerCase();

        List<Transacao> todas = transacaoDAO.porTipo(tipo());
        List<Transacao> resultado = new ArrayList<>();
        double somaValor = 0;

        for (Transacao t : todas) {
            boolean statusOk;
            switch (situacao) {
                case "Pendentes" -> statusOk = !Transacao.STATUS_PAGO.equals(t.getStatus()) && !t.isVencida();
                case "Vencidas" -> statusOk = t.isVencida();
                default -> {
                    if (verboConcluido.equals(situacao)) {
                        statusOk = Transacao.STATUS_PAGO.equals(t.getStatus());
                    } else {
                        statusOk = true; // "Todas"
                    }
                }
            }

            if (!statusOk) {
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
            somaValor += t.getValor();
        }

        Comparator<Transacao> cmp = Transacao.STATUS_PAGO.equals(estadoPrincipal(situacao))
                ? Comparator.comparing(Transacao::getData).reversed()
                : Comparator.comparing(Transacao::getDataReferencia);
        resultado.sort(cmp);
        tabela.getItems().setAll(resultado);
        tabela.refresh();

        lblQtdFiltrada.setText(resultado.size() + (resultado.size() == 1 ? " conta encontrada" : " contas encontradas"));
        lblSomaFiltrada.setText("Total listado: " + Formatador.moeda(somaValor));
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
