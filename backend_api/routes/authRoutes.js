const express = require("express");
const router = express.Router();
const db = require("../db");
const bcrypt = require("bcrypt");
const sendEmail = require("../utils/sendEmail");
const SALT_ROUNDS = 10;

// ... (Các route khác không thay đổi)

/* ============================================================
    1. SEND OTP (REGISTER)
   ============================================================ */

router.post("/send-otp", (req, res) => {
    const { email } = req.body;

    if (!email) {
        return res.status(400).json({ message: "Thiếu email!" });
    }

    const otp = Math.floor(100000 + Math.random() * 900000);

    db.query("SELECT * FROM users WHERE email=?", [email], (err, rows) => {
        if (err) return res.status(500).json({ message: err.message });

        if (rows.length === 0) {
            db.query(
                "INSERT INTO users (email, otp_code, status) VALUES (?, ?, 'pending')",
                [email, otp],
                (err2) => {
                    if (err2) return res.status(500).json({ message: err2.message });

                    res.json({ message: "OTP đã được gửi đến email!" });
                    sendEmail(email, "Mã OTP đăng ký", `Mã OTP của bạn là: ${otp}`)
                        .catch(emailErr => console.error("Gửi email thất bại:", emailErr));
                }
            );
        } else {
            const user = rows[0];
            if (user.status === "active") {
                return res.status(400).json({ message: "Email đã tồn tại!" });
            }
            db.query(
                "UPDATE users SET otp_code=?, status='pending' WHERE email=?",
                [otp, email],
                (err3) => {
                    if (err3) return res.status(500).json({ message: err3.message });
                    
                    res.json({ message: "OTP đã được gửi đến email!" });
                    sendEmail(email, "Mã OTP đăng ký", `Mã OTP của bạn là: ${otp}`)
                        .catch(emailErr => console.error("Gửi email thất bại:", emailErr));
                }
            );
        }
    });
});

/* ============================================================
    2. VERIFY OTP
   ============================================================ */

router.post("/verify-otp", (req, res) => {
    const { email, otp } = req.body;

    db.query(
        "SELECT * FROM users WHERE email=? AND otp_code=?",
        [email, otp],
        (err, rows) => {
            if (err) return res.status(500).json({ success: false, message: err.message });
            if (rows.length === 0) return res.status(400).json({ success: false, message: "OTP sai!" });
            return res.json({ success: true, message: "OTP hợp lệ!" });
        }
    );
});


/* ============================================================
    3. FINISH REGISTER
   ============================================================ */

router.post("/finish-register", async (req, res) => {
    const { name, phone, email, password } = req.body;

    if (!name || !phone || !email || !password) {
        return res.status(400).json({ message: "Thiếu thông tin!" });
    }

    const phoneNumberAsInt = parseInt(phone, 10);
    if (isNaN(phoneNumberAsInt)) {
        return res.status(400).json({ message: "Số điện thoại không hợp lệ." });
    }

    db.query("SELECT * FROM users WHERE sdt=? AND status='active'", [phoneNumberAsInt], async (err, phoneRows) => {
        if (err) {
            console.error("Lỗi kiểm tra SĐT:", err);
            return res.status(500).json({ message: "Lỗi phía server." });
        }

        if (phoneRows.length > 0) {
            return res.status(400).json({ message: "Số điện thoại đã được sử dụng!" });
        }

        const hashed = await bcrypt.hash(password, SALT_ROUNDS);

        db.query(
            "UPDATE users SET name=?, sdt=?, password=?, role='user', otp_code=NULL, status='active' WHERE email=?",
            [name, phoneNumberAsInt, hashed, email],
            (err2) => {
                if (err2) {
                    console.error("Lỗi cập nhật user:", err2);
                    return res.status(500).json({ message: "Lỗi khi tạo tài khoản." });
                }
                
                return res.json({ message: "Tạo tài khoản thành công!" });
            }
        );
    });
});

/* ============================================================
    4. LOGIN
   ============================================================ */

router.post("/login", (req, res) => {
    const { email, password } = req.body;

    db.query(
        "SELECT * FROM users WHERE email=?",
        [email],
        async (err, rows) => {
            if (err) return res.status(500).json({ message: err.message });

            if (rows.length === 0) {
                return res.status(401).json({ message: "Sai email hoặc mật khẩu!" });
            }

            const user = rows[0];

            if (user.status !== "active") {
                return res.status(403).json({ message: "Tài khoản chưa xác thực email!" });
            }

            const isMatch = await bcrypt.compare(password, user.password);
            if (!isMatch) {
                return res.status(401).json({ message: "Sai email hoặc mật khẩu!" });
            }

            delete user.password;

            return res.json({
                message: "Đăng nhập thành công!",
                user
            });
        }
    );
});


/* ============================================================
    5. FORGOT PASSWORD
   ============================================================ */

router.post("/forgot-password", (req, res) => {
    const { email } = req.body;
    const otp = Math.floor(100000 + Math.random() * 900000);

    db.query(
        "UPDATE users SET otp_code=? WHERE email=? AND status='active'",
        [otp, email],
        (err, result) => {
            if (err) return res.status(500).json({ message: err.message });
            if (result.affectedRows === 0) return res.status(404).json({ message: "Email không tồn tại hoặc chưa active!" });

            res.json({ success: true, message: "OTP đã được gửi đến email!" });
            sendEmail(email, "Mã OTP đặt lại mật khẩu", `Mã OTP của bạn là: ${otp}`)
                .catch(emailErr => console.error("Gửi email quên mật khẩu thất bại:", emailErr));
        }
    );
});

/* ============================================================
    6. RESET PASSWORD
   ============================================================ */

router.post("/reset-password", async (req, res) => {
    const { email, newPassword } = req.body;
    const hashed = await bcrypt.hash(newPassword, SALT_ROUNDS);

    db.query(
        "UPDATE users SET password=?, otp_code=NULL WHERE email=? AND status='active'",
        [hashed, email],
        (err) => {
            if (err) return res.status(500).json({ message: err.message });
            res.json({ success: true, message: "Đặt lại mật khẩu thành công!" });
        }
    );
});


/* ============================================================
    7. (TẠM THỜI) MAKE ADMIN TOOL
   ============================================================ */

router.post("/make-admin", (req, res) => {
    const { email } = req.body;

    if (!email) {
        return res.status(400).json({ message: "Vui lòng cung cấp email." });
    }

    db.query(
        "UPDATE users SET role = 'admin' WHERE email = ?",
        [email],
        (err, result) => {
            if (err) {
                console.error("Lỗi khi phong admin:", err);
                return res.status(500).json({ message: "Lỗi server khi cập nhật vai trò." });
            }
            if (result.affectedRows === 0) {
                return res.status(404).json({ message: "Không tìm thấy người dùng với email này." });
            }
            res.status(200).json({ message: `Đã cập nhật tài khoản ${email} thành admin.` });
        }
    );
});


module.exports = router;
