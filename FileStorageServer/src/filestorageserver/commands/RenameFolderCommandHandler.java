package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FolderDAO;
import filestorageserver.ServerActivityListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RenameFolderCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN"); return;
        }
        String oldFolderName = null; // Biến để lưu tên cũ
        try {
            int folderId = dis.readInt();
            String newFolderName = dis.readUTF();

            if (newFolderName == null || newFolderName.trim().isEmpty()) {
                dos.writeUTF("RENAME_FOLDER_FAIL_EMPTY_NAME"); return;
            }

            FolderDAO folderDAO = new FolderDAO();
            
            // Lấy tên cũ TRƯỚC KHI đổi tên
            oldFolderName = folderDAO.getFolderNameById(folderId, session.getCurrentUserId());
            
            boolean success = false;
            // Chỉ thực hiện đổi tên nếu lấy được tên cũ (tức là thư mục tồn tại và có quyền)
            if (oldFolderName != null) {
                 success = folderDAO.renameFolder(folderId, newFolderName.trim(), session.getCurrentUserId());
            }

            if (success) {
                dos.writeUTF("RENAME_FOLDER_SUCCESS");
                listener.onFolderRenamed(session.getCurrentUsername(), oldFolderName, newFolderName.trim()); 
            } else {
                // Kiểm tra xem lỗi có phải do không tìm thấy thư mục/không có quyền không
                if (oldFolderName == null && folderId > 0) { // folderId > 0 để tránh trường hợp gốc (-1)
                    dos.writeUTF("RENAME_FOLDER_FAIL_NOT_FOUND_OR_AUTH");
                } else {
                    // Lỗi khác (tên trùng hoặc lỗi DB)
                    dos.writeUTF("RENAME_FOLDER_FAIL"); 
                }
            }
        } catch (Exception e) {
            dos.writeUTF("RENAME_FOLDER_FAIL_INTERNAL_ERROR");
        } finally {
            dos.flush();
        }
    }
}
