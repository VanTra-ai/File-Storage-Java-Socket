/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.UserDAO;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class RegisterCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos) throws IOException {
        String username = dis.readUTF();
        String password = dis.readUTF();
        String email = dis.readUTF();

        UserDAO userDAO = new UserDAO();
        String result = userDAO.registerUser(username, password, email);

        dos.writeUTF(result);
        dos.flush();

        if (result.equals("REGISTER_SUCCESS")) {
            System.out.println("Đăng ký thành công User: " + username);
        } else if (result.equals("REGISTER_FAIL_USERNAME_EXIST")) {
            System.err.println("Đăng ký thất bại: Username " + username + " đã tồn tại.");
        } else if (result.equals("REGISTER_FAIL_EMAIL_EXIST")) {
            System.err.println("Đăng ký thất bại: Email " + email + " đã được sử dụng.");
        } else {
            System.err.println("Đăng ký thất bại cho User: " + username + ". Lỗi nội bộ hoặc CSDL: " + result);
        }
    }
}