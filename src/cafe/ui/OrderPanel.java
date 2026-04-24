package cafe.ui;

import cafe.dao.MenuDAO;
import cafe.dao.OrderDAO;
import cafe.models.MenuItem;
import cafe.models.User;
import cafe.utils.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListCellRenderer;

/**
 * OrderPanel - Browse menu, build cart, place order → prints black-and-white receipt.
 * Orders are saved as 'Completed' immediately on placement.
 */
public class OrderPanel extends JPanel {

    private User currentUser;
    private MenuDAO menuDAO;
    private Runnable onOrderPlaced;   // fired after a successful order (refresh dashboard)

    /** Optional callback invoked after an order is placed successfully. */
    public void setOnOrderPlaced(Runnable r) { this.onOrderPlaced = r; }
    private OrderDAO orderDAO;

    private JComboBox<String> cbCategory;
    private JTextField txtSearch;
    private JList<String> menuList;
    private DefaultListModel<String> menuListModel;
    private List<MenuItem> currentItems;

    // Cart table
    private DefaultTableModel cartModel;
    private JTable cartTable;
    private JLabel lblTotal;

    public OrderPanel(User user) {
        this.currentUser = user;
        this.menuDAO = new MenuDAO();
        this.orderDAO = new OrderDAO();
        initUI();
        loadCategories();
    }

    private void initUI() {
        setBackground(UIConstants.BG_LIGHT);
        setLayout(new BorderLayout(16, 16));
        setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);
        JLabel title = new JLabel("New Order");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_DARK);
        headerBar.add(title, BorderLayout.WEST);
        headerBar.add(UIConstants.createLiveClock(), BorderLayout.EAST);
        add(headerBar, BorderLayout.NORTH);

        // ── Left: menu browser ──
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBar.setOpaque(false);

        cbCategory = new JComboBox<>();
        cbCategory.setFont(UIConstants.FONT_REGULAR);
        cbCategory.addActionListener(e -> loadMenuForCategory());

        txtSearch = new JTextField(14);
        txtSearch.setFont(UIConstants.FONT_REGULAR);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                new EmptyBorder(5, 10, 5, 10)));
        JButton btnSearch = makeBtn("Search", UIConstants.BTN_PRIMARY_TOP, UIConstants.BTN_PRIMARY_BOT);
        btnSearch.addActionListener(e -> doMenuSearch());

        filterBar.add(new JLabel("Category:"));
        filterBar.add(cbCategory);
        filterBar.add(new JLabel("  Search:"));
        filterBar.add(txtSearch);
        filterBar.add(btnSearch);

        menuListModel = new DefaultListModel<>();
        menuList = new JList<>(menuListModel);
        menuList.setFont(UIConstants.FONT_REGULAR);
        menuList.setSelectionBackground(UIConstants.ACCENT);
        menuList.setSelectionForeground(Color.WHITE);
        menuList.setFixedCellHeight(36);
        menuList.setBackground(Color.WHITE);

        // Custom cell renderer for beautiful menu items
        menuList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JPanel cell = new JPanel(new BorderLayout(10, 0));

                if (index < currentItems.size()) {
                    MenuItem item = currentItems.get(index);

                    // Name + category inline
                    JLabel nameLabel = new JLabel(item.getName() + "  ");
                    nameLabel.setFont(UIConstants.FONT_BOLD);

                    JLabel catLabel = new JLabel("• " + item.getCategoryName());
                    catLabel.setFont(UIConstants.FONT_SMALL);

                    JPanel leftInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
                    leftInfo.setOpaque(false);
                    leftInfo.add(nameLabel);
                    leftInfo.add(catLabel);

                    JLabel priceLabel = new JLabel(String.format("Rs. %,.0f", item.getPrice()));
                    priceLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

                    if (isSelected) {
                        cell.setBackground(UIConstants.ACCENT);
                        nameLabel.setForeground(Color.WHITE);
                        priceLabel.setForeground(new Color(0xFFF3E0));
                        catLabel.setForeground(new Color(0xFFE0B2));
                    } else {
                        cell.setBackground(index % 2 == 0 ? UIConstants.TABLE_ROW1 : UIConstants.TABLE_ROW2);
                        nameLabel.setForeground(UIConstants.TEXT_DARK);
                        priceLabel.setForeground(UIConstants.PRIMARY);
                        catLabel.setForeground(UIConstants.TEXT_GRAY);
                    }

                    cell.add(leftInfo, BorderLayout.CENTER);
                    cell.add(priceLabel, BorderLayout.EAST);
                }

                // Bottom separator line
                cell.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_COLOR),
                        new EmptyBorder(4, 12, 4, 12)));

                return cell;
            }
        });

        JScrollPane menuScroll = new JScrollPane(menuList);
        menuScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR), "  Menu Items  "));

        // Bottom bar: quantity spinner + add to cart button
        JPanel bottomBar = new JPanel(new BorderLayout(8, 0));
        bottomBar.setOpaque(false);

        JPanel qtyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        qtyPanel.setOpaque(false);
        JLabel lblQty = new JLabel("Qty:");
        lblQty.setFont(UIConstants.FONT_BOLD);
        SpinnerNumberModel spinModel = new SpinnerNumberModel(1, 1, 100, 1);
        JSpinner spnQty = new JSpinner(spinModel);
        spnQty.setFont(UIConstants.FONT_REGULAR);
        spnQty.setPreferredSize(new Dimension(65, 36));
        qtyPanel.add(lblQty);
        qtyPanel.add(spnQty);

        JButton btnAddItem = makeBtn("  Add to Cart  ", UIConstants.BTN_SUCCESS_TOP, UIConstants.BTN_SUCCESS_BOT);
        btnAddItem.setFont(UIConstants.FONT_BOLD);
        btnAddItem.setPreferredSize(new Dimension(0, 42));
        btnAddItem.addActionListener(e -> {
            int qty = (int) spnQty.getValue();
            addToCart(qty);
            spnQty.setValue(1);
        });

        bottomBar.add(qtyPanel, BorderLayout.WEST);
        bottomBar.add(btnAddItem, BorderLayout.CENTER);

        left.add(filterBar, BorderLayout.NORTH);
        left.add(menuScroll, BorderLayout.CENTER);
        left.add(bottomBar, BorderLayout.SOUTH);

        // ── Right: cart + bill ──
        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(450, 0));

        // Cart table
        String[] cartCols = {"Item", "Qty", "Price", "Subtotal"};
        cartModel = new DefaultTableModel(cartCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 1; }
        };
        cartTable = new JTable(cartModel);
        styleCartTable();

        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR), "  Order Cart  "));

        JPanel billArea = buildBillArea();

        right.add(cartScroll, BorderLayout.CENTER);
        right.add(billArea, BorderLayout.SOUTH);

        add(left,  BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    /** Bottom bill panel: total label, centered Remove/Clear buttons, full-width Place Order. */
    private JPanel buildBillArea() {
        JPanel bill = new JPanel();
        bill.setLayout(new BoxLayout(bill, BoxLayout.Y_AXIS));
        bill.setBackground(Color.WHITE);
        bill.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                new EmptyBorder(14, 16, 14, 16)));

        // Total label
        lblTotal = new JLabel("Total:  Rs. 0");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTotal.setForeground(UIConstants.PRIMARY_DARK);
        lblTotal.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Centered utility buttons row ──
        JPanel utilRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        utilRow.setOpaque(false);
        utilRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        utilRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JButton btnRemove = makeBtn("Remove Selected", UIConstants.BTN_DANGER_TOP, UIConstants.BTN_DANGER_BOT);
        JButton btnClear  = makeBtn("Clear Cart",      UIConstants.BTN_GRAY_TOP,   UIConstants.BTN_GRAY_BOT);
        btnRemove.setPreferredSize(new Dimension(148, 34));
        btnClear.setPreferredSize(new Dimension(105, 34));
        btnRemove.addActionListener(e -> removeFromCart());
        btnClear.addActionListener(e -> clearCart());
        utilRow.add(btnRemove);
        utilRow.add(btnClear);

        // ── Full-width Place Order button ──
        JButton btnPlace = new JButton("PLACE ORDER") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color top = getModel().isRollover()
                        ? UIConstants.BTN_SUCCESS_TOP.brighter() : UIConstants.BTN_SUCCESS_TOP;
                Color bot = getModel().isRollover()
                        ? UIConstants.BTN_SUCCESS_BOT.brighter() : UIConstants.BTN_SUCCESS_BOT;
                GradientPaint gp = new GradientPaint(0, 0, top, 0, getHeight(), bot);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnPlace.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnPlace.setForeground(Color.WHITE);
        btnPlace.setContentAreaFilled(false);
        btnPlace.setBorderPainted(false);
        btnPlace.setFocusPainted(false);
        btnPlace.setOpaque(false);
        btnPlace.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnPlace.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnPlace.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPlace.addActionListener(e -> placeOrder());

        bill.add(lblTotal);
        bill.add(Box.createVerticalStrut(10));
        bill.add(utilRow);
        bill.add(Box.createVerticalStrut(10));
        bill.add(btnPlace);

        return bill;
    }

    // ── Data Loading ─────────────────────────────────────────────────────────

    public void refresh() {
        loadCategories();
    }

    private void loadCategories() {
        cbCategory.removeAllItems();
        cbCategory.addItem("All Items");
        for (String[] c : menuDAO.getAllCategories()) cbCategory.addItem(c[1]);
        loadMenuForCategory();
    }

    private void loadMenuForCategory() {
        int idx = cbCategory.getSelectedIndex();
        List<String[]> cats = menuDAO.getAllCategories();
        if (idx <= 0 || idx > cats.size()) {
            currentItems = menuDAO.getAllItems();
        } else {
            int catId = Integer.parseInt(cats.get(idx - 1)[0]);
            currentItems = menuDAO.getItemsByCategory(catId);
        }
        refreshMenuList(currentItems);
    }

    private void doMenuSearch() {
        String kw = txtSearch.getText().trim();
        if (kw.isEmpty()) { loadMenuForCategory(); return; }
        currentItems = menuDAO.searchItems(kw);
        refreshMenuList(currentItems);
    }

    private void refreshMenuList(List<MenuItem> items) {
        menuListModel.clear();
        for (MenuItem m : items)
            menuListModel.addElement(String.format("%-28s Rs. %,.0f", m.getName(), m.getPrice()));
    }

    // ── Cart Operations ───────────────────────────────────────────────────────

    private void addToCart(int addQty) {
        int idx = menuList.getSelectedIndex();
        if (idx < 0 || idx >= currentItems.size()) {
            JOptionPane.showMessageDialog(this, "Please select a menu item first."); return;
        }
        MenuItem item = currentItems.get(idx);
        for (int r = 0; r < cartModel.getRowCount(); r++) {
            if (cartModel.getValueAt(r, 0).equals(item.getName())) {
                int qty = (int) cartModel.getValueAt(r, 1) + addQty;
                cartModel.setValueAt(qty, r, 1);
                cartModel.setValueAt(String.format("%.0f", qty * item.getPrice()), r, 3);
                recalcTotal(); return;
            }
        }
        cartModel.addRow(new Object[]{
            item.getName(), addQty,
            String.format("%.0f", item.getPrice()),
            String.format("%.0f", addQty * item.getPrice())
        });
        recalcTotal();
    }

    private void removeFromCart() {
        int row = cartTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a cart item first."); return; }
        cartModel.removeRow(row);
        recalcTotal();
    }

    private void clearCart() {
        cartModel.setRowCount(0);
        lblTotal.setText("Total:  Rs. 0");
    }

    private void recalcTotal() {
        double total = 0;
        for (int r = 0; r < cartModel.getRowCount(); r++)
            total += Double.parseDouble(cartModel.getValueAt(r, 3).toString());
        lblTotal.setText(String.format("Total:  Rs. %,.0f", total));
    }

    // ── Place Order ───────────────────────────────────────────────────────────

    private void placeOrder() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Cart is empty — please add items first.",
                    "Empty Cart", JOptionPane.WARNING_MESSAGE); return;
        }
        // Snapshot cart BEFORE clearing
        int rows = cartModel.getRowCount();
        List<String[]> snapshot = new ArrayList<>();
        double total = 0;
        for (int r = 0; r < rows; r++) {
            String name = (String) cartModel.getValueAt(r, 0);
            
            int qty;
            Object qObj = cartModel.getValueAt(r, 1);
            if (qObj instanceof Integer) {
                qty = (Integer) qObj;
            } else {
                try {
                    qty = Integer.parseInt(qObj.toString());
                } catch(NumberFormatException ex) {
                    qty = 1;
                }
            }
            
            double sub  = Double.parseDouble(cartModel.getValueAt(r, 3).toString());
            snapshot.add(new String[]{name, String.valueOf(qty), String.format("%.0f", sub)});
            total += sub;
        }

        // Build items array for DAO
        Object[][] items = new Object[rows][3];
        List<MenuItem> allItems = menuDAO.getAllItems();
        for (int r = 0; r < rows; r++) {
            String itemName  = snapshot.get(r)[0];
            int qty          = Integer.parseInt(snapshot.get(r)[1]);
            double price     = Double.parseDouble(cartModel.getValueAt(r, 2).toString());
            int menuItemId   = -1;
            for (MenuItem m : allItems) { if (m.getName().equals(itemName)) { menuItemId = m.getId(); break; } }
            
            if (menuItemId == -1) {
                JOptionPane.showMessageDialog(this, "Item '" + itemName + "' is no longer available in the menu. Please clear it from your cart.", "Menu Updated", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            items[r] = new Object[]{menuItemId, qty, price};
        }

        int orderId = orderDAO.placeOrder("-", currentUser.getId(), items);
        if (orderId > 0) {
            clearCart();
            showReceipt(orderId, snapshot, total);
            if (onOrderPlaced != null) onOrderPlaced.run();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to save order. Please try again.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Receipt (black-and-white thermal style) ───────────────────────────────

    private void showReceipt(int orderId, List<String[]> snapshot, double total) {

        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm:ss"));
        int WIDTH = 38;

        StringBuilder sb = new StringBuilder();
        sb.append(center("================================", WIDTH)).append("\n");
        sb.append(center("     AL REHAN CAFE", WIDTH)).append("\n");
        sb.append(center("================================", WIDTH)).append("\n");
        sb.append(String.format("Order # : %d%n", orderId));
        sb.append(String.format("Date    : %s%n", time));
        sb.append(String.format("Cashier : %s%n", currentUser.getFullName()));
        sb.append("--------------------------------\n");
        sb.append(String.format("%-20s %4s  %7s%n", "Item", "Qty", "Amount"));
        sb.append("--------------------------------\n");

        for (String[] row : snapshot) {
            String name = row[0];
            int    qty  = Integer.parseInt(row[1]);
            double sub  = Double.parseDouble(row[2]);
            // wrap long names
            if (name.length() > 20) name = name.substring(0, 18) + "..";
            sb.append(String.format("%-20s %4d  Rs%,.0f%n", name, qty, sub));
        }

        sb.append("================================\n");
        sb.append(String.format("TOTAL           Rs. %,.0f%n", total));
        sb.append("================================\n");
        sb.append(center("  Thank you! Come again!  ", WIDTH)).append("\n");
        sb.append(center("================================", WIDTH)).append("\n");

        // Plain monospaced text area — true thermal receipt style
        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Monospaced", Font.PLAIN, 13));
        ta.setEditable(false);
        ta.setBackground(Color.WHITE);
        ta.setForeground(Color.BLACK);
        ta.setBorder(new EmptyBorder(12, 16, 12, 16));

        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(370, 340));
        sp.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JOptionPane.showMessageDialog(
                this, sp,
                "Receipt — Order #" + orderId,
                JOptionPane.PLAIN_MESSAGE);
    }

    private String center(String s, int width) {
        int pad = Math.max(0, (width - s.length()) / 2);
        return " ".repeat(pad) + s;
    }

    // ── Table Styling ─────────────────────────────────────────────────────────

    private void styleCartTable() {
        cartTable.setFont(UIConstants.FONT_REGULAR);
        cartTable.setRowHeight(32);
        cartTable.setShowGrid(false);
        cartTable.setSelectionBackground(UIConstants.ACCENT);
        cartTable.setSelectionForeground(Color.WHITE);

        UIConstants.applyGradientHeader(cartTable);

        cartTable.getColumnModel().getColumn(0).setPreferredWidth(160);
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        cartTable.getColumnModel().getColumn(1).setMaxWidth(55);
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        cartTable.getColumnModel().getColumn(2).setMaxWidth(90);
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        cartTable.getColumnModel().getColumn(3).setMaxWidth(110);

        cartTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) setBackground(r % 2 == 0 ? UIConstants.TABLE_ROW1 : UIConstants.TABLE_ROW2);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        cartModel.addTableModelListener(e -> {
            if (e.getColumn() == 1 && e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                try {
                    int qty   = Integer.parseInt(cartModel.getValueAt(row, 1).toString());
                    double px = Double.parseDouble(cartModel.getValueAt(row, 2).toString());
                    if (qty < 1) { cartModel.setValueAt(1, row, 1); return; }
                    cartModel.setValueAt(String.format("%.0f", qty * px), row, 3);
                    recalcTotal();
                } catch (NumberFormatException ignored) {}
            }
        });
    }

    // ── Button Factory ────────────────────────────────────────────────────────

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
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}