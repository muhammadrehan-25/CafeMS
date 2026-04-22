package cafe.ui;

import cafe.utils.UIConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.awt.geom.RoundRectangle2D;

/**
 * SplashScreen - Shown for ~2.5s on startup before the login window appears.
 * Matches the cafe's warm espresso gradient theme.
 */
public class SplashScreen extends JWindow {

    private static final int W = 480;
    private static final int H = 420;

    public SplashScreen() {
        setSize(W, H);
        setLocationRelativeTo(null);

        // Make window edges rounded
        setShape(new RoundRectangle2D.Double(0, 0, W, H, 24, 24));

        buildUI();
        setVisible(true);
    }

    private void buildUI() {
        // Full-window gradient panel
        JPanel bg = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Vertical espresso gradient
                GradientPaint gp = new GradientPaint(
                        0, 0,   UIConstants.GRAD_TOP,
                        0, getHeight(), UIConstants.GRAD_BOTTOM);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                // Subtle bottom glow
                GradientPaint glow = new GradientPaint(
                        0, getHeight() - 60, new Color(212, 169, 106, 0),
                        0, getHeight(),      new Color(212, 169, 106, 60));
                g2.setPaint(glow);
                g2.fillRoundRect(0, getHeight() - 60, getWidth(), 60, 0, 0);
                g2.dispose();
            }
        };
        bg.setLayout(new BorderLayout());
        bg.setBorder(new EmptyBorder(30, 40, 24, 40));
        setContentPane(bg);

        // ── Center content ──
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        // ── Logo (no circle — plain transparent PNG) ──
        final BufferedImage[] logoImg = {null};
        try {
            InputStream is = SplashScreen.class.getResourceAsStream("/cafe/logo.png");
            if (is != null) logoImg[0] = ImageIO.read(is);
        } catch (Exception ignored) {}

        final int LOGO_H = 200; // fixed height; width fills splash
        JPanel logoBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                if (logoImg[0] == null) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,     RenderingHints.VALUE_RENDER_QUALITY);
                int srcW = logoImg[0].getWidth();
                int srcH = logoImg[0].getHeight();
                double scale = Math.min((double) getWidth() / srcW, (double) getHeight() / srcH);
                int dw = (int) (srcW * scale);
                int dh = (int) (srcH * scale);
                int x  = (getWidth()  - dw) / 2;
                int y  = (getHeight() - dh) / 2;
                g2.drawImage(logoImg[0], x, y, dw, dh, null);
                g2.dispose();
            }
        };
        logoBadge.setOpaque(false);
        logoBadge.setPreferredSize(new Dimension(W - 80, LOGO_H));
        logoBadge.setMaximumSize(new Dimension(Integer.MAX_VALUE, LOGO_H));
        logoBadge.setAlignmentX(Component.CENTER_ALIGNMENT);


        // Cafe name
        JLabel lblName = new JLabel("AL Rehan Cafe", SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 30));
        lblName.setForeground(Color.WHITE);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Tagline
        JLabel lblTag = new JLabel("Cafe Management System", SwingConstants.CENTER);
        lblTag.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        lblTag.setForeground(UIConstants.ACCENT);
        lblTag.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Decorative line
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(255, 255, 255, 50));
        sep.setMaximumSize(new Dimension(320, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(logoBadge);
        center.add(Box.createVerticalStrut(14));
        center.add(lblName);
        center.add(Box.createVerticalStrut(6));
        center.add(lblTag);
        center.add(Box.createVerticalStrut(18));
        center.add(sep);
        center.add(Box.createVerticalGlue());


        bg.add(center, BorderLayout.CENTER);

        // ── Loading bar at bottom ──
        JPanel bottom = new JPanel(new BorderLayout(0, 6));
        bottom.setOpaque(false);

        JLabel lblLoading = new JLabel("Loading...", SwingConstants.CENTER);
        lblLoading.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblLoading.setForeground(new Color(255, 255, 255, 150));

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(false);
        bar.setPreferredSize(new Dimension(0, 5));
        bar.setBorderPainted(false);
        bar.setOpaque(false);
        // Custom progress bar painter
        bar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            @Override
            protected void paintDeterminate(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // track
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 4, 4);
                // filled
                int filled = (int) (c.getWidth() * (bar.getValue() / 100.0));
                GradientPaint gp = new GradientPaint(0, 0, UIConstants.ACCENT,
                        filled, 0, new Color(0xFF, 0xFF, 0xFF, 180));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, filled, c.getHeight(), 4, 4);
                g2.dispose();
            }
        });

        bottom.add(lblLoading, BorderLayout.NORTH);
        bottom.add(bar, BorderLayout.SOUTH);
        bg.add(bottom, BorderLayout.SOUTH);

        // ── Animate progress bar ──
        Timer timer = new Timer(25, null);
        timer.addActionListener(e -> {
            int v = bar.getValue() + 2;
            if (v >= 100) { bar.setValue(100); timer.stop(); }
            else bar.setValue(v);
        });
        timer.start();
    }

    /**
     * Shows the splash screen for the given duration (ms), then disposes it.
     * Call this from main() before opening the login window.
     */
    public static void showFor(int durationMs) {
        SplashScreen splash = new SplashScreen();
        try { Thread.sleep(durationMs); } catch (InterruptedException ignored) {}
        splash.dispose();
    }
}
