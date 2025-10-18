package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.UploadSessionDAO;
import filestorageserver.ServerActivityListener;
import filestorageserver.model.UploadSession;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class GetUploadStatusCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            dos.flush();
            return;
        }

        try {
            String sessionId = dis.readUTF();
            // TODO: Hoặc đọc fileName, totalSize để tìm session

            UploadSessionDAO sessionDAO = new UploadSessionDAO();
            UploadSession currentSession = sessionDAO.getSession(sessionId);

            // Chỉ trả về trạng thái nếu session tồn tại VÀ đang dở dang
            if (currentSession != null
                    && currentSession.getUserId() == session.getCurrentUserId()
                    && // Kiểm tra đúng chủ sở hữu session
                    ("UPLOADING".equals(currentSession.getStatus()) || "PAUSED".equals(currentSession.getStatus()))) {
                dos.writeUTF("UPLOAD_STATUS");
                dos.writeLong(currentSession.getCurrentOffset());
                dos.writeInt(currentSession.getChunkSize());
                System.out.println("Client yêu cầu resume session: " + sessionId + " tại offset: " + currentSession.getCurrentOffset());
            } else {
                dos.writeUTF("UPLOAD_NOT_FOUND"); // Không tìm thấy session hợp lệ để resume
            }

        } catch (IOException streamEx) {
            System.err.println("Lỗi đọc stream get status từ client: " + streamEx.getMessage());
            throw streamEx;
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi lấy trạng thái upload: " + e.getMessage());
            e.printStackTrace();
            try {
                dos.writeUTF("UPLOAD_STATUS_FAIL_INTERNAL_ERROR");
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
