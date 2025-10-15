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
import java.io.IOException;

public class DeleteCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            return;
        }

        try {
            int fileId = dis.readInt();
            FileDAO fileDAO = new FileDAO();
            File fileMetadata = fileDAO.getFileForDownload(fileId, session.getCurrentUserId());

            if (fileMetadata == null) {
                dos.writeUTF("DELETE_FAIL_NOT_FOUND");
                return;
            }

            if (fileMetadata.getOwnerId() != session.getCurrentUserId()) {
                dos.writeUTF("DELETE_FAIL_AUTH");
                return;
            }

            java.io.File fileToDelete = new java.io.File(fileMetadata.getFilePath());
            boolean physicalDeleted = true;

            if (fileToDelete.exists()) {
                physicalDeleted = fileToDelete.delete();
            }

            if (physicalDeleted) {
                boolean metadataDeleted = fileDAO.deleteFileMetadata(fileId, session.getCurrentUserId());
                if (metadataDeleted) {
                    dos.writeUTF("DELETE_SUCCESS");
                    System.out.println("User " + session.getCurrentUserId() + " đã xóa thành công File ID: " + fileId);
                } else {
                    dos.writeUTF("DELETE_FAIL_DB_PHYSICAL_GONE");
                    System.err.println("LỖI KHÔNG ĐỒNG BỘ: Xóa file vật lý thành công nhưng xóa CSDL thất bại cho File ID: " + fileId);
                }
            } else {
                dos.writeUTF("DELETE_FAIL_PHYSICAL");
                System.err.println("Lỗi xóa file vật lý cho File ID: " + fileId);
            }
        } catch (Exception e) {
            dos.writeUTF("DELETE_FAIL_INTERNAL");
            System.err.println("Lỗi nội bộ khi xóa file: " + e.getMessage());
        }
    }
}