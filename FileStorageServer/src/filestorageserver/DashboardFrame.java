package filestorageserver;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Giao diện đồ họa (Dashboard) để giám sát hoạt động của server. Lớp này
 * implement ServerActivityListener để nhận và hiển thị thông tin trực tiếp.
 */
public class DashboardFrame extends javax.swing.JFrame implements ServerActivityListener {

    private final DefaultTableModel sessionTableModel;
    private final Map<ClientHandler, Integer> clientHandlerRowMap = new ConcurrentHashMap<>();

    public DashboardFrame() {
        initComponents(); // Phương thức này sẽ được cập nhật ở dưới
        this.setLocationRelativeTo(null);
        this.setTitle("Server Dashboard - File Storage");

        this.sessionTableModel = (DefaultTableModel) tblActiveSessions.getModel();
        txtLogArea.setEditable(false);
        addLogMessage("Dashboard initialized. Waiting for server to start...");
    }

    //<editor-fold defaultstate="collapsed" desc="ServerActivityListener Implementation">
    @Override
    public void onClientConnected(ClientHandler handler) {
        SwingUtilities.invokeLater(() -> {
            String ip = handler.getClientSocket().getInetAddress().getHostAddress();
            sessionTableModel.addRow(new Object[]{ip, "[Not Logged In]", "Connected"});
            int newRowIndex = sessionTableModel.getRowCount() - 1;
            clientHandlerRowMap.put(handler, newRowIndex);
            addLogMessage("Client connected: " + ip);
        });
    }

    @Override
    public void onClientDisconnected(ClientHandler handler) {
        SwingUtilities.invokeLater(() -> {
            Integer rowIndex = clientHandlerRowMap.remove(handler);
            if (rowIndex != null && rowIndex < sessionTableModel.getRowCount()) {
                sessionTableModel.removeRow(rowIndex);
                updateRowIndexesAfterRemoval(rowIndex);
            }
            addLogMessage("Client disconnected: " + handler.getClientSocket().getInetAddress().getHostAddress());
        });
    }

    @Override
    public void onUserLoggedIn(ClientHandler handler, String username) {
        SwingUtilities.invokeLater(() -> {
            Integer rowIndex = clientHandlerRowMap.get(handler);
            if (rowIndex != null && rowIndex < sessionTableModel.getRowCount()) {
                sessionTableModel.setValueAt(username, rowIndex, 1);
                sessionTableModel.setValueAt("Authenticated", rowIndex, 2);
            }
            addLogMessage("User '" + username + "' logged in from " + handler.getClientSocket().getInetAddress().getHostAddress());
        });
    }

    @Override
    public void onFileUploaded(String username, String fileName) {
        addLogMessage(String.format("User '%s' uploaded file: %s", username, fileName));
    }

    @Override
    public void onFileDownloaded(String username, String fileName) {
        addLogMessage(String.format("User '%s' downloaded file: %s", username, fileName));
    }

    @Override
    public void onUserRegistered(String username) {
        addLogMessage(String.format("New account registered: '%s'", username));
    }

    @Override
    public void onUserLoggedOut(String username) {
        addLogMessage(String.format("User '%s' has logged out.", username));
    }

    @Override
    public void onFileDeleted(String username, String fileName) {
        addLogMessage(String.format("User '%s' deleted file: %s", username, fileName));
    }

    @Override
    public void onFileShared(String sharer, String receiver, String fileName) {
        addLogMessage(String.format("User '%s' shared '%s' with '%s'", sharer, fileName, receiver));
    }

    @Override
    public void onUserActivityChanged(ClientHandler handler, String newStatus) {
        SwingUtilities.invokeLater(() -> {
            Integer rowIndex = clientHandlerRowMap.get(handler);
            if (rowIndex != null && rowIndex < sessionTableModel.getRowCount()) {
                sessionTableModel.setValueAt(newStatus, rowIndex, 2);
            }
        });
    }

    @Override
    public void onFileUnshared(String unsharer, String receiver, String fileName) {
        addLogMessage(String.format("User '%s' unshared '%s' from '%s'", unsharer, fileName, receiver));
    }

    @Override
    public void onShareUpdated(String updater, String receiver, String fileName) {
        addLogMessage(String.format("User '%s' updated share permissions for '%s' on file '%s'", updater, receiver, fileName));
    }
    //</editor-fold>

    private void updateRowIndexesAfterRemoval(int removedIndex) {
        clientHandlerRowMap.forEach((handler, index) -> {
            if (index > removedIndex) {
                clientHandlerRowMap.put(handler, index - 1);
            }
        });
    }

    public void addLogMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
            txtLogArea.append(String.format("[%s] %s\n", timestamp, message));
            txtLogArea.setCaretPosition(txtLogArea.getDocument().getLength());
        });
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblActiveSessions = new javax.swing.JTable();
        jScrollPane4 = new javax.swing.JScrollPane();
        txtLogArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jSplitPane2.setDividerLocation(200);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        tblActiveSessions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Client IP", "Logged In User", "Status"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane3.setViewportView(tblActiveSessions);

        jSplitPane2.setLeftComponent(jScrollPane3);

        txtLogArea.setColumns(20);
        txtLogArea.setRows(5);
        jScrollPane4.setViewportView(txtLogArea);

        jSplitPane2.setRightComponent(jScrollPane4);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(39, 39, 39)
                .addComponent(jSplitPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 439, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(217, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 481, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * @param args the command line arguments
     */
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTable tblActiveSessions;
    private javax.swing.JTextArea txtLogArea;
    // End of variables declaration//GEN-END:variables
}
