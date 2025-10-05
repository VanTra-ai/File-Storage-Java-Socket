/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {
    
    // Cổng mà Server sẽ lắng nghe
    private static final int PORT = 12345; 

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("--- FILE STORAGE SERVER ---");
            System.out.println("Server đang lắng nghe kết nối tại cổng: " + PORT);

            // Vòng lặp liên tục chờ kết nối từ Client
            while (true) {
                // Chấp nhận (accept) kết nối và tạo ra một Socket mới
                Socket clientSocket = serverSocket.accept(); 
                
                System.out.println("Client mới kết nối từ: " + 
                                   clientSocket.getInetAddress().getHostAddress());
                
                // Tạo một luồng (Thread) riêng biệt để xử lý Client này
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start(); // Khởi chạy luồng
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi tạo Server Socket: " + e.getMessage());
        }
    }
}
