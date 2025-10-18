package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import filestorageserver.ServerActivityListener;
import filestorageserver.model.File; // Đảm bảo import đúng model.File
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý lệnh CMD_GET_FILES_IN_FOLDER để lấy danh sách file trong một thư mục.
 */
public class GetFilesInFolderCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        try {
            // Đọc folderId từ client. Client sẽ gửi -1 nếu muốn lấy file ở thư mục gốc.
            int folderIdInt = dis.readInt();
            Integer folderId = (folderIdInt == -1) ? null : folderIdInt;

            FileDAO fileDAO = new FileDAO();
            List<File> files = new ArrayList<>();

            // Lấy file sở hữu trong thư mục này
            List<File> ownedFiles = fileDAO.getFilesByFolder(session.getCurrentUserId(), folderId);
            if (ownedFiles != null) {
                files.addAll(ownedFiles);
            } else {
                dos.writeUTF("FILELIST_FAIL_SERVER_ERROR");
                System.err.println("Lỗi: getFilesByFolder trả về null cho User: " + session.getCurrentUserId());
                return;
            }

            // Chỉ lấy file được chia sẻ NẾU đang ở thư mục gốc (folderId == null)
            if (folderId == null) {
                List<File> sharedFiles = fileDAO.getSharedFilesForUser(session.getCurrentUserId());
                if (sharedFiles != null) {
                    files.addAll(sharedFiles);
                } else {
                    // Không cần báo lỗi nghiêm trọng nếu chỉ lỗi lấy file chia sẻ
                    System.err.println("Lỗi khi lấy file được chia sẻ cho User: " + session.getCurrentUserId());
                }
            }

            // Xây dựng chuỗi phản hồi tương tự ListFilesCommandHandler cũ
            StringBuilder sb = new StringBuilder();
            for (File file : files) {
                String dateString = "N/A";
                if (file.getUploadedAt() != null) {
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                    dateString = dateFormat.format(file.getUploadedAt());
                }

                String status = file.isIsSharedToMe() ? "Shared" : "Owned";
                String sharer = file.getSharerName() != null ? file.getSharerName() : "";

                sb.append(file.getFileId()).append("|")
                        .append(file.getFileName()).append("|")
                        .append(file.getFileSize()).append("|")
                        .append(dateString).append("|")
                        .append(status).append("|")
                        .append(sharer).append(";");
            }

            dos.writeUTF("FILELIST_START:" + sb.toString());

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách file trong thư mục cho User " + session.getCurrentUserId() + ": " + e.getMessage());
            e.printStackTrace();
            dos.writeUTF("FILELIST_FAIL_INTERNAL_ERROR");
        }
    }
}
