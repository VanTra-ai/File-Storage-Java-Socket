/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ComplexCommandHandler implements CommandHandler {

    // Phương thức này sẽ nhận toàn bộ chuỗi lệnh
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

    // Phương thức này không dùng trực tiếp trong mô hình Map, nhưng để tuân thủ interface
    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos) throws IOException {
        // Lớp này được thiết kế để được gọi với chuỗi lệnh đã được đọc
        // nên phương thức này có thể để trống hoặc báo lỗi.
        dos.writeUTF("ERROR_HANDLER_MISUSE");
    }

    // Các phương thức private để xử lý từng lệnh con
    private String handleShare(String command, ClientSession session, FileDAO fileDAO) {
        try {
            String data = command.substring("SHARE:".length());
            String[] parts = data.split("\\|");
            if (parts.length != 3) {
                return "SHARE_FAIL_INVALID_FORMAT";
            }

            int fileId = Integer.parseInt(parts[0]);
            String targetUsername = parts[1];
            int permissionLevel = Integer.parseInt(parts[2]);

            return fileDAO.shareFile(fileId, session.getCurrentUserId(), targetUsername, permissionLevel);
        } catch (NumberFormatException e) {
            return "SHARE_FAIL_INVALID_FORMAT";
        }
    }

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

    private String handleChangePermission(String command, ClientSession session, FileDAO fileDAO) {
        try {
            String data = command.substring("CHANGE_PERM:".length());
            String[] parts = data.split("\\|");
            if (parts.length != 3) {
                return "UPDATE_FAIL_INVALID_FORMAT";
            }
            int fileId = Integer.parseInt(parts[0]);
            String targetUsername = parts[1];
            int newPermissionLevel = Integer.parseInt(parts[2]);
            String result = fileDAO.updateFileSharePermission(fileId, session.getCurrentUserId(), targetUsername, newPermissionLevel);
            if (result.equals("UPDATE_SUCCESS")) {
                System.out.printf("User %d đã cập nhật quyền của User %s trên File ID %d thành Level %d thành công.\n", session.getCurrentUserId(), targetUsername, fileId, newPermissionLevel);
            } else {

                System.err.printf("User %d cập nhật quyền thất bại trên File ID %d. Mã lỗi: %s\n", session.getCurrentUserId(), fileId, result);
            }
            return result;
        } catch (NumberFormatException ex) {
            return "UPDATE_FAIL_INVALID_FORMAT";
        }
    }
}