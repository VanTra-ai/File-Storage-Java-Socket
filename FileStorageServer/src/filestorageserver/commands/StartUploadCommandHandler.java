package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileServer;
import filestorageserver.UploadSessionDAO;
import filestorageserver.ServerActivityListener;
import filestorageserver.model.UploadSession;
import filestorageserver.UserDAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File; // Sử dụng java.io.File
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class StartUploadCommandHandler implements CommandHandler {

    // Thư mục để lưu file tạm - NÊN đưa ra file cấu hình
    private static final String TEMP_UPLOAD_DIR = "I:/FileStorageTemp/";
    // Kích thước chunk mặc định (ví dụ: 5MB) - NÊN đưa ra file cấu hình
    private static final int DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024;

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            dos.flush();
            return;
        }

        String tempFilePathStr = null;
        try {
            String fileName = dis.readUTF();
            long totalSize = dis.readLong();
            int targetFolderIdInt = dis.readInt();
            Integer targetFolderId = (targetFolderIdInt == -1) ? null : targetFolderIdInt;

            // --- THÊM KIỂM TRA GIỚI HẠN ---
            // 1. Kiểm tra kích thước file tối đa
            if (totalSize > FileServer.MAX_FILE_SIZE_BYTES) { // Sử dụng hằng số từ FileServer
                dos.writeUTF("UPLOAD_START_FAIL_FILE_TOO_LARGE");
                dos.flush();
                System.err.println("User " + session.getCurrentUserId() + " tried to upload file larger than limit: " + totalSize + " bytes");
                return;
            }
            if (totalSize <= 0) { // Không cho upload file rỗng hoặc size âm
                dos.writeUTF("UPLOAD_START_FAIL_INVALID_SIZE");
                dos.flush();
                return;
            }

            // 2. Kiểm tra dung lượng tài khoản
            UserDAO userDAO = new UserDAO();
            long currentUsage = userDAO.getStorageUsed(session.getCurrentUserId());
            if (currentUsage == -1) { // Lỗi lấy dung lượng
                dos.writeUTF("UPLOAD_START_FAIL_INTERNAL_ERROR");
                dos.flush();
                return;
            }
            if (currentUsage + totalSize > FileServer.USER_QUOTA_BYTES) { // Vượt quá quota
                dos.writeUTF("UPLOAD_START_FAIL_QUOTA_EXCEEDED");
                dos.flush();
                System.err.println("User " + session.getCurrentUserId() + " quota exceeded. Current: " + currentUsage + ", Trying to add: " + totalSize);
                return;
            }

            // 3. (Tùy chọn) Gửi cảnh báo nếu gần đầy
            if ((currentUsage + totalSize) >= (FileServer.USER_QUOTA_BYTES * FileServer.QUOTA_WARNING_THRESHOLD)) {
                System.out.println("CẢNH BÁO: User " + session.getCurrentUserId() + " is nearing quota limit.");
                // Có thể gửi một mã đặc biệt về client nếu muốn client hiển thị cảnh báo
                // dos.writeUTF("QUOTA_WARNING"); // Ví dụ
            }
            // Đảm bảo thư mục tạm tồn tại TRƯỚC KHI resolve file path
            Path tempDir = Paths.get(TEMP_UPLOAD_DIR);
            if (!Files.exists(tempDir)) {
                try {
                    Files.createDirectories(tempDir);
                    System.out.println("Đã tạo thư mục tạm: " + TEMP_UPLOAD_DIR);
                } catch (IOException createDirEx) {
                    System.err.println("Lỗi nghiêm trọng: Không thể tạo thư mục tạm: " + TEMP_UPLOAD_DIR);
                    dos.writeUTF("UPLOAD_START_FAIL_INTERNAL_ERROR"); // Báo lỗi chung
                    dos.flush();
                    return;
                }
            }

            // Tạo tên file tạm duy nhất
            String tempFileName = UUID.randomUUID().toString() + "_" + fileName.replaceAll("[^a-zA-Z0-9.-]", "_") + ".tmp";
            Path tempFilePath = tempDir.resolve(tempFileName);
            tempFilePathStr = tempFilePath.toString();

            // Tạo file tạm rỗng
            File tempFile = tempFilePath.toFile();
            if (!tempFile.createNewFile()) {
                throw new IOException("Không thể tạo file tạm: " + tempFilePathStr);
            }

            // Tạo session trong CSDL
            UploadSessionDAO sessionDAO = new UploadSessionDAO();
            UploadSession newSession = sessionDAO.createSession(
                    session.getCurrentUserId(),
                    fileName.trim(),
                    tempFilePathStr,
                    totalSize,
                    DEFAULT_CHUNK_SIZE,
                    targetFolderId
            );

            if (newSession != null) {
                // Phản hồi thành công về client
                dos.writeUTF("UPLOAD_STARTED");
                dos.writeUTF(newSession.getSessionId());
                dos.writeInt(DEFAULT_CHUNK_SIZE);
                System.out.println("Bắt đầu upload session: " + newSession.getSessionId() + " cho file: " + fileName);
            } else {
                // Lỗi tạo session CSDL
                dos.writeUTF("UPLOAD_START_FAIL_DB_ERROR");
                // Xóa file tạm đã lỡ tạo
                Files.deleteIfExists(tempFilePath);
            }

        } catch (IOException ioEx) {
            System.err.println("Lỗi I/O khi bắt đầu upload: " + ioEx.getMessage());
            dos.writeUTF("UPLOAD_START_FAIL_IO_ERROR");
            // Xóa file tạm nếu đã tạo
            if (tempFilePathStr != null) {
                try {
                    Files.deleteIfExists(Paths.get(tempFilePathStr));
                } catch (IOException delEx) {
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi bắt đầu upload: " + e.getMessage());
            e.printStackTrace();
            dos.writeUTF("UPLOAD_START_FAIL_INTERNAL_ERROR");
            // Xóa file tạm nếu đã tạo
            if (tempFilePathStr != null) {
                try {
                    Files.deleteIfExists(Paths.get(tempFilePathStr));
                } catch (IOException delEx) {
                }
            }
        } finally {
            dos.flush();
        }
    }
}
