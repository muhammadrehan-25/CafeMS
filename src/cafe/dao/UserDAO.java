package cafe.dao;

import cafe.models.User;
import cafe.utils.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO - Data Access Object for User operations.
 * Handles all CRUD operations on the users table.
 */
public class UserDAO {

    private Connection conn;

    public UserDAO() {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    /**
     * Authenticates a user by username and password.
     * @return User object if valid, null otherwise.
     */
    public User login(String username, String password) {
        String sql = "SELECT * FROM users WHERE username=? AND password=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, password.trim());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns all users from the database.
     */
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY id";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Inserts a new user. Returns true on success.
     */
    public boolean addUser(User user) {
        if (user.getUsername().isEmpty() || user.getPassword().isEmpty() || user.getFullName().isEmpty()) {
            return false;
        }
        String sql = "INSERT INTO users (username,password,role,full_name) VALUES (?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername().trim());
            ps.setString(2, user.getPassword().trim());
            ps.setString(3, user.getRole());
            ps.setString(4, user.getFullName().trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Updates an existing user's details.
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET full_name=?, role=?, password=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName().trim());
            ps.setString(2, user.getRole());
            ps.setString(3, user.getPassword().trim());
            ps.setInt(4, user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Deletes a user by ID.
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Searches users by name or username.
     */
    public List<User> searchUsers(String keyword) {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE full_name LIKE ? OR username LIKE ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setRole(rs.getString("role"));
        u.setFullName(rs.getString("full_name"));
        u.setCreatedAt(rs.getString("created_at"));
        return u;
    }
}
