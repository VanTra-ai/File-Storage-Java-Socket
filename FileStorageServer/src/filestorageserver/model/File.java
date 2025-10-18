package filestorageserver.model;

import java.sql.Timestamp;

/**
 * Đại diện cho một đối tượng File, ánh xạ tới bảng 'files' trong cơ sở dữ liệu.
 */
public class File {

    /**
     * ID duy nhất của file, tương ứng với cột 'file_id'.
     */
    private int fileId;

    /**
     * ID của người dùng sở hữu file, tương ứng với cột 'owner_id'.
     */
    private int ownerId;

    /**
     * Tên file gốc mà người dùng nhìn thấy, tương ứng với cột 'file_name'.
     */
    private String fileName;

    /**
     * Đường dẫn vật lý của file trên Server, tương ứng với cột 'file_path'.
     */
    private String filePath;

    /**
     * Kích thước file (bytes), tương ứng với cột 'file_size'.
     */
    private long fileSize;

    /**
     * Kiểu file MIME (ví dụ: image/png), tương ứng với cột 'mime_type'.
     */
    private String fileType;

    /**
     * Thời điểm file được tải lên, tương ứng với cột 'created_at'.
     */
    private Timestamp uploadedAt;

    /**
     * Thời điểm file được sửa đổi lần cuối, tương ứng với cột 'last_modified'.
     */
    private Timestamp lastModified;

    /**
     * Cờ đánh dấu file này có đang được chia sẻ hay không, tương ứng với cột
     * 'is_shared'.
     */
    private boolean isShared;

    /**
     * Thuộc tính logic (không có trong CSDL) để xác định file này có phải được
     * chia sẻ cho người dùng hiện tại hay không.
     */
    private boolean isSharedToMe;

    /**
     * Thuộc tính logic (không có trong CSDL) để lưu tên người đã chia sẻ file.
     */
    private String sharerName;

    /**
     * Constructor mặc định.
     */
    public File() {
    }

    /**
     * Constructor đầy đủ để khởi tạo một đối tượng File.
     */
    private Integer folderId;

    public File(int fileId, int ownerId, String fileName, String filePath, long fileSize, String fileType, Timestamp uploadedAt, Timestamp lastModified, boolean isShared) {
        this.fileId = fileId;
        this.ownerId = ownerId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.fileType = fileType;
        this.uploadedAt = uploadedAt;
        this.lastModified = lastModified;
        this.isShared = isShared;
    }

    //<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public int getFileId() {
        return fileId;
    }

    public void setFileId(int fileId) {
        this.fileId = fileId;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public boolean isIsShared() {
        return isShared;
    }

    public void setIsShared(boolean isShared) {
        this.isShared = isShared;
    }

    public Timestamp getLastModified() {
        return lastModified;
    }

    public void setLastModified(Timestamp lastModified) {
        this.lastModified = lastModified;
    }

    public boolean isIsSharedToMe() {
        return isSharedToMe;
    }

    public void setIsSharedToMe(boolean isSharedToMe) {
        this.isSharedToMe = isSharedToMe;
    }

    public String getSharerName() {
        return sharerName;
    }

    public void setSharerName(String sharerName) {
        this.sharerName = sharerName;
    }

    public Integer getFolderId() {
        return folderId;
    }

    public void setFolderId(Integer folderId) {
        this.folderId = folderId;
    }
    //</editor-fold>
}
