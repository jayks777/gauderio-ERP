package br.com.gauderio.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;

public class GeradorIcone {
    public static void main(String[] args) throws Exception {
        int size = 128;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fundo escuro elegante arredondado
        RoundRectangle2D rect = new RoundRectangle2D.Float(4, 4, 120, 120, 28, 28);
        g.setColor(new Color(21, 29, 39));
        g.fill(rect);

        // Borda suave
        g.setColor(new Color(40, 52, 68));
        g.setStroke(new BasicStroke(2f));
        g.draw(rect);

        // Faixa tricolor gaúcha estilizada na base
        g.setColor(new Color(14, 122, 60)); // Verde
        g.fillRoundRect(22, 102, 26, 6, 3, 3);
        g.setColor(new Color(200, 16, 46)); // Vermelho
        g.fillRoundRect(51, 102, 26, 6, 3, 3);
        g.setColor(new Color(245, 168, 0)); // Amarelo
        g.fillRoundRect(80, 102, 26, 6, 3, 3);

        // Letra G dourada estilizada
        g.setColor(new Color(245, 168, 0));
        g.setFont(new Font("Segoe UI", Font.BOLD, 56));
        FontMetrics fm = g.getFontMetrics();
        String text = "G$";
        int tx = (size - fm.stringWidth(text)) / 2;
        int ty = ((size - fm.getHeight()) / 2) + fm.getAscent() - 2;
        g.drawString(text, tx, ty);

        g.dispose();

        File out1 = new File("src/main/resources/br/com/gauderio/icon.png");
        out1.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out1);

        File out2 = new File("favicon.ico");
        ImageIO.write(img, "PNG", out2);

        System.out.println("Icone gerado com sucesso em: " + out1.getAbsolutePath());
    }
}

