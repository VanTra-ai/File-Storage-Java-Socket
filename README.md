# 📂 File Storage System - Java Socket & MySQL (Version 2.0)

Đây là một ứng dụng quản lý lưu trữ file hoàn chỉnh, được phát triển bằng **Java**, sử dụng **Socket (TCP)** cho giao tiếp Client-Server và **MySQL** để quản lý metadata (thông tin file và tài khoản người dùng).

---

## 🚀 Tính năng Chính (Version 2.0: Sharing & Authorization)

Bản cập nhật này tập trung vào việc triển khai đầy đủ tính năng chia sẻ quyền truy cập, nâng cao khả năng quản lý file và bảo mật.

- **Xác thực Người dùng An toàn:** Đăng ký và Đăng nhập sử dụng **Bcrypt** để băm mật khẩu và Salting.
- **Quản lý File Cơ bản:** Upload, Download, Delete file khỏi Server.
- **Danh sách File Tổng hợp:** Hiển thị **cả file sở hữu và file được chia sẻ** với người dùng, bao gồm thông tin `Status` và `Sharer`.
- **Quản lý Quyền Chia sẻ (Authorization):**
  - **Chia sẻ File (`SHARE`):** Chủ sở hữu cấp quyền truy cập (Download Only) cho người dùng khác qua Username.
  - **Hủy Chia sẻ (`UNSHARE`):** Chủ sở hữu thu hồi quyền đã cấp.
  - **Cập nhật Quyền (`CHANGE_PERM`):** Thay đổi mức quyền truy cập cho người được chia sẻ.
  - **Liệt kê người được chia sẻ (`SHARE_LIST`):** Chỉ chủ sở hữu mới có thể xem danh sách chi tiết những người đang có quyền truy cập file.

---

## 🛠️ Công nghệ Sử dụng

| Thành phần             | Công nghệ/Thư viện                  |
| :--------------------- | :---------------------------------- |
| **Ngôn ngữ Lập trình** | Java (JDK 17)                       |
| **Giao tiếp**          | Java Socket (TCP)                   |
| **Cơ sở dữ liệu**      | MySQL (hoặc MariaDB)                |
| **Giao diện**          | Java Swing (NetBeans Form Designer) |
| **Thư viện Database**  | `mysql-connector-j-8.3.0.jar`       |
| **Bảo mật**            | `bcrypt-0.4.jar` (Mã hóa mật khẩu)  |

---

## ⚙️ Cấu trúc Dự án

| Project                 | Package/Folder      | Vai trò Chính                          | File quan trọng                                                                                                                     |
| :---------------------- | :------------------ | :------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------- |
| **`FileStorageServer`** | `filestorageserver` | Logic Server, Socket, Database Access. | `FileServer.java` (Main), `ClientHandler.java` (Giao thức), **`FileDAO.java`** (Quản lý File & Chia sẻ), `UserDAO.java` (Xác thực). |
| **`FileStorageClient`** | `filestorageclient` | Giao diện Người dùng, Logic gửi lệnh.  | `ClientSocketManager.java` (Giao tiếp Server), `frmMainClient.java`, **`frmShareFile.java`** (Quản lý Sharing UI).                  |

---

## 🔑 Thiết lập Cơ sở Dữ liệu

1.  **Tạo Database:** Tạo database có tên: **`file_storage_db`**.
2.  **Tạo Bảng:** Thực thi toàn bộ đoạn mã SQL dưới đây để tạo ba bảng: **`users`**, **`files`**, và **`file_shares`**.

### 2.1. Code SQL

```sql
-- 1. Bảng users
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

-- 2. Bảng files
CREATE TABLE `files` (
  `file_id` int(11) NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_path` varchar(250) NOT NULL, -- Đường dẫn vật lý trên Server
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

-- 3. Bảng file_shares (Quản lý Quyền Truy cập)
CREATE TABLE `file_shares` (
  `share_id` int(11) NOT NULL AUTO_INCREMENT,
  `file_id` int(11) NOT NULL,
  `shared_with_user_id` int(11) NOT NULL, -- Người nhận quyền
  `shared_by_user_id` int(11) NOT NULL, -- Chủ sở hữu/Người cấp quyền
  `permission_level` tinyint(1) DEFAULT '1', -- Mức quyền: 1 (Download Only)
  `shared_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`share_id`),
  FOREIGN KEY (`file_id`) REFERENCES `files` (`file_id`) ON DELETE CASCADE,
  FOREIGN KEY (`shared_with_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  FOREIGN KEY (`shared_by_user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```
## 🔑 Giao Thức Lệnh Mở Rộng (Client-Server Protocol)

Client gửi các lệnh dưới dạng chuỗi dữ liệu qua Socket. Server (`ClientHandler`) phân tích cú pháp lệnh.

### A. Lệnh Chia sẻ (String Commands)

| Lệnh Gửi (Cú pháp) | Gửi từ Client | Xử lý Server | Phản hồi Thành công |
| :--- | :--- | :--- | :--- |
| **Chia sẻ** | `SHARE:<FileID>|<Username_Target>|<Level>` | `FileDAO.shareFile` | `SHARE_SUCCESS` / Mã lỗi |
| **Hủy Chia sẻ** | `UNSHARE:<FileID>|<Username_Target>` | `FileDAO.unshareFile` | `UNSHARE_SUCCESS` / Mã lỗi |
| **Cập nhật Quyền** | `CHANGE_PERM:<FileID>|<Username_Target>|<NewLevel>` | `FileDAO.updateFileSharePermission` | `UPDATE_SUCCESS` / Mã lỗi |
| **Lấy DS Chia sẻ**| `SHARE_LIST:<FileID>` | `FileDAO.getSharedUsersByFile` | `SHARELIST_START:user1|1|date;...` / Mã lỗi |

### B. Lệnh Cố định (CMD\_)

| Lệnh | Gửi từ Client | Phản hồi Thành công | Mô tả |
| :--- | :--- | :--- | :--- |
| `CMD_LOGIN` | `UserID, Password` | `LOGIN_SUCCESS, UserID, Username` | Xác thực người dùng. |
| `CMD_LISTFILES`| (Không) | `FILELIST_START:data` | Hiển thị file sở hữu và file được chia sẻ. |
| `CMD_DOWNLOAD` | `FileID` | `DOWNLOAD_START:data` | Kiểm tra quyền Owner hoặc Shared. |
| `CMD_DELETE` | `FileID` | `DELETE_SUCCESS` | Chỉ cho phép Owner xóa. |

---

## 🔧 Hướng dẫn Thiết lập và Chạy

### 0. Thiết lập Thư viện (Libraries)

1.  Trong NetBeans, nhấp chuột phải vào mục **Libraries** của **cả hai Project** (`FileStorageServer` và `FileStorageClient`).
2.  Chọn **Add JAR/Folder** và thêm hai file `.jar` sau từ thư mục **`Drivers`**:
    * `mysql-connector-j-8.3.0.jar`
    * `bcrypt-0.4.jar`

### 1. Thiết lập Cơ sở Dữ liệu

1.  Tạo Database mới có tên: **`file_storage_db`** (sử dụng XAMPP/MySQL Workbench).
2.  Thực thi toàn bộ đoạn mã SQL trong mục "Cấu trúc Cơ sở Dữ liệu" để tạo ba bảng: users, files, và file_shares.
3.  Cập nhật thông tin kết nối Database (URL, User, Pass) trong file **`FileStorageServer/src/filestorageserver/MyConnection.java`**.
4.  Thiết lập đường dẫn lưu trữ file vật lý trên Server trong `FileServer.java` (nếu cần).
=======
1.  Sử dụng XAMPP để tạo một database mới với tên: **`file_storage_db`**.
2.  Chạy các lệnh SQL sau để tạo bảng **`users`** và **`files`**:

    ```sql
    -- Kiểm tra và xóa Database cũ nếu tồn tại
      -- DROP DATABASE IF EXISTS file_storage_db;
      
      -- Tạo Database mới
      -- CREATE DATABASE file_storage_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
      -- USE file_storage_db;
      
      CREATE TABLE `users` (
     `user_id` int(11) NOT NULL AUTO_INCREMENT,
     `username` varchar(50) NOT NULL UNIQUE,
     `password_hash` varchar(255) NOT NULL,
     `salt` varchar(50) NOT NULL,
     `email` varchar(100) DEFAULT NULL UNIQUE,
     `status` tinyint(4) DEFAULT '1',
     `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
     `is_active` tinyint(4) DEFAULT '1',
     `last_login` datetime DEFAULT CURRENT_TIMESTAMP, -- ĐÃ SỬA
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

1.  **Chạy Server:** Mở project `FileStorageServer` và chạy class **`FileServer.java`** (`Shift + F6`). Server sẽ khởi động và lắng nghe tại cổng **12345**.
2.  **Chạy Client:** Mở project `FileStorageClient` và chạy class **`frmLogin.java`** (`Shift + F6`).
3.  Đăng ký hai tài khoản (ví dụ: `userA` và `userB`) để kiểm tra tính năng chia sẻ.
