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

// Calcula o ângulo do ponteiro (0=12h, sentido horário) via PCA
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
    //  EXERCÍCIO 2 — Contagem de objetos coloridos por cor
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

    // =========================================================
    //  EXERCÍCIO 4 — Identificação de tipos de placas de trânsito
    // =========================================================

    public static String identificarPlacas(BufferedImage img) {
        List<int[]> regioes = segmentarRegioes(img);
        if (regioes.isEmpty()) return "Nenhuma placa encontrada.";
        List<String> resultados = new ArrayList<>();
        for (int[] reg : regioes)
            resultados.add(classificarPlaca(
                img.getSubimage(reg[0], reg[1], reg[2], reg[3])));
        return String.join(", ", resultados);
    }
    private static List<int[]> segmentarRegioes(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] marcado  = new boolean[h][w];
        boolean[][] visitado = new boolean[h][w];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (!ehFundoBranco(img.getRGB(x, y))) marcado[y][x] = true;

        List<int[]> regioes = new ArrayList<>();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (marcado[y][x] && !visitado[y][x]) {
                    int[] bb = bfs(marcado, visitado, x, y, w, h);
                    if ((bb[2]-bb[0]+1)>30 && (bb[3]-bb[1]+1)>30)
                        regioes.add(new int[]{bb[0],bb[1],bb[2]-bb[0]+1,bb[3]-bb[1]+1});
                }
        return mesclarRegioes(regioes, 20);
    }
    private static int[] bfs(boolean[][] marcado, boolean[][] visitado,
                              int sx, int sy, int w, int h) {
        Queue<int[]> q = new LinkedList<>();
        q.add(new int[]{sx,sy}); visitado[sy][sx]=true;
        int x0=sx,y0=sy,x1=sx,y1=sy;
        int[] dx={-1,1,0,0}, dy={0,0,-1,1};
        while(!q.isEmpty()){
            int[] p=q.poll(); int cx=p[0],cy=p[1];
            if(cx<x0)x0=cx; if(cx>x1)x1=cx;
            if(cy<y0)y0=cy; if(cy>y1)y1=cy;
            for(int d=0;d<4;d++){
                int nx=cx+dx[d],ny=cy+dy[d];
                if(nx>=0&&nx<w&&ny>=0&&ny<h&&marcado[ny][nx]&&!visitado[ny][nx]){
                    visitado[ny][nx]=true; q.add(new int[]{nx,ny});
                }
            }
        }
        return new int[]{x0,y0,x1,y1};
    }
    private static List<int[]> mesclarRegioes(List<int[]> regioes, int gap) {
        boolean mesclou=true;
        while(mesclou){
            mesclou=false;
            List<int[]> nova=new ArrayList<>();
            boolean[] usado=new boolean[regioes.size()];
            for(int i=0;i<regioes.size();i++){
                if(usado[i]) continue;
                int[] a=regioes.get(i).clone();
                for(int j=i+1;j<regioes.size();j++){
                    if(usado[j]) continue;
                    int[] b=regioes.get(j);
                    if((a[0]-gap)<(b[0]+b[2])&&(a[0]+a[2]+gap)>b[0]
                     &&(a[1]-gap)<(b[1]+b[3])&&(a[1]+a[3]+gap)>b[1]){
                        int x2=Math.max(a[0]+a[2],b[0]+b[2]);
                        int y2=Math.max(a[1]+a[3],b[1]+b[3]);
                        a[0]=Math.min(a[0],b[0]); a[1]=Math.min(a[1],b[1]);
                        a[2]=x2-a[0]; a[3]=y2-a[1];
                        usado[j]=true; mesclou=true;
                    }
                }
                nova.add(a);
            }
            regioes=nova;
        }
        return regioes;
    }
    private static String classificarPlaca(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();

        // Bounding box do vermelho (contorno da placa)
        int minX=w,minY=h,maxX=-1,maxY=-1;
        for(int y=0;y<h;y++) for(int x=0;x<w;x++){
            int rgb=img.getRGB(x,y);
            if(ehVermelho(rgb)){
                if(x<minX)minX=x; if(x>maxX)maxX=x;
                if(y<minY)minY=y; if(y>maxY)maxY=y;
            }
        }
        if(maxX<0) return "Placa não identificada";

        int bw=maxX-minX+1, bh=maxY-minY+1;
        int cx=(minX+maxX)/2, cy=(minY+maxY)/2;
        int raio=Math.min(bw,bh)/2;
        int rInt=(int)(raio*0.65); // círculo interno — exclui o anel

        // ── PASSO 1: Sentido proibido — interior cheio de vermelho ──────────
        // É a única placa onde o centro do círculo É vermelho (a seta some no JPEG)
        long vermInt=0;
        double areaInt = Math.PI*rInt*rInt;
        for(int y=cy-rInt;y<=cy+rInt;y++) for(int x=cx-rInt;x<=cx+rInt;x++){
            if(x<0||x>=w||y<0||y>=h) continue;
            if(dist(x,y,cx,cy)>rInt) continue;
            if(ehVermelho(img.getRGB(x,y))) vermInt++;
        }
        double ratioVermInt = vermInt/areaInt;
        if(ratioVermInt > 0.55) return "Sentido proibido";

        // ── PASSO 2: Monta projeção horizontal do escuro interno ─────────────
        // Conta pixels escuros por coluna (dentro do círculo interno)
        int[] projH = new int[bw];
        for(int y=minY;y<=maxY;y++) for(int x=minX;x<=maxX;x++){
            if(x<0||x>=w||y<0||y>=h) continue;
            if(dist(x,y,cx,cy)>rInt) continue;
            if(ehEscuro(img.getRGB(x,y))) projH[x-minX]++;
        }

        // Suaviza com média móvel (janela 15)
        double[] projS = suavizar(projH, 15);
        double picoMax = max(projS);
        if(picoMax==0) return "Placa não identificada";

        // Conta picos e colunas com escuro significativo
        int nPicos = contarPicos(projS, picoMax*0.30, 20);
        int colsComEsc = 0;
        for(double v : projS) if(v > picoMax*0.10) colsComEsc++;
        double ratioCols = (double)colsComEsc/bw;

        // ── PASSO 3: PARE — letras P,A,R,E geram muitos picos horizontais ───
        // 4+ picos = 4 letras separadas; colunas moderadas (não ocupa tudo)
        if(nPicos >= 4 && ratioCols < 0.35) return "Pare";

        // ── PASSO 4: Proibido estacionar — letra E estreita ──────────────────
        // E é estreito horizontalmente: 1 pico e poucas colunas
        if(nPicos <= 1 && ratioCols < 0.28) return "Proibido estacionar";

        // ── PASSO 5: Velocidade máxima — número ocupa mais colunas ──────────
        return "Velocidade máxima";
    }
    private static double dist(int x,int y,int cx,int cy){
        double dx=x-cx, dy=y-cy;
        return Math.sqrt(dx*dx+dy*dy);
    }
    private static double[] suavizar(int[] arr, int janela) {
        double[] s = new double[arr.length];
        for(int i=0;i<arr.length;i++){
            int cnt=0; double soma=0;
            for(int j=i-janela/2;j<=i+janela/2;j++)
                if(j>=0&&j<arr.length){ soma+=arr[j]; cnt++; }
            s[i]=cnt>0?soma/cnt:0;
        }
        return s;
    }
    private static double max(double[] arr){
        double m=0; for(double v:arr) if(v>m)m=v; return m;
    }
    private static int contarPicos(double[] arr, double altMin, int distMin){
        int picos=0; int ultimoPico=-distMin;
        for(int i=1;i<arr.length-1;i++){
            if(arr[i]>arr[i-1]&&arr[i]>arr[i+1]
               &&arr[i]>=altMin&&(i-ultimoPico)>=distMin){
                picos++; ultimoPico=i;
            }
        }
        return picos;
    }
    private static boolean ehFundoBranco(int rgb){
        int r=(rgb>>16)&0xFF,g=(rgb>>8)&0xFF,b=rgb&0xFF;
        return r>220&&g>220&&b>220;
    }
    private static boolean ehVermelho(int rgb){
        int r=(rgb>>16)&0xFF,g=(rgb>>8)&0xFF,b=rgb&0xFF;
        // HSV-like: matiz vermelha (r alto, g e b baixos)
        return r>150&&g<110&&b<110&&(r-g)>70;
    }
    private static boolean ehEscuro(int rgb){
        int r=(rgb>>16)&0xFF,g=(rgb>>8)&0xFF,b=rgb&0xFF;
        return r<100&&g<100&&b<100;
    }

    // =========================================================
    //  EXERCÍCIO 5 — Maior e menor barra em gráfico de barras
    // =========================================================
    public static String compararBarras(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();

        // 1) Encontra a linha de base do gráfico: a linha y mais baixa que
        // contém algum pixel "não-fundo" em qualquer coluna
        int linhaBase = -1;
        for (int y = h - 1; y >= 0; y--) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                boolean fundo = (r > 230 && g > 230 && b > 230);
                if (!fundo) { linhaBase = y; break; }
            }
            if (linhaBase != -1) break;
        }

        if (linhaBase == -1) return "Nenhuma barra encontrada.";

        // 2) Para cada coluna, conta a altura da barra a partir da linha de
        // base, subindo até encontrar fundo
        int[] alturaColuna = new int[w];
        for (int x = 0; x < w; x++) {
            int altura = 0;
            for (int y = linhaBase; y >= 0; y--) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                boolean fundo = (r > 230 && g > 230 && b > 230);
                if (!fundo) altura++;
                else break;
            }
            alturaColuna[x] = altura;
        }

        // 3) Agrupa colunas contíguas com altura > 0 em barras distintas,
        // tolerando pequenas falhas (gaps de poucas colunas)
        List<Integer> alturasBarras = new ArrayList<>();
        int x = 0;
        int tolerancia = Math.max(1, w / 200);

        while (x < w) {
            if (alturaColuna[x] == 0) { x++; continue; }

            int maxAltura = 0;
            int gapAtual = 0;
            int inicio = x;

            while (x < w) {
                if (alturaColuna[x] == 0) {
                    gapAtual++;
                    if (gapAtual > tolerancia) break;
                } else {
                    gapAtual = 0;
                    if (alturaColuna[x] > maxAltura) maxAltura = alturaColuna[x];
                }
                x++;
            }

            if (x - inicio > w / 100) {
                alturasBarras.add(maxAltura);
            }
        }

        if (alturasBarras.isEmpty()) return "Nenhuma barra encontrada.";

        int maior = Collections.max(alturasBarras);
        int menor = Collections.min(alturasBarras);

        return "Maior = " + maior + " | Menor = " + menor +
               "  (barras detectadas: " + alturasBarras.size() + ")";
    }
}