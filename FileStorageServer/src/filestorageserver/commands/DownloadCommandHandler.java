package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import filestorageserver.model.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import filestorageserver.ServerActivityListener;

/**
 * Xử lý logic cho lệnh tải file xuống (CMD_DOWNLOAD).
 */
public class DownloadCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        try {
            int fileId = dis.readInt();
            FileDAO fileDAO = new FileDAO();

            // Lấy metadata, đồng thời kiểm tra quyền sở hữu hoặc quyền được chia sẻ
            File fileMetadata = fileDAO.getFileForDownload(fileId, session.getCurrentUserId());

            if (fileMetadata == null) {
                dos.writeUTF("DOWNLOAD_FAIL_AUTH"); // Không có quyền hoặc file không tồn tại
                return;
            }

            java.io.File fileToDownload = new java.io.File(fileMetadata.getFilePath());

            if (!fileToDownload.exists() || !fileToDownload.isFile()) {
                dos.writeUTF("DOWNLOAD_FAIL_NOT_FOUND"); // File đã bị xóa trên server
                return;
            }

            // Bắt đầu quá trình gửi file
            dos.writeUTF("DOWNLOAD_START");
            dos.writeUTF(fileMetadata.getFileName());
            dos.writeLong(fileMetadata.getFileSize());
            dos.flush();

            // Đọc từ file vật lý và ghi vào stream của socket
            try (FileInputStream fis = new FileInputStream(fileToDownload)) {
                byte[] buffer = new byte[8192]; // Bộ đệm 8KB
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            dos.flush();
            System.out.println("User " + session.getCurrentUserId() + " đã download thành công: " + fileMetadata.getFileName());
            // --- THÔNG BÁO SỰ KIỆN DOWNLOAD THÀNH CÔNG ---
            listener.onFileDownloaded(session.getCurrentUsername(), fileMetadata.getFileName());
        } catch (IOException e) {
            // Lỗi này thường xảy ra nếu client ngắt kết nối giữa chừng
            System.err.println("Lỗi I/O khi xử lý download cho user " + session.getCurrentUsername() + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi download: " + e.getMessage());
            e.printStackTrace();
            // Cố gắng gửi một thông báo lỗi chung nếu có thể
            if (dos != null) {
                dos.writeUTF("DOWNLOAD_FAIL_SERVER_ERROR");
            }
        }
    }
}
