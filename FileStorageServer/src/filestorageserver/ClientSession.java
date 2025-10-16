package filestorageserver;

/**
 * Lưu trữ thông tin trạng thái cho một phiên kết nối của client. Mỗi đối tượng
 * của lớp này đại diện cho một người dùng đang đăng nhập.
 */
public class ClientSession {

    private int currentUserId = -1;
    private String currentUsername = null;
    private final ClientHandler handler;

    /**
     * Lấy ID của người dùng hiện tại đang đăng nhập.
     *
     * @return ID người dùng, hoặc -1 nếu chưa đăng nhập.
     */
    public int getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(int currentUserId) {
        this.currentUserId = currentUserId;
    }

    /**
     * Lấy tên đăng nhập của người dùng hiện tại.
     *
     * @return Tên đăng nhập, hoặc null nếu chưa đăng nhập.
     */
    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    /**
     * Kiểm tra xem phiên làm việc này đã được xác thực (đăng nhập) hay chưa.
     *
     * @return true nếu người dùng đã đăng nhập, ngược lại trả về false.
     */
    public boolean isLoggedIn() {
        return this.currentUserId != -1;
    }

    public ClientSession(ClientHandler handler) {
        this.handler = handler;
    }

    public ClientHandler getHandler() {
        return handler;
    }
}
