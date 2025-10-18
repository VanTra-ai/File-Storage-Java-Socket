package filestorageserver.model;

import java.sql.Timestamp;

/**
 * Đại diện cho một đối tượng Thư mục (Folder), ánh xạ tới bảng 'folders'.
 */
public class Folder {

    private int folderId;
    private String folderName;
    private int ownerId;

    /**
     * ID của thư mục cha. Sử dụng kiểu 'Integer' (object) thay vì 'int'
     * (primitive) để nó có thể chấp nhận giá trị 'null' (cho thư mục gốc).
     */
    private Integer parentFolderId;

    private Timestamp createdAt;

    public Folder() {
    }

    //<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public int getFolderId() {
        return folderId;
    }

    public void setFolderId(int folderId) {
        this.folderId = folderId;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public int getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(int ownerId) {
        this.ownerId = ownerId;
    }

    public Integer getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(Integer parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
    //</editor-fold>
}
