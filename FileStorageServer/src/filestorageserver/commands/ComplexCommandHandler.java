package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

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
    public void handleCommandString(String command, ClientSession session, DataOutputStream dos) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        FileDAO fileDAO = new FileDAO();
        String response = "UNKNOWN_STRING_COMMAND";

        if (command.startsWith("SHARE:")) {
            response = handleShare(command, session, fileDAO);
        } else if (command.startsWith("UNSHARE:")) {
            response = handleUnshare(command, session, fileDAO);
        } else if (command.startsWith("SHARE_LIST:")) {
            response = handleShareList(command, session, fileDAO);
        } else if (command.startsWith("CHANGE_PERM:")) {
            response = handleChangePermission(command, session, fileDAO);
        }

        dos.writeUTF(response);
    }

    /**
     * Phương thức này được implement để tuân thủ interface CommandHandler. Tuy
     * nhiên, nó không nên được gọi trực tiếp vì lớp này cần chuỗi lệnh đã được
     * đọc trước.
     */
    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos) throws IOException {
        dos.writeUTF("ERROR_HANDLER_MISUSE");
    }

    /**
     * Xử lý lệnh SHARE. Định dạng:
     * SHARE:FileID|TargetUsername|PermissionLevel|ExpiryMinutes
     */
    private String handleShare(String command, ClientSession session, FileDAO fileDAO) {
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

            return fileDAO.shareFile(fileId, session.getCurrentUserId(), targetUsername, permissionLevel, expiryMinutes);
        } catch (NumberFormatException e) {
            return "SHARE_FAIL_INVALID_FORMAT";
        }
    }

    /**
     * Xử lý lệnh UNSHARE. Định dạng: UNSHARE:FileID|TargetUsername
     */
    private String handleUnshare(String command, ClientSession session, FileDAO fileDAO) {
        try {
            String data = command.substring("UNSHARE:".length());
            String[] parts = data.split("\\|");
            if (parts.length != 2) {
                return "UNSHARE_FAIL_INVALID_FORMAT";
            }
            int fileId = Integer.parseInt(parts[0]);
            String targetUsername = parts[1];
            String result = fileDAO.unshareFile(fileId, session.getCurrentUserId(), targetUsername);

            if (result.equals("UNSHARE_SUCCESS")) {
                System.out.printf("User %d đã hủy chia sẻ File ID %d với User %s thành công.\n", session.getCurrentUserId(), fileId, targetUsername);
            } else {
                System.err.printf("User %d hủy chia sẻ File ID %d với User %s thất bại. Mã lỗi: %s\n", session.getCurrentUserId(), fileId, targetUsername, result);
            }
            return result;
        } catch (NumberFormatException ex) {
            return "UNSHARE_FAIL_INVALID_FORMAT";
        }
    }

    /**
     * Xử lý lệnh SHARE_LIST. Định dạng: SHARE_LIST:FileID
     */
    private String handleShareList(String command, ClientSession session, FileDAO fileDAO) {
        try {
            int fileId = Integer.parseInt(command.substring("SHARE_LIST:".length()));
            String result = fileDAO.getSharedUsersByFile(fileId, session.getCurrentUserId());

            if (result.startsWith("SHARELIST_START")) {
                System.out.printf("User %d đã lấy danh sách chia sẻ File ID %d thành công.\n", session.getCurrentUserId(), fileId);
            } else {
                System.err.printf("User %d lấy danh sách chia sẻ File ID %d thất bại. Mã lỗi: %s\n", session.getCurrentUserId(), fileId, result);
            }
            return result;
        } catch (NumberFormatException ex) {
            return "SHARELIST_FAIL_INVALID_FORMAT";
        }
    }

    /**
     * Xử lý lệnh CHANGE_PERM. Định dạng:
     * CHANGE_PERM:FileID|TargetUsername|NewPermissionLevel|ExpiryMinutes
     */
    private String handleChangePermission(String command, ClientSession session, FileDAO fileDAO) {
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

            if (result.equals("UPDATE_SUCCESS")) {
                System.out.printf("User %d đã cập nhật quyền cho User %s trên File ID %d thành công.\n", session.getCurrentUserId(), targetUsername, fileId);
            } else {
                System.err.printf("User %d cập nhật quyền thất bại trên File ID %d. Mã lỗi: %s\n", session.getCurrentUserId(), fileId, result);
            }
            return result;
        } catch (NumberFormatException ex) {
            return "UPDATE_FAIL_INVALID_FORMAT";
        }
    }
}
