/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver;

import filestorageserver.MyConnection;
import filestorageserver.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.mindrot.jbcrypt.BCrypt;

public class UserDAO {

    // Phương thức tiện ích để lấy kết nối
    private Connection getConnection() {
        // Tạo đối tượng MyConnection mới để lấy kết nối
        return new MyConnection().getConnection();
    }

    // 1. CHỨC NĂNG ĐĂNG KÝ (REGISTER)
    /**
     * Thực hiện đăng ký người dùng mới.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu thô (plain text).
     * @param email Email người dùng.
     * @return Mã trạng thái đăng ký.
     */
    public String registerUser(String username, String password, String email) {

        // 1. KIỂM TRA TÊN ĐĂNG NHẬP (USERNAME)
        if (usernameExists(username)) {
            System.err.println("Đăng ký thất bại: Tên đăng nhập " + username + " đã tồn tại.");
            return "REGISTER_FAIL_USERNAME_EXIST"; // Mã lỗi chi tiết
        }

        // 2. KIỂM TRA EMAIL ĐÃ TỒN TẠI
        if (emailExists(email)) {
            System.err.println("Đăng ký thất bại: Email " + email + " đã được sử dụng.");
            return "REGISTER_FAIL_EMAIL_EXIST"; // Mã lỗi chi tiết
        }

        // 3. TẠO HASH PASSWORD VÀ SALT (BCrypt tự nhúng salt vào hash)
        // 🔥 GIẢ ĐỊNH CSDL CHỈ CÓ CỘT password_hash (KHÔNG CỘT salt RIÊNG) 🔥
        String sql = "INSERT INTO users (username, password_hash, email) VALUES (?, ?, ?)";

        try {
            // BCrypt.gensalt() tạo ra salt ngẫu nhiên
            // BCrypt.hashpw() tạo ra chuỗi hash hoàn chỉnh bao gồm cost, salt và hash
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // 4. KẾT NỐI VÀ THỰC THI
            // Sửa lại cách sử dụng try-with-resources để con và ps được đóng đúng cách
            try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

                // Cần kiểm tra con == null ngay lập tức sau khi gọi getConnection()
                if (con == null) {
                    System.err.println("Không thể kết nối CSDL khi thực hiện đăng ký.");
                    return "REGISTER_FAIL_DB_ERROR"; // Lỗi kết nối
                }

                ps.setString(1, username);
                ps.setString(2, hashedPassword);
                ps.setString(3, email);

                int affectedRows = ps.executeUpdate();

                if (affectedRows > 0) {
                    return "REGISTER_SUCCESS"; // Thành công
                } else {
                    return "REGISTER_FAIL_DB_ERROR"; // Thất bại (không có hàng nào bị ảnh hưởng)
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi đăng ký: " + e.getMessage());
            // Lỗi CSDL thường do ràng buộc UNIQUE (nếu không kiểm tra trước) hoặc cấu trúc bảng
            e.printStackTrace();
            return "REGISTER_FAIL_DB_ERROR";
        } catch (Exception e) {
            System.err.println("Lỗi khi băm mật khẩu hoặc lỗi nội bộ khác: " + e.getMessage());
            e.printStackTrace();
            return "REGISTER_FAIL_INTERNAL_ERROR"; // Mã lỗi mới cho lỗi không liên quan đến CSDL/tồn tại
        }
    }

    // 2. CHỨC NĂNG ĐĂNG NHẬP (LOGIN)
    /**
     * Xác thực người dùng và trả về đối tượng User.
     *
     * @param username Tên đăng nhập.
     * @param password Mật khẩu thô (plain text).
     * @return Đối tượng User nếu đăng nhập thành công, null nếu thất bại.
     */
    public User login(String username, String password) {
        // Không cần truy vấn cột 'salt' vì nó nằm trong 'password_hash'
        String sql = "SELECT user_id, password_hash, email, created_at, last_login, is_active FROM Users WHERE username = ?";

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return null;
            }

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");

                    // XÁC MINH MẬT KHẨU BẰNG BCrypt (ĐÚNG BẢO MẬT)
                    boolean passwordMatches = BCrypt.checkpw(password, storedHash);

                    if (passwordMatches) {
                        updateLastLogin(rs.getInt("user_id"));

                        User user = new User();
                        user.setUserId(rs.getInt("user_id"));
                        user.setUsername(username);
                        user.setEmail(rs.getString("email"));
                        user.setCreatedAt(rs.getTimestamp("created_at"));
                        user.setLastLogin(rs.getTimestamp("last_login"));
                        user.setActive(rs.getBoolean("is_active"));

                        System.out.println("Đăng nhập thành công: " + username);
                        return user;
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi đăng nhập: " + ex.getMessage());
        }
        return null;
    }

    // 3. CẬP NHẬT (UPDATE) last_login
    /**
     * Cập nhật thời gian đăng nhập cuối cùng.
     *
     * @param userId ID người dùng.
     */
    private void updateLastLogin(int userId) {
        // Sử dụng NOW() hoặc CURRENT_TIMESTAMP() để Database tự điền thời gian hiện tại
        String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) {
                System.err.println("Không thể kết nối CSDL để cập nhật last_login.");
                return;
            }

            ps.setInt(1, userId);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                // System.out.println("Cập nhật last_login thành công cho User ID: " + userId);
            } else {
                System.err.println("Không tìm thấy người dùng để cập nhật last_login: " + userId);
            }

        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi cập nhật last_login: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // 4. GET USER BY ID
    /**
     * Lấy thông tin người dùng dựa trên ID.
     *
     * @param userId ID người dùng.
     * @return Đối tượng User nếu tìm thấy, null nếu không.
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
                    // Trả về đối tượng User (không cần password hash)
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

    // 5. KIỂM TRA TÍNH DUY NHẤT (Check Existence)
    /**
     * Kiểm tra username đã tồn tại chưa (hữu ích cho đăng ký).
     *
     * @param username Tên đăng nhập.
     * @return true nếu username đã tồn tại, false nếu chưa.
     */
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM Users WHERE username = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return false;
            }

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Nếu count > 0, nghĩa là username đã tồn tại
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi kiểm tra username: " + ex.getMessage());
        }
        return false;
    }

    // 6. ĐỔI MẬT KHẨU
    /**
     * Thay đổi mật khẩu của người dùng.
     *
     * @param userId ID người dùng.
     * @param newPassword Mật khẩu mới (plain text).
     * @return true nếu cập nhật thành công.
     */
    public boolean updatePassword(int userId, String newPassword) {
        // TẠO HASH MẬT KHẨU MỚI
        String newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

        String sql = "UPDATE Users SET password_hash = ? WHERE user_id = ?";

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return false;
            }

            ps.setString(1, newHashedPassword);
            ps.setInt(2, userId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi đổi mật khẩu: " + ex.getMessage());
        }
        return false;
    }

    // 7. CẬP NHẬT EMAIL
    /**
     * Cập nhật địa chỉ email của người dùng.
     *
     * @param userId ID người dùng.
     * @param newEmail Email mới.
     * @return true nếu cập nhật thành công.
     */
    public boolean updateEmail(int userId, String newEmail) {
        String sql = "UPDATE Users SET email = ? WHERE user_id = ?";

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return false;
            }

            ps.setString(1, newEmail);
            ps.setInt(2, userId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi cập nhật email: " + ex.getMessage());
            // Xử lý lỗi trùng UNIQUE (email)
            if (ex.getErrorCode() == 1062) {
                System.err.println("Email này đã được sử dụng bởi tài khoản khác.");
            }
        }
        return false;
    }

    // 8. XÓA TÀI KHOẢN
    /**
     * Xóa tài khoản người dùng và tất cả metadata file liên quan. LƯU Ý: Phải
     * xóa file vật lý trên Server bằng code Java trước khi gọi phương thức này!
     *
     * @param userId ID người dùng cần xóa.
     * @return true nếu xóa thành công.
     */
    public boolean deleteUser(int userId) {
        // Cần đảm bảo file vật lý đã bị xóa khỏi server trước bước này!
        // ... (Logic xóa file vật lý)

        String sql = "DELETE FROM Users WHERE user_id = ?";

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return false;
            }

            ps.setInt(1, userId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi xóa người dùng: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Kiểm tra email đã tồn tại chưa (hữu ích cho đăng ký).
     *
     * @param email Địa chỉ email.
     * @return true nếu email đã tồn tại, false nếu chưa.
     */
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM Users WHERE email = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return false;
            }

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi kiểm tra email: " + ex.getMessage());
        }
        return false;
    }

    /**
     * Tìm kiếm User ID dựa trên Tên đăng nhập.
     *
     * @return userId nếu tìm thấy, -1 nếu không tìm thấy.
     */
    public int getUserIdByUsername(String username) {
        // Kiểm tra trong bảng users
        String sql = "SELECT user_id FROM users WHERE username = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Trả về -1 nếu không tìm thấy
    }
}
