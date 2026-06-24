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
    //  EXERCÍCIO 1 — Leitura de horário em relógio analógico
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

        System.out.printf("DEBUG raio: raioPico=%d rMin=%.0f rMax=%.0f%n", raioPico, rMin, rMax);

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
        // Threshold subiu de 0.28 → 0.35 para capturar o octógono do PARE
        // que tinha score=0.322. Sentido Proibido tem score=0.237 (seguro).

        if (scoreCirculo < 0.35) {
            // Distingue pelo número de setores:
            // PARE (octógono): setores concentrados em poucos ângulos → ≤5
            // Sentido Proibido (círculo real): setores distribuídos → >5
            if (setoresPreenchidos <= 5) {
                return "PARE";
            } else {
                return "Sentido Proibido";
            }
        }

        // ── Decisão Velocidade Máxima ─────────────────────────────────
        // Com setoresPreenchidos=5 tanto para Vel.Máx quanto Proib.Estacionar,
        // o desempate é pelo score F1:
        //   Velocidade Máxima:    score=0,463 (círculo limpo, pouco conteúdo interno)
        //   Proibido Estacionar:  score=0,428 (letra E reduz score)
        // Threshold em 0.44 separa os dois com margem.

        if (setoresPreenchidos >= 6 || scoreCirculo > 0.44) {
            return "Velocidade Maxima";
        }

        // ── FEATURE 3: Sentido Proibido remanescente vs Proibido Estacionar

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
}