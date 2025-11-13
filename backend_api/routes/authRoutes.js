const express = require("express");
const router = express.Router();
const db = require("../db");
const bcrypt = require("bcrypt");
const sendEmail = require("../utils/sendEmail");
const SALT_ROUNDS = 10;

/* ============================================================
    1. SEND OTP (REGISTER)
   ============================================================ */

router.post("/send-otp", (req, res) => {
    const { email } = req.body;

    if (!email)
        return res.status(400).json({ message: "Thiếu email!" });

    const otp = Math.floor(100000 + Math.random() * 900000);

    // Kiểm tra email
    db.query("SELECT * FROM users WHERE email=?", [email], (err, rows) => {
        if (err) return res.status(500).json({ message: err.message });

        // Email chưa tồn tại → tạo user pending
        if (rows.length === 0) {
            db.query(
                "INSERT INTO users (email, otp_code, status) VALUES (?, ?, 'pending')",
                [email, otp],
                async (err2) => {
                    if (err2) return res.status(500).json({ message: err2.message });

                    await sendEmail(email, "Mã OTP đăng ký", `Mã OTP của bạn là: ${otp}`);
                    return res.json({ message: "OTP đã được gửi đến email!" });
                }
            );
        } else {
            // Email đã tồn tại → nếu ACTIVE thì không cho đăng ký lại
            const user = rows[0];
            if (user.status === "active") {
                return res.status(400).json({ message: "Email đã tồn tại!" });
            }

            // Nếu pending → cập nhật OTP mới
            db.query(
                "UPDATE users SET otp_code=?, status='pending' WHERE email=?",
                [otp, email],
                async (err3) => {
                    if (err3) return res.status(500).json({ message: err3.message });

                    await sendEmail(email, "Mã OTP đăng ký", `Mã OTP của bạn là: ${otp}`);
                    return res.json({ message: "OTP đã được gửi đến email!" });
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

    console.log("VERIFY_OTP_REQUEST:", req.body);  // ⬅ log body nhận được

    db.query(
        "SELECT * FROM users WHERE email=? AND otp_code=?",
        [email, otp],
        (err, rows) => {
            if (err) {
                console.error("VERIFY_OTP_DB_ERROR:", err);
                return res.status(500).json({ success: false, message: err.message });
            }

            console.log("VERIFY_OTP_DB_RESULT rows.length =", rows.length);  // ⬅ log số dòng

            if (rows.length === 0)
                return res
                    .status(400)
                    .json({ success: false, message: "OTP sai!" });

            return res.json({ success: true, message: "OTP hợp lệ!" });
        }
    );
});


/* ============================================================
    3. FINISH REGISTER
   ============================================================ */

router.post("/finish-register", async (req, res) => {
    const { name, email, password } = req.body;

    if (!name || !email || !password)
        return res.status(400).json({ message: "Thiếu thông tin!" });

    const hashed = await bcrypt.hash(password, SALT_ROUNDS);

    db.query(
        "UPDATE users SET name=?, password=?, otp_code=NULL, status='active' WHERE email=?",
        [name, hashed, email],
        (err) => {
            if (err) return res.status(500).json({ message: err.message });

            return res.json({ message: "Tạo tài khoản thành công!" });
        }
    );
});

/* ============================================================
    4. LOGIN (Chỉ active)
   ============================================================ */

router.post("/login", (req, res) => {
    const { email, password } = req.body;

    db.query(
        "SELECT * FROM users WHERE email=?",
        [email],
        async (err, rows) => {
            if (err) return res.status(500).json({ message: err.message });

            if (rows.length === 0)
                return res.status(401).json({ message: "Sai email hoặc mật khẩu!" });

            const user = rows[0];

            if (user.status !== "active") {
                return res.status(403).json({ message: "Tài khoản chưa xác thực email!" });
            }

            const isMatch = await bcrypt.compare(password, user.password);
            if (!isMatch)
                return res.status(401).json({ message: "Sai email hoặc mật khẩu!" });

            delete user.password;

            return res.json({
                message: "Đăng nhập thành công!",
                user
            });
        }
    );
});

/* ============================================================
    5. FORGOT PASSWORD → SEND OTP
   ============================================================ */

router.post("/forgot-password", (req, res) => {
    const { email } = req.body;

    const otp = Math.floor(100000 + Math.random() * 900000);

    db.query(
        "UPDATE users SET otp_code=? WHERE email=? AND status='active'",
        [otp, email],
        async (err, result) => {
            if (err) return res.status(500).json({ message: err.message });

            if (result.affectedRows === 0)
                return res.status(404).json({ message: "Email không tồn tại hoặc chưa active!" });

            await sendEmail(email, "Mã OTP đặt lại mật khẩu", `Mã OTP của bạn là: ${otp}`);

            res.json({ success: true, message: "OTP đã được gửi đến email!" });
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

module.exports = router;
