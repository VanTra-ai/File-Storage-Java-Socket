package filestorageserver;

import filestorageserver.model.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.sql.Types;

/**
 * Lớp Data Access Object (DAO) cho các thao tác liên quan đến file. Chịu trách
 * nhiệm cho mọi tương tác với bảng 'files' và 'file_shares' trong CSDL.
 */
public class FileDAO {

    private Connection getConnection() {
        return new MyConnection().getConnection();
    }

    private void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đóng kết nối CSDL: " + e.getMessage());
        }
    }

    /**
     * Chèn metadata của một file mới vào CSDL sau khi file vật lý đã được tải
     * lên.
     *
     * @param file Đối tượng File chứa thông tin metadata.
     * @return ID của file vừa được tạo, hoặc -1 nếu thất bại.
     */
    public int insertFileMetadata(File file) {
        // SỬA SQL: Thêm cột 'folder_id'
        String sql = "INSERT INTO Files (owner_id, folder_id, file_name, file_path, file_size, mime_type, created_at, is_shared) VALUES (?, ?, ?, ?, ?, ?, NOW(), ?)";
        int generatedFileId = -1;
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (con == null) {
                return -1;
            }

            ps.setInt(1, file.getOwnerId());

            // THÊM DÒNG NÀY:
            if (file.getFolderId() != null) {
                ps.setInt(2, file.getFolderId());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            // ĐẨY CHỈ SỐ CÁC CỘT CÒN LẠI LÊN 1
            ps.setString(3, file.getFileName());
            ps.setString(4, file.getFilePath());
            ps.setLong(5, file.getFileSize());
            ps.setString(6, file.getFileType());
            ps.setBoolean(7, file.isIsShared()); // Chỉ số cột này tăng từ 6 lên 7

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedFileId = rs.getInt(1);
                    }
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi chèn metadata file: " + ex.getMessage());
        }
        return generatedFileId;
    }

    /**
     * Lấy thông tin metadata của một file, đồng thời kiểm tra quyền truy cập
     * của người dùng. Người dùng có quyền nếu họ là chủ sở hữu hoặc được chia
     * sẻ file đó.
     *
     * @param fileId ID của file cần lấy.
     * @param userId ID của người dùng yêu cầu.
     * @return Đối tượng File nếu có quyền truy cập, ngược lại trả về null.
     */
    public File getFileForDownload(int fileId, int userId) {
        File file = null;
        String sql = "SELECT f.*, CASE WHEN f.owner_id = ? THEN 0 ELSE 1 END AS is_shared_to_me FROM files f LEFT JOIN file_shares fs ON f.file_id = fs.file_id AND fs.shared_with_user_id = ? WHERE f.file_id = ? AND (f.owner_id = ? OR fs.shared_with_user_id IS NOT NULL)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            if (conn == null) {
                return null;
            }

            ps.setInt(1, userId);
            ps.setInt(2, userId);
            ps.setInt(3, fileId);
            ps.setInt(4, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    file = new File();
                    file.setFileId(rs.getInt("file_id"));
                    file.setFileName(rs.getString("file_name"));
                    file.setFileSize(rs.getLong("file_size"));
                    file.setFilePath(rs.getString("file_path"));
                    file.setOwnerId(rs.getInt("owner_id"));
                    file.setFileType(rs.getString("mime_type"));
                    file.setIsShared(rs.getBoolean("is_shared"));
                    file.setIsSharedToMe(rs.getInt("is_shared_to_me") == 1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi lấy file để download: " + e.getMessage());
        }
        return file;
    }

    /**
     * Xóa metadata của một file trong CSDL. Chỉ chủ sở hữu mới có quyền thực
     * hiện.
     *
     * @param fileId ID của file cần xóa.
     * @param ownerId ID của người dùng yêu cầu xóa (phải là chủ sở hữu).
     * @return true nếu xóa thành công, ngược lại là false.
     */
    public boolean deleteFileMetadata(int fileId, int ownerId) {
        String sql = "DELETE FROM Files WHERE file_id = ? AND owner_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return false;
            }

            ps.setInt(1, fileId);
            ps.setInt(2, ownerId);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi xóa metadata file: " + ex.getMessage());
        }
        return false;
    }

    //<editor-fold defaultstate="collapsed" desc="Transactional Helper Methods">
    /**
     * Lấy ID người dùng từ username, sử dụng một kết nối CSDL có sẵn (an toàn
     * cho transaction).
     */
    public int getUserIdByUsername(String username, Connection conn) throws SQLException {
        String sql = "SELECT user_id FROM Users WHERE Username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }
        return -1;
    }

    /**
     * Lấy ID người dùng từ username (tự quản lý kết nối).
     */
    public int getUserIdByUsername(String username) {
        try (Connection conn = getConnection()) {
            if (conn == null) {
                return -1;
            }
            return getUserIdByUsername(username, conn);
        } catch (SQLException e) {
            System.err.println("Lỗi DAO khi tìm user_id theo Username: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Kiểm tra quyền sở hữu file, sử dụng một kết nối CSDL có sẵn (an toàn cho
     * transaction).
     */
    public boolean isFileOwner(int fileId, int ownerId, Connection conn) throws SQLException {
        String sql = "SELECT file_id FROM Files WHERE file_id = ? AND owner_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            ps.setInt(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Kiểm tra quyền sở hữu file (tự quản lý kết nối).
     */
    public boolean isFileOwner(int fileId, int ownerId) {
        try (Connection conn = getConnection()) {
            if (conn == null) {
                return false;
            }
            return isFileOwner(fileId, ownerId, conn);
        } catch (SQLException e) {
            System.err.println("Lỗi DAO khi kiểm tra chủ sở hữu file: " + e.getMessage());
            return false;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Sharing Logic Methods">
    /**
     * Thực hiện chia sẻ một file cho người dùng khác, có hỗ trợ thời gian hết
     * hạn.
     */
    public String shareFile(int fileId, int ownerId, String targetUsername, int permissionLevel, int expiryDurationInMinutes) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return "SHARE_FAIL_SERVER_ERROR";
            }
            conn.setAutoCommit(false);

            if (!isFileOwner(fileId, ownerId, conn)) {
                conn.rollback();
                return "SHARE_FAIL_AUTH";
            }

            int targetUserId = getUserIdByUsername(targetUsername, conn);
            if (targetUserId == -1) {
                conn.rollback();
                return "SHARE_FAIL_USER_NOT_FOUND";
            }

            if (targetUserId == ownerId) {
                conn.rollback();
                return "SHARE_FAIL_SELF_SHARE";
            }

            String checkExistingSQL = "SELECT share_id FROM file_shares WHERE file_id = ? AND shared_with_user_id = ?";
            try (PreparedStatement psCheck = conn.prepareStatement(checkExistingSQL)) {
                psCheck.setInt(1, fileId);
                psCheck.setInt(2, targetUserId);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        return "SHARE_FAIL_ALREADY_SHARED";
                    }
                }
            }

            String insertSQL = "INSERT INTO file_shares (file_id, shared_with_user_id, shared_by_user_id, permission_level, shared_at, share_expiry) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psShare = conn.prepareStatement(insertSQL)) {
                Timestamp currentTime = new Timestamp(System.currentTimeMillis());
                Timestamp expiryTime = null;
                if (expiryDurationInMinutes > 0) {
                    long expiryMillis = currentTime.getTime() + (long) expiryDurationInMinutes * 60 * 1000;
                    expiryTime = new Timestamp(expiryMillis);
                }
                psShare.setInt(1, fileId);
                psShare.setInt(2, targetUserId);
                psShare.setInt(3, ownerId);
                psShare.setInt(4, permissionLevel);
                psShare.setTimestamp(5, currentTime);
                psShare.setTimestamp(6, expiryTime);
                psShare.executeUpdate();
            }

            conn.commit();
            return "SHARE_SUCCESS";
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL trong shareFile: " + e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                /* ignored */ }
            return "SHARE_FAIL_SERVER_ERROR";
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                /* ignored */ }
            closeConnection(conn);
        }
    }

    /**
     * Hủy chia sẻ một file với một người dùng cụ thể.
     */
    public String unshareFile(int fileId, int ownerId, String targetUsername) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return "UNSHARE_FAIL_SERVER_ERROR";
            }

            // 1. Kiểm tra quyền sở hữu file (Quan trọng)
            if (!isFileOwner(fileId, ownerId)) {
                return "UNSHARE_FAIL_AUTH";
            }

            // 2. Tìm ID của người nhận chia sẻ (targetUsername)
            int targetUserId = getUserIdByUsername(targetUsername);
            if (targetUserId == -1) {
                return "UNSHARE_FAIL_USER_NOT_FOUND";
            }

            // 3. Xóa bản ghi chia sẻ (Chỉ xóa bản ghi do ownerId tạo)
            String deleteSQL = "DELETE FROM file_shares WHERE file_id = ? AND shared_with_user_id = ? AND shared_by_user_id = ?";
            try (PreparedStatement psDelete = conn.prepareStatement(deleteSQL)) {
                psDelete.setInt(1, fileId);
                psDelete.setInt(2, targetUserId);
                psDelete.setInt(3, ownerId);

                int rowsAffected = psDelete.executeUpdate();

                if (rowsAffected > 0) {
                    return "UNSHARE_SUCCESS";
                } else {
                    // Nếu rowsAffected = 0, có thể do bản ghi không tồn tại hoặc ownerId không phải shared_by_user_id
                    return "UNSHARE_FAIL_NOT_SHARED";
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi CSDL trong unshareFile: " + e.getMessage());
            return "UNSHARE_FAIL_SERVER_ERROR";
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * Lấy danh sách những người dùng đã được chia sẻ một file.
     */
    public String getSharedUsersByFile(int fileId, int ownerId) {
        StringBuilder result = new StringBuilder("SHARELIST_START:");
        Connection conn = null;

        try {
            conn = getConnection();
            if (conn == null) {
                return "SHARELIST_FAIL_SERVER_ERROR";
            }

            if (!isFileOwner(fileId, ownerId, conn)) {
                return "SHARELIST_FAIL_AUTH";
            }

            String sql = "SELECT u.username, fs.permission_level, fs.shared_at, fs.share_expiry "
                    + "FROM file_shares fs "
                    + "JOIN users u ON fs.shared_with_user_id = u.user_id "
                    + "WHERE fs.file_id = ? AND fs.shared_by_user_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, fileId);
                ps.setInt(2, ownerId);

                try (ResultSet rs = ps.executeQuery()) {
                    boolean hasData = false;
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    while (rs.next()) {
                        if (hasData) {
                            result.append(";");
                        }

                        String sharedAt = dateFormat.format(rs.getTimestamp("shared_at"));

                        // Lấy thời gian hết hạn và chuyển thành chuỗi (có thể là null)
                        Timestamp expiryTimestamp = rs.getTimestamp("share_expiry");
                        String expiryString = (expiryTimestamp != null) ? dateFormat.format(expiryTimestamp) : "NULL";

                        result.append(rs.getString("username"))
                                .append("|")
                                .append(rs.getInt("permission_level"))
                                .append("|")
                                .append(sharedAt)
                                .append("|")
                                .append(expiryString);

                        hasData = true;
                    }

                    if (!hasData) {
                        return "SHARELIST_EMPTY";
                    }
                }
            }

            return result.toString();

        } catch (SQLException e) {
            System.err.println("Lỗi CSDL trong getSharedUsersByFile: " + e.getMessage());
            return "SHARELIST_FAIL_SERVER_ERROR";
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * Cập nhật quyền và thời hạn chia sẻ của một file cho một người dùng.
     */
    public String updateFileSharePermission(int fileId, int ownerId, String targetUsername, int newPermissionLevel, int expiryMinutes) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return "UPDATE_FAIL_SERVER_ERROR";
            }

            // Tìm ID của người nhận chia sẻ
            int targetUserId = getUserIdByUsername(targetUsername);
            if (targetUserId == -1) {
                return "UPDATE_FAIL_USER_NOT_FOUND";
            }

            // Tính toán thời gian hết hạn mới
            Timestamp newExpiryTime = null;
            if (expiryMinutes > 0) {
                // Lấy thời gian bắt đầu chia sẻ từ CSDL để tính toán lại cho chính xác
                String getSharedAtSql = "SELECT shared_at FROM file_shares WHERE file_id = ? AND shared_with_user_id = ?";
                try (PreparedStatement ps = conn.prepareStatement(getSharedAtSql)) {
                    ps.setInt(1, fileId);
                    ps.setInt(2, targetUserId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        Timestamp sharedAt = rs.getTimestamp("shared_at");
                        long expiryMillis = sharedAt.getTime() + (long) expiryMinutes * 60 * 1000;
                        newExpiryTime = new Timestamp(expiryMillis);
                    } else {
                        return "UPDATE_FAIL_NOT_SHARED";
                    }
                }
            }

            // Cập nhật cả quyền và thời gian hết hạn
            String updateSQL = "UPDATE file_shares SET permission_level = ?, share_expiry = ? WHERE file_id = ? AND shared_with_user_id = ? AND shared_by_user_id = ?";
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSQL)) {
                psUpdate.setInt(1, newPermissionLevel);
                psUpdate.setTimestamp(2, newExpiryTime);
                psUpdate.setInt(3, fileId);
                psUpdate.setInt(4, targetUserId);
                psUpdate.setInt(5, ownerId);

                int rowsAffected = psUpdate.executeUpdate();

                if (rowsAffected > 0) {
                    return "UPDATE_SUCCESS";
                } else {
                    return "UPDATE_FAIL_NOT_SHARED";
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL trong updateFileSharePermission: " + e.getMessage());
            return "UPDATE_FAIL_SERVER_ERROR";
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * Xóa tất cả các bản ghi chia sẻ đã hết hạn khỏi CSDL. Được gọi định kỳ bởi
     * tác vụ chạy nền trên server.
     */
    public void deleteExpiredShares() {
        String sql = "DELETE FROM file_shares WHERE share_expiry IS NOT NULL AND share_expiry <= ?";
        int affectedRows = 0;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(1, now);
            affectedRows = ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Lỗi khi xóa các chia sẻ đã hết hạn: " + e.getMessage());
            e.printStackTrace();
        }
        if (affectedRows > 0) {
            System.out.println("Đã dọn dẹp " + affectedRows + " chia sẻ hết hạn.");
        }
    }
    // Trong FileDAO.java

    /**
     * Lấy danh sách file CỦA CHỦ SỞ HỮU trong một thư mục cụ thể.
     *
     * @param ownerId ID người sở hữu.
     * @param folderId ID thư mục (null = lấy file ở gốc).
     * @return List các đối tượng File (chỉ file sở hữu).
     */
    public List<File> getFilesByFolder(int ownerId, Integer folderId) {
        List<File> files = new ArrayList<>();
        String sql;

        if (folderId == null) {
            sql = "SELECT file_id, file_name, file_size, created_at, owner_id FROM files WHERE owner_id = ? AND folder_id IS NULL ORDER BY file_name";
        } else {
            sql = "SELECT file_id, file_name, file_size, created_at, owner_id FROM files WHERE owner_id = ? AND folder_id = ? ORDER BY file_name";
        }

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) {
                return null;
            }

            ps.setInt(1, ownerId);
            if (folderId != null) {
                ps.setInt(2, folderId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    File file = new File();
                    file.setFileId(rs.getInt("file_id"));
                    file.setFileName(rs.getString("file_name"));
                    file.setFileSize(rs.getLong("file_size"));
                    file.setUploadedAt(rs.getTimestamp("created_at"));
                    file.setOwnerId(rs.getInt("owner_id"));
                    file.setIsSharedToMe(false); // Đây là file sở hữu
                    file.setSharerName("");
                    files.add(file);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi liệt kê file theo thư mục: " + e.getMessage());
            return null;
        }
        return files;
    }

    /**
     * Lấy danh sách file ĐƯỢC CHIA SẺ VỚI BẠN (chỉ hiển thị ở thư mục gốc).
     *
     * @param userId ID của người dùng (người nhận chia sẻ).
     * @return List các đối tượng File (chỉ file được chia sẻ).
     */
    public List<File> getSharedFilesForUser(int userId) {
        List<File> files = new ArrayList<>();
        String sql = "SELECT f.file_id, f.file_name, f.file_size, f.created_at, f.owner_id, u.username AS sharer_name "
                + "FROM files f "
                + "JOIN file_shares fs ON f.file_id = fs.file_id "
                + "JOIN users u ON fs.shared_by_user_id = u.user_id "
                + "WHERE fs.shared_with_user_id = ? "
                + "ORDER BY fs.shared_at DESC";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            if (conn == null) {
                return null;
            }

            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    File file = new File();
                    file.setFileId(rs.getInt("file_id"));
                    file.setFileName(rs.getString("file_name"));
                    file.setFileSize(rs.getLong("file_size"));
                    file.setUploadedAt(rs.getTimestamp("created_at"));
                    file.setOwnerId(rs.getInt("owner_id"));
                    file.setIsSharedToMe(true); // Đây là file được chia sẻ
                    file.setSharerName(rs.getString("sharer_name"));
                    files.add(file);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi lấy file được chia sẻ: " + e.getMessage());
            return null;
        }
        return files;
    }

    /**
     * Di chuyển một file vào một thư mục mới.
     *
     * @param fileId ID file cần di chuyển.
     * @param newFolderId ID thư mục cha mới (null = di chuyển ra gốc).
     * @param ownerId ID người sở hữu (để xác thực).
     * @return true nếu thành công.
     */
    public boolean moveFile(int fileId, Integer newFolderId, int ownerId) {
        String sql = "UPDATE files SET folder_id = ? WHERE file_id = ? AND owner_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) {
                return false;
            }

            if (newFolderId != null) {
                ps.setInt(1, newFolderId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, fileId);
            ps.setInt(3, ownerId);

            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi di chuyển file: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Lấy tên file dựa trên ID (dùng cho mục đích log).
     */
    public String getFileNameById(int fileId, int ownerId) {
        String sql = "SELECT file_name FROM files WHERE file_id = ? AND owner_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) {
                return null;
            }
            ps.setInt(1, fileId);
            ps.setInt(2, ownerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("file_name");
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi lấy tên file: " + ex.getMessage());
        }
        return null; // Không tìm thấy hoặc lỗi
    }
}
