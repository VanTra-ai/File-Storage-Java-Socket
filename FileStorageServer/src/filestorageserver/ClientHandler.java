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
    private final ServerActivityListener listener;

    private static final Map<String, CommandHandler> commandMap = new HashMap<>();
    private static final ComplexCommandHandler complexCommandHandler = new ComplexCommandHandler();

    public static final String STATUS_UPLOADING = "Uploading...";
    public static final String STATUS_DOWNLOADING = "Downloading...";
    public static final String STATUS_LISTING = "Listing Files...";
    public static final String STATUS_DELETING = "Deleting...";
    public static final String STATUS_SHARING = "Sharing...";
    public static final String STATUS_AUTHENTICATED = "Authenticated";
    public static final String STATUS_CREATING_FOLDER = "Creating Folder...";
    public static final String STATUS_MOVING = "Moving Item...";

    // Khối static khởi tạo map một lần duy nhất, ánh xạ chuỗi lệnh tới lớp xử lý của nó.
    static {
        commandMap.put("CMD_LOGIN", new LoginCommandHandler());
        commandMap.put("CMD_REGISTER", new RegisterCommandHandler());
        commandMap.put("CMD_DOWNLOAD", new DownloadCommandHandler());
        commandMap.put("CMD_DELETE", new DeleteCommandHandler());
        commandMap.put("CMD_GET_FOLDERS", new GetFoldersCommandHandler());
        commandMap.put("CMD_GET_FILES_IN_FOLDER", new GetFilesInFolderCommandHandler());
        commandMap.put("CMD_CREATE_FOLDER", new CreateFolderCommandHandler());
        commandMap.put("CMD_MOVE_ITEM", new MoveItemCommandHandler());
        commandMap.put("CMD_RENAME_FOLDER", new RenameFolderCommandHandler());
        commandMap.put("CMD_DELETE_FOLDER", new DeleteFolderCommandHandler());
        commandMap.put("CMD_START_UPLOAD", new StartUploadCommandHandler());
        commandMap.put("CMD_UPLOAD_CHUNK", new UploadChunkCommandHandler());
        commandMap.put("CMD_COMPLETE_UPLOAD", new CompleteUploadCommandHandler());
        commandMap.put("CMD_GET_UPLOAD_STATUS", new GetUploadStatusCommandHandler());
        commandMap.put("CMD_GET_STORAGE_INFO", new GetStorageInfoCommandHandler());
    }

    public ClientHandler(Socket socket, ServerActivityListener listener) {
        this.clientSocket = socket;
        this.listener = listener;
        this.session = new ClientSession(this); // Truyền 'this' vào session để có tham chiếu ngược
    }

    public Socket getClientSocket() {
        return clientSocket;
    }

    @Override
    public void run() {
        // Thông báo sự kiện KẾT NỐI MỚI ngay khi luồng bắt đầu
        if (listener != null) {
            listener.onClientConnected(this);
        }

        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream()); DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            while (!clientSocket.isClosed() && clientSocket.isConnected()) {
                String commandKey = dis.readUTF();
                CommandHandler handler = commandMap.get(commandKey);

                // Cập nhật trạng thái "bận" trên Dashboard
                updateActivityStatus(commandKey);

                if (handler != null) {
                    // Truyền listener vào cho các lớp lệnh xử lý
                    handler.handle(session, dis, dos, this.listener);
                } else if (commandKey.startsWith("SHARE:") || commandKey.startsWith("UNSHARE:")
                        || commandKey.startsWith("SHARE_LIST:") || commandKey.startsWith("CHANGE_PERM:")) {
                    complexCommandHandler.handleCommandString(commandKey, session, dos, this.listener);
                } else {
                    dos.writeUTF("UNKNOWN_COMMAND");
                }

                // Sau khi xử lý xong, cập nhật lại trạng thái "rảnh"
                if (session.isLoggedIn() && listener != null) {
                    listener.onUserActivityChanged(this, STATUS_AUTHENTICATED);
                }
            }
        } catch (EOFException | SocketException e) {
            // Lỗi này xảy ra một cách bình thường khi client đóng ứng dụng đột ngột
            System.out.println("Client at " + clientSocket.getInetAddress().getHostAddress() + " disconnected.");
        } catch (IOException e) {
            System.err.println("Lỗi I/O với client " + clientSocket.getInetAddress().getHostAddress() + ": " + e.getMessage());
        } finally {
            // Thông báo sự kiện ĐĂNG XUẤT (nếu đã đăng nhập) và NGẮT KẾT NỐI
            if (listener != null) {
                if (session.isLoggedIn()) {
                    listener.onUserLoggedOut(session.getCurrentUsername());
                }
                listener.onClientDisconnected(this);
            }
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Cập nhật trạng thái hoạt động của client trên Dashboard.
     *
     * @param command Lệnh mà client vừa gửi.
     */
    private void updateActivityStatus(String command) {
        if (listener == null || !session.isLoggedIn()) {
            return;
        }

        String newStatus = null;
        if (command.startsWith("CMD_")) {
            switch (command) {
                case "CMD_UPLOAD":
                    newStatus = STATUS_UPLOADING;
                    break;
                case "CMD_DOWNLOAD":
                    newStatus = STATUS_DOWNLOADING;
                    break;
                case "CMD_LISTFILES":
                    newStatus = STATUS_LISTING;
                    break;
                case "CMD_DELETE":
                    newStatus = STATUS_DELETING;
                    break;
                case "CMD_CREATE_FOLDER":
                    newStatus = STATUS_CREATING_FOLDER;
                    break;
                case "CMD_MOVE_ITEM":
                    newStatus = STATUS_MOVING;
                    break;
            }
        } else if (command.startsWith("SHARE") || command.startsWith("UNSHARE") || command.startsWith("CHANGE_PERM")) {
            newStatus = STATUS_SHARING;
        }

        if (newStatus != null) {
            listener.onUserActivityChanged(this, newStatus);
        }
    }
}
