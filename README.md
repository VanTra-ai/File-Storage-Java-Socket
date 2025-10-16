# 📂 Hệ thống Lưu trữ File - Java Socket & MySQL (Version 3.0)

Đây là một ứng dụng Client-Server hoàn chỉnh được xây dựng bằng **Java**, mô phỏng một hệ thống lưu trữ file trên đám mây. Dự án sử dụng **Java Socket** với mã hóa **SSL/TLS** cho giao tiếp mạng, **Java Swing** cho giao diện người dùng, và **MySQL** để quản lý metadata.

---

## 🚀 Tính năng Chính (Version 3.0: Dashboard & Hoàn thiện)

Phiên bản này giới thiệu Giao diện Quản lý Server (Dashboard) và hoàn thiện các tính năng cốt lõi, mang lại một hệ thống mạnh mẽ và dễ kiểm soát.

- **Bảo mật Toàn diện:**

  - **Mã hóa Vận chuyển:** Mọi dữ liệu giữa Client và Server đều được mã hóa bằng **SSL/TLS**.
  - **Mã hóa Mật khẩu:** Mật khẩu người dùng được băm và salt an toàn bằng thư viện **Bcrypt** trước khi lưu vào CSDL.

- **Quản lý Người dùng & File:**

  - Đăng ký, Đăng nhập và Đăng xuất tài khoản.
  - Tải file lên (Upload), tải file xuống (Download), và Xóa file cá nhân.

- **Hệ thống Chia sẻ Nâng cao:**

  - Chia sẻ file cho người dùng khác qua Username.
  - **Chia sẻ có thời hạn:** Thiết lập thời gian hết hạn cho lượt chia sẻ (ví dụ: 1 phút, 5 phút), quyền truy cập sẽ được **tự động thu hồi** bởi Server.
  - Cập nhật quyền và thời hạn chia sẻ.
  - Hủy chia sẻ (Unshare).

- **🖥️ Giao diện Quản lý Server (Dashboard):**
  - Hiển thị danh sách các client đang kết nối trong thời gian thực.
  - Theo dõi trạng thái của từng client: IP, Tên tài khoản, Hoạt động hiện tại (`Uploading...`, `Downloading...`, `Idle`).
  - Ghi lại (log) tất cả các sự kiện quan trọng: kết nối/ngắt kết nối, đăng nhập/đăng xuất, đăng ký, upload, download, xóa, và các thao tác chia sẻ.

---

## 🛠️ Công nghệ Sử dụng

| Thành phần          | Công nghệ / Thư viện              |
| :------------------ | :-------------------------------- |
| **Ngôn ngữ**        | Java (JDK 17+)                    |
| **Giao diện**       | Java Swing (NetBeans GUI Builder) |
| **Giao tiếp Mạng**  | Java Socket (TCP/IP qua SSL/TLS)  |
| **Cơ sở dữ liệu**   | MySQL (hoặc MariaDB)              |
| **Kết nối CSDL**    | JDBC (`mysql-connector-j`)        |
| **Mã hóa Mật khẩu** | `jbcrypt`                         |

---

## ⚙️ Cấu trúc Dự án

Dự án được tổ chức theo kiến trúc Client-Server rõ ràng, áp dụng các mẫu thiết kế như **Singleton**, **Command Pattern**, và **Observer**.

- **`FileStorageServer`** (Dự án Server)

  - `filestorageserver`: Chứa các lớp lõi.
    - `FileServer.java`: Điểm khởi chạy chính, quản lý kết nối và các tác vụ nền.
    - `ClientHandler.java`: Bộ điều phối, nhận lệnh và giao việc.
    - `DashboardFrame.java`: Giao diện quản lý, đóng vai trò "Người lắng nghe" sự kiện.
    - `FileDAO.java` / `UserDAO.java`: Các lớp truy cập dữ liệu.
  - `filestorageserver.commands`: Mỗi file là một lệnh riêng biệt (Command Pattern).
  - `filestorageserver.model`: Các lớp POJO ánh xạ tới bảng CSDL.

- **`FileStorageClient`** (Dự án Client)
  - `filestorageclient`:
    - `ClientSocketManager.java`: "Bộ não" của Client, quản lý kết nối và mọi giao tiếp với Server (Singleton Pattern).
    - `frmLogin.java`, `frmMainClient.java`,...: Các lớp giao diện Swing.

---

## 🔑 Hướng dẫn Cài đặt và Chạy

### **Bước 0: Yêu cầu**

- JDK 17 hoặc mới hơn.
- Apache NetBeans IDE.
- XAMPP hoặc một hệ quản trị CSDL MySQL/MariaDB.

### **Bước 1: Thiết lập Cơ sở dữ liệu**

1.  Sử dụng **phpMyAdmin** hoặc một công cụ tương tự, tạo một database mới với tên `file_storage_db` và collation là `utf8mb4_unicode_ci`.
2.  Thực thi toàn bộ đoạn mã SQL dưới đây để tạo các bảng `users`, `files`, và `file_shares`.

```sql
CREATE TABLE `users` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `email` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `last_login` datetime DEFAULT current_timestamp(),
  `is_active` tinyint(1) DEFAULT 1,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `files` (
  `file_id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) NOT NULL,
  `file_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_path` varchar(250) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `file_size` bigint(20) NOT NULL,
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `last_modified` datetime DEFAULT NULL,
  `is_shared` tinyint(1) DEFAULT 0,
  `share_token` varchar(36) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `share_expiry` datetime DEFAULT NULL,
  PRIMARY KEY (`file_id`),
  KEY `owner_id` (`owner_id`),
  CONSTRAINT `files_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `file_shares` (
  `share_id` int(11) NOT NULL AUTO_INCREMENT,
  `file_id` int(11) NOT NULL,
  `shared_with_user_id` int(11) NOT NULL,
  `shared_by_user_id` int(11) NOT NULL,
  `permission_level` tinyint(1) DEFAULT 1,
  `shared_at` datetime NOT NULL DEFAULT current_timestamp(),
  `share_expiry` datetime DEFAULT NULL,
  PRIMARY KEY (`share_id`),
  UNIQUE KEY `uk_file_user` (`file_id`,`shared_with_user_id`),
  KEY `shared_with_user_id` (`shared_with_user_id`),
  KEY `shared_by_user_id` (`shared_by_user_id`),
  CONSTRAINT `file_shares_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `files` (`file_id`) ON DELETE CASCADE,
  CONSTRAINT `file_shares_ibfk_2` FOREIGN KEY (`shared_with_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  CONSTRAINT `file_shares_ibfk_3` FOREIGN KEY (`shared_by_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## 🧩 Bước 2: Cấu hình Dự án

> ⚠️ **Quan trọng:** Bạn cần cập nhật **các đường dẫn tuyệt đối** trong code để khớp với **cấu trúc thư mục trên máy của bạn**.

### 🖥️ Server – Kết nối CSDL

- **Mở file:**  
  `FileStorageServer/src/filestorageserver/MyConnection.java`
- **Thực hiện:**  
  Chỉnh sửa chuỗi `URL`, `username`, hoặc `password` nếu thông tin MySQL của bạn **khác mặc định** (ví dụ: cổng, tài khoản, mật khẩu).

---

### 💾 Server – Đường dẫn Lưu trữ & SSL

1. **Thiết lập thư mục lưu trữ**
   - **Mở file:**  
     `FileStorageServer/src/filestorageserver/commands/UploadCommandHandler.java`
   - **Thực hiện:**  
     Thay đổi giá trị của biến:
     ```java
     SERVER_STORAGE_ROOT
     ```
     thành **thư mục bạn muốn dùng để lưu file**.
2. **Cấu hình SSL**
   - **Mở file:**  
     `FileStorageServer/src/filestorageserver/FileServer.java`
   - **Thực hiện:**  
     Cập nhật giá trị:
     ```java
     absoluteKeyStorePath
     ```
     thành **đường dẫn tuyệt đối** đến file `server.jks` trên máy của bạn.

---

### 💻 Client – SSL TrustStore

- **Mở file:**  
  `FileStorageClient/src/filestorageclient/ClientSocketManager.java`
- **Thực hiện:**  
  Cập nhật giá trị:
  ```java
  absoluteTrustStorePath
  ```

## 🚀 Bước 3: Chạy Ứng dụng

1. **Khởi động CSDL**  
   Đảm bảo **module MySQL trong XAMPP** đang ở trạng thái **“Running”**.

2. **Chạy Server**

   - Mở project `FileStorageServer`
   - Chuột phải vào file `FileServer.java` → chọn **Run File**
   - Cửa sổ **Server Dashboard** sẽ xuất hiện.

3. **Chạy Client**
   - Mở project `FileStorageClient`
   - Chuột phải vào file `frmLogin.java` → chọn **Run File**

> 💡 Bạn có thể **chạy nhiều Client cùng lúc** để kiểm tra **tính năng đa người dùng** và **quan sát trạng thái trên Dashboard**.
