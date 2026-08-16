package br.com.gauderio;

import br.com.gauderio.db.Database;
import br.com.gauderio.ui.BoletosView;
import br.com.gauderio.ui.ConfiguracoesView;
import br.com.gauderio.ui.ContasView;
import br.com.gauderio.ui.DashboardView;
import br.com.gauderio.ui.FinanceiroView;
import br.com.gauderio.ui.Refreshable;
import br.com.gauderio.ui.RelatoriosView;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
 */
public class Main extends Application {

    private BorderPane root;
    private BorderPane areaCentral;
    private Label tituloPagina;
    private final Map<String, Button> botoesMenu = new LinkedHashMap<>();

    @Override
    public void start(Stage stage) {
        Database.init();

        root = new BorderPane();

        areaCentral = new BorderPane();
        areaCentral.setTop(criarCabecalho());
        areaCentral.setBottom(criarRodape());
        areaCentral.setCenter(new DashboardView());

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

        adicionarBotaoMenu(menu, "Dashboard", "📊", new DashboardView());
        adicionarBotaoMenu(menu, "Financeiro", "💵", new FinanceiroView());
        adicionarBotaoMenu(menu, "Contas bancárias", "🏦", new ContasView());
        adicionarBotaoMenu(menu, "Central de boletos", "🧾", new BoletosView());
        adicionarBotaoMenu(menu, "Relatórios", "📈", new RelatoriosView());
        adicionarBotaoMenu(menu, "Configurações", "⚙️", new ConfiguracoesView());

        return menu;
    }

    private void adicionarBotaoMenu(VBox menu, String rotulo, String icone, Node view) {
        String nome = rotulo.substring(rotulo.indexOf(' ') + 1);
        Button botao = new Button(icone + "  " + rotulo);
        botao.setMaxWidth(Double.MAX_VALUE);
        botao.setAlignment(Pos.CENTER_LEFT);
        botao.getStyleClass().add("menu-button");
        botao.setOnAction(e -> navegar(rotulo, botao, view));
        botoesMenu.put(nome, botao);
        menu.getChildren().add(botao);
    }

    private void navegar(String titulo, Button botao, Node view) {
        tituloPagina.setText(titulo);
        botoesMenu.values().forEach(b -> b.getStyleClass().remove("active"));
        botao.getStyleClass().add("active");
        if (view instanceof Refreshable r) {
            r.refresh(); // recarrega os dados do banco ao abrir a tela
        }
        areaCentral.setCenter(view);
    }

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