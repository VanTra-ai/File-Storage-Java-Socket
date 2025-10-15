/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver;

/**
 * Lưu trữ thông tin trạng thái cho một phiên kết nối của client.
 */
public class ClientSession {
    private int currentUserId = -1;
    private String currentUsername = null;

    public int getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }
    
    public boolean isLoggedIn() {
        return this.currentUserId != -1;
    }
}
