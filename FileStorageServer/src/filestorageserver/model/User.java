package filestorageserver.model;

import java.sql.Timestamp;

/**
 * Đại diện cho một đối tượng Người dùng (User), ánh xạ tới bảng 'users' trong
 * cơ sở dữ liệu.
 */
public class User {

    /**
     * ID duy nhất của người dùng, tương ứng với cột 'user_id'.
     */
    private int userId;

    /**
     * Tên đăng nhập của người dùng, tương ứng với cột 'username'.
     */
    private String username;

    /**
     * Chuỗi mật khẩu đã được băm bằng BCrypt, tương ứng với cột
     * 'password_hash'.
     */
    private String passwordHash;

    /**
     * Địa chỉ email của người dùng, tương ứng với cột 'email'.
     */
    private String email;

    /**
     * Thời điểm tài khoản được tạo, tương ứng với cột 'created_at'.
     */
    private Timestamp createdAt;

    /**
     * Thời điểm đăng nhập cuối cùng, tương ứng với cột 'last_login'.
     */
    private Timestamp lastLogin;

    /**
     * Trạng thái hoạt động của tài khoản, tương ứng với cột 'is_active'.
     */
    private boolean isActive;

    /**
     * Constructor mặc định.
     */
    public User() {
    }

    /**
     * Constructor đầy đủ để khởi tạo một đối tượng User với tất cả thuộc tính.
     */
    public User(int userId, String username, String passwordHash, String email, Timestamp createdAt, Timestamp lastLogin, boolean isActive) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.isActive = isActive;
    }

    //<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Timestamp lastLogin) {
        this.lastLogin = lastLogin;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    //</editor-fold>
}
