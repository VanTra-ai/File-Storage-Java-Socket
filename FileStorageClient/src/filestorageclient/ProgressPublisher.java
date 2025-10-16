package filestorageclient;

/**
 * Một interface đơn giản để làm kênh liên lạc, cho phép các lớp bên ngoài
 * gửi dữ liệu tiến độ về cho SwingWorker một cách an toàn.
 */
public interface ProgressPublisher {
    void publishProgress(ClientSocketManager.ProgressData data);
}