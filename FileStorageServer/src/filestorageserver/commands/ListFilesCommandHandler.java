/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import filestorageserver.model.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class ListFilesCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        try {
            FileDAO fileDAO = new FileDAO();
            List<File> files = fileDAO.listUserFiles(session.getCurrentUserId());
            StringBuilder sb = new StringBuilder();

            for (File file : files) {
                String dateString = "N/A";
                if (file.getUploadedAt() != null) {
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                    dateString = dateFormat.format(file.getUploadedAt());
                }

                String status = file.isIsSharedToMe() ? "Shared" : "Owned";
                String sharer = file.getSharerName();

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
            System.err.println("Lỗi CSDL hoặc I/O khi liệt kê file: " + e.getMessage());
            dos.writeUTF("FILELIST_FAIL_SERVER_ERROR");
        }
    }
}
