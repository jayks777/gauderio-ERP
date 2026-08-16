package br.com.gauderio.ui;

import br.com.gauderio.dao.CategoriaDAO;
import br.com.gauderio.dao.ContaDAO;
import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.model.Categoria;
import br.com.gauderio.model.ContaBancaria;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.Formatador;
import br.com.gauderio.util.UiUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
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
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Cadastro e gestão de lançamentos:
 * receitas, despesas, lançamentos recorrentes e lançamentos futuros (boletos/vendas).
 */
public class FinanceiroView extends BorderPane implements Refreshable {

    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final ContaDAO contaDAO = new ContaDAO();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    private int idEmEdicao = -1;

    private final ComboBox<String> tipoCombo = new ComboBox<>(
            FXCollections.observableArrayList(Transacao.TIPO_RECEITA, Transacao.TIPO_DESPESA));
    private final TextField descricaoField = new TextField();
    private final TextField valorField = new TextField();
    private final ComboBox<Categoria> categoriaCombo = new ComboBox<>();
    private final ComboBox<ContaBancaria> contaCombo = new ComboBox<>();
    private final DatePicker dataPicker = new DatePicker(LocalDate.now());
    private final DatePicker vencimentoPicker = new DatePicker();
    private final ComboBox<String> statusCombo = new ComboBox<>(
            FXCollections.observableArrayList(Transacao.STATUS_PENDENTE, Transacao.STATUS_PAGO));
    private final CheckBox recorrenteCheck = new CheckBox("Lançamento recorrente");
    private final ComboBox<String> frequenciaCombo = new ComboBox<>(
            FXCollections.observableArrayList("SEMANAL", "MENSAL", "BIMESTRAL", "TRIMESTRAL", "SEMESTRAL", "ANUAL"));
    private final Spinner<Integer> ocorrenciasSpinner = new Spinner<>(1, 120, 12);

    private final ComboBox<String> filtroTipo = new ComboBox<>(
            FXCollections.observableArrayList("TODAS", Transacao.TIPO_RECEITA, Transacao.TIPO_DESPESA));
    private final ComboBox<String> filtroPeriodo = new ComboBox<>(
            FXCollections.observableArrayList("Todos", "Este mês", "Mês anterior", "Últimos 12 meses"));

    private final TableView<Transacao> tabela = new TableView<>();

    public FinanceiroView() {
        setPadding(new Insets(24));
        getStyleClass().add("content");

        setTop(criarCabecalho());

        VBox area = new VBox(16,
                criarFormulario(),
                criarFiltros(),
                criarTabela());
        area.setPadding(new Insets(18, 0, 0, 0));
        setCenter(area);

        filtroTipo.setValue("TODAS");
        filtroPeriodo.setValue("Todos");
        recorrenteCheck.setSelected(false);
        statusCombo.setValue(Transacao.STATUS_PENDENTE);
        atualizarFrequencias();
        recarregarCategoriaCombo();
        recarregarContaCombo();
        aplicarFiltros();
    }

    private BorderPane criarCabecalho() {
        Button novaReceita = new Button("+ Nova receita");
        novaReceita.getStyleClass().addAll("btn", "btn-green");
        novaReceita.setOnAction(e -> {
            limparFormulario();
            tipoCombo.setValue(Transacao.TIPO_RECEITA);
            descricaoField.requestFocus();
        });

        Button novaDespesa = new Button("+ Nova despesa / boleto");
        novaDespesa.getStyleClass().addAll("btn", "btn-red");
        novaDespesa.setOnAction(e -> {
            limparFormulario();
            tipoCombo.setValue(Transacao.TIPO_DESPESA);
            descricaoField.requestFocus();
        });

        Button atualizar = new Button("Atualizar");
        atualizar.getStyleClass().addAll("btn", "btn-outline");
        atualizar.setOnAction(e -> aplicarFiltros());

        return UiUtil.cabecalhoComAcoes(
                "Financeiro · Receitas e Despesas",
                "Cadastre receitas, despesas, lançamentos recorrentes e futuros (boletos e vendas).",
                novaReceita, novaDespesa, atualizar);
    }

    @Override
    public void refresh() {
        recarregarContaCombo();
        recarregarCategoriaCombo();
        aplicarFiltros();
    }

    private VBox criarFormulario() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        Label titulo = new Label("Novo lançamento");
        titulo.getStyleClass().add("form-title");

        tipoCombo.setMaxWidth(Double.MAX_VALUE);
        descricaoField.setPromptText("Ex.: Venda avulsa, Conta de luz, Aluguel...");
        valorField.setPromptText("0,00");
        vencimentoPicker.setPromptText("Opcional (usado em boletos)");
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        grid.addColumn(0);
        grid.add(new Label("Tipo"), 0, 0);
        grid.add(tipoCombo, 0, 1);

        grid.add(new Label("Descrição"), 1, 0);
        grid.add(descricaoField, 1, 1);

        grid.add(new Label("Valor (R$)"), 0, 2);
        grid.add(valorField, 0, 3);

        grid.add(new Label("Categoria"), 1, 2);
        grid.add(categoriaCombo, 1, 3);

        grid.add(new Label("Conta bancária"), 2, 2);
        grid.add(contaCombo, 2, 3);

        grid.add(new Label("Data do lançamento"), 0, 4);
        grid.add(dataPicker, 0, 5);

        grid.add(new Label("Vencimento"), 1, 4);
        grid.add(vencimentoPicker, 1, 5);

        grid.add(new Label("Situação"), 2, 4);
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(statusCombo, 2, 5);

        HBox recBox = new HBox(10, recorrenteCheck, frequenciaCombo, new Label("Nº de ocorrências"), ocorrenciasSpinner);
        recBox.setAlignment(Pos.CENTER_LEFT);
        recBox.getStyleClass().add("recorrencia-box");

        HBox acoes = new HBox(10);
        Button salvar = new Button("Salvar lançamento");
        salvar.getStyleClass().addAll("btn", "btn-green");
        salvar.setOnAction(e -> salvar());

        Button limpar = new Button("Limpar");
        limpar.getStyleClass().addAll("btn", "btn-outline");
        limpar.setOnAction(e -> limparFormulario());

        acoes.getChildren().addAll(salvar, limpar);

        Label dica = new Label();
        dica.getStyleClass().add("form-hint");
        atualizarDica(dica);

        card.getChildren().addAll(titulo, grid, recBox, dica, acoes);

        // Comportamento do formulário
        tipoCombo.valueProperty().addListener((obs, antes, depois) -> {
            recarregarCategoriaCombo();
            atualizarDica(dica);
        });
        recorrenteCheck.selectedProperty().addListener((obs, antes, depois) -> {
            frequenciaCombo.setDisable(!depois);
            ocorrenciasSpinner.setDisable(!depois);
        });
        frequenciaCombo.setValue("MENSAL");
        recorrenteCheck.setSelected(false);
        frequenciaCombo.setDisable(true);
        ocorrenciasSpinner.setDisable(true);

        return card;
    }

    private void atualizarDica(Label dica) {
        if (Transacao.TIPO_RECEITA.equals(tipoCombo.getValue())) {
            dica.setText("💡 Dica: preencha um vencimento futuro para registrar uma venda a receber. "
                    + "Ative “recorrente” para gerar várias parcelas de uma vez.");
        } else {
            dica.setText("💡 Dica: preencha um vencimento futuro para registrar um boleto a pagar. "
                    + "Ative “recorrente” para gerar várias parcelas de uma vez.");
        }
    }

    private HBox criarFiltros() {
        Label lTipo = new Label("Filtro:");
        Label lPeriodo = new Label("Período:");

        filtroTipo.setPrefWidth(150);
        filtroPeriodo.setPrefWidth(160);

        filtroTipo.valueProperty().addListener((obs, a, b) -> aplicarFiltros());
        filtroPeriodo.valueProperty().addListener((obs, a, b) -> aplicarFiltros());

        HBox barra = new HBox(8, lTipo, filtroTipo, lPeriodo, filtroPeriodo);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("filter-bar");
        return barra;
    }

    private VBox criarTabela() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label titulo = new Label("Lançamentos");
        titulo.getStyleClass().add("card-label");

        TableColumn<Transacao, LocalDate> cData = colunaData();
        TableColumn<Transacao, String> cTipo = colunaTexto("Tipo", "tipo", 90);
        TableColumn<Transacao, String> cDesc = colunaTexto("Descrição", "descricao", 220);
        TableColumn<Transacao, String> cCat = colunaTexto("Categoria", "categoria", 140);
        TableColumn<Transacao, String> cConta = colunaTexto("Conta", "conta", 130);
        TableColumn<Transacao, Double> cValor = colunaValor();
        TableColumn<Transacao, String> cVenc = colunaVencimento();
        TableColumn<Transacao, String> cStatus = colunaStatus();
        TableColumn<Transacao, String> cRec = colunaTexto("Recorrente", "recorrente", 85);

        tabela.getColumns().add(cData);
        tabela.getColumns().add(cTipo);
        tabela.getColumns().add(cDesc);
        tabela.getColumns().add(cCat);
        tabela.getColumns().add(cConta);
        tabela.getColumns().add(cValor);
        tabela.getColumns().add(cVenc);
        tabela.getColumns().add(cStatus);
        tabela.getColumns().add(cRec);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setItems(FXCollections.observableArrayList());
        VBox.setVgrow(tabela, Priority.ALWAYS);

        Button marcarPago = new Button("Marcar como pago / recebido");
        marcarPago.getStyleClass().addAll("btn", "btn-green");
        marcarPago.setOnAction(e -> marcarPagoSelecionado());

        Button editar = new Button("✎ Editar");
        editar.getStyleClass().addAll("btn", "btn-outline");
        editar.setOnAction(e -> preencherFormularioParaEdicao());

        Button excluir = new Button("Excluir");
        excluir.getStyleClass().addAll("btn", "btn-outline-red");
        excluir.setOnAction(e -> excluirSelecionado());

        HBox acoes = new HBox(10, marcarPago, editar, excluir);
        acoes.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(titulo, tabela, acoes);
        return card;
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
        return col;
    }

    private TableColumn<Transacao, String> colunaTexto(String titulo, String prop, double largura) {
        TableColumn<Transacao, String> col = new TableColumn<>(titulo);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setMinWidth(largura);
        return col;
    }

    private TableColumn<Transacao, Double> colunaValor() {
        TableColumn<Transacao, Double> col = new TableColumn<>("Valor");
        col.setCellValueFactory(new PropertyValueFactory<>("valor"));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Formatador.moeda(item));
                if (!empty) {
                    Transacao t = getTableView().getItems().get(getIndex());
                    setStyle(Transacao.TIPO_RECEITA.equals(t.getTipo())
                            ? "-fx-text-fill:#0E7A3C;"
                            : "-fx-text-fill:#C8102E;");
                } else {
                    setStyle("");
                }
            }
        });
        col.setMinWidth(110);
        return col;
    }

    private TableColumn<Transacao, String> colunaVencimento() {
        TableColumn<Transacao, String> col = new TableColumn<>("Vencimento");
        col.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(
                d.getValue().getVencimento() == null ? "—" : Formatador.data(d.getValue().getVencimento())));
        col.setMinWidth(100);
        return col;
    }

    private TableColumn<Transacao, String> colunaStatus() {
        TableColumn<Transacao, String> col = new TableColumn<>("Status");
        col.setCellValueFactory(d -> new javafx.beans.property.ReadOnlyStringWrapper(
                d.getValue().getStatusExibido()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText("");
                    setStyle("");
                    return;
                }
                setText(status);
                if ("PAGO".equals(status)) {
                    setStyle("-fx-text-fill:#0E7A3C; -fx-font-weight:bold;");
                } else if ("VENCIDO".equals(status)) {
                    setStyle("-fx-text-fill:#C8102E; -fx-font-weight:bold;");
                } else {
                    setStyle("-fx-text-fill:#9A6B00; -fx-font-weight:bold;");
                }
            }
        });
        col.setMinWidth(100);
        return col;
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
            if (!"TODAS".equals(tipo) && !tipo.equals(t.getTipo())) {
                continue;
            }
            if (inicio != null && (t.getData().isBefore(inicio) || t.getData().isAfter(fim))) {
                continue;
            }
            resultado.add(t);
        }
        tabela.getItems().setAll(resultado);
        tabela.refresh();
    }

    private void recarregarCategoriaCombo() {
        String tipo = tipoCombo.getValue();
        if (tipo == null) {
            return;
        }
        Categoria selecionada = categoriaCombo.getValue();
        categoriaCombo.getItems().setAll(categoriaDAO.porTipo(tipo));
        if (selecionada != null) {
            for (Categoria c : categoriaCombo.getItems()) {
                if (c.getNome().equals(selecionada.getNome())) {
                    categoriaCombo.setValue(c);
                    return;
                }
            }
        }
        if (!categoriaCombo.getItems().isEmpty()) {
            categoriaCombo.setValue(categoriaCombo.getItems().get(0));
        }
    }

    private void recarregarContaCombo() {
        ContaBancaria atual = contaCombo.getValue();
        contaCombo.getItems().setAll(contaDAO.findAll());
        if (atual != null) {
            for (ContaBancaria c : contaCombo.getItems()) {
                if (c.getNome().equals(atual.getNome())) {
                    contaCombo.setValue(c);
                    return;
                }
            }
        }
    }

    private void atualizarFrequencias() {
        frequenciaCombo.setDisable(!recorrenteCheck.isSelected());
        ocorrenciasSpinner.setDisable(!recorrenteCheck.isSelected());
        if (frequenciaCombo.getValue() == null) {
            frequenciaCombo.setValue("MENSAL");
        }
    }

    private void alerta(String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void salvar() {
        try {
            String tipo = tipoCombo.getValue();
            String descricao = descricaoField.getText().trim();
            double valor = Formatador.parseValor(valorField.getText());
            Categoria categoria = categoriaCombo.getValue();
            ContaBancaria conta = contaCombo.getValue();
            LocalDate data = dataPicker.getValue();
            LocalDate vencimento = vencimentoPicker.getValue();
            String status = statusCombo.getValue() == null ? Transacao.STATUS_PENDENTE : statusCombo.getValue();

            if (tipo == null) {
                alerta("Informe o tipo do lançamento.", Alert.AlertType.WARNING);
                return;
            }
            if (descricao.isEmpty()) {
                descricao = Transacao.TIPO_RECEITA.equals(tipo)
                        ? "Receita (sem descrição)"
                        : "Despesa (sem descrição)";
            }
            if (valor <= 0) {
                alerta("O valor deve ser maior que zero.", Alert.AlertType.WARNING);
                return;
            }
            if (data == null) {
                data = LocalDate.now();
            }
            String contaNome = conta == null ? "" : conta.getNome();

            String catNome = categoria == null ? "Sem categoria" : categoria.getNome();
            boolean recorrente = recorrenteCheck.isSelected();
            String frequencia = recorrente ? frequenciaCombo.getValue() : null;

            if (idEmEdicao > 0) {
                Transacao t = new Transacao(tipo, descricao, valor, catNome, contaNome,
                        data, vencimento, status, recorrente, frequencia);
                t.setId(idEmEdicao);
                transacaoDAO.update(t);
                alerta("Lançamento atualizado com sucesso!", Alert.AlertType.INFORMATION);
                idEmEdicao = -1;
                limparFormulario();
                aplicarFiltros();
                return;
            }

            List<Transacao> lancamentos = gerarLancamentos(tipo, descricao, valor, catNome,
                    contaNome, data, vencimento, status, recorrente, frequencia, ocorrenciasSpinner.getValue());

            if (lancamentos.size() > 1) {
                transacaoDAO.insertAll(lancamentos);
                alerta(lancamentos.size() + " lançamentos recorrentes gerados com sucesso!",
                        Alert.AlertType.INFORMATION);
            } else {
                transacaoDAO.insert(lancamentos.get(0));
                alerta("Lançamento salvo com sucesso!", Alert.AlertType.INFORMATION);
            }
            limparFormulario();
            aplicarFiltros();
        } catch (NumberFormatException ex) {
            alerta("Valor inválido. Use o formato 123,45.", Alert.AlertType.ERROR);
        } catch (Exception ex) {
            alerta("Erro ao salvar: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private List<Transacao> gerarLancamentos(String tipo, String descricao, double valor, String categoria,
                                             String conta, LocalDate dataBase, LocalDate vencimentoBase,
                                             String status, boolean recorrente, String frequencia, int ocorrencias) {
        List<Transacao> lista = new ArrayList<>();
        int total = recorrente ? Math.max(1, ocorrencias) : 1;
        Period intervalo = periodoDaFrequencia(frequencia);

        for (int i = 0; i < total; i++) {
            LocalDate data = dataBase;
            LocalDate venc = vencimentoBase;
            String st = status;
            if (i > 0) {
                Period salto = intervalo.multipliedBy(i);
                data = dataBase.plus(salto);
                venc = vencimentoBase == null ? data : vencimentoBase.plus(salto);
                st = Transacao.STATUS_PENDENTE; // parcelas futuras já nascem pendentes
            }
            lista.add(new Transacao(tipo, descricao, valor, categoria, conta,
                    data, venc, st, recorrente, recorrente ? frequencia : null));
        }
        return lista;
    }

    private Period periodoDaFrequencia(String frequencia) {
        return switch (frequencia == null ? "MENSAL" : frequencia) {
            case "SEMANAL" -> Period.ofWeeks(1);
            case "MENSAL" -> Period.ofMonths(1);
            case "BIMESTRAL" -> Period.ofMonths(2);
            case "TRIMESTRAL" -> Period.ofMonths(3);
            case "SEMESTRAL" -> Period.ofMonths(6);
            case "ANUAL" -> Period.ofYears(1);
            default -> Period.ofMonths(1);
        };
    }

    private void marcarPagoSelecionado() {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione um lançamento na lista.", Alert.AlertType.WARNING);
            return;
        }
        transacaoDAO.setStatus(t.getId(), Transacao.STATUS_PAGO);
        tabela.refresh();
        aplicarFiltros();
    }

    private void excluirSelecionado() {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione um lançamento na lista.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir o lançamento \"" + t.getDescricao() + "\"?");
        confirm.setHeaderText("Confirmar exclusão");
        confirm.showAndWait()
                .filter(b -> b == javafx.scene.control.ButtonType.OK)
                .ifPresent(b -> {
                    transacaoDAO.delete(t.getId());
                    aplicarFiltros();
                });
    }

    private void preencherFormularioParaEdicao() {
        Transacao t = tabela.getSelectionModel().getSelectedItem();
        if (t == null) {
            alerta("Selecione um lançamento na lista.", Alert.AlertType.WARNING);
            return;
        }
        idEmEdicao = t.getId();
        tipoCombo.setValue(t.getTipo());
        descricaoField.setText(t.getDescricao());
        valorField.setText(String.format("%.2f", t.getValor()).replace(".", ","));
        dataPicker.setValue(t.getData());
        vencimentoPicker.setValue(t.getVencimento());
        statusCombo.setValue(t.getStatus());
        recorrenteCheck.setSelected(t.isRecorrente());
        if (t.getFrequencia() != null) {
            frequenciaCombo.setValue(t.getFrequencia());
        }
        recarregarCategoriaCombo();
        recarregarContaCombo();
        selectCategoria(t.getCategoria());
        selectConta(t.getConta());
    }

    private void selectCategoria(String nome) {
        if (nome == null) {
            return;
        }
        for (Categoria c : categoriaCombo.getItems()) {
            if (nome.equals(c.getNome())) {
                categoriaCombo.setValue(c);
                return;
            }
        }
    }

    private void selectConta(String nome) {
        if (nome == null) {
            return;
        }
        for (ContaBancaria c : contaCombo.getItems()) {
            if (nome.equals(c.getNome())) {
                contaCombo.setValue(c);
                return;
            }
        }
    }

    private void limparFormulario() {
        idEmEdicao = -1;
        descricaoField.clear();
        valorField.clear();
        dataPicker.setValue(LocalDate.now());
        vencimentoPicker.setValue(null);
        statusCombo.setValue(Transacao.STATUS_PENDENTE);
        recorrenteCheck.setSelected(false);
        tipoCombo.setValue(Transacao.TIPO_RECEITA);
    }
}