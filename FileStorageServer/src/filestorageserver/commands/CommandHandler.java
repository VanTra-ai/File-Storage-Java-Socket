/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver.commands;

import filestorageserver.ClientSession;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author Admin
 */
public interface CommandHandler {
    /**
     * Thực thi logic xử lý cho một lệnh cụ thể.
     * @param session Phiên làm việc của client, chứa thông tin người dùng.
     * @param dis Stream để đọc dữ liệu từ client.
     * @param dos Stream để gửi dữ liệu đến client.
     * @throws IOException Nếu có lỗi I/O xảy ra.
     */
    void handle(ClientSession session, DataInputStream dis, DataOutputStream dos) throws IOException;
}
