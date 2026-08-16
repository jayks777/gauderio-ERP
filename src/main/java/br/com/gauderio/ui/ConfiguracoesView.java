package br.com.gauderio.ui;

import br.com.gauderio.dao.CategoriaDAO;
import br.com.gauderio.model.Categoria;
import br.com.gauderio.util.UiUtil;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Configurações: categorias de receitas/despesas e informações do sistema. */
public class ConfiguracoesView extends BorderPane implements Refreshable {

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();

    private int idCategoriaEmEdicao = -1;

    private final TextField nomeField = new TextField();
    private final ComboBox<String> tipoCombo = new ComboBox<>(
            FXCollections.observableArrayList(Categoria.TIPO_RECEITA, Categoria.TIPO_DESPESA));
    private final TableView<Categoria> tabela = new TableView<>();

    public ConfiguracoesView() {
        setPadding(new Insets(24));
        getStyleClass().add("content");

        VBox cabecalho = UiUtil.cabecalho("Configurações",
                "Gerencie as categorias usadas nos lançamentos de receitas e despesas.");

        HBox area = new HBox(18, criarFormCategoria(), criarListaCategorias());
        HBox.setHgrow(criarListaCategorias(), Priority.ALWAYS);
        area.setAlignment(Pos.TOP_LEFT);

        VBox box = new VBox(16, cabecalho, area, criarCardSobre());
        setCenter(box);
    }

    @Override
    public void refresh() {
        atualizarLista();
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
        tipoCombo.setValue(Categoria.TIPO_RECEITA);

        VBox campos = new VBox(6,
                new Label("Nome da categoria"), nomeField,
                new Label("Tipo"), tipoCombo);

        Button salvar = new Button("Salvar categoria");
        salvar.getStyleClass().addAll("btn", "btn-green");
        salvar.setOnAction(e -> salvarCategoria());

        Button novo = new Button("Nova");
        novo.getStyleClass().addAll("btn", "btn-outline");
        novo.setOnAction(e -> novo());

        Button excluir = new Button("Excluir");
        excluir.getStyleClass().addAll("btn", "btn-outline-red");
        excluir.setOnAction(e -> excluir());

        HBox botoes = new HBox(8, salvar, novo, excluir);
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
                setText(Categoria.TIPO_RECEITA.equals(tipo) ? "Receita" : "Despesa");
                setStyle(Categoria.TIPO_RECEITA.equals(tipo)
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
                tipoCombo.setValue(depois.getTipo());
            }
        });

        card.getChildren().addAll(l, tabela);
        atualizarLista();
        return card;
    }

    private VBox criarCardSobre() {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));

        Label titulo = new Label("Sobre o Gauderio-ERP");
        titulo.getStyleClass().add("form-title");

        Label texto = new Label("""
                Sistema ERP financeiro desenvolvido em JavaFX com banco de dados SQLite.
                Funcionalidades:
                • Cadastro de receitas, despesas, lançamentos recorrentes e futuros
                  (boletos a pagar e vendas a receber);
                • Controle de contas bancárias com entrada e saída de saldo;
                • Central de boletos, gráficos e relatórios financeiros;
                • Todos os dados ficam salvos em um arquivo SQLite (gauderio.db)
                  na pasta de execução do projeto, para consultas futuras.""");
        texto.getStyleClass().add("about-text");

        Label assinatura = new Label("Desenvolvido por Jaykson Bolico");
        assinatura.getStyleClass().add("about-dev");

        card.getChildren().addAll(titulo, texto, assinatura);
        return card;
    }

    private void salvarCategoria() {
        String nome = nomeField.getText() == null ? "" : nomeField.getText().trim();
        String tipo = tipoCombo.getValue();
        if (nome.isBlank() || tipo == null) {
            alerta("Informe o nome e o tipo da categoria.", Alert.AlertType.WARNING);
            return;
        }
        if (idCategoriaEmEdicao > 0) {
            categoriaDAO.update(idCategoriaEmEdicao, nome, tipo);
            alerta("Categoria atualizada!", Alert.AlertType.INFORMATION);
        } else {
            try {
                categoriaDAO.insert(nome, tipo);
                alerta("Categoria criada!", Alert.AlertType.INFORMATION);
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
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir a categoria \"" + c.getNome() + "\"?");
        confirm.setHeaderText("Confirmar exclusão");
        confirm.showAndWait()
                .filter(b -> b == ButtonType.OK)
                .ifPresent(b -> {
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
        tipoCombo.setValue(Categoria.TIPO_RECEITA);
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