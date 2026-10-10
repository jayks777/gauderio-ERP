package br.com.gauderio;

/**
 * Ponto de entrada neutro usado pelo JAR e pelo instalador.
 *
 * Manter esta classe separada de {@link javafx.application.Application} evita
 * que o launcher da JVM tente localizar o JavaFX antes de montar o classpath
 * da aplicação empacotada.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Main.main(args);
    }
}
