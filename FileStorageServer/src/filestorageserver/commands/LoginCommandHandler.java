/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
        String username = dis.readUTF();
        String password = dis.readUTF();

        // Lưu ý: UserDAO đang được tạo mới ở đây.
        // Để tối ưu hơn, ta có thể áp dụng Dependency Injection.
        UserDAO userDAO = new UserDAO();
        User user = userDAO.login(username, password);

        if (user != null) {
            // Cập nhật thông tin vào session
            session.setCurrentUserId(user.getUserId());
            session.setCurrentUsername(user.getUsername());
            
            // Gửi phản hồi thành công
            dos.writeUTF("LOGIN_SUCCESS");
            dos.writeInt(session.getCurrentUserId());
            dos.writeUTF(session.getCurrentUsername());
            dos.flush();
            System.out.println("Đăng nhập thành công: " + session.getCurrentUsername());
        } else {
            dos.writeUTF("LOGIN_FAIL");
        }
    }
}