/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver;

import filestorageserver.UserDAO;
import filestorageserver.model.User;
import filestorageserver.model.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

public class ClientHandler extends Thread {

    // Hằng số chứa thư mục gốc lưu trữ file trên Server
    private static final String SERVER_STORAGE_ROOT = "I:/FileStorageRoot/";

    private final Socket clientSocket;
    private UserDAO userDAO;
    private FileDAO fileDAO;
    private int currentUserId = -1; // ID người dùng hiện tại đang đăng nhập

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
            dos.writeUTF("LOGIN_SUCCESS");
            dos.writeInt(this.currentUserId);
            dos.writeUTF(user.getUsername());
            dos.flush();
            System.out.println("Đăng nhập thành công: " + user.getUsername());
        } else {
            dos.writeUTF("LOGIN_FAIL");
        }
    }

    private void handleRegister(DataInputStream dis, DataOutputStream dos) throws IOException {
        String username = dis.readUTF();
        String password = dis.readUTF();
        String email = dis.readUTF();

        boolean success = userDAO.registerUser(username, password, email);

        if (success) {
            dos.writeUTF("REGISTER_SUCCESS");
        } else {
            dos.writeUTF("REGISTER_FAIL");
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

            // Tạo tên file ngẫu nhiên VÀ an toàn
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

                while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) > 0) {
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
            }

            // 3. Ghi Metadata vào CSDL
            File fileMetadata = new File();
            fileMetadata.setOwnerId(currentUserId);
            fileMetadata.setFileName(originalFileName);
            // BỎ QUA setStoredName() để tránh nhầm lẫn. Ta chỉ dùng fileMetadata.setFilePath()
            fileMetadata.setFilePath(fullFilePath);
            fileMetadata.setFileSize(fileSize);
            fileMetadata.setFileType(fileType); // Tương ứng với mime_type trong CSDL
            fileMetadata.setIsShared(false);

            int fileId = fileDAO.insertFileMetadata(fileMetadata);

            if (fileId != -1) {
                dos.writeUTF("UPLOAD_SUCCESS");
                dos.writeInt(fileId);
                System.out.println("User " + currentUserId + " đã upload thành công: " + originalFileName);
            } else {
                dos.writeUTF("UPLOAD_FAIL_DB");
                // Xóa file vật lý đã lưu nếu không chèn được vào CSDL
                new java.io.File(fullFilePath).delete();
                System.err.println("Lỗi CSDL khi chèn metadata. Đã xóa file vật lý: " + fullFilePath);
            }
        } catch (IOException e) {
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
        File fileMetadata = fileDAO.getFileById(fileId, currentUserId);

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
            java.util.List<filestorageserver.model.File> files = fileDAO.listUserFiles(currentUserId);

            // Bắt đầu bằng phản hồi thành công và số lượng file
            dos.writeUTF("FILE_LIST_SUCCESS");
            dos.writeInt(files.size());

            // Gửi từng file metadata
            for (filestorageserver.model.File file : files) {
                dos.writeInt(file.getFileId());
                // Thay thế ký tự phân cách (nếu cần) và gửi từng trường riêng biệt.
                // Việc gửi từng trường riêng biệt (writeUTF/writeLong/...) an toàn hơn việc tạo một chuỗi lớn, 
                // giúp Client dễ dàng đọc và tránh lỗi phân tách chuỗi.
                dos.writeUTF(file.getFileName());
                dos.writeLong(file.getFileSize());
                dos.writeUTF(file.getUploadedAt().toString());
                // Ta chỉ gửi 4 trường cần thiết cho JTable.
            }

            dos.flush();
            System.out.println("Gửi danh sách " + files.size() + " file thành công cho User: " + currentUserId);

        } catch (Exception e) {
            System.err.println("Lỗi CSDL hoặc I/O khi liệt kê file: " + e.getMessage());
            dos.writeUTF("ERROR_LIST_FAIL");
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
            // Dùng logic DAO nghiêm ngặt: chỉ cho phép lấy nếu là chủ sở hữu HOẶC file được chia sẻ.
            // Sau đó ta kiểm tra lại OwnerId để đảm bảo quyền xóa.
            fileMetadata = fileDAO.getFileById(fileId, currentUserId);

            if (fileMetadata == null || fileMetadata.getOwnerId() != currentUserId) {
                dos.writeUTF("DELETE_FAIL_AUTH");
                return;
            }

            String fullFilePath = fileMetadata.getFilePath();
            java.io.File fileToDelete = new java.io.File(fullFilePath);

            boolean physicalDeleted = true;

            // BƯỚC 3 (Đảo ngược): Xóa file vật lý trước
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
}
