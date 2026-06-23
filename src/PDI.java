import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PDI extends JFrame {

    // ── Paleta de cores da interface ──────────────────────────────
    private static final Color FUNDO  = new Color(248, 248, 250);
    private static final Color PAINEL = Color.WHITE;
    private static final Color INNER  = new Color(245, 245, 248);
    private static final Color BORDA  = new Color(220, 220, 228);
    private static final Color TEXTO  = new Color(30,  30,  40);
    private static final Color SUB    = new Color(130, 130, 150);
    private static final Color AZUL   = new Color(59,  130, 246);

    // ── Fontes ────────────────────────────────────────────────────
    private static final Font F_TITULO  = new Font("Segoe UI", Font.BOLD,  14);
    private static final Font F_LABEL   = new Font("Segoe UI", Font.BOLD,  12);
    private static final Font F_NORMAL  = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_PEQUENA = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_MONO    = new Font("Consolas",  Font.PLAIN, 11);

    // ── Componentes de exibição ───────────────────────────────────
    private final JLabel lblOriginal  = new JLabel();
    private final JLabel lblTransf    = new JLabel();
    private final JLabel infoOriginal = new JLabel("Nenhuma imagem carregada");
    private final JLabel infoTransf   = new JLabel("—");

    // ── Imagens em memória ────────────────────────────────────────
    private BufferedImage imgOriginal;     // imagem aberta pelo usuário (nunca alterada)
    private BufferedImage imgTransformada; // resultado do filtro/transformação aplicado

    static {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
    }

    public PDI() {
        configurarUI();
        setTitle("PDI — Processamento Digital de Imagens");
        setSize(1300, 820);
        setMinimumSize(new Dimension(900, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(FUNDO);
        criarMenu();
        add(criarTopo(),    BorderLayout.NORTH);
        add(criarCentral(), BorderLayout.CENTER);
    }

    // ── Estilo da interface ───────────────────────────────────────
    private void configurarUI() {
        UIManager.put("MenuBar.background",           PAINEL);
        UIManager.put("MenuBar.border",               new MatteBorder(0, 0, 1, 0, BORDA));
        UIManager.put("Menu.background",              PAINEL);
        UIManager.put("Menu.foreground",              TEXTO);
        UIManager.put("Menu.selectionBackground",     AZUL);
        UIManager.put("Menu.selectionForeground",     Color.WHITE);
        UIManager.put("Menu.border",                  BorderFactory.createEmptyBorder(4, 6, 4, 6));
        UIManager.put("MenuItem.background",          PAINEL);
        UIManager.put("MenuItem.foreground",          TEXTO);
        UIManager.put("MenuItem.selectionBackground", AZUL);
        UIManager.put("MenuItem.selectionForeground", Color.WHITE);
        UIManager.put("MenuItem.border",              BorderFactory.createEmptyBorder(5, 14, 5, 14));
        UIManager.put("PopupMenu.background",         PAINEL);
        UIManager.put("PopupMenu.border",             BorderFactory.createCompoundBorder(
                new LineBorder(BORDA, 1), BorderFactory.createEmptyBorder(4, 0, 4, 0)));
        UIManager.put("ScrollBar.width", 8);
    }

    // ── Barra superior com título e botões rápidos ────────────────
    private JPanel criarTopo() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(PAINEL);
        p.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDA),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)));

        JPanel esq = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        esq.setOpaque(false);

        JLabel titulo = new JLabel("PDI");
        titulo.setFont(F_TITULO);
        titulo.setForeground(TEXTO);

        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 20));
        sep.setForeground(BORDA);

        esq.add(titulo);
        esq.add(sep);
        esq.add(botaoRapido("Abrir",  e -> abrirImagem()));
        esq.add(botaoRapido("Salvar", e -> salvarImagem()));

        // Nome do autor exibido no canto direito
        JLabel autor = new JLabel("Vitor Matheus Scheffler");
        autor.setFont(F_PEQUENA);
        autor.setForeground(SUB);

        p.add(esq,   BorderLayout.WEST);
        p.add(autor, BorderLayout.EAST);
        return p;
    }

    // Cria um botão simples para a barra de atalhos
    private JButton botaoRapido(String texto, java.awt.event.ActionListener acao) {
        JButton btn = new JButton(texto);
        btn.setFont(F_PEQUENA);
        btn.setForeground(TEXTO);
        btn.setBackground(new Color(243, 244, 246));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDA, 1), BorderFactory.createEmptyBorder(4, 10, 4, 10)));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(acao);
        return btn;
    }

    // ── Painel central: original | transformada ───────────────────
    private JPanel criarCentral() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setBackground(FUNDO);
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        p.add(cartao("Imagem Original",     lblOriginal, infoOriginal));
        p.add(cartao("Imagem Transformada", lblTransf,   infoTransf));
        return p;
    }

    // Cria um cartão com título, área de imagem e rodapé de info
    private JPanel cartao(String titulo, JLabel imgLabel, JLabel statusLabel) {
        JPanel c = new JPanel(new BorderLayout(0, 8));
        c.setBackground(PAINEL);
        c.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDA, 1), BorderFactory.createEmptyBorder(10, 10, 8, 10)));

        JLabel lbl = new JLabel(titulo);
        lbl.setFont(F_LABEL);
        lbl.setForeground(TEXTO);

        imgLabel.setHorizontalAlignment(JLabel.CENTER);
        imgLabel.setVerticalAlignment(JLabel.CENTER);
        imgLabel.setText("<html><center><span style='color:#d1d5db;font-size:11px;" +
                "font-family:Segoe UI'>Nenhuma imagem</span></center></html>");

        JScrollPane scroll = new JScrollPane(imgLabel);
        scroll.setBackground(INNER);
        scroll.getViewport().setBackground(INNER);
        scroll.setBorder(new LineBorder(BORDA, 1));

        statusLabel.setFont(F_MONO);
        statusLabel.setForeground(SUB);

        c.add(lbl,         BorderLayout.NORTH);
        c.add(scroll,      BorderLayout.CENTER);
        c.add(statusLabel, BorderLayout.SOUTH);
        return c;
    }

    // ── Menus ─────────────────────────────────────────────────────
    private void criarMenu() {
        JMenuBar bar = new JMenuBar();

        // Menu Arquivo
        JMenu arquivo = menu("Arquivo");
        arquivo.add(item("Abrir Imagem",  e -> abrirImagem()));
        arquivo.add(item("Salvar Imagem", e -> salvarImagem()));
        arquivo.addSeparator();
        arquivo.add(item("Sobre", e -> JOptionPane.showMessageDialog(this,
                "<html><b>PDI — Processamento Digital de Imagens</b><br><br>" +
                "Universidade Feevale<br><br>" +
                "<span style='color:#888'>Autor: Vitor Matheus Scheffler</span></html>",
                "Sobre", JOptionPane.INFORMATION_MESSAGE)));
        arquivo.addSeparator();
        arquivo.add(item("Sair", e -> System.exit(0)));

        // Menu Transformações Geométricas
        JMenu transf = menu("Transformações Geométricas");
        transf.add(item("Transladar", e -> transladar()));
        transf.add(item("Rotacionar", e -> rotacionar()));
        transf.add(item("Aumentar",   e -> aumentar()));
        transf.add(item("Diminuir",   e -> diminuir()));
        JMenu espelhar = menu("Espelhar");
        espelhar.add(item("Horizontal", e -> espelharHorizontal()));
        espelhar.add(item("Vertical",   e -> espelharVertical()));
        transf.add(espelhar);

        // Menu Filtros
        JMenu filtros = menu("Filtros");
        filtros.add(item("Grayscale",      e -> grayscale()));
        filtros.add(item("Ajustar Brilho", e -> ajustarBrilho()));
        filtros.add(item("Contraste",      e -> ajustarContraste()));
        filtros.add(item("Threshold",      e -> threshold()));
        filtros.add(item("Passa Baixa",    e -> passaBaixa()));
        JMenu passaAlta = menu("Passa Alta");
        passaAlta.add(item("Sobel", e -> sobel()));
        passaAlta.add(item("Canny", e -> canny()));
        filtros.add(passaAlta);

        // Menu Morfologia Matemática
        JMenu morfologia = menu("Morfologia Matemática");
        morfologia.add(item("Dilatação", e -> dilatacao()));
        morfologia.add(item("Erosão",    e -> erosao()));
        morfologia.add(item("Abertura",  e -> abertura()));
        morfologia.add(item("Fechamento",e -> fechamento()));
        morfologia.add(item("Afinamento",e -> afinamento()));

        // Menu Exercícios
        JMenu exercicios = menu("Exercícios");
        exercicios.add(item("1 - Relógio Analógico",        e -> exercicio1()));
        exercicios.add(item("2 - Contagem de Cores (OK)",        e -> exercicio2()));
        exercicios.add(item("3 - Reconhecimento de Letras (OK)", e -> exercicio3()));
        exercicios.add(item("4 - Placas de Trânsito",       e -> exercicio4()));
        exercicios.add(item("5 - Comparação de Barras (OK)",     e -> exercicio5()));

        bar.add(arquivo);
        bar.add(transf);
        bar.add(filtros);
        bar.add(morfologia);
        bar.add(exercicios);
        setJMenuBar(bar);
    }

    private JMenu menu(String t) {
        JMenu m = new JMenu(t);
        m.setFont(F_NORMAL);
        return m;
    }

    private JMenuItem item(String t, java.awt.event.ActionListener a) {
        JMenuItem mi = new JMenuItem(t);
        mi.setFont(F_NORMAL);
        mi.addActionListener(a);
        return mi;
    }

    // ── Utilitários ───────────────────────────────────────────────

    // Exibe aviso se nenhuma imagem foi carregada
    private boolean semImagem() {
        if (imgOriginal == null) {
            JOptionPane.showMessageDialog(this, "Carregue uma imagem primeiro!",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    // Atualiza o painel de imagem transformada com o resultado e a descrição
    private void mostrar(String descricao) {
        lblOriginal.setText(null);
        lblTransf.setText(null);

        lblOriginal.setIcon(new ImageIcon(escalarParaCaber(imgOriginal, lblOriginal)));
        lblTransf.setIcon(new ImageIcon(escalarParaCaber(imgTransformada, lblTransf)));

        infoTransf.setText(descricao + "  ·  " +
                imgTransformada.getWidth() + " × " + imgTransformada.getHeight() + " px");
    }

    // Escala a imagem para caber dentro da área visível do label, mantendo proporção
    private BufferedImage escalarParaCaber(BufferedImage img, JLabel label) {
        Container viewport = label.getParent(); // o viewport do JScrollPane
        int larguraDisp = viewport != null ? viewport.getWidth()  : 500;
        int alturaDisp  = viewport != null ? viewport.getHeight() : 500;

        if (larguraDisp <= 0) larguraDisp = 500;
        if (alturaDisp  <= 0) alturaDisp  = 500;

        double escala = Math.min(
                (double) larguraDisp / img.getWidth(),
                (double) alturaDisp  / img.getHeight());

        // Não amplia imagens pequenas, só reduz as grandes
        if (escala >= 1.0) return img;

        int novaLargura = Math.max(1, (int) (img.getWidth()  * escala));
        int novaAltura  = Math.max(1, (int) (img.getHeight() * escala));

        BufferedImage redimensionada = new BufferedImage(novaLargura, novaAltura, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = redimensionada.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(img, 0, 0, novaLargura, novaAltura, null);
        g2.dispose();

        return redimensionada;
    }

    // Pede um número decimal ao usuário via caixa de diálogo
    private double pedirDouble(String msg, String titulo) {
        String s = JOptionPane.showInputDialog(this, msg, titulo, JOptionPane.PLAIN_MESSAGE);
        if (s == null) return Double.NaN; // usuário cancelou
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido. Digite um número.");
            return Double.NaN;
        }
    }

    // ── Arquivo ───────────────────────────────────────────────────

    // Abre uma imagem do disco; começa direto na pasta 'img' do projeto
    private void abrirImagem() {
        // Tenta abrir na pasta img/ do projeto; se não existir, abre no diretório atual
        File pastaImg = new File(System.getProperty("user.dir") + File.separator + "img");
        JFileChooser c = pastaImg.exists() ? new JFileChooser(pastaImg) : new JFileChooser();
        c.setFileFilter(new FileNameExtensionFilter("Imagens (jpg, png, bmp)", "jpg", "jpeg", "png", "bmp"));

        if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            imgOriginal = ImageIO.read(c.getSelectedFile());
            lblOriginal.setText(null);
            lblOriginal.setIcon(new ImageIcon(escalarParaCaber(imgOriginal, lblOriginal)));
            infoOriginal.setText(c.getSelectedFile().getName() +
                    "  ·  " + imgOriginal.getWidth() + " × " + imgOriginal.getHeight() + " px");
            imgTransformada = imgOriginal;
            mostrar("Original");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir imagem.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Salva a imagem transformada como PNG
    private void salvarImagem() {
        if (imgTransformada == null) {
            JOptionPane.showMessageDialog(this, "Nenhuma imagem para salvar!",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JFileChooser c = new JFileChooser();
        c.setFileFilter(new FileNameExtensionFilter("PNG (*.png)", "png"));
        if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            File f = c.getSelectedFile();
            if (!f.getName().toLowerCase().endsWith(".png"))
                f = new File(f.getAbsolutePath() + ".png");
            ImageIO.write(imgTransformada, "png", f);
            JOptionPane.showMessageDialog(this, "Imagem salva: " + f.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar imagem.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    //  TRANSFORMAÇÕES GEOMÉTRICAS
    //  Obs.: cada transformação parte sempre da imagem original
    // =========================================================

    private void transladar() {
        if (semImagem()) return;
        JTextField txF = new JTextField("50"), tyF = new JTextField("50");
        Object[] campos = {"Deslocamento X:", txF, "Deslocamento Y:", tyF};
        if (JOptionPane.showConfirmDialog(this, campos, "Translação",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            int tx = Integer.parseInt(txF.getText().trim());
            int ty = Integer.parseInt(tyF.getText().trim());
            imgTransformada = Opcoes.translacao(imgOriginal, tx, ty);
            mostrar("Translação (" + tx + ", " + ty + ")");
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Digite valores inteiros válidos.");
        }
    }

    private void rotacionar() {
        if (semImagem()) return;
        double g = pedirDouble("Ângulo de rotação (graus):", "Rotação");
        if (!Double.isNaN(g)) {
            imgTransformada = Opcoes.rotacao(imgOriginal, g);
            mostrar("Rotação " + g + "°");
        }
    }

    private void espelharHorizontal() {
        if (semImagem()) return;
        imgTransformada = Opcoes.espelharHorizontal(imgOriginal);
        mostrar("Espelhamento Horizontal");
    }

    private void espelharVertical() {
        if (semImagem()) return;
        imgTransformada = Opcoes.espelharVertical(imgOriginal);
        mostrar("Espelhamento Vertical");
    }

    private void aumentar() {
        if (semImagem()) return;
        double f = pedirDouble("Fator de aumento (ex: 2, 3, 1.5):", "Aumentar");
        if (!Double.isNaN(f) && f > 0) {
            imgTransformada = Opcoes.escala(imgOriginal, f, f);
            mostrar("Escala ×" + f);
        }
    }

    private void diminuir() {
        if (semImagem()) return;
        double f = pedirDouble("Fator de redução (ex: 2, 3, 4):", "Diminuir");
        if (!Double.isNaN(f) && f > 0) {
            imgTransformada = Opcoes.escala(imgOriginal, 1.0 / f, 1.0 / f);
            mostrar("Escala ÷" + f);
        }
    }

    // =========================================================
    //  FILTROS
    //  Obs.: cada filtro parte sempre da imgOriginal, zerando
    //        qualquer filtro anterior aplicado
    // =========================================================

    private void grayscale() {
        if (semImagem()) return;

        String[] opcoes = {
            "Média simples  (R+G+B)/3",
            "Luminância BT.709  (0.2125R + 0.7154G + 0.0721B)",
            "Ponderação alt.  (0.50R + 0.419G + 0.081B)"
        };
        int resp = JOptionPane.showOptionDialog(this,
                "Selecione a fórmula de conversão para escala de cinza:",
                "Grayscale",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, opcoes, opcoes[0]);

        if (resp < 0) return;
        imgTransformada = Opcoes.grayscale(imgOriginal, resp);
        mostrar("Grayscale — " + opcoes[resp].split("  ")[0]);
    }

    private void ajustarBrilho() {
        if (semImagem()) return;

        JSlider slider = new JSlider(-255, 255, 0);
        slider.setMajorTickSpacing(85);
        slider.setMinorTickSpacing(17);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(420, 60));

        JLabel valorLabel = new JLabel("Brilho (B): 0", JLabel.CENTER);
        valorLabel.setFont(F_MONO);

        final int MAX_PV = 300;
        double esc = Math.min(1.0, Math.min(
                (double) MAX_PV / imgOriginal.getWidth(),
                (double) MAX_PV / imgOriginal.getHeight()));
        final BufferedImage mini = Opcoes.escala(imgOriginal, esc, esc);
        final JLabel preview    = new JLabel(new ImageIcon(mini));
        preview.setHorizontalAlignment(JLabel.CENTER);

        slider.addChangeListener(e -> {
            int delta = slider.getValue();
            valorLabel.setText("Brilho (B): " + (delta >= 0 ? "+" : "") + delta);
            preview.setIcon(new ImageIcon(Opcoes.ajustarBrilho(mini, delta)));
        });

        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.add(valorLabel, BorderLayout.NORTH);
        painel.add(preview,    BorderLayout.CENTER);
        painel.add(slider,     BorderLayout.SOUTH);

        int resp = JOptionPane.showConfirmDialog(this, painel, "Ajustar brilho",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (resp == JOptionPane.OK_OPTION) {
            int delta = slider.getValue();
            imgTransformada = Opcoes.ajustarBrilho(imgOriginal, delta);
            mostrar("Brilho B=" + (delta >= 0 ? "+" : "") + delta);
        }
    }

    private void ajustarContraste() {
        if (semImagem()) return;

        // Contraste usa valores decimais: C < 1 reduz, C > 1 aumenta, C = 1 neutro
        // Slider de 0.0 a 3.0, passo 0.05, valor neutro = 1.0
        JSlider slider = new JSlider(0, 300, 100); // internamente ×100 para ter decimais
        slider.setMajorTickSpacing(50);
        slider.setMinorTickSpacing(10);
        slider.setPaintTicks(true);

        // Rótulos customizados (0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0)
        java.util.Hashtable<Integer,JLabel> labels = new java.util.Hashtable<>();
        for (int v = 0; v <= 300; v += 50)
            labels.put(v, new JLabel(String.format("%.1f", v / 100.0)));
        slider.setLabelTable(labels);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(420, 60));

        JLabel valorLabel = new JLabel("Contraste (C): 1.00  [neutro]", JLabel.CENTER);
        valorLabel.setFont(F_MONO);

        final int MAX_PV = 300;
        double esc = Math.min(1.0, Math.min(
                (double) MAX_PV / imgOriginal.getWidth(),
                (double) MAX_PV / imgOriginal.getHeight()));
        final BufferedImage mini = Opcoes.escala(imgOriginal, esc, esc);
        final JLabel preview    = new JLabel(new ImageIcon(mini));
        preview.setHorizontalAlignment(JLabel.CENTER);

        slider.addChangeListener(e -> {
            double c = slider.getValue() / 100.0;
            String nota = (c == 1.0) ? "  [neutro]" : (c < 1.0) ? "  [reduz]" : "  [aumenta]";
            valorLabel.setText(String.format("Contraste (C): %.2f%s", c, nota));
            preview.setIcon(new ImageIcon(Opcoes.ajustarContraste(mini, c)));
        });

        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.add(valorLabel, BorderLayout.NORTH);
        painel.add(preview,    BorderLayout.CENTER);
        painel.add(slider,     BorderLayout.SOUTH);

        int resp = JOptionPane.showConfirmDialog(this, painel, "Ajustar Contraste",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (resp == JOptionPane.OK_OPTION) {
            double c = slider.getValue() / 100.0;
            imgTransformada = Opcoes.ajustarContraste(imgOriginal, c);
            mostrar(String.format("Contraste C=%.2f", c));
        }
    }

    private void threshold() {
        if (semImagem()) return;

        // Slider de 0 a 255; limiar padrão 128
        JSlider slider = new JSlider(0, 255, 128);
        slider.setMajorTickSpacing(51);
        slider.setMinorTickSpacing(17);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(420, 60));

        JLabel valorLabel = new JLabel("Limiar: 128  —  pixels ≥ limiar → branco; < limiar → preto", JLabel.CENTER);
        valorLabel.setFont(F_MONO);

        final int MAX_PV = 300;
        double esc = Math.min(1.0, Math.min(
                (double) MAX_PV / imgOriginal.getWidth(),
                (double) MAX_PV / imgOriginal.getHeight()));
        final BufferedImage mini = Opcoes.escala(imgOriginal, esc, esc);
        final JLabel preview    = new JLabel(new ImageIcon(
                Opcoes.threshold(mini, 128)));
        preview.setHorizontalAlignment(JLabel.CENTER);

        slider.addChangeListener(e -> {
            int lim = slider.getValue();
            valorLabel.setText("Limiar: " + lim +
                    "  —  pixels ≥ " + lim + " → branco; < " + lim + " → preto");
            preview.setIcon(new ImageIcon(Opcoes.threshold(mini, lim)));
        });

        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.add(valorLabel, BorderLayout.NORTH);
        painel.add(preview,    BorderLayout.CENTER);
        painel.add(slider,     BorderLayout.SOUTH);

        int resp = JOptionPane.showConfirmDialog(this, painel, "Threshold (Binarização)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (resp == JOptionPane.OK_OPTION) {
            int lim = slider.getValue();
            imgTransformada = Opcoes.threshold(imgOriginal, lim);
            mostrar("Threshold (limiar=" + lim + ")");
        }
    }

    private void passaBaixa() {
        if (semImagem()) return;

        String[] opcoes = {"3×3 (suave)", "5×5 (médio)", "7×7 (intenso)"};
        int resp = JOptionPane.showOptionDialog(this,
                "Selecione o tamanho do kernel gaussiano:",
                "Passa Baixa",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, opcoes, opcoes[0]);

        if (resp < 0) return;
        int tam = resp * 2 + 3; // 3, 5 ou 7

        // Aplica na original — reseta qualquer filtro anterior
        imgTransformada = Opcoes.passaBaixa(imgOriginal, tam);
        mostrar("Passa Baixa " + tam + "×" + tam);
    }

    private void sobel() {
        if (semImagem()) return;

        double limiar = pedirDouble(
                "Limiar de detecção (0–255)  —  sugerido: 80 a 120:", "Sobel");
        if (Double.isNaN(limiar)) return;

        // Aplica na original — reseta qualquer filtro anterior
        imgTransformada = Opcoes.sobel(imgOriginal, (int) Math.round(limiar));
        mostrar("Sobel (limiar=" + (int) Math.round(limiar) + ")");
    }

    private void canny() {
        if (semImagem()) return;

        JTextField loF = new JTextField("50");
        JTextField hiF = new JTextField("150");
        Object[] campos = {
            "Threshold baixo — bordas fracas (0–255):", loF,
            "Threshold alto  — bordas fortes (0–255):", hiF
        };

        if (JOptionPane.showConfirmDialog(this, campos, "Canny",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try {
            int lo = Integer.parseInt(loF.getText().trim());
            int hi = Integer.parseInt(hiF.getText().trim());

            if (lo < 0 || hi < 0 || lo > 255 || hi > 255) {
                JOptionPane.showMessageDialog(this, "Os valores devem estar entre 0 e 255.");
                return;
            }
            if (lo >= hi) {
                JOptionPane.showMessageDialog(this, "O threshold baixo deve ser menor que o alto.");
                return;
            }

            // Aplica na original — reseta qualquer filtro anterior
            imgTransformada = Opcoes.canny(imgOriginal, lo, hi);
            mostrar("Canny (lo=" + lo + ", hi=" + hi + ")");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Digite valores inteiros válidos.");
        }
    }

    // =========================================================
    //  MORFOLOGIA MATEMÁTICA
    //  Obs.: trabalham sobre a versão binarizada (threshold 128)
    //        da imagem original, elemento estruturante 3x3 em cruz
    // =========================================================

    private void dilatacao() {
        if (semImagem()) return;
        imgTransformada = Opcoes.dilatacao(imgOriginal);
        mostrar("Morfologia — Dilatação");
    }

    private void erosao() {
        if (semImagem()) return;
        imgTransformada = Opcoes.erosao(imgOriginal);
        mostrar("Morfologia — Erosão");
    }

    private void abertura() {
        if (semImagem()) return;
        imgTransformada = Opcoes.abertura(imgOriginal);
        mostrar("Morfologia — Abertura");
    }

    private void fechamento() {
        if (semImagem()) return;
        imgTransformada = Opcoes.fechamento(imgOriginal);
        mostrar("Morfologia — Fechamento");
    }

    private void afinamento() {
        if (semImagem()) return;
        imgTransformada = Opcoes.afinamento(imgOriginal);
        mostrar("Morfologia — Afinamento (Zhang-Suen)");
    }

    // =========================================================
    //  EXERCÍCIOS RESOLVIDOS
    // =========================================================

    private void exercicio1() {
        if (semImagem()) return;
        String resultado = Exercicios.lerRelogio(imgOriginal);
        JOptionPane.showMessageDialog(this,
                "Horário identificado: " + resultado,
                "Exercício 1 — Relógio Analógico",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void exercicio2() {
        if (semImagem()) return;
        String resultado = Exercicios.contarObjetosPorCor(imgOriginal);
        JOptionPane.showMessageDialog(this,
                resultado,
                "Exercício 2 — Contagem de Objetos por Cor",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void exercicio3() {
        if (semImagem()) return;
        String resultado = Exercicios.identificarLetras(imgOriginal);
        JOptionPane.showMessageDialog(this,
                "Letras identificadas: " + resultado,
                "Exercício 3 — Identificação de Letras",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void exercicio4() {
        if (semImagem()) return;
        String resultado = Exercicios.identificarPlacas(imgOriginal);
        JOptionPane.showMessageDialog(this,
                "Placas identificadas: " + resultado,
                "Exercício 4 — Placas de Trânsito",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void exercicio5() {
        if (semImagem()) return;
        String resultado = Exercicios.compararBarras(imgOriginal);
        JOptionPane.showMessageDialog(this,
                resultado,
                "Exercício 5 — Comparação de Barras",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Main ──────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PDI().setVisible(true));
    }
}