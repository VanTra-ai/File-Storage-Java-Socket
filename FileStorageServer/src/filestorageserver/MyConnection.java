package filestorageserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Lớp tiện ích để tạo kết nối đến cơ sở dữ liệu MySQL. * LƯU Ý KIẾN TRÚC: Cách
 * triển khai này tạo một kết nối mới cho mỗi lần gọi, điều này không hiệu quả.
 * Trong môi trường thực tế, nên sử dụng một Connection Pool (như HikariCP) để
 * quản lý và tái sử dụng các kết nối.
 */
public class MyConnection {

    /**
     * Thiết lập một kết nối mới đến cơ sở dữ liệu.
     *
     * @return một đối tượng Connection, hoặc null nếu kết nối thất bại.
     */
    public Connection getConnection() {
        // LƯU Ý: Chuỗi kết nối này nên được đọc từ một file cấu hình bên ngoài.
        String URL = "jdbc:mysql://localhost/file_storage_db?user=root&password=&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";

        try {
            return DriverManager.getConnection(URL);
        } catch (SQLException ex) {
            System.err.println("LỖI KẾT NỐI CSDL: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }
}
