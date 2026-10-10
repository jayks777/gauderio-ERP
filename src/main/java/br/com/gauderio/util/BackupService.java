package br.com.gauderio.util;

import br.com.gauderio.dao.CategoriaDAO;
import br.com.gauderio.dao.ContaDAO;
import br.com.gauderio.dao.TransacaoDAO;
import br.com.gauderio.db.Database;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Serviço responsável por operações de segurança, backup e restauração do banco SQLite.
 */
public final class BackupService {

    private BackupService() {
    }

    /**
     * Cria uma cópia de segurança do banco de dados no destino indicado.
     */
    public static void criarBackup(File destino) throws IOException {
        Database.forcarCheckpoint();
        Path dbOriginal = Database.getDbFilePath();
        if (!Files.exists(dbOriginal)) {
            throw new IOException("Arquivo original do banco de dados não encontrado: " + dbOriginal);
        }
        Files.copy(dbOriginal, destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Restaura o banco de dados a partir de um arquivo de backup.
     */
    public static void restaurarBackup(File arquivoBackup) throws IOException {
        if (!arquivoBackup.exists() || !arquivoBackup.isFile()) {
            throw new IOException("O arquivo de backup selecionado não existe ou é inválido.");
        }

        // Fecha conexão e locks
        Database.fecharConexao();

        Path dbDestino = Database.getDbFilePath();
        Files.copy(arquivoBackup.toPath(), dbDestino, StandardCopyOption.REPLACE_EXISTING);

        // Reabre e valida
        Database.init();
    }

    /**
     * Gera um nome de arquivo de backup sugerido com timestamp.
     */
    public static String nomeArquivoSugerido() {
        String data = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmm").format(LocalDateTime.now());
        return "gauderio-backup-" + data + ".db";
    }

    public static String tamanhoBancoFormatado() {
        Path p = Database.getDbFilePath();
        if (!Files.exists(p)) {
            return "0 KB";
        }
        try {
            long bytes = Files.size(p);
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return new DecimalFormat("#,##0.0 KB").format(bytes / 1024.0);
            } else {
                return new DecimalFormat("#,##0.0 MB").format(bytes / (1024.0 * 1024.0));
            }
        } catch (IOException e) {
            return "Desconhecido";
        }
    }

    public static EstatisticasBanco obterEstatisticas() {
        int totalTransacoes = new TransacaoDAO().findAll().size();
        int totalContas = new ContaDAO().findAll().size();
        int totalCategorias = new CategoriaDAO().findAll().size();
        String tamanho = tamanhoBancoFormatado();
        String caminho = Database.getDbFilePath().toAbsolutePath().toString();
        return new EstatisticasBanco(totalTransacoes, totalContas, totalCategorias, tamanho, caminho);
    }

    public record EstatisticasBanco(
            int totalTransacoes,
            int totalContas,
            int totalCategorias,
            String tamanhoFormatado,
            String caminhoArquivo
    ) {}
}

