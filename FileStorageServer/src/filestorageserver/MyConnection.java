/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package filestorageserver;

import java.sql.Connection;
import java.sql.DriverManager;
import javax.swing.JOptionPane;

public class MyConnection {

    public Connection getConnection() {
        try {
            String URL = "jdbc:mysql://localhost/file_storage_db?user=root&password=&serverTimezone=UTC&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";
            Connection con = DriverManager.getConnection(URL);
            return con;
        } catch (Exception ex) {
            // Thay thế JOptionPane bằng việc in ra console/log
            System.err.println("Lỗi kết nối CSDL: " + ex.getMessage());
            ex.printStackTrace(); // In ra stack trace chi tiết
            return null;
        }
    }
}
