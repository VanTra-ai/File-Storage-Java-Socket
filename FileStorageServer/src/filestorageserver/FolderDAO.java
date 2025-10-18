package filestorageserver;

import filestorageserver.model.Folder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp Data Access Object (DAO) cho các thao tác liên quan đến thư mục. Chịu
 * trách nhiệm tương tác với bảng 'folders'.
 */
public class FolderDAO {

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
     * Tạo một thư mục mới trong CSDL.
     *
     * @param folderName Tên thư mục.
     * @param ownerId ID người sở hữu.
     * @param parentFolderId ID của thư mục cha (có thể là null).
     * @return ID của thư mục vừa tạo, hoặc -1 nếu thất bại.
     */
    public int createFolder(String folderName, int ownerId, Integer parentFolderId) {
        String sql = "INSERT INTO folders (folder_name, owner_id, parent_folder_id, created_at) VALUES (?, ?, ?, NOW())";
        int generatedFolderId = -1;
        Connection con = null; // Cần khai báo bên ngoài try-with-resources để dùng cho kiểm tra

        try {
            con = getConnection();
            if (con == null) {
                return -1;
            }

            if (folderNameExists(folderName, ownerId, parentFolderId, con, -1)) { // excludeFolderId = -1
                return -2; // Trả về mã lỗi mới (-2) cho tên trùng lặp
            }

            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, folderName);
                ps.setInt(2, ownerId);

                if (parentFolderId != null) {
                    ps.setInt(3, parentFolderId);
                } else {
                    ps.setNull(3, Types.INTEGER);
                }

                if (ps.executeUpdate() > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedFolderId = rs.getInt(1);
                        }
                    }
                }
            } // PreparedStatement được tự động đóng
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi tạo thư mục: " + ex.getMessage());
            generatedFolderId = -1; // Đảm bảo trả về -1 khi có lỗi SQL
        } finally {
            closeConnection(con); // Đóng kết nối
        }
        return generatedFolderId;
    }

    /**
     * Lấy danh sách các thư mục con trực tiếp của một thư mục cha.
     *
     * @param ownerId ID người sở hữu.
     * @param parentFolderId ID của thư mục cha (null = lấy thư mục gốc).
     * @return List các đối tượng Folder.
     */
    public List<Folder> getFoldersByParent(int ownerId, Integer parentFolderId) {
        List<Folder> folders = new ArrayList<>();
        String sql;

        if (parentFolderId == null) {
            sql = "SELECT * FROM folders WHERE owner_id = ? AND parent_folder_id IS NULL ORDER BY folder_name";
        } else {
            sql = "SELECT * FROM folders WHERE owner_id = ? AND parent_folder_id = ? ORDER BY folder_name";
        }

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) {
                return null;
            }

            ps.setInt(1, ownerId);
            if (parentFolderId != null) {
                ps.setInt(2, parentFolderId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Folder folder = new Folder();
                    folder.setFolderId(rs.getInt("folder_id"));
                    folder.setFolderName(rs.getString("folder_name"));
                    folder.setOwnerId(rs.getInt("owner_id"));
                    folder.setParentFolderId((Integer) rs.getObject("parent_folder_id"));
                    folder.setCreatedAt(rs.getTimestamp("created_at"));
                    folders.add(folder);
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi lấy danh sách thư mục: " + ex.getMessage());
            return null;
        }
        return folders;
    }

    /**
     * Di chuyển một thư mục vào một thư mục cha mới.
     *
     * @param folderId ID thư mục cần di chuyển.
     * @param newParentFolderId ID thư mục cha mới (null = di chuyển ra gốc).
     * @param ownerId ID người sở hữu (để xác thực).
     * @return true nếu thành công.
     */
    public boolean moveFolder(int folderId, Integer newParentFolderId, int ownerId) {
        // TODO: Thêm logic kiểm tra (ví dụ: không cho phép di chuyển thư mục vào chính nó)
        String sql = "UPDATE folders SET parent_folder_id = ? WHERE folder_id = ? AND owner_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) {
                return false;
            }

            if (newParentFolderId != null) {
                ps.setInt(1, newParentFolderId);
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setInt(2, folderId);
            ps.setInt(3, ownerId);

            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi di chuyển thư mục: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Xóa một thư mục (và các thư mục con, file con bên trong, tùy thuộc vào
     * CSDL).
     *
     * @param folderId ID thư mục cần xóa.
     * @param ownerId ID người sở hữu (để xác thực).
     * @return true nếu thành công.
     */
    public boolean deleteFolder(int folderId, int ownerId) {
        String sql = "DELETE FROM folders WHERE folder_id = ? AND owner_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) {
                return false;
            }

            ps.setInt(1, folderId);
            ps.setInt(2, ownerId);

            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi xóa thư mục: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Lấy tên thư mục dựa trên ID (dùng cho mục đích log).
     */
    public String getFolderNameById(int folderId, int ownerId) {
        String sql = "SELECT folder_name FROM folders WHERE folder_id = ? AND owner_id = ?";
        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            if (con == null) {
                return null;
            }
            ps.setInt(1, folderId);
            ps.setInt(2, ownerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("folder_name");
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi lấy tên thư mục: " + ex.getMessage());
        }
        return null; // Không tìm thấy hoặc lỗi
    }

    /**
     * Đổi tên một thư mục.
     *
     * @param folderId ID thư mục cần đổi tên.
     * @param newFolderName Tên mới.
     * @param ownerId ID người sở hữu (để xác thực).
     * @return true nếu thành công.
     */
    public boolean renameFolder(int folderId, String newFolderName, int ownerId) {
        String sql = "UPDATE folders SET folder_name = ? WHERE folder_id = ? AND owner_id = ?";
        Connection con = null; // Khai báo bên ngoài
        boolean success = false;

        try {
            con = getConnection();
            if (con == null) {
                return false;
            }

            // --- THÊM KIỂM TRA TRÙNG TÊN ---
            // Lấy parentFolderId của thư mục đang đổi tên để kiểm tra đúng chỗ
            Integer parentFolderId = getParentFolderId(folderId, ownerId, con);
            if (parentFolderId == null && folderId > 0) { // Kiểm tra xem thư mục có tồn tại không
                System.err.println("Đổi tên thất bại: Không tìm thấy thư mục ID " + folderId + " hoặc không có quyền.");
                return false;
            }

            // Kiểm tra tên mới có trùng với thư mục khác TRONG CÙNG thư mục cha không?
            if (folderNameExists(newFolderName, ownerId, parentFolderId, con, folderId)) { // Loại trừ chính nó
                System.err.println("Đổi tên thất bại: Tên '" + newFolderName + "' đã tồn tại trong thư mục cha.");
                return false; // Trả về false nếu tên trùng
            }
            // --- KẾT THÚC KIỂM TRA ---

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, newFolderName);
                ps.setInt(2, folderId);
                ps.setInt(3, ownerId);
                success = ps.executeUpdate() > 0;
            } // PreparedStatement được tự động đóng
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi đổi tên thư mục: " + ex.getMessage());
            success = false;
        } finally {
            closeConnection(con); // Đóng kết nối
        }
        return success;
    }

    /**
     * Helper method để lấy parent_folder_id (dùng trong rename). Trả về null
     * nếu thư mục ở gốc hoặc không tìm thấy. Trả về -1 nếu có lỗi SQL.
     */
    private Integer getParentFolderId(int folderId, int ownerId, Connection conn) throws SQLException {
        String sql = "SELECT parent_folder_id FROM folders WHERE folder_id = ? AND owner_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, folderId);
            ps.setInt(2, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Lấy giá trị, có thể là null nếu ở gốc
                    return (Integer) rs.getObject("parent_folder_id");
                } else {
                    return null; // Không tìm thấy thư mục (hoặc không có quyền) -> cũng xem như gốc để kiểm tra tên
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy parent_folder_id: " + e.getMessage());
            throw e; // Ném lại lỗi để renameFolder biết
        }
    }

    /**
     * Kiểm tra xem tên thư mục đã tồn tại trong thư mục cha hay chưa.
     *
     * @param folderName Tên cần kiểm tra.
     * @param ownerId ID chủ sở hữu.
     * @param parentFolderId ID thư mục cha (null = gốc).
     * @param conn Kết nối CSDL đang sử dụng (để dùng trong transaction).
     * @param excludeFolderId ID thư mục cần loại trừ khỏi việc kiểm tra (dùng
     * khi đổi tên), -1 nếu không loại trừ.
     * @return true nếu tên đã tồn tại.
     * @throws SQLException
     */
    private boolean folderNameExists(String folderName, int ownerId, Integer parentFolderId, Connection conn, int excludeFolderId) throws SQLException {
        String sql;
        // Xây dựng câu SQL dựa trên việc có kiểm tra thư mục gốc hay không
        // và có loại trừ thư mục nào không
        if (parentFolderId == null) {
            sql = "SELECT COUNT(*) FROM folders WHERE owner_id = ? AND parent_folder_id IS NULL AND folder_name = ?";
        } else {
            sql = "SELECT COUNT(*) FROM folders WHERE owner_id = ? AND parent_folder_id = ? AND folder_name = ?";
        }

        // Thêm điều kiện loại trừ nếu cần (cho chức năng đổi tên)
        if (excludeFolderId != -1) {
            sql += " AND folder_id != ?";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int paramIndex = 1;
            ps.setInt(paramIndex++, ownerId);
            if (parentFolderId != null) {
                ps.setInt(paramIndex++, parentFolderId);
            }
            ps.setString(paramIndex++, folderName);
            if (excludeFolderId != -1) {
                ps.setInt(paramIndex++, excludeFolderId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // Trả về true nếu count > 0 (tên đã tồn tại)
                }
            }
        }
        return false; // Mặc định là không tồn tại nếu có lỗi hoặc không tìm thấy
    }
}
