/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageclient;

import java.io.*;
import java.net.Socket;

public class ClientSocketManager {

    // Hằng số kết nối
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;       // Cổng phải khớp với FileServer.java

    private Socket socket;
    private DataInputStream dis;
    private DataOutputStream dos;

    private int currentUserId = -1; // ID người dùng hiện tại đang đăng nhập
    private boolean isConnected = false;

    // --- QUẢN LÝ KẾT NỐI ---
    /**
     * Thiết lập kết nối đến Server và khởi tạo Streams.
     *
     * @return true nếu kết nối thành công.
     */
    public boolean connect() {
        if (isConnected) {
            return true;
        }
        try {
            socket = new Socket(SERVER_IP, SERVER_PORT);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            isConnected = true;
            System.out.println("Đã kết nối thành công đến Server.");
            return true;
        } catch (IOException e) {
            System.err.println("Lỗi kết nối đến Server: " + e.getMessage());
            isConnected = false;
            return false;
        }
    }

    /**
     * Đóng kết nối và Streams.
     */
    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (dos != null) {
                dos.close();
            }
            if (dis != null) {
                dis.close();
            }
            isConnected = false;
            currentUserId = -1;
            System.out.println("Đã ngắt kết nối.");
        } catch (IOException e) {
            System.err.println("Lỗi đóng kết nối: " + e.getMessage());
        }
    }

    // --- XỬ LÝ AUTHENTICATION ---
    /**
     * Thực hiện yêu cầu đăng ký người dùng mới.
     *
     * @return Chuỗi phản hồi từ Server (REGISTER_SUCCESS/FAIL).
     */
    public String register(String username, String password, String email) {
        if (!isConnected && !connect()) {
            return "ERROR_CONNECTION";
        }
        try {
            dos.writeUTF("CMD_REGISTER");
            dos.writeUTF(username);
            dos.writeUTF(password);
            dos.writeUTF(email);
            dos.flush();
            return dis.readUTF(); // Nhận phản hồi
        } catch (IOException e) {
            System.err.println("Lỗi I/O khi đăng ký: " + e.getMessage());
            return "ERROR_IO";
        }
    }

    /**
     * Thực hiện yêu cầu đăng nhập.
     *
     * @return Chuỗi phản hồi từ Server (LOGIN_SUCCESS/FAIL/ERROR).
     */
    public String login(String username, String password) {
        if (!isConnected && !connect()) {
            return "ERROR_CONNECTION";
        }
        try {
            dos.writeUTF("CMD_LOGIN");
            dos.writeUTF(username);
            dos.writeUTF(password);
            dos.flush();

            String response = dis.readUTF();

            if ("LOGIN_SUCCESS".equals(response)) {
                this.currentUserId = dis.readInt(); // Nhận ID
                String serverUsername = dis.readUTF(); // NHẬN USERNAME TỪ SERVER
                System.out.println("Đăng nhập thành công. User ID: " + currentUserId + ", Username: " + serverUsername);

                // Trả về định dạng mà frmLogin đã sửa để mong đợi
                return "LOGIN_SUCCESS:" + serverUsername;
            }
            return response;
        } catch (IOException e) {
            System.err.println("Lỗi I/O khi đăng nhập: " + e.getMessage());
            return "ERROR_IO";
        }
    }

    // --- XỬ LÝ FILE ---
    /**
     * Gửi một file lên Server.
     *
     * @param fileToUpload Đối tượng java.io.File cần upload.
     * @return Chuỗi phản hồi từ Server.
     */
    public String uploadFile(java.io.File fileToUpload) {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }
        if (!fileToUpload.exists() || !fileToUpload.isFile()) {
            return "ERROR_FILE_NOT_FOUND";
        }

        try (FileInputStream fis = new FileInputStream(fileToUpload)) {
            // 1. Gửi lệnh và Metadata
            dos.writeUTF("CMD_UPLOAD");
            dos.writeUTF(fileToUpload.getName());
            dos.writeLong(fileToUpload.length());
            // Lấy kiểu file cơ bản (Cần thư viện phức tạp hơn cho MIME Type chính xác)
            dos.writeUTF(getFileType(fileToUpload.getName()));
            dos.flush();

            // 2. Gửi Dữ liệu File Vật lý
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) > 0) {
                dos.write(buffer, 0, bytesRead);
            }
            dos.flush(); // Đảm bảo tất cả byte đã được gửi

            // 3. Nhận phản hồi cuối cùng
            String response = dis.readUTF();
            if ("UPLOAD_SUCCESS".equals(response)) {
                dis.readInt();
                return "UPLOAD_SUCCESS";
            }
            return response;

        } catch (IOException e) {
            System.err.println("Lỗi I/O khi upload file: " + e.getMessage());
            return "ERROR_IO_UPLOAD";
        }
    }

    /**
     * Yêu cầu Server gửi file và lưu về máy cục bộ.
     *
     * @param fileName Tên của file cần download trên Server.
     * @param fileToSave Đối tượng File cục bộ để lưu.
     * @return Chuỗi phản hồi (DOWNLOAD_SUCCESS/FAIL).
     */
    public String downloadFile(int fileId, java.io.File fileToSave) {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }

        try {
            // 1. Gửi lệnh và Tên file
            dos.writeUTF("CMD_DOWNLOAD");
            dos.writeInt(fileId); // Gửi tên file thay vì ID
            dos.flush();

            // 2. Đọc phản hồi khởi đầu
            String startResponse = dis.readUTF();
            if (!"DOWNLOAD_START".equals(startResponse)) {
                return startResponse;
            }

            // 3. Nhận Metadata
            // Server sẽ gửi lại tên file và kích thước
            String receivedFileName = dis.readUTF();
            long fileSize = dis.readLong();

            // 4. Mở FileOutputStream để ghi file tại đường dẫn đã chọn
            try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                // 5. Nhận Dữ liệu File Vật lý (Phần logic này giữ nguyên)
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) > 0) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }

                if (totalBytesRead == fileSize) {
                    return "DOWNLOAD_SUCCESS";
                } else {
                    return "DOWNLOAD_FAIL_INCOMPLETE";
                }
            }

        } catch (IOException e) {
            System.err.println("Lỗi I/O khi download file: " + e.getMessage());
            return "ERROR_IO_DOWNLOAD";
        }
    }

    /**
     * Thực hiện yêu cầu đăng xuất và ngắt kết nối.
     */
    public void logout() {
        if (isConnected) {
            try {
                dos.writeUTF("CMD_LOGOUT");
                dos.flush();
            } catch (IOException e) {
                System.err.println("Lỗi gửi lệnh Logout: " + e.getMessage());
            } finally {
                disconnect(); // Đóng kết nối
            }
        }
    }

    /**
     * Yêu cầu Server gửi danh sách file của người dùng hiện tại.
     *
     * @return Chuỗi chứa dữ liệu danh sách file đã định dạng lại
     * (ID|Tên|Size|Date;...) hoặc thông báo lỗi.
     */
    public String listFiles() {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }
        try {
            dos.writeUTF("CMD_LISTFILES");
            dos.flush();

            // 1. Nhận phản hồi khởi đầu (LIST_START hoặc LIST_FAIL)
            String response = dis.readUTF();
            if (!"LIST_START".equals(response)) {
                return response; // Trả về LIST_FAIL hoặc lỗi khác
            }

            // 2. Nhận số lượng file
            int numberOfFiles = dis.readInt();

            StringBuilder fileListString = new StringBuilder("LIST_SUCCESS:"); // Tiền tố mới

            // 3. Lặp lại để đọc dữ liệu cho từng file
            for (int i = 0; i < numberOfFiles; i++) {
                // LƯU Ý: Phải đọc đúng thứ tự Server đã gửi (FileID, FileName, FileSize, UploadDate)
                int fileId = dis.readInt();
                String fileName = dis.readUTF();
                long fileSize = dis.readLong();
                String uploadDate = dis.readUTF(); // Timestamp dưới dạng String

                // Xây dựng chuỗi dữ liệu file mới theo định dạng Client dễ xử lý (ví dụ: | là ngăn cách cột, ; là ngăn cách hàng)
                fileListString.append(fileId).append("|")
                        .append(fileName).append("|")
                        .append(fileSize).append("|")
                        .append(uploadDate);

                if (i < numberOfFiles - 1) {
                    fileListString.append(";"); // Thêm dấu chấm phẩy nếu không phải là file cuối cùng
                }
            }

            // Trả về chuỗi danh sách file đã được định dạng lại
            return fileListString.toString();

        } catch (IOException e) {
            System.err.println("Lỗi I/O khi yêu cầu danh sách file: " + e.getMessage());
            return "ERROR_IO_LIST";
        }
    }

    /**
     * Yêu cầu Server xóa một file theo tên.
     *
     * @param fileName Tên file cần xóa.
     * @return Chuỗi phản hồi từ Server (DELETE_SUCCESS/FAIL).
     */
    public String deleteFile(int fileId) {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }
        try {
            dos.writeUTF("CMD_DELETE");
            dos.writeInt(fileId);
            dos.flush();
            return dis.readUTF(); // Chờ phản hồi: DELETE_SUCCESS hoặc DELETE_FAIL
        } catch (IOException e) {
            System.err.println("Lỗi I/O khi xóa file: " + e.getMessage());
            return "ERROR_IO_DELETE";
        }
    }

    // --- PHƯƠNG THỨC TIỆN ÍCH ---
    // Giả định đơn giản, có thể dùng thư viện Apache Tika để xử lý tốt hơn
    private String getFileType(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == fileName.length() - 1) {
            return "application/octet-stream"; // Loại không xác định
        }
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
            default:
                return "application/octet-stream";
        }
    }

    public boolean isClientConnected() {
        return isConnected;
    }

    public int getCurrentUserId() {
        return currentUserId;
    }
}
