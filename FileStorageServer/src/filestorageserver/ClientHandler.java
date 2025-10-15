/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver;

import filestorageserver.commands.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ClientHandler extends Thread {

    private final Socket clientSocket;
    private final ClientSession session; // Sử dụng ClientSession để lưu trạng thái

    // Map lưu trữ tất cả các lệnh và lớp xử lý tương ứng
    private static final Map<String, CommandHandler> commandMap = new HashMap<>();
    private static final ComplexCommandHandler complexCommandHandler = new ComplexCommandHandler();

    // Khối static để khởi tạo map một lần duy nhất khi lớp được tải
    static {
        commandMap.put("CMD_LOGIN", new LoginCommandHandler());
        commandMap.put("CMD_REGISTER", new RegisterCommandHandler());
        commandMap.put("CMD_UPLOAD", new UploadCommandHandler());
        commandMap.put("CMD_DOWNLOAD", new DownloadCommandHandler());
        commandMap.put("CMD_LISTFILES", new ListFilesCommandHandler());
        commandMap.put("CMD_DELETE", new DeleteCommandHandler());
    }

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.session = new ClientSession(); // Mỗi client có một session riêng
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            while (clientSocket.isConnected()) {
                String commandKey = dis.readUTF();

                // Tìm lớp xử lý lệnh trong Map
                CommandHandler handler = commandMap.get(commandKey);

                if (handler != null) {
                    handler.handle(session, dis, dos);
                } else if (commandKey.startsWith("SHARE:") || commandKey.startsWith("UNSHARE:")
                        || commandKey.startsWith("SHARE_LIST:") || commandKey.startsWith("CHANGE_PERM:")) {
                    // Gọi phương thức xử lý chuỗi lệnh từ complexCommandHandler
                    complexCommandHandler.handleCommandString(commandKey, session, dos);
                } else {
                    dos.writeUTF("UNKNOWN_COMMAND");
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected or I/O error: " + clientSocket.getInetAddress());
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
