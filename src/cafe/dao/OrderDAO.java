package cafe.dao;

import cafe.utils.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * OrderDAO - Data Access Object for Order operations.
 */
public class OrderDAO {

    private Connection conn;

    public OrderDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    /**
     * Places a new order. Returns the new order ID, or -1 on failure.
     */
    public int placeOrder(String tableNo, int userId, Object[][] items) {
        String orderSql = "INSERT INTO orders (table_no, user_id, total_amount, status) VALUES (?,?,?,'Completed')";
        String itemSql  = "INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price) VALUES (?,?,?,?)";

        try {
            // Use explicit SQL transactions (safer with shared SQLite connection)
            conn.createStatement().execute("BEGIN");

            double total = 0;
            for (Object[] item : items) {
                total += ((int) item[1]) * ((double) item[2]);
            }

            PreparedStatement ps = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, tableNo);
            ps.setInt(2, userId);
            ps.setDouble(3, total);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            int orderId = -1;
            if (keys.next()) orderId = keys.getInt(1);

            if (orderId == -1) {
                conn.createStatement().execute("ROLLBACK");
                return -1;
            }

            PreparedStatement ps2 = conn.prepareStatement(itemSql);
            for (Object[] item : items) {
                ps2.setInt(1, orderId);
                ps2.setInt(2, (int) item[0]);
                ps2.setInt(3, (int) item[1]);
                ps2.setDouble(4, (double) item[2]);
                ps2.addBatch();
            }
            ps2.executeBatch();

            conn.createStatement().execute("COMMIT");
            return orderId;

        } catch (SQLException e) {
            e.printStackTrace();
            try { conn.createStatement().execute("ROLLBACK"); } catch (SQLException ex) { ex.printStackTrace(); }
            return -1;
        }
    }

    /** Updates order status (Pending/Completed/Cancelled). */
    public boolean updateStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); return false; }
    }

    /** Permanently deletes an order and its line-items from the database. */
    public boolean deleteOrder(int orderId) {
        try {
            conn.setAutoCommit(false);
            try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM order_items WHERE order_id=?")) {
                ps1.setInt(1, orderId);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement("DELETE FROM orders WHERE id=?")) {
                ps2.setInt(1, orderId);
                ps2.executeUpdate();
            }
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) { ex.printStackTrace(); }
            return false;
        }
    }

    /** Returns all orders (header data) for the reports/admin view. */
    public List<Object[]> getAllOrders() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT o.id, o.table_no, u.full_name, o.total_amount, o.status, " +
                     "STRFTIME('%I:%M %p  %d/%m/%Y', o.created_at, 'localtime') AS created_at " +
                     "FROM orders o JOIN users u ON o.user_id=u.id ORDER BY o.id DESC";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"),
                    rs.getString("table_no"),
                    rs.getString("full_name"),
                    rs.getDouble("total_amount"),
                    rs.getString("status"),
                    rs.getString("created_at")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Returns orders filtered by status. */
    public List<Object[]> getOrdersByStatus(String status) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT o.id, o.table_no, u.full_name, o.total_amount, o.status, " +
                     "STRFTIME('%I:%M %p  %d/%m/%Y', o.created_at, 'localtime') AS created_at " +
                     "FROM orders o JOIN users u ON o.user_id=u.id WHERE o.status=? ORDER BY o.id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("table_no"), rs.getString("full_name"),
                    rs.getDouble("total_amount"), rs.getString("status"), rs.getString("created_at")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /** Returns order line-items for a specific order ID. */
    public List<Object[]> getOrderItems(int orderId) {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT COALESCE(m.name, 'Deleted Item') AS name, oi.quantity, oi.unit_price, (oi.quantity*oi.unit_price) AS subtotal " +
                     "FROM order_items oi LEFT JOIN menu_items m ON oi.menu_item_id=m.id WHERE oi.order_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getString("name"), rs.getInt("quantity"),
                    rs.getDouble("unit_price"), rs.getDouble("subtotal")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Aggregates dashboard stats from completed orders only.
     * Returns double[4]:
     *   [0] totalRevenue    - sum of all completed orders
     *   [1] completedOrders - count of completed orders
     *   [2] todayRevenue    - sum of completed orders placed today
     *   [3] avgOrderValue   - average total_amount of completed orders
     */
    public double[] getSummaryStats() {
        double[] stats = {0, 0, 0, 0};
        try {
            ResultSet rs1 = conn.createStatement().executeQuery(
                "SELECT SUM(total_amount), COUNT(*), AVG(total_amount) " +
                "FROM orders WHERE status='Completed'");
            if (rs1.next()) {
                stats[0] = rs1.getDouble(1);
                stats[1] = rs1.getDouble(2);
                stats[3] = rs1.getDouble(3);
            }

            ResultSet rs2 = conn.createStatement().executeQuery(
                "SELECT SUM(total_amount) FROM orders " +
                "WHERE status='Completed' AND DATE(created_at, 'localtime')=DATE('now', 'localtime')");
            if (rs2.next()) stats[2] = rs2.getDouble(1);

        } catch (SQLException e) { e.printStackTrace(); }
        return stats;
    }

    /**
     * Returns the latest N orders (any status) for the dashboard Recent Orders list.
     * Each Object[] = { orderId, itemsSummary, totalAmount, status, timeStr }
     */
    public List<Object[]> getRecentOrders(int limit) {
        List<Object[]> list = new ArrayList<>();
        // Subquery builds a comma-separated item name list per order
        String sql =
            "SELECT o.id, " +
            "  (SELECT GROUP_CONCAT(COALESCE(m.name, 'Deleted Item'), ', ') " +
            "   FROM order_items oi LEFT JOIN menu_items m ON oi.menu_item_id=m.id " +
            "   WHERE oi.order_id=o.id) AS items_summary, " +
            "  o.total_amount, o.status, " +
            "  STRFTIME('%I:%M %p  %d/%m/%Y', o.created_at, 'localtime') AS time_str " +
            "FROM orders o " +
            "WHERE DATE(o.created_at, 'localtime') = DATE('now', 'localtime') " +
            "ORDER BY o.id DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String items = rs.getString("items_summary");
                if (items == null) items = "—";
                // Truncate long item lists
                if (items.length() > 32) items = items.substring(0, 30) + "…";
                list.add(new Object[]{
                    rs.getInt("id"),
                    items,
                    rs.getDouble("total_amount"),
                    rs.getString("status"),
                    rs.getString("time_str")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    /**
     * Returns revenue per day for the last 7 calendar days (oldest → newest).
     * Each Object[] = { dayLabel (e.g. "Mon"), revenue }
     * Days with no completed orders return 0.
     */
    public Object[][] getLast7DaysRevenue() {
        Object[][] result = new Object[7][2];
        // Generate 7 days: index 0 = 6 days ago, index 6 = today
        String[] dayNames = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        java.time.LocalDate today = java.time.LocalDate.now();

        for (int i = 0; i < 7; i++) {
            java.time.LocalDate d = today.minusDays(6 - i);
            result[i][0] = dayNames[d.getDayOfWeek().getValue() % 7]; // Sun=0
            result[i][1] = 0.0;
        }

        String sql =
            "SELECT DATE(created_at, 'localtime') AS day, SUM(total_amount) AS rev " +
            "FROM orders " +
            "WHERE status='Completed' " +
            "  AND DATE(created_at, 'localtime') >= DATE('now', 'localtime', '-6 days') " +
            "GROUP BY DATE(created_at, 'localtime') " +
            "ORDER BY day ASC";

        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String dayStr = rs.getString("day"); // yyyy-MM-dd
                double rev    = rs.getDouble("rev");
                java.time.LocalDate d = java.time.LocalDate.parse(dayStr);
                int idx = (int) java.time.temporal.ChronoUnit.DAYS.between(
                        today.minusDays(6), d);
                if (idx >= 0 && idx < 7) {
                    result[idx][1] = rev;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return result;
    }
}