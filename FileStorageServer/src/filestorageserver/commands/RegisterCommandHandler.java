package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.UserDAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import filestorageserver.ServerActivityListener;

/**
 * Xử lý logic cho lệnh đăng ký tài khoản mới (CMD_REGISTER).
 */
public class RegisterCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        try {
            String username = dis.readUTF();
            String password = dis.readUTF();
            String email = dis.readUTF();

            UserDAO userDAO = new UserDAO();
            String result = userDAO.registerUser(username, password, email);

            dos.writeUTF(result);
            dos.flush();

            // Ghi log trên server để theo dõi
            if (result.equals("REGISTER_SUCCESS")) {
                System.out.println("Đăng ký thành công User: " + username);
                listener.onUserRegistered(username);
            } else if (result.equals("REGISTER_FAIL_USERNAME_EXIST")) {
                System.err.println("Đăng ký thất bại: Username '" + username + "' đã tồn tại.");
            } else if (result.equals("REGISTER_FAIL_EMAIL_EXIST")) {
                System.err.println("Đăng ký thất bại: Email '" + email + "' đã được sử dụng.");
            } else {
                System.err.println("Đăng ký thất bại cho User '" + username + "'. Mã lỗi: " + result);
            }
        } catch (IOException e) {
            System.err.println("Lỗi I/O khi xử lý đăng ký: " + e.getMessage());
            // Ném lại ngoại lệ để ClientHandler có thể xử lý việc đóng kết nối
            throw e;
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi xử lý đăng ký: " + e.getMessage());
            e.printStackTrace();
            dos.writeUTF("REGISTER_FAIL_SERVER_ERROR");
        }
    }
}
