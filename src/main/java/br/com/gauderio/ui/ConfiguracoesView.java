package br.com.gauderio.ui;

import br.com.gauderio.dao.CategoriaDAO;
import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.db.Database;
import br.com.gauderio.model.Categoria;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.BackupService;
import br.com.gauderio.util.ExportadorCsv;
import br.com.gauderio.util.IconeUtil;
import br.com.gauderio.util.UiUtil;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Configurações: categorias, backup e salvamento de dados, e informações do sistema.
 */
public class ConfiguracoesView extends BorderPane implements Refreshable {

    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final Runnable onDadosRestaurados;

    private int idCategoriaEmEdicao = -1;

    private final TextField nomeField = new TextField();
    private final ComboBox<String> tipoCombo = new ComboBox<>(
            FXCollections.observableArrayList("Receita", "Despesa"));
    private final TableView<Categoria> tabela = new TableView<>();

    private final TabPane abas = new TabPane();
    private final Tab abaCategorias = new Tab("Categorias");
    private final Tab abaBackup = new Tab("Backup & Dados");
    private final Tab abaSobre = new Tab("Sobre");

    // Labels da aba de backup
    private final Label lblDbPath = new Label();
    private final Label lblDbTamanho = new Label();
    private final Label lblDbTotalTransacoes = new Label();
    private final Label lblDbTotalContas = new Label();
    private final Label lblDbTotalCategorias = new Label();

    public ConfiguracoesView() {
        this(() -> {});
    }

    public ConfiguracoesView(Runnable onDadosRestaurados) {
        this.onDadosRestaurados = onDadosRestaurados == null ? () -> {} : onDadosRestaurados;
        setPadding(new Insets(24));
        getStyleClass().add("content");

        abas.getStyleClass().add("conta-tabs");
        abas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        abas.getTabs().addAll(abaCategorias, abaBackup, abaSobre);
        abaCategorias.setContent(criarConteudoCategorias());
        abaBackup.setContent(criarConteudoBackup());
        abaSobre.setContent(criarConteudoSobre());

        setCenter(abas);
    }

    @Override
    public void refresh() {
        atualizarLista();
        atualizarInfoBackup();
    }

    public void selecionarCategorias() {
        abas.getSelectionModel().select(abaCategorias);
        atualizarLista();
    }

    public void selecionarBackup() {
        abas.getSelectionModel().select(abaBackup);
        atualizarInfoBackup();
    }

    public void selecionarSobre() {
        abas.getSelectionModel().select(abaSobre);
    }

    // =========================================================
    // CONTEÚDO DA ABA CATEGORIAS
    // =========================================================
    private VBox criarConteudoCategorias() {
        VBox cabecalho = UiUtil.cabecalho("Categorias",
                "Organize suas receitas e despesas por categorias para relatórios precisos.");

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

        nomeField.setPromptText("Ex.: Plano de saúde, Venda de gado...");
        tipoCombo.setMaxWidth(Double.MAX_VALUE);
        tipoCombo.setValue("Receita");

        VBox campos = new VBox(6,
                new Label("Nome da categoria *"), nomeField,
                new Label("Tipo (Receita ou Despesa)"), tipoCombo);

        Button salvar = new Button("Salvar", IconeUtil.iconeBotao(IconeUtil.PATH_SALVAR, "#ffffff"));
        salvar.getStyleClass().addAll("btn", "btn-green");
        salvar.setOnAction(e -> salvarCategoria());

        Button nova = new Button("Nova", IconeUtil.iconeBotao(IconeUtil.PATH_MAIS, "#41505c"));
        nova.getStyleClass().addAll("btn", "btn-outline");
        nova.setOnAction(e -> novo());

        Button excluir = new Button("Excluir", IconeUtil.iconeBotao(IconeUtil.PATH_EXCLUIR, "#C8102E"));
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
                setText(receita ? "▲ Receita" : "▼ Despesa");
                setStyle(receita
                        ? "-fx-text-fill:#0E7A3C; -fx-font-weight:bold;"
                        : "-fx-text-fill:#C8102E; -fx-font-weight:bold;");
            }
        });

        tabela.getColumns().clear();
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

    // =========================================================
    // CONTEÚDO DA ABA BACKUP & DADOS (SALVAMENTO E SEGURANÇA)
    // =========================================================
    private VBox criarConteudoBackup() {
        VBox cabecalho = UiUtil.cabecalho("Backup & Salvamento de Dados",
                "Faça cópias de segurança do seu banco de dados e exporte relatórios para evitar perda de informações.");

        VBox cardInfo = new VBox(12);
        cardInfo.getStyleClass().add("card");
        cardInfo.setPadding(new Insets(18));
        cardInfo.setMaxWidth(750);

        Label titInfo = new Label("ESTADO ATUAL DO BANCO DE DADOS LOCAL");
        titInfo.getStyleClass().add("card-label");

        lblDbPath.getStyleClass().add("form-hint");
        lblDbPath.setWrapText(true);

        GridPane gridEstatisticas = new GridPane();
        gridEstatisticas.setHgap(20);
        gridEstatisticas.setVgap(10);
        gridEstatisticas.add(new Label("Local do banco de dados:"), 0, 0);
        gridEstatisticas.add(lblDbPath, 1, 0);
        gridEstatisticas.add(new Label("Tamanho em disco:"), 0, 1);
        gridEstatisticas.add(lblDbTamanho, 1, 1);
        gridEstatisticas.add(new Label("Lançamentos registrados:"), 0, 2);
        gridEstatisticas.add(lblDbTotalTransacoes, 1, 2);
        gridEstatisticas.add(new Label("Contas bancárias:"), 0, 3);
        gridEstatisticas.add(lblDbTotalContas, 1, 3);
        gridEstatisticas.add(new Label("Categorias:"), 0, 4);
        gridEstatisticas.add(lblDbTotalCategorias, 1, 4);

        Button btnAbrirPasta = new Button("Abrir pasta de dados no Windows", IconeUtil.iconeBotao(IconeUtil.PATH_PASTA, "#41505c"));
        btnAbrirPasta.getStyleClass().addAll("btn", "btn-outline");
        btnAbrirPasta.setOnAction(e -> abrirPastaDados());

        cardInfo.getChildren().addAll(titInfo, gridEstatisticas, btnAbrirPasta);

        VBox cardAcoes = new VBox(14);
        cardAcoes.getStyleClass().add("card");
        cardAcoes.setPadding(new Insets(18));
        cardAcoes.setMaxWidth(750);

        Label titAcoes = new Label("CÓPIAS DE SEGURANÇA (BACKUP)");
        titAcoes.getStyleClass().add("card-label");

        Label dicaBackup = new Label("Recomendamos criar uma cópia de segurança periodicamente em um pendrive ou pasta na nuvem.");
        dicaBackup.getStyleClass().add("form-hint");

        Button btnFazerBackup = new Button("Criar cópia de segurança (Backup)", IconeUtil.iconeBotao(IconeUtil.PATH_BACKUP, "#ffffff"));
        btnFazerBackup.getStyleClass().addAll("btn", "btn-green");
        btnFazerBackup.setOnAction(e -> executarBackup());

        Button btnRestaurarBackup = new Button("Restaurar cópia de segurança", IconeUtil.iconeBotao(IconeUtil.PATH_IMPORTAR, "#41505c"));
        btnRestaurarBackup.getStyleClass().addAll("btn", "btn-outline");
        btnRestaurarBackup.setOnAction(e -> executarRestauracao());

        Button btnExportarCsvGeral = new Button("Exportar todos os lançamentos para Excel (CSV)", IconeUtil.iconeBotao(IconeUtil.PATH_EXPORTAR, "#41505c"));
        btnExportarCsvGeral.getStyleClass().addAll("btn", "btn-outline");
        btnExportarCsvGeral.setOnAction(e -> exportarTudoCsv());

        HBox botoesBackup = new HBox(12, btnFazerBackup, btnRestaurarBackup);
        botoesBackup.setAlignment(Pos.CENTER_LEFT);

        cardAcoes.getChildren().addAll(titAcoes, dicaBackup, botoesBackup, new Separator(), btnExportarCsvGeral);

        atualizarInfoBackup();

        return new VBox(16, cabecalho, cardInfo, cardAcoes);
    }

    private void atualizarInfoBackup() {
        BackupService.EstatisticasBanco stats = BackupService.obterEstatisticas();
        lblDbPath.setText(stats.caminhoArquivo());
        lblDbTamanho.setText(stats.tamanhoFormatado());
        lblDbTotalTransacoes.setText(String.valueOf(stats.totalTransacoes()));
        lblDbTotalContas.setText(String.valueOf(stats.totalContas()));
        lblDbTotalCategorias.setText(String.valueOf(stats.totalCategorias()));
    }

    private void abrirPastaDados() {
        try {
            File pasta = Database.getDataDirectory().toFile();
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pasta);
            } else {
                alerta("A pasta está em: " + pasta.getAbsolutePath(), Alert.AlertType.INFORMATION);
            }
        } catch (IOException ex) {
            alerta("Não foi possível abrir a pasta: " + ex.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void executarBackup() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar cópia de segurança (Backup)");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Banco SQLite (*.db)", "*.db"));
        chooser.setInitialFileName(BackupService.nomeArquivoSugerido());
        File arquivo = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (arquivo != null) {
            try {
                BackupService.criarBackup(arquivo);
                alerta("Backup concluído com sucesso!\nArquivo salvo em:\n" + arquivo.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                alerta("Erro ao criar backup: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void executarRestauracao() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Atenção - Restaurar Backup");
        confirm.setHeaderText("Substituir dados atuais?");
        confirm.setContentText("A restauração de um backup irá substituir todos os dados atuais pelos dados contidos no arquivo selecionado.\n\nRecomendamos criar um backup prévio antes de continuar. Deseja prosseguir?");
        ButtonType btnSim = new ButtonType("Continuar", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNao = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(btnSim, btnNao);

        if (confirm.showAndWait().orElse(btnNao) != btnSim) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar arquivo de backup para restauração");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Banco SQLite (*.db)", "*.db"));
        File arquivo = chooser.showOpenDialog(getScene() == null ? null : getScene().getWindow());
        if (arquivo != null) {
            try {
                BackupService.restaurarBackup(arquivo);
                alerta("Dados restaurados com sucesso a partir de:\n" + arquivo.getName(), Alert.AlertType.INFORMATION);
                atualizarInfoBackup();
                atualizarLista();
                onDadosRestaurados.run();
            } catch (Exception ex) {
                alerta("Erro ao restaurar banco: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void exportarTudoCsv() {
        List<Transacao> todas = new TransacaoDAO().findAll();
        if (todas.isEmpty()) {
            alerta("Não há lançamentos cadastrados para exportar.", Alert.AlertType.WARNING);
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar todos os lançamentos para CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Planilha CSV (*.csv)", "*.csv"));
        chooser.setInitialFileName("todas-movimentacoes-" + LocalDate.now() + ".csv");
        File arquivo = chooser.showSaveDialog(getScene() == null ? null : getScene().getWindow());
        if (arquivo != null) {
            try {
                ExportadorCsv.exportarTransacoes(todas, arquivo);
                alerta("Arquivo exportado com sucesso em:\n" + arquivo.getAbsolutePath(), Alert.AlertType.INFORMATION);
            } catch (Exception ex) {
                alerta("Erro na exportação: " + ex.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    // =========================================================
    // CONTEÚDO DA ABA SOBRE
    // =========================================================
    private VBox criarConteudoSobre() {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(20));
        card.setMaxWidth(680);

        Label titulo = new Label("Gauderio-ERP · Sistema Financeiro Gaúcho");
        titulo.getStyleClass().add("form-title");

        Label texto = new Label("""
                O Gauderio-ERP é um sistema de gestão financeira pensado para empreendedores e gestores
                que buscam simplicidade, clareza e controle absoluto sobre suas finanças.

                Recursos disponíveis:
                • Dashboard intuitivo com fluxo financeiro mensal e saldo em tempo real;
                • Gestão de Entradas e Saídas com suporte a repetição/recorrência;
                • Controle rigoroso de Contas a Pagar e Contas a Receber com cálculo de atrasos;
                • Controle de múltiplas Contas Bancárias e ajustes de saldo;
                • Demonstrativos e relatórios com gráficos e exportação para Excel (CSV);
                • Backup e restauração rápida de dados com armazenamento 100% local e seguro.

                Seus dados pertencem a você e ficam armazenados localmente no seu computador.""");
        texto.getStyleClass().add("about-text");

        Label versao = new Label("Versão 1.0.0 · Edição Rio Grande do Sul");
        versao.getStyleClass().add("form-hint");

        Label assinatura = new Label("Desenvolvido por Jaykson Bolico");
        assinatura.getStyleClass().add("about-dev");

        card.getChildren().addAll(titulo, texto, versao, assinatura);

        VBox box = new VBox(16, UiUtil.cabecalho("Sobre o Gauderio-ERP", "Informações e créditos do software."), card);
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
        confirm.setTitle("Excluir categoria");
        confirm.setHeaderText(null);
        confirm.setContentText("Excluir a categoria \"" + c.getNome() + "\"?");
        ButtonType excluirBtn = new ButtonType("Sim, excluir", ButtonBar.ButtonData.OK_DONE);
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
        atualizarInfoBackup();
    }

    private void alerta(String msg, Alert.AlertType tipo) {
        Alert a = new Alert(tipo, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }
}