/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import filestorageserver.model.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class UploadCommandHandler implements CommandHandler {
    // Hằng số này cần được định nghĩa ở đây hoặc trong một file cấu hình chung
    private static final String SERVER_STORAGE_ROOT = "I:/FileStorageRoot/";

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        String fullFilePath = null;
        try {
            String originalFileName = dis.readUTF();
            long fileSize = dis.readLong();
            String fileType = dis.readUTF();

            String uniqueName = UUID.randomUUID().toString() + "_" + originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String userDir = SERVER_STORAGE_ROOT + "user_" + session.getCurrentUserId() + "/";
            fullFilePath = userDir + uniqueName;

            java.io.File directory = new java.io.File(userDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(fullFilePath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

                while (totalBytesRead < fileSize) {
                    int bytesToRead = (int) Math.min(buffer.length, fileSize - totalBytesRead);
                    bytesRead = dis.read(buffer, 0, bytesToRead);
                    if (bytesRead == -1) {
                        throw new IOException("Client closed stream unexpectedly.");
                    }
                    fos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
            }

            File fileMetadata = new File();
            fileMetadata.setOwnerId(session.getCurrentUserId());
            fileMetadata.setFileName(originalFileName);
            fileMetadata.setFilePath(fullFilePath);
            fileMetadata.setFileSize(fileSize);
            fileMetadata.setFileType(fileType);
            fileMetadata.setIsShared(false);

            FileDAO fileDAO = new FileDAO();
            int fileId = fileDAO.insertFileMetadata(fileMetadata);

            if (fileId != -1) {
                dos.writeUTF("UPLOAD_SUCCESS");
                dos.writeInt(fileId);
                System.out.println("User " + session.getCurrentUserId() + " đã upload thành công: " + originalFileName);
            } else {
                dos.writeUTF("UPLOAD_FAIL_DB");
                new java.io.File(fullFilePath).delete();
                System.err.println("Lỗi CSDL khi chèn metadata. Đã xóa file vật lý: " + fullFilePath);
            }
        } catch (IOException e) {
            dos.writeUTF("UPLOAD_FAIL_IO");
            System.err.println("Lỗi I/O khi xử lý upload: " + e.getMessage());
            if (fullFilePath != null) {
                new java.io.File(fullFilePath).delete();
            }
        }
    }
}
