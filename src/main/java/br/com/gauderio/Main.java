package br.com.gauderio;

import br.com.gauderio.db.Database;
import br.com.gauderio.ui.ConfiguracoesView;
import br.com.gauderio.ui.ContasParaView;
import br.com.gauderio.ui.ContasView;
import br.com.gauderio.ui.DashboardView;
import br.com.gauderio.ui.MovimentacoesView;
import br.com.gauderio.ui.Refreshable;
import br.com.gauderio.ui.RelatoriosView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Gauderio-ERP — sistema financeiro em JavaFX com SQLite.
 * Desenvolvido por Jaykson Bolico.
 *
 * Navegação organizada por seções para facilitar o uso por quem
 * não tem experiência com sistemas financeiros.
 */
public class Main extends Application {

    private BorderPane root;
    private BorderPane areaCentral;
    private Label tituloPagina;
    private final Map<String, Button> botoesMenu = new LinkedHashMap<>();
    private final Map<Node, Button> botaoPorView = new LinkedHashMap<>();

    private DashboardView dashboardView;
    private MovimentacoesView movimentacoesView;
    private ContasParaView contasPagarView;
    private ContasParaView contasReceberView;
    private ContasView contasView;
    private RelatoriosView relatoriosView;
    private ConfiguracoesView configuracoesView;

    @Override
    public void start(Stage stage) {
        Database.init();

        Runnable onDadosAlterados = this::recarregarPainelFinanceiro;

        root = new BorderPane();
        dashboardView = new DashboardView(
                () -> abrirRegistrar(true),
                () -> abrirRegistrar(false),
                () -> navegarPara(contasPagarView, "Contas a pagar"),
                () -> navegarPara(contasReceberView, "Contas a receber"));
        movimentacoesView = new MovimentacoesView(onDadosAlterados);
        contasPagarView = new ContasParaView(false, onDadosAlterados);
        contasReceberView = new ContasParaView(true, onDadosAlterados);
        contasView = new ContasView(onDadosAlterados);
        relatoriosView = new RelatoriosView();
        configuracoesView = new ConfiguracoesView();

        areaCentral = new BorderPane();
        areaCentral.setTop(criarCabecalho());
        areaCentral.setBottom(criarRodape());
        areaCentral.setCenter(criarAreaRolavel(dashboardView));

        root.setLeft(criarSidebar());
        root.setCenter(areaCentral);

        Scene scene = new Scene(root, 1180, 740);
        scene.getStylesheets().add(
                getClass().getResource("/br/com/gauderio/style.css").toExternalForm());

        stage.setTitle("Gauderio-ERP · Finanças");
        stage.setMinWidth(1024);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    // =========================================================
    // MENU LATERAL
    // =========================================================
    private Node criarSidebar() {
        VBox menu = new VBox(8);
        menu.setPadding(new Insets(20, 14, 20, 14));
        menu.setPrefWidth(235);
        menu.getStyleClass().add("sidebar");

        Label logo = new Label("GAUDERIO");
        logo.getStyleClass().add("logo");

        Label tagline = new Label("ERP FINANCEIRO · RS");
        tagline.getStyleClass().add("logo-subtitle");

        menu.getChildren().addAll(new VBox(2, logo, tagline), faixaTricolor(), criarSeparador());

        adicionarSecao(menu, "INÍCIO");
        adicionarBotaoMenu(menu, "Dashboard", "Dashboard", "icon-dashboard", dashboardView);

        adicionarSecao(menu, "MOVIMENTAÇÕES");
        adicionarBotaoMenu(menu, "Movimentações", "Registrar entrada", "icon-entrada", movimentacoesView,
                () -> movimentacoesView.mostrarRegistro(true));
        adicionarBotaoMenu(menu, "Movimentações", "Registrar saída", "icon-saida", movimentacoesView,
                () -> movimentacoesView.mostrarRegistro(false));
        adicionarBotaoMenu(menu, "Movimentações", "Histórico", "icon-historico", movimentacoesView,
                () -> movimentacoesView.mostrarHistorico());

        adicionarSecao(menu, "CONTAS A PAGAR");
        adicionarBotaoMenu(menu, "Contas a pagar", "Contas a pagar", "icon-pagar", contasPagarView);

        adicionarSecao(menu, "CONTAS A RECEBER");
        adicionarBotaoMenu(menu, "Contas a receber", "Contas a receber", "icon-receber", contasReceberView);

        adicionarSecao(menu, "BANCOS E RELATÓRIOS");
        adicionarBotaoMenu(menu, "Contas bancárias", "Contas bancárias", "icon-banco", contasView);
        adicionarBotaoMenu(menu, "Relatórios", "Relatórios", "icon-relatorios", relatoriosView);

        adicionarSecao(menu, "CONFIGURAÇÕES");
        adicionarBotaoMenu(menu, "Configurações", "Categorias", "icon-categorias", configuracoesView,
                () -> configuracoesView.selecionarCategorias());
        adicionarBotaoMenu(menu, "Configurações", "Sobre", "icon-sobre", configuracoesView,
                () -> configuracoesView.selecionarSobre());

        return menu;
    }

    private void adicionarSecao(VBox menu, String titulo) {
        Label secao = new Label(titulo);
        secao.getStyleClass().add("menu-section");
        menu.getChildren().add(secao);
    }

    private void adicionarBotaoMenu(VBox menu, String tituloPagina, String rotulo, String iconeClasse, Node view) {
        adicionarBotaoMenu(menu, tituloPagina, rotulo, iconeClasse, view, null);
    }

    private void adicionarBotaoMenu(VBox menu, String tituloPagina, String rotulo, String iconeClasse, Node view,
            Runnable extra) {
        Button botao = new Button(rotulo);
        botao.setMaxWidth(Double.MAX_VALUE);
        botao.setAlignment(Pos.CENTER_LEFT);
        botao.getStyleClass().add("menu-button");
        botao.setGraphic(criarIconeMenu(iconeClasse));
        botao.setGraphicTextGap(10);
        botao.setOnAction(e -> {
            navegar(tituloPagina, botao, view);
            if (extra != null) {
                extra.run();
            }
        });
        botoesMenu.put(rotulo, botao);
        botaoPorView.putIfAbsent(view, botao);
        menu.getChildren().add(botao);
    }

    private Region criarIconeMenu(String iconeClasse) {
        Region icone = new Region();
        icone.getStyleClass().addAll("menu-icon", iconeClasse);
        icone.setMinSize(16, 16);
        icone.setPrefSize(16, 16);
        icone.setMaxSize(16, 16);
        return icone;
    }

    // =========================================================
    // NAVEGAÇÃO
    // =========================================================
    /** Navegação programática (ex.: botões de "Ações rápidas" do Dashboard). */
    private void navegarPara(Node view, String titulo) {
        Button botao = botaoPorView.get(view);
        if (botao != null) {
            navegar(titulo, botao, view);
        } else {
            tituloPagina.setText(titulo);
            if (view instanceof Refreshable r) {
                r.refresh();
            }
            areaCentral.setCenter(criarAreaRolavel(view));
        }
    }

    private void navegar(String titulo, Button botao, Node view) {
        tituloPagina.setText(titulo);
        botoesMenu.values().forEach(b -> b.getStyleClass().remove("active"));
        botao.getStyleClass().add("active");
        if (view instanceof Refreshable r) {
            r.refresh(); // recarrega os dados do banco ao abrir a tela
        }
        areaCentral.setCenter(criarAreaRolavel(view));
    }

    private void abrirRegistrar(boolean entrada) {
        navegarPara(movimentacoesView, "Movimentações");
        movimentacoesView.mostrarRegistro(entrada);
    }

    private ScrollPane criarAreaRolavel(Node conteudo) {
        ScrollPane scroll = new ScrollPane(conteudo);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.getStyleClass().add("content-scroll");
        return scroll;
    }

    private void recarregarPainelFinanceiro() {
        dashboardView.refresh();
        movimentacoesView.refresh();
        contasPagarView.refresh();
        contasReceberView.refresh();
        contasView.refresh();
    }

    // =========================================================
    // ELEMENTOS VISUAIS (faixa tricolor, cabeçalho e rodapé)
    // =========================================================
    private HBox faixaTricolor() {
        HBox faixa = new HBox();
        faixa.setPrefHeight(6);
        faixa.setMaxWidth(Double.MAX_VALUE);
        faixa.getStyleClass().add("rs-strip");
        faixa.getChildren().addAll(
                trecho("rs-green"), trecho("rs-red"), trecho("rs-yellow"));
        return faixa;
    }

    private Region trecho(String cor) {
        Region r = new Region();
        r.getStyleClass().add(cor);
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private Region criarSeparador() {
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setMaxWidth(Double.MAX_VALUE);
        sep.getStyleClass().add("separator");
        return sep;
    }

    // =========================================================
    // CABEÇALHO
    // =========================================================

    private Node criarCabecalho() {
        tituloPagina = new Label("Dashboard");
        tituloPagina.getStyleClass().add("page-title");

        Label usuario = new Label("Jaykson Bolico · Desenvolvedor");
        usuario.getStyleClass().add("user-label");

        BorderPane header = new BorderPane();
        header.setLeft(tituloPagina);
        header.setRight(usuario);
        header.setPadding(new Insets(18, 28, 18, 28));
        header.getStyleClass().add("header");
        return header;
    }

    // =========================================================
    // RODAPÉ
    // =========================================================

    private Node criarRodape() {
        HBox rodape = new HBox();
        rodape.setAlignment(Pos.CENTER);
        rodape.setPadding(new Insets(10, 24, 10, 24));
        rodape.getStyleClass().add("footer");

        Label assinatura = new Label("Gauderio-ERP · Desenvolvido por Jaykson Bolico · © 2026");
        assinatura.getStyleClass().add("footer-text");
        rodape.getChildren().add(assinatura);
        return rodape;
    }

    public static void main(String[] args) {
        launch();
    }
}