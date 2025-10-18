package filestorageclient;

import java.io.*;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;
import java.util.List;
import java.util.Properties;
import java.nio.file.Paths;
import java.nio.file.Files;

/**
 * Quản lý kết nối và giao tiếp với server. Lớp này được triển khai theo mẫu
 * Singleton để đảm bảo chỉ có một kết nối duy nhất trong toàn bộ ứng dụng.
 */
public class ClientSocketManager {

    private static final ClientSocketManager instance = new ClientSocketManager();

    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private int currentUserId = -1;
    private String currentUsername = null;
    private boolean isConnected = false;

    private ClientSocketManager() {
        String absoluteTrustStorePath = "C:\\Users\\Admin\\OneDrive\\Documents\\NetBeansProjects\\File-Storage-Java-Socket\\Drivers\\SSL\\client.jks";
        System.setProperty("javax.net.ssl.trustStore", absoluteTrustStorePath);
        System.setProperty("javax.net.ssl.trustStorePassword", "123456");
    }

    public static ClientSocketManager getInstance() {
        return instance;
    }

    // Lớp nhỏ để đóng gói dữ liệu tiến độ
    public static class ProgressData {

        private final long totalBytesTransferred;
        private final long totalFileSize;

        public ProgressData(long transferred, long total) {
            this.totalBytesTransferred = transferred;
            this.totalFileSize = total;
        }

        public int getPercentage() {
            if (totalFileSize == 0) {
                return 100;
            }
            return (int) ((totalBytesTransferred * 100) / totalFileSize);
        }

        public long getTotalBytesTransferred() {
            return totalBytesTransferred;
        }

        public long getTotalFileSize() {
            return totalFileSize;
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Connection & Auth Methods">
    public synchronized boolean connect() {
        if (isConnected) {
            return true;
        }
        try {
            SocketFactory sf = SSLSocketFactory.getDefault();
            socket = sf.createSocket(SERVER_IP, SERVER_PORT);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            isConnected = true;
            System.out.println("Đã kết nối an toàn (SSL) đến Server.");
            return true;
        } catch (IOException e) {
            System.err.println("Lỗi kết nối SSL đến Server: " + e.getMessage());
            isConnected = false;
            return false;
        }
    }

    public synchronized void disconnect() {
        try {
            if (dos != null) {
                dos.close();
            }
            if (dis != null) {
                dis.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Bỏ qua lỗi khi đóng
        } finally {
            isConnected = false;
            currentUserId = -1;
            currentUsername = null;
            System.out.println("Đã ngắt kết nối và reset trạng thái.");
        }
    }

    public String register(String username, String password, String email) {
        String command = "CMD_REGISTER";
        if (!isConnected && !connect()) {
            return "ERROR_CONNECTION";
        }
        try {
            dos.writeUTF(command);
            dos.writeUTF(username);
            dos.writeUTF(password);
            dos.writeUTF(email);
            dos.flush();
            return dis.readUTF();
        } catch (IOException e) {
            handleIOException(e, command);
            return "ERROR_IO";
        }
    }

    public String login(String username, String password) {
        String command = "CMD_LOGIN";
        if (!isConnected && !connect()) {
            return "ERROR_CONNECTION";
        }
        try {
            dos.writeUTF(command);
            dos.writeUTF(username);
            dos.writeUTF(password);
            dos.flush();
            String response = dis.readUTF();
            if ("LOGIN_SUCCESS".equals(response)) {
                this.currentUserId = dis.readInt();
                this.currentUsername = dis.readUTF();
                return "LOGIN_SUCCESS:" + this.currentUsername;
            }
            return response;
        } catch (IOException e) {
            handleIOException(e, command);
            return "ERROR_IO";
        }
    }

    public void logout() {
        if (isConnected) {
            try {
                dos.writeUTF("CMD_LOGOUT");
                dos.flush();
            } catch (IOException e) {
                // Bỏ qua lỗi
            } finally {
                disconnect();
            }
        }
    }

    //<editor-fold defaultstate="collapsed" desc="File Transfer Methods">
    public String uploadFile(java.io.File fileToUpload, Integer folderId, ProgressPublisher publisher) {
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }
        if (!fileToUpload.exists() || !fileToUpload.isFile()) {
            return "ERROR_FILE_NOT_FOUND";
        }
        if (!isConnected && !connect()) {
            return "ERROR_CONNECTION";
        }

        String sessionId = null;
        int chunkSize = -1;
        long totalFileSize = fileToUpload.length();
        long currentOffset = 0; // Offset bắt đầu
        String filePathKey = fileToUpload.getAbsolutePath(); // Dùng đường dẫn tuyệt đối làm key cache

        try {
            // --- Bước 0: Kiểm tra Cache để Resume ---
            String cachedSessionId = getCachedSessionId(filePathKey);
            boolean resuming = false;

            if (cachedSessionId != null) {
                System.out.println("Tìm thấy session ID trong cache: " + cachedSessionId + ". Thử resume...");
                String statusResponse = getUploadStatus(cachedSessionId); // Gọi CMD_GET_UPLOAD_STATUS

                if (statusResponse.startsWith("UPLOAD_STATUS:")) {
                    try {
                        String[] parts = statusResponse.split(":");
                        currentOffset = Long.parseLong(parts[1]);
                        chunkSize = Integer.parseInt(parts[2]);
                        sessionId = cachedSessionId; // Sử dụng lại session ID cũ
                        resuming = true;
                        System.out.println("Resume thành công tại offset: " + currentOffset + ", chunk size: " + chunkSize);
                        if (publisher != null) { // Báo cáo tiến độ ban đầu khi resume
                            publisher.publishProgress(new ProgressData(currentOffset, totalFileSize));
                        }
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                        System.err.println("Lỗi phân tích phản hồi UPLOAD_STATUS: " + statusResponse);
                        removeCachedSessionId(filePathKey); // Xóa cache lỗi
                        currentOffset = 0; // Bắt đầu lại từ đầu
                    }
                } else {
                    System.out.println("Server báo không tìm thấy session (" + statusResponse + "). Bắt đầu upload mới.");
                    removeCachedSessionId(filePathKey); // Xóa cache không hợp lệ
                    currentOffset = 0; // Bắt đầu lại từ đầu
                }
            }

            // --- Bước 1: Bắt đầu phiên Upload (Nếu không resume) ---
            if (!resuming) {
                dos.writeUTF("CMD_START_UPLOAD");
                dos.writeUTF(fileToUpload.getName());
                dos.writeLong(totalFileSize);
                dos.writeInt(folderId != null ? folderId : -1);
                dos.flush();

                String startResponse = dis.readUTF();
                if (!"UPLOAD_STARTED".equals(startResponse)) {
                    System.err.println("Lỗi bắt đầu upload: " + startResponse);
                    return startResponse;
                }
                sessionId = dis.readUTF();
                chunkSize = dis.readInt();
                System.out.println("Upload session mới bắt đầu: " + sessionId + ", Chunk size: " + chunkSize);

                // Lưu session ID vào cache
                cacheSessionId(filePathKey, sessionId);
            }

            // --- Bước 2: Gửi từng Chunk (Từ currentOffset) ---
            try (FileInputStream fis = new FileInputStream(fileToUpload)) {

                // *** QUAN TRỌNG: Bỏ qua phần đã upload nếu resume ***
                if (resuming && currentOffset > 0) {
                    long skipped = fis.skip(currentOffset);
                    if (skipped != currentOffset) {
                        System.err.println("Lỗi khi skip file đến offset " + currentOffset);
                        removeCachedSessionId(filePathKey); // Xóa cache lỗi
                        return "ERROR_FILE_SEEK_FAILED";
                    }
                    System.out.println("Đã skip " + skipped + " bytes để resume.");
                }

                byte[] buffer = new byte[chunkSize];
                int bytesReadFromFile;

                while (currentOffset < totalFileSize) {
                    bytesReadFromFile = fis.read(buffer);
                    if (bytesReadFromFile == -1) {
                        break;
                    }

                    dos.writeUTF("CMD_UPLOAD_CHUNK");
                    dos.writeUTF(sessionId);
                    dos.writeLong(currentOffset);
                    dos.writeInt(bytesReadFromFile);
                    dos.write(buffer, 0, bytesReadFromFile);
                    dos.flush();

                    String chunkResponse = dis.readUTF();
                    if (!"CHUNK_SUCCESS".equals(chunkResponse)) {
                        System.err.println("Lỗi gửi chunk tại offset " + currentOffset + ": " + chunkResponse);
                        // Khi lỗi chunk, không xóa cache ngay, để lần sau có thể thử resume lại
                        return chunkResponse;
                    }

                    currentOffset += bytesReadFromFile;
                    if (publisher != null) {
                        publisher.publishProgress(new ProgressData(currentOffset, totalFileSize));
                    }
                } // Kết thúc vòng lặp chunk
            }

            // --- Bước 3: Hoàn tất Upload ---
            if (currentOffset == totalFileSize) {
                System.out.println("Đã gửi xong các chunk, gửi lệnh hoàn tất...");
                dos.writeUTF("CMD_COMPLETE_UPLOAD");
                dos.writeUTF(sessionId);
                dos.flush();

                String completeResponse = dis.readUTF();
                if ("UPLOAD_COMPLETE_SUCCESS".equals(completeResponse)) {
                    int newFileId = dis.readInt();
                    System.out.println("Upload thành công! File ID mới: " + newFileId);

                    // *** Xóa session ID khỏi cache khi hoàn thành ***
                    removeCachedSessionId(filePathKey);

                    return "UPLOAD_COMPLETE_SUCCESS";
                } else {
                    System.err.println("Lỗi hoàn tất upload: " + completeResponse);
                    // Không xóa cache nếu hoàn tất lỗi, để có thể thử lại? (Tùy logic)
                    // removeCachedSessionId(filePathKey); 
                    return completeResponse;
                }
            } else {
                System.err.println("Lỗi: Chưa gửi đủ file. Offset cuối: " + currentOffset + ", Tổng size: " + totalFileSize);
                // Không xóa cache để lần sau resume
                return "UPLOAD_FAIL_INCOMPLETE_CLIENT";
            }

        } catch (IOException e) {
            // Khi lỗi IO, không xóa cache, để lần sau resume
            handleIOException(e, "UPLOAD_PROCESS (Session: " + sessionId + ")");
            return "ERROR_IO_UPLOAD";
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi upload (Session: " + sessionId + "): " + e.getMessage());
            e.printStackTrace();
            // Có thể xóa cache khi lỗi nghiêm trọng?
            // removeCachedSessionId(filePathKey); 
            return "ERROR_UNKNOWN_UPLOAD";
        }
    }

    /**
     * Tải file xuống và báo cáo tiến độ.
     */
    public String downloadFile(int fileId, java.io.File fileToSave, ProgressPublisher publisher) {
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }

        try {
            dos.writeUTF("CMD_DOWNLOAD");
            dos.writeInt(fileId);
            dos.flush();

            String startResponse = dis.readUTF();
            if (!"DOWNLOAD_START".equals(startResponse)) {
                return startResponse;
            }

            dis.readUTF(); // Đọc tên file
            long totalFileSize = dis.readLong();

            try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < totalFileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, totalFileSize - totalBytesRead))) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    // Gửi thông tin tiến độ qua "kênh liên lạc"
                    if (publisher != null) {
                        publisher.publishProgress(new ProgressData(totalBytesRead, totalFileSize));
                    }
                }

                return (totalBytesRead == totalFileSize) ? "DOWNLOAD_SUCCESS" : "DOWNLOAD_FAIL_INCOMPLETE";
            }
        } catch (IOException e) {
            handleIOException(e, "CMD_DOWNLOAD");
            return "ERROR_IO_DOWNLOAD";
        }
    }

    /**
     * Yêu cầu server gửi nội dung của một thư mục.
     *
     * @param folderId ID của thư mục cần lấy nội dung. Dùng null cho thư mục
     * gốc.
     * @return Chuỗi phản hồi từ server.
     */
    public String getFolderContent(Integer folderId) {
        String command = "CMD_GET_FOLDER_CONTENT";
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }
        try {
            dos.writeUTF(command);
            // Gửi -1 nếu là thư mục gốc, ngược lại gửi ID của thư mục
            dos.writeInt(folderId == null ? -1 : folderId);
            dos.flush();
            return dis.readUTF();
        } catch (IOException e) {
            handleIOException(e, command);
            return "ERROR_IO_COMMAND";
        }
    }

    /**
     * Gửi yêu cầu tạo một thư mục mới.
     *
     * @param folderName Tên thư mục mới.
     * @param parentFolderId ID của thư mục cha. Dùng null cho thư mục gốc.
     * @return Chuỗi phản hồi từ server.
     */
    public String createFolder(String folderName, Integer parentFolderId) {
        String command = "CMD_CREATE_FOLDER";
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }
        try {
            dos.writeUTF(command);
            dos.writeUTF(folderName);
            dos.writeInt(parentFolderId == null ? -1 : parentFolderId);
            dos.flush();
            return dis.readUTF();
        } catch (IOException e) {
            handleIOException(e, command);
            return "ERROR_IO_COMMAND";
        }
    }

    public String getUploadStatus(String sessionId) {
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }
        if (!isConnected && !connect()) {
            return "ERROR_CONNECTION";
        }

        try {
            dos.writeUTF("CMD_GET_UPLOAD_STATUS");
            dos.writeUTF(sessionId);
            dos.flush();
            String response = dis.readUTF();

            if ("UPLOAD_STATUS".equals(response)) {
                long currentOffset = dis.readLong();
                int chunkSize = dis.readInt();
                // Trả về chuỗi chứa cả 3 thông tin để dễ phân tích
                return "UPLOAD_STATUS:" + currentOffset + ":" + chunkSize;
            }
            return response; // Trả về UPLOAD_NOT_FOUND hoặc lỗi khác
        } catch (IOException e) {
            handleIOException(e, "CMD_GET_UPLOAD_STATUS");
            return "ERROR_IO_COMMAND";
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="File & Share Management Methods">    
    public String deleteFile(int fileId) {
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }
        try {
            dos.writeUTF("CMD_DELETE");
            dos.writeInt(fileId);
            dos.flush();
            return dis.readUTF();
        } catch (IOException e) {
            handleIOException(e, "CMD_DELETE");
            return "ERROR_IO_DELETE";
        }
    }

    public String shareFile(int fileId, String targetUsername, String permission, int expiryMinutes) {
        String command = "SHARE:" + fileId + "|" + targetUsername + "|" + permission + "|" + expiryMinutes;
        return sendCommand(command);
    }

    public String listShares(int fileId) {
        return sendCommand("SHARE_LIST:" + fileId);
    }

    public String unshareFile(int fileId, String targetUsername) {
        return sendCommand("UNSHARE:" + fileId + "|" + targetUsername);
    }

    public String changeSharePermission(int fileId, String targetUsername, int newPermissionLevel, int expiryMinutes) {
        String command = "CHANGE_PERM:" + fileId + "|" + targetUsername + "|" + newPermissionLevel + "|" + expiryMinutes;
        return sendCommand(command);
    }

    private String sendCommand(String command) {
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }
        if (!isConnected) {
            return "ERROR_CONNECTION";
        }
        try {
            dos.writeUTF(command);
            dos.flush();
            return dis.readUTF();
        } catch (IOException e) {
            handleIOException(e, command);
            return "ERROR_IO_COMMAND";
        }
    }

    private void handleIOException(IOException e, String context) {
        System.err.println("Lỗi I/O trong lúc " + context + ": " + e.getMessage());
        disconnect();
    }

    private String getFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            String extension = fileName.substring(dotIndex + 1).toLowerCase();
            switch (extension) {
                case "png":
                    return "image/png";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "pdf":
                    return "application/pdf";
                case "txt":
                    return "text/plain";
            }
        }
        return "application/octet-stream";
    }

    public boolean isLoggedIn() {
        return currentUserId != -1;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Gửi yêu cầu lấy danh sách thư mục con.
     *
     * @param parentFolderId ID của thư mục cha (-1 cho thư mục gốc)
     */
    public String getFolders(int parentFolderId) {
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }

        try {
            dos.writeUTF("CMD_GET_FOLDERS");
            dos.writeInt(parentFolderId);
            dos.flush();
            return dis.readUTF();
        } catch (IOException e) {
            handleIOException(e, "CMD_GET_FOLDERS");
            return "ERROR_IO_COMMAND";
        }
    }

    /**
     * Gửi yêu cầu lấy danh sách file trong một thư mục.
     *
     * @param folderId ID của thư mục (-1 cho thư mục gốc)
     */
    public String getFilesInFolder(int folderId) {
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }

        try {
            dos.writeUTF("CMD_GET_FILES_IN_FOLDER");
            dos.writeInt(folderId);
            dos.flush();
            return dis.readUTF();
        } catch (IOException e) {
            handleIOException(e, "CMD_GET_FILES_IN_FOLDER");
            return "ERROR_IO_COMMAND";
        }
    }

    /**
     * Gửi yêu cầu tạo thư mục mới.
     *
     * @param folderName Tên thư mục mới
     * @param parentFolderId ID của thư mục cha (-1 cho thư mục gốc)
     */
    public String createFolder(String folderName, int parentFolderId) {
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }

        try {
            dos.writeUTF("CMD_CREATE_FOLDER");
            dos.writeUTF(folderName);
            dos.writeInt(parentFolderId);
            dos.flush();
            String response = dis.readUTF(); // Chờ phản hồi (ví dụ: CREATE_FOLDER_SUCCESS)

            if ("CREATE_FOLDER_SUCCESS".equals(response)) {
                try {
                    int newFolderId = dis.readInt(); // Đọc ID mới trả về
                    return "CREATE_FOLDER_SUCCESS:" + newFolderId; // Trả về kèm ID
                } catch (IOException readEx) {
                    System.err.println("Warning: Không thể đọc ID thư mục mới sau khi tạo thành công.");
                    // Vẫn báo thành công nhưng với ID không hợp lệ, client sẽ fallback
                    return "CREATE_FOLDER_SUCCESS:-1";
                }
            }
            return response; // Trả về mã lỗi nếu có
        } catch (IOException e) {
            handleIOException(e, "CMD_CREATE_FOLDER");
            return "ERROR_IO_COMMAND";
        }
    }

    /**
     * Gửi yêu cầu đổi tên thư mục.
     */
    public String renameFolder(int folderId, String newName) {
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }
        try {
            dos.writeUTF("CMD_RENAME_FOLDER");
            dos.writeInt(folderId);
            dos.writeUTF(newName);
            dos.flush();
            return dis.readUTF();
        } catch (IOException e) {
            handleIOException(e, "CMD_RENAME_FOLDER");
            return "ERROR_IO_COMMAND";
        }
    }

    /**
     * Gửi yêu cầu xóa thư mục.
     */
    public String deleteFolder(int folderId) {
        if (!isLoggedIn()) {
            return "ERROR_NOT_LOGGED_IN";
        }
        try {
            dos.writeUTF("CMD_DELETE_FOLDER");
            dos.writeInt(folderId);
            dos.flush();
            return dis.readUTF();
        } catch (IOException e) {
            handleIOException(e, "CMD_DELETE_FOLDER");
            return "ERROR_IO_COMMAND";
        }
    }
    //</editor-fold>
    // --- Helper Methods for Upload Cache ---

    private static final String CACHE_FILE_NAME = "upload_cache.properties";

    /**
     * Tải cache từ file properties.
     */
    private Properties loadUploadCache() {
        Properties cache = new Properties();
        File cacheFile = new File(CACHE_FILE_NAME);
        if (cacheFile.exists()) {
            try (InputStream input = new FileInputStream(cacheFile)) {
                cache.load(input);
            } catch (IOException e) {
                System.err.println("Lỗi đọc file cache upload: " + e.getMessage());
            }
        }
        return cache;
    }

    /**
     * Lưu cache vào file properties.
     */
    private void saveUploadCache(Properties cache) {
        try (OutputStream output = new FileOutputStream(CACHE_FILE_NAME)) {
            cache.store(output, "Upload Session Cache");
        } catch (IOException e) {
            System.err.println("Lỗi lưu file cache upload: " + e.getMessage());
        }
    }

    /**
     * Lấy sessionId đã lưu cho một file path. Dùng file path làm key.
     */
    private String getCachedSessionId(String filePath) {
        Properties cache = loadUploadCache();
        return cache.getProperty(filePath); // Trả về null nếu không có
    }

    /**
     * Lưu sessionId cho một file path.
     */
    private void cacheSessionId(String filePath, String sessionId) {
        Properties cache = loadUploadCache();
        cache.setProperty(filePath, sessionId);
        saveUploadCache(cache);
    }

    /**
     * Xóa sessionId khỏi cache (khi upload xong hoặc thất bại).
     */
    private void removeCachedSessionId(String filePath) {
        Properties cache = loadUploadCache();
        cache.remove(filePath);
        saveUploadCache(cache);
    }

//</editor-fold> //
}
