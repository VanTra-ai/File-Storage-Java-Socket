package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FolderDAO;
import filestorageserver.ServerActivityListener;
import filestorageserver.model.Folder;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import filestorageserver.model.PagedResult;

/**
 * Xử lý lệnh CMD_GET_FOLDERS để lấy danh sách thư mục con.
 */
public class GetFoldersCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            dos.flush(); // Thêm flush
            return;
        }

        try {
            int parentFolderIdInt = dis.readInt();      // ID thư mục cha (-1 = gốc)
            int pageNumber = dis.readInt();          // Trang muốn lấy (ví dụ: 1)
            int pageSize = dis.readInt();            // Số item/trang (ví dụ: 20)
            String sortBy = dis.readUTF();             // Tiêu chí sắp xếp (ví dụ: "name_asc")

            Integer parentFolderId = (parentFolderIdInt == -1) ? null : parentFolderIdInt;

            // Đảm bảo giá trị hợp lệ
            if (pageNumber < 1) {
                pageNumber = 1;
            }
            if (pageSize <= 0) {
                pageSize = 20; // Giá trị mặc định nếu client gửi không hợp lệ
            }
            FolderDAO folderDAO = new FolderDAO();
            PagedResult<Folder> pagedResult = folderDAO.getFoldersByParent(session.getCurrentUserId(), parentFolderId, pageNumber, pageSize, sortBy);

            if (pagedResult == null) {
                dos.writeUTF("FOLDERLIST_FAIL_SERVER_ERROR");
                System.err.println("Lỗi: getFoldersByParent (paged) trả về null cho User: " + session.getCurrentUserId());
                dos.flush();
                return;
            }

            StringBuilder response = new StringBuilder("FOLDERLIST_PAGED_START:");
            response.append(pagedResult.getTotalItems()).append("|")
                    .append(pagedResult.getTotalPages()).append("|")
                    .append(pagedResult.getCurrentPage()).append("|"); // Thêm thông tin phân trang

            // Nối dữ liệu thư mục
            for (Folder folder : pagedResult.getItems()) {
                response.append(folder.getFolderId()).append("|")
                        .append(folder.getFolderName()).append(";");
            }

            dos.writeUTF(response.toString());

        } catch (IOException streamEx) {
            System.err.println("Lỗi đọc stream get folders (paged) từ client: " + streamEx.getMessage());
            throw streamEx; // Ném lại để ClientHandler đóng kết nối
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách thư mục (paged) cho User " + session.getCurrentUserId() + ": " + e.getMessage());
            e.printStackTrace();
            try {
                dos.writeUTF("FOLDERLIST_FAIL_INTERNAL_ERROR");
            } catch (IOException ignored) {
            }
        } finally {
            try {
                dos.flush();
            } catch (IOException ignored) {
            } 
        }
    }
}
