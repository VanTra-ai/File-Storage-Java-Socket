package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import filestorageserver.model.File;
import filestorageserver.UserDAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import filestorageserver.ServerActivityListener;

/**
 * Xử lý logic cho lệnh xóa file (CMD_DELETE).
 */
public class DeleteCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        try {
            int fileId = dis.readInt();
            FileDAO fileDAO = new FileDAO();
            UserDAO userDAO = new UserDAO(); // Khởi tạo UserDAO

            // --- SỬA LOGIC XÓA VÀ CẬP NHẬT DUNG LƯỢNG ---
            // Gọi hàm deleteFileMetadata mới trả về kích thước file
            long deletedFileSize = fileDAO.deleteFileMetadata(fileId, session.getCurrentUserId());

            if (deletedFileSize > 0) { // Xóa thành công và có kích thước > 0
                dos.writeUTF("DELETE_SUCCESS");

                // Cập nhật storage_used (trừ đi kích thước file)
                boolean usageUpdated = userDAO.updateStorageUsed(session.getCurrentUserId(), -deletedFileSize); // Dùng số âm
                if (!usageUpdated) {
                    System.err.println("LỖI NGHIÊM TRỌNG: Không thể cập nhật storage_used cho user " + session.getCurrentUserId() + " sau khi xóa file ID " + fileId);
                }

                // Ghi log (cần lấy tên file trước khi xóa nếu muốn log tên)
                System.out.println("User " + session.getCurrentUserId() + " đã xóa thành công File ID: " + fileId);
                // listener.onFileDeleted(session.getCurrentUsername(), fileName); // Cần lấy fileName trước

            } else if (deletedFileSize == 0) { // Không tìm thấy file hoặc không có quyền
                dos.writeUTF("DELETE_FAIL_NOT_FOUND_OR_AUTH");
            } else { // deletedFileSize == -1 (Lỗi CSDL)
                dos.writeUTF("DELETE_FAIL_DB_ERROR");
                System.err.println("Lỗi CSDL khi xóa file ID: " + fileId);
            }
        } catch (Exception e) {
            System.err.println("Lỗi nội bộ khi xóa file: " + e.getMessage());
            e.printStackTrace();
            dos.writeUTF("DELETE_FAIL_INTERNAL");
        }
    }
}
