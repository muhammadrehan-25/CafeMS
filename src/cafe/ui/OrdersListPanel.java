package cafe.ui;

import cafe.dao.OrderDAO;
import cafe.models.User;
import cafe.utils.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * OrdersListPanel - Displays all orders with filter by status.
 * Admins can update order status.
 */
public class OrdersListPanel extends JPanel {

    private User currentUser;
    private OrderDAO orderDAO;
    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> cbStatusFilter;
    private Runnable onStatusChange;   // called after any status update

    /** Optional callback invoked after status changes (e.g., to refresh dashboard). */
    public void setOnStatusChange(Runnable r) { this.onStatusChange = r; }

    public OrdersListPanel(User user) {
        this.currentUser = user;
        this.orderDAO = new OrderDAO();
        initUI();
        loadData();
    }

    private void initUI() {
        setBackground(UIConstants.BG_LIGHT);
        setLayout(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);
        JLabel title = new JLabel("Order History");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_DARK);
        headerBar.add(title, BorderLayout.WEST);
        headerBar.add(UIConstants.createLiveClock(), BorderLayout.EAST);

        // ── Filter bar ──
        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        filterBar.setOpaque(false);

        cbStatusFilter = new JComboBox<>(new String[]{"All", "Pending", "Completed", "Cancelled"});
        cbStatusFilter.setFont(UIConstants.FONT_REGULAR);
        JButton btnFilter = makeBtn("Filter", UIConstants.BTN_PRIMARY_TOP, UIConstants.BTN_PRIMARY_BOT);
        btnFilter.addActionListener(e -> loadData());

        JButton btnRefresh = makeBtn("Refresh", UIConstants.BTN_GRAY_TOP, UIConstants.BTN_GRAY_BOT);
        btnRefresh.addActionListener(e -> loadData());

        filterBar.add(new JLabel("Status:"));
        filterBar.add(cbStatusFilter);
        filterBar.add(btnFilter);
        filterBar.add(btnRefresh);

        JButton btnViewDetail = makeBtn("View Items", UIConstants.BTN_ACCENT_TOP, UIConstants.BTN_ACCENT_BOT);
        btnViewDetail.addActionListener(e -> viewOrderDetails());
        filterBar.add(btnViewDetail);

        // Cancel + Delete visible to ALL users (Admin and User)
        filterBar.add(Box.createHorizontalStrut(10));

        JButton btnCancel = makeBtn("Mark Cancelled", UIConstants.BTN_DANGER_TOP, UIConstants.BTN_DANGER_BOT);
        btnCancel.addActionListener(e -> updateStatus("Cancelled"));
        filterBar.add(btnCancel);

        JButton btnDelete = makeBtn("Delete Order", new Color(0x8B0000), new Color(0x5C0000));
        btnDelete.addActionListener(e -> deleteOrder());
        filterBar.add(btnDelete);

        // Export CSV available to all
        JButton btnExport = makeBtn("Export CSV", UIConstants.BTN_SUCCESS_TOP, UIConstants.BTN_SUCCESS_BOT);
        btnExport.addActionListener(e -> exportCSV());
        filterBar.add(Box.createHorizontalStrut(10));
        filterBar.add(btnExport);

        // ── Table ──
        String[] cols = {"#ID", "Table", "Served By", "Total (Rs.)", "Status", "Date & Time"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override
            public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
                String firstCol = getValueAt(rowIndex, 0) != null ? getValueAt(rowIndex, 0).toString() : "";
                if (firstCol.startsWith("DATE_SEP::")) return; // Prevent selecting date rows
                super.changeSelection(rowIndex, columnIndex, toggle, extend);
            }
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                for (int row = 0; row < getRowCount(); row++) {
                    String firstCol = getValueAt(row, 0) != null ? getValueAt(row, 0).toString() : "";
                    if (firstCol.startsWith("DATE_SEP::")) {
                        Rectangle r = getCellRect(row, 0, true);
                        r.width = getWidth(); // Span full table width
                        
                        // Apply brown gradient
                        Color c1 = new Color(0x8D5524); // Warm brown
                        Color c2 = new Color(0x4A2E1B); // Dark brown
                        GradientPaint gp = new GradientPaint(0, r.y, c1, r.width, r.y, c2);
                        g2.setPaint(gp);
                        g2.fillRect(0, r.y, r.width, r.height);
                        
                        // Draw header text
                        g2.setColor(new Color(0xFFF3E0)); // Very light orange/cream
                        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
                        String text = firstCol.replace("DATE_SEP::", "");
                        FontMetrics fm = g2.getFontMetrics();
                        int y = r.y + ((r.height - fm.getHeight()) / 2) + fm.getAscent();
                        g2.drawString(text, 16, y); // 16px left padding
                    }
                }
                g2.dispose();
            }
        };
        table.setRowHeight(32);
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));

        add(filterBar, BorderLayout.CENTER);
        add(scroll, BorderLayout.SOUTH);

        // Make scroll fill remaining space
        setLayout(new BorderLayout(0, 12));
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(headerBar, BorderLayout.NORTH);
        top.add(filterBar, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    public void loadData() {
        String status = (String) cbStatusFilter.getSelectedItem();
        List<Object[]> orders = "All".equals(status)
                ? orderDAO.getAllOrders()
                : orderDAO.getOrdersByStatus(status);
        tableModel.setRowCount(0);

        String lastDate = "";
        for (Object[] row : orders) {
            // row[5] is "hh:mm AM  dd/MM/yyyy" — extract date part
            String dateTime = row[5] != null ? row[5].toString() : "";
            String datePart = dateTime.contains("  ") ? dateTime.split("  ")[1].trim() : "";

            // Convert dd/MM/yyyy to friendly label
            String dateLabel = datePart;
            try {
                DateTimeFormatter inFmt  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter outFmt = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy");
                LocalDate d = LocalDate.parse(datePart, inFmt);
                LocalDate today = LocalDate.now();
                if (d.equals(today))            dateLabel = "Today — " + d.format(outFmt);
                else if (d.equals(today.minusDays(1))) dateLabel = "Yesterday — " + d.format(outFmt);
                else                            dateLabel = d.format(outFmt);
            } catch (Exception ignored) {}

            // Insert a separator row when date changes
            if (!datePart.isEmpty() && !datePart.equals(lastDate)) {
                tableModel.addRow(new Object[]{ "DATE_SEP::" + dateLabel, "", "", "", "", "" });
                lastDate = datePart;
            }

            tableModel.addRow(new Object[]{
                row[0], row[1], row[2],
                String.format("%.0f", row[3]),
                row[4], dateTime.contains("  ") ? dateTime.split("  ")[0].trim() : dateTime
            });
        }
    }
    private void updateStatus(String newStatus) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an order first."); return; }
        int orderId = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Mark Order #" + orderId + " as " + newStatus + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (orderDAO.updateStatus(orderId, newStatus)) {
                loadData();
                if (onStatusChange != null) onStatusChange.run(); // refresh dashboard
            }
        }
    }

    private void deleteOrder() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an order first."); return; }
        int orderId = (int) tableModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Permanently DELETE Order #" + orderId + "?\nThis cannot be undone!",
                "Delete Order", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            if (orderDAO.deleteOrder(orderId)) {
                JOptionPane.showMessageDialog(this, "Order #" + orderId + " deleted successfully.");
                loadData();
                if (onStatusChange != null) onStatusChange.run();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete order.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Orders Report");
        chooser.setSelectedFile(new File("AL_Rehan_Cafe_Orders_Report.csv"));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().endsWith(".csv")) file = new File(file.getAbsolutePath() + ".csv");

        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            // Header
            pw.println("Order ID,Served By,Total (Rs.),Status,Date & Time");

            // Data — use whatever is currently in the table (respects filters)
            for (int r = 0; r < tableModel.getRowCount(); r++) {
                pw.printf("%s,\"%s\",%s,%s,\"%s\"%n",
                        tableModel.getValueAt(r, 0),
                        tableModel.getValueAt(r, 2),  // Served By (col 1 is hidden Table)
                        tableModel.getValueAt(r, 3),
                        tableModel.getValueAt(r, 4),
                        tableModel.getValueAt(r, 5));
            }

            JOptionPane.showMessageDialog(this,
                    "Report exported successfully!\n" + file.getAbsolutePath(),
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);

            // Open the file location
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file.getParentFile());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewOrderDetails() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an order first."); return; }
        int orderId = (int) tableModel.getValueAt(row, 0);
        String tableNo = (String) tableModel.getValueAt(row, 1);

        List<Object[]> items = orderDAO.getOrderItems(orderId);
        if (items.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items found for this order.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Order #").append(orderId).append(" | Table: ").append(tableNo).append("\n\n");
        sb.append(String.format("%-26s %5s %10s %12s%n", "Item", "Qty", "Price", "Subtotal"));
        sb.append("─────────────────────────────────────────────────\n");
        double total = 0;
        for (Object[] item : items) {
            sb.append(String.format("%-26s %5s %10.0f %12.0f%n",
                    item[0], item[1], item[2], item[3]));
            total += (double) item[3];
        }
        sb.append("─────────────────────────────────────────────────\n");
        sb.append(String.format("%-26s %5s %10s %12.0f%n", "TOTAL", "", "", total));

        JTextArea area = new JTextArea(sb.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Order Details - #" + orderId, JOptionPane.INFORMATION_MESSAGE);
    }

    private void styleTable() {
        table.setFont(UIConstants.FONT_REGULAR);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(UIConstants.ACCENT);
        table.setSelectionForeground(Color.WHITE);

        UIConstants.applyGradientHeader(table);

        table.getColumnModel().getColumn(0).setMaxWidth(50);
        // Hide Table column — no table numbers in this cafe
        javax.swing.table.TableColumn tableCol = table.getColumnModel().getColumn(1);
        tableCol.setMinWidth(0); tableCol.setMaxWidth(0); tableCol.setPreferredWidth(0); tableCol.setResizable(false);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(180);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                String firstCol = t.getValueAt(r, 0) != null ? t.getValueAt(r, 0).toString() : "";
                if (firstCol.startsWith("DATE_SEP::")) {
                    setText(""); // Clear text to avoid peeking under the gradient
                    setBackground(Color.WHITE);
                    return this;
                }
                
                if (!s) {
                    setBackground(r % 2 == 0 ? UIConstants.TABLE_ROW1 : UIConstants.TABLE_ROW2);
                    if (c == 4) {
                        String status = v != null ? v.toString() : "";
                        if ("Completed".equals(status)) setForeground(UIConstants.SUCCESS);
                        else if ("Cancelled".equals(status)) setForeground(UIConstants.DANGER);
                        else setForeground(UIConstants.WARNING);
                    } else {
                        setForeground(UIConstants.TEXT_DARK);
                    }
                }
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
    }

    private JButton makeBtn(String text, Color topColor, Color botColor) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color t = getModel().isRollover() ? topColor.brighter() : topColor;
                Color bot = getModel().isRollover() ? botColor.brighter() : botColor;
                GradientPaint gp = new GradientPaint(0, 0, t, 0, getHeight(), bot);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(UIConstants.FONT_REGULAR);
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
