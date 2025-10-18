package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FolderDAO;
import filestorageserver.ServerActivityListener;
import filestorageserver.model.Folder;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Xử lý lệnh CMD_GET_FOLDERS để lấy danh sách thư mục con.
 */
public class GetFoldersCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        try {
            // Đọc parentFolderId từ client. Client sẽ gửi -1 nếu muốn lấy thư mục gốc.
            int parentFolderIdInt = dis.readInt();
            Integer parentFolderId = (parentFolderIdInt == -1) ? null : parentFolderIdInt;

            FolderDAO folderDAO = new FolderDAO();
            List<Folder> folders = folderDAO.getFoldersByParent(session.getCurrentUserId(), parentFolderId);

            if (folders == null) {
                dos.writeUTF("FOLDERLIST_FAIL_SERVER_ERROR");
                System.err.println("Lỗi: getFoldersByParent trả về null cho User: " + session.getCurrentUserId());
                return;
            }

            // Xây dựng chuỗi phản hồi: FOLDERLIST_START:id|name;id|name;...
            StringBuilder response = new StringBuilder("FOLDERLIST_START:");
            for (Folder folder : folders) {
                response.append(folder.getFolderId()).append("|")
                        .append(folder.getFolderName()).append(";");
            }

            dos.writeUTF(response.toString());
            dos.flush();

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách thư mục cho User " + session.getCurrentUserId() + ": " + e.getMessage());
            e.printStackTrace();
            try { 
                dos.writeUTF("FOLDERLIST_FAIL_INTERNAL_ERROR");
                dos.flush();
            } catch (IOException ioEx) {
                System.err.println("Không thể gửi lỗi về client: " + ioEx.getMessage());
            }
        }
    }
}
