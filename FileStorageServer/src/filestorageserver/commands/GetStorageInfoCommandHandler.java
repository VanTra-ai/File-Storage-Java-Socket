package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileServer; // Cần truy cập hằng số Quota
import filestorageserver.ServerActivityListener;
import filestorageserver.UserDAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Xử lý lệnh CMD_GET_STORAGE_INFO để lấy thông tin dung lượng lưu trữ của user.
 */
public class GetStorageInfoCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            dos.flush();
            return;
        }

        try {
            UserDAO userDAO = new UserDAO();
            long storageUsed = userDAO.getStorageUsed(session.getCurrentUserId());

            if (storageUsed != -1) { // Lấy được dung lượng
                // Lấy quota tổng từ FileServer (đã sửa thành public)
                long totalQuota = FileServer.USER_QUOTA_BYTES;

                // Gửi phản hồi: STORAGE_INFO:used:quota
                dos.writeUTF("STORAGE_INFO");
                dos.writeLong(storageUsed);
                dos.writeLong(totalQuota);
            } else {
                // Lỗi khi lấy dung lượng từ DB
                dos.writeUTF("STORAGE_INFO_FAIL_DB_ERROR");
            }

        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi lấy thông tin dung lượng cho User " + session.getCurrentUserId() + ": " + e.getMessage());
            e.printStackTrace();
            try {
                dos.writeUTF("STORAGE_INFO_FAIL_INTERNAL_ERROR");
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
