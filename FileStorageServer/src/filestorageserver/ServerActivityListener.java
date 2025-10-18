package filestorageserver;

/**
 * Interface cho các đối tượng muốn lắng nghe và phản hồi lại các hoạt động xảy
 * ra trên server, ví dụ như trong các ClientHandler.
 */
public interface ServerActivityListener {

    /**
     * Được gọi khi một client kết nối thành công.
     *
     * @param handler Luồng xử lý cho client vừa kết nối.
     */
    void onClientConnected(ClientHandler handler);

    /**
     * Được gọi khi một client ngắt kết nối.
     *
     * @param handler Luồng xử lý cho client vừa ngắt kết nối.
     */
    void onClientDisconnected(ClientHandler handler);

    /**
     * Được gọi khi một người dùng đăng nhập thành công.
     *
     * @param handler Luồng xử lý của client.
     * @param username Tên người dùng vừa đăng nhập.
     */
    void onUserLoggedIn(ClientHandler handler, String username);

    /**
     * Được gọi khi một file được tải lên thành công.
     *
     * @param username Tên người dùng thực hiện.
     * @param fileName Tên file đã tải lên.
     */
    void onFileUploaded(String username, String fileName);

    /**
     * Được gọi khi một file được tải xuống thành công.
     *
     * @param username Tên người dùng thực hiện.
     * @param fileName Tên file đã tải xuống.
     */
    void onFileDownloaded(String username, String fileName);

    /**
     * Được gọi khi một người dùng đăng xuất.
     *
     * @param username Tên người dùng vừa đăng xuất.
     */
    void onUserLoggedOut(String username);

    /**
     * Được gọi khi một tài khoản mới được đăng ký thành công.
     *
     * @param username Tên tài khoản mới.
     */
    void onUserRegistered(String username);

    /**
     * Được gọi khi một file bị xóa thành công.
     *
     * @param username Tên người dùng thực hiện.
     * @param fileName Tên file đã bị xóa.
     */
    void onFileDeleted(String username, String fileName);

    /**
     * Được gọi khi một file được chia sẻ thành công.
     *
     * @param sharer Tên người chia sẻ.
     * @param receiver Tên người nhận.
     * @param fileName Tên file được chia sẻ.
     */
    void onFileShared(String sharer, String receiver, String fileName);

    /**
     * Được gọi khi trạng thái hoạt động của một client thay đổi.
     *
     * @param handler Luồng xử lý của client.
     * @param newStatus Trạng thái mới (ví dụ: "Uploading...", "Idle").
     */
    void onUserActivityChanged(ClientHandler handler, String newStatus);

    /**
     * Được gọi khi một lượt chia sẻ file bị hủy.
     *
     * @param unsharer Tên người hủy chia sẻ.
     * @param receiver Tên người bị hủy quyền.
     * @param fileName Tên file liên quan.
     */
    void onFileUnshared(String unsharer, String receiver, String fileName);

    /**
     * Được gọi khi quyền hoặc thời hạn của một lượt chia sẻ được cập nhật.
     *
     * @param updater Tên người cập nhật.
     * @param receiver Tên người nhận.
     * @param fileName Tên file liên quan.
     */
    void onShareUpdated(String updater, String receiver, String fileName);

    /**
     * Được gọi khi một thư mục mới được tạo.
     *
     * @param username Tên người dùng thực hiện.
     * @param folderName Tên thư mục đã tạo.
     */
    void onFolderCreated(String username, String folderName);

    /**
     * Được gọi khi một file được di chuyển sang thư mục khác.
     *
     * @param username Tên người dùng thực hiện.
     * @param fileName Tên file đã di chuyển.
     * @param targetFolderId ID thư mục đích (null nếu ra gốc).
     */
    void onFileMoved(String username, String fileName, Integer targetFolderId);

    /**
     * Được gọi khi một thư mục được di chuyển sang thư mục khác.
     *
     * @param username Tên người dùng thực hiện.
     * @param folderName Tên thư mục đã di chuyển.
     * @param targetFolderId ID thư mục đích (null nếu ra gốc).
     */
    void onFolderMoved(String username, String folderName, Integer targetFolderId);
    /**
     * Được gọi khi một thư mục được đổi tên thành công.
     *
     * @param username Tên người dùng thực hiện.
     * @param oldFolderName Tên thư mục cũ.
     * @param newFolderName Tên thư mục mới.
     */
    void onFolderRenamed(String username, String oldFolderName, String newFolderName);

    /**
     * Được gọi khi một thư mục bị xóa thành công.
     *
     * @param username Tên người dùng thực hiện.
     * @param folderName Tên thư mục đã bị xóa.
     */
    void onFolderDeleted(String username, String folderName);
}
