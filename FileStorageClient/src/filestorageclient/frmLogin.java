/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package filestorageclient;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.logging.Logger;

public class frmLogin extends javax.swing.JFrame {

    private final ClientSocketManager clientManager = ClientSocketManager.getInstance();
    private static final Logger logger = Logger.getLogger(frmLogin.class.getName());

    public frmLogin() {
        if (!clientManager.connect()) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi kết nối Server. Vui lòng kiểm tra Server có đang chạy không.",
                    "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
        }

        initComponents();
        this.setLocationRelativeTo(null);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        txtPassword = new javax.swing.JPasswordField();
        btnLogin = new javax.swing.JButton();
        btnRegister = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Đăng nhập");

        jLabel1.setText("Username");

        jLabel2.setText("Password");

        txtUsername.setText("Username");

        txtPassword.setText("jPasswordField1");

        btnLogin.setText("Login");
        btnLogin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLoginActionPerformed(evt);
            }
        });

        btnRegister.setText("Register");
        btnRegister.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegisterActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnLogin)
                        .addGap(34, 34, 34)
                        .addComponent(btnRegister))
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtUsername)
                            .addComponent(txtPassword, javax.swing.GroupLayout.DEFAULT_SIZE, 147, Short.MAX_VALUE))))
                .addContainerGap(152, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnLogin)
                    .addComponent(btnRegister))
                .addContainerGap(163, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoginActionPerformed
        handleLogin(); // Gọi logic xử lý chính
    }//GEN-LAST:event_btnLoginActionPerformed

    private void btnRegisterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegisterActionPerformed
        handleRegisterButton(); // Gọi logic xử lý chính
    }//GEN-LAST:event_btnRegisterActionPerformed
    /**
     * Logic xử lý chức năng Đăng nhập.
     */
    private void handleLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Tên đăng nhập và Mật khẩu.",
                    "Thiếu thông tin", JOptionPane.WARNING_MESSAGE);
            return;
        }
        btnLogin.setEnabled(false);
        new javax.swing.SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return clientManager.login(username, password);
            }

            @Override
            protected void done() {
                try {
                    String result = get(); 

                    if (result.startsWith("LOGIN_SUCCESS:")) {
                        String loggedInUsername = result.substring("LOGIN_SUCCESS:".length()).trim();
                        JOptionPane.showMessageDialog(frmLogin.this, "Đăng nhập thành công! Chào mừng " + loggedInUsername);

                        new frmMainClient().setVisible(true);
                        frmLogin.this.dispose();

                    } else if ("LOGIN_FAIL".equals(result)) {
                        JOptionPane.showMessageDialog(frmLogin.this, "Tên đăng nhập hoặc mật khẩu không đúng.",
                                "Lỗi Đăng nhập", JOptionPane.ERROR_MESSAGE);
                        txtPassword.setText("");
                    } else {
                        JOptionPane.showMessageDialog(frmLogin.this, "Lỗi hệ thống: " + result,
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                        if ("ERROR_CONNECTION".equals(result) || "ERROR_IO".equals(result)) {
                            clientManager.connect();
                        }
                    }
                } catch (Exception ex) {
                    logger.log(java.util.logging.Level.SEVERE, "Lỗi SwingWorker khi Đăng nhập", ex);
                    JOptionPane.showMessageDialog(frmLogin.this, "Lỗi không xác định: " + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnLogin.setEnabled(true); 
                }
            }
        }.execute(); // Chạy SwingWorker
    }

    /**
     * Logic xử lý chức năng Đăng ký.
     */
    private void handleRegisterButton() {
        frmRegister registerForm = new frmRegister(this);
        this.setVisible(false);
        registerForm.setVisible(true);
    }

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> new frmLogin().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogin;
    private javax.swing.JButton btnRegister;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
