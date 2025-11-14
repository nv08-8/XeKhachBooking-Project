# XeKhachBooking-Project 🚍

> Hệ thống đặt vé xe khách trực tuyến với ứng dụng Android và Backend API
> 
> Online Bus Ticket Booking System with Android App and Backend API

## 📱 Giới thiệu / Introduction

**XeKhachBooking** là một hệ thống đặt vé xe khách hoàn chỉnh, bao gồm:
- **Ứng dụng Android** (Java) - Giao diện người dùng để đặt vé
- **Backend API** (Node.js + Express + MySQL) - Xử lý nghiệp vụ và lưu trữ dữ liệu

**XeKhachBooking** is a complete bus ticket booking system that includes:
- **Android Application** (Java) - User interface for booking tickets
- **Backend API** (Node.js + Express + MySQL) - Business logic and data storage

## 🎯 Tính năng / Features

### Ứng dụng Android / Android App
- ✅ Đăng ký tài khoản với xác thực OTP qua email
- ✅ Đăng nhập/Đăng xuất
- ✅ Quên mật khẩu và đặt lại mật khẩu
- ✅ Tìm kiếm chuyến xe theo tuyến đường
- ✅ Xem danh sách chuyến xe
- ✅ Chọn ghế ngồi
- ✅ Chọn điểm đón và trả khách
- ✅ Nhập thông tin hành khách
- ✅ Thanh toán
- ✅ Xem thông tin tài khoản
- ✅ Màn hình khách (Guest mode)

### Backend API
- ✅ Quản lý người dùng (User management)
- ✅ Gửi OTP qua email
- ✅ Xác thực OTP
- ✅ Đăng ký tài khoản
- ✅ Đăng nhập
- ✅ Quên mật khẩu
- ✅ Đặt lại mật khẩu

## 🛠️ Công nghệ / Technology Stack

### Android App
- **Language:** Java
- **Min SDK:** 23 (Android 6.0)
- **Target SDK:** 36
- **Libraries:**
  - Retrofit 2.9.0 - HTTP client
  - Gson - JSON parsing
  - AppCompat, Material Design Components
  - ConstraintLayout
  - Core SplashScreen

### Backend API
- **Runtime:** Node.js
- **Framework:** Express.js 4.21.2
- **Database:** MySQL 2 (via mysql2 package)
- **Libraries:**
  - bcrypt 6.0.0 - Password hashing
  - cors 2.8.5 - Cross-Origin Resource Sharing
  - dotenv 16.6.1 - Environment variables
  - nodemailer 7.0.10 - Email sending

## 📁 Cấu trúc dự án / Project Structure

```
XeKhachBooking-Project/
├── app/                          # Android Application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/vn/hcmute/busbooking/
│   │   │   │   ├── activity/     # Activities (UI screens)
│   │   │   │   ├── adapter/      # RecyclerView adapters
│   │   │   │   ├── api/          # Retrofit API client
│   │   │   │   ├── fragment/     # Fragments
│   │   │   │   ├── model/        # Data models
│   │   │   │   └── utils/        # Utility classes
│   │   │   ├── res/              # Resources (layouts, drawables, etc.)
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/          # Instrumented tests
│   │   └── test/                 # Unit tests
│   └── build.gradle.kts          # App-level Gradle config
│
├── backend_api/                  # Backend API Server
│   ├── routes/
│   │   └── authRoutes.js        # Authentication endpoints
│   ├── utils/
│   │   └── sendEmail.js         # Email utility
│   ├── db.js                    # Database connection
│   ├── server.js                # Express server entry point
│   ├── package.json             # Node.js dependencies
│   └── .env                     # Environment variables (not in git)
│
├── build.gradle.kts             # Root-level Gradle config
├── settings.gradle.kts          # Gradle settings
└── README.md                    # This file
```

## 🚀 Hướng dẫn cài đặt / Installation Guide

### Prerequisites / Yêu cầu
- Android Studio (for Android app)
- Node.js (v14 or higher)
- MySQL Server
- Git

### Backend API Setup

1. **Clone the repository**
```bash
git clone https://github.com/nv08-8/XeKhachBooking-Project.git
cd XeKhachBooking-Project/backend_api
```

2. **Install dependencies**
```bash
npm install
```

3. **Configure environment variables**
Create a `.env` file in the `backend_api` directory:
```env
DB_HOST=your_mysql_host
DB_USER=your_mysql_user
DB_PASS=your_mysql_password
DB_NAME=your_database_name
DB_PORT=3306

PORT=3000

MAIL_USER=your_email@gmail.com
MAIL_PASS=your_app_password
```

4. **Setup MySQL Database**
Create a database and a `users` table:
```sql
CREATE DATABASE railway;
USE railway;

CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255),
    otp_code VARCHAR(6),
    status ENUM('pending', 'active') DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

5. **Start the server**
```bash
npm start
```

The API will be running at `http://localhost:3000`

### Android App Setup

1. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned repository folder
   - Wait for Gradle sync to complete

2. **Configure API Base URL**
   Update the API base URL in `app/src/main/java/vn/hcmute/busbooking/utils/Constants.java`:
   ```java
   public static final String BASE_URL = "http://your-api-url:3000/";
   ```

3. **Build and Run**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio
   - Select your device/emulator

## 📡 API Endpoints

### Authentication Routes (`/api/auth`)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/send-otp` | Send OTP to email for registration |
| POST | `/verify-otp` | Verify OTP code |
| POST | `/finish-register` | Complete registration with name and password |
| POST | `/login` | User login |
| POST | `/forgot-password` | Send OTP for password reset |
| POST | `/reset-password` | Reset password with new password |

#### Example Request: Send OTP
```json
POST /api/auth/send-otp
Content-Type: application/json

{
  "email": "user@example.com"
}
```

#### Example Response
```json
{
  "message": "OTP đã được gửi đến email!"
}
```

## 🎨 Screenshots

> Add screenshots of your app here

## 👥 Contributors / Đóng góp

- [nv08-8](https://github.com/nv08-8)

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

## 🤝 Contributing / Đóng góp

Contributions, issues, and feature requests are welcome!

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📞 Contact / Liên hệ

Project Link: [https://github.com/nv08-8/XeKhachBooking-Project](https://github.com/nv08-8/XeKhachBooking-Project)

---

Made with ❤️ by HCMUTE Students