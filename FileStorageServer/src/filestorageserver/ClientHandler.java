/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver;

import filestorageserver.model.User;
import filestorageserver.model.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;
import java.util.List;

public class ClientHandler extends Thread {

    // Hằng số chứa thư mục gốc lưu trữ file trên Server
    private static final String SERVER_STORAGE_ROOT = "I:/FileStorageRoot/";

    private final Socket clientSocket;
    private UserDAO userDAO;
    private FileDAO fileDAO;
    private int currentUserId = -1; // ID người dùng hiện tại đang đăng nhập
    private String currentUsername = null; // Tên người dùng hiện tại (Hữu ích cho logging)

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.userDAO = new UserDAO();
        this.fileDAO = new FileDAO();
        // Đảm bảo thư mục gốc tồn tại khi server chạy
        new java.io.File(SERVER_STORAGE_ROOT).mkdirs();
    }

    @Override
    public void run() {
        // Sử dụng try-with-resources để tự động đóng Stream và Socket
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            while (clientSocket.isConnected()) {
                String command = dis.readUTF();

                // Kiểm tra nếu là lệnh dạng chuỗi (SHARE, UNSHARE, SHARE_LIST)
                if (command.startsWith("SHARE:") || command.startsWith("UNSHARE:") || command.startsWith("SHARE_LIST:") || command.startsWith("CHANGE_PERM:")) {
                    // Dùng hàm processCommand để xử lý các lệnh chuỗi
                    String response = processCommand(command);
                    dos.writeUTF(response);
                    dos.flush();
                    continue; // Tiếp tục vòng lặp
                }

                // Nếu là lệnh cũ CMD_ (Login, Upload, v.v.), sử dụng switch
                switch (command) {
                    case "CMD_LOGIN":
                        handleLogin(dis, dos);
                        break;
                    case "CMD_REGISTER":
                        handleRegister(dis, dos);
                        break;
                    case "CMD_UPLOAD":
                        handleUpload(dis, dos);
                        break;
                    case "CMD_DOWNLOAD":
                        handleDownload(dis, dos);
                        break;
                    case "CMD_LISTFILES":
                        handleListFiles(dos);
                        break;
                    case "CMD_DELETE":
                        handleDelete(dis, dos);
                        break;
                    default:
                        dos.writeUTF("UNKNOWN_COMMAND");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected or I/O error: " + clientSocket.getInetAddress());
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleLogin(DataInputStream dis, DataOutputStream dos) throws IOException {
        String username = dis.readUTF();
        String password = dis.readUTF();

        User user = userDAO.login(username, password);

        if (user != null) {
            this.currentUserId = user.getUserId();
            this.currentUsername = user.getUsername(); // Lưu lại Username
            dos.writeUTF("LOGIN_SUCCESS");
            dos.writeInt(this.currentUserId);
            dos.writeUTF(this.currentUsername);
            dos.flush();
            System.out.println("Đăng nhập thành công: " + this.currentUsername);
        } else {
            dos.writeUTF("LOGIN_FAIL");
        }
    }

    private void handleRegister(DataInputStream dis, DataOutputStream dos) throws IOException {
        String username = dis.readUTF();
        String password = dis.readUTF();
        String email = dis.readUTF();

        // Gọi phương thức UserDAO.registerUser() mới trả về String (Mã trạng thái)
        String result = userDAO.registerUser(username, password, email);

        // Gửi mã trạng thái chi tiết về Client
        dos.writeUTF(result); // Gửi trực tiếp mã lỗi/thành công về Client
        dos.flush();

        if (result.equals("REGISTER_SUCCESS")) {
            System.out.println("Đăng ký thành công User: " + username);
        } else if (result.equals("REGISTER_FAIL_USERNAME_EXIST")) {
            System.err.println("Đăng ký thất bại: Username " + username + " đã tồn tại.");
        } else if (result.equals("REGISTER_FAIL_EMAIL_EXIST")) {
            System.err.println("Đăng ký thất bại: Email " + email + " đã được sử dụng.");
        } else {
            // Bao gồm REGISTER_FAIL_DB_ERROR và REGISTER_FAIL_INTERNAL_ERROR
            System.err.println("Đăng ký thất bại cho User: " + username + ". Lỗi nội bộ hoặc CSDL: " + result);
        }
    }

    // XỬ LÝ LỆNH UPLOAD (CMD_UPLOAD)
    private void handleUpload(DataInputStream dis, DataOutputStream dos) throws IOException {
        if (currentUserId == -1) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        String fullFilePath = null; // Khởi tạo để dùng trong khối catch

        try {
            // 1. Đọc Metadata từ Client
            String originalFileName = dis.readUTF();
            long fileSize = dis.readLong();
            String fileType = dis.readUTF();

            // Tạo tên file ngẫu nhiên và an toàn
            String uniqueName = UUID.randomUUID().toString() + "_" + originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String userDir = SERVER_STORAGE_ROOT + "user_" + currentUserId + "/";
            fullFilePath = userDir + uniqueName; // Cập nhật fullFilePath

            // Đảm bảo thư mục của người dùng tồn tại
            java.io.File directory = new java.io.File(userDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 2. Nhận Dữ liệu File Vật lý
            try (FileOutputStream fos = new FileOutputStream(fullFilePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fileSize) {
                    int bytesRemaining = (int) Math.min(buffer.length, fileSize - totalBytesRead);
                    bytesRead = dis.read(buffer, 0, bytesRemaining);

                    if (bytesRead == -1) {
                        throw new IOException("Client closed stream unexpectedly, file is incomplete.");
                    }

                    if (bytesRead > 0) {
                        fos.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;
                    }
                }

                if (totalBytesRead != fileSize) {
                    throw new IOException("File size mismatch: Expected " + fileSize + " bytes but received " + totalBytesRead + " bytes.");
                }
            }

            // 3. Ghi Metadata vào CSDL
            File fileMetadata = new File();
            fileMetadata.setOwnerId(currentUserId);
            fileMetadata.setFileName(originalFileName);
            fileMetadata.setFilePath(fullFilePath);
            fileMetadata.setFileSize(fileSize);
            fileMetadata.setFileType(fileType); // Tương ứng với mime_type trong CSDL
            fileMetadata.setIsShared(false);

            int fileId = fileDAO.insertFileMetadata(fileMetadata);

            if (fileId != -1) {
                dos.writeUTF("UPLOAD_SUCCESS");
                dos.writeInt(fileId);
                dos.flush(); // Đảm bảo phản hồi được gửi ngay lập tức
                System.out.println("User " + currentUserId + " đã upload thành công: " + originalFileName);
            } else {
                dos.writeUTF("UPLOAD_FAIL_DB");
                // Xóa file vật lý đã lưu nếu không chèn được vào CSDL
                new java.io.File(fullFilePath).delete();
                System.err.println("Lỗi CSDL khi chèn metadata. Đã xóa file vật lý: " + fullFilePath);
            }
        } catch (IOException e) {
            // Gửi phản hồi thất bại cho Client
            dos.writeUTF("UPLOAD_FAIL_IO");
            System.err.println("Lỗi I/O khi xử lý upload: " + e.getMessage());
            if (fullFilePath != null) {
                // Đảm bảo xóa file vật lý nếu quá trình truyền file bị lỗi I/O
                new java.io.File(fullFilePath).delete();
            }
        }
    }

    // XỬ LÝ LỆNH DOWNLOAD (CMD_DOWNLOAD)
    private void handleDownload(DataInputStream dis, DataOutputStream dos) throws IOException {
        if (currentUserId == -1) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        int fileId = dis.readInt();
        // Dùng fileDAO.getFileForDownload để kiểm tra quyền truy cập (bao gồm cả file sở hữu và file được chia sẻ)
        File fileMetadata = fileDAO.getFileForDownload(fileId, currentUserId);

        if (fileMetadata == null) {
            dos.writeUTF("DOWNLOAD_FAIL_AUTH");
            return;
        }

        java.io.File fileToDownload = new java.io.File(fileMetadata.getFilePath());

        if (!fileToDownload.exists() || !fileToDownload.isFile()) {
            dos.writeUTF("DOWNLOAD_FAIL_NOT_FOUND");
            return;
        }

        try {
            dos.writeUTF("DOWNLOAD_START");
            dos.writeUTF(fileMetadata.getFileName());
            dos.writeLong(fileMetadata.getFileSize());
            dos.flush();

            try (FileInputStream fis = new FileInputStream(fileToDownload)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) > 0) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            System.out.println("User " + currentUserId + " đã download thành công: " + fileMetadata.getFileName());
        } catch (IOException e) {
            System.err.println("Lỗi I/O khi xử lý download: " + e.getMessage());
        }
    }

    // XỬ LÝ LỆNH LIỆT KÊ FILE (CMD_LISTFILES)
    private void handleListFiles(DataOutputStream dos) throws IOException {
        if (currentUserId == -1) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        try {
            // Lấy danh sách file (bao gồm file sở hữu và file được chia sẻ)
            List<File> files = fileDAO.listUserFiles(currentUserId);

            StringBuilder sb = new StringBuilder();

            // Duyệt qua từng File và ghép dữ liệu thành chuỗi format: ID|Name|Size|Date|Status;
            for (File file : files) {

                // Định dạng lại ngày tháng 
                String dateString = "N/A";
                if (file.getUploadedAt() != null) {
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                    dateString = dateFormat.format(file.getUploadedAt());
                }

                // Xác định Trạng thái (Status)
                // file.isIsSharedToMe() được set trong FileDAO
                String status = file.isIsSharedToMe() ? "Shared" : "Owned";
                // Lấy tên người chia sẻ (sẽ là "" nếu là Owned)
                String sharer = file.getSharerName();

                // Format: ID|Name|Size|Date|Status;
                sb.append(file.getFileId()).append("|")
                        .append(file.getFileName()).append("|")
                        .append(file.getFileSize()).append("|")
                        .append(dateString).append("|")
                        .append(status).append("|") // Thêm cột Status
                        .append(sharer).append(";");
            }

            // Gửi chuỗi kết quả cuối cùng với tiền tố nhận dạng
            String finalData = "FILELIST_START:" + sb.toString();
            dos.writeUTF(finalData);
            dos.flush();

            System.out.println("Gửi danh sách " + files.size() + " file thành công cho User: " + currentUserId);

        } catch (Exception e) {
            System.err.println("Lỗi CSDL hoặc I/O khi liệt kê file: " + e.getMessage());
            // Gửi một mã lỗi duy nhất dưới dạng chuỗi UTF
            dos.writeUTF("FILELIST_FAIL_SERVER_ERROR");
            dos.flush();
        }
    }

    // XỬ LÝ LỆNH XÓA FILE (CMD_DELETE)
    private void handleDelete(DataInputStream dis, DataOutputStream dos) throws IOException {
        if (currentUserId == -1) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        int fileId = -1;
        File fileMetadata = null;

        try {
            // 1. Đọc ID file cần xóa từ Client
            fileId = dis.readInt();

            // 2. Kiểm tra quyền sở hữu và lấy metadata
            fileMetadata = fileDAO.getFileForDownload(fileId, currentUserId);

            if (fileMetadata == null) {
                // File không tồn tại hoặc người dùng không có quyền truy cập.
                dos.writeUTF("DELETE_FAIL_NOT_FOUND");
                return;
            }

            if (fileMetadata.getOwnerId() != currentUserId) {
                // Người dùng có thể download (do được chia sẻ) nhưng không có quyền xóa.
                dos.writeUTF("DELETE_FAIL_AUTH");
                return;
            }

            String fullFilePath = fileMetadata.getFilePath();
            java.io.File fileToDelete = new java.io.File(fullFilePath);

            boolean physicalDeleted = true;

            // BƯỚC 3: Xóa file vật lý trước
            if (fileToDelete.exists()) {
                physicalDeleted = fileToDelete.delete();
            }

            if (physicalDeleted) {
                // BƯỚC 4: Nếu xóa vật lý thành công, tiến hành xóa metadata
                boolean metadataDeleted = fileDAO.deleteFileMetadata(fileId, currentUserId);

                if (metadataDeleted) {
                    dos.writeUTF("DELETE_SUCCESS");
                    System.out.println("User " + currentUserId + " đã xóa thành công File ID: " + fileId);
                } else {
                    // CSDL thất bại nhưng file vật lý đã bị xóa -> Vấn đề nghiêm trọng (File mồ côi metadata)
                    dos.writeUTF("DELETE_FAIL_DB_PHYSICAL_GONE");
                    System.err.println("LỖI KHÔNG ĐỒNG BỘ: Xóa file vật lý thành công nhưng xóa CSDL thất bại cho File ID: " + fileId);
                }
            } else {
                // Xóa vật lý thất bại
                dos.writeUTF("DELETE_FAIL_PHYSICAL");
                System.err.println("Lỗi xóa file vật lý cho File ID: " + fileId + ". File vẫn còn trong CSDL.");
            }

        } catch (IOException e) {
            System.err.println("Lỗi I/O khi xử lý delete: " + e.getMessage());
        } catch (Exception e) {
            try {
                dos.writeUTF("DELETE_FAIL_INTERNAL");
            } catch (IOException ignored) {
            }
            System.err.println("Lỗi nội bộ khi xóa file: " + e.getMessage());
        }
    }

    // =========================================================================
    // XỬ LÝ CÁC LỆNH CHUỖI (SHARE/UNSHARE/SHARE_LIST) ĐÃ ĐƯỢC CẬP NHẬT
    // =========================================================================
    /**
     * Xử lý các lệnh dạng chuỗi (String Command) từ ClientSocketManager.
     */
    private String processCommand(String command) {
        if (currentUserId == -1) {
            return "ERROR_NOT_LOGGED_IN";
        }

        // --- LỆNH SHARE ---
        if (command.startsWith("SHARE:")) {
            // Định dạng: SHARE:FileID|TargetUsername|Permission
            try {
                String data = command.substring("SHARE:".length());
                String[] parts = data.split("\\|");
                if (parts.length != 3) {
                    return "SHARE_FAIL_INVALID_FORMAT";
                }

                int fileId = Integer.parseInt(parts[0]);
                String targetUsername = parts[1];
                int permissionLevel = Integer.parseInt(parts[2]);

                // 🔥 SỬ DỤNG PHƯƠNG THỨC MỚI CỦA FIEDAO (Trả về mã lỗi string)
                String result = fileDAO.shareFile(fileId, currentUserId, targetUsername, permissionLevel);

                if (result.equals("SHARE_SUCCESS")) {
                    System.out.printf("User %d đã chia sẻ File ID %d với User %s thành công.\n", currentUserId, fileId, targetUsername);
                } else {
                    System.err.printf("User %d chia sẻ File ID %d với User %s thất bại. Mã lỗi: %s\n", currentUserId, fileId, targetUsername, result);
                }

                return result;

            } catch (NumberFormatException ex) {
                return "SHARE_FAIL_INVALID_FORMAT";
            }
        } // --- LỆNH SHARE_LIST (CMD_GETSHARELIST) ---
        else if (command.startsWith("SHARE_LIST:")) {
            // Định dạng: SHARE_LIST:FileID
            try {
                int fileId = Integer.parseInt(command.substring("SHARE_LIST:".length()));

                // 🔥 SỬ DỤNG PHƯƠNG THỨC MỚI CỦA FIEDAO (Trả về chuỗi kết quả có tiền tố)
                String result = fileDAO.getSharedUsersByFile(fileId, currentUserId);

                if (result.startsWith("SHARELIST_START")) {
                    System.out.printf("User %d đã lấy danh sách chia sẻ File ID %d thành công.\n", currentUserId, fileId);
                } else {
                    System.err.printf("User %d lấy danh sách chia sẻ File ID %d thất bại. Mã lỗi: %s\n", currentUserId, fileId, result);
                }

                // Trả về chuỗi kết quả (SHARELIST_START:..., SHARELIST_EMPTY, SHARELIST_FAIL_AUTH,...)
                return result;

            } catch (NumberFormatException ex) {
                return "SHARELIST_FAIL_INVALID_FORMAT";
            }
        } // --- LỆNH UNSHARE (Đã sửa để dùng FileID và TargetUsername) ---
        else if (command.startsWith("UNSHARE:")) {
            // Định dạng MỚI: UNSHARE:FileID|TargetUsername
            try {
                String data = command.substring("UNSHARE:".length());
                String[] parts = data.split("\\|");
                if (parts.length != 2) {
                    return "UNSHARE_FAIL_INVALID_FORMAT";
                }

                int fileId = Integer.parseInt(parts[0]);
                String targetUsername = parts[1];

                // 🔥 SỬ DỤNG PHƯƠNG THỨC MỚI CỦA FIEDAO (Trả về mã lỗi string)
                String result = fileDAO.unshareFile(fileId, currentUserId, targetUsername);

                if (result.equals("UNSHARE_SUCCESS")) {
                    System.out.printf("User %d đã hủy chia sẻ File ID %d với User %s thành công.\n", currentUserId, fileId, targetUsername);
                } else {
                    System.err.printf("User %d hủy chia sẻ File ID %d với User %s thất bại. Mã lỗi: %s\n", currentUserId, fileId, targetUsername, result);
                }

                return result;

            } catch (NumberFormatException ex) {
                return "UNSHARE_FAIL_INVALID_FORMAT";
            }
        } else if (command.startsWith("CHANGE_PERM:")) {
            // Định dạng: CHANGE_PERM:FileID|TargetUsername|NewPermissionLevel
            try {
                String data = command.substring("CHANGE_PERM:".length());
                String[] parts = data.split("\\|");
                if (parts.length != 3) {
                    return "UPDATE_FAIL_INVALID_FORMAT";
                }

                int fileId = Integer.parseInt(parts[0]);
                String targetUsername = parts[1];
                int newPermissionLevel = Integer.parseInt(parts[2]);
                // 🔥 GỌI HÀM DAO MỚI ĐỂ CẬP NHẬT QUYỀN
                String result = fileDAO.updateFileSharePermission(fileId, currentUserId, targetUsername, newPermissionLevel);
                if (result.equals("UPDATE_SUCCESS")) {
                    System.out.printf("User %d đã cập nhật quyền của User %s trên File ID %d thành Level %d thành công.\n", currentUserId, targetUsername, fileId, newPermissionLevel);
                } else {

                    System.err.printf("User %d cập nhật quyền thất bại trên File ID %d. Mã lỗi: %s\n", currentUserId, fileId, result);
                }

                return result;
            } catch (NumberFormatException ex) {
                return "UPDATE_FAIL_INVALID_FORMAT";
            }
        }

        return "UNKNOWN_STRING_COMMAND";
    }
}
