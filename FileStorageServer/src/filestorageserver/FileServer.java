package filestorageserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.swing.SwingUtilities;

/**
 * Lớp chính của Server, chịu trách nhiệm khởi động, lắng nghe kết nối SSL, quản
 * lý các luồng xử lý client và các tác vụ chạy nền.
 */
public class FileServer {

    private static final int PORT = 12345;
    private static final int THREAD_POOL_SIZE = 10;
    private static final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    // Giới hạn dung lượng mỗi tài khoản (1 GB)
    public static final long USER_QUOTA_BYTES = 1 * 1024 * 1024 * 1024L; 
    // Giới hạn kích thước file tối đa (200 MB)
    public static final long MAX_FILE_SIZE_BYTES = 200 * 1024 * 1024L;
    // Ngưỡng cảnh báo (90%)
    public static final double QUOTA_WARNING_THRESHOLD = 0.9;

    /**
     * Phương thức chính khởi chạy toàn bộ server và giao diện Dashboard.
     */
    public static void main(String[] args) {
        // Tạo và hiển thị Dashboard trên Luồng Giao diện Đồ họa (EDT)
        DashboardFrame dashboard = new DashboardFrame();
        SwingUtilities.invokeLater(() -> {
            dashboard.setVisible(true);
        });

        // LƯU Ý: Các giá trị này nên được đọc từ một file cấu hình bên ngoài.
        String absoluteKeyStorePath = "C:\\Users\\Admin\\OneDrive\\Documents\\NetBeansProjects\\File-Storage-Java-Socket\\Drivers\\SSL\\server.jks";
        String keyStorePassword = "123456";

        System.setProperty("javax.net.ssl.keyStore", absoluteKeyStorePath);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);

        try {
            System.out.println("--- FILE STORAGE SERVER ---");
            startExpiredShareCleanupTask(dashboard); // Truyền dashboard vào để ghi log

            ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
            try (ServerSocket serverSocket = ssf.createServerSocket(PORT)) {
                dashboard.addLogMessage("Server is listening on SSL port: " + PORT);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    // Giao mỗi client cho một luồng xử lý, và truyền Dashboard vào làm "Người lắng nghe"
                    clientProcessingPool.execute(new ClientHandler(clientSocket, dashboard));
                }
            }
        } catch (IOException e) {
            dashboard.addLogMessage("FATAL ERROR: Could not start server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Khởi động và lập lịch cho tác vụ chạy nền để xóa các bản ghi chia sẻ đã
     * hết hạn.
     */
    private static void startExpiredShareCleanupTask(DashboardFrame dashboard) {
        Runnable cleanupTask = () -> {
            try {
                new FileDAO().deleteExpiredShares();
            } catch (Exception e) {
                System.err.println("Lỗi trong tác vụ dọn dẹp chia sẻ: " + e.getMessage());
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(cleanupTask, 10, 30, TimeUnit.SECONDS);
        dashboard.addLogMessage("Cleanup task for expired shares has been scheduled.");
    }
}
