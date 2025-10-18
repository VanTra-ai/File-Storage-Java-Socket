package filestorageserver.commands;

import filestorageserver.ClientSession;
import filestorageserver.FileDAO;
import filestorageserver.ServerActivityListener;
import filestorageserver.model.File;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import filestorageserver.model.PagedResult;

/**
 * Xử lý lệnh CMD_GET_FILES_IN_FOLDER để lấy danh sách file trong một thư mục.
 */
public class GetFilesInFolderCommandHandler implements CommandHandler {

    @Override
    public void handle(ClientSession session, DataInputStream dis, DataOutputStream dos, ServerActivityListener listener) throws IOException {
        if (!session.isLoggedIn()) {
            dos.writeUTF("ERROR_NOT_LOGGED_IN");
            dos.flush();
            return;
        }

        try {
            int folderIdInt = dis.readInt();          // ID thư mục (-1 = gốc)
            int pageNumber = dis.readInt();          // Trang muốn lấy
            int pageSize = dis.readInt();            // Số item/trang
            String sortBy = dis.readUTF();             // Tiêu chí sắp xếp

            Integer folderId = (folderIdInt == -1) ? null : folderIdInt;

            // Đảm bảo giá trị hợp lệ
            if (pageNumber < 1) {
                pageNumber = 1;
            }
            if (pageSize <= 0) {
                pageSize = 20;
            }

            FileDAO fileDAO = new FileDAO();
            PagedResult<File> finalResult; // Kết quả cuối cùng

            // Xử lý logic khác nhau cho thư mục gốc (có file shared) và thư mục con
            if (folderId == null) {

                // Lấy TẤT CẢ file sở hữu ở gốc (không phân trang ở DAO)
                PagedResult<File> ownedFilesResult = fileDAO.getFilesByFolder(session.getCurrentUserId(), null, 1, Integer.MAX_VALUE, sortBy);
                // Lấy TẤT CẢ file được chia sẻ (không phân trang ở DAO)
                PagedResult<File> sharedFilesResult = fileDAO.getSharedFilesForUser(session.getCurrentUserId(), 1, Integer.MAX_VALUE, sortBy);

                List<File> allFiles = new ArrayList<>();
                if (ownedFilesResult != null && ownedFilesResult.getItems() != null) {
                    allFiles.addAll(ownedFilesResult.getItems());
                } else { // Lỗi nghiêm trọng nếu không lấy được file sở hữu
                    dos.writeUTF("FILELIST_FAIL_SERVER_ERROR");
                    System.err.println("Lỗi: getFilesByFolder (paged/root) trả về null cho User: " + session.getCurrentUserId());
                    dos.flush();
                    return;
                }
                if (sharedFilesResult != null && sharedFilesResult.getItems() != null) {
                    allFiles.addAll(sharedFilesResult.getItems());
                } else {
                    System.err.println("Cảnh báo: getSharedFilesForUser (paged) trả về null cho User: " + session.getCurrentUserId());
                }

                // *** Tự sắp xếp lại danh sách tổng hợp trên Java ***
                sortFileList(allFiles, sortBy);

                // *** Tự phân trang danh sách tổng hợp trên Java ***
                long totalItems = allFiles.size();
                int totalPages = (pageSize > 0) ? (int) Math.ceil((double) totalItems / pageSize) : 1;
                if (totalPages == 0 && totalItems > 0) {
                    totalPages = 1;
                } else if (totalItems == 0) {
                    totalPages = 0;
                }

                // Đảm bảo pageNumber hợp lệ
                if (pageNumber > totalPages && totalPages > 0) {
                    pageNumber = totalPages;
                }

                int startIndex = (pageNumber - 1) * pageSize;
                int endIndex = Math.min(startIndex + pageSize, allFiles.size());
                List<File> pageItems = (startIndex < endIndex) ? allFiles.subList(startIndex, endIndex) : new ArrayList<>();

                finalResult = new PagedResult<>(pageItems, totalItems, pageNumber, pageSize);

            } else {
                // Đang ở thư mục con: Chỉ cần gọi DAO lấy file sở hữu
                finalResult = fileDAO.getFilesByFolder(session.getCurrentUserId(), folderId, pageNumber, pageSize, sortBy);
            }

            // Xử lý kết quả cuối cùng
            if (finalResult == null) {
                dos.writeUTF("FILELIST_FAIL_SERVER_ERROR");
                System.err.println("Lỗi: finalResult là null khi lấy file cho User: " + session.getCurrentUserId() + ", Folder: " + folderId);
                dos.flush();
                return;
            }

            StringBuilder sb = new StringBuilder("FILELIST_PAGED_START:");
            sb.append(finalResult.getTotalItems()).append("|")
                    .append(finalResult.getTotalPages()).append("|")
                    .append(finalResult.getCurrentPage()).append("|"); // Thêm thông tin phân trang

            for (File file : finalResult.getItems()) {
                String dateString = "N/A";
                if (file.getUploadedAt() != null) {
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
                    dateString = dateFormat.format(file.getUploadedAt());
                }
                String status = file.isIsSharedToMe() ? "Shared" : "Owned";
                String sharer = file.getSharerName() != null ? file.getSharerName() : "";

                sb.append(file.getFileId()).append("|")
                        .append(file.getFileName()).append("|")
                        .append(file.getFileSize()).append("|")
                        .append(dateString).append("|")
                        .append(status).append("|")
                        .append(sharer).append(";");
            }

            dos.writeUTF(sb.toString());

        } catch (IOException streamEx) {
            System.err.println("Lỗi đọc stream get files (paged) từ client: " + streamEx.getMessage());
            throw streamEx;
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách file (paged) cho User " + session.getCurrentUserId() + ": " + e.getMessage());
            e.printStackTrace();
            try {
                dos.writeUTF("FILELIST_FAIL_INTERNAL_ERROR");
            } catch (IOException ignored) {
            }
        } finally {
            try {
                dos.flush();
            } catch (IOException ignored) {
            }
        }
    }

    // Hàm helper mới để sắp xếp danh sách file trên Java (cần cho thư mục gốc)
    private void sortFileList(List<File> files, String sortBy) {
        files.sort((f1, f2) -> {
            int comparison = 0;
            if ("size_asc".equalsIgnoreCase(sortBy)) {
                comparison = Long.compare(f1.getFileSize(), f2.getFileSize());
            } else if ("size_desc".equalsIgnoreCase(sortBy)) {
                comparison = Long.compare(f2.getFileSize(), f1.getFileSize());
            } else if ("date_asc".equalsIgnoreCase(sortBy)) {
                comparison = f1.getUploadedAt().compareTo(f2.getUploadedAt());
            } else if ("date_desc".equalsIgnoreCase(sortBy)) {
                comparison = f2.getUploadedAt().compareTo(f1.getUploadedAt());
            } else if ("sharer_asc".equalsIgnoreCase(sortBy)) {
                comparison = f1.getSharerName().compareToIgnoreCase(f2.getSharerName());
            } else if ("sharer_desc".equalsIgnoreCase(sortBy)) {
                comparison = f2.getSharerName().compareToIgnoreCase(f1.getSharerName());
            } else { // Mặc định name_asc
                comparison = f1.getFileName().compareToIgnoreCase(f2.getFileName());
            }
            // Nếu sắp xếp chính bằng nhau, sắp xếp phụ theo tên
            if (comparison == 0) {
                return f1.getFileName().compareToIgnoreCase(f2.getFileName());
            }
            return comparison;
        });
    }
}
