package br.com.gauderio.ui;

import br.com.gauderio.dao.CategoriaDAO;
import br.com.gauderio.model.Categoria;
import br.com.gauderio.util.UiUtil;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Configurações: categorias de entradas/saídas e informações do sistema (Sobre). */
public class ConfiguracoesView extends BorderPane implements Refreshable {

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    private int idCategoriaEmEdicao = -1;

    private final TextField nomeField = new TextField();
    private final ComboBox<String> tipoCombo = new ComboBox<>(
            FXCollections.observableArrayList("Receita", "Despesa"));
    private final TableView<Categoria> tabela = new TableView<>();

    private final TabPane abas = new TabPane();
    private final Tab abaCategorias = new Tab("Categorias");
    private final Tab abaSobre = new Tab("Sobre");

    public ConfiguracoesView() {
        setPadding(new Insets(24));
        getStyleClass().add("content");

        abas.getStyleClass().add("conta-tabs");
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        abas.getTabs().addAll(abaCategorias, abaSobre);
        abaCategorias.setContent(criarConteudoCategorias());
        abaSobre.setContent(criarConteudoSobre());

        setCenter(abas);
    }

    @Override
    public void refresh() {
        atualizarLista();
    }

    public void selecionarCategorias() {
        abas.getSelectionModel().select(abaCategorias);
        atualizarLista();
    }

    public void selecionarSobre() {
        abas.getSelectionModel().select(abaSobre);
    }

    // =========================================================
    // CONTEÚDO DA ABA CATEGORIAS
    // =========================================================
    private VBox criarConteudoCategorias() {
        VBox cabecalho = UiUtil.cabecalho("Categorias",
                "Use categorias para organizar suas receitas e despesas.");

        HBox area = new HBox(18, criarFormCategoria(), criarListaCategorias());
        HBox.setHgrow(area, Priority.ALWAYS);
        area.setAlignment(Pos.TOP_LEFT);

        return new VBox(16, cabecalho, area);
    }

    private VBox criarFormCategoria() {
        VBox card = new VBox(12);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setPrefWidth(330);

        Label l = new Label("Nova categoria");
        l.getStyleClass().add("form-title");

        nomeField.setPromptText("Ex.: Plano de saúde, Comissões...");
        tipoCombo.setMaxWidth(Double.MAX_VALUE);
        tipoCombo.setValue("Receita");

        VBox campos = new VBox(6,
                new Label("Nome da categoria"), nomeField,
                new Label("Tipo (Receita ou Despesa)"), tipoCombo);

        Button salvar = new Button("Salvar");
        salvar.getStyleClass().addAll("btn", "btn-green");
        salvar.setOnAction(e -> salvarCategoria());

        Button nova = new Button("Nova categoria");
        nova.getStyleClass().addAll("btn", "btn-outline");
        nova.setOnAction(e -> novo());

        Button excluir = new Button("Excluir");
        excluir.getStyleClass().addAll("btn", "btn-outline-red");
        excluir.setOnAction(e -> excluir());

        HBox botoes = new HBox(8, salvar, nova, excluir);
        botoes.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(l, campos, botoes);
        return card;
    }

    private VBox criarListaCategorias() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(14));

        Label l = new Label("Categorias cadastradas");
        l.getStyleClass().add("card-label");

        TableColumn<Categoria, String> cNome = new TableColumn<>("Nome");
        cNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        cNome.setMinWidth(220);

        TableColumn<Categoria, String> cTipo = new TableColumn<>("Tipo");
        cTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        cTipo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String tipo, boolean empty) {
                super.updateItem(tipo, empty);
                if (empty || tipo == null) {
                    setText("");
                    setStyle("");
                    return;
                }
                boolean receita = Categoria.TIPO_RECEITA.equals(tipo);
                setText(receita ? "Receita" : "Despesa");
                setStyle(receita
                        ? "-fx-text-fill:#0E7A3C; -fx-font-weight:bold;"
                        : "-fx-text-fill:#C8102E; -fx-font-weight:bold;");
            }
        });

        tabela.getColumns().add(cNome);
        tabela.getColumns().add(cTipo);
        tabela.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(tabela, Priority.ALWAYS);

        tabela.getSelectionModel().selectedItemProperty().addListener((obs, antes, depois) -> {
            if (depois != null) {
                idCategoriaEmEdicao = depois.getId();
                nomeField.setText(depois.getNome());
                tipoCombo.setValue(Categoria.TIPO_RECEITA.equals(depois.getTipo()) ? "Receita" : "Despesa");
            }
        });

        card.getChildren().addAll(l, tabela);
        atualizarLista();
        return card;
    }

    private VBox criarConteudoSobre() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(18));
        card.setMaxWidth(640);

        Label titulo = new Label("Sobre o Gauderio-ERP");
        titulo.getStyleClass().add("form-title");

        Label texto = new Label("""
                Uma ferramenta simples para acompanhar o dinheiro do seu negócio:

                • Registre entradas e saídas;
                • Acompanhe contas a pagar e a receber;
                • Controle o saldo das suas contas bancárias;
                • Veja gráficos e relatórios do mês.

                Seus dados são armazenados localmente neste computador.
                Banco de dados local: SQLite (informação técnica).""");
        texto.getStyleClass().add("about-text");

        Label assinatura = new Label("Desenvolvido por Jaykson Bolico");
        assinatura.getStyleClass().add("about-dev");

        card.getChildren().addAll(titulo, texto, assinatura);

        VBox box = new VBox(16, UiUtil.cabecalho("Sobre", "Informações do sistema."), card);
        box.setPadding(new Insets(0));
        return box;
    }

    // =========================================================
    // AÇÕES DE CATEGORIAS
    // =========================================================
    private void salvarCategoria() {
        String nome = nomeField.getText() == null ? "" : nomeField.getText().trim();
        String tipo = "Receita".equals(tipoCombo.getValue())
                ? Categoria.TIPO_RECEITA : Categoria.TIPO_DESPESA;
        if (nome.isBlank() || tipoCombo.getValue() == null) {
            alerta("Informe o nome e o tipo da categoria.", Alert.AlertType.WARNING);
            return;
        }
        if (idCategoriaEmEdicao > 0) {
            categoriaDAO.update(idCategoriaEmEdicao, nome, tipo);
            alerta("Categoria atualizada com sucesso.", Alert.AlertType.INFORMATION);
        } else {
            try {
                categoriaDAO.insert(nome, tipo);
                alerta("Categoria criada com sucesso.", Alert.AlertType.INFORMATION);
            } catch (IllegalStateException ex) {
                alerta(ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
        novo();
        atualizarLista();
    }

    private void excluir() {
        Categoria c = tabela.getSelectionModel().getSelectedItem();
        if (c == null) {
            alerta("Selecione uma categoria na lista.", Alert.AlertType.WARNING);
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setHeaderText(null);
        confirm.setContentText("Excluir a categoria \"" + c.getNome() + "\"?");
        ButtonType excluirBtn = new ButtonType("Excluir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelarBtn = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(excluirBtn, cancelarBtn);
        confirm.showAndWait().filter(b -> b == excluirBtn).ifPresent(b -> {
            try {
                categoriaDAO.delete(c.getId());
                atualizarLista();
                novo();
            } catch (IllegalStateException ex) {
                alerta(ex.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void novo() {
        idCategoriaEmEdicao = -1;
        nomeField.clear();
        tipoCombo.setValue("Receita");
        tabela.getSelectionModel().clearSelection();
    }

    private void atualizarLista() {
        tabela.getItems().setAll(categoriaDAO.findAll());
        tabela.refresh();
    }

    private void alerta(String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}