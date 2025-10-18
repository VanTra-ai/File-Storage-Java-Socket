/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package filestorageclient;

import java.awt.Color;
import java.io.File;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.tree.TreePath;

public class frmMainClient extends javax.swing.JFrame {

    private final ClientSocketManager clientManager = ClientSocketManager.getInstance();
    private String username;
    private DefaultTableModel tableModel;
    private static final Logger logger = Logger.getLogger(frmMainClient.class.getName());
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private int currentFilePage = 1;
    private int totalFilePages = 1;
    private String currentSortBy = "name_asc"; // Mặc định sắp xếp theo tên A-Z
    private final int PAGE_SIZE = 10; // Số item mỗi trang, bạn có thể thay đổi

    public frmMainClient() {
        this.username = clientManager.getCurrentUsername();
        initComponents();
        initializeTable();
        lblWelcome.setText("Xin chào, " + this.username + "!");
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPopupMenu folderContextMenu = new JPopupMenu();
        JMenuItem renameItem = new JMenuItem("Rename");
        JMenuItem deleteItem = new JMenuItem("Delete");

        // Thêm hành động cho Rename
        renameItem.addActionListener(e -> handleRenameFolder());
        folderContextMenu.add(renameItem);

        // Thêm hành động cho Delete
        deleteItem.addActionListener(e -> handleDeleteFolder());
        folderContextMenu.add(deleteItem);
        loadInitialStorageInfo();

        // Thêm MouseListener để hiển thị menu khi chuột phải
        folderTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Lấy vị trí chuột và xác định node được click
                    TreePath path = folderTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        // Chọn node đó trên cây
                        folderTree.setSelectionPath(path);
                        // Lấy node
                        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                        // Không cho xóa/đổi tên node gốc
                        if (selectedNode != rootNode) {
                            // Hiển thị menu tại vị trí chuột
                            folderContextMenu.show(folderTree, e.getX(), e.getY());
                        }
                    }
                }
            }
        });
    }

    /**
     * Khởi tạo cấu trúc bảng và tải danh sách file ban đầu
     */
    private void initializeTable() {
        String[] columnNames = {"File ID", "File Name", "Size (bytes)", "Upload Date", "Status", "Sharer"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        fileTable.setModel(tableModel);

        fileTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        fileTable.getColumnModel().getColumn(0).setMinWidth(0);
        fileTable.getColumnModel().getColumn(0).setMaxWidth(0);

        fileTable.getColumnModel().getColumn(4).setPreferredWidth(60);
        fileTable.getColumnModel().getColumn(4).setMaxWidth(80);

        fileTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        fileTable.getColumnModel().getColumn(5).setMaxWidth(150);

        initializeFolderTree();
    }

    /**
     * Khởi tạo cây thư mục
     */
    private void initializeFolderTree() {
        // Tạo một node gốc ảo, hiển thị "My Storage"
        // Chúng ta dùng ID -1 để đại diện cho thư mục gốc (root) trên server
        FolderNode rootFolder = new FolderNode(-1, "My Storage");
        rootNode = new DefaultMutableTreeNode(rootFolder);

        treeModel = new DefaultTreeModel(rootNode);
        folderTree.setModel(treeModel);

        // Bắt đầu tải các thư mục con từ gốc
        loadSubFolders(rootNode);

        // Mở rộng node gốc
        folderTree.expandRow(0);
        // Thêm listener để xử lý khi người dùng chọn một node
        folderTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
                if (selectedNode == null || !(selectedNode.getUserObject() instanceof FolderNode)) {
                    tableModel.setRowCount(0);
                    // Cập nhật phân trang khi không có gì chọn
                    totalFilePages = 0;
                    currentFilePage = 1;
                    updatePaginationControls();
                    return;
                }

                FolderNode selectedFolder = (FolderNode) selectedNode.getUserObject();
                currentFilePage = 1;

                // Gọi hàm tải file (nó sẽ dùng currentFilePage = 1)
                loadFilesForFolder(selectedFolder.getId());
            }
        });

        folderTree.addTreeWillExpandListener(new TreeWillExpandListener() {
            @Override
            public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
                TreePath path = event.getPath();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

                if (node.getChildCount() == 1
                        && node.getFirstChild() instanceof DefaultMutableTreeNode
                        && ((DefaultMutableTreeNode) node.getFirstChild()).getUserObject().toString().equals("Loading...")) {
                    loadSubFolders(node);
                }
            }

            @Override
            public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
                // Không cần làm gì khi thu gọn
            }
        });

    }

    /**
     * Tải các thư mục con cho một node cha (ví dụ: "My Storage")
     *
     * @param parentNode Node cha trên cây (JTree)
     */
    private void loadSubFolders(DefaultMutableTreeNode parentNode) {
        // Lấy FolderNode từ node JTree
        FolderNode parentFolder = (FolderNode) parentNode.getUserObject();
        int parentFolderId = parentFolder.getId();

        // Tải danh sách thư mục con trong một luồng nền
        // Sửa kiểu trả về của SwingWorker thành PagedResponse
        new SwingWorker<ClientSocketManager.PagedResponse, Void>() {
            @Override
            protected ClientSocketManager.PagedResponse doInBackground() throws Exception {
                // Gọi phiên bản mới với tham số phân trang/sắp xếp mặc định
                // Trang 1, Kích thước rất lớn (để lấy hết), Sắp xếp theo tên
                return clientManager.getFolders(parentFolderId, 1, Integer.MAX_VALUE, "name_asc");
            }

            @Override
            protected void done() {
                try {
                    // Lấy PagedResponse từ doInBackground
                    ClientSocketManager.PagedResponse pagedResponse = get();

                    // Kiểm tra responseCode trong đối tượng PagedResponse
                    if ("FOLDERLIST_PAGED_START".equals(pagedResponse.responseCode)) {
                        String data = pagedResponse.rawData; // Lấy dữ liệu thô

                        parentNode.removeAllChildren();

                        if (data.isEmpty()) {
                            // Không có thư mục con, báo cho model biết cấu trúc thay đổi để xóa icon expand
                            treeModel.nodeStructureChanged(parentNode);
                            return;
                        }

                        // Phân tích dữ liệu và thêm node con (logic giữ nguyên)
                        String[] folders = data.split(";");
                        for (String folderInfo : folders) {
                            if (folderInfo.trim().isEmpty()) {
                                continue;
                            }

                            String[] parts = folderInfo.split("\\|");
                            if (parts.length == 2) { // Đảm bảo đúng định dạng id|name
                                try {
                                    int folderId = Integer.parseInt(parts[0]);
                                    String folderName = parts[1];

                                    FolderNode folder = new FolderNode(folderId, folderName);
                                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(folder);
                                    newNode.add(new DefaultMutableTreeNode("Loading...")); // Thêm placeholder
                                    parentNode.add(newNode);
                                } catch (NumberFormatException e) {
                                    logger.log(Level.WARNING, "Lỗi parse folder info: " + folderInfo, e);
                                }
                            } else {
                                logger.log(Level.WARNING, "Định dạng folder info không đúng: " + folderInfo);
                            }
                        }

                        // Cập nhật lại giao diện cây cho node cha
                        treeModel.nodeStructureChanged(parentNode); // Dùng nodeStructureChanged thay vì reload để giữ trạng thái expand/collapse của các node cháu (nếu có)

                    } else { // Xử lý lỗi (phản hồi không phải FOLDERLIST_PAGED_START)
                        logger.log(Level.WARNING, "Không thể tải thư mục: " + pagedResponse.responseCode);
                        parentNode.removeAllChildren(); // Xóa "Loading..." khi có lỗi
                        treeModel.nodeStructureChanged(parentNode); // Cập nhật cây
                        // Có thể hiển thị lỗi cho người dùng nếu muốn
                        JOptionPane.showMessageDialog(frmMainClient.this, "Lỗi tải thư mục: " + pagedResponse.responseCode, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }

                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Lỗi khi tải thư mục con", ex);
                    parentNode.removeAllChildren(); // Xóa "Loading..." khi có lỗi nghiêm trọng
                    treeModel.nodeStructureChanged(parentNode); // Cập nhật cây
                    // Hiển thị lỗi nghiêm trọng
                    JOptionPane.showMessageDialog(frmMainClient.this, "Lỗi nghiêm trọng khi tải thư mục: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /**
     * Tải danh sách file cho một thư mục cụ thể theo trang/sắp xếp và cập nhật
     * JTable.
     *
     * @param folderId ID của thư mục (-1 cho thư mục gốc)
     */
    private void loadFilesForFolder(int folderId) {
        tableModel.setRowCount(0); // Xóa dữ liệu cũ
        // Vô hiệu hóa nút khi đang tải
        btnPrevPage.setEnabled(false);
        btnNextPage.setEnabled(false);
        lblPageInfo.setText("Loading...");

        new SwingWorker<ClientSocketManager.PagedResponse, Void>() { // Sửa kiểu trả về
            @Override
            protected ClientSocketManager.PagedResponse doInBackground() throws Exception {
                // Gọi ClientSocketManager với tham số phân trang/sắp xếp
                return clientManager.getFilesInFolder(folderId, currentFilePage, PAGE_SIZE, currentSortBy);
            }

            @Override
            protected void done() {
                try {
                    ClientSocketManager.PagedResponse pagedResponse = get(); // Lấy PagedResponse

                    if ("FILELIST_PAGED_START".equals(pagedResponse.responseCode)) {
                        totalFilePages = pagedResponse.totalPages; // Cập nhật tổng số trang
                        currentFilePage = pagedResponse.currentPage; // Cập nhật trang hiện tại

                        String data = pagedResponse.rawData;
                        if (!data.isEmpty()) {
                            // Logic phân tích chuỗi và thêm vào tableModel giữ nguyên
                            String[] files = data.split(";");
                            for (String fileInfo : files) {
                                if (fileInfo.trim().isEmpty()) {
                                    continue;
                                }
                                String[] parts = fileInfo.split("\\|", -1);
                                if (parts.length == 6) {
                                    try {
                                        long fileSize = Long.parseLong(parts[2]);
                                        String formattedSize = formatSize(fileSize);
                                        String sharerName = parts[5].isEmpty() ? "" : parts[5];
                                        Object[] rowData = new Object[]{
                                            parts[0], parts[1], formattedSize, parts[3], parts[4], sharerName
                                        };
                                        tableModel.addRow(rowData);
                                    } catch (NumberFormatException e) {
                                        logger.log(Level.WARNING, "Lỗi parse kích thước file: " + parts[2], e);
                                    }
                                } else {
                                    logger.log(Level.WARNING, "Định dạng file không đúng: " + fileInfo);
                                }
                            }
                        }
                        // Cập nhật giao diện phân trang
                        updatePaginationControls();

                    } else { // Xử lý lỗi
                        JOptionPane.showMessageDialog(frmMainClient.this, "Không thể tải danh sách file: " + pagedResponse.responseCode, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        lblPageInfo.setText("Error");
                        totalFilePages = 0; // Reset
                        currentFilePage = 1;
                        updatePaginationControls(); // Vô hiệu hóa nút
                    }
                } catch (Exception ex) {
                    logger.log(Level.SEVERE, "Lỗi khi tải danh sách file", ex);
                    JOptionPane.showMessageDialog(frmMainClient.this, "Lỗi nghiêm trọng khi tải file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    lblPageInfo.setText("Error");
                    totalFilePages = 0;
                    currentFilePage = 1;
                    updatePaginationControls();
                }
            }
        }.execute();
    }

    /**
     * Cập nhật trạng thái của các nút phân trang và label hiển thị trang.
     */
    private void updatePaginationControls() {
        if (totalFilePages <= 0) { // Trường hợp không có dữ liệu hoặc lỗi
            lblPageInfo.setText("Page 0 / 0");
            btnPrevPage.setEnabled(false);
            btnNextPage.setEnabled(false);
        } else {
            lblPageInfo.setText("Page " + currentFilePage + " / " + totalFilePages);
            btnPrevPage.setEnabled(currentFilePage > 1); // Bật nếu không phải trang đầu
            btnNextPage.setEnabled(currentFilePage < totalFilePages); // Bật nếu không phải trang cuối
        }
        // Có thể thêm code để cập nhật cmbSortBy ở đây nếu cần
    }

    // LOGIC XỬ LÝ CHỨC NĂNG  
    /**
     * Xử lý Đăng xuất
     */
    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn đăng xuất?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    clientManager.logout();
                    return null;
                }

                @Override
                protected void done() {
                    new frmLogin().setVisible(true);
                    frmMainClient.this.dispose();
                }
            }.execute();
        }
    }

    /**
     * Xử lý Tải file lên (Upload)
     */
    private void handleUpload() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fileChooser.getSelectedFile();
            final ProgressDialog progressDialog = new ProgressDialog(this, "Uploading File...");
            final long startTime = System.currentTimeMillis();

            // Định nghĩa một lớp nội bộ cục bộ để xử lý tác vụ upload.
            // Lớp này vừa kế thừa SwingWorker, vừa implement ProgressPublisher.
            class UploadWorker extends SwingWorker<String, ClientSocketManager.ProgressData> implements ProgressPublisher {

                @Override
                public void publishProgress(ClientSocketManager.ProgressData data) {
                    // Phương thức public này làm cầu nối, gọi đến phương thức protected của SwingWorker.
                    publish(data);
                }

                @Override
                protected String doInBackground() throws Exception {
                    // Lấy folderId đang được chọn trên cây
                    Integer targetFolderId = getSelectedFolderId();

                    // Gọi phương thức uploadFile mới trong ClientSocketManager
                    // Nó sẽ tự xử lý việc gửi chunk
                    return clientManager.uploadFile(selectedFile, targetFolderId, this);
                }

                @Override
                protected void process(List<ClientSocketManager.ProgressData> chunks) {
                    // Logic cập nhật progress bar vẫn giữ nguyên
                    ClientSocketManager.ProgressData latestData = chunks.get(chunks.size() - 1);
                    progressDialog.updateProgress(latestData.getPercentage());

                    String transferredMB = String.format("%.2f", (double) latestData.getTotalBytesTransferred() / (1024 * 1024));
                    String totalMB = String.format("%.2f", (double) latestData.getTotalFileSize() / (1024 * 1024));
                    progressDialog.setStatusText(String.format("Uploading %s MB / %s MB...", transferredMB, totalMB));
                }

                @Override
                protected void done() {
                    long endTime = System.currentTimeMillis();
                    double durationInSeconds = (endTime - startTime) / 1000.0;

                    try {
                        String uploadResult = get();

                        if ("UPLOAD_COMPLETE_SUCCESS".equals(uploadResult)) {
                            progressDialog.setStatusText("Upload Complete!");
                            JOptionPane.showMessageDialog(frmMainClient.this,
                                    String.format("Upload thành công!\nThời gian: %.2f giây", durationInSeconds));

                            // Làm mới danh sách file và thông tin dung lượng
                            refreshCurrentFolderView();
                            loadInitialStorageInfo();

                        } else {
                            String errorMessage;
                            switch (uploadResult) {
                                case "UPLOAD_START_FAIL_FILE_TOO_LARGE":
                                    errorMessage = "File quá lớn! Kích thước tối đa cho phép là 200 MB.";
                                    break;
                                case "UPLOAD_START_FAIL_QUOTA_EXCEEDED":
                                    errorMessage = "Đã hết dung lượng lưu trữ tài khoản (Giới hạn 1 GB).";
                                    break;
                                case "UPLOAD_START_FAIL_INVALID_SIZE":
                                    errorMessage = "Không thể upload file rỗng hoặc có kích thước không hợp lệ.";
                                    break;
                                case "CHUNK_FAIL_OFFSET_MISMATCH":
                                    errorMessage = "Lỗi đồng bộ (offset mismatch). Vui lòng thử lại.";
                                    break;
                                default:
                                    errorMessage = "Upload thất bại: " + uploadResult;
                                    break;
                            }
                            JOptionPane.showMessageDialog(frmMainClient.this, errorMessage, "Lỗi Upload", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Lỗi khi Upload file", ex);
                        JOptionPane.showMessageDialog(frmMainClient.this, "Lỗi nghiêm trọng khi upload: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        progressDialog.closeDialog();
                    }
                } // Kết thúc done()
            }

            // Khởi tạo và thực thi worker
            UploadWorker worker = new UploadWorker();
            worker.execute();
            progressDialog.setVisible(true);
        }
    }

    /**
     * Xử lý Tải file về (Download)
     */
    private void handleDownload() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một file để tải xuống.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final int fileId = Integer.parseInt((String) tableModel.getValueAt(selectedRow, 0));
        String fileName = (String) tableModel.getValueAt(selectedRow, 1);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(fileName));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            final File fileToSave = fileChooser.getSelectedFile();
            final ProgressDialog progressDialog = new ProgressDialog(this, "Downloading File...");
            final long startTime = System.currentTimeMillis();

            class DownloadWorker extends SwingWorker<String, ClientSocketManager.ProgressData> implements ProgressPublisher {

                @Override
                public void publishProgress(ClientSocketManager.ProgressData data) {
                    publish(data);
                }

                @Override
                protected String doInBackground() throws Exception {
                    return clientManager.downloadFile(fileId, fileToSave, this);
                }

                @Override
                protected void process(List<ClientSocketManager.ProgressData> chunks) {
                    ClientSocketManager.ProgressData latestData = chunks.get(chunks.size() - 1);
                    progressDialog.updateProgress(latestData.getPercentage());

                    String transferredMB = String.format("%.2f", (double) latestData.getTotalBytesTransferred() / (1024 * 1024));
                    String totalMB = String.format("%.2f", (double) latestData.getTotalFileSize() / (1024 * 1024));
                    progressDialog.setStatusText(String.format("Downloading %s MB / %s MB...", transferredMB, totalMB));
                }

                @Override
                protected void done() {
                    long endTime = System.currentTimeMillis();
                    double durationInSeconds = (endTime - startTime) / 1000.0;

                    try {
                        String downloadResult = get();
                        if ("DOWNLOAD_SUCCESS".equals(downloadResult)) {
                            progressDialog.setStatusText("Download Complete!");
                            JOptionPane.showMessageDialog(frmMainClient.this,
                                    String.format("Tải file thành công!\nThời gian: %.2f giây", durationInSeconds));
                        } else {
                            String errorMessage;
                            switch (downloadResult) {
                                case "DOWNLOAD_FAIL_AUTH":
                                    errorMessage = "Tải file thất bại: Bạn không có quyền tải file này.";
                                    break;
                                case "DOWNLOAD_FAIL_NOT_FOUND":
                                    errorMessage = "Tải file thất bại: File không còn tồn tại trên server.";
                                    break;
                                case "DOWNLOAD_FAIL_INCOMPLETE":
                                    errorMessage = "Tải file thất bại: File tải về không hoàn chỉnh.";
                                    break;
                                default:
                                    errorMessage = "Tải file thất bại: " + downloadResult;
                                    break;
                            }
                            JOptionPane.showMessageDialog(frmMainClient.this, errorMessage, "Lỗi Download", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Lỗi khi Download file", ex);
                    } finally {
                        progressDialog.closeDialog();
                    }
                }
            }

            DownloadWorker worker = new DownloadWorker();
            worker.execute();
            progressDialog.setVisible(true);
        }
    }

    /**
     * Xử lý Xóa file (Delete)
     */
    private void handleDelete() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một file để xóa.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int fileId = Integer.parseInt((String) tableModel.getValueAt(selectedRow, 0));
        String fileName = (String) tableModel.getValueAt(selectedRow, 1);
        String status = (String) tableModel.getValueAt(selectedRow, 4);

        if (status.equals("Shared")) {
            JOptionPane.showMessageDialog(this, "Bạn không thể xóa file được chia sẻ cho bạn.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc chắn muốn xóa file '" + fileName + "'?", "Xác nhận Xóa", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            btnDelete.setEnabled(false);
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return clientManager.deleteFile(fileId);
                }

                @Override
                protected void done() {
                    try {
                        String deleteResult = get();
                        if ("DELETE_SUCCESS".equals(deleteResult)) {
                            JOptionPane.showMessageDialog(frmMainClient.this, "Đã xóa file '" + fileName + "' thành công!");

                            refreshCurrentFolderView();
                            loadInitialStorageInfo();
                        } else {
                            String errorMessage;
                            switch (deleteResult) {
                                case "DELETE_FAIL_NOT_FOUND_OR_AUTH":
                                    errorMessage = "Xóa file thất bại: Không tìm thấy file hoặc bạn không có quyền xóa.";
                                    break;
                                case "DELETE_FAIL_DB_ERROR":
                                    errorMessage = "Xóa file thất bại: Đã xảy ra lỗi CSDL.";
                                    break;
                                default:
                                    errorMessage = "Xóa file thất bại: " + deleteResult;
                                    break;
                            }
                            JOptionPane.showMessageDialog(frmMainClient.this, errorMessage, "Lỗi Xóa File", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Lỗi khi Xóa file", ex);
                        JOptionPane.showMessageDialog(frmMainClient.this, "Lỗi nghiêm trọng khi xóa file: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); // Thêm thông báo
                    } finally {
                        btnDelete.setEnabled(true);
                    }
                }
            }.execute();
        }
    }

    /**
     * Xử lý Chia sẻ file (Share)
     */
    private void handleShare() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một file để chia sẻ.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int fileId = Integer.parseInt((String) tableModel.getValueAt(selectedRow, 0));
        String fileName = (String) tableModel.getValueAt(selectedRow, 1);
        String status = (String) tableModel.getValueAt(selectedRow, 4);

        if (status.equals("Shared")) {
            JOptionPane.showMessageDialog(this, "Bạn chỉ có thể quản lý việc chia sẻ cho file bạn sở hữu ('Owned').", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        this.setEnabled(false);

        // THAY ĐỔI 3: KHÔNG CẦN TRUYỀN clientManager QUA CONSTRUCTOR NỮA
        frmShareFile shareForm = new frmShareFile(this, fileId, fileName);
        shareForm.setVisible(true);
    }

    /**
     * Lấy ID của thư mục đang được chọn trên JTree.
     *
     * @return ID thư mục, hoặc null nếu không có gì được chọn hoặc node gốc
     * được chọn.
     */
    private Integer getSelectedFolderId() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();

        if (selectedNode != null && selectedNode.getUserObject() instanceof FolderNode) {
            FolderNode folderNode = (FolderNode) selectedNode.getUserObject();
            // ID -1 đại diện cho gốc, nên ta trả về null
            return (folderNode.getId() == -1) ? null : folderNode.getId();
        }

        // Nếu không có gì được chọn, mặc định upload vào gốc
        return null;
    }

    /**
     * Chuyển đổi kích thước byte sang định dạng MB, KB, GB
     */
    /**
     * Xử lý logic khi người dùng nhấn nút Tạo Thư Mục Mới.
     */
    private void handleNewFolder() {
        // 1. Xác định thư mục cha đang được chọn
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
        FolderNode parentFolderNode;

        if (selectedNode != null && selectedNode.getUserObject() instanceof FolderNode) {
            parentFolderNode = (FolderNode) selectedNode.getUserObject();
        } else {
            // Nếu không có gì chọn, mặc định tạo ở thư mục gốc
            selectedNode = rootNode; // Dùng node gốc đã lưu
            parentFolderNode = (FolderNode) rootNode.getUserObject();
        }

        // Lấy ID thư mục cha (sẽ là -1 nếu là gốc)
        final int parentFolderId = parentFolderNode.getId();
        final DefaultMutableTreeNode finalSelectedNode = selectedNode; // Cần final để dùng trong SwingWorker

        // 2. Hỏi tên thư mục mới
        String newFolderName = JOptionPane.showInputDialog(this, "Nhập tên thư mục mới:", "Tạo Thư Mục", JOptionPane.PLAIN_MESSAGE);

        if (newFolderName != null && !newFolderName.trim().isEmpty()) {
            newFolderName = newFolderName.trim(); // Loại bỏ khoảng trắng thừa

            // 3. Gọi server để tạo thư mục (dùng SwingWorker)
            final String finalNewFolderName = newFolderName; // Cần final
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return clientManager.createFolder(finalNewFolderName, parentFolderId);
                }

                @Override
                protected void done() {
                    try {
                        String response = get();
                        if (response.startsWith("CREATE_FOLDER_SUCCESS:")) {
                            JOptionPane.showMessageDialog(frmMainClient.this, "Đã tạo thư mục '" + finalNewFolderName + "'!", "Thành công", JOptionPane.INFORMATION_MESSAGE);

                            int newFolderId = -1;
                            try {
                                // Lấy ID mới từ chuỗi phản hồi
                                newFolderId = Integer.parseInt(response.substring("CREATE_FOLDER_SUCCESS:".length()));
                            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                                System.err.println("Không thể phân tích ID thư mục mới từ phản hồi: " + response);
                            }

                            if (newFolderId != -1) {
                                // 1. Tạo node mới trên client
                                FolderNode newFolder = new FolderNode(newFolderId, finalNewFolderName);
                                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(newFolder);
                                newNode.add(new DefaultMutableTreeNode("Loading...")); // Thêm placeholder

                                // 2. Xóa "Loading..." khỏi node cha (nếu có)
                                if (finalSelectedNode.getChildCount() == 1
                                        && finalSelectedNode.getFirstChild() instanceof DefaultMutableTreeNode
                                        && ((DefaultMutableTreeNode) finalSelectedNode.getFirstChild()).getUserObject().toString().equals("Loading...")) {
                                    // Chỉ xóa khi nó thực sự là node "Loading..." duy nhất
                                    treeModel.removeNodeFromParent((MutableTreeNode) finalSelectedNode.getFirstChild());
                                }

                                // 3. Thêm node mới vào model -> Tự động cập nhật JTree
                                treeModel.insertNodeInto(newNode, finalSelectedNode, finalSelectedNode.getChildCount());

                                // 4. Đảm bảo node cha được mở rộng để thấy node mới
                                folderTree.expandPath(new TreePath(finalSelectedNode.getPath()));

                            } else {
                                // Fallback: Nếu không lấy được ID, tải lại toàn bộ node cha như cũ
                                System.err.println("Fallback: Tải lại node cha vì không lấy được ID thư mục mới.");
                                // (Code fallback giống như trước)
                                if (finalSelectedNode.getChildCount() == 1
                                        && finalSelectedNode.getFirstChild() instanceof DefaultMutableTreeNode
                                        && ((DefaultMutableTreeNode) finalSelectedNode.getFirstChild()).getUserObject().toString().equals("Loading...")) {
                                    finalSelectedNode.removeAllChildren();
                                } else if (finalSelectedNode.getChildCount() > 0 && parentFolderId != -1) {
                                    finalSelectedNode.removeAllChildren();
                                }
                                if (!finalSelectedNode.isLeaf() || finalSelectedNode == rootNode) {
                                    finalSelectedNode.add(new DefaultMutableTreeNode("Loading..."));
                                }
                                loadSubFolders(finalSelectedNode);
                                folderTree.expandPath(new TreePath(finalSelectedNode.getPath()));
                            }

                        } else {
                            String errorMessage;
                            switch (response) {
                                case "CREATE_FOLDER_FAIL_NAME_EXIST":
                                    errorMessage = "Tạo thư mục thất bại: Tên này đã tồn tại.";
                                    break;
                                case "CREATE_FOLDER_FAIL_EMPTY_NAME":
                                    errorMessage = "Tạo thư mục thất bại: Tên không được để trống.";
                                    break;
                                default:
                                    errorMessage = "Tạo thư mục thất bại: " + response;
                                    break;
                            }
                            JOptionPane.showMessageDialog(frmMainClient.this, errorMessage, "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Lỗi khi tạo thư mục", ex);
                        JOptionPane.showMessageDialog(frmMainClient.this, "Lỗi không xác định khi tạo thư mục: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    /**
     * Xử lý hành động đổi tên thư mục từ context menu.
     */
    private void handleRenameFolder() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
        if (selectedNode == null || selectedNode == rootNode || !(selectedNode.getUserObject() instanceof FolderNode)) {
            return; // Không phải node thư mục hợp lệ hoặc là node gốc
        }

        FolderNode currentFolder = (FolderNode) selectedNode.getUserObject();
        int folderId = currentFolder.getId();
        String currentName = currentFolder.getName();

        // Hỏi tên mới
        String newName = JOptionPane.showInputDialog(this, "Nhập tên mới cho thư mục:", currentName);

        if (newName != null && !newName.trim().isEmpty() && !newName.trim().equals(currentName)) {
            newName = newName.trim();
            final String finalNewName = newName; // Cần final

            // Gọi server (dùng SwingWorker)
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return clientManager.renameFolder(folderId, finalNewName);
                }

                @Override
                protected void done() {
                    try {
                        String response = get();
                        if ("RENAME_FOLDER_SUCCESS".equals(response)) {
                            // 1. Tạo FolderNode MỚI với tên mới
                            FolderNode updatedFolderNode = new FolderNode(folderId, finalNewName);

                            // 2. Cập nhật trực tiếp User Object của node đang chọn
                            selectedNode.setUserObject(updatedFolderNode);

                            // 3. Báo cho tree model biết node đã thay đổi (để vẽ lại tên mới)
                            treeModel.nodeChanged(selectedNode);

                            JOptionPane.showMessageDialog(frmMainClient.this, "Đổi tên thành công!");
                        } else {
                            String errorMessage;
                            switch (response) {
                                case "RENAME_FOLDER_FAIL_NOT_FOUND_OR_AUTH":
                                    errorMessage = "Đổi tên thất bại: Không tìm thấy thư mục hoặc bạn không có quyền.";
                                    break;
                                case "RENAME_FOLDER_FAIL_DB_OR_NAME_EXIST":
                                    errorMessage = "Đổi tên thất bại: Tên này đã tồn tại hoặc đã xảy ra lỗi CSDL.";
                                    break;
                                case "RENAME_FOLDER_FAIL_EMPTY_NAME":
                                    errorMessage = "Đổi tên thất bại: Tên không được để trống.";
                                    break;
                                default:
                                    errorMessage = "Đổi tên thất bại: " + response;
                                    break;
                            }
                            JOptionPane.showMessageDialog(frmMainClient.this, errorMessage, "Lỗi Đổi Tên", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Lỗi khi đổi tên thư mục", ex);
                    }
                }
            }.execute();
        }
    }

    /**
     * Xử lý hành động xóa thư mục từ context menu.
     */
    private void handleDeleteFolder() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
        if (selectedNode == null || selectedNode == rootNode || !(selectedNode.getUserObject() instanceof FolderNode)) {
            return; // Không phải node thư mục hợp lệ hoặc là node gốc
        }

        FolderNode folderToDelete = (FolderNode) selectedNode.getUserObject();
        int folderId = folderToDelete.getId();
        String folderName = folderToDelete.getName();

        // Xác nhận xóa
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn xóa thư mục '" + folderName + "'?\n(Tất cả thư mục và file con bên trong cũng sẽ bị xóa!)",
                "Xác nhận Xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            // Gọi server (dùng SwingWorker)
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() throws Exception {
                    return clientManager.deleteFolder(folderId);
                }

                @Override
                protected void done() {
                    try {
                        String response = get();
                        if ("DELETE_FOLDER_SUCCESS".equals(response)) {
                            // 1. Lấy node cha TRƯỚC KHI xóa node con
                            DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) selectedNode.getParent();

                            // 2. Xóa node con khỏi model
                            treeModel.removeNodeFromParent(selectedNode);

                            // 3. Cập nhật trạng thái node cha (QUAN TRỌNG)
                            if (parentNode != null) {
                                // Nếu node cha không còn con nào sau khi xóa
                                if (parentNode.getChildCount() == 0) {
                                    // Báo cho model biết cấu trúc đã thay đổi
                                    treeModel.nodeStructureChanged(parentNode);
                                }
                                // Tùy chọn: Nếu muốn load lại cha ngay lập tức (an toàn hơn)
                                /* else { 
                                     parentNode.removeAllChildren();
                                     parentNode.add(new DefaultMutableTreeNode("Loading..."));
                                     treeModel.reload(parentNode); // Hoặc loadSubFolders(parentNode);
                                 } */
                            }

                            JOptionPane.showMessageDialog(frmMainClient.this, "Đã xóa thư mục '" + folderName + "'!");

                            // Nếu node bị xóa đang được chọn, chọn lại node gốc
                            if (folderTree.getLastSelectedPathComponent() == null && rootNode != null) {
                                TreePath rootPath = new TreePath(rootNode.getPath());
                                folderTree.setSelectionPath(rootPath);
                                // Không cần loadFilesForFolder(-1) vì setSelectionPath sẽ trigger valueChanged listener
                            }

                        } else {
                            String errorMessage;
                            switch (response) {
                                case "DELETE_FOLDER_FAIL_ROOT":
                                    errorMessage = "Xóa thư mục thất bại: Không thể xóa thư mục gốc.";
                                    break;
                                case "DELETE_FOLDER_FAIL_NOT_FOUND_OR_AUTH":
                                    errorMessage = "Xóa thư mục thất bại: Không tìm thấy thư mục hoặc bạn không có quyền.";
                                    break;
                                case "DELETE_FOLDER_FAIL_DB_ERROR":
                                    errorMessage = "Xóa thư mục thất bại: Lỗi CSDL (Thư mục có thể cần rỗng, hoặc file/thư mục con đang bị khóa).";
                                    break;
                                default:
                                    errorMessage = "Xóa thư mục thất bại: " + response;
                                    break;
                            }
                            JOptionPane.showMessageDialog(frmMainClient.this, errorMessage, "Lỗi Xóa Thư Mục", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Lỗi khi xóa thư mục", ex);
                        JOptionPane.showMessageDialog(frmMainClient.this, "Lỗi nghiêm trọng khi xóa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); // Thêm thông báo lỗi
                    }
                }
            }.execute();
        }
    }

    /**
     * Làm mới (tải lại) danh sách file cho thư mục đang được chọn trên cây.
     */
    public void refreshCurrentFolderView() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) folderTree.getLastSelectedPathComponent();
        int folderIdToRefresh = -1;

        if (selectedNode != null && selectedNode.getUserObject() instanceof FolderNode) {
            FolderNode selectedFolder = (FolderNode) selectedNode.getUserObject();
            folderIdToRefresh = selectedFolder.getId();
        } else {
            if (rootNode != null) {
                folderTree.setSelectionPath(new TreePath(rootNode.getPath()));
                folderIdToRefresh = -1; // Đảm bảo là gốc
            }
        }

        loadFilesForFolder(folderIdToRefresh);
    }

    /**
     * Tải thông tin dung lượng ban đầu khi mở form.
     */
    private void loadInitialStorageInfo() {
        new SwingWorker<long[], Void>() { // Kiểu trả về là long[]
            @Override
            protected long[] doInBackground() throws Exception {
                // Gọi trực tiếp hàm mới trả về long[] hoặc null
                long[] storageData = clientManager.getStorageInfo();
                return storageData; // Trả về mảng hoặc null
            }

            @Override
            protected void done() {
                try {
                    long[] storageInfo = get(); // Lấy mảng long[] hoặc null
                    if (storageInfo != null && storageInfo.length == 2) {
                        // Nếu thành công, cập nhật hiển thị
                        updateStorageDisplay(storageInfo[0], storageInfo[1]);
                    } else {
                        // Ghi log lỗi (đã được ghi chi tiết hơn ở ClientSocketManager)
                        System.err.println("frmMainClient: Không thể lấy thông tin dung lượng từ server.");
                        updateStorageDisplay(-1, -1); // Hiển thị N/A khi lỗi
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Lỗi nghiêm trọng khi xử lý kết quả storage info", e); // Sửa log lỗi
                    updateStorageDisplay(-1, -1);
                }
            }
        }.execute();
    }

    /**
     * Cập nhật JLabel hiển thị thông tin dung lượng.
     *
     * @param usedBytes Dung lượng đã dùng.
     * @param totalBytes Dung lượng tổng.
     */
    private void updateStorageDisplay(long usedBytes, long totalBytes) {
        if (usedBytes < 0 || totalBytes <= 0) {
            lblStorageInfo.setText("Storage: N/A");
            return;
        }
        // Chuyển đổi sang MB hoặc GB để dễ đọc
        String usedStr;
        String totalStr;
        double usedMB = (double) usedBytes / (1024 * 1024);
        double totalMB = (double) totalBytes / (1024 * 1024);

        if (totalMB >= 1024) { // Hiển thị GB nếu > 1GB
            usedStr = String.format("%.2f GB", usedMB / 1024);
            totalStr = String.format("%.2f GB", totalMB / 1024);
        } else { // Hiển thị MB
            usedStr = String.format("%.1f MB", usedMB);
            totalStr = String.format("%.1f MB", totalMB);
        }

        // Tính phần trăm
        int percentage = (int) Math.round(((double) usedBytes * 100) / totalBytes);

        lblStorageInfo.setText(String.format("Storage: %s / %s (%d%%)", usedStr, totalStr, percentage));

        // (Tùy chọn) Đổi màu nếu gần đầy
        if (percentage >= 90) {
            lblStorageInfo.setForeground(Color.RED);
        } else {
            lblStorageInfo.setForeground(Color.BLACK); // Hoặc màu mặc định
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " Bytes";
        }
        int unit = 1024;
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.2f %s", bytes / Math.pow(unit, exp), pre);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblWelcome = new javax.swing.JLabel();
        btnLogout = new javax.swing.JButton();
        btnUpload = new javax.swing.JButton();
        btnDownload = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnShare = new javax.swing.JButton();
        jSplitPane2 = new javax.swing.JSplitPane();
        folderTreeScrollPane = new javax.swing.JScrollPane();
        folderTree = new javax.swing.JTree();
        jScrollPane1 = new javax.swing.JScrollPane();
        fileTable = new javax.swing.JTable();
        btnNewFolder = new javax.swing.JButton();
        btnPrevPage = new javax.swing.JButton();
        lblPageInfo = new javax.swing.JLabel();
        btnNextPage = new javax.swing.JButton();
        cmbSortBy = new javax.swing.JComboBox<>();
        lblStorageInfo = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Quản lý File - File Storage Client");

        lblWelcome.setText("Xin chào, [Username]!");

        btnLogout.setText("Logout");
        btnLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogoutActionPerformed(evt);
            }
        });

        btnUpload.setText("Upload");
        btnUpload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUploadActionPerformed(evt);
            }
        });

        btnDownload.setText("Download");
        btnDownload.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDownloadActionPerformed(evt);
            }
        });

        btnDelete.setText("Delete");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        btnShare.setText("Share");
        btnShare.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnShareActionPerformed(evt);
            }
        });

        jSplitPane2.setDividerLocation(200);

        folderTreeScrollPane.setViewportView(folderTree);

        jSplitPane2.setLeftComponent(folderTreeScrollPane);

        fileTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "File name", "Size", "Date modified", "Status", "Sharer"
            }
        ));
        jScrollPane1.setViewportView(fileTable);

        jSplitPane2.setRightComponent(jScrollPane1);

        btnNewFolder.setText("New Folder");
        btnNewFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNewFolderActionPerformed(evt);
            }
        });

        btnPrevPage.setText("Previous");
        btnPrevPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrevPageActionPerformed(evt);
            }
        });

        lblPageInfo.setText("Page 1 / ?");

        btnNextPage.setText("Next");
        btnNextPage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextPageActionPerformed(evt);
            }
        });

        cmbSortBy.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Name (A-Z)", "Date (Newest)", "Size (Largest)" }));
        cmbSortBy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbSortByActionPerformed(evt);
            }
        });

        lblStorageInfo.setText("Storage: Loading...");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(43, 43, 43)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 536, Short.MAX_VALUE)
                                .addComponent(btnPrevPage)
                                .addGap(29, 29, 29)
                                .addComponent(lblPageInfo, javax.swing.GroupLayout.PREFERRED_SIZE, 77, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnNextPage))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addComponent(lblStorageInfo)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnNewFolder)
                                .addGap(37, 37, 37)
                                .addComponent(btnUpload)
                                .addGap(18, 18, 18)
                                .addComponent(btnDownload)
                                .addGap(18, 18, 18)
                                .addComponent(btnShare)
                                .addGap(45, 45, 45)
                                .addComponent(btnDelete)))
                        .addGap(61, 61, 61))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jSplitPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 822, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(cmbSortBy, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(82, 82, 82)
                                .addComponent(lblWelcome)
                                .addGap(39, 39, 39)
                                .addComponent(btnLogout)))
                        .addGap(34, 34, 34))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(8, 8, 8)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblWelcome)
                    .addComponent(btnLogout)
                    .addComponent(cmbSortBy, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(20, 20, 20)
                .addComponent(jSplitPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 366, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnUpload)
                    .addComponent(btnDownload)
                    .addComponent(btnDelete)
                    .addComponent(btnShare)
                    .addComponent(btnNewFolder)
                    .addComponent(lblStorageInfo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnPrevPage)
                    .addComponent(lblPageInfo)
                    .addComponent(btnNextPage))
                .addContainerGap(31, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogoutActionPerformed
        handleLogout();
    }//GEN-LAST:event_btnLogoutActionPerformed

    private void btnUploadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUploadActionPerformed
        handleUpload();
    }//GEN-LAST:event_btnUploadActionPerformed

    private void btnDownloadActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDownloadActionPerformed
        handleDownload();
    }//GEN-LAST:event_btnDownloadActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        handleDelete();
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnShareActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnShareActionPerformed
        handleShare();
    }//GEN-LAST:event_btnShareActionPerformed

    private void btnNewFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNewFolderActionPerformed
        handleNewFolder();
    }//GEN-LAST:event_btnNewFolderActionPerformed

    private void btnPrevPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrevPageActionPerformed
        if (currentFilePage > 1) {
            currentFilePage--; // Giảm trang hiện tại
            refreshCurrentFolderView(); // Tải lại dữ liệu trang mới
        }
    }//GEN-LAST:event_btnPrevPageActionPerformed

    private void btnNextPageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNextPageActionPerformed
        if (currentFilePage < totalFilePages) {
            currentFilePage++; // Tăng trang hiện tại
            refreshCurrentFolderView(); // Tải lại dữ liệu trang mới
        }
    }//GEN-LAST:event_btnNextPageActionPerformed

    private void cmbSortByActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbSortByActionPerformed
        String selectedSort = (String) cmbSortBy.getSelectedItem();
        String newSortBy = "name_asc"; // Mặc định
        if ("Date (Newest)".equals(selectedSort)) {
            newSortBy = "date_desc";
        } else if ("Size (Largest)".equals(selectedSort)) {
            newSortBy = "size_desc";
        } // Thêm các trường hợp khác nếu cần (vd: Size (Smallest) -> "size_asc")

        if (!newSortBy.equals(currentSortBy)) { // Chỉ tải lại nếu tiêu chí thay đổi
            currentSortBy = newSortBy;
            currentFilePage = 1; // Quay về trang đầu khi đổi sắp xếp
            refreshCurrentFolderView();
        }
    }//GEN-LAST:event_cmbSortByActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnDownload;
    private javax.swing.JButton btnLogout;
    private javax.swing.JButton btnNewFolder;
    private javax.swing.JButton btnNextPage;
    private javax.swing.JButton btnPrevPage;
    private javax.swing.JButton btnShare;
    private javax.swing.JButton btnUpload;
    private javax.swing.JComboBox<String> cmbSortBy;
    private javax.swing.JTable fileTable;
    private javax.swing.JTree folderTree;
    private javax.swing.JScrollPane folderTreeScrollPane;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JLabel lblPageInfo;
    private javax.swing.JLabel lblStorageInfo;
    private javax.swing.JLabel lblWelcome;
    // End of variables declaration//GEN-END:variables
}
