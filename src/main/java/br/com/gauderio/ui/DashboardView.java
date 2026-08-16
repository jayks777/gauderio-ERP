package br.com.gauderio.ui;

import br.com.gauderio.dao.ContaDAO;
import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.Formatador;
import br.com.gauderio.util.UiUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tela inicial com visão geral: saldos, entradas, saídas, pendências e gráfico. */
public class DashboardView extends VBox implements Refreshable {

    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final ContaDAO contaDAO = new ContaDAO();

    public DashboardView() {
        setPadding(new Insets(24));
        setSpacing(18);
        getStyleClass().add("content");
        gerar();
    }

    private void gerar() {
        getChildren().clear();

        LocalDate hoje = LocalDate.now();
        YearMonth mesAtual = YearMonth.now();
        LocalDate ini = mesAtual.atDay(1);
        LocalDate fim = mesAtual.atEndOfMonth();

        double receitasMes = transacaoDAO.somaPagaEntre(Transacao.TIPO_RECEITA, ini, fim);
        double despesasMes = transacaoDAO.somaPagaEntre(Transacao.TIPO_DESPESA, ini, fim);
        double saldoContas = contaDAO.saldoGeral();
        double aReceber = transacaoDAO.somaPendente(Transacao.TIPO_RECEITA);
        double aPagar = transacaoDAO.somaPendente(Transacao.TIPO_DESPESA);

        VBox cabecalho = UiUtil.cabecalho("Dashboard", "Visão geral das finanças · " + Formatador.data(hoje));

        FlowPane cards = new FlowPane(14, 14);
        cards.getChildren().addAll(
                criarCard("Saldo nas contas", Formatador.moeda(saldoContas), "blue"),
                criarCard("Receitas do mês", Formatador.moeda(receitasMes), "green"),
                criarCard("Despesas do mês", Formatador.moeda(despesasMes), "red"),
                criarCard("Saldo do mês", Formatador.moeda(receitasMes - despesasMes),
                        receitasMes - despesasMes >= 0 ? "green" : "red"),
                criarCard("A receber", Formatador.moeda(aReceber), "yellow"),
                criarCard("A pagar", Formatador.moeda(aPagar), "yellow")
        );

        VBox pie = criarCardDespesas(ini, fim);
        VBox ultimas = criarCardUltimas();
        HBox.setHgrow(pie, Priority.ALWAYS);
        HBox.setHgrow(ultimas, Priority.ALWAYS);

        HBox graficos = new HBox(18, pie, ultimas);
        graficos.setAlignment(Pos.TOP_LEFT);

        getChildren().addAll(cabecalho, cards, graficos);
    }

    @Override
    public void refresh() {
        gerar();
    }

    private VBox criarCard(String rotulo, String valor, String cor) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Region faixa = new Region();
        faixa.setPrefHeight(6);
        faixa.setMaxWidth(Double.MAX_VALUE);
        faixa.getStyleClass().add("accent-" + cor);

        Label lRotulo = new Label(rotulo.toUpperCase());
        lRotulo.getStyleClass().add("card-label");

        Label lValor = new Label(valor);
        lValor.getStyleClass().add("card-value");

        card.setPrefWidth(190);
        card.getChildren().addAll(faixa, lRotulo, lValor);
        return card;
    }

    private VBox criarCardDespesas(LocalDate inicio, LocalDate fim) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label titulo = new Label("Despesas do mês por categoria");
        titulo.getStyleClass().add("card-label");

        PieChart pie = new PieChart();
        pie.setPrefHeight(300);
        pie.setTitle("");

        Map<String, Double> porCategoria = new LinkedHashMap<>();
        for (Transacao t : transacaoDAO.pagasEntre(inicio, fim)) {
            if (Transacao.TIPO_DESPESA.equals(t.getTipo())) {
                String cat = t.getCategoria() == null || t.getCategoria().isBlank() ? "Sem categoria" : t.getCategoria();
                porCategoria.merge(cat, t.getValor(), Double::sum);
            }
        }

        if (porCategoria.isEmpty()) {
            Label vazio = new Label("Nenhuma despesa registrada no mês.");
            vazio.getStyleClass().add("empty-text");
            card.getChildren().addAll(titulo, vazio);
            return card;
        }

        for (Map.Entry<String, Double> e : porCategoria.entrySet()) {
            pie.getData().add(new PieChart.Data(e.getKey() + " · " + Formatador.moeda(e.getValue()), e.getValue()));
        }

        card.getChildren().addAll(titulo, pie);
        return card;
    }

    private VBox criarCardUltimas() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label titulo = new Label("Últimos lançamentos");
        titulo.getStyleClass().add("card-label");

        List<Transacao> ultimas = transacaoDAO.ultimos(10);

        TableView<Transacao> tabela = new TableView<>();
        tabela.setMaxHeight(300);

        TableColumn<Transacao, LocalDate> cData = new TableColumn<>("Data");
        cData.setCellValueFactory(new PropertyValueFactory<>("data"));
        cData.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Formatador.data(item));
            }
        });

        TableColumn<Transacao, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        cTipo.setMinWidth(90);

        TableColumn<Transacao, String> cDesc = new TableColumn<>("Descrição");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        cDesc.setMinWidth(150);

        TableColumn<Transacao, Double> cValor = new TableColumn<>("Valor");
        cValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        cValor.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : Formatador.moeda(item));
                setStyle(empty ? "" : (item != null && item >= 0 ? "-fx-text-fill:#0E7A3C;" : "-fx-text-fill:#C8102E;"));
            }
        });

        TableColumn<Transacao, String> cStatus = new TableColumn<>("Status");
        cStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tabela.getColumns().add(cData);
        tabela.getColumns().add(cTipo);
        tabela.getColumns().add(cDesc);
        tabela.getColumns().add(cValor);
        tabela.getColumns().add(cStatus);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.getItems().setAll(ultimas);
        card.getChildren().add(titulo);
        card.getChildren().add(tabela);
        return card;
    }
}