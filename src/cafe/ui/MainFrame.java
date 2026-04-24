package cafe.ui;

import cafe.models.User;
import cafe.utils.UIConstants;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MainFrame - The main application window.
 * Contains a sidebar for navigation and a content panel that swaps panels.
 */
public class MainFrame extends JFrame {

    private User currentUser;
    private JPanel contentPanel;
    private CardLayout cardLayout;

    // Track active card + nav buttons for active-state highlight
    private String activeCard = "Dashboard";
    private final List<JButton> navButtons = new ArrayList<>();
    private final List<String>  navCards   = new ArrayList<>();

    // Panels
    private DashboardPanel dashboardPanel;
    private MenuPanel menuPanel;
    private OrderPanel orderPanel;
    private OrdersListPanel ordersListPanel;
    private UserManagementPanel userPanel;

    public MainFrame(User user) {
        this.currentUser = user;
        initUI();
    }

    private void initUI() {
        setTitle("AL Rehan Cafe");
        setSize(1180, 720);
        setMinimumSize(new Dimension(1000, 600));
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ── Sidebar ──
        JPanel sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);

        // ── Content area (CardLayout) ──
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(UIConstants.BG_LIGHT);

        dashboardPanel  = new DashboardPanel(currentUser);
        menuPanel       = new MenuPanel(currentUser);
        orderPanel      = new OrderPanel(currentUser);
        ordersListPanel = new OrdersListPanel(currentUser);

        // Wire dashboard refresh callback — fires when order is cancelled/completed/deleted
        ordersListPanel.setOnStatusChange(() -> dashboardPanel.refresh());

        // Wire dashboard refresh callback — fires when a new order is placed
        orderPanel.setOnOrderPlaced(() -> {
            dashboardPanel.refresh();
            ordersListPanel.loadData();
        });

        // Wire menu→order connection: when menu items change, refresh New Order list
        menuPanel.setOnMenuChanged(() -> orderPanel.refresh());

        contentPanel.add(dashboardPanel,  "Dashboard");
        contentPanel.add(menuPanel,       "Menu");
        contentPanel.add(orderPanel,      "NewOrder");
        contentPanel.add(ordersListPanel, "Orders");

        if (currentUser.isAdmin()) {
            userPanel = new UserManagementPanel(currentUser);
            contentPanel.add(userPanel, "Users");
        }

        add(contentPanel, BorderLayout.CENTER);

        cardLayout.show(contentPanel, "Dashboard");
        refreshNavStates();
        setVisible(true);

        // ── Keyboard shortcuts ──
        // Ctrl+F  /  F11  → toggle fullscreen
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
    }

    /** Helper: paint a vertical gradient on any panel */
    private static JPanel gradientPanel(Color top, Color bottom) {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bottom);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
    }

    private void refreshNavStates() {
        for (JButton b : navButtons) b.repaint();
    }

    private JPanel buildSidebar() {
        JPanel sidebar = gradientPanel(UIConstants.GRAD_TOP, UIConstants.GRAD_BOTTOM);
        sidebar.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));
        sidebar.setLayout(new BorderLayout());

        // ── Top brand area with logo ──
        JPanel brand = gradientPanel(UIConstants.GRAD_TOP, UIConstants.GRAD_BOTTOM);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(new EmptyBorder(4, 0, 6, 0));

        // ── Circular logo badge ──
        final BufferedImage[] logoImg = {null};
        try {
            InputStream is = MainFrame.class.getResourceAsStream("/cafe/logo.png");
            if (is != null) logoImg[0] = ImageIO.read(is);
        } catch (Exception ignored) {}

        final int LOGO_H = 160; // fixed height; width fills sidebar
        JPanel logoBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                if (logoImg[0] == null) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,   RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,  RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,      RenderingHints.VALUE_RENDER_QUALITY);
                // Draw logo centred, preserving aspect ratio, filling width
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
        logoBadge.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, LOGO_H));
        logoBadge.setMaximumSize(new Dimension(Integer.MAX_VALUE, LOGO_H));
        logoBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        brand.add(logoBadge);

        // ── Divider ──
        JPanel divider = new JPanel();
        divider.setOpaque(false);
        divider.setPreferredSize(new Dimension(0, 1));
        divider.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(255, 255, 255, 40)));

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setOpaque(false);
        topSection.add(brand, BorderLayout.CENTER);
        topSection.add(divider, BorderLayout.SOUTH);

        // ── Nav links ──
        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(new EmptyBorder(16, 0, 0, 0));

        addNavButton(nav, "Dashboard", "Dashboard");
        addNavButton(nav, "Menu Items", "Menu");
        addNavButton(nav, "New Order",  "NewOrder");
        addNavButton(nav, "All Orders", "Orders");
        if (currentUser.isAdmin()) {
            addNavButton(nav, "Users", "Users");
        }

        // ── Bottom logout ──
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(12, 16, 20, 16));

        JButton btnLogout = new JButton("Logout") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isRollover()
                        ? UIConstants.BTN_DANGER_TOP.brighter() : UIConstants.BTN_DANGER_TOP;
                Color bot = getModel().isRollover()
                        ? UIConstants.BTN_DANGER_BOT.brighter() : UIConstants.BTN_DANGER_BOT;
                GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bot);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnLogout.setFont(UIConstants.FONT_SMALL);
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setContentAreaFilled(false);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setOpaque(false);
        btnLogout.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLogout.setPreferredSize(new Dimension(0, 36));
        btnLogout.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                dispose();
                new LoginFrame();
            }
        });

        bottom.add(btnLogout, BorderLayout.SOUTH);

        sidebar.add(topSection, BorderLayout.NORTH);
        sidebar.add(nav, BorderLayout.CENTER);
        sidebar.add(bottom, BorderLayout.SOUTH);

        return sidebar;
    }

    private void addNavButton(JPanel nav, String label, String card) {
        final Color ACTIVE_TOP = new Color(0xA0734F);
        final Color ACTIVE_BOT = new Color(0x6B4228);
        final Color HOVER_TOP  = new Color(255, 255, 255, 40);
        final Color HOVER_BOT  = new Color(255, 255, 255, 10);

        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                boolean isActive = card.equals(activeCard);
                if (isActive) {
                    GradientPaint gp = new GradientPaint(0, 0, ACTIVE_TOP, 0, getHeight(), ACTIVE_BOT);
                    g2.setPaint(gp);
                    g2.fillRoundRect(6, 2, getWidth() - 12, getHeight() - 4, 12, 12);
                    g2.setColor(UIConstants.ACCENT);
                    g2.fillRoundRect(0, 4, 4, getHeight() - 8, 4, 4);
                } else if (getModel().isArmed() || getModel().isRollover()) {
                    GradientPaint gp = new GradientPaint(0, 0, HOVER_TOP, 0, getHeight(), HOVER_BOT);
                    g2.setPaint(gp);
                    g2.fillRoundRect(6, 2, getWidth() - 12, getHeight() - 4, 12, 12);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UIConstants.FONT_REGULAR);
        btn.setForeground(new Color(230, 210, 185));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setBorder(new EmptyBorder(10, 8, 10, 8));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            activeCard = card;
            cardLayout.show(contentPanel, card);
            refreshNavStates();
            if ("Dashboard".equals(card)) dashboardPanel.refresh();
            else if ("Menu".equals(card)) menuPanel.loadData();
            else if ("NewOrder".equals(card)) orderPanel.refresh();
            else if ("Orders".equals(card)) ordersListPanel.loadData();
            else if ("Users".equals(card) && userPanel != null) userPanel.loadData();
        });

        navButtons.add(btn);
        navCards.add(card);
        nav.add(btn);
        nav.add(Box.createVerticalStrut(2));
    }
}