package br.com.gauderio.util;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;

/**
 * Utilitário central de ícones vetoriais SVG para a interface do Gauderio-ERP.
 * Garante ícones nítidos, escaláveis e esteticamente harmonizados com a identidade visual.
 */
public final class IconeUtil {

    private IconeUtil() {
    }

    // Paths vetoriais SVG (viewBox 0 0 24 24 ou 0 0 16 16)
    public static final String PATH_DASHBOARD = "M3 3h8v8H3zm10 0h8v5h-8zm0 7h8v11h-8zm-10 3h8v8H3z";
    public static final String PATH_ENTRADA = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 14h-2v-4H8l4-4 4 4h-3v4z";
    public static final String PATH_SAIDA = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 6h2v4h3l-4 4-4-4h3V8z";
    public static final String PATH_HISTORICO = "M13 3a9 9 0 0 0-9 9H1l3.89 3.89.07.14L9 12H6c0-3.87 3.13-7 7-7s7 3.13 7 7-3.13 7-7 7c-1.93 0-3.68-.79-4.94-2.06l-1.42 1.42A8.954 8.954 0 0 0 13 21a9 9 0 0 0 0-18zm-1 5v5l4.28 2.54.72-1.21-3.5-2.08V8H12z";
    public static final String PATH_PAGAR = "M20 4H4c-1.11 0-1.99.89-1.99 2L2 18c0 1.11.89 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.11-.9-2-2-2zm0 14H4v-6h16v6zm0-10H4V6h16v2zm-4 7l-3-3h2V9h2v4h2l-3 3z";
    public static final String PATH_RECEBER = "M21 18v1c0 1.1-.9 2-2 2H5c-1.11 0-2-.9-2-2V5c0-1.1.89-2 2-2h14c1.1 0 2 .9 2 2v1h-9c-1.11 0-2 .9-2 2v8c0 1.1.89 2 2 2h9zm-9-2h10V8H12v8zm4-2.5c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5z";
    public static final String PATH_BANCO = "M4 10v7h3v-7H4zm6 0v7h3v-7h-3zM2 22h19v-3H2v3zm14-12v7h3v-7h-3zm-4.5-9L2 6v2h19V6l-9.5-5z";
    public static final String PATH_RELATORIOS = "M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z";
    public static final String PATH_CATEGORIAS = "M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58.55 0 1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41 0-.55-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z";
    public static final String PATH_SOBRE = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z";
    public static final String PATH_BACKUP = "M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM17 13l-5 5-5-5h3V9h4v4h3z";

    // Ícones para botões e ações
    public static final String PATH_MAIS = "M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z";
    public static final String PATH_SALVAR = "M17 3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V7l-4-4zm-5 16c-1.66 0-3-1.34-3-3s1.34-3 3-3 3 1.34 3 3-1.34 3-3 3zm3-10H5V5h10v4z";
    public static final String PATH_LIMPAR = "M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z";
    public static final String PATH_EDITAR = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
    public static final String PATH_EXCLUIR = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";
    public static final String PATH_CHECK = "M9 16.2L4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z";
    public static final String PATH_CHECK_CIRCLE = "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z";
    public static final String PATH_EXPORTAR = "M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z";
    public static final String PATH_IMPORTAR = "M9 16h6v-6h4l-7-7-7 7h4v6zm-4 2h14v2H5v-2z";
    public static final String PATH_PASTA = "M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z";
    public static final String PATH_BUSCA = "M15.5 14h-.79l-.28-.27A6.471 6.471 0 0 0 16 9.5 6.5 6.5 0 1 0 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z";

    /**
     * Cria um SVGPath configurado com o path, tamanho e cor desejados.
     */
    public static SVGPath criarIcone(String svgPath, double tamanho, String corHex) {
        SVGPath path = new SVGPath();
        path.setContent(svgPath);
        path.setFill(Color.web(corHex));
        // Escala padrão baseada em 24x24
        double escala = tamanho / 24.0;
        path.setScaleX(escala);
        path.setScaleY(escala);
        return path;
    }

    /**
     * Cria uma Region estilizada para ícones no CSS que herdam cor do pai ou do tema.
     */
    public static Region criarIconeRegion(String svgPath, double tamanho) {
        Region r = new Region();
        r.setShape(new SVGPath() {{ setContent(svgPath); }});
        r.setMinSize(tamanho, tamanho);
        r.setPrefSize(tamanho, tamanho);
        r.setMaxSize(tamanho, tamanho);
        r.getStyleClass().add("icone-svg");
        return r;
    }

    /**
     * Cria um nó de ícone colorido diretamente (ótimo para botões com cores fixas).
     */
    public static Node iconeBotao(String svgPath, String corHex) {
        return criarIcone(svgPath, 15, corHex);
    }

    public static Node iconeBotao(String svgPath) {
        return criarIcone(svgPath, 15, "#ffffff");
    }
}

