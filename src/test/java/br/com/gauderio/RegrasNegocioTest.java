package br.com.gauderio;

import br.com.gauderio.dao.CategoriaDAO;
import br.com.gauderio.dao.ContaDAO;
import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.db.Database;
import br.com.gauderio.model.ContaBancaria;
import br.com.gauderio.model.Transacao;
import br.com.gauderio.util.BackupService;
import br.com.gauderio.util.ExportadorCsv;
import br.com.gauderio.util.Formatador;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RegrasNegocioTest {

    @TempDir
    static Path diretorioTemporario;

    @BeforeAll
    public static void setup() {
        System.setProperty(Database.DATA_DIRECTORY_PROPERTY, diretorioTemporario.toString());
        Database.init();
    }

    @AfterAll
    public static void encerrar() {
        Database.fecharConexao();
        System.clearProperty(Database.DATA_DIRECTORY_PROPERTY);
    }

    @Test
    public void testTipoTextoCorreto() {
        Transacao receita = new Transacao(Transacao.TIPO_RECEITA, "Venda", 100.0, "Vendas", "Banrisul",
                LocalDate.now(), null, Transacao.STATUS_PAGO, false, null);
        Transacao despesa = new Transacao(Transacao.TIPO_DESPESA, "Conta de luz", 50.0, "Energia elétrica", "Banrisul",
                LocalDate.now(), null, Transacao.STATUS_PAGO, false, null);

        // Receita DEVE ser Entrada, Despesa DEVE ser Saída
        assertEquals("Entrada", receita.getTipoTexto());
        assertTrue(receita.isReceita());
        assertFalse(receita.isDespesa());

        assertEquals("Saída", despesa.getTipoTexto());
        assertTrue(despesa.isDespesa());
        assertFalse(despesa.isReceita());
    }

    @Test
    public void testCalculoSaldoConta() {
        ContaDAO contaDAO = new ContaDAO();
        TransacaoDAO transacaoDAO = new TransacaoDAO();

        String nomeConta = "Conta Teste Regras " + System.currentTimeMillis();
        contaDAO.insert(new ContaBancaria(nomeConta, "Banrisul", "001", "111-2", 1000.00));

        // 1000 inicial
        assertEquals(1000.00, contaDAO.saldoAtual(nomeConta), 0.001);

        // Entrada / Receita paga de 300 -> saldo 1300
        transacaoDAO.insert(new Transacao(Transacao.TIPO_RECEITA, "Entrada teste", 300.00, "Vendas",
                nomeConta, LocalDate.now(), null, Transacao.STATUS_PAGO, false, null));
        assertEquals(1300.00, contaDAO.saldoAtual(nomeConta), 0.001);

        // Saída / Despesa paga de 200 -> saldo 1100
        transacaoDAO.insert(new Transacao(Transacao.TIPO_DESPESA, "Saída teste", 200.00, "Impostos",
                nomeConta, LocalDate.now(), null, Transacao.STATUS_PAGO, false, null));
        assertEquals(1100.00, contaDAO.saldoAtual(nomeConta), 0.001);

        // Lançamento PENDENTE não deve afetar o saldo
        transacaoDAO.insert(new Transacao(Transacao.TIPO_DESPESA, "Boleto futuro", 500.00, "Impostos",
                nomeConta, LocalDate.now(), LocalDate.now().plusDays(10), Transacao.STATUS_PENDENTE, false, null));
        assertEquals(1100.00, contaDAO.saldoAtual(nomeConta), 0.001);
    }

    @Test
    public void testBackupEServico() throws Exception {
        File tempBackup = File.createTempFile("gauderio-test-backup", ".db");
        tempBackup.deleteOnExit();

        BackupService.criarBackup(tempBackup);
        assertTrue(tempBackup.exists());
        assertTrue(tempBackup.length() > 0);

        BackupService.EstatisticasBanco stats = BackupService.obterEstatisticas();
        assertNotNull(stats.caminhoArquivo());
        assertNotNull(stats.tamanhoFormatado());
    }

    @Test
    public void testExportadorCsv() throws Exception {
        File tempCsv = File.createTempFile("gauderio-test-export", ".csv");
        tempCsv.deleteOnExit();

        Transacao t = new Transacao(Transacao.TIPO_RECEITA, "Teste CSV", 150.50, "Vendas",
                "Caixa", LocalDate.now(), null, Transacao.STATUS_PAGO, false, null);

        ExportadorCsv.exportarTransacoes(List.of(t), tempCsv);
        assertTrue(tempCsv.exists());

        String conteudo = Files.readString(tempCsv.toPath());
        assertTrue(conteudo.contains("Teste CSV"));
        assertTrue(conteudo.contains("150,50"));
        assertTrue(conteudo.contains("Entrada"));
    }

    @Test
    public void testFormatador() {
        assertEquals("150,00", String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", 150.0));
        assertEquals(1234.56, Formatador.parseValor("1.234,56"), 0.001);
        assertNotNull(Formatador.mesAno(LocalDate.now()));
    }
}

