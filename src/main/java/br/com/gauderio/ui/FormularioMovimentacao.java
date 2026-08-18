package br.com.gauderio.ui;

import br.com.gauderio.dao.CategoriaDAO;
import br.com.gauderio.dao.ContaDAO;
import br.com.gauderio.model.Categoria;
import br.com.gauderio.model.ContaBancaria;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.Formatador;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Formulário simplificado para registrar movimentações.
 * O usuário nunca precisa escolher "PENDENTE" ou "PAGO":
 * "Recebida agora / Paga agora" vira paga/recebida e
 * "A receber depois / A pagar depois" vira pendente com data de vencimento/recebimento.
 */
public class FormularioMovimentacao extends VBox {

    private static final String RECEBIDA_AGORA = "Recebida agora";
    private static final String A_RECEBER_DEPOIS = "A receber depois";
    private static final String PAGA_AGORA = "Paga agora";
    private static final String A_PAGAR_DEPOIS = "A pagar depois";

    private final ComboBox<String> tipoCombo = new ComboBox<>(
            FXCollections.observableArrayList("Entrada", "Saída"));
    private final TextField descricaoField = new TextField();
    private final TextField valorField = new TextField();
    private final ComboBox<Categoria> categoriaCombo = new ComboBox<>();
    private final ComboBox<ContaBancaria> contaCombo = new ComboBox<>();
    private final ComboBox<String> quandoCombo = new ComboBox<>();
    private final Label lblDataFutura = new Label("Data de vencimento");
    private final DatePicker dataFuturaPicker = new DatePicker();
    private final Label lblAjudaData = new Label();
    private final CheckBox recorrenteCheck = new CheckBox("Repetir este lançamento");
    private final ComboBox<String> frequenciaCombo = new ComboBox<>(
            FXCollections.observableArrayList("Semanal", "Mensal", "Bimestral", "Trimestral", "Semestral", "Anual"));
    private final Spinner<Integer> ocorrenciasSpinner = new Spinner<>(1, 120, 12);
    private final Label lblFrequencia = new Label("Periodicidade");
    private final Label lblOcorrencias = new Label("Ocorrências");
    private final HBox recorrenciaBox = new HBox(10,
            recorrenteCheck, lblFrequencia, frequenciaCombo, lblOcorrencias, ocorrenciasSpinner);

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final ContaDAO contaDAO = new ContaDAO();

    private Transacao original; // preenchido quando estiver editando

    public FormularioMovimentacao() {
        super(14);
        setMaxWidth(Double.MAX_VALUE);

        tipoCombo.setMaxWidth(Double.MAX_VALUE);
        quandoCombo.setMaxWidth(Double.MAX_VALUE);
        valorField.setPromptText("0,00");
        descricaoField.setPromptText("Ex.: Venda de produtos, Conta de luz, Aluguel");
        descricaoField.setMaxWidth(Double.MAX_VALUE);
        dataFuturaPicker.setValue(LocalDate.now().plusDays(7));
        lblAjudaData.getStyleClass().add("form-hint");
        lblAjudaData.setWrapText(true);
        recorrenciaBox.setAlignment(Pos.CENTER_LEFT);
        recorrenciaBox.getStyleClass().add("recorrencia-box");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Tipo"), 0, 0);
        grid.add(tipoCombo, 0, 1);
        grid.add(new Label("Descrição"), 1, 0);
        grid.add(descricaoField, 1, 1);

        grid.add(new Label("Valor (R$)"), 0, 2);
        grid.add(valorField, 0, 3);
        grid.add(new Label("Categoria"), 1, 2);
        grid.add(categoriaCombo, 1, 3);

        grid.add(new Label("Conta bancária"), 0, 4);
        grid.add(contaCombo, 0, 5);
        grid.add(new Label("Quando acontece?"), 1, 4);
        grid.add(quandoCombo, 1, 5);

        grid.add(lblDataFutura, 0, 6);
        grid.add(dataFuturaPicker, 0, 7);
        grid.add(lblAjudaData, 1, 6, 3, 2);

        grid.add(recorrenciaBox, 0, 8, 3, 1);

        getChildren().add(grid);

        tipoCombo.valueProperty().addListener((obs, a, b) -> {
            recarregarCategorias();
            atualizarQuando();
        });
        quandoCombo.valueProperty().addListener((obs, a, b) -> atualizarVisibilidadeData());
        recorrenteCheck.selectedProperty().addListener((obs, a, b) -> atualizarVisibilidadeRecorrencia());

        recarregarContas();
        tipoCombo.setValue("Entrada");
        frequenciaCombo.setValue("Mensal");
        recorrenteCheck.setSelected(false);
        atualizarVisibilidadeRecorrencia();
    }

    // =========================================================
    // Leitores públicos usados pelas telas
    // =========================================================

    /** Tipo interno (RECEITA ou DESPESA), ou null se ainda não escolhido. */
    public String getTipo() {
        String t = tipoCombo.getValue();
        if ("Entrada".equals(t)) {
            return Transacao.TIPO_RECEITA;
        }
        if ("Saída".equals(t)) {
            return Transacao.TIPO_DESPESA;
        }
        return null;
    }

    public boolean ehDepois() {
        String v = quandoCombo.getValue();
        return A_RECEBER_DEPOIS.equals(v) || A_PAGAR_DEPOIS.equals(v);
    }

    /** Pré-seleciona o tipo: true = Entrada, false = Saída. */
    public void setTipoPreferido(boolean entrada) {
        tipoCombo.setValue(entrada ? "Entrada" : "Saída");
    }

    /** Usado pelas telas de contas a pagar/receber para já nascer como pendência. */
    public void selecionarDepois() {
        atualizarQuando();
        if (quandoCombo.getItems().size() > 1) {
            quandoCombo.setValue(quandoCombo.getItems().get(1));
        }
    }

    /** Recarrega a lista de contas (útil após cadastrar uma nova conta). */
    public void recarregarContas() {
        contaCombo.getItems().setAll(contaDAO.findAll());
    }

    // =========================================================
    // Construção / validação
    // =========================================================

    /** Retorna mensagem de erro amigável, ou vazio se os dados estiverem corretos. */
    public Optional<String> validar() {
        if (getTipo() == null) {
            return Optional.of("Escolha se é uma entrada ou uma saída.");
        }
        if (descricaoField.getText() == null || descricaoField.getText().isBlank()) {
            return Optional.of(Formatador.mensagemDescricaoObrigatoria());
        }
        if (valorField.getText() == null || valorField.getText().isBlank()) {
            return Optional.of(Formatador.mensagemValorInvalido());
        }
        double valor;
        try {
            valor = Formatador.parseValor(valorField.getText());
        } catch (NumberFormatException ex) {
            return Optional.of(Formatador.mensagemValorInvalido());
        }
        if (valor <= 0) {
            return Optional.of("Informe um valor maior que zero, por exemplo: 50,00.");
        }
        if (ehDepois() && dataFuturaPicker.getValue() == null) {
            return Optional.of("Informe a data de vencimento/recebimento.");
        }
        if (ehDepois() && dataFuturaPicker.getValue().isBefore(LocalDate.now())) {
            String sugestao = Transacao.TIPO_RECEITA.equals(getTipo()) ? RECEBIDA_AGORA : PAGA_AGORA;
            return Optional.of("A data informada já passou. Escolha uma data futura ou use \""
                    + sugestao + "\".");
        }
        return Optional.empty();
    }

    /** Monta um lançamento único a partir dos campos (status definido automaticamente). */
    public Transacao construirBase() {
        String tipo = getTipo() == null ? Transacao.TIPO_RECEITA : getTipo();
        boolean depois = ehDepois();
        String status = depois ? Transacao.STATUS_PENDENTE : Transacao.STATUS_PAGO;
        LocalDate data = LocalDate.now();
        if (original != null && original.getData() != null) {
            data = original.getData(); // preserva a data original ao editar
        }
        LocalDate vencimento = depois ? dataFuturaPicker.getValue() : null;
        if (original != null && original.getVencimento() != null && !depois) {
            vencimento = original.getVencimento(); // mantém o vencimento histórico ao editar algo já pago
        }
        String descricao = descricaoField.getText() == null ? "" : descricaoField.getText().trim();
        String categoria = categoriaCombo.getValue() == null ? "Sem categoria" : categoriaCombo.getValue().getNome();
        String conta = contaCombo.getValue() == null ? "" : contaCombo.getValue().getNome();
        boolean recorrente = recorrenteCheck.isSelected();
        String frequencia = recorrente ? frequenciaInterna() : null;

        Transacao t = new Transacao(tipo, descricao, 0, categoria, conta, data, vencimento, status,
                recorrente, frequencia);
        try {
            t.setValor(Formatador.parseValor(valorField.getText()));
        } catch (NumberFormatException ignorado) {
            // valor 0 fica até o usuário corrigir; validar() já avisou antes
        }
        return t;
    }

    /** Gera os lançamentos (1 ou N parcelas quando marcado como recorrente). */
    public List<Transacao> gerarLancamentos() {
        Transacao base = construirBase();
        int total = recorrenteCheck.isSelected() ? Math.max(1, ocorrenciasSpinner.getValue()) : 1;
        List<Transacao> lista = new ArrayList<>();
        lista.add(base);
        if (total == 1) {
            return lista;
        }
        Period intervalo = periodoDaFrequencia(frequenciaInterna());
        for (int i = 1; i < total; i++) {
            Period salto = intervalo.multipliedBy(i);
            LocalDate data = base.getData().plus(salto);
            LocalDate venc = base.getVencimento() == null ? data : base.getVencimento().plus(salto);
            lista.add(new Transacao(base.getTipo(), base.getDescricao(), base.getValor(),
                    base.getCategoria(), base.getConta(), data, venc,
                    Transacao.STATUS_PENDENTE, true, base.getFrequencia()));
        }
        return lista;
    }

    // =========================================================
    // Edição / limpeza
    // =========================================================

    public void preencherParaEdicao(Transacao t) {
        original = t;
        tipoCombo.setValue(Transacao.TIPO_RECEITA.equals(t.getTipo()) ? "Entrada" : "Saída");
        descricaoField.setText(t.getDescricao());
        valorField.setText(String.format("%.2f", t.getValor()).replace(".", ","));
        selectCategoria(t.getCategoria());
        selectConta(t.getConta());
        dataFuturaPicker.setValue(t.getVencimento() != null ? t.getVencimento() : LocalDate.now().plusDays(7));
        if (Transacao.STATUS_PAGO.equals(t.getStatus())) {
            quandoCombo.setValue(Transacao.TIPO_RECEITA.equals(t.getTipo()) ? RECEBIDA_AGORA : PAGA_AGORA);
        } else {
            quandoCombo.setValue(Transacao.TIPO_RECEITA.equals(t.getTipo()) ? A_RECEBER_DEPOIS : A_PAGAR_DEPOIS);
        }
        recorrenteCheck.setSelected(t.isRecorrente());
        if (t.getFrequencia() != null) {
            frequenciaCombo.setValue(frequenciaRotulo(t.getFrequencia()));
        }
        atualizarVisibilidadeData();
        atualizarVisibilidadeRecorrencia();
    }

    public void limpar() {
        original = null;
        tipoCombo.setValue("Entrada");
        descricaoField.clear();
        valorField.clear();
        dataFuturaPicker.setValue(LocalDate.now().plusDays(7));
        recorrenteCheck.setSelected(false);
        frequenciaCombo.setValue("Mensal");
        ocorrenciasSpinner.getValueFactory().setValue(12);
        selectCategoria(null);
        selectConta(null);
        quandoCombo.setValue(quandoCombo.getItems().isEmpty() ? null : quandoCombo.getItems().get(0));
        atualizarVisibilidadeData();
        atualizarVisibilidadeRecorrencia();
    }

    // =========================================================
    // Internos
    // =========================================================

    private void atualizarQuando() {
        String tipo = getTipo();
        if (Transacao.TIPO_RECEITA.equals(tipo)) {
            quandoCombo.setItems(FXCollections.observableArrayList(RECEBIDA_AGORA, A_RECEBER_DEPOIS));
            lblDataFutura.setText("Data de recebimento");
            lblAjudaData.setText("Use esta data para registrar algo que a empresa ainda vai receber.");
        } else if (Transacao.TIPO_DESPESA.equals(tipo)) {
            quandoCombo.setItems(FXCollections.observableArrayList(PAGA_AGORA, A_PAGAR_DEPOIS));
            lblDataFutura.setText("Data de vencimento");
            lblAjudaData.setText("Use esta data para registrar uma conta que ainda precisa ser paga.");
        } else {
            quandoCombo.getItems().clear();
        }
        if (!quandoCombo.getItems().isEmpty()) {
            quandoCombo.setValue(quandoCombo.getItems().get(0));
        }
        atualizarVisibilidadeData();
    }

    private void atualizarVisibilidadeData() {
        boolean depois = ehDepois();
        lblDataFutura.setVisible(depois);
        lblDataFutura.setManaged(depois);
        dataFuturaPicker.setVisible(depois);
        dataFuturaPicker.setManaged(depois);
        lblAjudaData.setVisible(depois);
        lblAjudaData.setManaged(depois);
    }

    private void atualizarVisibilidadeRecorrencia() {
        boolean rec = recorrenteCheck.isSelected();
        lblFrequencia.setVisible(rec);
        lblFrequencia.setManaged(rec);
        frequenciaCombo.setVisible(rec);
        frequenciaCombo.setManaged(rec);
        lblOcorrencias.setVisible(rec);
        lblOcorrencias.setManaged(rec);
        ocorrenciasSpinner.setVisible(rec);
        ocorrenciasSpinner.setManaged(rec);
    }

    private void recarregarCategorias() {
        String tipo = getTipo();
        if (tipo == null) {
            categoriaCombo.getItems().clear();
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

    private String frequenciaInterna() {
        String v = frequenciaCombo.getValue();
        return v == null ? "MENSAL" : v.toUpperCase(Locale.ROOT);
    }

    private String frequenciaRotulo(String interna) {
        if (interna == null || interna.isBlank()) {
            return "Mensal";
        }
        return interna.substring(0, 1) + interna.substring(1).toLowerCase(Locale.ROOT);
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
}