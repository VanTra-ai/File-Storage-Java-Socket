package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.UploadSessionDAO;
import filestorageserver.ServerActivityListener;
import filestorageserver.model.UploadSession;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile; // Dùng RandomAccessFile để ghi đúng offset

public class UploadChunkCommandHandler implements CommandHandler {

    // Kích thước buffer đọc từ stream (có thể điều chỉnh)
    private static final int BUFFER_SIZE = 8192;

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            dos.flush();
            return;
        }

        String sessionId = null;
        try {
            sessionId = dis.readUTF();
            long chunkOffset = dis.readLong();
            int chunkLength = dis.readInt();
            // String chunkChecksum = dis.readUTF(); // Đọc checksum nếu client gửi

            UploadSessionDAO sessionDAO = new UploadSessionDAO();
            UploadSession currentSession = sessionDAO.getSession(sessionId);

            if (currentSession == null) {
                dos.writeUTF("CHUNK_FAIL_SESSION_NOT_FOUND");
                // Đọc bỏ phần data client đã gửi để tránh lỗi stream
                dis.skipBytes(chunkLength);
                return;
            }

            // Kiểm tra offset quan trọng!
            if (currentSession.getCurrentOffset() != chunkOffset) {
                dos.writeUTF("CHUNK_FAIL_OFFSET_MISMATCH");
                dis.skipBytes(chunkLength);
                return;
            }

            // Mở file tạm bằng RandomAccessFile để ghi đúng vị trí
            try (RandomAccessFile raf = new RandomAccessFile(currentSession.getTempFilePath(), "rw")) {
                raf.seek(chunkOffset); // Di chuyển con trỏ đến đúng offset

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesReadFromStream;
                int totalBytesWrittenForChunk = 0;

                while (totalBytesWrittenForChunk < chunkLength) {
                    // Đọc từ stream client, tối đa buffer size hoặc phần còn lại của chunk
                    bytesReadFromStream = dis.read(buffer, 0,
                            Math.min(buffer.length, chunkLength - totalBytesWrittenForChunk));

                    if (bytesReadFromStream == -1) {
                        // Lỗi client ngắt kết nối giữa chừng
                        throw new IOException("Client disconnected while sending chunk data.");
                    }

                    raf.write(buffer, 0, bytesReadFromStream); // Ghi vào file tạm
                    totalBytesWrittenForChunk += bytesReadFromStream;
                }

                // TODO: Nếu dùng checksum, tính checksum của dữ liệu vừa ghi và so sánh với chunkChecksum
                // Cập nhật offset trong CSDL *sau khi* ghi thành công
                boolean offsetUpdated = sessionDAO.updateOffset(sessionId, chunkOffset, totalBytesWrittenForChunk);

                if (offsetUpdated) {
                    dos.writeUTF("CHUNK_SUCCESS");
                } else {
                    // Lỗi này hiếm khi xảy ra nếu kiểm tra offset ở trên đã đúng
                    dos.writeUTF("CHUNK_FAIL_DB_UPDATE_ERROR");
                    System.err.println("Lỗi không thể cập nhật offset cho session: " + sessionId + " tại offset: " + chunkOffset);
                    // Có thể cần rollback file tạm nếu nghiêm trọng?
                }

            } catch (IOException fileEx) {
                System.err.println("Lỗi ghi file tạm cho session " + sessionId + ": " + fileEx.getMessage());
                dos.writeUTF("CHUNK_FAIL_DISK_WRITE_ERROR");
                // Không cần skipBytes vì lỗi xảy ra khi đang ghi, stream có thể đã lỗi
            }

        } catch (IOException streamEx) {
            System.err.println("Lỗi đọc stream chunk từ client (session " + sessionId + "): " + streamEx.getMessage());
            // Không gửi phản hồi vì stream đã lỗi
            throw streamEx; // Ném lại để ClientHandler đóng kết nối
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi xử lý chunk (session " + sessionId + "): " + e.getMessage());
            e.printStackTrace();
            try {
                dos.writeUTF("CHUNK_FAIL_INTERNAL_ERROR");
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
