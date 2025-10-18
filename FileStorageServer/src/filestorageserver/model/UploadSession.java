package filestorageserver.model;

import java.sql.Timestamp;

/**
 * Model ánh xạ tới bảng upload_sessions.
 */
public class UploadSession {

    private String sessionId;
    private int userId;
    private String fileName;
    private String tempFilePath;
    private long totalSize;
    private long currentOffset;
    private int chunkSize;
    private String status;
    private Integer targetFolderId;
    private Timestamp createdAt;
    private Timestamp lastUpdatedAt;

    public UploadSession() {
    }

    //<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getTempFilePath() {
        return tempFilePath;
    }

    public void setTempFilePath(String tempFilePath) {
        this.tempFilePath = tempFilePath;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentOffset(long currentOffset) {
        this.currentOffset = currentOffset;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTargetFolderId() {
        return targetFolderId;
    }

    public void setTargetFolderId(Integer targetFolderId) {
        this.targetFolderId = targetFolderId;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Timestamp lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
    //</editor-fold>
}
