package br.com.gauderio.util;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Utilitários de formatação (moeda e data) no padrão brasileiro. */
public final class Formatador {

    private static final Locale PT_BR = new Locale("pt", "BR");
    private static final NumberFormat MOEDA = NumberFormat.getCurrencyInstance(PT_BR);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR);

    private Formatador() {
    }

    public static String moeda(double valor) {
        return MOEDA.format(valor);
    }

    public static String data(LocalDate data) {
        return data == null ? "—" : DATA.format(data);
    }

    public static double parseValor(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new NumberFormatException("Valor vazio");
        }
        return Double.parseDouble(texto.trim().replace(".", "").replace(",", "."));
    }
}