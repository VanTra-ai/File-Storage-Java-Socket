package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import filestorageserver.ServerActivityListener;
import filestorageserver.model.File;

/**
 * Xử lý các lệnh dạng chuỗi phức tạp từ client (ví dụ: SHARE:, UNSHARE:,...).
 * Lớp này hoạt động như một bộ điều phối, gọi các phương thức xử lý con tương
 * ứng.
 */
public class ComplexCommandHandler implements CommandHandler {

    /**
     * Phân tích chuỗi lệnh và gọi phương thức xử lý con phù hợp.
     *
     * @param command Chuỗi lệnh đầy đủ nhận từ client.
     * @param session Phiên làm việc của client.
     * @param dos Stream để gửi phản hồi về client.
     * @throws IOException Khi có lỗi giao tiếp mạng.
     */
    public void handleCommandString(String command, ClientSession session, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        FileDAO fileDAO = new FileDAO();
        String response = "UNKNOWN_STRING_COMMAND";

        if (command.startsWith("SHARE:")) {
            response = handleShare(command, session, fileDAO, listener);
        } else if (command.startsWith("UNSHARE:")) {
            response = handleUnshare(command, session, fileDAO, listener);
        } else if (command.startsWith("SHARE_LIST:")) {
            response = handleShareList(command, session, fileDAO, listener);
        } else if (command.startsWith("CHANGE_PERM:")) {
            response = handleChangePermission(command, session, fileDAO, listener);
        }

        dos.writeUTF(response);
    }

    /**
     * Phương thức này được implement để tuân thủ interface CommandHandler.
     */
    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        dos.writeUTF("ERROR_HANDLER_MISUSE");
    }

    /**
     * Xử lý lệnh SHARE.
     */
    private String handleShare(String command, ClientSession session, FileDAO fileDAO, ServerActivityListener listener) {
        try {
            String data = command.substring("SHARE:".length());
            String[] parts = data.split("\\|");
            if (parts.length != 4) {
                return "SHARE_FAIL_INVALID_FORMAT";
            }

            int fileId = Integer.parseInt(parts[0]);
            String targetUsername = parts[1];
            int permissionLevel = Integer.parseInt(parts[2]);
            int expiryMinutes = Integer.parseInt(parts[3]);

            String result = fileDAO.shareFile(fileId, session.getCurrentUserId(), targetUsername, permissionLevel, expiryMinutes);

            if ("SHARE_SUCCESS".equals(result)) {
                File sharedFile = fileDAO.getFileForDownload(fileId, session.getCurrentUserId());
                if (sharedFile != null) {
                    listener.onFileShared(session.getCurrentUsername(), targetUsername, sharedFile.getFileName());
                }
            }
            return result;
        } catch (NumberFormatException e) {
            return "SHARE_FAIL_INVALID_FORMAT";
        }
    }

    /**
     * Xử lý lệnh UNSHARE.
     */
    private String handleUnshare(String command, ClientSession session, FileDAO fileDAO, ServerActivityListener listener) {
        try {
            String data = command.substring("UNSHARE:".length());
            String[] parts = data.split("\\|");
            if (parts.length != 2) {
                return "UNSHARE_FAIL_INVALID_FORMAT";
            }

            int fileId = Integer.parseInt(parts[0]);
            String targetUsername = parts[1];

            String result = fileDAO.unshareFile(fileId, session.getCurrentUserId(), targetUsername);

            if ("UNSHARE_SUCCESS".equals(result)) {
                File unsharedFile = fileDAO.getFileForDownload(fileId, session.getCurrentUserId());
                if (unsharedFile != null) {
                    listener.onFileUnshared(session.getCurrentUsername(), targetUsername, unsharedFile.getFileName());
                }
            }
            return result;
        } catch (NumberFormatException ex) {
            return "UNSHARE_FAIL_INVALID_FORMAT";
        }
    }

    /**
     * Xử lý lệnh SHARE_LIST.
     */
    private String handleShareList(String command, ClientSession session, FileDAO fileDAO, ServerActivityListener listener) {
        try {
            int fileId = Integer.parseInt(command.substring("SHARE_LIST:".length()));
            // Lệnh này chỉ đọc dữ liệu nên thường không cần thông báo log đặc biệt
            return fileDAO.getSharedUsersByFile(fileId, session.getCurrentUserId());
        } catch (NumberFormatException ex) {
            return "SHARELIST_FAIL_INVALID_FORMAT";
        }
    }

    /**
     * Xử lý lệnh CHANGE_PERM.
     */
    private String handleChangePermission(String command, ClientSession session, FileDAO fileDAO, ServerActivityListener listener) {
        try {
            String data = command.substring("CHANGE_PERM:".length());
            String[] parts = data.split("\\|");
            if (parts.length != 4) {
                return "UPDATE_FAIL_INVALID_FORMAT";
            }

            int fileId = Integer.parseInt(parts[0]);
            String targetUsername = parts[1];
            int newPermissionLevel = Integer.parseInt(parts[2]);
            int expiryMinutes = Integer.parseInt(parts[3]);

            String result = fileDAO.updateFileSharePermission(fileId, session.getCurrentUserId(), targetUsername, newPermissionLevel, expiryMinutes);

            if ("UPDATE_SUCCESS".equals(result)) {
                File updatedFile = fileDAO.getFileForDownload(fileId, session.getCurrentUserId());
                if (updatedFile != null) {
                    listener.onShareUpdated(session.getCurrentUsername(), targetUsername, updatedFile.getFileName());
                }
            }
            return result;
        } catch (NumberFormatException ex) {
            return "UPDATE_FAIL_INVALID_FORMAT";
        }
    }
}
