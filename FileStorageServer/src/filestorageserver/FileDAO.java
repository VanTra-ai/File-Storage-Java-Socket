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

public class FileDAO {

    // Phương thức tiện ích để lấy kết nối
    private Connection getConnection() {
        // Gọi lớp MyConnection nằm trong cùng package
        return new MyConnection().getConnection();
    }

    // 1. THÊM METADATA (SAU KHI UPLOAD FILE VẬT LÝ)
    /**
     * Chèn metadata file mới vào CSDL.
     *
     * @param file Đối tượng File chứa thông tin metadata.
     * @return ID của file vừa được chèn, hoặc -1 nếu thất bại.
     */
    public int insertFileMetadata(File file) {
        String sql = "INSERT INTO Files (owner_id, file_name, file_path, file_size, mime_type, created_at, is_shared) VALUES (?, ?, ?, ?, ?, NOW(), ?)";
        int generatedFileId = -1;

        // Sử dụng Statement.RETURN_GENERATED_KEYS để lấy ID vừa được tạo
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
                // Lấy khóa chính (file_id) vừa được sinh ra
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

    // 2. LẤY FILE METADATA THEO ID (KIỂM TRA QUYỀN TRUY CẬP)
    /**
     * Lấy metadata của một file cụ thể và kiểm tra quyền sở hữu.
     *
     * @param fileId ID của file.
     * @param ownerId ID của người dùng (để kiểm tra quyền).
     * @return Đối tượng File nếu tìm thấy và người dùng có quyền, null nếu
     * không.
     */
    public File getFileById(int fileId, int ownerId) {
        // Chỉ lấy file nếu nó thuộc về owner_id HOẶC nó được đánh dấu là is_shared = 1
        String sql = "SELECT * FROM Files WHERE file_id = ? AND (owner_id = ? OR is_shared = 1)";

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return null;
            }

            ps.setInt(1, fileId);
            ps.setInt(2, ownerId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToFile(rs); // Sử dụng hàm map tiện ích
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi lấy file theo ID: " + ex.getMessage());
        }
        return null;
    }

    // 3. LIỆT KÊ FILE CỦA NGƯỜI DÙNG
    /**
     * Lấy danh sách tất cả file thuộc sở hữu của một người dùng.
     *
     * @param ownerId ID người dùng.
     * @return Danh sách các đối tượng File.
     */
    public List<File> listUserFiles(int ownerId) {
        List<File> fileList = new ArrayList<>();
        String sql = "SELECT * FROM Files WHERE owner_id = ? ORDER BY created_at DESC";

        try (Connection con = getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (con == null) {
                return fileList;
            }

            ps.setInt(1, ownerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    fileList.add(mapResultSetToFile(rs));
                }
            }
        } catch (SQLException ex) {
            System.err.println("Lỗi CSDL khi liệt kê file: " + ex.getMessage());
        }
        return fileList;
    }

    // 4. XÓA METADATA (SAU KHI XÓA FILE VẬT LÝ)
    /**
     * Xóa metadata của một file khỏi CSDL.
     *
     * @param fileId ID của file cần xóa.
     * @param ownerId ID người dùng (để xác nhận quyền sở hữu).
     * @return true nếu xóa thành công, false nếu thất bại hoặc không có quyền.
     */
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

    // PHƯƠNG THỨC TIỆN ÍCH (HELPER METHOD)
    /**
     * Ánh xạ dữ liệu từ ResultSet sang đối tượng File.
     */
    private File mapResultSetToFile(ResultSet rs) throws SQLException {
        File file = new File();
        file.setFileId(rs.getInt("file_id"));
        file.setOwnerId(rs.getInt("owner_id"));
        file.setFileName(rs.getString("file_name"));
        file.setFilePath(rs.getString("file_path"));
        file.setFileSize(rs.getLong("file_size"));
        file.setFileType(rs.getString("mime_type"));
        file.setUploadedAt(rs.getTimestamp("created_at"));
        file.setLastModified(rs.getTimestamp("last_modified"));
        file.setIsShared(rs.getBoolean("is_shared"));
        return file;
    }
}
