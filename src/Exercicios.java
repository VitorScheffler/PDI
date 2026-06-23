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

    // =========================================================
    //  EXERCÍCIO 1 — Leitura de horário em relógio analógico
    // =========================================================

    public static String lerRelogio(BufferedImage img) {

        // 1. Binarizar
        BufferedImage bin = Opcoes.threshold(img, 128);
        int w = bin.getWidth();
        int h = bin.getHeight();

        // Centro do relógio
        int cx = w / 2;
        int cy = h / 2;
        double raio = Math.min(w, h) / 2.0;

        // 2. Rotular componentes conectados
        int[][] rotulo = rotular(bin);
        int numRotulos = contarRotulos(rotulo);

        // 3. Para cada componente, calcular propriedades
        // Acumuladores para PCA e comprimento
        long[] somaX   = new long[numRotulos + 1];
        long[] somaY   = new long[numRotulos + 1];
        long[] somaXX  = new long[numRotulos + 1];
        long[] somaYY  = new long[numRotulos + 1];
        long[] somaXY  = new long[numRotulos + 1];
        int[]  tamanho = new int[numRotulos + 1];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = rotulo[y][x];
                if (r == 0) continue;
                somaX[r]  += x;
                somaY[r]  += y;
                somaXX[r] += (long) x * x;
                somaYY[r] += (long) y * y;
                somaXY[r] += (long) x * y;
                tamanho[r]++;
            }
        }

        // 4. Filtrar: só componentes que passam pelo centro
        //    e têm tamanho mínimo razoável
        int    melhorR1 = -1, melhorR2 = -1;
        double compR1   =  0, compR2   =  0;

        for (int r = 1; r <= numRotulos; r++) {
            int n = tamanho[r];

            // Ignora componentes muito pequenos ou muito grandes
            if (n < 30 || n > (int)(raio * raio * 0.5)) continue;

            double mX = (double) somaX[r] / n;
            double mY = (double) somaY[r] / n;

            // Distância do centróide ao centro do relógio
            double distCentro = Math.sqrt((mX - cx) * (mX - cx) + (mY - cy) * (mY - cy));

            // Ponteiros têm centróide próximo ao centro (não é número ou borda)
            if (distCentro > raio * 0.45) continue;

            // 5. PCA: calcular eixo principal do componente
            double cxx = (double) somaXX[r] / n - mX * mX;
            double cyy = (double) somaYY[r] / n - mY * mY;
            double cxy = (double) somaXY[r] / n - mX * mY;

            // Razão de aspecto via autovalores — ponteiro deve ser alongado
            double trace = cxx + cyy;
            double det   = cxx * cyy - cxy * cxy;
            double disc  = Math.sqrt(Math.max(0, trace * trace / 4 - det));
            double lambda1 = trace / 2 + disc; // maior autovalor
            double lambda2 = trace / 2 - disc; // menor autovalor

            // Proporção: ponteiro tem lambda1 >> lambda2
            if (lambda2 < 1 || lambda1 / lambda2 < 3.0) continue;

            // Ângulo do eixo principal (em graus, 0 = 12h, sentido horário)
            double angRad = Math.atan2(cxy, lambda1 - cyy);
            double angGraus = Math.toDegrees(angRad);

            // Normaliza para 0–360 a partir do 12 (eixo Y negativo)
            // atan2 retorna ângulo do vetor (cxy, lambda1-cyy)
            // precisamos o ângulo em relação ao topo
            double angFinal = (angGraus + 90 + 360) % 360;
            // Ponteiro pode apontar nos dois sentidos: pega o que faz mais sentido
            // (entre angFinal e angFinal+180)

            // Comprimento: distância máxima do centróide a um pixel do componente
            double compMax = 0;
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (rotulo[y][x] != r) continue;
                    double dx = x - mX;
                    double dy = y - mY;
                    double d  = Math.sqrt(dx * dx + dy * dy);
                    if (d > compMax) compMax = d;
                }
            }

            // Guarda os dois maiores (serão hora e minuto)
            if (compMax > compR1) {
                compR2 = compR1; melhorR2 = melhorR1;
                compR1 = compMax; melhorR1 = r;
            } else if (compMax > compR2) {
                compR2 = compMax; melhorR2 = r;
            }
        }

        if (melhorR1 == -1 || melhorR2 == -1)
            return "Não foi possível identificar os ponteiros.";

        // 6. Calcular ângulo de cada ponteiro usando PCA
        double angMinuto = calcularAngulo(bin, rotulo, melhorR1, cx, cy); // maior = minuto
        double angHora   = calcularAngulo(bin, rotulo, melhorR2, cx, cy); // menor = hora

        // 7. Converter ângulos em hora e minuto
        int hora = (int) Math.round(angHora / 30.0);
        if (hora == 0 || hora > 12) hora = hora == 0 ? 12 : hora % 12;

        int minIdx = (int) Math.round(angMinuto / 30.0) % 12;
        int minuto = minIdx * 5;

        return String.format("%02d:%02d", hora, minuto);
    }
    private static double calcularAngulo(
        BufferedImage bin, int[][] rotulo, int r, int cx, int cy) {

    int n = 0;
    long sx = 0, sy = 0, sxx = 0, syy = 0, sxy = 0;

    for (int y = 0; y < bin.getHeight(); y++) {
        for (int x = 0; x < bin.getWidth(); x++) {
            if (rotulo[y][x] != r) continue;
            sx  += x; sy  += y;
            sxx += (long) x * x;
            syy += (long) y * y;
            sxy += (long) x * y;
            n++;
        }
    }

    double mX = (double) sx / n;
    double mY = (double) sy / n;

    double cxx = (double) sxx / n - mX * mX;
    double cyy = (double) syy / n - mY * mY;
    double cxy = (double) sxy / n - mX * mY;

    // Ângulo do autovetor principal
    double angRad = 0.5 * Math.atan2(2 * cxy, cxx - cyy);
    double angGraus = Math.toDegrees(angRad);

    // O vetor pode apontar em qualquer dos dois sentidos (±180°)
    // Descobrimos qual ponta está mais longe do centro
    double dx1 = Math.cos(angRad);
    double dy1 = Math.sin(angRad);

    // Testa as duas pontas — pega a que aponta para longe do centro
    double dot1 = (mX - cx) * dx1 + (mY - cy) * dy1;

    double finalAng;
    if (dot1 > 0) {
        // vetor aponta na direção do centróide → ponta do ponteiro
        finalAng = Math.toDegrees(Math.atan2(dx1, -dy1));
    } else {
        finalAng = Math.toDegrees(Math.atan2(-dx1, dy1));
    }

    return (finalAng + 360) % 360;
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
    //  EXERCÍCIO 3 — Identificação de letras presentes na imagem
    // =========================================================

    public static String identificarLetras(BufferedImage img, Map<Character, BufferedImage> templates) {
        BufferedImage bin = Opcoes.threshold(img, 128);
        int w = bin.getWidth(), h = bin.getHeight();
        int[][] rotulo = rotular(bin);
        int n = contarRotulos(rotulo);

        if (n == 0) return "Nenhum caractere encontrado.";

        int[][] bbox = calcularBoundingBoxes(rotulo, n, w, h);

        Set<Character> encontradas = new LinkedHashSet<>();

        for (int r = 1; r <= n; r++) {
            int minX = bbox[r][0], minY = bbox[r][1];
            int maxX = bbox[r][2], maxY = bbox[r][3];
            int largura = maxX - minX + 1, altura = maxY - minY + 1;
            if (largura < 3 || altura < 3) continue; // ignora ruído muito pequeno

            // Recorta o componente, redimensiona para o tamanho do template
            // e compara com cada letra usando correlação por sobreposição
            BufferedImage recorte = recortarComponente(bin, rotulo, r, minX, minY, largura, altura);

            char melhor = '?';
            double melhorScore = -1;

            for (Map.Entry<Character, BufferedImage> e : templates.entrySet()) {
                BufferedImage modelo = e.getValue();
                BufferedImage recorteEsc = Opcoes.escala(recorte,
                        modelo.getWidth() / (double) recorte.getWidth(),
                        modelo.getHeight() / (double) recorte.getHeight());
                double score = correlacao(recorteEsc, modelo);
                if (score > melhorScore) {
                    melhorScore = score;
                    melhor = e.getKey();
                }
            }

            if (melhorScore > 0.6) encontradas.add(melhor); // limiar de aceitação
        }

        if (encontradas.isEmpty()) return "Nenhuma letra reconhecida.";

        StringBuilder sb = new StringBuilder();
        for (char c : encontradas) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(c);
        }
        return sb.toString();
    }
    private static BufferedImage recortarComponente(BufferedImage bin, int[][] rotulo, int r,
                                                       int minX, int minY, int largura, int altura) {
        BufferedImage out = new BufferedImage(largura, altura, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                boolean pertence = rotulo[minY + y][minX + x] == r;
                int v = pertence ? 0 : 255;
                out.setRGB(x, y, 0xFF000000 | (v << 16) | (v << 8) | v);
            }
        }
        return out;
    }
    private static double correlacao(BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        int iguais = 0, total = w * h;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int va = a.getRGB(x, y) & 0xFF;
                int vb = b.getRGB(x, y) & 0xFF;
                if (va == vb) iguais++;
            }
        return total == 0 ? 0 : (double) iguais / total;
    }

// =================================================================
    //  EXERCÍCIO 4 — Identificação de Placas de Trânsito
    //
    //  Lógica em cascata (3 features):
    //
    //  Feature 1 — razaoTopo: largura do vermelho no y+10% / y+30%
    //    PARE (octógono): lado reto no topo → não alarga rápido → > 1.50
    //    Círculos:        arco curvo        → alarga rapidamente → < 1.35
    //
    //  Feature 2 — pctDiag: % de vermelho na diagonal interna (↘ e ↗)
    //    Sentido Proibido: interior preenchido de vermelho → ~67%
    //    Demais:           só a borda vermelha             → ~7-11%
    //
    //  Feature 3 — razaoLH: largura / altura da bbox dos pixels pretos
    //    Proibido Estacionar: letra E = alta e estreita → < 0.40
    //    Velocidade Máxima:   números = mais largos     → ≥ 0.40
    // =================================================================

    public static String identificarPlacas(BufferedImage img) {
        int w  = img.getWidth();
        int h  = img.getHeight();
        int cx = w / 2;
        int cy = h / 2;

        // ── 1. Bounding box da região vermelha ────────────────────
        int xmin = w, xmax = 0, ymin = h, ymax = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8)  & 0xFF;
                int b =  rgb        & 0xFF;
                if (r > 180 && g < 80 && b < 80 && r > g + 100) {
                    if (x < xmin) xmin = x;
                    if (x > xmax) xmax = x;
                    if (y < ymin) ymin = y;
                    if (y > ymax) ymax = y;
                }
            }
        }
        if (xmax <= xmin) return "Nenhuma placa detectada.";

        int largBbox = xmax - xmin;
        int altBbox  = ymax - ymin;
        int raio     = Math.min(largBbox, altBbox) / 2;
        int passo    = altBbox / 10;

        // ── FEATURE 1: octógono (PARE) vs círculo ─────────────────
        // Mede largura do vermelho em y+10% e y+30% do topo da bbox.
        // Octógono tem lado reto → razão alta; círculo curva → razão baixa.
        int largY10 = larguraVmEmY(img, w, ymin + passo,     0, w);
        int largY30 = larguraVmEmY(img, w, ymin + passo * 3, 0, w);
        double razaoTopo = (largY30 > 0) ? (double) largY10 / largY30 : 0;

        if (razaoTopo > 1.50) {
            return "PARE";
        }

        // ── FEATURE 2: diagonal vermelha interna ──────────────────
        // Mede % de vermelho nas diagonais ↘ e ↗ dentro do raio.
        // Sentido Proibido: interior todo vermelho → ~67%.
        int raioD  = raio * 2 / 3;
        int diagVm = 0, antiVm = 0, diagTot = 0;
        for (int i = -raioD; i <= raioD; i += 2) {
            int px1 = cx + i, py1 = cy + i; // ↘
            int px2 = cx + i, py2 = cy - i; // ↗
            if (px1 >= 0 && px1 < w && py1 >= 0 && py1 < h) {
                int rgb = img.getRGB(px1, py1);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                if (r > 180 && g < 80 && b < 80 && r > g + 100) diagVm++;
                diagTot++;
            }
            if (px2 >= 0 && px2 < w && py2 >= 0 && py2 < h) {
                int rgb = img.getRGB(px2, py2);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                if (r > 180 && g < 80 && b < 80 && r > g + 100) antiVm++;
            }
        }
        double pctDiag = (diagTot > 0)
                ? (double) Math.max(diagVm, antiVm) / diagTot
                : 0;

        if (pctDiag > 0.30) {
            return "Sentido Proibido";
        }

        // ── FEATURE 3: largura/altura dos pixels pretos internos ──
        // Proibido Estacionar: letra E = alta e estreita → razaoLH < 0.40
        // Velocidade Máxima:   números = mais largos     → razaoLH ≥ 0.40
        int raioP      = raio * 55 / 100;
        int dxMinP = raioP, dxMaxP = -raioP;
        int dyMinP = raioP, dyMaxP = -raioP;
        boolean temPreto = false;

        for (int dy = -raioP; dy <= raioP; dy++) {
            for (int dx = -raioP; dx <= raioP; dx++) {
                if (dx * dx + dy * dy <= raioP * raioP) {
                    int px = cx + dx, py = cy + dy;
                    if (px >= 0 && px < w && py >= 0 && py < h) {
                        int rgb = img.getRGB(px, py);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8)  & 0xFF;
                        int b =  rgb        & 0xFF;
                        if (r < 50 && g < 50 && b < 50) {
                            temPreto = true;
                            if (dx < dxMinP) dxMinP = dx;
                            if (dx > dxMaxP) dxMaxP = dx;
                            if (dy < dyMinP) dyMinP = dy;
                            if (dy > dyMaxP) dyMaxP = dy;
                        }
                    }
                }
            }
        }

        if (temPreto) {
            int largPreto = dxMaxP - dxMinP;
            int altPreto  = dyMaxP - dyMinP;
            double razaoLH = (altPreto > 0) ? (double) largPreto / altPreto : 0;
            if (razaoLH < 0.40) {
                return "Proibido Estacionar";
            }
        }

        return "Velocidade Maxima";
    }

    // Conta colunas com pelo menos 1 pixel vermelho na linha y dado
    private static int larguraVmEmY(BufferedImage img, int w, int y, int xIni, int xFim) {
        if (y < 0 || y >= img.getHeight()) return 0;
        int count = 0;
        for (int x = xIni; x < xFim; x++) {
            int rgb = img.getRGB(x, y);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8)  & 0xFF;
            int b =  rgb        & 0xFF;
            if (r > 180 && g < 80 && b < 80 && r > g + 100) count++;
        }
        return count;
    }
    
    // =========================================================
    //  EXERCÍCIO 5 — Maior e menor barra em gráfico de barras - OK
    // =========================================================

    public static String compararBarras(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();

        // 1) Conta pixels da cor da barra em cada coluna
        //    Critério: R dominante (>200), G e B baixos (<160), R bem maior que G
        //    Robusto para JPEG (cobre variações de compressão)
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
        //    Tolerância de gap evita que bordas arredondadas quebrem uma barra em duas
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