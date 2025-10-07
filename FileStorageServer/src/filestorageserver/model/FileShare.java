/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver.model;

import java.sql.Timestamp;

public class FileShare {
    private int shareId;
    private int fileId;
    private int sharedWithUserId; // Người nhận chia sẻ
    private int sharedByUserId;   // Chủ sở hữu/Người chia sẻ
    private int permissionLevel;  // Mức quyền: 1 (Read Only), 2 (Read/Write)
    private Timestamp sharedAt;

    // Constructor (Bạn có thể tùy chỉnh nếu cần)
    public FileShare() {
    }

    // Getters and Setters

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
}
