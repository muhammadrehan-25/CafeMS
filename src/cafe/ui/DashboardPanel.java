package cafe.ui;

import cafe.dao.OrderDAO;
import cafe.models.User;
import cafe.utils.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * DashboardPanel - Shows welcome header, live stat cards, recent orders,
 * and a 7-day revenue bar chart. Auto-refreshes every 30 seconds.
 */
public class DashboardPanel extends JPanel {

    private User user;
    private OrderDAO orderDAO;

    // Stat labels
    private JLabel lblRevenue, lblOrders, lblToday, lblAvg;

    // Recent orders table
    private DefaultTableModel recentTableModel;

    // 7-day chart state
    private String[] chartDays   = new String[7];
    private double[] chartValues = new double[7];
    private JPanel   chartPanel;

    // Auto-refresh timer (30 s)
    private Timer autoRefresh;

    public DashboardPanel(User user) {
        this.user     = user;
        this.orderDAO = new OrderDAO();
        initUI();
        refresh();

        autoRefresh = new Timer(30_000, e -> refresh());
        autoRefresh.setRepeats(true);
        autoRefresh.start();
    }

    // ── UI construction ──────────────────────────────────────────────────────

    private void initUI() {
        setBackground(UIConstants.BG_LIGHT);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(28, 28, 28, 28));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // 4 stat cards
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 18, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(24, 0, 20, 0));
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        lblRevenue = new JLabel("Rs. 0");
        lblOrders  = new JLabel("0");
        lblToday   = new JLabel("Rs. 0");
        lblAvg     = new JLabel("Rs. 0");

        statsRow.add(buildStatCard("Total Revenue",    lblRevenue, UIConstants.SUCCESS));
        statsRow.add(buildStatCard("Today's Revenue",  lblToday,   UIConstants.WARNING));
        statsRow.add(buildStatCard("Completed Orders", lblOrders,  UIConstants.PRIMARY));
        statsRow.add(buildStatCard("Avg. Order Value", lblAvg,     UIConstants.DANGER));

        center.add(statsRow);

        // Bottom row: Recent Orders + 7-day Chart
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 18, 0));
        bottomRow.setOpaque(false);
        bottomRow.add(buildRecentOrdersPanel());
        bottomRow.add(buildChartPanel());

        center.add(bottomRow);
        add(center, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel lblGreeting = new JLabel("Welcome back, " + user.getFullName() + "!");
        lblGreeting.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblGreeting.setForeground(UIConstants.PRIMARY_DARK);

        String timeStr = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy"));
        JLabel lblDate = new JLabel(timeStr);
        lblDate.setFont(UIConstants.FONT_REGULAR);
        lblDate.setForeground(UIConstants.TEXT_GRAY);

        JPanel titleBlock = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBlock.setOpaque(false);
        titleBlock.add(lblGreeting);
        titleBlock.add(lblDate);
        header.add(titleBlock, BorderLayout.WEST);

        // Role badge + live clock top-right
        JLabel lblRole = new JLabel("  " + user.getRole() + "  ");
        lblRole.setFont(UIConstants.FONT_SMALL);
        lblRole.setForeground(Color.WHITE);
        lblRole.setOpaque(true);
        lblRole.setBackground(user.isAdmin() ? UIConstants.PRIMARY : UIConstants.SUCCESS);
        lblRole.setBorder(new EmptyBorder(5, 12, 5, 12));

        JLabel clock = UIConstants.createLiveClock();

        JPanel roleWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        roleWrap.setOpaque(false);
        roleWrap.add(clock);
        roleWrap.add(lblRole);
        header.add(roleWrap, BorderLayout.EAST);

        return header;
    }

    // ── Stat card ────────────────────────────────────────────────────────────

    private JPanel buildStatCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(accent);
                g2.fillRoundRect(0, getHeight() - 8, getWidth(), 8, 4, 4);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 22, 16, 22));

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(UIConstants.FONT_REGULAR);
        lTitle.setForeground(UIConstants.TEXT_GRAY);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(UIConstants.TEXT_DARK);

        card.add(lTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    // ── Recent Orders table ───────────────────────────────────────────────────

    private JPanel buildRecentOrdersPanel() {
        JPanel card = buildRoundedCard();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Recent Orders");
        title.setFont(UIConstants.FONT_BOLD);
        title.setForeground(UIConstants.PRIMARY_DARK);
        card.add(title, BorderLayout.NORTH);

        String[] cols = {"#", "Items", "Amount", "Status", "Time"};
        recentTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(recentTableModel);
        table.setFont(UIConstants.FONT_SMALL);
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(0xFFF3E0));
        table.setSelectionForeground(UIConstants.TEXT_DARK);

        UIConstants.applyGradientHeader(table);

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(35);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(3).setPreferredWidth(75);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);

        // Status column – coloured text
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                String s = val == null ? "" : val.toString();
                if (s.equals("Completed"))      setForeground(UIConstants.SUCCESS);
                else if (s.equals("Cancelled")) setForeground(UIConstants.DANGER);
                else                            setForeground(UIConstants.WARNING);
                setBackground(row % 2 == 0 ? UIConstants.TABLE_ROW1 : UIConstants.TABLE_ROW2);
                setFont(UIConstants.FONT_SMALL);
                setBorder(new EmptyBorder(0, 6, 0, 0));
                return this;
            }
        });

        // Alternating row renderer for other columns
        DefaultTableCellRenderer altRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setBackground(row % 2 == 0 ? UIConstants.TABLE_ROW1 : UIConstants.TABLE_ROW2);
                setFont(UIConstants.FONT_SMALL);
                setBorder(new EmptyBorder(0, 6, 0, 0));
                return this;
            }
        };
        for (int c : new int[]{0, 1, 2, 4}) {
            table.getColumnModel().getColumn(c).setCellRenderer(altRenderer);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR, 1));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── 7-day revenue chart ───────────────────────────────────────────────────

    private JPanel buildChartPanel() {
        JPanel card = buildRoundedCard();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel title = new JLabel("Revenue \u2014 Last 7 Days");
        title.setFont(UIConstants.FONT_BOLD);
        title.setForeground(UIConstants.PRIMARY_DARK);
        card.add(title, BorderLayout.NORTH);

        chartPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setBackground(Color.WHITE);
                drawBarChart((Graphics2D) g);
            }
        };
        chartPanel.setOpaque(false);
        card.add(chartPanel, BorderLayout.CENTER);
        return card;
    }

    private void drawBarChart(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = chartPanel.getWidth();
        int h = chartPanel.getHeight();
        if (w < 20 || h < 20) return;

        int padL = 58, padR = 12, padT = 14, padB = 36;
        int chartW = w - padL - padR;
        int chartH = h - padT - padB;

        double maxVal = 1;
        for (double v : chartValues) if (v > maxVal) maxVal = v;

        // Grid lines + Y labels
        int gridLines = 4;
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        for (int i = 0; i <= gridLines; i++) {
            int y = padT + chartH - (int) ((double) i / gridLines * chartH);
            g2.setColor(new Color(0xE8DDD4));
            g2.setStroke(new BasicStroke(0.7f, BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER, 1f, new float[]{3, 3}, 0));
            g2.drawLine(padL, y, padL + chartW, y);
            g2.setColor(UIConstants.TEXT_GRAY);
            g2.setStroke(new BasicStroke(1f));
            String lbl = i == 0 ? "0" : String.format("%.0f", maxVal * i / gridLines);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(lbl, padL - fm.stringWidth(lbl) - 4, y + 4);
        }

        // Bars
        int n   = 7;
        int barW = Math.max(10, (chartW / n) - 12);
        int gap  = (chartW - barW * n) / (n + 1);

        for (int i = 0; i < n; i++) {
            int barH = chartValues[i] <= 0 ? 2
                    : Math.max(2, (int) (chartValues[i] / maxVal * chartH));
            int x    = padL + gap + i * (barW + gap);
            int y    = padT + chartH - barH;

            GradientPaint gp = new GradientPaint(
                    x, y,    new Color(0xA07040),
                    x, padT + chartH, new Color(0x5C3D20));
            g2.setPaint(gp);
            g2.fillRoundRect(x, y, barW, barH, 5, 5);

            // Day label
            g2.setColor(UIConstants.TEXT_GRAY);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            FontMetrics fm = g2.getFontMetrics();
            String day = chartDays[i] == null ? "" : chartDays[i];
            g2.drawString(day, x + (barW - fm.stringWidth(day)) / 2,
                    padT + chartH + 18);

            // Rs value on top of bar (only if tall enough)
            if (barH > 22 && chartValues[i] > 0) {
                g2.setFont(new Font("Segoe UI", Font.BOLD, 9));
                g2.setColor(Color.WHITE);
                String val = String.format("%.0f", chartValues[i]);
                fm = g2.getFontMetrics();
                g2.drawString(val, x + (barW - fm.stringWidth(val)) / 2, y + 13);
            }
        }

        // X-axis line
        g2.setColor(new Color(0xC8B8A8));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawLine(padL, padT + chartH, padL + chartW, padT + chartH);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private JPanel buildRoundedCard() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            }
        };
    }

    // ── Public refresh ────────────────────────────────────────────────────────

    /**
     * Refreshes all dashboard data from the database.
     * Called on: nav to dashboard, order placement, order cancellation, timer tick.
     */
    public void refresh() {
        // 1. Summary stats: [totalRevenue, completedOrders, todayRevenue, avgOrderValue]
        double[] stats = orderDAO.getSummaryStats();
        lblRevenue.setText(String.format("Rs. %,.0f", stats[0]));
        lblOrders .setText(String.valueOf((int) stats[1]));
        lblToday  .setText(String.format("Rs. %,.0f", stats[2]));
        lblAvg    .setText(stats[3] > 0
                ? String.format("Rs. %,.0f", stats[3]) : "Rs. 0");

        // 2. Recent orders (latest 6)
        recentTableModel.setRowCount(0);
        List<Object[]> recent = orderDAO.getRecentOrders(6);
        for (Object[] row : recent) {
            recentTableModel.addRow(new Object[]{
                "#" + row[0],
                row[1],
                String.format("Rs. %,.0f", (double) row[2]),
                row[3],
                row[4]
            });
        }

        // 3. 7-day revenue data
        Object[][] days7 = orderDAO.getLast7DaysRevenue();
        for (int i = 0; i < 7; i++) {
            chartDays[i]   = days7[i][0].toString();
            chartValues[i] = (double) days7[i][1];
        }
        if (chartPanel != null) chartPanel.repaint();
    }
}