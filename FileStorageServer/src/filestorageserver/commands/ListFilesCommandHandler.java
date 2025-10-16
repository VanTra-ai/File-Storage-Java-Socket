package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import filestorageserver.model.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import filestorageserver.ServerActivityListener;

/**
 * Xử lý logic cho lệnh yêu cầu danh sách file (CMD_LISTFILES).
 */
public class ListFilesCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        try {
            FileDAO fileDAO = new FileDAO();
            List<File> files = fileDAO.listUserFiles(session.getCurrentUserId());

            // Xử lý trường hợp lớp DAO trả về null do lỗi kết nối CSDL
            if (files == null) {
                dos.writeUTF("FILELIST_FAIL_SERVER_ERROR");
                System.err.println("Lỗi: listUserFiles trả về null cho User: " + session.getCurrentUserId());
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (File file : files) {
                String dateString = "N/A";
                if (file.getUploadedAt() != null) {
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                    dateString = dateFormat.format(file.getUploadedAt());
                }

                String status = file.isIsSharedToMe() ? "Shared" : "Owned";
                String sharer = file.getSharerName();

                // Xây dựng chuỗi dữ liệu cho mỗi file theo định dạng đã thống nhất
                sb.append(file.getFileId()).append("|")
                        .append(file.getFileName()).append("|")
                        .append(file.getFileSize()).append("|")
                        .append(dateString).append("|")
                        .append(status).append("|")
                        .append(sharer).append(";");
            }

            dos.writeUTF("FILELIST_START:" + sb.toString());
            System.out.println("Gửi danh sách " + files.size() + " file thành công cho User: " + session.getCurrentUserId());

        } catch (Exception e) {
            // Bắt tất cả các lỗi không mong muốn để ngăn luồng bị sập
            System.err.println("Lỗi nghiêm trọng khi liệt kê file cho User " + session.getCurrentUserId() + ": " + e.getMessage());
            e.printStackTrace();

            // Cố gắng gửi một phản hồi lỗi chung về cho client
            try {
                dos.writeUTF("FILELIST_FAIL_SERVER_ERROR");
            } catch (IOException ioEx) {
                System.err.println("Không thể gửi thông báo lỗi về client, socket có thể đã đóng.");
            }
        }
    }
}
