package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import filestorageserver.FolderDAO;
import filestorageserver.ServerActivityListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Xử lý lệnh CMD_MOVE_ITEM để di chuyển file hoặc thư mục.
 */
public class MoveItemCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        try {
            String itemType = dis.readUTF(); // Client gửi "FILE" hoặc "FOLDER"
            int itemId = dis.readInt();
            int targetFolderIdInt = dis.readInt(); // Client gửi -1 cho thư mục gốc
            Integer targetFolderId = (targetFolderIdInt == -1) ? null : targetFolderIdInt;

            boolean success = false;
            String failReason = "MOVE_FAIL_UNKNOWN";
            String itemName = null;

            FileDAO fileDAO = new FileDAO();
            FolderDAO folderDAO = new FolderDAO();

            if ("FILE".equalsIgnoreCase(itemType)) {
                itemName = fileDAO.getFileNameById(itemId, session.getCurrentUserId());
            } else if ("FOLDER".equalsIgnoreCase(itemType)) {
                itemName = folderDAO.getFolderNameById(itemId, session.getCurrentUserId());
            }

            if (itemName == null) {
                failReason = "MOVE_FAIL_NOT_FOUND_OR_AUTH";
                success = false;
            } else {
                // Item tồn tại, tiến hành di chuyển
                if ("FILE".equalsIgnoreCase(itemType)) {
                    success = fileDAO.moveFile(itemId, targetFolderId, session.getCurrentUserId());
                    failReason = "MOVE_FAIL_FILE_ERROR";
                } else if ("FOLDER".equalsIgnoreCase(itemType)) {
                    success = folderDAO.moveFolder(itemId, targetFolderId, session.getCurrentUserId());
                    failReason = "MOVE_FAIL_FOLDER_ERROR";
                } else {
                    failReason = "MOVE_FAIL_INVALID_TYPE";
                    success = false;
                }
            }

            if (success) {
                dos.writeUTF("MOVE_SUCCESS");

                // Ghi log tương ứng
                if ("FILE".equalsIgnoreCase(itemType)) {
                    listener.onFileMoved(session.getCurrentUsername(), itemName, targetFolderId);
                } else if ("FOLDER".equalsIgnoreCase(itemType)) {
                    listener.onFolderMoved(session.getCurrentUsername(), itemName, targetFolderId);
                }

            } else {
                dos.writeUTF(failReason);
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi di chuyển item cho User " + session.getCurrentUserId() + ": " + e.getMessage());
            e.printStackTrace();
            try {
                dos.writeUTF("MOVE_FAIL_INTERNAL_ERROR");
            } catch (IOException ioEx) {
            }
        } finally {
            try {
                if (dos != null) {
                    dos.flush();
                }
            } catch (IOException flushEx) {
                System.err.println("Lỗi khi flush stream: " + flushEx.getMessage());
            }
        }
    }
}
