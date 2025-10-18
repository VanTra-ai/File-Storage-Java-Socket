package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FolderDAO;
import filestorageserver.ServerActivityListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DeleteFolderCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }
        String folderName = null; // Biến lưu tên thư mục
        try {
            int folderId = dis.readInt();

            if (folderId == -1) {
                dos.writeUTF("DELETE_FOLDER_FAIL_ROOT");
                return;
            }

            FolderDAO folderDAO = new FolderDAO();

            folderName = folderDAO.getFolderNameById(folderId, session.getCurrentUserId());

            boolean success = false;
            // Chỉ xóa nếu lấy được tên (tức là thư mục tồn tại và có quyền)
            if (folderName != null) {
                success = folderDAO.deleteFolder(folderId, session.getCurrentUserId());
            }

            if (success) {
                dos.writeUTF("DELETE_FOLDER_SUCCESS");
                listener.onFolderDeleted(session.getCurrentUsername(), folderName);
            } else {
                // Kiểm tra xem lỗi có phải do không tìm thấy/không có quyền không
                if (folderName == null && folderId > 0) {
                    dos.writeUTF("DELETE_FOLDER_FAIL_NOT_FOUND_OR_AUTH");
                } else { // Lỗi DB khác
                    dos.writeUTF("DELETE_FOLDER_FAIL_DB_ERROR");
                }
            }
        } catch (Exception e) {
            dos.writeUTF("DELETE_FOLDER_FAIL_INTERNAL_ERROR");
        } finally {
            dos.flush();
        }
    }
}
