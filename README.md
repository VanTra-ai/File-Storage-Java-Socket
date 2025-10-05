# 📂 File Storage System - Java Socket & MySQL

Đây là một ứng dụng quản lý lưu trữ file đơn giản, được phát triển bằng Java, sử dụng **Socket** cho giao tiếp Client-Server và **MySQL** để quản lý metadata (thông tin file và tài khoản người dùng).

---

## 🚀 Tính năng Chính

* **Xác thực Người dùng:**
    * Đăng ký tài khoản mới (sử dụng băm mật khẩu và Salting an toàn).
    * Đăng nhập hệ thống.
* **Quản lý File:**
    * **Upload** file từ Client lên Server.
    * **Download** file từ Server về Client.
    * **Delete** file khỏi hệ thống.
    * Hiển thị danh sách file cá nhân (Metadata).

---

## 🛠️ Công nghệ Sử dụng

* **Ngôn ngữ Lập trình:** Java (JDK 8 trở lên)
* **Giao tiếp:** Java Socket (TCP)
* **Cơ sở dữ liệu:** MySQL (hoặc MariaDB thông qua XAMPP)
* **Thư viện UI:** Swing (Sử dụng NetBeans Form Designer)
* **Thư viện Database:** MySQL Connector/J

---

## ⚙️ Cấu trúc Dự án

Repository này chứa hai thư mục chính:

1.  ### `FileStorageServer`
    * Chứa logic xử lý kết nối, giao thức (Commands), tương tác Database (FileDAO, UserDAO), và lưu trữ file vật lý trên đĩa.
    * **File cấu hình quan trọng:** `MyConnection.java` (để cấu hình kết nối DB).

2.  ### `FileStorageClient`
    * Chứa giao diện người dùng (Forms) và logic giao tiếp với Server thông qua `ClientSocketManager.java`.

---

## 🔧 Hướng dẫn Thiết lập và Chạy

### 1. Thiết lập Cơ sở Dữ liệu

1.  Sử dụng XAMPP hoặc MySQL Workbench để tạo một database mới với tên: **`file_storage_db`**.
2.  Chạy các lệnh SQL sau để tạo bảng **`users`** và **`files`**:

    ```sql
    -- Bảng users
    CREATE TABLE `users` (
      `user_id` int(11) NOT NULL AUTO_INCREMENT,
      `username` varchar(50) NOT NULL UNIQUE,
      `password_hash` varchar(255) NOT NULL,
      `salt` varchar(50) NOT NULL,
      `email` varchar(100) DEFAULT NULL UNIQUE,
      `status` tinyint(4) DEFAULT '1',
      `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `is_active` tinyint(4) DEFAULT '1',
      `last_login` datetime DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    -- Bảng files
    CREATE TABLE `files` (
      `file_id` int(11) NOT NULL AUTO_INCREMENT,
      `owner_id` int(11) NOT NULL,
      `file_name` varchar(255) NOT NULL,
      `file_path` varchar(250) NOT NULL,
      `mime_type` varchar(50) DEFAULT NULL,
      `file_size` bigint(20) NOT NULL,
      `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `last_modified` datetime DEFAULT CURRENT_TIMESTAMP,
      `is_shared` tinyint(1) DEFAULT '0',
      `share_token` varchar(36) DEFAULT NULL,
      `share_expiry` datetime DEFAULT NULL,
      PRIMARY KEY (`file_id`),
      FOREIGN KEY (`owner_id`) REFERENCES `users` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
    ```

3.  Cập nhật thông tin kết nối Database (URL, Tên người dùng, Mật khẩu) trong file **`FileStorageServer/src/filestorageserver/MyConnection.java`**.

### 2. Chạy Ứng dụng

1.  **Chạy Server:** Mở project `FileStorageServer` và chạy class `FileServer.java`. Server sẽ lắng nghe kết nối tại cổng **12345**.
2.  **Chạy Client:** Mở project `FileStorageClient` và chạy class `frmLogin.java` để bắt đầu sử dụng ứng dụng.
