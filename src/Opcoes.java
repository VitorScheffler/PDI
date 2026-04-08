import java.awt.image.BufferedImage;

public class Opcoes {

    // ── Fábrica de imagem ─────────────────────────────────────────
    private static BufferedImage nova(int w, int h, int tipo) {
        int t = (tipo == BufferedImage.TYPE_CUSTOM || tipo == 0)
                ? BufferedImage.TYPE_INT_ARGB : tipo;
        return new BufferedImage(w, h, t);
    }

    // Garante que um valor de canal fique no intervalo [0, 255]
    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // ── Translação ────────────────────────────────────────────────
    public static BufferedImage translacao(BufferedImage img, int tx, int ty) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = nova(w, h, img.getType());
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int xo = x - tx, yo = y - ty;
                if (xo >= 0 && xo < w && yo >= 0 && yo < h)
                    out.setRGB(x, y, img.getRGB(xo, yo));
            }
        return out;
    }

    // ── Rotação (qualquer ângulo) ─────────────────────────────────
    public static BufferedImage rotacao(BufferedImage img, double angulo) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = nova(w, h, img.getType());
        double rad = Math.toRadians(angulo);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double cx = (w - 1) / 2.0, cy = (h - 1) / 2.0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                double dx = x - cx, dy = y - cy;
                int xs = (int) Math.round( cos * dx + sin * dy + cx);
                int ys = (int) Math.round(-sin * dx + cos * dy + cy);
                if (xs >= 0 && xs < w && ys >= 0 && ys < h)
                    out.setRGB(x, y, img.getRGB(xs, ys));
            }
        return out;
    }

    // ── Escala (aumentar / diminuir) ─────────────────────────────
    public static BufferedImage escala(BufferedImage img, double sx, double sy) {
        if (sx <= 0 || sy <= 0)
            throw new IllegalArgumentException("Fatores devem ser positivos.");
        int sw = img.getWidth(), sh = img.getHeight();
        int dw = Math.max(1, (int) Math.round(sw * sx));
        int dh = Math.max(1, (int) Math.round(sh * sy));
        BufferedImage out = nova(dw, dh, img.getType());
        for (int y = 0; y < dh; y++)
            for (int x = 0; x < dw; x++) {
                int xs = (int) Math.round(x / sx);
                int ys = (int) Math.round(y / sy);
                if (xs >= 0 && xs < sw && ys >= 0 && ys < sh)
                    out.setRGB(x, y, img.getRGB(xs, ys));
            }
        return out;
    }

    // ── Espelhamentos ─────────────────────────────────────────────
    public static BufferedImage espelharHorizontal(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = nova(w, h, img.getType());
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(x, y, img.getRGB(w - 1 - x, y));
        return out;
    }

    public static BufferedImage espelharVertical(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = nova(w, h, img.getType());
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(x, y, img.getRGB(x, h - 1 - y));
        return out;
    }

    // ══════════════════════════════════════════════════════════════
    //  FILTROS
    // ══════════════════════════════════════════════════════════════

    /**
     * Converte a imagem para escala de cinzas usando o método da
     * LUMINOSIDADE (ponderado pela percepção humana):
     *
     *   Y = 0.299·R + 0.587·G + 0.114·B
     *
     * Essa fórmula é a mesma utilizada no padrão ITU-R BT.601 e
     * produz resultados equivalentes ao Visnode.
     * O canal alfa original é preservado.
     *
     * @param img Imagem de entrada (qualquer tipo)
     * @return Nova imagem em tons de cinza (TYPE_INT_ARGB)
     */
    public static BufferedImage grayscale(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb   = img.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                int r     = (rgb >> 16) & 0xFF;
                int g     = (rgb >>  8) & 0xFF;
                int b     =  rgb        & 0xFF;

                // Fórmula de luminosidade (ITU-R BT.601)
                int gray = (int) Math.round(0.299 * r + 0.587 * g + 0.114 * b);
                gray = clamp(gray);

                int pixel = (alpha << 24) | (gray << 16) | (gray << 8) | gray;
                out.setRGB(x, y, pixel);
            }
        }
        return out;
    }

    /**
     * Ajusta o brilho somando (ou subtraindo) um valor fixo a cada
     * canal de cor (R, G, B).  O canal alfa não é alterado.
     *
     * Valores positivos de {@code delta} CLAREIAM a imagem;
     * valores negativos ESCURECEM.  O resultado é limitado a [0, 255].
     *
     * Exemplo de uso combinado (grayscale + brilho):
     * <pre>
     *   BufferedImage cinza    = Opcoes.grayscale(original);
     *   BufferedImage clareada = Opcoes.ajustarBrilho(cinza, 40);
     * </pre>
     *
     * @param img   Imagem de entrada
     * @param delta Incremento de brilho, no intervalo [-255, 255]
     * @return Nova imagem com brilho ajustado
     */
    public static BufferedImage ajustarBrilho(BufferedImage img, int delta) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb   = img.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                int r     = clamp(((rgb >> 16) & 0xFF) + delta);
                int g     = clamp(((rgb >>  8) & 0xFF) + delta);
                int b     = clamp(( rgb        & 0xFF) + delta);

                out.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }
}