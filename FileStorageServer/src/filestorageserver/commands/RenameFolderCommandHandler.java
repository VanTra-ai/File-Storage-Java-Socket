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
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            dos.flush();
            return;
        }
        String oldFolderName = null;
        int folderId = -1;
        try {
            folderId = dis.readInt();
            String newFolderName = dis.readUTF();

            if (newFolderName == null || newFolderName.trim().isEmpty()) {
                dos.writeUTF("RENAME_FOLDER_FAIL_EMPTY_NAME");
                return;
            }

            FolderDAO folderDAO = new FolderDAO();

            // Lấy tên cũ TRƯỚC KHI đổi tên để kiểm tra tồn tại và ghi log
            oldFolderName = folderDAO.getFolderNameById(folderId, session.getCurrentUserId());

            boolean success = false;
            if (oldFolderName != null) { // Chỉ đổi tên nếu thư mục tồn tại và có quyền
                success = folderDAO.renameFolder(folderId, newFolderName.trim(), session.getCurrentUserId());
            }

            if (success) {
                dos.writeUTF("RENAME_FOLDER_SUCCESS");
                listener.onFolderRenamed(session.getCurrentUsername(), oldFolderName, newFolderName.trim());
            } else {
                // Phân biệt lỗi
                if (oldFolderName == null && folderId > 0) { // Lỗi do không tìm thấy/không có quyền ngay từ đầu
                    dos.writeUTF("RENAME_FOLDER_FAIL_NOT_FOUND_OR_AUTH");
                } else { // Lỗi khác (tên trùng hoặc lỗi DB khi UPDATE)
                    dos.writeUTF("RENAME_FOLDER_FAIL_DB_OR_NAME_EXIST");
                }
            }
        } catch (IOException streamEx) {
            System.err.println("Lỗi đọc stream rename folder từ client: " + streamEx.getMessage());
            throw streamEx;
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi đổi tên thư mục (ID: " + folderId + "): " + e.getMessage());
            e.printStackTrace();
            try {
                dos.writeUTF("RENAME_FOLDER_FAIL_INTERNAL_ERROR");
            } catch (IOException ignored) {
            }
        } finally {
            try {
                dos.flush();
            } catch (IOException ignored) {
            }
        }
    }
}
