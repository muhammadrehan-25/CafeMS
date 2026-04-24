package cafe.dao;

import cafe.models.MenuItem;
import cafe.utils.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MenuDAO - Data Access Object for Menu operations.
 * Handles CRUD on menu_items and categories tables.
 */
public class MenuDAO {

    private Connection conn;

    public MenuDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ─── MENU ITEMS ────────────────────────────────────────────────────────

    /** Returns all menu items with their category names. */
    public List<MenuItem> getAllItems() {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT m.*, c.name AS cat_name FROM menu_items m " +
                     "JOIN categories c ON m.category_id = c.id ORDER BY c.name, m.name";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapItem(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Returns items filtered by category. */
    public List<MenuItem> getItemsByCategory(int categoryId) {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT m.*, c.name AS cat_name FROM menu_items m " +
                     "JOIN categories c ON m.category_id=c.id WHERE m.category_id=? AND m.is_available=1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapItem(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Searches items by name or description. */
    public List<MenuItem> searchItems(String keyword) {
        List<MenuItem> list = new ArrayList<>();
        String sql = "SELECT m.*, c.name AS cat_name FROM menu_items m " +
                     "JOIN categories c ON m.category_id=c.id " +
                     "WHERE m.name LIKE ? OR m.description LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapItem(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public boolean addItem(MenuItem item) {
        if (item.getName().isEmpty() || item.getPrice() < 0) return false;
        String sql = "INSERT INTO menu_items (name,category_id,price,description,is_available) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName().trim());
            ps.setInt(2, item.getCategoryId());
            ps.setDouble(3, item.getPrice());
            ps.setString(4, item.getDescription());
            ps.setInt(5, item.isAvailable() ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Updates an existing menu item. */
    public boolean updateItem(MenuItem item) {
        String sql = "UPDATE menu_items SET name=?,category_id=?,price=?,description=?,is_available=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getName().trim());
            ps.setInt(2, item.getCategoryId());
            ps.setDouble(3, item.getPrice());
            ps.setString(4, item.getDescription());
            ps.setInt(5, item.isAvailable() ? 1 : 0);
            ps.setInt(6, item.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Deletes a menu item by ID. */
    public boolean deleteItem(int itemId) {
        boolean success = false;
        try {
            // Disable foreign keys temporarily to force delete
            conn.createStatement().execute("PRAGMA foreign_keys = OFF");
            String sql = "DELETE FROM menu_items WHERE id=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, itemId);
                success = ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            // Always re-enable
            try { conn.createStatement().execute("PRAGMA foreign_keys = ON"); } catch (Exception ignored) {}
        }
        return success;
    }

    // ─── CATEGORIES ────────────────────────────────────────────────────────

    /** Returns all categories as a 2D array [id, name]. */
    public List<String[]> getAllCategories() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY name";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{String.valueOf(rs.getInt("id")), rs.getString("name")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private MenuItem mapItem(ResultSet rs) throws SQLException {
        MenuItem m = new MenuItem();
        m.setId(rs.getInt("id"));
        m.setName(rs.getString("name"));
        m.setCategoryId(rs.getInt("category_id"));
        m.setCategoryName(rs.getString("cat_name"));
        m.setPrice(rs.getDouble("price"));
        m.setDescription(rs.getString("description"));
        m.setAvailable(rs.getInt("is_available") == 1);
        return m;
    }
}
