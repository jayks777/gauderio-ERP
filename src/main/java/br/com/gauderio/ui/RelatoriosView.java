package br.com.gauderio.ui;

import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.Formatador;
import br.com.gauderio.util.UiUtil;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Gráficos e relatórios para análise financeira. */
public class RelatoriosView extends BorderPane implements Refreshable {

    private final TransacaoDAO transacaoDAO = new TransacaoDAO();

    private final Spinner<Integer> anoSpan = new Spinner<>(2000, 2100, LocalDate.now().getYear());
    private final ComboBox<String> tipoCombo = new ComboBox<>(
            FXCollections.observableArrayList("Todas", Transacao.TIPO_RECEITA, Transacao.TIPO_DESPESA));

    private final BarChart<String, Number> barra = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final LineChart<String, Number> linha = new LineChart<>(new CategoryAxis(), new NumberAxis());
    private final PieChart pizza = new PieChart();
    private final TableView<LinhaMensal> tabela = new TableView<>();

    public RelatoriosView() {
        setPadding(new Insets(24));
        getStyleClass().add("content");

        anoSpan.setPrefWidth(110);
        tipoCombo.setValue("Todas");

        Button atualizar = new Button("Gerar relatório");
        atualizar.getStyleClass().addAll("btn", "btn-green");
        atualizar.setOnAction(e -> carregar());

        HBox filtros = new HBox(10,
                new Label("Ano:"), anoSpan,
                new Label("Tipo:"), tipoCombo,
                atualizar);
        filtros.setAlignment(Pos.CENTER_LEFT);
        filtros.getStyleClass().add("filter-bar");

        VBox cabecalho = new VBox(4,
                UiUtil.cabecalho("Relatórios e gráficos",
                        "Análise financeira de lançamentos pagos/recebidos no ano."),
                filtros);

        barra.setTitle("Receitas × Despesas por mês");
        barra.setPrefHeight(300);
        linha.setTitle("Saldo acumulado no ano");
        linha.setPrefHeight(300);
        pizza.setTitle("Distribuição (por categoria)");
        pizza.setPrefHeight(300);

        HBox graficos = new HBox(18, wrapCard(barra), wrapCard(linha));
        HBox graficos2 = new HBox(18, wrapCard(pizza), wrapCard(createTabelaCard()));
        HBox.setHgrow(graficos, Priority.ALWAYS);
        HBox.setHgrow(graficos2, Priority.ALWAYS);

        VBox area = new VBox(16, cabecalho, graficos, graficos2);
        setCenter(area);

        carregar();
    }

    @Override
    public void refresh() {
        carregar();
    }

    private VBox wrapCard(javafx.scene.Node node) {
        VBox card = new VBox(node);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));
        card.setPrefWidth(520);
        return card;
    }

    private void carregar() {
        int ano = anoSpan.getValue();
        LocalDate inicio = LocalDate.of(ano, 1, 1);
        LocalDate fim = LocalDate.of(ano, 12, 31);
        String tipoFiltro = tipoCombo.getValue() == null ? "Todas" : tipoCombo.getValue();

        List<Transacao> pagas = transacaoDAO.pagasEntre(inicio, fim);
        String[] meses = {"JAN", "FEV", "MAR", "ABR", "MAI", "JUN",
                "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"};

        double[] receitas = new double[12];
        double[] despesas = new double[12];
        Map<String, Double> porCategoria = new LinkedHashMap<>();
        double totalReceitas = 0;
        double totalDespesas = 0;

        for (Transacao t : pagas) {
            if (!"Todas".equals(tipoFiltro) && !tipoFiltro.equals(t.getTipo())) {
                continue;
            }
            int mes = t.getData().getMonthValue() - 1;
            if (Transacao.TIPO_RECEITA.equals(t.getTipo())) {
                receitas[mes] += t.getValor();
                totalReceitas += t.getValor();
            } else {
                despesas[mes] += t.getValor();
                totalDespesas += t.getValor();
            }
            String cat = t.getCategoria() == null || t.getCategoria().isBlank()
                    ? "Sem categoria" : t.getCategoria();
            porCategoria.merge(cat, t.getValor(), Double::sum);
        }

        barra.getData().clear();
        XYChart.Series<String, Number> serieReceitas = new XYChart.Series<>();
        serieReceitas.setName("Receitas");
        XYChart.Series<String, Number> serieDespesas = new XYChart.Series<>();
        serieDespesas.setName("Despesas");
        for (int i = 0; i < 12; i++) {
            serieReceitas.getData().add(new XYChart.Data<>(meses[i], receitas[i]));
            serieDespesas.getData().add(new XYChart.Data<>(meses[i], despesas[i]));
        }
        barra.getData().addAll(serieReceitas, serieDespesas);

        linha.getData().clear();
        XYChart.Series<String, Number> serieAcumulo = new XYChart.Series<>();
        serieAcumulo.setName("Saldo acumulado");
        double acumulado = 0;
        for (int i = 0; i < 12; i++) {
            acumulado += receitas[i] - despesas[i];
            serieAcumulo.getData().add(new XYChart.Data<>(meses[i], acumulado));
        }
        linha.getData().add(serieAcumulo);

        pizza.getData().clear();
        if ("Todas".equals(tipoFiltro)) {
            pizza.getData().add(new PieChart.Data("Receitas (" + Formatador.moeda(totalReceitas) + ")", totalReceitas));
            pizza.getData().add(new PieChart.Data("Despesas (" + Formatador.moeda(totalDespesas) + ")", totalDespesas));
        } else {
            porCategoria.forEach((categoria, valor) ->
                    pizza.getData().add(new PieChart.Data(categoria + " (" + Formatador.moeda(valor) + ")", valor)));
        }

        List<LinhaMensal> linhas = new java.util.ArrayList<>();
        double acum = 0;
        for (int i = 0; i < 12; i++) {
            acum += receitas[i] - despesas[i];
            linhas.add(new LinhaMensal(meses[i], receitas[i], despesas[i], receitas[i] - despesas[i], acum));
        }
        tabela.getItems().setAll(linhas);
        tabela.refresh();
    }

    private VBox createTabelaCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));
        card.setPrefWidth(520);

        Label titulo = new Label("Relatório mensal do ano");
        titulo.getStyleClass().add("card-label");

        TableColumn<LinhaMensal, String> cMes = new TableColumn<>("Mês");
        cMes.setCellValueFactory(new PropertyValueFactory<>("mes"));

        TableColumn<LinhaMensal, Double> cRec = colunaMoeda("Receitas", "receita");
        TableColumn<LinhaMensal, Double> cDesp = colunaMoeda("Despesas", "despesa");
        TableColumn<LinhaMensal, Double> cRes = colunaMoeda("Resultado", "resultado");
        TableColumn<LinhaMensal, Double> cAcum = colunaMoeda("Acumulado", "acumulado");

        tabela.getColumns().add(cMes);
        tabela.getColumns().add(cRec);
        tabela.getColumns().add(cDesp);
        tabela.getColumns().add(cRes);
        tabela.getColumns().add(cAcum);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.setPrefHeight(300);

        card.getChildren().addAll(titulo, tabela);
        return card;
    }

    private TableColumn<LinhaMensal, Double> colunaMoeda(String rotulo, String prop) {
        TableColumn<LinhaMensal, Double> col = new TableColumn<>(rotulo);
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Formatador.moeda(item));
                setStyle(empty ? "" : (item != null && item >= 0
                        ? "-fx-text-fill:#0E7A3C;"
                        : "-fx-text-fill:#C8102E;"));
            }
        });
        return col;
    }

    /** Linha do relatório mensal. */
    public static class LinhaMensal {
        private final String mes;
        private final double receita;
        private final double despesa;
        private final double resultado;
        private final double acumulado;

        public LinhaMensal(String mes, double receita, double despesa, double resultado, double acumulado) {
            this.mes = mes;
            this.receita = receita;
            this.despesa = despesa;
            this.resultado = resultado;
            this.acumulado = acumulado;
        }

        public String getMes() { return mes; }
        public double getReceita() { return receita; }
        public double getDespesa() { return despesa; }
        public double getResultado() { return resultado; }
        public double getAcumulado() { return acumulado; }
    }
}