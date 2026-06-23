import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompararBarras {

    /**
     * Verifica se um pixel pertence à cor da barra (rosa/vermelho).
     * Critério: R alto, G e B baixos — robusto para JPEG (que comprime cores).
     */
    private static boolean isBarra(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8)  & 0xFF;
        int b =  rgb        & 0xFF;
        // R dominante, G e B bem menores — cobre variações de compressão JPEG
        return r > 200 && g < 160 && b < 160 && r > g + 60;
    }

    public static String compararBarras(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // 1) Para cada coluna X, conta quantos pixels são da cor da barra
        int[] alturaColuna = new int[w];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (isBarra(img.getRGB(x, y))) {
                    alturaColuna[x]++;
                }
            }
        }

        // 2) Agrupa colunas contíguas em barras distintas
        //    Tolerância: ignora pequenos gaps causados por bordas/arredondamento
        List<Integer> alturasBarras = new ArrayList<>();
        int tolerancia = Math.max(5, w / 100); // ~1% da largura da imagem
        int x = 0;

        while (x < w) {
            if (alturaColuna[x] == 0) { x++; continue; }

            int maxAltura = 0;
            int gapAtual  = 0;
            int inicio    = x;

            while (x < w) {
                if (alturaColuna[x] == 0) {
                    gapAtual++;
                    if (gapAtual > tolerancia) break;
                } else {
                    gapAtual = 0;
                    if (alturaColuna[x] > maxAltura)
                        maxAltura = alturaColuna[x];
                }
                x++;
            }

            // Descarta grupos muito estreitos (ruído, bordas)
            if ((x - inicio) > w / 100) {
                alturasBarras.add(maxAltura);
            }
        }

        if (alturasBarras.isEmpty()) return "Nenhuma barra encontrada.";

        int maior = Collections.max(alturasBarras);
        int menor = Collections.min(alturasBarras);

        return "Maior = " + maior + " | Menor = " + menor
             + "  (barras detectadas: " + alturasBarras.size() + ")"
             + "\n  Alturas em pixels: " + alturasBarras;
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Uso: java CompararBarras <caminho_imagem>");
            return;
        }
        BufferedImage img = ImageIO.read(new File(args[0]));
        if (img == null) {
            System.out.println("Erro: não foi possível ler a imagem.");
            return;
        }
        System.out.println(compararBarras(img));
    }
}