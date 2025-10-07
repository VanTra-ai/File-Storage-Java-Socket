/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

public class FileServer {

    // Cổng mà Server sẽ lắng nghe
    private static final int PORT = 12345;
    private static final int THREAD_POOL_SIZE = 10;
    private static ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    public static void main(String[] args) {
        // ĐƯỜNG DẪN TUYỆT ĐỐI CẦN THAY THẾ
        String absoluteKeyStorePath = "C:\\Users\\Admin\\OneDrive\\Documents\\NetBeansProjects\\File-Storage-Java-Socket\\Drivers\\SSL\\server.jks";
        try {
            // Đặt đường dẫn KeyStore (lưu ý đường dẫn tương đối từ thư mục gốc dự án)
            System.setProperty("javax.net.ssl.keyStore", absoluteKeyStorePath);
            // Đặt mật khẩu KeyStore
            System.setProperty("javax.net.ssl.keyStorePassword", "123456"); // SỬ DỤNG MẬT KHẨU CỦA BẠN

            System.out.println("--- FILE STORAGE SERVER ---");
            // TẠO SSLServerSocketFactory
            ServerSocketFactory ssf = SSLServerSocketFactory.getDefault();
            try (ServerSocket serverSocket = ssf.createServerSocket(PORT)) {
                System.out.println("Server đang lắng nghe kết nối tại cổng: " + PORT);

                // Vòng lặp liên tục chờ kết nối từ Client
                while (true) {
                    // Chấp nhận (accept) kết nối và tạo ra một Socket mới
                    Socket clientSocket = serverSocket.accept();

                    System.out.println("Client mới kết nối từ: "
                            + clientSocket.getInetAddress().getHostAddress());

                    ClientHandler handler = new ClientHandler(clientSocket);
                    pool.execute(handler);
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo Server Socket hoặc lỗi SSL: " + e.getMessage());
        }
    }
}
