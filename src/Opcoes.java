import java.awt.image.BufferedImage;

public class Opcoes {

    // ── Fábrica de imagem ─────────────────────────────────────────
    private static BufferedImage nova(int w, int h, int tipo) {
        int t = (tipo == BufferedImage.TYPE_CUSTOM || tipo == 0) ? BufferedImage.TYPE_INT_ARGB : tipo;
        return new BufferedImage(w, h, t);
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
        if (sx <= 0 || sy <= 0) throw new IllegalArgumentException("Fatores devem ser positivos.");
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
}