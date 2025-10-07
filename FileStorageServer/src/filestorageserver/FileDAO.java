/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver;

import filestorageserver.model.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;

public class FileDAO {

    // Phương thức tiện ích để lấy kết nối
    private Connection getConnection() {
        // Giả định MyConnection nằm trong cùng package và có phương thức getConnection()
        return new MyConnection().getConnection();
    }

    private void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Lỗi đóng kết nối: " + e.getMessage());
        }
    }

    // 1. THÊM METADATA (SAU KHI UPLOAD FILE VẬT LÝ)
    public int insertFileMetadata(File file) {
        String sql = "INSERT INTO Files (owner_id, file_name, file_path, file_size, mime_type, created_at, is_shared) VALUES (?, ?, ?, ?, ?, NOW(), ?)";
        int generatedFileId = -1;

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (con == null) {
                return -1;
            }

            ps.setInt(1, file.getOwnerId());
            ps.setString(2, file.getFileName());
            ps.setString(3, file.getFilePath());
            ps.setLong(4, file.getFileSize());
            ps.setString(5, file.getFileType());
            ps.setBoolean(6, file.isIsShared());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedFileId = rs.getInt(1);
                    }
                }
                System.out.println("Metadata file được thêm thành công. ID: " + generatedFileId);
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi chèn metadata file: " + ex.getMessage());
        }
        return generatedFileId;
    }

    // 2. LẤY FILE METADATA VÀ KIỂM TRA QUYỀN TRUY CẬP (Dùng cho Download)
    public File getFileForDownload(int fileId, int userId) {
        File file = null;
        // Kiểm tra nếu người dùng là chủ sở hữu HOẶC tồn tại bản ghi chia sẻ
        String sql = "SELECT f.*, "
                + "CASE WHEN f.owner_id = ? THEN 0 ELSE 1 END AS is_shared_to_me "
                + "FROM files f "
                + "LEFT JOIN file_shares fs ON f.file_id = fs.file_id AND fs.shared_with_user_id = ? "
                + "WHERE f.file_id = ? AND (f.owner_id = ? OR fs.shared_with_user_id IS NOT NULL)";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

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
                    // Thêm các trường cần thiết khác cho Download
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

    // 3. LIỆT KÊ FILE CỦA NGƯỜI DÙNG
    public List<File> listUserFiles(int userId) {
        List<File> files = new ArrayList<>();
        // Sử dụng UNION ALL để gộp file sở hữu và file được chia sẻ
        String sql = "SELECT file_id, file_name, file_size, created_at, owner_id, 0 AS is_shared_to_me, NULL AS sharer_name " // 0: Owned, Sharer là NULL
                + "FROM files WHERE owner_id = ? "
                + "UNION ALL "
                + "SELECT f.file_id, f.file_name, f.file_size, f.created_at, f.owner_id, 1 AS is_shared_to_me, u.username AS sharer_name " // 1: Shared, Sharer là tên người chia sẻ
                + "FROM files f "
                + "JOIN file_shares fs ON f.file_id = fs.file_id "
                + "JOIN users u ON fs.shared_by_user_id = u.user_id "
                + "WHERE fs.shared_with_user_id = ? "
                + "ORDER BY created_at DESC";

        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    File file = new File();
                    file.setFileId(rs.getInt("file_id"));
                    file.setFileName(rs.getString("file_name"));
                    file.setFileSize(rs.getLong("file_size"));
                    file.setUploadedAt(rs.getTimestamp("created_at"));
                    file.setOwnerId(rs.getInt("owner_id"));
                    file.setIsSharedToMe(rs.getInt("is_shared_to_me") == 1);
                    String sharerName = rs.getString("sharer_name");
                    file.setSharerName(sharerName != null ? sharerName : "");

                    files.add(file);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi liệt kê file: " + e.getMessage());
        }
        return files;
    }

    // 4. XÓA METADATA (SAU KHI XÓA FILE VẬT LÝ)
    public boolean deleteFileMetadata(int fileId, int ownerId) {
        // Đảm bảo chỉ chủ sở hữu mới có thể xóa metadata
        String sql = "DELETE FROM Files WHERE file_id = ? AND owner_id = ?";

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) {
                return false;
            }

            ps.setInt(1, fileId);
            ps.setInt(2, ownerId);

            int affectedRows = ps.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi xóa metadata file: " + ex.getMessage());
        }
        return false;
    }

    // =========================================================================
    // PHẦN XỬ LÝ CHIA SẺ VÀ QUYỀN TRUY CẬP (Đã Tối Ưu)
    // =========================================================================
    /**
     * Lấy ID người dùng (UserId) từ tên đăng nhập (Username).
     *
     * @param username Tên đăng nhập.
     * @return UserId nếu tìm thấy, -1 nếu không tồn tại hoặc lỗi.
     */
    public int getUserIdByUsername(String username) {
        String sql = "SELECT user_id FROM Users WHERE Username = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi DAO khi tìm user_id theo Username: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Kiểm tra xem người dùng có phải là chủ sở hữu file không.
     */
    public boolean isFileOwner(int fileId, int ownerId) {
        String sql = "SELECT file_id FROM Files WHERE file_id = ? AND owner_id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, fileId);
            ps.setInt(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // True nếu tìm thấy
            }
        } catch (SQLException e) {
            System.err.println("Lỗi DAO khi kiểm tra chủ sở hữu file: " + e.getMessage());
            return false;
        }
    }

    // Phương thức tiện ích để ánh xạ permission string sang int
    private int mapPermissionToLevel(String permission) {
        // Chỉ hỗ trợ mức quyền 1 (Download Only) như logic cũ của bạn
        return 1;
    }

    /**
     * 5. CHIA SẺ FILE (Sử dụng username để nhất quán với ClientHandler) Thao
     * tác Transaction đảm bảo tính toàn vẹn CSDL.
     *
     * * @param fileId ID file.
     * @param ownerId ID của người chia sẻ (chủ sở hữu).
     * @param targetUsername Username người nhận chia sẻ.
     * @param permissionLevel Mức quyền.
     * @return Mã kết quả: SHARE_SUCCESS, SHARE_FAIL_AUTH,
     * SHARE_FAIL_USER_NOT_FOUND, SHARE_FAIL_ALREADY_SHARED,
     * SHARE_FAIL_SELF_SHARE, SHARE_FAIL_SERVER_ERROR
     */
    public String shareFile(int fileId, int ownerId, String targetUsername, int permissionLevel) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return "SHARE_FAIL_SERVER_ERROR";
            }
            conn.setAutoCommit(false);

            // 1. Kiểm tra quyền sở hữu file (Quan trọng)
            if (!isFileOwner(fileId, ownerId)) {
                conn.rollback();
                return "SHARE_FAIL_AUTH";
            }

            // 2. Tìm ID của người nhận chia sẻ (targetUsername)
            int targetUserId = getUserIdByUsername(targetUsername);
            if (targetUserId == -1) {
                conn.rollback();
                return "SHARE_FAIL_USER_NOT_FOUND";
            }

            // 3. Kiểm tra chia sẻ cho chính mình
            if (targetUserId == ownerId) {
                conn.rollback();
                return "SHARE_FAIL_SELF_SHARE";
            }

            // 4. Kiểm tra xem đã chia sẻ chưa (Tránh trùng lặp)
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

            // 5. Chèn bản ghi chia sẻ
            String insertSQL = "INSERT INTO file_shares (file_id, shared_with_user_id, shared_by_user_id, permission_level, shared_at) VALUES (?, ?, ?, ?, NOW())";
            try (PreparedStatement psShare = conn.prepareStatement(insertSQL)) {
                psShare.setInt(1, fileId);
                psShare.setInt(2, targetUserId);
                psShare.setInt(3, ownerId);
                psShare.setInt(4, permissionLevel);
                psShare.executeUpdate();
            }

            conn.commit();
            return "SHARE_SUCCESS";

        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                System.err.println("Lỗi rollback: " + ex.getMessage());
            }
            System.err.println("Lỗi CSDL trong shareFile: " + e.getMessage());
            return "SHARE_FAIL_SERVER_ERROR";
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("Lỗi setAutoCommit(true): " + e.getMessage());
            }
            closeConnection(conn);
        }
    }

    /**
     * 6. HỦY CHIA SẺ FILE (Sử dụng username để nhất quán với ClientHandler)
     *
     * @param fileId ID file.
     * @param ownerId ID của người thực hiện hủy chia sẻ (chủ sở hữu).
     * @param targetUsername Username người bị hủy quyền.
     * @return Mã kết quả: UNSHARE_SUCCESS, UNSHARE_FAIL_AUTH,
     * UNSHARE_FAIL_USER_NOT_FOUND, UNSHARE_FAIL_NOT_SHARED,
     * UNSHARE_FAIL_SERVER_ERROR
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
            // LƯU Ý: ownerId ở đây là shared_by_user_id (người chủ sở hữu file)
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
     * 7. LẤY DANH SÁCH USER ĐƯỢC CHIA SẺ (Chỉ dành cho chủ sở hữu)
     *
     * @param fileId ID file.
     * @param ownerId ID của chủ sở hữu file (người yêu cầu).
     * @return Chuỗi kết quả: SHARELIST_START:user1|perm1;user2|perm2;... hoặc
     * SHARELIST_EMPTY, SHARELIST_FAIL_AUTH, SHARELIST_FAIL_SERVER_ERROR
     */
    public String getSharedUsersByFile(int fileId, int ownerId) {
        StringBuilder result = new StringBuilder("SHARELIST_START:");
        Connection conn = null;

        try {
            conn = getConnection();
            if (conn == null) {
                return "SHARELIST_FAIL_SERVER_ERROR";
            }

            // 1. Kiểm tra quyền sở hữu
            if (!isFileOwner(fileId, ownerId)) {
                return "SHARELIST_FAIL_AUTH";
            }

            // 2. Truy vấn JOIN để lấy username và permission_level
            String sql = "SELECT u.username, fs.permission_level, fs.shared_at "
                    + "FROM file_shares fs "
                    + "JOIN users u ON fs.shared_with_user_id = u.user_id "
                    + "WHERE fs.file_id = ? AND fs.shared_by_user_id = ?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, fileId);
                ps.setInt(2, ownerId);

                try (ResultSet rs = ps.executeQuery()) {
                    boolean first = true;
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                    while (rs.next()) {
                        if (!first) {
                            result.append(";");
                        }

                        String sharedAt = dateFormat.format(rs.getTimestamp("shared_at"));

                        // Định dạng: username|permissionLevel|SharingDay
                        result.append(rs.getString("username"))
                                .append("|")
                                .append(rs.getInt("permission_level"))
                                .append("|")
                                .append(sharedAt);

                        first = false;
                    }
                }
            }

            if (result.toString().equals("SHARELIST_START:")) {
                return "SHARELIST_EMPTY";
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
     * 8. CẬP NHẬT QUYỀN CHIA SẺ FILE. Chỉ có chủ sở hữu mới có thể thay đổi
     * quyền.
     *
     * @param fileId ID file.
     * @param ownerId ID của người chia sẻ (chủ sở hữu).
     * @param targetUsername Username người nhận chia sẻ.
     * @param newPermissionLevel Mức quyền mới.
     * @return Mã kết quả: UPDATE_SUCCESS, UPDATE_FAIL_AUTH,
     * UPDATE_FAIL_USER_NOT_FOUND, UPDATE_FAIL_NOT_SHARED,
     * UPDATE_FAIL_SERVER_ERROR
     */
    public String updateFileSharePermission(int fileId, int ownerId, String targetUsername, int newPermissionLevel) {
        Connection conn = null;
        try {
            conn = getConnection();
            if (conn == null) {
                return "UPDATE_FAIL_SERVER_ERROR";
            }

            conn.setAutoCommit(false); // Bắt đầu Transaction

            // 1. Kiểm tra quyền sở hữu file (Bắt buộc)
            // Sử dụng hàm isFileOwner không truyền Connection, hàm này tự mở/đóng kết nối riêng
            if (!isFileOwner(fileId, ownerId)) {
                conn.rollback(); // Rollback nếu có vấn đề (mặc dù chưa làm gì)
                return "UPDATE_FAIL_AUTH";
            }

            // 2. Tìm ID của người nhận chia sẻ
            int targetUserId = getUserIdByUsername(targetUsername);
            if (targetUserId == -1) {
                conn.rollback();
                return "UPDATE_FAIL_USER_NOT_FOUND";
            }

            // 3. Cập nhật permission_level trong bảng file_shares
            String updateSQL = "UPDATE file_shares SET permission_level = ? WHERE file_id = ? AND shared_with_user_id = ? AND shared_by_user_id = ?";
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSQL)) {

                // THAY ĐỔI THỨ TỰ THAM SỐ ĐỂ KHỚP VỚI CÂU LỆNH SQL
                psUpdate.setInt(1, newPermissionLevel); // 1. permission_level = ?
                psUpdate.setInt(2, fileId);            // 2. WHERE file_id = ?
                psUpdate.setInt(3, targetUserId);      // 3. AND shared_with_user_id = ?
                psUpdate.setInt(4, ownerId);           // 4. AND shared_by_user_id = ?

                int rowsAffected = psUpdate.executeUpdate();

                if (rowsAffected > 0) {
                    conn.commit();
                    return "UPDATE_SUCCESS";
                } else {
                    conn.rollback();
                    // Có thể là file đó chưa được chia sẻ với người dùng này
                    return "UPDATE_FAIL_NOT_SHARED";
                }
            } // psUpdate tự động đóng ở đây
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL trong updateFileSharePermission: " + e.getMessage());
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rbEx) {
                System.err.println("Lỗi rollback updateFileSharePermission: " + rbEx.getMessage());
            }
            return "UPDATE_FAIL_SERVER_ERROR";
        } finally {
            // Đảm bảo setAutoCommit(true) và đóng Connection
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                System.err.println("Lỗi setAutoCommit(true): " + e.getMessage());
            }
            closeConnection(conn); // Sử dụng hàm closeConnection tiện ích
        }
    }
}
