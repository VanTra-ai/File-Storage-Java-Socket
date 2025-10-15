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

/**
 * Lớp chính của Server, chịu trách nhiệm khởi động, lắng nghe kết nối SSL, quản
 * lý các luồng xử lý client và các tác vụ chạy nền.
 */
public class FileServer {

    private static final int PORT = 12345;
    private static final int THREAD_POOL_SIZE = 10;
    private static final ExecutorService clientProcessingPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * Phương thức chính khởi chạy toàn bộ server.
     */
    public static void main(String[] args) {
        // LƯU Ý: Các giá trị này nên được đọc từ một file cấu hình bên ngoài.
        String absoluteKeyStorePath = "C:\\Users\\Admin\\OneDrive\\Documents\\NetBeansProjects\\File-Storage-Java-Socket\\Drivers\\SSL\\server.jks";
        String keyStorePassword = "123456";

        try {
            // Thiết lập các thuộc tính hệ thống cho SSL KeyStore
            System.setProperty("javax.net.ssl.keyStore", absoluteKeyStorePath);
            System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);

            System.out.println("--- FILE STORAGE SERVER ---");

            // Khởi động tác vụ nền để dọn dẹp các lượt chia sẻ hết hạn
            startExpiredShareCleanupTask();

            ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
            try (ServerSocket serverSocket = ssf.createServerSocket(PORT)) {
                System.out.println("Server đang lắng nghe kết nối SSL tại cổng: " + PORT);

                // Vòng lặp vô tận để chấp nhận các kết nối mới từ client
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client mới kết nối từ: " + clientSocket.getInetAddress().getHostAddress());

                    // Giao mỗi client cho một luồng trong pool để xử lý
                    clientProcessingPool.execute(new ClientHandler(clientSocket));
                }
            }
        } catch (IOException e) {
            System.err.println("Không thể khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Khởi động và lập lịch cho tác vụ chạy nền để xóa các bản ghi chia sẻ đã
     * hết hạn trong CSDL.
     */
    private static void startExpiredShareCleanupTask() {
        Runnable cleanupTask = () -> {
            try {
                new FileDAO().deleteExpiredShares();
            } catch (Exception e) {
                System.err.println("Lỗi trong tác vụ dọn dẹp chia sẻ: " + e.getMessage());
                e.printStackTrace();
            }
        };

        // Lập lịch: Chạy lần đầu sau 10 giây, sau đó lặp lại mỗi 30 giây.
        scheduler.scheduleAtFixedRate(cleanupTask, 10, 30, TimeUnit.SECONDS);
        System.out.println("Đã khởi động tác vụ dọn dẹp (chạy lần đầu sau 10s, lặp lại mỗi 30s).");
    }
}
