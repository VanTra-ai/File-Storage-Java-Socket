package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.UserDAO;
import filestorageserver.model.User;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Xử lý logic cho lệnh đăng nhập (CMD_LOGIN).
 */
public class LoginCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos) throws IOException {
        try {
            String username = dis.readUTF();
            String password = dis.readUTF();

            UserDAO userDAO = new UserDAO();
            User user = userDAO.login(username, password);

            if (user != null) {
                // Cập nhật thông tin người dùng vào phiên làm việc
                session.setCurrentUserId(user.getUserId());
                session.setCurrentUsername(user.getUsername());

                // Gửi phản hồi thành công về cho client
                dos.writeUTF("LOGIN_SUCCESS");
                dos.writeInt(session.getCurrentUserId());
                dos.writeUTF(session.getCurrentUsername());
                dos.flush();
                System.out.println("Đăng nhập thành công: " + session.getCurrentUsername());
            } else {
                dos.writeUTF("LOGIN_FAIL");
            }
        } catch (IOException e) {
            System.err.println("Lỗi I/O khi xử lý đăng nhập: " + e.getMessage());
            // Ném lại ngoại lệ để ClientHandler có thể xử lý việc đóng kết nối
            throw e;
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi xử lý đăng nhập: " + e.getMessage());
            e.printStackTrace();
            dos.writeUTF("LOGIN_FAIL_SERVER_ERROR");
        }
    }
}
