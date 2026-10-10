package br.com.gauderio.ui;

import br.com.gauderio.dao.ContaDAO;
import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.Formatador;
import br.com.gauderio.util.IconeUtil;
import br.com.gauderio.util.UiUtil;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Dashboard: principal central de operações do Gauderio-ERP.
 * Mostra, com clareza imediata: saldo disponível, receitas e despesas do mês,
 * resultado do período, contas a pagar e receber, contas em atraso, ações rápidas,
 * próximos vencimentos e histórico recente.
 */
public class DashboardView extends VBox implements Refreshable {

    private final TransacaoDAO transacaoDAO = new TransacaoDAO();
    private final ContaDAO contaDAO = new ContaDAO();
    private final Runnable onRegistrarEntrada;
    private final Runnable onRegistrarSaida;
    private final Runnable onContasPagar;
    private final Runnable onContasReceber;

    public DashboardView() {
        this(() -> {}, () -> {}, () -> {}, () -> {});
    }

    public DashboardView(Runnable onRegistrarEntrada, Runnable onRegistrarSaida,
                         Runnable onContasPagar, Runnable onContasReceber) {
        this.onRegistrarEntrada = onRegistrarEntrada == null ? () -> {} : onRegistrarEntrada;
        this.onRegistrarSaida = onRegistrarSaida == null ? () -> {} : onRegistrarSaida;
        this.onContasPagar = onContasPagar == null ? () -> {} : onContasPagar;
        this.onContasReceber = onContasReceber == null ? () -> {} : onContasReceber;
        setPadding(new Insets(24));
        setSpacing(18);
        getStyleClass().add("content");
        gerar();
    }

    @Override
    public void refresh() {
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
        double vencidasPagar = transacaoDAO.somaVencido(Transacao.TIPO_DESPESA);
        double vencidasReceber = transacaoDAO.somaVencido(Transacao.TIPO_RECEITA);

        long qtdVencidasPagar = transacaoDAO.pendentes().stream()
                .filter(Transacao::isDespesa)
                .filter(Transacao::isVencida)
                .count();

        VBox cabecalho = UiUtil.cabecalho("Dashboard",
                "Visão geral das finanças da sua empresa · " + Formatador.mesAno(hoje));

        double resultado = receitasMes - despesasMes;
        String descResultado = resultado >= 0 ? "Superávit (Receitas > Despesas)" : "Déficit (Despesas > Receitas)";

        FlowPane cards = new FlowPane(14, 14);
        cards.getChildren().addAll(
                criarCard("Saldo disponível", Formatador.moeda(saldoContas), "blue", "Soma de todas as contas bancárias"),
                criarCard("Receitas do mês", Formatador.moeda(receitasMes), "green", "Entradas recebidas em " + Formatador.mesAno(hoje)),
                criarCard("Despesas do mês", Formatador.moeda(despesasMes), "red", "Saídas pagas em " + Formatador.mesAno(hoje)),
                criarCard("Resultado do mês", Formatador.moeda(resultado), resultado >= 0 ? "green" : "red", descResultado),
                criarCard("A receber", Formatador.moeda(aReceber), "green", "Valores pendentes de recebimento"),
                criarCard("A pagar", Formatador.moeda(aPagar), "yellow", "Contas pendentes de pagamento"),
                criarCard("Atrasadas (a pagar)", Formatador.moeda(vencidasPagar), "red",
                        qtdVencidasPagar + (qtdVencidasPagar == 1 ? " conta a pagar vencida" : " contas a pagar vencidas")));

        getChildren().addAll(cabecalho, cards, criarAcoesRapidas(), criarProximosVencimentos(), criarUltimas());
    }

    // =========================================================
    // CARDS DE INDICADORES
    // =========================================================
    private VBox criarCard(String rotulo, String valor, String cor, String subTexto) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(0));
        card.setPrefWidth(240);

        Region faixa = new Region();
        faixa.setPrefHeight(6);
        faixa.setMaxWidth(Double.MAX_VALUE);
        faixa.getStyleClass().add("accent-" + cor);

        Label lRotulo = new Label(rotulo.toUpperCase());
        lRotulo.getStyleClass().add("card-label");
        lRotulo.setPadding(new Insets(10, 12, 0, 12));

        Label lValor = new Label(valor);
        lValor.getStyleClass().add("card-value");
        lValor.setPadding(new Insets(0, 12, 0, 12));

        VBox textos = new VBox(2, lRotulo, lValor);
        if (subTexto != null && !subTexto.isBlank()) {
            Label sub = new Label(subTexto);
            sub.getStyleClass().add("card-sub");
            sub.setPadding(new Insets(0, 12, 10, 12));
            textos.getChildren().add(sub);
        } else {
            textos.setPadding(new Insets(0, 0, 10, 0));
        }

        card.getChildren().addAll(faixa, textos);
        return card;
    }

    private VBox criarAcoesRapidas() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label titulo = new Label("AÇÕES RÁPIDAS");
        titulo.getStyleClass().add("card-label");

        Button btnEntrada = new Button("+ Registrar entrada", IconeUtil.iconeBotao(IconeUtil.PATH_ENTRADA, "#ffffff"));
        btnEntrada.getStyleClass().addAll("btn", "btn-green");
        btnEntrada.setOnAction(e -> onRegistrarEntrada.run());

        Button btnSaida = new Button("- Registrar saída", IconeUtil.iconeBotao(IconeUtil.PATH_SAIDA, "#ffffff"));
        btnSaida.getStyleClass().addAll("btn", "btn-red");
        btnSaida.setOnAction(e -> onRegistrarSaida.run());

        Button btnPagar = new Button("Contas a pagar", IconeUtil.iconeBotao(IconeUtil.PATH_PAGAR, "#41505c"));
        btnPagar.getStyleClass().addAll("btn", "btn-outline");
        btnPagar.setOnAction(e -> onContasPagar.run());

        Button btnReceber = new Button("Contas a receber", IconeUtil.iconeBotao(IconeUtil.PATH_RECEBER, "#41505c"));
        btnReceber.getStyleClass().addAll("btn", "btn-outline");
        btnReceber.setOnAction(e -> onContasReceber.run());

        HBox botoes = new HBox(10, btnEntrada, btnSaida, btnPagar, btnReceber);
        botoes.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(titulo, botoes);
        return card;
    }

    // =========================================================
    // PRÓXIMOS VENCIMENTOS
    // =========================================================
    private VBox criarProximosVencimentos() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label titulo = new Label("PRÓXIMOS VENCIMENTOS (7 DIAS)");
        titulo.getStyleClass().add("card-label");

        List<Transacao> proximos = transacaoDAO.proximosVencimentos(LocalDate.now().plusDays(7), 8);

        TableView<Transacao> tabela = new TableView<>();
        tabela.setMaxHeight(250);

        TableColumn<Transacao, String> cVenc = new TableColumn<>("Vencimento");
        cVenc.setCellValueFactory(d -> new ReadOnlyStringWrapper(Formatador.data(d.getValue().getDataReferencia())));
        cVenc.setMinWidth(100);

        TableColumn<Transacao, String> cPrazo = new TableColumn<>("Prazo");
        cPrazo.setCellValueFactory(d -> {
            LocalDate ref = d.getValue().getDataReferencia();
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), ref);
            if (dias == 0) return new ReadOnlyStringWrapper("Hoje");
            if (dias > 0) return new ReadOnlyStringWrapper("Em " + dias + " d");
            return new ReadOnlyStringWrapper(Math.abs(dias) + " d atraso");
        });
        cPrazo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("atraso") || item.equals("Hoje")) {
                        setStyle("-fx-text-fill: #C8102E; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #5b6c7a;");
                    }
                }
            }
        });
        cPrazo.setMinWidth(90);

        TableColumn<Transacao, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTipoTexto()));
        cTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                    setStyle("");
                } else {
                    boolean entrada = "Entrada".equalsIgnoreCase(item);
                    setText(entrada ? "▲ Entrada" : "▼ Saída");
                    setStyle("-fx-text-fill:" + (entrada ? "#0E7A3C" : "#C8102E") + "; -fx-font-weight:bold;");
                }
            }
        });
        cTipo.setMinWidth(95);

        TableColumn<Transacao, String> cDesc = new TableColumn<>("Descrição");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        cDesc.setMinWidth(240);

        TableColumn<Transacao, Double> cValor = new TableColumn<>("Valor");
        cValor.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getValor()));
        cValor.setCellFactory(col -> new TableCell<>() {
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
                    setStyle("-fx-text-fill:" + (entrada ? "#0E7A3C" : "#C8102E") + "; -fx-font-weight:bold;");
                }
            }
        });
        cValor.setMinWidth(120);

        TableColumn<Transacao, String> cSituacao = new TableColumn<>("Situação");
        cSituacao.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStatusTexto()));
        cSituacao.setCellFactory(col -> new TableCell<>() {
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
        cSituacao.setMinWidth(100);

        tabela.getColumns().add(cVenc);
        tabela.getColumns().add(cPrazo);
        tabela.getColumns().add(cTipo);
        tabela.getColumns().add(cDesc);
        tabela.getColumns().add(cValor);
        tabela.getColumns().add(cSituacao);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.getItems().setAll(proximos);

        card.getChildren().add(titulo);
        if (proximos.isEmpty()) {
            Label vazio = new Label("Nenhuma conta vence nos próximos 7 dias. Tudo em dia!");
            vazio.getStyleClass().add("empty-text");
            card.getChildren().add(vazio);
        } else {
            card.getChildren().add(tabela);
        }
        return card;
    }

    // =========================================================
    // ÚLTIMAS MOVIMENTAÇÕES
    // =========================================================
    private VBox criarUltimas() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label titulo = new Label("ÚLTIMAS MOVIMENTAÇÕES REGISTRADAS");
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
        cData.setMinWidth(95);

        TableColumn<Transacao, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getTipoTexto()));
        cTipo.setCellFactory(col -> new TableCell<>() {
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
        cTipo.setMinWidth(95);

        TableColumn<Transacao, String> cDesc = new TableColumn<>("Descrição");
        cDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        cDesc.setMinWidth(240);

        TableColumn<Transacao, Double> cValor = new TableColumn<>("Valor");
        cValor.setCellValueFactory(d -> new SimpleObjectProperty<>(d.getValue().getValor()));
        cValor.setCellFactory(col -> new TableCell<>() {
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
                    setStyle("-fx-text-fill:" + (entrada ? "#0E7A3C" : "#C8102E") + "; -fx-font-weight:bold;");
                }
            }
        });
        cValor.setMinWidth(120);

        TableColumn<Transacao, String> cSituacao = new TableColumn<>("Situação");
        cSituacao.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().getStatusTexto()));
        cSituacao.setCellFactory(col -> new TableCell<>() {
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
        cSituacao.setMinWidth(100);

        tabela.getColumns().add(cData);
        tabela.getColumns().add(cTipo);
        tabela.getColumns().add(cDesc);
        tabela.getColumns().add(cValor);
        tabela.getColumns().add(cSituacao);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabela.getItems().setAll(ultimas);

        card.getChildren().addAll(titulo, tabela);
        return card;
    }
}
