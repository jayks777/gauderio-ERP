package br.com.gauderio.util;

import br.com.gauderio.model.Transacao;
import br.com.gauderio.ui.RelatoriosView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Utilitário para exportar relatórios e movimentações para arquivos CSV.
 * Gera arquivos com BOM UTF-8 e delimitador ';' para abertura direta e perfeita no Excel em português.
 */
public final class ExportadorCsv {

    private ExportadorCsv() {
    }

    /**
     * Exporta a lista de lançamentos para arquivo CSV.
     */
    public static void exportarTransacoes(List<Transacao> lista, File arquivo) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(arquivo);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            // Escreve o BOM UTF-8 (\uFEFF) para garantir abertura correta no Excel Windows
            writer.write('\uFEFF');

            // Cabeçalho
            writer.write("ID;Data;Tipo;Descrição;Categoria;Conta Bancária;Valor (R$);Vencimento;Situação;Recorrente;Frequência");
            writer.newLine();

            for (Transacao t : lista) {
                StringBuilder sb = new StringBuilder();
                sb.append(t.getId()).append(';');
                sb.append(Formatador.data(t.getData())).append(';');
                sb.append(escapar(t.getTipoTexto())).append(';');
                sb.append(escapar(t.getDescricao())).append(';');
                sb.append(escapar(t.getCategoria() == null ? "" : t.getCategoria())).append(';');
                sb.append(escapar(t.getConta() == null ? "" : t.getConta())).append(';');
                sb.append(String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", t.getValor())).append(';');
                sb.append(t.getVencimento() == null ? "" : Formatador.data(t.getVencimento())).append(';');
                sb.append(escapar(t.getStatusTexto())).append(';');
                sb.append(t.isRecorrente() ? "Sim" : "Não").append(';');
                sb.append(escapar(t.getFrequencia() == null ? "" : t.getFrequencia()));

                writer.write(sb.toString());
                writer.newLine();
            }
        }
    }

    /**
     * Exporta o demonstrativo mensal de receitas e despesas para CSV.
     */
    public static void exportarRelatorioMensal(int ano, List<RelatoriosView.LinhaMensal> linhas,
                                              double totalReceitas, double totalDespesas,
                                              double resultadoGeral, File arquivo) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(arquivo);
             OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
             BufferedWriter writer = new BufferedWriter(osw)) {

            writer.write('\uFEFF');

            writer.write("DEMONSTRATIVO FINANCEIRO GAUDERIO-ERP - ANO " + ano);
            writer.newLine();
            writer.newLine();

            writer.write("Mês;Receitas (R$);Despesas (R$);Resultado do Mês (R$);Acumulado (R$)");
            writer.newLine();

            for (RelatoriosView.LinhaMensal l : linhas) {
                StringBuilder sb = new StringBuilder();
                sb.append(escapar(l.getMes())).append(';');
                sb.append(String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", l.getReceita())).append(';');
                sb.append(String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", l.getDespesa())).append(';');
                sb.append(String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", l.getResultado())).append(';');
                sb.append(String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", l.getAcumulado()));

                writer.write(sb.toString());
                writer.newLine();
            }

            writer.newLine();
            writer.write("TOTAIS CONSOLIDADOS;;;");
            writer.newLine();
            writer.write("Total de Receitas;" + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", totalReceitas));
            writer.newLine();
            writer.write("Total de Despesas;" + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", totalDespesas));
            writer.newLine();
            writer.write("Resultado Líquido;" + String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", resultadoGeral));
            writer.newLine();
        }
    }

    private static String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}

