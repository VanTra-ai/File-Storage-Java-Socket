package filestorageserver;

import filestorageserver.model.UploadSession; // Tạo lớp model này ở bước sau
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.sql.Timestamp;
import java.util.UUID;

/**
 * DAO cho việc quản lý bảng upload_sessions.
 */
public class UploadSessionDAO {

    private Connection getConnection() {
        return new MyConnection().getConnection();
    }

    private void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đóng kết nối CSDL (UploadSessionDAO): " + e.getMessage());
        }
    }

    /**
     * Tạo một session upload mới trong CSDL.
     *
     * @param userId ID người dùng.
     * @param fileName Tên file gốc.
     * @param tempFilePath Đường dẫn file tạm.
     * @param totalSize Tổng kích thước file.
     * @param chunkSize Kích thước chunk.
     * @param targetFolderId ID thư mục đích (có thể null).
     * @return Đối tượng UploadSession mới được tạo (chứa sessionId), hoặc null
     * nếu lỗi.
     */
    public UploadSession createSession(int userId, String fileName, String tempFilePath, long totalSize, int chunkSize, Integer targetFolderId) {
        String sessionId = UUID.randomUUID().toString(); // Tạo UUID mới
        String sql = "INSERT INTO upload_sessions (session_id, user_id, file_name, temp_file_path, total_size, current_offset, chunk_size, status, target_folder_id, created_at, last_updated_at) "
                + "VALUES (?, ?, ?, ?, ?, 0, ?, 'UPLOADING', ?, NOW(), NOW())";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (conn == null) {
                return null;
            }

            ps.setString(1, sessionId);
            ps.setInt(2, userId);
            ps.setString(3, fileName);
            ps.setString(4, tempFilePath);
            ps.setLong(5, totalSize);
            ps.setInt(6, chunkSize);

            if (targetFolderId != null) {
                ps.setInt(7, targetFolderId);
            } else {
                ps.setNull(7, Types.INTEGER);
            }

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                UploadSession session = new UploadSession();
                session.setSessionId(sessionId);
                session.setUserId(userId);
                session.setFileName(fileName);
                session.setTempFilePath(tempFilePath);
                session.setTotalSize(totalSize);
                session.setCurrentOffset(0); // Bắt đầu từ 0
                session.setChunkSize(chunkSize);
                session.setStatus("UPLOADING");
                session.setTargetFolderId(targetFolderId);
                // Lấy thời gian từ DB nếu cần chính xác hơn, tạm thời bỏ qua
                return session;
            }
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi tạo upload session: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Lấy thông tin session upload dựa vào sessionId.
     *
     * @param sessionId ID của session.
     * @return Đối tượng UploadSession, hoặc null nếu không tìm thấy/lỗi.
     */
    public UploadSession getSession(String sessionId) {
        String sql = "SELECT * FROM upload_sessions WHERE session_id = ?";
        UploadSession session = null;

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (conn == null) {
                return null;
            }

            ps.setString(1, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    session = mapRowToSession(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi lấy upload session: " + e.getMessage());
        }
        return session;
    }

    /**
     * Cập nhật offset hiện tại cho một session upload. Chỉ cập nhật nếu offset
     * mới khớp với offset mong đợi (ngăn lỗi ghi đè).
     *
     * @param sessionId ID của session.
     * @param expectedOffset Offset hiện tại mà server đang mong đợi.
     * @param bytesWritten Số byte vừa được ghi thành công.
     * @return true nếu cập nhật thành công, false nếu offset không khớp hoặc
     * lỗi.
     */
    public boolean updateOffset(String sessionId, long expectedOffset, int bytesWritten) {
        // Cập nhật cả last_updated_at (tự động nhờ CSDL)
        String sql = "UPDATE upload_sessions SET current_offset = current_offset + ? "
                + "WHERE session_id = ? AND current_offset = ?";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (conn == null) {
                return false;
            }

            ps.setInt(1, bytesWritten);
            ps.setString(2, sessionId);
            ps.setLong(3, expectedOffset); // Điều kiện quan trọng

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0; // Trả về true nếu có 1 hàng được cập nhật

        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi cập nhật offset session: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật trạng thái cuối cùng của session (COMPLETE hoặc ERROR).
     *
     * @param sessionId ID của session.
     * @param status Trạng thái mới ('COMPLETE' hoặc 'ERROR').
     * @return true nếu cập nhật thành công.
     */
    public boolean finalizeSessionStatus(String sessionId, String status) {
        String sql = "UPDATE upload_sessions SET status = ? WHERE session_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (conn == null) {
                return false;
            }

            ps.setString(1, status); // Trạng thái mới
            ps.setString(2, sessionId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi cập nhật trạng thái session cuối cùng: " + e.getMessage());
            return false;
        }
    }

    /**
     * Xóa một session upload (ví dụ khi client hủy hoặc hoàn thành).
     *
     * @param sessionId ID của session cần xóa.
     * @return true nếu xóa thành công.
     */
    public boolean deleteSession(String sessionId) {
        String sql = "DELETE FROM upload_sessions WHERE session_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (conn == null) {
                return false;
            }
            ps.setString(1, sessionId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi xóa upload session: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hàm tiện ích để ánh xạ một hàng ResultSet thành đối tượng UploadSession.
     */
    private UploadSession mapRowToSession(ResultSet rs) throws SQLException {
        UploadSession session = new UploadSession();
        session.setSessionId(rs.getString("session_id"));
        session.setUserId(rs.getInt("user_id"));
        session.setFileName(rs.getString("file_name"));
        session.setTempFilePath(rs.getString("temp_file_path"));
        session.setTotalSize(rs.getLong("total_size"));
        session.setCurrentOffset(rs.getLong("current_offset"));
        session.setChunkSize(rs.getInt("chunk_size"));
        session.setStatus(rs.getString("status"));
        session.setTargetFolderId((Integer) rs.getObject("target_folder_id")); // Dùng getObject để lấy NULL
        session.setCreatedAt(rs.getTimestamp("created_at"));
        session.setLastUpdatedAt(rs.getTimestamp("last_updated_at"));
        return session;
    }

    // --- (Tùy chọn) Thêm các phương thức khác nếu cần ---
    /*
    // Ví dụ: Tìm session dở dang theo tên file và kích thước (dùng cho resume)
    public UploadSession findIncompleteSession(int userId, String fileName, long totalSize) {
        String sql = "SELECT * FROM upload_sessions "
                   + "WHERE user_id = ? AND file_name = ? AND total_size = ? "
                   + "AND status IN ('UPLOADING', 'PAUSED') " // Chỉ tìm session đang dở
                   + "ORDER BY last_updated_at DESC LIMIT 1"; // Lấy session mới nhất nếu có nhiều
        UploadSession session = null;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
             // ... set params, execute query, mapRowToSession ...
        } catch (SQLException e) { // ... handle error ...}
        return session;
    }

    // Ví dụ: Dọn dẹp các session bị treo quá lâu
    public int cleanupStaleSessions(int timeoutMinutes) {
         String sql = "DELETE FROM upload_sessions "
                    + "WHERE status IN ('UPLOADING', 'PAUSED') "
                    + "AND last_updated_at < NOW() - INTERVAL ? MINUTE";
         int deletedRows = 0;
         try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
              ps.setInt(1, timeoutMinutes);
              deletedRows = ps.executeUpdate();
              if (deletedRows > 0) {
                 System.out.println("Đã dọn dẹp " + deletedRows + " upload session bị treo.");
              }
         } catch (SQLException e) { // ... handle error ...}
         return deletedRows;
    }
     */
}
