package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.UploadSessionDAO;
import filestorageserver.FileDAO; // Cần FileDAO để tạo metadata cuối cùng
import filestorageserver.ServerActivityListener;
import filestorageserver.model.File; // Model File chính
import filestorageserver.model.UploadSession;
import filestorageserver.UserDAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID; // Dùng để tạo tên file cuối cùng

public class CompleteUploadCommandHandler implements CommandHandler {

    // Lấy từ UploadCommandHandler cũ - NÊN đưa ra file cấu hình
    private static final String SERVER_STORAGE_ROOT = "I:/FileStorageRoot/";

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            dos.flush();
            return;
        }

        String sessionId = null;
        UploadSession currentSession = null;
        UploadSessionDAO sessionDAO = new UploadSessionDAO();
        Path tempFilePath = null;

        try {
            sessionId = dis.readUTF();
            // String finalChecksum = dis.readUTF(); // Đọc checksum nếu client gửi

            currentSession = sessionDAO.getSession(sessionId);

            if (currentSession == null) {
                dos.writeUTF("UPLOAD_COMPLETE_FAIL_SESSION_NOT_FOUND");
                return;
            }

            tempFilePath = Paths.get(currentSession.getTempFilePath());

            // 1. Kiểm tra file đã hoàn chỉnh chưa?
            if (currentSession.getCurrentOffset() != currentSession.getTotalSize() || !Files.exists(tempFilePath)) {
                dos.writeUTF("UPLOAD_COMPLETE_FAIL_INCOMPLETE");
                sessionDAO.finalizeSessionStatus(sessionId, "ERROR"); // Đánh dấu lỗi
                // Giữ lại file tạm để debug? Hoặc xóa đi.
                // Files.deleteIfExists(tempFilePath);
                return;
            }

            // 2. TODO: Nếu dùng checksum, tính checksum của file tạm và so sánh với finalChecksum
            // 3. Di chuyển file tạm vào thư mục lưu trữ chính
            String finalFileName = UUID.randomUUID().toString() + "_" + currentSession.getFileName().replaceAll("[^a-zA-Z0-9.-]", "_");
            String userDirStr = SERVER_STORAGE_ROOT + "user_" + session.getCurrentUserId() + "/";
            Path userDir = Paths.get(userDirStr);
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
            }
            Path finalFilePath = userDir.resolve(finalFileName);

            try {
                Files.move(tempFilePath, finalFilePath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Đã di chuyển file tạm: " + tempFilePath + " -> " + finalFilePath);
            } catch (IOException moveEx) {
                System.err.println("Lỗi di chuyển file tạm: " + moveEx.getMessage());
                dos.writeUTF("UPLOAD_COMPLETE_FAIL_MOVE_ERROR");
                sessionDAO.finalizeSessionStatus(sessionId, "ERROR");
                // Không xóa file tạm để thử lại?
                return;
            }

            // 4. Tạo metadata trong bảng 'files'
            File fileMetadata = new File();
            fileMetadata.setOwnerId(session.getCurrentUserId());
            fileMetadata.setFileName(currentSession.getFileName());
            fileMetadata.setFilePath(finalFilePath.toString()); // Lưu đường dẫn cuối cùng
            fileMetadata.setFileSize(currentSession.getTotalSize());
            fileMetadata.setFileType("application/octet-stream"); // TODO: Lấy fileType đúng nếu có
            fileMetadata.setIsShared(false);
            fileMetadata.setFolderId(currentSession.getTargetFolderId());

            FileDAO fileDAO = new FileDAO();
            int newFileId = fileDAO.insertFileMetadata(fileMetadata);

            if (newFileId != -1) {
                // THÀNH CÔNG HOÀN TOÀN!
                dos.writeUTF("UPLOAD_COMPLETE_SUCCESS");
                dos.writeInt(newFileId);

                UserDAO userDAO = new UserDAO();
                boolean usageUpdated = userDAO.updateStorageUsed(session.getCurrentUserId(), currentSession.getTotalSize()); // Cộng thêm kích thước file
                if (!usageUpdated) {
                    // Lỗi nghiêm trọng: Không cập nhật được dung lượng sau khi upload!
                    System.err.println("LỖI NGHIÊM TRỌNG: Không thể cập nhật storage_used cho user " + session.getCurrentUserId() + " sau khi upload file ID " + newFileId);
                    // Có thể cần cơ chế rollback hoặc báo cáo?
                }

                sessionDAO.finalizeSessionStatus(sessionId, "COMPLETE");
                listener.onFileUploaded(session.getCurrentUsername(), currentSession.getFileName());
                System.out.println("Hoàn tất upload session: " + sessionId + ", File ID mới: " + newFileId);
            } else {
                // Lỗi nghiêm trọng: File đã di chuyển nhưng không lưu được metadata
                System.err.println("LỖI NGHIÊM TRỌNG: Không thể lưu metadata cho file đã upload: " + finalFilePath);
                dos.writeUTF("UPLOAD_COMPLETE_FAIL_DB_ERROR");
                sessionDAO.finalizeSessionStatus(sessionId, "ERROR");
                // Cần cơ chế xử lý file mồ côi này!
            }

        } catch (IOException streamEx) {
            System.err.println("Lỗi đọc stream complete từ client (session " + sessionId + "): " + streamEx.getMessage());
            throw streamEx;
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi hoàn tất upload (session " + sessionId + "): " + e.getMessage());
            e.printStackTrace();
            try {
                dos.writeUTF("UPLOAD_COMPLETE_FAIL_INTERNAL_ERROR");
            } catch (IOException ignored) {
            }
            // Đánh dấu session lỗi nếu có thể
            if (sessionId != null) {
                sessionDAO.finalizeSessionStatus(sessionId, "ERROR");
            }
            // Xóa file tạm nếu có lỗi?
            // if (tempFilePath != null) try { Files.deleteIfExists(tempFilePath); } catch (IOException delEx) {}
        } finally {
            try {
                dos.flush();
            } catch (IOException ignored) {
            }
        }
    }
}
