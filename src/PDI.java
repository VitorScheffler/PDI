import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class PDI extends JFrame {

    // ── Paleta ────────────────────────────────────────────────────
    private static final Color FUNDO   = new Color(248, 248, 250);
    private static final Color PAINEL  = Color.WHITE;
    private static final Color INNER   = new Color(245, 245, 248);
    private static final Color BORDA   = new Color(220, 220, 228);
    private static final Color TEXTO   = new Color(30,  30,  40);
    private static final Color SUB     = new Color(130, 130, 150);
    private static final Color AZUL    = new Color(59,  130, 246);

    // ── Fontes ────────────────────────────────────────────────────
    private static final Font F_TITULO  = new Font("Segoe UI", Font.BOLD,  14);
    private static final Font F_LABEL   = new Font("Segoe UI", Font.BOLD,  12);
    private static final Font F_NORMAL  = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_PEQUENA = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font F_MONO    = new Font("Consolas",  Font.PLAIN, 11);

    // ── Componentes ───────────────────────────────────────────────
    private final JLabel lblOriginal   = new JLabel();
    private final JLabel lblTransf     = new JLabel();
    private final JLabel infoOriginal  = new JLabel("Nenhuma imagem carregada");
    private final JLabel infoTransf    = new JLabel("—");

    private BufferedImage imgOriginal;
    private BufferedImage imgTransformada;

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

    // ── Look & Feel ───────────────────────────────────────────────
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
        UIManager.put("ScrollBar.width",              8);
    }

    // ── Barra de título ───────────────────────────────────────────
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

        JLabel autor = new JLabel("Vitor Matheus Scheffler");
        autor.setFont(F_PEQUENA);
        autor.setForeground(SUB);

        p.add(esq,   BorderLayout.WEST);
        p.add(autor, BorderLayout.EAST);
        return p;
    }

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

    // ── Painel central ────────────────────────────────────────────
    private JPanel criarCentral() {
        JPanel p = new JPanel(new GridLayout(1, 2, 12, 0));
        p.setBackground(FUNDO);
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        p.add(cartao("Imagem Original",     lblOriginal, infoOriginal));
        p.add(cartao("Imagem Transformada", lblTransf,   infoTransf));
        return p;
    }

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
        imgLabel.setText("<html><center><span style='color:#d1d5db;font-size:11px;font-family:Segoe UI'>" +
                "Nenhuma imagem</span></center></html>");

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

    // ══════════════════════════════════════════════════════════════
    //  MENU
    // ══════════════════════════════════════════════════════════════
    private void criarMenu() {
        JMenuBar bar = new JMenuBar();

        // Arquivo
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

        // Transformações Geométricas
        JMenu transf = menu("Transformações Geométricas");
        transf.add(item("Transladar",  e -> transladar()));
        transf.add(item("Rotacionar",  e -> rotacionar()));
        transf.add(item("Aumentar", e -> aumentar()));
        transf.add(item("Diminuir", e -> diminuir()));
        JMenu espelhar = menu("Espelhar");
        espelhar.add(item("Horizontal", e -> espelharHorizontal()));
        espelhar.add(item("Vertical",   e -> espelharVertical()));
        transf.add(espelhar);

        // Filtros
        JMenu filtros = menu("Filtros");
        filtros.add(item("Grayscale",          e -> grayscale()));
        filtros.add(item("Ajustar Brilho",     e -> ajustarBrilho()));
        filtros.add(item("Passa Baixa",        e -> passaBaixa()));
        filtros.add(item("Passa Alta",         e -> passaAlta()));
        JMenu threshold = menu("Threshold");
        threshold.add(item("Sobel",   e -> thresholdSobel()));
        threshold.add(item("Canny",   e -> thresholdCanny()));
        filtros.add(threshold);

        // Morfologia
        JMenu morf = menu("Morfologia Matemática");
        morf.add(item("Dilatação",  e -> dilatacao()));
        morf.add(item("Erosão",     e -> erosao()));
        morf.add(item("Abertura",   e -> abertura()));
        morf.add(item("Fechamento", e -> fechamento()));
        morf.add(item("Afinamento", e -> afinamento()));

        // Extração
        JMenu extr = menu("Extração de Características");
        extr.add(item("Desafio", e -> desafio()));

        bar.add(arquivo);
        bar.add(transf);
        bar.add(filtros);
        bar.add(morf);
        bar.add(extr);
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
    private boolean semImagem() {
        if (imgOriginal == null) {
            JOptionPane.showMessageDialog(this, "Carregue uma imagem primeiro!",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return true;
        }
        return false;
    }

    private void mostrar(String descricao) {
        lblOriginal.setText(null);
        lblTransf.setText(null);
        lblTransf.setIcon(new ImageIcon(imgTransformada));
        infoTransf.setText(descricao + "  ·  " +
                imgTransformada.getWidth() + " × " + imgTransformada.getHeight() + " px");
    }

    private double pedirDouble(String msg, String titulo, double padrao) {
        String s = JOptionPane.showInputDialog(this, msg, titulo, JOptionPane.PLAIN_MESSAGE);
        if (s == null) return Double.NaN;
        try { return Double.parseDouble(s.trim()); }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor inválido.");
            return Double.NaN;
        }
    }

    // ── Arquivo ───────────────────────────────────────────────────
    private void abrirImagem() {
        JFileChooser c = new JFileChooser();
        c.setFileFilter(new FileNameExtensionFilter("Imagens (jpg, png, bmp)", "jpg","jpeg","png","bmp"));
        if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try {
            imgOriginal = ImageIO.read(c.getSelectedFile());
            lblOriginal.setText(null);
            lblOriginal.setIcon(new ImageIcon(imgOriginal));
            infoOriginal.setText(c.getSelectedFile().getName() +
                    "  ·  " + imgOriginal.getWidth() + " × " + imgOriginal.getHeight() + " px");
            imgTransformada = imgOriginal;
            mostrar("Original");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir imagem.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

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
            if (!f.getName().toLowerCase().endsWith(".png")) f = new File(f.getAbsolutePath() + ".png");
            ImageIO.write(imgTransformada, "png", f);
            JOptionPane.showMessageDialog(this, "Imagem salva: " + f.getName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar imagem.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  TRANFORMAÇÕES GEOMÉTICAS
    // ══════════════════════════════════════════════════════════════

    // ── Transladar ────────────────────────────────
    private void transladar() {
        if (semImagem()) return;
        JTextField txF = new JTextField("50"), tyF = new JTextField("50");
        Object[] msg = {"Deslocamento X:", txF, "Deslocamento Y:", tyF};
        if (JOptionPane.showConfirmDialog(this, msg, "Translação",
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

    // ── Rotacionar ────────────────────────────────
    private void rotacionar() {
        if (semImagem()) return;
        double g = pedirDouble("Ângulo de rotação (graus):", "Rotação", 90);
        if (!Double.isNaN(g)) {
            imgTransformada = Opcoes.rotacao(imgOriginal, g);
            mostrar("Rotação " + g + "°");
        }
    }

    // ── Espelhar Horizontalmente ────────────────────────────────
    private void espelharHorizontal() {
        if (semImagem()) return;
        imgTransformada = Opcoes.espelharHorizontal(imgOriginal);
        mostrar("Espelhamento Horizontal");
    }

    // ── Espelhar Verticalmente ────────────────────────────────
    private void espelharVertical() {
        if (semImagem()) return;
        imgTransformada = Opcoes.espelharVertical(imgOriginal);
        mostrar("Espelhamento Vertical");
    }

    // ── Aumentar ────────────────────────────────
    private void aumentar() {
        if (semImagem()) return;
        double f = pedirDouble("Fator de aumento (ex: 2, 3, 1.5):", "Aumentar", 2);
        if (!Double.isNaN(f) && f > 0) {
            imgTransformada = Opcoes.escala(imgOriginal, f, f);
            mostrar("Escala ×" + f);
        } else if (!Double.isNaN(f)) {
            JOptionPane.showMessageDialog(this, "O fator deve ser positivo.");
        }
    }

    // ── Diminuir ────────────────────────────────
    private void diminuir() {
        if (semImagem()) return;
        double f = pedirDouble("Fator de redução (ex: 2, 3, 4):", "Diminuir", 2);
        if (!Double.isNaN(f) && f > 0) {
            imgTransformada = Opcoes.escala(imgOriginal, 1.0 / f, 1.0 / f);
            mostrar("Escala ÷" + f);
        } else if (!Double.isNaN(f)) {
            JOptionPane.showMessageDialog(this, "O fator deve ser positivo.");
        }
    }

    
    // ══════════════════════════════════════════════════════════════
    //  FILTROS
    // ══════════════════════════════════════════════════════════════

    // ── Ajusta escalas de cinza ────────────────────────────────
    private void grayscale() {
        if (semImagem()) return;
        imgTransformada = Opcoes.grayscale(imgOriginal);
        mostrar("Grayscale (luminosidade)");
    }

    // ── Ajustar Brilho ────────────────────────────────
    private void ajustarBrilho() {
        if (semImagem()) return;

        final BufferedImage base = imgTransformada;

        JSlider slider = new JSlider(-255, 255, 0);
        slider.setMajorTickSpacing(85);
        slider.setMinorTickSpacing(17);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setPreferredSize(new Dimension(420, 60));

        JLabel valorLabel = new JLabel("Brilho: 0", JLabel.CENTER);
        valorLabel.setFont(F_MONO);

        final int MAX_PV = 300;
        double escPv = Math.min(1.0, Math.min(
                (double) MAX_PV / base.getWidth(),
                (double) MAX_PV / base.getHeight()));
        final BufferedImage baseMin = Opcoes.escala(base, escPv, escPv);
        final JLabel previewLabel  = new JLabel(new ImageIcon(baseMin));
        previewLabel.setHorizontalAlignment(JLabel.CENTER);

        slider.addChangeListener(e -> {
            int delta = slider.getValue();
            valorLabel.setText("Brilho: " + (delta >= 0 ? "+" : "") + delta);
            previewLabel.setIcon(new ImageIcon(Opcoes.ajustarBrilho(baseMin, delta)));
        });

        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.add(valorLabel,   BorderLayout.NORTH);
        painel.add(previewLabel, BorderLayout.CENTER);
        painel.add(slider,       BorderLayout.SOUTH);

        int resp = JOptionPane.showConfirmDialog(
                this, painel, "Ajustar Brilho",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (resp == JOptionPane.OK_OPTION) {
            int delta = slider.getValue();
            imgTransformada = Opcoes.ajustarBrilho(base, delta);
            mostrar("Brilho " + (delta >= 0 ? "+" : "") + delta);
        }
    }

    // ── Filtros Pendentes ─────────────────────────────────────────
    private void passaBaixa() { if (semImagem()) return; /* TODO */ }
    private void passaAlta()  { if (semImagem()) return; /* TODO */ }
    private void thresholdSobel()  { if (semImagem()) return; /* TODO */ }
    private void thresholdCanny()  { if (semImagem()) return; /* TODO */ }

    // ══════════════════════════════════════════════════════════════
    //  MORFOLOGIA
    // ══════════════════════════════════════════════════════════════
    private void dilatacao()  { if (semImagem()) return; /* TODO */ }
    private void erosao()     { if (semImagem()) return; /* TODO */ }
    private void abertura()   { if (semImagem()) return; /* TODO */ }
    private void fechamento() { if (semImagem()) return; /* TODO */ }
    private void afinamento() { if (semImagem()) return; /* TODO */ }

    // ══════════════════════════════════════════════════════════════
    //  DESAFIO
    // ══════════════════════════════════════════════════════════════
    private void desafio()    { if (semImagem()) return; /* TODO */ }


    // ══════════════════════════════════════════════════════════════
    //  MAIN
    // ══════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PDI().setVisible(true));
    }
}