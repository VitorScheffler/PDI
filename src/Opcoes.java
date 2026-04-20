import java.awt.image.BufferedImage;

public class Opcoes {

    // Garante que o valor fique entre 0 e 255
    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    // Garante que o valor fique entre min e max (usado para índices de pixel na borda)
    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // =========================================================
    //  TRANSFORMAÇÕES GEOMÉTRICAS
    // =========================================================

    // Translação: desloca a imagem em X e Y
    public static BufferedImage translacao(BufferedImage img, int tx, int ty) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int xOrigem = x - tx;
                int yOrigem = y - ty;
                // Só copia se a posição de origem estiver dentro da imagem
                if (xOrigem >= 0 && xOrigem < w && yOrigem >= 0 && yOrigem < h) {
                    out.setRGB(x, y, img.getRGB(xOrigem, yOrigem));
                }
            }
        }
        return out;
    }

    // Rotação: gira a imagem em torno do centro pelo ângulo dado (em graus)
    public static BufferedImage rotacao(BufferedImage img, double angulo) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        double rad = Math.toRadians(angulo);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double cx  = (w - 1) / 2.0; // centro X
        double cy  = (h - 1) / 2.0; // centro Y

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double dx = x - cx;
                double dy = y - cy;
                // Rotação inversa
                int xs = (int) Math.round(cos * dx + sin * dy + cx);
                int ys = (int) Math.round(-sin * dx + cos * dy + cy);
                if (xs >= 0 && xs < w && ys >= 0 && ys < h) {
                    out.setRGB(x, y, img.getRGB(xs, ys));
                }
            }
        }
        return out;
    }

    // Escala: redimensiona a imagem pelos fatores sx (largura) e sy (altura)
    // sx > 1 = aumenta; sx < 1 = diminui
    public static BufferedImage escala(BufferedImage img, double sx, double sy) {
        int sw = img.getWidth(),  sh = img.getHeight();
        int dw = Math.max(1, (int) Math.round(sw * sx));
        int dh = Math.max(1, (int) Math.round(sh * sy));
        BufferedImage out = new BufferedImage(dw, dh, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                // Mapeia o pixel destino de volta para a imagem original
                int xs = (int) Math.round(x / sx);
                int ys = (int) Math.round(y / sy);
                if (xs >= 0 && xs < sw && ys >= 0 && ys < sh) {
                    out.setRGB(x, y, img.getRGB(xs, ys));
                }
            }
        }
        return out;
    }

    // Espelhamento horizontal: inverte a posição X de cada pixel
    public static BufferedImage espelharHorizontal(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(x, y, img.getRGB(w - 1 - x, y));

        return out;
    }

    // Espelhamento vertical: inverte a posição Y de cada pixel
    public static BufferedImage espelharVertical(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(x, y, img.getRGB(x, h - 1 - y));

        return out;
    }

    // =========================================================
    //  FILTROS
    // =========================================================

    // Grayscale — método 0: média simples (R+G+B)/3
    // Grayscale — método 1: luminância ITU-R BT.709  (0.2125R + 0.7154G + 0.0721B)
    // Grayscale — método 2: ponderação alternativa   (0.50R   + 0.419G  + 0.081B)
    public static BufferedImage grayscale(BufferedImage img, int metodo) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb   = img.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                int r     = (rgb >> 16) & 0xFF;
                int g     = (rgb >>  8) & 0xFF;
                int b     =  rgb        & 0xFF;

                int gray;
                switch (metodo) {
                    case 1:  // ITU-R BT.709
                        gray = clamp((int) Math.round(0.2125 * r + 0.7154 * g + 0.0721 * b));
                        break;
                    case 2:  // Ponderação alternativa
                        gray = clamp((int) Math.round(0.50 * r + 0.419 * g + 0.081 * b));
                        break;
                    default: // Média simples
                        gray = clamp((r + g + b) / 3);
                        break;
                }

                out.setRGB(x, y, (alpha << 24) | (gray << 16) | (gray << 8) | gray);
            }
        }
        return out;
    }

    // Grayscale com método padrão (média simples) — mantido para uso interno (Sobel, Canny, Threshold)
    public static BufferedImage grayscale(BufferedImage img) {
        return grayscale(img, 0);
    }

    // Brilho: D(x,y) = 1 * f(x,y) + B  →  apenas soma B em cada canal
    // Conforme o PDF: C=1 (sem contraste), B=delta
    public static BufferedImage ajustarBrilho(BufferedImage img, int b) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb   = img.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                int r     = clamp(((rgb >> 16) & 0xFF) + b);
                int g     = clamp(((rgb >>  8) & 0xFF) + b);
                int bv    = clamp(( rgb        & 0xFF) + b);
                out.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | bv);
            }
        }
        return out;
    }

    // Contraste: D(x,y) = C * f(x,y) + 0  →  multiplica cada canal por C
    // Conforme o PDF: B=0 (sem brilho), C=fator
    // Valores resultantes são limitados a [0, 255]
    public static BufferedImage ajustarContraste(BufferedImage img, double c) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb   = img.getRGB(x, y);
                int alpha = (rgb >> 24) & 0xFF;
                int r     = clamp((int) Math.round(c * ((rgb >> 16) & 0xFF)));
                int g     = clamp((int) Math.round(c * ((rgb >>  8) & 0xFF)));
                int b     = clamp((int) Math.round(c * ( rgb        & 0xFF)));
                out.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    // =========================================================
    //  CONVOLUÇÃO — base para passa-baixa e passa-alta
    // =========================================================

    // Aplica um kernel (matriz de pesos) sobre cada pixel da imagem
    // Cada pixel de saída = soma dos vizinhos × pesos do kernel
    // Pixels fora da borda usam o pixel mais próximo (replicação de borda)
    private static BufferedImage convolucao(BufferedImage img, double[][] kernel) {
        int w  = img.getWidth(),  h  = img.getHeight();
        int kh = kernel.length,   kw = kernel[0].length;
        int kr = kh / 2,          kc = kw / 2; // raio do kernel

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double sumR = 0, sumG = 0, sumB = 0;
                int alpha = (img.getRGB(x, y) >> 24) & 0xFF;

                for (int ky = 0; ky < kh; ky++) {
                    for (int kx = 0; kx < kw; kx++) {
                        int px  = clamp(x + kx - kc, 0, w - 1);
                        int py  = clamp(y + ky - kr, 0, h - 1);
                        int rgb = img.getRGB(px, py);
                        double peso = kernel[ky][kx];
                        sumR += ((rgb >> 16) & 0xFF) * peso;
                        sumG += ((rgb >>  8) & 0xFF) * peso;
                        sumB += ( rgb        & 0xFF) * peso;
                    }
                }

                int r = clamp((int) Math.round(sumR));
                int g = clamp((int) Math.round(sumG));
                int b = clamp((int) Math.round(sumB));
                out.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    // Gera um kernel gaussiano de tamanho tam x tam normalizado (soma dos pesos = 1)
    // Pixels mais próximos do centro recebem pesos maiores
    private static double[][] kernelGaussiano(int tam) {
        if (tam % 2 == 0) tam++; // garante tamanho ímpar
        double sigma = tam / 6.0;
        int    c     = tam / 2;
        double[][] k = new double[tam][tam];
        double sum   = 0;

        for (int y = 0; y < tam; y++) {
            for (int x = 0; x < tam; x++) {
                double dx = x - c, dy = y - c;
                k[y][x] = Math.exp(-(dx * dx + dy * dy) / (2 * sigma * sigma));
                sum += k[y][x];
            }
        }
        // Normaliza para que a soma seja 1 (não altera o brilho médio)
        for (int y = 0; y < tam; y++)
            for (int x = 0; x < tam; x++)
                k[y][x] /= sum;

        return k;
    }

    // =========================================================
    //  PASSA BAIXA
    // =========================================================

    // Filtro passa-baixa gaussiano: suaviza a imagem borrando detalhes e ruídos
    public static BufferedImage passaBaixa(BufferedImage img, int tam) {
        return convolucao(img, kernelGaussiano(tam));
    }

    // =========================================================
    //  PASSA ALTA
    // =========================================================

    // Filtro passa-alta: realça bordas e detalhes finos da imagem
    public static BufferedImage passaAlta(BufferedImage img, double k) {
        double[][] kernel = {
            {  0,      -k,      0 },
            { -k,  1 + 4*k,    -k },
            {  0,      -k,      0 }
        };
        return convolucao(img, kernel);
    }

    // =========================================================
    //  SOBEL
    // =========================================================

    // Calcula a magnitude do gradiente Sobel para cada pixel
    private static double[][] sobelMagnitude(BufferedImage img) {
        BufferedImage gray = grayscale(img);
        int w = gray.getWidth(), h = gray.getHeight();

        // Kernels Sobel: detectam variação em X e em Y separadamente
        int[][] Gx = { {-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1} };
        int[][] Gy = { {-1,-2,-1}, { 0, 0, 0}, { 1, 2, 1} };

        double[][] mag = new double[h][w];
        double maxMag  = 1e-9;

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int gx = 0, gy = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int lum = gray.getRGB(x + kx, y + ky) & 0xFF;
                        gx += Gx[ky + 1][kx + 1] * lum;
                        gy += Gy[ky + 1][kx + 1] * lum;
                    }
                }
                // Magnitude do gradiente: sqrt(Gx² + Gy²)
                mag[y][x] = Math.sqrt((double) gx * gx + (double) gy * gy);
                if (mag[y][x] > maxMag) maxMag = mag[y][x];
            }
        }

        // Normaliza para [0, 255]
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                mag[y][x] = mag[y][x] * 255.0 / maxMag;

        return mag;
    }

    // Threshold Sobel: detecta bordas e gera imagem binária (preto e branco)
    public static BufferedImage sobel(BufferedImage img, int limiar) {
        double[][] mag = sobelMagnitude(img);
        int h = mag.length, w = mag[0].length;
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out.setRGB(x, y, mag[y][x] >= limiar ? 0xFFFFFFFF : 0xFF000000);

        return out;
    }

    // =========================================================
    //  CANNY
    // =========================================================

    // Detector de bordas Canny — mais preciso e limpo que o Sobel
    public static BufferedImage canny(BufferedImage img, int limiarBaixo, int limiarAlto) {

        // Passo 1: converte para cinza e aplica blur gaussiano 5x5 (reduz ruído)
        BufferedImage blur = convolucao(grayscale(img), kernelGaussiano(5));
        int w = blur.getWidth(), h = blur.getHeight();

        // Passo 2: calcula gradientes Sobel (magnitude e direção)
        int[][] Gx = { {-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1} };
        int[][] Gy = { {-1,-2,-1}, { 0, 0, 0}, { 1, 2, 1} };
        double[][] mag   = new double[h][w];
        double[][] angle = new double[h][w];
        double maxMag    = 1e-9;

        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int gx = 0, gy = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int lum = blur.getRGB(x + kx, y + ky) & 0xFF;
                        gx += Gx[ky + 1][kx + 1] * lum;
                        gy += Gy[ky + 1][kx + 1] * lum;
                    }
                }
                mag[y][x]   = Math.sqrt((double) gx * gx + (double) gy * gy);
                angle[y][x] = Math.toDegrees(Math.atan2(gy, gx));
                if (mag[y][x] > maxMag) maxMag = mag[y][x];
            }
        }
        // Normaliza magnitudes para [0, 255]
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                mag[y][x] = mag[y][x] * 255.0 / maxMag;

        // Passo 3: supressão de não-máximos
        double[][] supr = new double[h][w];
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                double a = angle[y][x] % 180;
                if (a < 0) a += 180;

                // Seleciona os dois vizinhos na direção do gradiente
                double q, r;
                if      (a < 22.5  || a >= 157.5) { q = mag[y][x+1];   r = mag[y][x-1];   }
                else if (a < 67.5)                  { q = mag[y-1][x+1]; r = mag[y+1][x-1]; }
                else if (a < 112.5)                 { q = mag[y-1][x];   r = mag[y+1][x];   }
                else                                { q = mag[y+1][x+1]; r = mag[y-1][x-1]; }

                // Só mantém se for o maior entre os vizinhos
                supr[y][x] = (mag[y][x] >= q && mag[y][x] >= r) ? mag[y][x] : 0;
            }
        }

        // Passo 4: duplo threshold
        int[][] result = new int[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                if      (supr[y][x] >= limiarAlto)  result[y][x] = 255;
                else if (supr[y][x] >= limiarBaixo) result[y][x] = 128;
            }

        // Passo 5: histerese
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    if (result[y][x] != 128) continue;
                    outer:
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dx = -1; dx <= 1; dx++)
                            if (result[y + dy][x + dx] == 255) {
                                result[y][x] = 255;
                                mudou = true;
                                break outer;
                            }
                }
            }
        }
        // Descarta bordas fracas restantes
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (result[y][x] == 128) result[y][x] = 0;

        // Monta imagem final: branco = borda, preto = fundo
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int v = result[y][x];
                out.setRGB(x, y, 0xFF000000 | (v << 16) | (v << 8) | v);
            }
        return out;
    }

    // =========================================================
    //  THRESHOLD SIMPLES (BINARIZAÇÃO)
    // =========================================================

    // Converte imagem em preto e branco baseado em um limiar
    // Se o pixel >= limiar → branco, senão → preto
    public static BufferedImage threshold(BufferedImage img, int limiar) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        // Garante que estamos trabalhando em cinza
        BufferedImage gray = grayscale(img);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {

                int lum = gray.getRGB(x, y) & 0xFF;

                int cor = (lum >= limiar) ? 255 : 0;

                out.setRGB(x, y, 0xFF000000 | (cor << 16) | (cor << 8) | cor);
            }
        }

        return out;
    }
}