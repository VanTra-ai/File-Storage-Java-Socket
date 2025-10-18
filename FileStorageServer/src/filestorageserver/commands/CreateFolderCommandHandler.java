package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FolderDAO;
import filestorageserver.ServerActivityListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Xử lý lệnh CMD_CREATE_FOLDER để tạo thư mục mới.
 */
public class CreateFolderCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        try {
            String folderName = dis.readUTF();
            int parentFolderIdInt = dis.readInt(); // Client gửi -1 cho thư mục gốc
            Integer parentFolderId = (parentFolderIdInt == -1) ? null : parentFolderIdInt;

            if (folderName == null || folderName.trim().isEmpty()) {
                dos.writeUTF("CREATE_FOLDER_FAIL_EMPTY_NAME");
                return;
            }

            FolderDAO folderDAO = new FolderDAO();
            int newFolderId = folderDAO.createFolder(folderName.trim(), session.getCurrentUserId(), parentFolderId);

            if (newFolderId > 0) { // Thành công nếu ID > 0
                dos.writeUTF("CREATE_FOLDER_SUCCESS");
                dos.writeInt(newFolderId);
                listener.onFolderCreated(session.getCurrentUsername(), folderName.trim());
            } else if (newFolderId == -2) { // Mã lỗi mới cho tên trùng
                dos.writeUTF("CREATE_FOLDER_FAIL_NAME_EXIST");
            } else { // newFolderId == -1 hoặc lỗi khác
                dos.writeUTF("CREATE_FOLDER_FAIL_DB_ERROR");
            }
            dos.flush();

        } catch (Exception e) {
            System.err.println("Lỗi khi tạo thư mục cho User " + session.getCurrentUserId() + ": " + e.getMessage());
            e.printStackTrace();
            dos.writeUTF("CREATE_FOLDER_FAIL_INTERNAL_ERROR");
        }
    }
}
