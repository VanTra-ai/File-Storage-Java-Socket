/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver.model;

import java.sql.Timestamp;

public class File {

    // Tương ứng với cột file_id (Khóa chính)
    private int fileId;

    // Tương ứng với cột owner_id (Khóa ngoại trỏ đến Users.user_id)
    private int ownerId;

    // Tên file gốc mà người dùng nhìn thấy
    private String fileName;

    // Đường dẫn vật lý của file trên Server (ví dụ: C:/storage/user_1/file_xyz.dat)
    private String filePath;

    // Kích thước file (bytes)
    private long fileSize;

    // Kiểu file MIME (ví dụ: image/png, application/pdf)
    private String fileType;

    // Thời gian upload (created_at)
    private Timestamp uploadedAt;

    // Thời gian sửa đổi cuối cùng (last_modified)
    private Timestamp lastModified;

    // Trạng thái chia sẻ (1: Shared, 0: Private)
    private boolean isShared;

    // --- CONSTRUCTORS ---
    public File() {
    }

    // Constructor khi lấy dữ liệu từ CSDL
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

    // --- GETTERS & SETTERS ---
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
}
