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
import java.io.FileInputStream;
import java.io.IOException;

public class DownloadCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        int fileId = dis.readInt();
        FileDAO fileDAO = new FileDAO();
        File fileMetadata = fileDAO.getFileForDownload(fileId, session.getCurrentUserId());

        if (fileMetadata == null) {
            dos.writeUTF("DOWNLOAD_FAIL_AUTH");
            return;
        }

        java.io.File fileToDownload = new java.io.File(fileMetadata.getFilePath());

        if (!fileToDownload.exists() || !fileToDownload.isFile()) {
            dos.writeUTF("DOWNLOAD_FAIL_NOT_FOUND");
            return;
        }

        try {
            dos.writeUTF("DOWNLOAD_START");
            dos.writeUTF(fileMetadata.getFileName());
            dos.writeLong(fileMetadata.getFileSize());
            dos.flush();

            try (FileInputStream fis = new FileInputStream(fileToDownload)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }
            dos.flush();
            System.out.println("User " + session.getCurrentUserId() + " đã download thành công: " + fileMetadata.getFileName());
        } catch (IOException e) {
            System.err.println("Lỗi I/O khi xử lý download cho user " + session.getCurrentUserId() + ": " + e.getMessage());
        }
    }
}