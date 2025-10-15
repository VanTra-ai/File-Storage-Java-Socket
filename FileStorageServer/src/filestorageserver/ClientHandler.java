package filestorageserver;

import filestorageserver.commands.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * Xử lý tất cả các giao tiếp cho một client kết nối duy nhất. Lớp này hoạt động
 * như một bộ điều phối, đọc các lệnh từ client và giao việc cho các lớp
 * CommandHandler tương ứng.
 */
public class ClientHandler extends Thread {

    private final Socket clientSocket;
    private final ClientSession session;

    private static final Map<String, CommandHandler> commandMap = new HashMap<>();
    private static final ComplexCommandHandler complexCommandHandler = new ComplexCommandHandler();

    // Khối static khởi tạo map một lần duy nhất, ánh xạ chuỗi lệnh tới lớp xử lý của nó.
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
        this.session = new ClientSession(); // Mỗi client có một phiên làm việc (session) riêng.
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            while (!clientSocket.isClosed() && clientSocket.isConnected()) {
                String commandKey = dis.readUTF();

                // Tìm lớp xử lý tương ứng trong map
                CommandHandler handler = commandMap.get(commandKey);

                if (handler != null) {
                    // Nếu là lệnh đơn giản (CMD_...), gọi handler tương ứng
                    handler.handle(session, dis, dos);
                } else if (commandKey.startsWith("SHARE:") || commandKey.startsWith("UNSHARE:")
                        || commandKey.startsWith("SHARE_LIST:") || commandKey.startsWith("CHANGE_PERM:")) {
                    // Nếu là lệnh chia sẻ dạng chuỗi, giao cho handler phức tạp
                    complexCommandHandler.handleCommandString(commandKey, session, dos);
                } else {
                    dos.writeUTF("UNKNOWN_COMMAND");
                }
            }
        } catch (EOFException | SocketException e) {
            // Đây là lỗi bình thường khi client đóng ứng dụng đột ngột.
            System.out.println("Client at " + clientSocket.getInetAddress().getHostAddress() + " disconnected.");
        } catch (IOException e) {
            System.err.println("Lỗi giao tiếp với client " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
