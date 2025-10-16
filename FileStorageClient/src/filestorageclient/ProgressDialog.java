package filestorageclient;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

/**
 * Một JDialog đơn giản để hiển thị tiến độ của một tác vụ.
 */
public class ProgressDialog extends JDialog {

    private final JProgressBar progressBar;
    private final JLabel lblStatus;

    public ProgressDialog(JFrame parent, String title) {
        super(parent, title, true); // true = modal, chặn tương tác với cửa sổ cha

        setSize(400, 120);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // Ngăn người dùng đóng cửa sổ

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        lblStatus = new JLabel("Starting...");
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        contentPanel.add(lblStatus, BorderLayout.NORTH);
        contentPanel.add(progressBar, BorderLayout.CENTER);

        add(contentPanel);
    }

    /**
     * Cập nhật giá trị phần trăm trên thanh tiến độ.
     *
     * @param percentage Giá trị từ 0 đến 100.
     */
    public void updateProgress(int percentage) {
        progressBar.setValue(percentage);
    }

    /**
     * Cập nhật văn bản trạng thái (ví dụ: "Đã tải lên 5.2 / 10.8 MB").
     *
     * @param statusText Văn bản mới.
     */
    public void setStatusText(String statusText) {
        lblStatus.setText(statusText);
    }

    /**
     * Đóng hộp thoại.
     */
    public void closeDialog() {
        dispose();
    }
}
