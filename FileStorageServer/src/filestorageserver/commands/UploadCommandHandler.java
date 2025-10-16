package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import filestorageserver.model.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import filestorageserver.ServerActivityListener;

/**
 * Xử lý logic cho lệnh tải file lên server (CMD_UPLOAD).
 */
public class UploadCommandHandler implements CommandHandler {

    /**
     * Thư mục gốc trên server để lưu trữ tất cả các file của người dùng. Lưu ý:
     * Trong môi trường sản xuất, đường dẫn này nên được đặt trong một file cấu
     * hình.
     */
    private static final String SERVER_STORAGE_ROOT = "I:/FileStorageRoot/";

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        String fullFilePath = null;
        try {
            // Bước 1: Đọc metadata của file từ client
            String originalFileName = dis.readUTF();
            long fileSize = dis.readLong();
            String fileType = dis.readUTF();

            // Tạo tên file duy nhất và an toàn để lưu trữ trên server
            String uniqueName = UUID.randomUUID().toString() + "_" + originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String userDir = SERVER_STORAGE_ROOT + "user_" + session.getCurrentUserId() + "/";
            fullFilePath = userDir + uniqueName;

            // Đảm bảo thư mục của người dùng tồn tại
            java.io.File directory = new java.io.File(userDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Bước 2: Nhận nội dung file vật lý và ghi ra ổ đĩa
            try (FileOutputStream fos = new FileOutputStream(fullFilePath)) {
                byte[] buffer = new byte[8192]; // Bộ đệm 8KB
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fileSize) {
                    int bytesToRead = (int) Math.min(buffer.length, fileSize - totalBytesRead);
                    bytesRead = dis.read(buffer, 0, bytesToRead);
                    if (bytesRead == -1) {
                        throw new IOException("Client ngắt kết nối đột ngột khi đang upload.");
                    }
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
            }

            // Bước 3: Lưu thông tin metadata của file vào cơ sở dữ liệu
            File fileMetadata = new File();
            fileMetadata.setOwnerId(session.getCurrentUserId());
            fileMetadata.setFileName(originalFileName);
            fileMetadata.setFilePath(fullFilePath);
            fileMetadata.setFileSize(fileSize);
            fileMetadata.setFileType(fileType);
            fileMetadata.setIsShared(false);

            FileDAO fileDAO = new FileDAO();
            int fileId = fileDAO.insertFileMetadata(fileMetadata);

            if (fileId != -1) {
                dos.writeUTF("UPLOAD_SUCCESS");
                dos.writeInt(fileId);
                System.out.println("User " + session.getCurrentUsername() + " đã upload thành công: " + originalFileName);
                // --- THÔNG BÁO SỰ KIỆN UPLOAD THÀNH CÔNG ---
                listener.onFileUploaded(session.getCurrentUsername(), originalFileName);
            } else {
                // Nếu không lưu được vào CSDL, xóa file vật lý đã tạo để tránh rác
                dos.writeUTF("UPLOAD_FAIL_DB");
                new java.io.File(fullFilePath).delete();
                System.err.println("Lỗi CSDL khi chèn metadata. Đã xóa file vật lý: " + fullFilePath);
            }
        } catch (IOException e) {
            System.err.println("Lỗi I/O khi xử lý upload từ user " + session.getCurrentUsername() + ": " + e.getMessage());
            // Xóa file đang upload dở nếu có lỗi xảy ra
            if (fullFilePath != null) {
                new java.io.File(fullFilePath).delete();
            }
            // Ném lại ngoại lệ để ClientHandler biết và đóng kết nối an toàn
            throw e;
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi upload: " + e.getMessage());
            e.printStackTrace();
            dos.writeUTF("UPLOAD_FAIL_SERVER_ERROR");
            if (fullFilePath != null) {
                new java.io.File(fullFilePath).delete();
            }
        }
    }
}
