package br.com.gauderio.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Auxílios de UI para manter um visual consistente entre as telas. */
public final class UiUtil {

    private UiUtil() {
    }

    /** Cabeçalho padrão: título + subtítulo. */
    public static VBox cabecalho(String titulo, String subtitulo) {
        Label t = new Label(titulo);
        t.getStyleClass().add("section-title");
        Label s = new Label(subtitulo);
        s.getStyleClass().add("section-subtitle");
        return new VBox(4, t, s);
    }

    /** Cabeçalho com ações à direita (botões). */
    public static BorderPane cabecalhoComAcoes(String titulo, String subtitulo, Node... acoes) {
        BorderPane bp = new BorderPane();
        bp.setLeft(cabecalho(titulo, subtitulo));
        if (acoes != null && acoes.length > 0) {
            HBox h = new HBox(10, acoes);
            h.setAlignment(Pos.CENTER_RIGHT);
            BorderPane.setMargin(h, new Insets(0, 0, 0, 12));
            bp.setRight(h);
        }
        return bp;
    }
}