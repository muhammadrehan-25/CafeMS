package cafe.ui;

import cafe.dao.UserDAO;
import cafe.models.User;
import cafe.utils.UIConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * LoginFrame - Entry-point login window with role-based authentication.
 */
public class LoginFrame extends JFrame {

    private JTextField     txtUsername;
    private JPasswordField txtPassword;
    private JLabel         lblError;
    private JButton        btnLogin;
    private UserDAO        userDAO;

    public LoginFrame() {
        userDAO = new UserDAO();
        initUI();
    }

    private void initUI() {
        setTitle("AL Rehan Cafe - Login");
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridBagLayout());
        setContentPane(root);

        // ── Left Panel (Brand Showcase) ──
        JPanel leftPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, UIConstants.GRAD_TOP,
                        0, getHeight(), UIConstants.GRAD_BOTTOM);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // Logo
        final BufferedImage[] logoImg = {null};
        try {
            InputStream is = LoginFrame.class.getResourceAsStream("/cafe/logo.png");
            if (is != null) logoImg[0] = ImageIO.read(is);
        } catch (Exception ignored) {}

        JPanel logoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                if (logoImg[0] == null) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
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
        logoPanel.setOpaque(false);
        logoPanel.setPreferredSize(new Dimension(500, 420));
        logoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 420));
        logoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblWelcome = new JLabel("Welcome to", SwingConstants.CENTER);
        lblWelcome.setFont(new Font("Segoe UI", Font.PLAIN, 28));
        lblWelcome.setForeground(new Color(0xD4B896));
        lblWelcome.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblName = new JLabel("AL REHAN CAFE", SwingConstants.CENTER);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 54));
        lblName.setForeground(UIConstants.ACCENT);
        lblName.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblCity = new JLabel("Premium Coffee & Dining  •  Karachi, Pakistan", SwingConstants.CENTER);
        lblCity.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        lblCity.setForeground(new Color(0xD4B896));
        lblCity.setAlignmentX(Component.CENTER_ALIGNMENT);

        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(logoPanel);
        leftPanel.add(Box.createVerticalStrut(30));
        leftPanel.add(lblWelcome);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(lblName);
        leftPanel.add(Box.createVerticalStrut(15));
        leftPanel.add(lblCity);
        leftPanel.add(Box.createVerticalGlue());

        // ── Right Panel (Authentication) ──
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(UIConstants.BG_LIGHT);

        // Login Card Area
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(40, 80, 40, 80));
        card.setMaximumSize(new Dimension(500, 600));

        JLabel lblLoginTitle = new JLabel("Login", SwingConstants.CENTER);
        lblLoginTitle.setFont(new Font("Segoe UI", Font.BOLD, 36));
        lblLoginTitle.setForeground(UIConstants.TEXT_DARK);
        lblLoginTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("Sign in to your account to continue", SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblSub.setForeground(UIConstants.TEXT_GRAY);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Username Field
        JLabel lUser = fieldLabel("Username");
        txtUsername = new JTextField();
        styleField(txtUsername);
        addFocusBorder(txtUsername);

        // Password Field
        JLabel lPass = fieldLabel("Password");
        txtPassword = new JPasswordField();
        txtPassword.setEchoChar('●');
        styleField(txtPassword);
        addFocusBorder(txtPassword);

        JPanel passRow = new JPanel(new BorderLayout());
        passRow.setOpaque(false);
        passRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        passRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        passRow.add(txtPassword, BorderLayout.CENTER);

        // Eye Toggle Button
        JButton btnEye = new JButton("👁") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(getModel().isRollover() ? new Color(0xEDE0D4) : Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        btnEye.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        btnEye.setForeground(UIConstants.TEXT_GRAY);
        btnEye.setContentAreaFilled(false);
        btnEye.setBorderPainted(false);
        btnEye.setFocusPainted(false);
        btnEye.setPreferredSize(new Dimension(46, 46));
        btnEye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEye.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 1, UIConstants.BORDER_COLOR),
                new EmptyBorder(0, 4, 0, 4)));
        final boolean[] passVisible = {false};
        btnEye.addActionListener(e -> {
            passVisible[0] = !passVisible[0];
            txtPassword.setEchoChar(passVisible[0] ? (char) 0 : '●');
            btnEye.setForeground(passVisible[0] ? UIConstants.PRIMARY : UIConstants.TEXT_GRAY);
        });
        passRow.add(btnEye, BorderLayout.EAST);

        // Error Label
        lblError = new JLabel(" ");
        lblError.setFont(UIConstants.FONT_SMALL);
        lblError.setForeground(UIConstants.DANGER);
        lblError.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Login Button
        btnLogin = new JButton("LOGIN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isPressed() ? UIConstants.PRIMARY_DARK
                        : (getModel().isRollover() ? new Color(0x8D6244) : UIConstants.PRIMARY);
                Color bot = getModel().isPressed() ? new Color(0x2C1A0E)
                        : (getModel().isRollover() ? UIConstants.PRIMARY : UIConstants.PRIMARY_DARK);
                GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bot);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
            }
        };
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 16));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setContentAreaFilled(false);
        btnLogin.setBorderPainted(false);
        btnLogin.setFocusPainted(false);
        btnLogin.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogin.addActionListener(e -> doLogin());
        getRootPane().setDefaultButton(btnLogin);

        // Hint Label

        // Assemble Right Panel Card
        card.add(lblLoginTitle);
        card.add(Box.createVerticalStrut(10));
        card.add(lblSub);
        card.add(Box.createVerticalStrut(50));
        card.add(lUser);
        card.add(Box.createVerticalStrut(10));
        card.add(txtUsername);
        card.add(Box.createVerticalStrut(25));
        card.add(lPass);
        card.add(Box.createVerticalStrut(10));
        card.add(passRow);
        card.add(Box.createVerticalStrut(15));
        card.add(lblError);
        card.add(Box.createVerticalStrut(25));
        card.add(btnLogin);
        card.add(Box.createVerticalStrut(30));

        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(card);
        rightPanel.add(Box.createVerticalGlue());

        // Add both panels to root using GridBagLayout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;

        gbc.gridx = 0;
        gbc.weightx = 0.55; // Left panel takes 55% width
        root.add(leftPanel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.45; // Right panel takes 45% width
        root.add(rightPanel, gbc);

        // ── Keyboard shortcuts ──
        KeyStroke ctrlF = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F,
                java.awt.event.InputEvent.CTRL_DOWN_MASK);
        KeyStroke f11   = KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F11, 0);
        javax.swing.Action toggleFS = new javax.swing.AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int state = getExtendedState();
                if ((state & MAXIMIZED_BOTH) != 0) {
                    setExtendedState(NORMAL);
                } else {
                    setExtendedState(MAXIMIZED_BOTH);
                }
            }
        };
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ctrlF, "toggleFS");
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(f11,   "toggleFS");
        getRootPane().getActionMap().put("toggleFS", toggleFS);

        setVisible(true);
    }

    // ── Login logic ───────────────────────────────────────────────────────────

    private void doLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }
        btnLogin.setText("...");
        btnLogin.setEnabled(false);

        SwingUtilities.invokeLater(() -> {
            User user = userDAO.login(username, password);
            btnLogin.setText("LOGIN");
            btnLogin.setEnabled(true);
            if (user != null) {
                dispose();
                new MainFrame(user);
            } else {
                showError("Invalid username or password. Try again.");
                txtPassword.setText("");
                txtPassword.requestFocus();
                shakeFrame();
            }
        });
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setForeground(UIConstants.DANGER);
    }

    private void shakeFrame() {
        final int[] offsets = {-10, 10, -8, 8, -5, 5, -3, 3, 0};
        final Point origin  = getLocation();
        final int[] i = {0};
        Timer t = new Timer(40, null);
        t.addActionListener(e -> {
            setLocation(origin.x + offsets[i[0]], origin.y);
            if (++i[0] >= offsets.length) {
                ((Timer) e.getSource()).stop();
                setLocation(origin);
            }
        });
        t.start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_BOLD);
        lbl.setForeground(UIConstants.TEXT_DARK);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        return lbl;
    }

    private void styleField(JTextField field) {
        field.setFont(UIConstants.FONT_REGULAR);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        field.setBackground(Color.WHITE);
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                new EmptyBorder(6, 12, 6, 12)));
    }

    private void addFocusBorder(JTextField field) {
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.PRIMARY, 2, true),
                        new EmptyBorder(5, 11, 5, 11)));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1, true),
                        new EmptyBorder(6, 12, 6, 12)));
            }
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SplashScreen.showFor(2500);
        SwingUtilities.invokeLater(LoginFrame::new);
    }
}