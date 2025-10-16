package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import filestorageserver.model.File;
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

            // Lấy metadata file để kiểm tra sự tồn tại và quyền truy cập
            File fileMetadata = fileDAO.getFileForDownload(fileId, session.getCurrentUserId());

            if (fileMetadata == null) {
                dos.writeUTF("DELETE_FAIL_NOT_FOUND");
                return;
            }

            // Rất quan trọng: Chỉ chủ sở hữu mới có quyền xóa file
            if (fileMetadata.getOwnerId() != session.getCurrentUserId()) {
                dos.writeUTF("DELETE_FAIL_AUTH");
                return;
            }

            java.io.File fileToDelete = new java.io.File(fileMetadata.getFilePath());
            boolean physicalFileDeleted = true;

            // Bước 1: Xóa file vật lý trên ổ đĩa trước
            if (fileToDelete.exists()) {
                physicalFileDeleted = fileToDelete.delete();
            }

            if (physicalFileDeleted) {
                // Bước 2: Nếu xóa file vật lý thành công, tiến hành xóa metadata trong CSDL
                boolean metadataDeleted = fileDAO.deleteFileMetadata(fileId, session.getCurrentUserId());

                if (metadataDeleted) {
                    dos.writeUTF("DELETE_SUCCESS");
                    System.out.println("User " + session.getCurrentUserId() + " đã xóa thành công File ID: " + fileId);
                    listener.onFileDeleted(session.getCurrentUsername(), fileMetadata.getFileName());
                } else {
                    // Trường hợp hiếm gặp nhưng nguy hiểm: file vật lý đã mất nhưng metadata vẫn còn
                    dos.writeUTF("DELETE_FAIL_DB_PHYSICAL_GONE");
                    System.err.println("LỖI KHÔNG ĐỒNG BỘ: Xóa file vật lý thành công nhưng xóa CSDL thất bại cho File ID: " + fileId);
                }
            } else {
                // Nếu xóa file vật lý thất bại, không thực hiện xóa CSDL và báo lỗi
                dos.writeUTF("DELETE_FAIL_PHYSICAL");
                System.err.println("Lỗi xóa file vật lý cho File ID: " + fileId + ". Metadata vẫn còn trong CSDL.");
            }
        } catch (Exception e) {
            System.err.println("Lỗi nội bộ khi xóa file: " + e.getMessage());
            e.printStackTrace();
            dos.writeUTF("DELETE_FAIL_INTERNAL");
        }
    }
}
