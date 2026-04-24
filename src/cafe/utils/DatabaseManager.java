package cafe.utils;

import java.sql.*;
import java.io.File;

/**
 * DatabaseManager - Singleton class for SQLite connection management.
 * Handles DB initialization and provides a shared connection.
 *
 * @author K25SW - Cafe Management System
 */
public class DatabaseManager {

    private static final String DB_URL;

    static {
        String appData = System.getenv("APPDATA");
        if (appData == null) appData = System.getProperty("user.home");
        File dbDir = new File(appData + File.separator + "CafeMS");
        dbDir.mkdirs();
        DB_URL = "jdbc:sqlite:" + dbDir.getAbsolutePath() + File.separator + "cafe_management.db";
    }
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.createStatement().execute("PRAGMA foreign_keys = ON");
            initializeDatabase();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to database: " + e.getMessage());
        }
    }

    /**
     * Returns the singleton instance of DatabaseManager.
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }

    /**
     * Runs SQL schema to create all tables and insert default data.
     */
    private void initializeDatabase() throws SQLException {
        Statement stmt = connection.createStatement();

        stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "role TEXT NOT NULL CHECK(role IN ('Admin','User'))," +
                "full_name TEXT NOT NULL," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");

        stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE)");

        stmt.execute("CREATE TABLE IF NOT EXISTS menu_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "category_id INTEGER NOT NULL," +
                "price REAL NOT NULL CHECK(price >= 0)," +
                "description TEXT," +
                "is_available INTEGER DEFAULT 1," +
                "FOREIGN KEY (category_id) REFERENCES categories(id))");

        // Remove duplicate seed rows if any exist, then enforce a unique index so insert-or-ignore works.
        stmt.execute("DELETE FROM menu_items WHERE id NOT IN (" +
                "SELECT MIN(id) FROM menu_items GROUP BY name, category_id, price, description)");
        stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_menu_items_unique " +
                "ON menu_items(name, category_id, price, description)");

        stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "table_no TEXT NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "total_amount REAL NOT NULL," +
                "status TEXT DEFAULT 'Pending'," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id))");

        stmt.execute("CREATE TABLE IF NOT EXISTS order_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "order_id INTEGER NOT NULL," +
                "menu_item_id INTEGER NOT NULL," +
                "quantity INTEGER NOT NULL," +
                "unit_price REAL NOT NULL," +
                "FOREIGN KEY (order_id) REFERENCES orders(id)," +
                "FOREIGN KEY (menu_item_id) REFERENCES menu_items(id))");

        // Only seed the admin account — extra users should NOT be re-seeded so deletes persist
        stmt.execute("INSERT OR IGNORE INTO users (username,password,role,full_name) VALUES ('admin','admin123','Admin','Administrator')");

        String[] cats = {"Hot Drinks", "Cold Drinks", "Snacks", "Main Course", "Desserts"};
        for (String c : cats) {
            stmt.execute("INSERT OR IGNORE INTO categories (name) VALUES ('" + c + "')");
        }

        ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM menu_items");
        int count = rs.getInt(1);
        if (count == 0) {
            String[][] items = {
                {"Chai (Tea)",     "1", "50",  "Traditional desi chai"},
                {"Cappuccino",     "1", "250", "Italian espresso with milk foam"},
                {"Latte",          "1", "280", "Espresso with steamed milk"},
                {"Cold Coffee",    "2", "300", "Blended iced coffee"},
                {"Lemonade",       "2", "150", "Fresh lemon juice"},
                {"Samosa (2pcs)",  "3", "80",  "Crispy fried pastry"},
                {"Club Sandwich",  "3", "350", "Triple-decker sandwich"},
                {"Chicken Burger", "4", "450", "Grilled chicken burger"},
                {"Pasta",          "4", "400", "Creamy white sauce pasta"},
                {"Chocolate Cake", "5", "200", "Rich chocolate slice"}
            };
            for (String[] item : items) {
                stmt.execute("INSERT INTO menu_items (name,category_id,price,description) VALUES " +
                        "('" + item[0] + "'," + item[1] + "," + item[2] + ",'" + item[3] + "')");
            }
        }

        stmt.close();
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
