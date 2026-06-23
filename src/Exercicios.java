import java.awt.image.BufferedImage;
import java.util.*;


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
    private static boolean[][] binarizar(BufferedImage img, int threshold) {
        int w = img.getWidth();
        int h = img.getHeight();
        boolean[][] bin = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >>  8) & 0xFF;
                int b =  rgb        & 0xFF;
                int lum = (r + g + b) / 3;
                bin[y][x] = (lum < threshold);
            }
        }
        return bin;
    }
    private static int[] encontrarPino(boolean[][] bin,
                                        int cx, int cy,
                                        int w, int h, int janela) {
        // Tenta janela inicial
        long somaX = 0, somaY = 0, count = 0;
        int y0 = Math.max(0, cy - janela);
        int y1 = Math.min(h, cy + janela);
        int x0 = Math.max(0, cx - janela);
        int x1 = Math.min(w, cx + janela);

        for (int y = y0; y < y1; y++) {
            for (int x = x0; x < x1; x++) {
                if (bin[y][x]) { somaX += x; somaY += y; count++; }
            }
        }

        if (count >= 10) {
            return new int[]{ (int)(somaX / count), (int)(somaY / count) };
        }

        // Fallback: raio crescente até 15% da menor dimensão
        int raioMax = Math.min(w, h) / 2;
        for (int r = janela; r <= raioMax * 15 / 100; r += 10) {
            somaX = 0; somaY = 0; count = 0;
            y0 = Math.max(0, cy - r); y1 = Math.min(h, cy + r);
            x0 = Math.max(0, cx - r); x1 = Math.min(w, cx + r);
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    if (bin[y][x]) { somaX += x; somaY += y; count++; }
                }
            }
            if (count >= 10) {
                return new int[]{ (int)(somaX / count), (int)(somaY / count) };
            }
        }

        // Último recurso: centro geométrico
        return new int[]{ cx, cy };
    }
    private static boolean[][] erodirCruz(boolean[][] src, int w, int h) {
        boolean[][] dst = new boolean[h][w];
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                dst[y][x] = src[y][x]
                         && src[y-1][x]
                         && src[y+1][x]
                         && src[y][x-1]
                         && src[y][x+1];
            }
        }
        return dst;
    }
    private static int indiceMaior(double[] arr) {
        int idx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[idx]) idx = i;
        }
        return idx;
    }
    private static String angToClock(int ang) {
        // converte 0° = 12h, sentido horário
        int h = (int) Math.round((ang % 360) / 30.0);
        if (h == 0) h = 12;
        return h + "h";
    }

    // =========================================================
    //  EXERCÍCIO 1 — Leitura de horário em relógio analógico
    // =========================================================

    public static String lerRelogio(BufferedImage img) {

        int w = img.getWidth();
        int h = img.getHeight();
        int cx = w / 2;
        int cy = h / 2;

        // Raio estimado (imagens assumidas com relógio bem centralizado)
        int raio = Math.min(w, h) / 2;

        // ── 1. Binarizar ──────────────────────────────────────────
        boolean[][] bin = binarizar(img, 128);

        // ── 2. Encontrar o pino central ───────────────────────────
        // Centróide dos pixels pretos numa janela de 80x80 ao redor
        // do centro geométrico da imagem.
        int[] pino = encontrarPino(bin, cx, cy, w, h, 80);
        int pcx = pino[0];
        int pcy = pino[1];

        // ── 3. Erosão morfológica 3x ──────────────────────────────
        // Remove elementos finos (borda, tracinhos, números) e
        // mantém os ponteiros, que são mais espessos.
        boolean[][] erodido = bin;
        for (int i = 0; i < 3; i++) {
            erodido = erodirCruz(erodido, w, h);
        }

        // ── 4. Scan radial com peso por distância ─────────────────
        // Para cada ângulo θ (0–359°), percorre raios do pino para
        // fora e soma as distâncias dos pixels pretos erodidos.
        // Isso favorece ponteiros longos sobre marcações curtas.
        double[] hist = new double[360];
        double rMin = raio * 0.07;
        double rMax = raio * 0.72;

        for (int angDeg = 0; angDeg < 360; angDeg++) {
            double ang = Math.toRadians(angDeg);
            double sinA = Math.sin(ang);
            double cosA = Math.cos(ang);
            double score = 0;

            for (double r = rMin; r <= rMax; r += 2.0) {
                int x = (int) Math.round(pcx + r * sinA);
                int y = (int) Math.round(pcy - r * cosA);
                if (x < 0 || x >= w || y < 0 || y >= h) break;
                if (erodido[y][x]) {
                    score += r;   // peso = distância ao pino
                }
            }
            hist[angDeg] = score;
        }

        // ── 5. Suavizar histograma (janela ±4°) ───────────────────
        double[] histS = new double[360];
        for (int i = 0; i < 360; i++) {
            double soma = 0;
            for (int j = -4; j <= 4; j++) {
                soma += hist[(i + j + 360) % 360];
            }
            histS[i] = soma / 9.0;
        }

        // ── 6. Encontrar 2 picos principais ───────────────────────
        // Pico 1: ângulo de maior score.
        int p1 = indiceMaior(histS);

        // Pico 2: maior score fora de ±30° do pico 1.
        int p2 = -1;
        double melhor = -1;
        for (int i = 0; i < 360; i++) {
            int diff = Math.abs(i - p1);
            diff = Math.min(diff, 360 - diff);
            if (diff < 30) continue;
            if (histS[i] > melhor) {
                melhor = histS[i];
                p2 = i;
            }
        }

        if (p2 == -1) {
            return "Não foi possível identificar os ponteiros.";
        }

        // ── DEBUG DOS PONTEIROS ───────────────────────────────────────
        System.out.println("\n========== DEBUG RELÓGIO ==========");

        System.out.println("Pico 1: " + p1 + "° | score=" + histS[p1]);
        System.out.println("Pico 2: " + p2 + "° | score=" + histS[p2]);

        // separação angular
        int diff = Math.abs(p1 - p2);
        diff = Math.min(diff, 360 - diff);
        System.out.println("Diferença angular entre picos: " + diff + "°");

        // decisão de minuto/hora (como está hoje no seu código)
        int angMinuto, angHora;

        double len1 = calcularComprimento(erodido, p1, pcx, pcy, w, h);
        double len2 = calcularComprimento(erodido, p2, pcx, pcy, w, h);

        // bônus: alinhamento com múltiplos de 30° (hora “gosta” disso)
        double align1 = 1.0 - (Math.min(p1 % 30, 30 - (p1 % 30)) / 15.0);
        double align2 = 1.0 - (Math.min(p2 % 30, 30 - (p2 % 30)) / 15.0);

        // ponteiro de hora tende a alinhar melhor com marcações
        double score1 = len1 * 1.0 + align1 * 20;
        double score2 = len2 * 1.0 + align2 * 20;

        // regra híbrida leve (não dominante)
        if (score1 >= score2) {
            angMinuto = p1;
            angHora = p2;
        } else {
            angMinuto = p2;
            angHora = p1;
        }

        // normaliza função angular
        String dirMin = angToClock(angMinuto);
        String dirHora = angToClock(angHora);

        // “tamanho” (proxy de força do ponteiro)
        double tamMin = histS[angMinuto];
        double tamHora = histS[angHora];

        System.out.println("\n-- Ponteiro MINUTOS --");
        System.out.println("Ângulo: " + angMinuto + "° (" + dirMin + ")");
        System.out.println("Força/Comprimento: " + tamMin);

        System.out.println("\n-- Ponteiro HORAS --");
        System.out.println("Ângulo: " + angHora + "° (" + dirHora + ")");
        System.out.println("Força/Comprimento: " + tamHora);

        System.out.println("===================================\n");

        // ── 8. Converter ângulos → hora e minuto ──────────────────
        // Cada posição de hora ocupa 30° (360° / 12).
        // Arredondamos para o número mais próximo do mostrador.
        int hora = (int) Math.floor(angHora / 30.0);
        hora = (hora == 0) ? 12 : hora;

        int minuto = (int) Math.round(angMinuto / 6.0) % 60;

        return String.format("%02d:%02d", hora, minuto);
    }
    
    private static double calcularComprimento(boolean[][] img, int ang, int cx, int cy, int w, int h) {
        double rad = Math.toRadians(ang);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        for (int r = 0; r < Math.min(w, h) / 2; r++) {
            int x = (int) Math.round(cx + r * sin);
            int y = (int) Math.round(cy - r * cos);

            if (x < 0 || x >= w || y < 0 || y >= h) break;

            if (!img[y][x]) {
                return r; // fim do ponteiro
            }
        }
        return Math.min(w, h) / 2;
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

        System.out.printf(
    "DEBUG r=%d bbox=%dx%d aspecto=%.3f buracos=%d razaoEsq=%.2f%n",
        r,
        largura,
        altura,
        aspecto,
        buracos,
        razaoEsq
    );

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
    //  EXERCÍCIO 4 — Identificação de Placas de Trânsito
    // =================================================================

    public static String identificarPlacas(BufferedImage img) {

        int w = img.getWidth();
        int h = img.getHeight();
        int cx = w / 2;
        int cy = h / 2;

        BufferedImage bordas = Opcoes.sobel(img, 10);

        int maxRaioHist = (int)(Math.min(w, h) * 0.75);
        int[] histRaio = new int[maxRaioHist + 1];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {
                    int dist = (int) Math.round(
                        Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy))
                    );

                    if (dist <= maxRaioHist) {
                        histRaio[dist]++;
                    }
                }
            }
        }

        int[] histSmooth = new int[maxRaioHist + 1];

        for (int i = 2; i < maxRaioHist - 2; i++) {
            histSmooth[i] = (
                histRaio[i - 2] +
                histRaio[i - 1] +
                histRaio[i] +
                histRaio[i + 1] +
                histRaio[i + 2]
            ) / 5;
        }

        int raioPico = 0, picVal = 0;

        for (int i = 0; i <= maxRaioHist; i++) {
            if (histSmooth[i] > picVal) {
                picVal = histSmooth[i];
                raioPico = i;
            }
        }

        double rMin = raioPico * 0.88;
        double rMax = raioPico * 1.12;

        System.out.printf(
            "DEBUG raio: raioPico=%d rMin=%.0f rMax=%.0f%n",
            raioPico, rMin, rMax
        );

        // ── FEATURE 1: circular ou octógono? ─────────────────────

        int totalBorda = 0, naCircunferencia = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if ((bordas.getRGB(x, y) & 0xFF) > 128) {

                    double dist = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));

                    if (dist > rMin * 0.5) {
                        totalBorda++;

                        if (dist >= rMin && dist <= rMax) {
                            naCircunferencia++;
                        }
                    }
                }
            }
        }

        double scoreCirculo = (totalBorda > 0)
            ? (double) naCircunferencia / totalBorda
            : 0;

        System.out.printf(
            "DEBUG F1: totalBorda=%d naCirc=%d score=%.3f%n",
            totalBorda, naCircunferencia, scoreCirculo
        );

        // Círculo: borda concentrada na circunferência → score alto
        // Octógono: borda em 8 segmentos retos espalhados → score baixo

        if (scoreCirculo < 0.40) {
            return "PARE";
        }

        // ── FEATURE 2: círculo completo ou dois semicírculos? ────

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

        for (int c : setores) {
            maxSetor = Math.max(maxSetor, c);
        }

        int limiar = maxSetor / 4;
        int setoresPreenchidos = 0;

        for (int c : setores) {
            if (c > limiar) {
                setoresPreenchidos++;
            }
        }

        System.out.printf(
            "DEBUG F2: setores=%d|%d|%d|%d|%d|%d|%d|%d preenchidos=%d%n",
            setores[0], setores[1], setores[2], setores[3],
            setores[4], setores[5], setores[6], setores[7],
            setoresPreenchidos
        );

        if (setoresPreenchidos >= 7) {
            return "Velocidade Maxima";
        }

        // ── FEATURE 3: seta vs letra E ───────────────────────────

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
                ? (double)(bxMax - bxMin) / (byMax - byMin)
                : 0;

            System.out.printf(
                "DEBUG F3: larg=%d alt=%d razaoLH=%.3f%n",
                bxMax - bxMin, byMax - byMin, razaoLH
            );

            if (razaoLH < 0.55) {
                return "Sentido Proibido";
            }
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