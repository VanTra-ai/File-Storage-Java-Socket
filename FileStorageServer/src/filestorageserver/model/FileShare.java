package filestorageserver.model;

import java.sql.Timestamp;

/**
 * Đại diện cho một bản ghi chia sẻ file, ánh xạ tới bảng 'file_shares' trong
 * CSDL.
 */
public class FileShare {

    /**
     * ID duy nhất của lượt chia sẻ, tương ứng với cột 'share_id'.
     */
    private int shareId;

    /**
     * ID của file được chia sẻ, tương ứng với cột 'file_id'.
     */
    private int fileId;

    /**
     * ID của người dùng được nhận chia sẻ, tương ứng với cột
     * 'shared_with_user_id'.
     */
    private int sharedWithUserId;

    /**
     * ID của người dùng thực hiện chia sẻ (chủ sở hữu), tương ứng với cột
     * 'shared_by_user_id'.
     */
    private int sharedByUserId;

    /**
     * Mức độ quyền hạn (ví dụ: 1 = chỉ đọc), tương ứng với cột
     * 'permission_level'.
     */
    private int permissionLevel;

    /**
     * Thời điểm bắt đầu chia sẻ, tương ứng với cột 'shared_at'.
     */
    private Timestamp sharedAt;

    /**
     * BỔ SUNG: Thời điểm lượt chia sẻ hết hạn, tương ứng với cột
     * 'share_expiry'. Có thể là null nếu chia sẻ không có thời hạn.
     */
    private Timestamp shareExpiry;

    /**
     * Constructor mặc định.
     */
    public FileShare() {
    }

    //<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public int getShareId() {
        return shareId;
    }

    public void setShareId(int shareId) {
        this.shareId = shareId;
    }

    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public int getSharedWithUserId() {
        return sharedWithUserId;
    }

    public void setSharedWithUserId(int sharedWithUserId) {
        this.sharedWithUserId = sharedWithUserId;
    }

    public int getSharedByUserId() {
        return sharedByUserId;
    }

    public void setSharedByUserId(int sharedByUserId) {
        this.sharedByUserId = sharedByUserId;
    }

    public int getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public Timestamp getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(Timestamp sharedAt) {
        this.sharedAt = sharedAt;
    }

    public Timestamp getShareExpiry() {
        return shareExpiry;
    }

    public void setShareExpiry(Timestamp shareExpiry) {
        this.shareExpiry = shareExpiry;
    }
    //</editor-fold>
}
