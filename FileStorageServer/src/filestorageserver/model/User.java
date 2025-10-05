/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver.model;

import java.sql.Timestamp; // Sử dụng Timestamp cho các cột DATETIME

public class User {
    
    // Tên thuộc tính phải khớp với tên cột trong CSDL (hoặc tuân theo quy ước Java camelCase)
    private int userId; 
    private String username;
    private String passwordHash; // Cần thiết để lấy hash ra so sánh khi đăng nhập
    private String email;
    private Timestamp createdAt;
    private Timestamp lastLogin;
    private boolean isActive; // Dùng boolean thay cho TINYINT(1) trong Java
    
    // Constructor mặc định
    public User() {
    }

    // Constructor đầy đủ (có thể dùng khi tạo user mới)
    public User(int userId, String username, String passwordHash, String email, Timestamp createdAt, Timestamp lastLogin, boolean isActive) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.isActive = isActive;
    }
    
    // --- GETTERS & SETTERS (RẤT QUAN TRỌNG) ---
    // NetBeans có thể tự động tạo bằng cách nhấn chuột phải -> Insert Code -> Getter and Setter

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

    // Bắt buộc phải có getter cho passwordHash để UserDAO có thể lấy ra so sánh!
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
}
