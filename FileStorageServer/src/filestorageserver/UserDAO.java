package filestorageserver;

import filestorageserver.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Lớp Data Access Object (DAO) cho các thao tác liên quan đến người dùng. Chịu
 * trách nhiệm cho mọi tương tác với bảng 'users' trong CSDL.
 */
public class UserDAO {

    private Connection getConnection() {
        return new MyConnection().getConnection();
    }

    /**
     * Đăng ký một người dùng mới vào hệ thống.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu thô (chưa băm).
     * @param email Email của người dùng.
     * @return Một chuỗi mã trạng thái (ví dụ: "REGISTER_SUCCESS").
     */
    public String registerUser(String username, String password, String email) {
        if (usernameExists(username)) {
            return "REGISTER_FAIL_USERNAME_EXIST";
        }
        if (emailExists(email)) {
            return "REGISTER_FAIL_EMAIL_EXIST";
        }

        String sql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";
        try {
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
                if (con == null) {
                    return "REGISTER_FAIL_DB_ERROR";
                }
                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                ps.setString(3, email);

                if (ps.executeUpdate() > 0) {
                    return "REGISTER_SUCCESS";
                } else {
                    return "REGISTER_FAIL_DB_ERROR";
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi đăng ký: " + e.getMessage());
            e.printStackTrace();
            return "REGISTER_FAIL_DB_ERROR";
        } catch (Exception e) {
            System.err.println("Lỗi nội bộ khi đăng ký: " + e.getMessage());
            e.printStackTrace();
            return "REGISTER_FAIL_INTERNAL_ERROR";
        }
    }

    /**
     * Xác thực thông tin đăng nhập của người dùng.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu thô để so sánh.
     * @return Một đối tượng User nếu đăng nhập thành công, ngược lại trả về
     * null.
     */
    public User login(String username, String password) {
        String sql = "SELECT user_id, password_hash, email, created_at, last_login, is_active FROM Users WHERE username = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return null;
            }

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (BCrypt.checkpw(password, storedHash)) {
                        int userId = rs.getInt("user_id");
                        updateLastLogin(userId);

                        User user = new User();
                        user.setUserId(userId);
                        user.setUsername(username);
                        user.setEmail(rs.getString("email"));
                        user.setCreatedAt(rs.getTimestamp("created_at"));
                        user.setLastLogin(rs.getTimestamp("last_login"));
                        user.setActive(rs.getBoolean("is_active"));
                        return user;
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi đăng nhập: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Cập nhật thời điểm đăng nhập cuối cùng cho người dùng.
     *
     * @param userId ID của người dùng cần cập nhật.
     */
    private void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                System.err.println("Không thể kết nối CSDL để cập nhật last_login.");
                return;
            }
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi cập nhật last_login: " + ex.getMessage());
        }
    }

    /**
     * Lấy thông tin người dùng dựa trên ID.
     *
     * @param userId ID người dùng cần tìm.
     * @return Đối tượng User nếu tìm thấy, ngược lại trả về null.
     */
    public User getUserById(int userId) {
        String sql = "SELECT user_id, username, email, created_at, last_login, is_active FROM Users WHERE user_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return null;
            }

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setUserId(rs.getInt("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setCreatedAt(rs.getTimestamp("created_at"));
                    user.setLastLogin(rs.getTimestamp("last_login"));
                    user.setActive(rs.getBoolean("is_active"));
                    return user;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi lấy user theo ID: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Tìm ID của người dùng dựa trên tên đăng nhập.
     *
     * @param username Tên đăng nhập cần tìm.
     * @return ID người dùng nếu tìm thấy, ngược lại trả về -1.
     */
    public int getUserIdByUsername(String username) {
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) {
                return -1;
            }

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    //<editor-fold defaultstate="collapsed" desc="Existence Check Methods">
    /**
     * Kiểm tra xem một username đã tồn tại trong CSDL hay chưa.
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT 1 FROM Users WHERE username = ? LIMIT 1";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return true; // Giả định là có để tránh tạo trùng lặp khi CSDL lỗi
            }
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi kiểm tra username: " + ex.getMessage());
            return true;
        }
    }

    /**
     * Kiểm tra xem một email đã tồn tại trong CSDL hay chưa.
     */
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM Users WHERE email = ? LIMIT 1";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return true;
            }
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi kiểm tra email: " + ex.getMessage());
            return true;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Update Methods (Password, Email, etc.)">
    /**
     * Cập nhật mật khẩu cho người dùng.
     */
    public boolean updatePassword(int userId, String newPassword) {
        String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        String sql = "UPDATE Users SET password_hash = ? WHERE user_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return false;
            }
            ps.setString(1, newHashedPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi đổi mật khẩu: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Cập nhật email cho người dùng.
     */
    public boolean updateEmail(int userId, String newEmail) {
        String sql = "UPDATE Users SET email = ? WHERE user_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return false;
            }
            ps.setString(1, newEmail);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            // Xử lý lỗi ràng buộc UNIQUE (email đã tồn tại)
            if (ex.getErrorCode() == 1062) {
                System.err.println("Cập nhật thất bại: Email '" + newEmail + "' đã được sử dụng.");
            } else {
                System.err.println("Lỗi CSDL khi cập nhật email: " + ex.getMessage());
            }
        }
        return false;
    }

    /**
     * Xóa một tài khoản người dùng khỏi CSDL. Lưu ý: Cần xử lý logic xóa file
     * vật lý và các bản ghi liên quan ở tầng cao hơn trước khi gọi hàm này.
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM Users WHERE user_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return false;
            }
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi xóa người dùng: " + ex.getMessage());
        }
        return false;
    }
    //</editor-fold>
}
