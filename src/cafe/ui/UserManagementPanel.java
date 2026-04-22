package cafe.ui;

import cafe.dao.UserDAO;
import cafe.models.User;
import cafe.utils.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * UserManagementPanel - Admin-only panel for managing system users.
 * Full CRUD operations with search functionality.
 */
public class UserManagementPanel extends JPanel {

    private User currentUser;
    private UserDAO userDAO;
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField txtSearch;

    // Form fields
    private JTextField txtUsername, txtPassword, txtFullName;
    private JComboBox<String> cbRole;
    private JButton btnSave, btnClear, btnDelete;
    private int editingId = -1;

    public UserManagementPanel(User user) {
        this.currentUser = user;
        this.userDAO = new UserDAO();
        initUI();
        loadData();
    }

    private void initUI() {
        setBackground(UIConstants.BG_LIGHT);
        setLayout(new BorderLayout(16, 16));
        setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);
        JLabel title = new JLabel("User Management");
        title.setFont(UIConstants.FONT_TITLE);
        title.setForeground(UIConstants.PRIMARY_DARK);
        headerBar.add(title, BorderLayout.WEST);
        headerBar.add(UIConstants.createLiveClock(), BorderLayout.EAST);
        add(headerBar, BorderLayout.NORTH);

        // ── Left: table + search ──
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setOpaque(false);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        searchBar.setOpaque(false);
        txtSearch = new JTextField(18);
        txtSearch.setFont(UIConstants.FONT_REGULAR);
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                new EmptyBorder(6, 10, 6, 10)));
        JButton btnSearch = makeBtn("Search", UIConstants.BTN_PRIMARY_TOP, UIConstants.BTN_PRIMARY_BOT);
        btnSearch.addActionListener(e -> doSearch());
        JButton btnAll = makeBtn("Show All", UIConstants.BTN_GRAY_TOP, UIConstants.BTN_GRAY_BOT);
        btnAll.addActionListener(e -> loadData());
        searchBar.add(new JLabel("Search:"));
        searchBar.add(txtSearch);
        searchBar.add(btnSearch);
        searchBar.add(btnAll);

        String[] cols = {"ID", "Username", "Full Name", "Role", "Created At"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) populateForm();
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_COLOR));

        left.add(searchBar, BorderLayout.NORTH);
        left.add(scroll, BorderLayout.CENTER);

        // ── Right: form ──
        JPanel right = buildFormPanel();
        right.setPreferredSize(new Dimension(280, 0));

        add(left, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                new EmptyBorder(20, 16, 20, 16)));

        JLabel lTitle = new JLabel("User Details");
        lTitle.setFont(UIConstants.FONT_HEADING);
        lTitle.setForeground(UIConstants.PRIMARY_DARK);
        lTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        txtFullName = ff();
        txtUsername = ff();
        txtPassword = ff();

        cbRole = new JComboBox<>(new String[]{"User", "Admin"});
        cbRole.setFont(UIConstants.FONT_REGULAR);
        cbRole.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cbRole.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnSave   = makeBtn("Save User",   UIConstants.BTN_SUCCESS_TOP, UIConstants.BTN_SUCCESS_BOT);
        btnDelete = makeBtn("Delete User", UIConstants.BTN_DANGER_TOP,  UIConstants.BTN_DANGER_BOT);
        btnClear  = makeBtn("Clear",        UIConstants.BTN_GRAY_TOP,   UIConstants.BTN_GRAY_BOT);

        for (JButton b : new JButton[]{btnSave, btnDelete, btnClear}) {
            b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            b.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        btnSave.addActionListener(e -> saveUser());
        btnDelete.addActionListener(e -> deleteUser());
        btnClear.addActionListener(e -> clearForm());

        JLabel note = new JLabel("<html><i>Note: Cannot delete the admin<br>account or your own account.</i></html>");
        note.setFont(UIConstants.FONT_SMALL);
        note.setForeground(UIConstants.TEXT_GRAY);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(lTitle);
        panel.add(Box.createVerticalStrut(14));
        panel.add(lbl("Full Name:")); panel.add(txtFullName); panel.add(Box.createVerticalStrut(8));
        panel.add(lbl("Username:")); panel.add(txtUsername); panel.add(Box.createVerticalStrut(8));
        panel.add(lbl("Password:")); panel.add(txtPassword); panel.add(Box.createVerticalStrut(8));
        panel.add(lbl("Role:")); panel.add(cbRole); panel.add(Box.createVerticalStrut(16));
        panel.add(btnSave); panel.add(Box.createVerticalStrut(6));
        panel.add(btnDelete); panel.add(Box.createVerticalStrut(6));
        panel.add(btnClear); panel.add(Box.createVerticalStrut(14));
        panel.add(note);
        return panel;
    }

    public void loadData() {
        refreshTable(userDAO.getAllUsers());
    }

    private void refreshTable(List<User> users) {
        tableModel.setRowCount(0);
        for (User u : users) {
            tableModel.addRow(new Object[]{
                u.getId(), u.getUsername(), u.getFullName(), u.getRole(), u.getCreatedAt()
            });
        }
    }

    private void doSearch() {
        String kw = txtSearch.getText().trim();
        if (kw.isEmpty()) { loadData(); return; }
        refreshTable(userDAO.searchUsers(kw));
    }

    private void populateForm() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        editingId = (int) tableModel.getValueAt(row, 0);
        txtUsername.setText((String) tableModel.getValueAt(row, 1));
        txtFullName.setText((String) tableModel.getValueAt(row, 2));
        String role = (String) tableModel.getValueAt(row, 3);
        cbRole.setSelectedItem(role);
        txtPassword.setText("");
    }

    private void saveUser() {
        String full = txtFullName.getText().trim();
        String uname = txtUsername.getText().trim();
        String pass = txtPassword.getText().trim();
        if (full.isEmpty() || uname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Full name and username are required.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (editingId < 0 && pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password is required for new users.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        User u = new User();
        u.setFullName(full);
        u.setUsername(uname);
        u.setRole((String) cbRole.getSelectedItem());
        u.setPassword(pass.isEmpty() ? "waiter123" : pass);

        boolean ok;
        if (editingId > 0) {
            u.setId(editingId);
            ok = userDAO.updateUser(u);
        } else {
            ok = userDAO.addUser(u);
        }

        if (ok) {
            JOptionPane.showMessageDialog(this, "User saved successfully!");
            clearForm(); loadData();
        } else {
            JOptionPane.showMessageDialog(this, "Operation failed. Username may already exist.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteUser() {
        if (editingId < 0) { JOptionPane.showMessageDialog(this, "Select a user first."); return; }
        if (editingId == currentUser.getId()) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own account.", "Forbidden", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this user?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            if (userDAO.deleteUser(editingId)) {
                JOptionPane.showMessageDialog(this, "User deleted.");
                clearForm(); loadData();
            }
        }
    }

    private void clearForm() {
        editingId = -1;
        txtFullName.setText(""); txtUsername.setText(""); txtPassword.setText("");
        cbRole.setSelectedIndex(0);
        table.clearSelection();
    }

    private JTextField ff() {
        JTextField f = new JTextField();
        f.setFont(UIConstants.FONT_REGULAR);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_COLOR),
                new EmptyBorder(4, 8, 4, 8)));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }

    private JLabel lbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UIConstants.FONT_REGULAR);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
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

    private void styleTable() {
        table.setFont(UIConstants.FONT_REGULAR);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(UIConstants.ACCENT);
        table.setSelectionForeground(Color.WHITE);

        UIConstants.applyGradientHeader(table);

        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(3).setMaxWidth(80);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(t, v, s, f, r, c);
                if (!s) setBackground(r % 2 == 0 ? UIConstants.TABLE_ROW1 : UIConstants.TABLE_ROW2);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
    }
}
