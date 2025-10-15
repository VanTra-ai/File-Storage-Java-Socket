package filestorageserver.commands;

import filestorageserver.ClientSession;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Định nghĩa cấu trúc chung cho tất cả các lớp xử lý lệnh từ client. Mỗi lớp
 * thực thi interface này sẽ chịu trách nhiệm cho một chức năng cụ thể.
 */
public interface CommandHandler {

    /**
     * Thực thi logic xử lý cho một lệnh.
     *
     * @param session Phiên làm việc của client, chứa thông tin người dùng đang
     * đăng nhập.
     * @param dis Stream để đọc dữ liệu từ client.
     * @param dos Stream để gửi dữ liệu phản hồi về client.
     * @throws IOException khi có lỗi giao tiếp mạng xảy ra.
     */
    void handle(ClientSession session, DataInputStream dis, DataOutputStream dos) throws IOException;
}
