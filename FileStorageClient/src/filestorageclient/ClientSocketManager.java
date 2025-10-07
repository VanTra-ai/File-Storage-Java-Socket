/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageclient;

import java.io.*;
import java.net.Socket;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

public class ClientSocketManager {

    // Hằng số kết nối
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

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
        String absoluteTrustStorePath = "C:\\Users\\Admin\\OneDrive\\Documents\\NetBeansProjects\\File-Storage-Java-Socket\\Drivers\\SSL\\client.jks";
        try {
            // Đặt đường dẫn TrustStore (Drivers/SSL/client.jks)
            System.setProperty("javax.net.ssl.trustStore", absoluteTrustStorePath);
            // Đặt mật khẩu TrustStore
            System.setProperty("javax.net.ssl.trustStorePassword", "123456"); // SỬ DỤNG MẬT KHẨU ĐÃ ĐẶT

            // TẠO SSLSocket
            SocketFactory sf = SSLSocketFactory.getDefault();
            socket = sf.createSocket(SERVER_IP, SERVER_PORT);
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            isConnected = true;
            System.out.println("Đã kết nối SECURE thành công đến Server.");
            return true;
        } catch (IOException e) {
            System.err.println("Lỗi kết nối SSL đến Server: " + e.getMessage());
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

    public String downloadFile(int fileId, java.io.File fileToSave) {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }

        try {
            // 1. Gửi lệnh và Tên file
            dos.writeUTF("CMD_DOWNLOAD");
            dos.writeInt(fileId); // Gửi ID file (int)
            dos.flush();

            // 2. Đọc phản hồi khởi đầu
            String startResponse = dis.readUTF();
            if (!"DOWNLOAD_START".equals(startResponse)) {
                return startResponse;
            }

            // 3. Nhận Metadata
            String receivedFileName = dis.readUTF();
            long fileSize = dis.readLong();

            // 4. Mở FileOutputStream để ghi file tại đường dẫn đã chọn
            try (FileOutputStream fos = new FileOutputStream(fileToSave)) {
                // 5. Nhận Dữ liệu File Vật lý
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
     * (ID|Tên|Size|Date|Status;...) hoặc thông báo lỗi.
     *
     * 🔥 ĐÃ SỬA: Đồng bộ với giao thức trả về chuỗi của Server.
     */
    public String listFiles() {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }
        try {
            dos.writeUTF("CMD_LISTFILES");
            dos.flush();

            // Server sẽ gửi một chuỗi duy nhất:
            // - Thành công: "FILELIST_START:ID|Name|Size|Date|Status;..."
            // - Thất bại: "FILELIST_FAIL_SERVER_ERROR"
            String response = dis.readUTF();

            if (response.startsWith("FILELIST_START:")) {
                // Trả về chuỗi dữ liệu (có thể dùng subString để loại bỏ tiền tố nếu cần ở lớp ngoài)
                return response;
            } else {
                return response; // Trả về mã lỗi
            }

        } catch (IOException e) {
            System.err.println("Lỗi I/O khi yêu cầu danh sách file: " + e.getMessage());
            return "ERROR_IO_LIST";
        }
    }

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

    /**
     * Gửi yêu cầu chia sẻ file 🔥 ĐÃ SỬA: Loại bỏ throws IOException, sử dụng
     * sendCommand mới
     */
    public String shareFile(int fileId, String targetUsername, String permission) {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }
        // Lệnh SHARE: [ID file]| [Tên người nhận]| [Quyền hạn (Download Only)]
        String command = "SHARE:" + fileId + "|" + targetUsername + "|" + permission;
        return sendCommand(command);
    }

    /**
     * Gửi yêu cầu lấy danh sách người dùng được chia sẻ file 🔥 ĐÃ SỬA: Loại bỏ
     * throws IOException, sử dụng sendCommand mới
     */
    public String listShares(int fileId) {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }
        // Lệnh SHARE_LIST: [ID file]
        String command = "SHARE_LIST:" + fileId;
        return sendCommand(command);
    }

    /**
     * Gửi yêu cầu hủy chia sẻ
     *
     * @param fileId ID file sở hữu
     * @param targetUsername Username người nhận đã được chia sẻ
     * @return Chuỗi phản hồi từ Server 🔥 ĐÃ SỬA: Đồng bộ với định dạng Server
     * yêu cầu: FileID|TargetUsername
     */
    public String unshareFile(int fileId, String targetUsername) {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }
        // Lệnh UNSHARE: [FileID]| [TargetUsername]
        String command = "UNSHARE:" + fileId + "|" + targetUsername;
        return sendCommand(command);
    }

    /**
     * Gửi một lệnh dạng chuỗi đến Server và chờ phản hồi.
     *
     * @param command Chuỗi lệnh cần gửi
     * @return Chuỗi phản hồi từ Server hoặc mã lỗi cục bộ. 🔥 ĐÃ SỬA: Bắt
     * IOException tại đây thay vì ném lên.
     */
    public String sendCommand(String command) {
        if (!isConnected) {
            return "ERROR_CONNECTION";
        }
        try {
            // Gửi lệnh dạng chuỗi
            dos.writeUTF(command);
            dos.flush();

            // Đọc phản hồi từ Server
            return dis.readUTF();
        } catch (IOException e) {
            System.err.println("Lỗi I/O khi gửi lệnh (" + command + "): " + e.getMessage());
            return "ERROR_IO_COMMAND";
        }
    }

    /**
     * Gửi yêu cầu cập nhật quyền chia sẻ cho một người dùng cụ thể.
     *
     * * @param fileId ID của file sở hữu
     * @param targetUsername Username người nhận cần thay đổi quyền
     * @param newPermissionLevel Mức quyền mới (1: Read-Only, 2: Read-Write,...)
     * @return Chuỗi phản hồi từ Server (ví dụ: "UPDATE_SUCCESS",
     * "UPDATE_FAIL_AUTH",...)
     */
    public String changeSharePermission(int fileId, String targetUsername, int newPermissionLevel) {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }

        // Định dạng lệnh: CHANGE_PERM:[FileID]|[TargetUsername]|[NewPermissionLevel]
        String command = "CHANGE_PERM:" + fileId + "|" + targetUsername + "|" + newPermissionLevel;

        // Sử dụng hàm sendCommand đã có để gửi lệnh và nhận phản hồi
        String response = sendCommand(command);

        // Chỉ log lỗi I/O nếu sendCommand trả về lỗi I/O chung
        if (response.equals("ERROR_IO_COMMAND") || response.equals("ERROR_CONNECTION")) {
            System.err.println("Lỗi khi gọi changeSharePermission. Mã lỗi: " + response);
        }

        return response;
    }

    public boolean isClientConnected() {
        return isConnected;
    }

    public int getCurrentUserId() {
        return currentUserId;
    }
}
