package cafe.ui;

import cafe.dao.MenuDAO;
import cafe.models.MenuItem;
import cafe.models.User;
import cafe.utils.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * MenuPanel - Full CRUD interface for menu items.
 * Admins can Add, Edit, Delete items. Both roles can search/filter.
 */
public class MenuPanel extends JPanel {

    private Runnable onMenuChanged; // fired when menu items are added/updated/deleted
    public void setOnMenuChanged(Runnable r) { this.onMenuChanged = r; }

    private User currentUser;
    private MenuDAO menuDAO;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;
    private JComboBox<String> cbCategory;
    private List<String[]> categories;

    // Form fields
    private JTextField txtName, txtPrice, txtDesc;
    private JComboBox<String> cbFormCategory, cbAvailable;
    private JButton btnSave, btnClear, btnDelete;
    private int editingId = -1;

    public MenuPanel(User user) {
        this.currentUser = user;
        this.menuDAO = new MenuDAO();
        initUI();
        loadData();
    }

    private void initUI() {
        setBackground(UIConstants.BG_LIGHT);
        setLayout(new BorderLayout(16, 16));
        setBorder(new EmptyBorder(24, 24, 24, 24));

        // ── Page title + clock ──
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);
        JLabel title = new JLabel("Menu Management");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_DARK);
        headerBar.add(title, BorderLayout.WEST);
        headerBar.add(UIConstants.createLiveClock(), BorderLayout.EAST);
        add(headerBar, BorderLayout.NORTH);

        // ── Left: table + search ──
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);

        // Search + filter bar
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setOpaque(false);

        txtSearch = new JTextField(16);
        txtSearch.setFont(UIConstants.FONT_REGULAR);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                new EmptyBorder(6, 10, 6, 10)));

        cbCategory = new JComboBox<>();
        cbCategory.setFont(UIConstants.FONT_REGULAR);
        cbCategory.addItem("All Categories");

        JButton btnSearch = makeButton("Search", UIConstants.BTN_PRIMARY_TOP, UIConstants.BTN_PRIMARY_BOT);
        btnSearch.addActionListener(e -> doSearch());

        JButton btnRefresh = makeButton("Refresh", UIConstants.BTN_GRAY_TOP, UIConstants.BTN_GRAY_BOT);
        btnRefresh.addActionListener(e -> loadData());

        searchBar.add(new JLabel("Search:"));
        searchBar.add(txtSearch);
        searchBar.add(new JLabel("Category:"));
        searchBar.add(cbCategory);
        searchBar.add(btnSearch);
        searchBar.add(btnRefresh);

        // Table
        String[] cols = {"ID", "Name", "Category", "Price (Rs.)", "Description", "Available"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(3).setMaxWidth(100);
        table.getColumnModel().getColumn(5).setMaxWidth(80);

        // Row click -> populate form
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) populateForm();
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));

        left.add(searchBar, BorderLayout.NORTH);
        left.add(scroll, BorderLayout.CENTER);

        // ── Right: form (Admin only) ──
        JPanel right = buildFormPanel();
        right.setPreferredSize(new Dimension(280, 0));

        add(left, BorderLayout.CENTER);
        if (currentUser.isAdmin()) {
            add(right, BorderLayout.EAST);
        }
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                new EmptyBorder(20, 16, 20, 16)));

        JLabel formTitle = new JLabel("Item Details");
        formTitle.setFont(UIConstants.FONT_HEADING);
        formTitle.setForeground(UIConstants.PRIMARY_DARK);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtName  = formField("Item Name *");
        txtPrice = formField("Price (Rs.) *");
        txtDesc  = formField("Description");

        cbFormCategory = new JComboBox<>();
        cbFormCategory.setFont(UIConstants.FONT_REGULAR);
        cbFormCategory.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbFormCategory.setAlignmentX(Component.LEFT_ALIGNMENT);

        cbAvailable = new JComboBox<>(new String[]{"Available", "Unavailable"});
        cbAvailable.setFont(UIConstants.FONT_REGULAR);
        cbAvailable.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbAvailable.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnSave   = makeButton("Save Item", UIConstants.BTN_SUCCESS_TOP, UIConstants.BTN_SUCCESS_BOT);
        btnClear  = makeButton("Clear",     UIConstants.BTN_GRAY_TOP,    UIConstants.BTN_GRAY_BOT);
        btnDelete = makeButton("Delete",    UIConstants.BTN_DANGER_TOP,  UIConstants.BTN_DANGER_BOT);

        btnSave.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnClear.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnDelete.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnSave.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnClear.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnDelete.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnSave.addActionListener(e -> saveItem());
        btnClear.addActionListener(e -> clearForm());
        btnDelete.addActionListener(e -> deleteItem());

        panel.add(formTitle);
        panel.add(Box.createVerticalStrut(14));
        panel.add(new JLabel("Name:")); panel.add(txtName); panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Category:")); panel.add(cbFormCategory); panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Price (Rs.):")); panel.add(txtPrice); panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Description:")); panel.add(txtDesc); panel.add(Box.createVerticalStrut(8));
        panel.add(new JLabel("Status:")); panel.add(cbAvailable); panel.add(Box.createVerticalStrut(16));
        panel.add(btnSave); panel.add(Box.createVerticalStrut(6));
        panel.add(btnDelete); panel.add(Box.createVerticalStrut(6));
        panel.add(btnClear);

        return panel;
    }

    public void loadData() {
        categories = menuDAO.getAllCategories();
        cbCategory.removeAllItems();
        cbCategory.addItem("All Categories");
        if (cbFormCategory != null) cbFormCategory.removeAllItems();
        for (String[] c : categories) {
            cbCategory.addItem(c[1]);
            if (cbFormCategory != null) cbFormCategory.addItem(c[1]);
        }
        refreshTable(menuDAO.getAllItems());
    }

    private void refreshTable(List<MenuItem> items) {
        tableModel.setRowCount(0);
        for (MenuItem m : items) {
            tableModel.addRow(new Object[]{
                m.getId(), m.getName(), m.getCategoryName(),
                String.format("%.0f", m.getPrice()), m.getDescription(),
                m.isAvailable() ? "Yes" : "No"
            });
        }
    }

    private void doSearch() {
        String kw = txtSearch.getText().trim();
        int catIdx = cbCategory.getSelectedIndex();

        List<MenuItem> results;
        if (!kw.isEmpty()) {
            results = menuDAO.searchItems(kw);
        } else if (catIdx > 0 && catIdx <= categories.size()) {
            int catId = Integer.parseInt(categories.get(catIdx - 1)[0]);
            results = menuDAO.getItemsByCategory(catId);
        } else {
            results = menuDAO.getAllItems();
        }
        refreshTable(results);
    }

    private void populateForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        editingId = (int) tableModel.getValueAt(row, 0);
        txtName.setText((String) tableModel.getValueAt(row, 1));
        String catName = (String) tableModel.getValueAt(row, 2);
        for (int i = 0; i < cbFormCategory.getItemCount(); i++) {
            if (cbFormCategory.getItemAt(i).equals(catName)) {
                cbFormCategory.setSelectedIndex(i); break;
            }
        }
        txtPrice.setText(tableModel.getValueAt(row, 3).toString());
        txtDesc.setText(tableModel.getValueAt(row, 4) != null ? tableModel.getValueAt(row, 4).toString() : "");
        cbAvailable.setSelectedIndex("Yes".equals(tableModel.getValueAt(row, 5)) ? 0 : 1);
    }

    private void saveItem() {
        String name = txtName.getText().trim();
        String priceStr = txtPrice.getText().trim();
        if (name.isEmpty() || priceStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name and Price are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double price;
        try { price = Double.parseDouble(priceStr); }
        catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a valid number.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (price < 0) {
            JOptionPane.showMessageDialog(this, "Price cannot be negative.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int catIdx = cbFormCategory.getSelectedIndex();
        int catId = Integer.parseInt(categories.get(catIdx)[0]);

        MenuItem item = new MenuItem();
        item.setName(name);
        item.setCategoryId(catId);
        item.setPrice(price);
        item.setDescription(txtDesc.getText().trim());
        item.setAvailable(cbAvailable.getSelectedIndex() == 0);

        boolean ok;
        if (editingId > 0) {
            item.setId(editingId);
            ok = menuDAO.updateItem(item);
            if (ok) JOptionPane.showMessageDialog(this, "Item updated successfully!");
        } else {
            ok = menuDAO.addItem(item);
            if (ok) JOptionPane.showMessageDialog(this, "Item added successfully!");
        }
        if (!ok) JOptionPane.showMessageDialog(this, "Operation failed.", "Error", JOptionPane.ERROR_MESSAGE);
        clearForm();
        loadData();
    }

    private void deleteItem() {
        if (editingId < 0) {
            JOptionPane.showMessageDialog(this, "Select an item first.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this item?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (menuDAO.deleteItem(editingId)) {
                JOptionPane.showMessageDialog(this, "Item deleted successfully.");
                if (onMenuChanged != null) onMenuChanged.run();
                clearForm(); loadData();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete item. Please try again.", "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void clearForm() {
        editingId = -1;
        txtName.setText(""); txtPrice.setText(""); txtDesc.setText("");
        if (cbFormCategory.getItemCount() > 0) cbFormCategory.setSelectedIndex(0);
        cbAvailable.setSelectedIndex(0);
        table.clearSelection();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private JTextField formField(String hint) {
        JTextField f = new JTextField();
        f.setFont(UIConstants.FONT_REGULAR);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                new EmptyBorder(4, 8, 4, 8)));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    private JButton makeButton(String text, Color topColor, Color botColor) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color t = getModel().isRollover() ? topColor.brighter() : topColor;
                Color b = getModel().isRollover() ? botColor.brighter() : botColor;
                GradientPaint gp = new GradientPaint(0, 0, t, 0, getHeight(), b);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UIConstants.FONT_REGULAR);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void styleTable(JTable t) {
        t.setFont(UIConstants.FONT_REGULAR);
        t.setRowHeight(32);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(UIConstants.ACCENT);
        t.setSelectionForeground(Color.WHITE);
        t.setBackground(UIConstants.BG_LIGHT);

        UIConstants.applyGradientHeader(t);

        // Alternating rows
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
                if (!sel) setBackground(r % 2 == 0 ? UIConstants.TABLE_ROW1 : UIConstants.TABLE_ROW2);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
    }
}
