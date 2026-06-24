import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.awt.*;


public class Exercicios {

    // =========================================================
    //  UTILITÁRIOS COMUNS
    // =========================================================

    private static int[][] rotular(BufferedImage bin) {
        int w = bin.getWidth(), h = bin.getHeight();
        int[][] rotulo = new int[h][w]; // 0 = não rotulado
        int proximoRotulo = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean objeto = (bin.getRGB(x, y) & 0xFF) == 0; // preto = objeto
                if (objeto && rotulo[y][x] == 0) {
                    proximoRotulo++;
                    // floodfill com pilha (4 direções)
                    Deque<int[]> pilha = new ArrayDeque<>();
                    pilha.push(new int[]{x, y});
                    rotulo[y][x] = proximoRotulo;

                    while (!pilha.isEmpty()) {
                        int[] p = pilha.pop();
                        int px = p[0], py = p[1];
                        int[][] viz = {{px+1,py}, {px-1,py}, {px,py+1}, {px,py-1}};
                        for (int[] v : viz) {
                            int vx = v[0], vy = v[1];
                            if (vx < 0 || vx >= w || vy < 0 || vy >= h) continue;
                            if (rotulo[vy][vx] != 0) continue;
                            boolean vObjeto = (bin.getRGB(vx, vy) & 0xFF) == 0;
                            if (!vObjeto) continue;
                            rotulo[vy][vx] = proximoRotulo;
                            pilha.push(new int[]{vx, vy});
                        }
                    }
                }
            }
        }
        return rotulo;
    }
    private static int contarRotulos(int[][] rotulo) {
        int max = 0;
        for (int[] linha : rotulo)
            for (int v : linha)
                if (v > max) max = v;
        return max;
    }
    private static int[][] calcularBoundingBoxes(int[][] rotulo, int numRotulos, int w, int h) {
        int[][] bbox = new int[numRotulos + 1][4];
        for (int r = 1; r <= numRotulos; r++) {
            bbox[r][0] = Integer.MAX_VALUE; // minX
            bbox[r][1] = Integer.MAX_VALUE; // minY
            bbox[r][2] = Integer.MIN_VALUE; // maxX
            bbox[r][3] = Integer.MIN_VALUE; // maxY
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = rotulo[y][x];
                if (r == 0) continue;
                if (x < bbox[r][0]) bbox[r][0] = x;
                if (y < bbox[r][1]) bbox[r][1] = y;
                if (x > bbox[r][2]) bbox[r][2] = x;
                if (y > bbox[r][3]) bbox[r][3] = y;
            }
        }
        return bbox;
    }
    private static int[][] calcularCoresMedias(BufferedImage original, int[][] rotulo, int numRotulos) {
        long[][] somaRGB = new long[numRotulos + 1][3];
        int[] contagem = new int[numRotulos + 1];
        int w = original.getWidth(), h = original.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = rotulo[y][x];
                if (r == 0) continue;
                int rgb = original.getRGB(x, y);
                somaRGB[r][0] += (rgb >> 16) & 0xFF;
                somaRGB[r][1] += (rgb >> 8)  & 0xFF;
                somaRGB[r][2] +=  rgb        & 0xFF;
                contagem[r]++;
            }
        }

        int[][] medias = new int[numRotulos + 1][3];
        for (int r = 1; r <= numRotulos; r++) {
            if (contagem[r] == 0) continue;
            medias[r][0] = (int) (somaRGB[r][0] / contagem[r]);
            medias[r][1] = (int) (somaRGB[r][1] / contagem[r]);
            medias[r][2] = (int) (somaRGB[r][2] / contagem[r]);
        }
        return medias;
    }
    private static String classificarCor(int r, int g, int b) {

        // Tolerância para "branco"/"fundo" — ignora regiões muito claras
        if (r > 230 && g > 230 && b > 230) return null; // fundo branco

        if (r > 180 && g > 180 && b < 100) return "amarelo";
        if (r > 150 && g < 100 && b < 100) return "vermelho";
        if (g > 120 && r < 100 && b < 100) return "verde";
        if (b > 120 && r < 100 && g < 100) return "azul";

        // fallback: compara qual canal é dominante
        if (r >= g && r >= b) return "vermelho";
        if (g >= r && g >= b) return "verde";
        return "azul";
    }

    // =========================================================
    //  EXERCÍCIO 1 — Leitura de horário em relógio analógico - OK
    // =========================================================

    public static class ResultadoRelogio {
        double centroX, centroY, raio;
        double anguloMaior, comprimentoMaior; // minutos
        double anguloMenor, comprimentoMenor; // horas
    }
    public static ResultadoRelogio detectarPonteiros(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // 1) Máscara inicial de pixels escuros + bbox para estimar raio/centro aproximados
        boolean[][] escuro = new boolean[h][w];
        int minX = w, maxX = 0, minY = h, maxY = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean e = isEscuro(img.getRGB(x, y));
                escuro[y][x] = e;
                if (e) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }

        double centroAproxX = (minX + maxX) / 2.0;
        double centroAproxY = (minY + maxY) / 2.0;
        double raio = ((maxX - minX) + (maxY - minY)) / 4.0;

        // 1.1) Dilata a máscara para fechar pequenas falhas de conectividade
        //      no traço dos ponteiros (antialiasing / compressão JPEG),
        //      reaproveitando a morfologia já existente em Opcoes.
        escuro = dilatarMascara(escuro, w, h, 2);

        // 2) Os ponteiros formam UM ÚNICO componente conectado (compartilham o
        //    pino central). Números e marcações são componentes separados —
        //    isolando o componente que contém o centro, descartamos tudo o resto.
        int[] seed = encontrarPixelEscuroMaisProximo(escuro, w, h,
                (int) Math.round(centroAproxX), (int) Math.round(centroAproxY),
                (int) (raio * 0.15));

        ResultadoRelogio vazio = new ResultadoRelogio();
        vazio.anguloMenor = -1;
        if (seed == null) return vazio;

        List<int[]> componentePonteiros = floodFillComponente(escuro, w, h, seed[0], seed[1]);

        // 3) Centro refinado: centróide dos pixels do componente bem próximos
        //    do centro aproximado (região do pino) — mais preciso que o bbox geral.
        double somaX = 0, somaY = 0;
        int contPino = 0;
        double raioPino = raio * 0.06;
        for (int[] p : componentePonteiros) {
            double dx = p[0] - centroAproxX;
            double dy = p[1] - centroAproxY;
            if (Math.sqrt(dx * dx + dy * dy) <= raioPino) {
                somaX += p[0];
                somaY += p[1];
                contPino++;
            }
        }
        double centroX = (contPino > 0) ? somaX / contPino : centroAproxX;
        double centroY = (contPino > 0) ? somaY / contPino : centroAproxY;

        // 4) Perfil radial — agora só sobre os pixels dos ponteiros
        //    (números/marcações já ficaram fora do componente).
        int bins = 360;
        double[] maxDistPorAngulo = new double[bins];

        for (int[] p : componentePonteiros) {
            double dx = p[0] - centroX;
            double dy = centroY - p[1]; // inverte Y
            double dist = Math.sqrt(dx * dx + dy * dy);

            double anguloGraus = Math.toDegrees(Math.atan2(dy, dx));
            if (anguloGraus < 0) anguloGraus += 360;

            int bin = (int) (anguloGraus / (360.0 / bins)) % bins;
            if (dist > maxDistPorAngulo[bin]) {
                maxDistPorAngulo[bin] = dist;
            }
        }

        // 5) Ponteiro mais longo = maior valor do perfil radial
        int binMaior = 0;
        for (int i = 1; i < bins; i++) {
            if (maxDistPorAngulo[i] > maxDistPorAngulo[binMaior]) binMaior = i;
        }
        double comprimentoMaior = maxDistPorAngulo[binMaior];

        // 6) Em vez de uma janela fixa de exclusão em graus, descobre
        //    dinamicamente até onde o ponteiro maior se estende angularmente
        //    (cobre a ponta em seta, que costuma ser mais larga que o traço).
        double limiarOcupacao = comprimentoMaior * 0.5;
        boolean[] excluido = new boolean[bins];
        excluido[binMaior] = true;

        int i = binMaior;
        while (true) {
            int prox = (i + 1) % bins;
            if (prox == binMaior || maxDistPorAngulo[prox] < limiarOcupacao) break;
            excluido[prox] = true;
            i = prox;
        }
        i = binMaior;
        while (true) {
            int prox = (i - 1 + bins) % bins;
            if (prox == binMaior || maxDistPorAngulo[prox] < limiarOcupacao) break;
            excluido[prox] = true;
            i = prox;
        }

        // Margem extra de segurança (alguns graus) para garantir que não
        // sobre nenhum resquício da base larga do ponteiro maior
        int margemExtra = 8;
        boolean[] excluidoFinal = excluido.clone();
        for (int b = 0; b < bins; b++) {
            if (!excluido[b]) continue;
            for (int d = -margemExtra; d <= margemExtra; d++) {
                excluidoFinal[((b + d) % bins + bins) % bins] = true;
            }
        }

        // 7) Ponteiro mais curto = maior valor do perfil radial fora da região excluída
        int binMenor = -1;
        double comprimentoMenor = -1;
        for (int b = 0; b < bins; b++) {
            if (excluidoFinal[b]) continue;
            if (maxDistPorAngulo[b] > comprimentoMenor) {
                comprimentoMenor = maxDistPorAngulo[b];
                binMenor = b;
            }
        }

        ResultadoRelogio r = new ResultadoRelogio();
        r.centroX = centroX;
        r.centroY = centroY;
        r.raio = raio;
        r.anguloMaior = binMaior * (360.0 / bins);
        r.comprimentoMaior = comprimentoMaior;
        r.anguloMenor = (binMenor == -1) ? -1 : binMenor * (360.0 / bins);
        r.comprimentoMenor = comprimentoMenor;
        return r;
    }
    private static boolean[][] dilatarMascara(boolean[][] mask, int w, int h, int iteracoes) {
        BufferedImage bin = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                bin.setRGB(x, y, mask[y][x] ? 0xFFFFFFFF : 0xFF000000); // objeto=branco, fundo=preto

        for (int it = 0; it < iteracoes; it++) {
            bin = Opcoes.dilatacao(bin);
        }

        boolean[][] out = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                out[y][x] = (bin.getRGB(x, y) & 0xFF) > 128; // branco = objeto

        return out;
    }
    private static int[] encontrarPixelEscuroMaisProximo(boolean[][] escuro, int w, int h,
                                                        int cx, int cy, int raioMax) {
        if (cx >= 0 && cx < w && cy >= 0 && cy < h && escuro[cy][cx]) {
            return new int[]{cx, cy};
        }
        for (int r = 1; r <= raioMax; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue; // só o "anel" externo
                    int x = cx + dx, y = cy + dy;
                    if (x < 0 || x >= w || y < 0 || y >= h) continue;
                    if (escuro[y][x]) return new int[]{x, y};
                }
            }
        }
        return null;
    }
    private static List<int[]> floodFillComponente(boolean[][] escuro, int w, int h,
                                                    int startX, int startY) {
        List<int[]> pixels = new ArrayList<>();
        boolean[][] visitado = new boolean[h][w];
        Deque<int[]> pilha = new ArrayDeque<>();
        pilha.push(new int[]{startX, startY});
        visitado[startY][startX] = true;

        while (!pilha.isEmpty()) {
            int[] p = pilha.pop();
            int px = p[0], py = p[1];
            pixels.add(p);

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int x = px + dx, y = py + dy;
                    if (x < 0 || x >= w || y < 0 || y >= h) continue;
                    if (visitado[y][x] || !escuro[y][x]) continue;
                    visitado[y][x] = true;
                    pilha.push(new int[]{x, y});
                }
            }
        }
        return pixels;
    }
    public static String formatarResultadoRelogio(ResultadoRelogio r) {
        if (r.anguloMenor < 0) {
            return "Não foi possível identificar dois ponteiros distintos.";
        }
        return String.format(
            "Centro: (%.0f, %.0f) | Raio: %.0f px%n" +
            "Ponteiro MINUTOS (longo):  %.1f px  (ângulo ~%.0f°)%n" +
            "Ponteiro HORAS   (curto):  %.1f px  (ângulo ~%.0f°)%n" +
            "Horário detectado: %s",
            r.centroX, r.centroY, r.raio,
            r.comprimentoMaior, r.anguloMaior,
            r.comprimentoMenor, r.anguloMenor,
            calcularHorario(r)
        );
    }
    public static BufferedImage desenharPonteiros(BufferedImage original, ResultadoRelogio r) {
        BufferedImage out = new BufferedImage(
            original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(original, 0, 0, null);

        if (r.anguloMenor >= 0) {
            g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
            g2.setStroke(new BasicStroke(3));

            // Ponteiro dos MINUTOS — azul
            desenharPonteiro(g2, r.centroX, r.centroY, r.anguloMaior, r.comprimentoMaior,
                    new Color(30, 100, 230), "MIN: " + (int) r.comprimentoMaior + "px");

            // Ponteiro das HORAS — vermelho
            desenharPonteiro(g2, r.centroX, r.centroY, r.anguloMenor, r.comprimentoMenor,
                    new Color(220, 40, 40), "HOR: " + (int) r.comprimentoMenor + "px");

            // Marca o centro
            g2.setColor(Color.GREEN);
            int raioMarcador = 4;
            g2.fillOval((int) r.centroX - raioMarcador, (int) r.centroY - raioMarcador,
                    raioMarcador * 2, raioMarcador * 2);
        }

        g2.dispose();
        return out;
    }
    private static void desenharPonteiro(Graphics2D g2, double cx, double cy,
                                        double anguloGraus, double comprimento,
                                        Color cor, String rotulo) {
        double rad = Math.toRadians(anguloGraus);
        double px = cx + comprimento * Math.cos(rad);
        double py = cy - comprimento * Math.sin(rad); // inverte Y de volta

        g2.setColor(cor);
        g2.drawLine((int) cx, (int) cy, (int) px, (int) py);

        // Pequeno círculo na ponta do ponteiro
        g2.fillOval((int) px - 5, (int) py - 5, 10, 10);

        // Texto com o comprimento, deslocado um pouco da ponta
        int textX = (int) px + 8;
        int textY = (int) py;
        g2.setColor(Color.BLACK);
        g2.drawString(rotulo, textX + 1, textY + 1); // sombra leve
        g2.setColor(cor);
        g2.drawString(rotulo, textX, textY);
    }
    private static boolean isEscuro(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8)  & 0xFF;
        int b =  rgb        & 0xFF;
        // Considera "escuro" qualquer pixel próximo do preto, com tolerância
        // a cinzas intermediários (antialiasing / compressão JPEG)
        return (r + g + b) < 380;
    }
    public static String calcularHorario(ResultadoRelogio r) {
        if (r.anguloMenor < 0) {
            return "Não foi possível calcular o horário.";
        }

        // Converte ângulo matemático → ângulo do relógio
        // Ângulo matemático: 0° = direita, anti-horário
        // Ângulo do relógio: 0° = 12h, horário
        double anguloMinutos = (90.0 - r.anguloMaior % 360 + 360) % 360;
        double anguloHoras   = (90.0 - r.anguloMenor % 360 + 360) % 360;

        // Minutos: cada minuto = 6°
        int minutos = (int) Math.round(anguloMinutos / 6.0) % 60;

        // Horas: cada hora = 30°; o ponteiro está sempre na hora exata
        int horas = (int) Math.round(anguloHoras / 30.0) % 12;
        if (horas == 0) horas = 12; // 0 → 12

        return String.format("%02d:%02d", horas, minutos);
    }

    // =========================================================
    //  EXERCÍCIO 2 — Contagem de objetos coloridos por cor - OK
    // =========================================================

    public static String contarObjetosPorCor(BufferedImage img) {
        // Considera "objeto" qualquer pixel que não seja próximo do branco
        BufferedImage bin = binarizarPorCor(img);
        int[][] rotulo = rotular(bin);
        int n = contarRotulos(rotulo);

        if (n == 0) return "Nenhum objeto encontrado.";

        int[][] cores = calcularCoresMedias(img, rotulo, n);

        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (int r = 1; r <= n; r++) {
            String nome = classificarCor(cores[r][0], cores[r][1], cores[r][2]);
            if (nome == null) continue;
            contagem.merge(nome, 1, Integer::sum);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : contagem.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getValue()).append(" objeto(s) ").append(e.getKey());
        }
        return sb.length() == 0 ? "Nenhum objeto colorido identificado." : sb.toString();
    }
    private static BufferedImage binarizarPorCor(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                boolean fundo = (r > 230 && g > 230 && b > 230);
                int v = fundo ? 255 : 0;
                out.setRGB(x, y, 0xFF000000 | (v << 16) | (v << 8) | v);
            }
        }
        return out;
    }
    public static BufferedImage desenharObjetosPorCor(BufferedImage img) {
        BufferedImage bin = binarizarPorCor(img);
        int[][] rotulo = rotular(bin);
        int n = contarRotulos(rotulo);

        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(img, 0, 0, null);

        if (n == 0) { g2.dispose(); return out; }

        int w = img.getWidth(), h = img.getHeight();
        int[][] cores  = calcularCoresMedias(img, rotulo, n);
        int[][] bboxes = calcularBoundingBoxes(rotulo, n, w, h);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setStroke(new BasicStroke(2.5f));

        for (int r = 1; r <= n; r++) {
            String nome = classificarCor(cores[r][0], cores[r][1], cores[r][2]);
            if (nome == null) continue;

            int minX = bboxes[r][0], minY = bboxes[r][1];
            int maxX = bboxes[r][2], maxY = bboxes[r][3];
            int bw = maxX - minX + 1, bh = maxY - minY + 1;

            // Cor de destaque baseada na classificação
            Color cor = switch (nome) {
                case "vermelho" -> new Color(220, 50,  50);
                case "verde"    -> new Color(50,  200, 50);
                case "azul"     -> new Color(50,  100, 230);
                case "amarelo"  -> new Color(220, 180, 0);
                default         -> Color.WHITE;
            };

            // Bounding box
            g2.setColor(cor);
            g2.drawRect(minX, minY, bw, bh);

            // Fundo semitransparente para o label
            String label = nome + " (RGB " + cores[r][0] + "," + cores[r][1] + "," + cores[r][2] + ")";
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(label) + 6;
            int th = fm.getHeight();
            int ly = minY - th - 2 < 0 ? maxY + 4 : minY - 4;

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(minX, ly, tw, th + 4, 4, 4);

            g2.setColor(cor);
            g2.drawString(label, minX + 3, ly + th);
        }

        g2.dispose();
        return out;
    }

    // =========================================================
    //  EXERCÍCIO 3 — Identificação de letras presentes na imagem - OK
    // =========================================================

    public static String identificarLetras(BufferedImage img) {

        BufferedImage bin = Opcoes.threshold(img, 160);

        int[][] rotulo = rotular(bin);
        int n = contarRotulos(rotulo);
        if (n == 0) return "Nenhuma letra encontrada.";

        int w = bin.getWidth(), h = bin.getHeight();
        int[][] bbox = calcularBoundingBoxes(rotulo, n, w, h);

        Set<String> letras = new LinkedHashSet<>();

        for (int r = 1; r <= n; r++) {
            int minX = bbox[r][0], minY = bbox[r][1];
            int maxX = bbox[r][2], maxY = bbox[r][3];
            if (minX > maxX || minY > maxY) continue;

            int largura = maxX - minX + 1;
            int altura  = maxY - minY + 1;

            int areaImg = w * h;
            if (largura * altura < areaImg / 200) continue;

            int pad = 4;
            int subW = largura + 2 * pad;
            int subH = altura  + 2 * pad;

            boolean[][] obj = new boolean[subH][subW];
            BufferedImage sub = new BufferedImage(subW, subH, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < subH; y++)
                for (int x = 0; x < subW; x++)
                    sub.setRGB(x, y, 0xFFFFFF);

            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (rotulo[y][x] == r) {
                        int sx = x - minX + pad;
                        int sy = y - minY + pad;
                        sub.setRGB(sx, sy, 0x000000);
                        obj[sy][sx] = true;
                    }
                }
            }

            int buracos = contarBuracos(obj, subW, subH);

            BufferedImage esqueletoImg = Opcoes.afinamento(sub);
            boolean[][] sk = new boolean[subH][subW];
            for (int y = 0; y < subH; y++)
                for (int x = 0; x < subW; x++)
                    sk[y][x] = (esqueletoImg.getRGB(x, y) & 0xFF) == 0;

            // ── CORREÇÃO PRINCIPAL: iterações calibradas pela espessura do traço ──
            int espessuraEstimada = Math.min(largura, altura) / 6;
            int iteracoes = Math.max(3, Math.min(12, espessuraEstimada / 2));

            boolean[][] skPodado = podarEsqueleto(sk, subW, subH, iteracoes);

            int[] topo = contarExtremidadesEJuncoes(skPodado, subW, subH);
            int finais  = topo[0];
            int juncoes = topo[1];

            int inkEsq = 0, inkDir = 0;
            int meio = subW / 2;
            for (int y = 0; y < subH; y++) {
                for (int x = 0; x < subW; x++) {
                    if (!obj[y][x]) continue;
                    if (x < meio) inkEsq++; else inkDir++;
                }
            }
            double razaoEsq = (inkEsq + inkDir > 0)
                ? (double) inkEsq / (inkEsq + inkDir) : 0.5;

            // ── DEBUG — aparece no console/terminal ──
            System.out.printf("DEBUG r=%d  bbox=%dx%d  buracos=%d  finais=%d  juncoes=%d  iteracoes=%d  razaoEsq=%.2f%n",
                r, largura, altura, buracos, finais, juncoes, iteracoes, razaoEsq);

            String letra = classificarLetra(
                r,
                buracos,
                finais,
                juncoes,
                razaoEsq,
                largura,
                altura
            );
            if (letra != null) letras.add(letra);
        }

        if (letras.isEmpty()) return "Nenhuma letra identificada.";

        StringBuilder sb = new StringBuilder();
        for (String l : letras) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(l);
        }
        return sb.toString();
    }
    private static int contarBuracos(boolean[][] obj, int w, int h) {
        boolean[][] visitado = new boolean[h][w];
        Deque<int[]> pilha = new ArrayDeque<>();

        // Inicia o flood-fill a partir de toda a borda (pixels de fundo)
        for (int x = 0; x < w; x++) {
            if (!obj[0][x])     { visitado[0][x] = true;     pilha.push(new int[]{x, 0}); }
            if (!obj[h-1][x])   { visitado[h-1][x] = true;   pilha.push(new int[]{x, h-1}); }
        }
        for (int y = 0; y < h; y++) {
            if (!obj[y][0])     { visitado[y][0] = true;     pilha.push(new int[]{0, y}); }
            if (!obj[y][w-1])   { visitado[y][w-1] = true;   pilha.push(new int[]{w-1, y}); }
        }

        while (!pilha.isEmpty()) {
            int[] p = pilha.pop();
            int px = p[0], py = p[1];
            int[][] viz = {{px+1,py},{px-1,py},{px,py+1},{px,py-1}};
            for (int[] v : viz) {
                int vx = v[0], vy = v[1];
                if (vx < 0 || vx >= w || vy < 0 || vy >= h) continue;
                if (visitado[vy][vx] || obj[vy][vx]) continue;
                visitado[vy][vx] = true;
                pilha.push(new int[]{vx, vy});
            }
        }

        // Qualquer pixel de fundo não alcançado pela borda é um buraco novo
        int buracos = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (obj[y][x] || visitado[y][x]) continue;
                buracos++;
                Deque<int[]> p2 = new ArrayDeque<>();
                p2.push(new int[]{x, y});
                visitado[y][x] = true;
                while (!p2.isEmpty()) {
                    int[] p = p2.pop();
                    int px = p[0], py = p[1];
                    int[][] viz = {{px+1,py},{px-1,py},{px,py+1},{px,py-1}};
                    for (int[] v : viz) {
                        int vx = v[0], vy = v[1];
                        if (vx < 0 || vx >= w || vy < 0 || vy >= h) continue;
                        if (visitado[vy][vx] || obj[vy][vx]) continue;
                        visitado[vy][vx] = true;
                        p2.push(new int[]{vx, vy});
                    }
                }
            }
        }
        return buracos;
    }
    private static boolean[][] podarEsqueleto(boolean[][] sk, int w, int h, int iteracoes) {
        boolean[][] atual = sk;
        for (int it = 0; it < iteracoes; it++) {
            List<int[]> remover = new ArrayList<>();
            for (int y = 1; y < h - 1; y++) {
                for (int x = 1; x < w - 1; x++) {
                    if (!atual[y][x]) continue;
                    int grau = 0;
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            if (atual[y + dy][x + dx]) grau++;
                        }
                    if (grau <= 1) remover.add(new int[]{y, x});
                }
            }
            if (remover.isEmpty()) break;

            boolean[][] novo = new boolean[h][w];
            for (int y = 0; y < h; y++)
                System.arraycopy(atual[y], 0, novo[y], 0, w);
            for (int[] p : remover) novo[p[0]][p[1]] = false;
            atual = novo;
        }
        return atual;
    }
    private static int[] contarExtremidadesEJuncoes(boolean[][] sk, int w, int h) {
        int finais = 0, juncoes = 0;
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                if (!sk[y][x]) continue;
                int grau = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        if (sk[y + dy][x + dx]) grau++;
                    }
                if (grau == 1) finais++;
                else if (grau >= 3) juncoes++;
            }
        }
        return new int[]{finais, juncoes};
        // ← APAGUE AS DUAS LINHAS DO printf QUE ESTÃO AQUI
    }
    private static String classificarLetra(
        int r,
        int buracos,
        int finais,
        int juncoes,
        double razaoEsq,
        int largura,
        int altura
    ) {

        if (buracos >= 2)
            return "B";

        if (buracos == 1)
            return "A";

        double aspecto = (double) largura / altura;

        // C costuma concentrar mais pixels à esquerda
        if (razaoEsq > 0.60)
            return "C";

        // M é a mais larga
        if (aspecto > 1.15)
            return "M";

        // Z é mais larga que alta? não.
        if (aspecto < 0.95)
            return "Z";

        // X costuma ser um pouco mais larga
        if (aspecto > 1.04)
            return "X";

        return "Y";
    }
    public static BufferedImage desenharLetras(BufferedImage img) {
        BufferedImage bin = Opcoes.threshold(img, 160);
        int[][] rotulo = rotular(bin);
        int n = contarRotulos(rotulo);

        BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(img, 0, 0, null);

        if (n == 0) { g2.dispose(); return out; }

        int w = bin.getWidth(), h = bin.getHeight();
        int[][] bbox = calcularBoundingBoxes(rotulo, n, w, h);

        g2.setStroke(new BasicStroke(2));
        g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

        for (int r = 1; r <= n; r++) {
            int minX = bbox[r][0], minY = bbox[r][1];
            int maxX = bbox[r][2], maxY = bbox[r][3];
            if (minX > maxX || minY > maxY) continue;

            int largura = maxX - minX + 1;
            int altura  = maxY - minY + 1;
            int areaImg = w * h;
            if (largura * altura < areaImg / 200) continue;

            int pad = 4;
            int subW = largura + 2 * pad, subH = altura + 2 * pad;
            boolean[][] obj = new boolean[subH][subW];
            BufferedImage sub = new BufferedImage(subW, subH, BufferedImage.TYPE_INT_RGB);
            for (int y2 = 0; y2 < subH; y2++)
                for (int x2 = 0; x2 < subW; x2++)
                    sub.setRGB(x2, y2, 0xFFFFFF);
            for (int y2 = minY; y2 <= maxY; y2++)
                for (int x2 = minX; x2 <= maxX; x2++)
                    if (rotulo[y2][x2] == r) {
                        sub.setRGB(x2 - minX + pad, y2 - minY + pad, 0x000000);
                        obj[y2 - minY + pad][x2 - minX + pad] = true;
                    }

            int buracos = contarBuracos(obj, subW, subH);
            BufferedImage esqueletoImg = Opcoes.afinamento(sub);
            boolean[][] sk = new boolean[subH][subW];
            for (int y2 = 0; y2 < subH; y2++)
                for (int x2 = 0; x2 < subW; x2++)
                    sk[y2][x2] = (esqueletoImg.getRGB(x2, y2) & 0xFF) == 0;

            int espessura = Math.min(largura, altura) / 6;
            int iteracoes = Math.max(3, Math.min(12, espessura / 2));
            boolean[][] skPodado = podarEsqueleto(sk, subW, subH, iteracoes);
            int[] topo = contarExtremidadesEJuncoes(skPodado, subW, subH);
            int finais = topo[0], juncoes = topo[1];

            int inkEsq = 0, inkDir = 0, meio = subW / 2;
            for (int y2 = 0; y2 < subH; y2++)
                for (int x2 = 0; x2 < subW; x2++) {
                    if (!obj[y2][x2]) continue;
                    if (x2 < meio) inkEsq++; else inkDir++;
                }
            double razaoEsq = (inkEsq + inkDir > 0) ? (double) inkEsq / (inkEsq + inkDir) : 0.5;

            String letra = classificarLetra(r, buracos, finais, juncoes, razaoEsq, largura, altura);
            if (letra == null) continue;

            // Bounding box em ciano
            g2.setColor(new Color(0, 200, 220));
            g2.drawRect(minX, minY, largura, altura);

            // Label com letra e features usadas
            String info = String.format("%s  bur=%d fin=%d jun=%d  razE=%.2f", letra, buracos, finais, juncoes, razaoEsq);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(info) + 6;
            int th = fm.getHeight();
            int ly = minY - th - 4 < 0 ? maxY + 4 : minY - 4;

            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(minX, ly, tw, th + 4, 4, 4);
            g2.setColor(new Color(0, 220, 240));
            g2.drawString(info, minX + 3, ly + th);

            // Letra grande sobreposta
            g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(18, altura / 2)));
            g2.setColor(new Color(255, 255, 0, 180));
            g2.drawString(letra, minX + largura / 2 - g2.getFontMetrics().stringWidth(letra) / 2,
                    minY + altura / 2 + g2.getFontMetrics().getAscent() / 2);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
        }

        g2.dispose();
        return out;
    }

    // =================================================================
    //  EXERCÍCIO 4 — Identificação de Placas de Trânsito - OK
    // =================================================================

    public static String identificarPlacas(BufferedImage img) {

        int w = img.getWidth();
        int h = img.getHeight();
        int cx = w / 2;
        int cy = h / 2;

        BufferedImage bordas = Opcoes.sobel(img, 10);

        // ── Histograma de raios ──────────────────────────────────────

        int maxRaioHist = (int)(Math.min(w, h) * 0.75);
        int[] histRaio = new int[maxRaioHist + 1];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                    int dist = (int) Math.round(
                        Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
                    );
                    if (dist <= maxRaioHist) histRaio[dist]++;
                }
            }
        }

        int[] histSmooth = new int[maxRaioHist + 1];
        for (int i = 2; i < maxRaioHist - 2; i++) {
            histSmooth[i] = (
                histRaio[i - 2] + histRaio[i - 1] + histRaio[i] +
                histRaio[i + 1] + histRaio[i + 2]
            ) / 5;
        }

        int raioPico = 0, picVal = 0;
        for (int i = 0; i <= maxRaioHist; i++) {
            if (histSmooth[i] > picVal) { picVal = histSmooth[i]; raioPico = i; }
        }

        double rMin = raioPico * 0.88;
        double rMax = raioPico * 1.12;

        // ── Recalcula centro real da placa ───────────────────────────
        // O cx/cy inicial era o centro da imagem (chute inicial).
        // Agora pegamos a média das posições dos pixels que estão
        // no anel do raio detectado — esse é o centro geométrico real.

        System.out.printf("DEBUG raio: raioPico=%d rMin=%.0f rMax=%.0f%n", raioPico, rMin, rMax);
        System.out.printf("DEBUG centro: cx=%d cy=%d%n", cx, cy);

        // ── FEATURE 1: score de circularidade ───────────────────────

        int totalBorda = 0, naCircunferencia = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                    double dist = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                    if (dist > rMin * 0.5) {
                        totalBorda++;
                        if (dist >= rMin && dist <= rMax) naCircunferencia++;
                    }
                }
            }
        }

        double scoreCirculo = (totalBorda > 0)
            ? (double) naCircunferencia / totalBorda : 0;

        System.out.printf("DEBUG F1: totalBorda=%d naCirc=%d score=%.3f%n",
            totalBorda, naCircunferencia, scoreCirculo);

        // ── FEATURE 2: setores da circunferência ────────────────────

        int[] setores = new int[8];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                    double dist = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                    if (dist >= rMin && dist <= rMax) {
                        double angulo = Math.toDegrees(Math.atan2(y - cy, x - cx));
                        if (angulo < 0) angulo += 360;
                        setores[(int)(angulo / 45) % 8]++;
                    }
                }
            }
        }

        int maxSetor = 0;
        for (int c : setores) maxSetor = Math.max(maxSetor, c);

        int limiar = maxSetor / 6;
        int setoresPreenchidos = 0;
        for (int c : setores) { if (c > limiar) setoresPreenchidos++; }

        System.out.printf("DEBUG F2: setores=%d|%d|%d|%d|%d|%d|%d|%d preenchidos=%d%n",
            setores[0], setores[1], setores[2], setores[3],
            setores[4], setores[5], setores[6], setores[7],
            setoresPreenchidos);

        // ── Decisão PARE vs Sentido Proibido ─────────────────────────

        if (scoreCirculo < 0.35) {
            if (setoresPreenchidos <= 5) {
                return "PARE";
            } else {
                return "Sentido Proibido";
            }
        }

        // ── Decisão Velocidade Máxima ─────────────────────────────────

        if (setoresPreenchidos >= 6 || scoreCirculo > 0.44) {
            return "Velocidade Maxima";
        }

        // ── FEATURE 3: borda interna — Sentido Proibido vs Proibido Estacionar

        double rInterno = raioPico * 0.70;
        int bxMin = w, bxMax = 0, byMin = h, byMax = 0;
        boolean temBordaInterna = false;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                    double dist = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                    if (dist < rInterno) {
                        temBordaInterna = true;
                        if (x < bxMin) bxMin = x;
                        if (x > bxMax) bxMax = x;
                        if (y < byMin) byMin = y;
                        if (y > byMax) byMax = y;
                    }
                }
            }
        }

        if (temBordaInterna) {
            double razaoLH = (byMax > byMin)
                ? (double)(bxMax - bxMin) / (byMax - byMin) : 0;

            System.out.printf("DEBUG F3: larg=%d alt=%d razaoLH=%.3f%n",
                bxMax - bxMin, byMax - byMin, razaoLH);

            if (razaoLH > 1.5) return "Sentido Proibido";
        }

        return "Proibido Estacionar";
    }

    static class ResultadoPlaca {
        String tipo;
        double scoreCirculo;
        int setoresPreenchidos;
        int[] setores;
        boolean temBordaInterna;
        double razaoLH;
        int bxMin, bxMax, byMin, byMax;
        int raioPico;
        int cx, cy;
    }

    public static BufferedImage desenharPlacas(BufferedImage img, String tipoDetectado) {
        int w = img.getWidth(), h = img.getHeight();
        int cx = w / 2, cy = h / 2;

        BufferedImage bordas = Opcoes.sobel(img, 10);

        // ── Recalcula raio pico ──────────────────────────────────────
        int maxRaioHist = (int)(Math.min(w, h) * 0.75);
        int[] histRaio = new int[maxRaioHist + 1];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                    int dist = (int) Math.round(Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy)));
                    if (dist <= maxRaioHist) histRaio[dist]++;
                }
        int[] histSmooth = new int[maxRaioHist + 1];
        for (int i = 2; i < maxRaioHist - 2; i++)
            histSmooth[i] = (histRaio[i-2] + histRaio[i-1] + histRaio[i] + histRaio[i+1] + histRaio[i+2]) / 5;
        int raioPico = 0, picVal = 0;
        for (int i = 0; i <= maxRaioHist; i++)
            if (histSmooth[i] > picVal) { picVal = histSmooth[i]; raioPico = i; }

        double rMin = raioPico * 0.88, rMax = raioPico * 1.12;

        // ── F1: score de circularidade ───────────────────────────────
        int totalBorda = 0, naCirc = 0;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                    double dist = Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
                    if (dist > rMin * 0.5) {
                        totalBorda++;
                        if (dist >= rMin && dist <= rMax) naCirc++;
                    }
                }
        double scoreCirculo = totalBorda > 0 ? (double) naCirc / totalBorda : 0;

        // ── F2: setores ──────────────────────────────────────────────
        int[] setores = new int[8];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                    double dist = Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
                    if (dist >= rMin && dist <= rMax) {
                        double angulo = Math.toDegrees(Math.atan2(y - cy, x - cx));
                        if (angulo < 0) angulo += 360;
                        setores[(int)(angulo / 45) % 8]++;
                    }
                }
        int maxSetor = 0;
        for (int c : setores) maxSetor = Math.max(maxSetor, c);
        int limiar = maxSetor / 6;
        int setoresPreench = 0;
        for (int c : setores) if (c > limiar) setoresPreench++;

        // ── F3: borda interna ────────────────────────────────────────
        double rInterno = raioPico * 0.70;
        int bxMin = w, bxMax = 0, byMin = h, byMax = 0;
        boolean temBordaInterna = false;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                    double dist = Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
                    if (dist < rInterno) {
                        temBordaInterna = true;
                        if (x < bxMin) bxMin = x;
                        if (x > bxMax) bxMax = x;
                        if (y < byMin) byMin = y;
                        if (y > byMax) byMax = y;
                    }
                }
        double razaoLH = (temBordaInterna && byMax > byMin)
            ? (double)(bxMax - bxMin) / (byMax - byMin) : 0;

        // ── Qual feature foi decisiva? ───────────────────────────────
        String featureDecisiva;
        if (scoreCirculo < 0.35) {
            featureDecisiva = setoresPreench <= 5
                ? "F1+F2: score baixo + poucos setores → PARE"
                : "F1+F2: score baixo + muitos setores → Sentido Proibido";
        } else if (setoresPreench >= 6 || scoreCirculo > 0.44) {
            featureDecisiva = "F1+F2: score alto / setores cheios → Vel. Maxima";
        } else if (temBordaInterna && razaoLH > 1.5) {
            featureDecisiva = "F3: borda interna larga (razao=" + String.format("%.2f", razaoLH) + ") → Sent. Proibido";
        } else {
            featureDecisiva = "F3: borda interna vertical (razao=" + String.format("%.2f", razaoLH) + ") → Prob. Estacionar";
        }

        // ── Desenho ──────────────────────────────────────────────────
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(img, 0, 0, null);

        // Anel de detecção (amarelo = zona onde procura a borda da placa)
        g2.setColor(new Color(255, 200, 0, 80));
        g2.fillOval((int)(cx - rMax), (int)(cy - rMax), (int)(2*rMax), (int)(2*rMax));
        g2.setColor(new Color(30, 30, 30, 80));
        g2.fillOval((int)(cx - rMin), (int)(cy - rMin), (int)(2*rMin), (int)(2*rMin));

        // Círculo do raio pico (tracejado amarelo)
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1, new float[]{8, 5}, 0));
        g2.setColor(new Color(255, 200, 0));
        g2.drawOval(cx - raioPico, cy - raioPico, 2*raioPico, 2*raioPico);

        // Círculo interno do F3 (tracejado ciano) — onde procura símbolo interno
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1, new float[]{5, 4}, 0));
        g2.setColor(new Color(0, 220, 255, 150));
        int ri = (int) rInterno;
        g2.drawOval(cx - ri, cy - ri, 2*ri, 2*ri);

        // Ponto central
        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.YELLOW);
        g2.fillOval(cx - 5, cy - 5, 10, 10);

        // Pixels da borda interna (F3) destacados em ciano
        if (temBordaInterna) {
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++)
                    if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                        double dist = Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
                        if (dist < rInterno)
                            out.setRGB(x, y, new Color(0, 255, 255, 200).getRGB());
                    }

            // Retângulo bounding box da região interna
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(0, 220, 255));
            g2.drawRect(bxMin, byMin, bxMax - bxMin, byMax - byMin);
        }

        // Pixels que estão NO ANEL (F1) destacados em laranja
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                    double dist = Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy));
                    if (dist >= rMin && dist <= rMax)
                        out.setRGB(x, y, new Color(255, 140, 0, 220).getRGB());
                }

        // Arcos dos setores (F2) — verde = ativo, vermelho = vazio
        g2.setStroke(new BasicStroke(7));
        for (int s = 0; s < 8; s++) {
            double angInicio = s * 45.0 - 90;
            boolean ativo = setores[s] > limiar;
            g2.setColor(ativo ? new Color(0, 255, 120, 180) : new Color(255, 60, 60, 80));
            g2.drawArc(cx - raioPico, cy - raioPico, 2*raioPico, 2*raioPico,
                    (int)-angInicio, -40);
        }

        // ── Painel de debug ──────────────────────────────────────────
        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        String[] linhas = {
            "Tipo: " + tipoDetectado,
            String.format("Raio detectado: %d px", raioPico),
            String.format("F1 Score circular: %.3f  (thresh 0.35 / 0.44)", scoreCirculo),
            String.format("F2 Setores preench: %d / 8  (thresh 5 ou 6)", setoresPreench),
            String.format("F3 Borda interna: %s  razao=%.2f  (thresh 1.5)",
                temBordaInterna ? "SIM" : "NAO", razaoLH),
            "Decisao: " + featureDecisiva
        };

        int panelW = 520, panelH = linhas.length * 20 + 12;
        g2.setColor(new Color(0, 0, 0, 190));
        g2.fillRoundRect(8, 8, panelW, panelH, 8, 8);

        for (int i = 0; i < linhas.length; i++) {
            // Última linha (Decisão) em verde claro para destacar
            g2.setColor(i == linhas.length - 1
                ? new Color(100, 255, 140)
                : new Color(255, 220, 50));
            g2.drawString(linhas[i], 14, 26 + i * 20);
        }

        // Legenda das cores no canto inferior esquerdo
        g2.setFont(new Font("Consolas", Font.PLAIN, 11));
        String[] legenda = {
            "● Laranja = pixels no anel (F1/F2)",
            "● Verde/Vermelho = setores ativos/inativos (F2)",
            "● Ciano = borda interna + bbox (F3)",
            "● Ciano tracejado = raio interno (F3)"
        };
        Color[] coresLegenda = {
            new Color(255, 140, 0),
            new Color(0, 255, 120),
            new Color(0, 220, 255),
            new Color(0, 220, 255)
        };
        int ly = h - legenda.length * 18 - 10;
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(8, ly - 6, 320, legenda.length * 18 + 12, 6, 6);
        for (int i = 0; i < legenda.length; i++) {
            g2.setColor(coresLegenda[i]);
            g2.drawString(legenda[i], 14, ly + i * 18 + 12);
        }

        g2.dispose();
        return out;
    }

    // =========================================================
    //  EXERCÍCIO 5 — Maior e menor barra em gráfico de barras - OK
    // =========================================================

    public static String compararBarras(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // 1) Conta pixels da cor da barra em cada coluna
        int[] alturaColuna = new int[w];
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8)  & 0xFF;
                int b =  rgb        & 0xFF;
                if (r > 200 && g < 160 && b < 160 && r > g + 60) {
                    alturaColuna[x]++;
                }
            }
        }

        // 2) Agrupa colunas contíguas em barras distintas
        List<Integer> alturasBarras = new ArrayList<>();
        int tolerancia = Math.max(5, w / 100);
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
            + "\nBarras detectadas: " + alturasBarras.size()
            + "\nAlturas (px): " + alturasBarras;

            
    }
    public static BufferedImage desenharBarras(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();

        int[] alturaColuna = new int[w];
        // Também guarda a posição Y mais alta da barra em cada coluna
        int[] topoColuna = new int[w];
        Arrays.fill(topoColuna, h);

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8)  & 0xFF;
                int b =  rgb        & 0xFF;
                if (r > 200 && g < 160 && b < 160 && r > g + 60) {
                    alturaColuna[x]++;
                    if (y < topoColuna[x]) topoColuna[x] = y;
                }
            }
        }

        // Agrupa em barras (mesmo algoritmo do compararBarras)
        List<int[]> barras = new ArrayList<>(); // {xInicio, xFim, alturaMax, topoMin}
        int tolerancia = Math.max(5, w / 100);
        int x = 0;

        while (x < w) {
            if (alturaColuna[x] == 0) { x++; continue; }

            int maxAltura = 0, topoMin = h;
            int gapAtual = 0, inicio = x;

            while (x < w) {
                if (alturaColuna[x] == 0) {
                    gapAtual++;
                    if (gapAtual > tolerancia) break;
                } else {
                    gapAtual = 0;
                    if (alturaColuna[x] > maxAltura) maxAltura = alturaColuna[x];
                    if (topoColuna[x] < topoMin) topoMin = topoColuna[x];
                }
                x++;
            }

            if ((x - inicio) > w / 100)
                barras.add(new int[]{inicio, x - gapAtual - 1, maxAltura, topoMin});
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(img, 0, 0, null);

        if (barras.isEmpty()) { g2.dispose(); return out; }

        int maior = barras.stream().mapToInt(b -> b[2]).max().getAsInt();
        int menor = barras.stream().mapToInt(b -> b[2]).min().getAsInt();

        g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g2.setStroke(new BasicStroke(2.5f));

        for (int i = 0; i < barras.size(); i++) {
            int[] b = barras.get(i);
            int bx = b[0], bfim = b[1], balt = b[2], btopo = b[3];
            int bw2 = bfim - bx + 1;

            boolean ehMaior = balt == maior;
            boolean ehMenor = balt == menor;

            // Overlay semitransparente sobre a barra
            Color fill = ehMaior ? new Color(0, 200, 80, 60)
                    : ehMenor ? new Color(220, 60, 60, 60)
                    : new Color(255, 200, 0, 40);
            g2.setColor(fill);
            g2.fillRect(bx, btopo, bw2, balt);

            // Contorno
            Color borda = ehMaior ? new Color(0, 230, 80)
                        : ehMenor ? new Color(230, 60, 60)
                        : new Color(255, 200, 0);
            g2.setColor(borda);
            g2.drawRect(bx, btopo, bw2, balt);

            // Linha de topo
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(bx, btopo, bfim, btopo);
            g2.setStroke(new BasicStroke(2.5f));

            // Label acima da barra
            String tag = (ehMaior ? "▲ MAIOR" : ehMenor ? "▼ MENOR" : "#" + (i+1))
                    + "  " + balt + "px";
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(tag) + 8;
            int ly = btopo - fm.getHeight() - 4;
            if (ly < 0) ly = btopo + 4;

            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(bx, ly, tw, fm.getHeight() + 4, 4, 4);
            g2.setColor(borda);
            g2.drawString(tag, bx + 4, ly + fm.getHeight());

            // Seta de altura no lado direito da barra
            int ax = bfim + 6;
            if (ax + 50 < w) {
                g2.setColor(new Color(255, 255, 255, 180));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1, new float[]{4, 3}, 0));
                g2.drawLine(ax, btopo, ax, btopo + balt);
                g2.setStroke(new BasicStroke(2.5f));
            }
        }

        // Painel de resumo no canto
        g2.setFont(new Font("Consolas", Font.BOLD, 13));
        String[] linhas = {
            "Barras: " + barras.size(),
            "Maior: " + maior + " px",
            "Menor: " + menor + " px"
        };
        int pw = 180, ph = linhas.length * 20 + 12;
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(8, 8, pw, ph, 8, 8);
        g2.setColor(new Color(255, 220, 50));
        for (int i2 = 0; i2 < linhas.length; i2++)
            g2.drawString(linhas[i2], 14, 26 + i2 * 20);

        g2.dispose();
        return out;
    }

}