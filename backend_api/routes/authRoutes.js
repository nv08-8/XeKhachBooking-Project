const express = require("express");
const router = express.Router();
const db = require("../db");
const bcrypt = require("bcrypt");
const sendEmail = require("../utils/sendEmail");
const SALT_ROUNDS = 10;

router.post("/send-otp", async (req, res) => {
    const { email } = req.body;

    if (!email)
        return res.status(400).json({ message: "Thiếu email!" });

    const otp = Math.floor(100000 + Math.random() * 900000);

    try {
        const { rows } = await db.query("SELECT id, status FROM users WHERE email=$1", [email]);

        if (!rows.length) {
            await db.query(
                "INSERT INTO users (email, otp_code, status) VALUES ($1, $2, 'pending')",
                [email, otp]
            );
        } else {
            const user = rows[0];
            if (user.status === "active") {
                return res.status(400).json({ message: "Email đã tồn tại!" });
            }
            await db.query(
                "UPDATE users SET otp_code=$1, status='pending' WHERE email=$2",
                [otp, email]
            );
        }

        res.json({ message: "OTP đã được gửi đến email!" });
        sendEmail(email, "Mã OTP đăng ký", `Mã OTP của bạn là: ${otp}`)
            .catch(emailErr => console.error("Gửi email thất bại:", emailErr));
    } catch (err) {
        console.error("Lỗi gửi OTP:", err);
        res.status(500).json({ message: "Lỗi phía server." });
    }
});

router.post("/verify-otp", async (req, res) => {
    const { email, otp } = req.body;
    try {
        const { rows } = await db.query(
            "SELECT id FROM users WHERE email=$1 AND otp_code=$2",
            [email, otp]
        );
        if (!rows.length) {
            return res.status(400).json({ success: false, message: "OTP sai!" });
        }
        return res.json({ success: true, message: "OTP hợp lệ!" });
    } catch (err) {
        console.error("Lỗi verify OTP:", err);
        return res.status(500).json({ success: false, message: "Lỗi phía server." });
    }
});

router.post("/finish-register", async (req, res) => {
    const { name, phone, email, password } = req.body;
    if (!name || !phone || !email || !password)
        return res.status(400).json({ message: "Thiếu thông tin!" });

    const phoneStr = phone.toString().trim();
    if (!/^\d{9,11}$/.test(phoneStr)) {
        return res.status(400).json({ message: "Số điện thoại không hợp lệ." });
    }

    try {
        const { rows: phoneRows } = await db.query(
            "SELECT id FROM users WHERE sdt=$1 AND status='active'",
            [phoneStr]
        );
        if (phoneRows.length > 0) {
            return res.status(400).json({ message: "Số điện thoại đã được sử dụng!" });
        }

        const hashed = await bcrypt.hash(password, SALT_ROUNDS);
        const { rowCount } = await db.query(
            "UPDATE users SET name=$1, sdt=$2, password=$3, otp_code=NULL, status='active' WHERE email=$4",
            [name, phoneStr, hashed, email]
        );
        if (!rowCount) {
            return res.status(404).json({ message: "Không tìm thấy tài khoản." });
        }

        return res.json({ message: "Tạo tài khoản thành công!" });
    } catch (err) {
        console.error("Lỗi tạo tài khoản:", err);
        return res.status(500).json({ message: "Lỗi phía server." });
    }
});

router.post("/login", async (req, res) => {
    const { email, password } = req.body;
    try {
        const { rows } = await db.query(
            "SELECT * FROM users WHERE email=$1",
            [email]
        );
        if (!rows.length)
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
    } catch (err) {
        console.error("Lỗi đăng nhập:", err);
        return res.status(500).json({ message: "Lỗi phía server." });
    }
});

router.post("/forgot-password", async (req, res) => {
    const { email } = req.body;
    const otp = Math.floor(100000 + Math.random() * 900000);
    try {
        const result = await db.query(
            "UPDATE users SET otp_code=$1 WHERE email=$2 AND status='active'",
            [otp, email]
        );
        if (!result.rowCount)
            return res.status(404).json({ message: "Email không tồn tại hoặc chưa active!" });

        res.json({ success: true, message: "OTP đã được gửi đến email!" });
        sendEmail(email, "Mã OTP đặt lại mật khẩu", `Mã OTP của bạn là: ${otp}`)
            .catch(emailErr => console.error("Gửi email quên mật khẩu thất bại:", emailErr));
    } catch (err) {
        console.error("Lỗi forgot password:", err);
        return res.status(500).json({ message: "Lỗi phía server." });
    }
});

router.post("/reset-password", async (req, res) => {
    const { email, newPassword } = req.body;
    try {
        const hashed = await bcrypt.hash(newPassword, SALT_ROUNDS);
        const { rowCount } = await db.query(
            "UPDATE users SET password=$1, otp_code=NULL WHERE email=$2 AND status='active'",
            [hashed, email]
        );
        if (!rowCount) {
            return res.status(404).json({ message: "Email không tồn tại hoặc chưa active!" });
        }
        res.json({ success: true, message: "Đặt lại mật khẩu thành công!" });
    } catch (err) {
        console.error("Lỗi reset mật khẩu:", err);
        return res.status(500).json({ message: "Lỗi phía server." });
    }
});

router.get("/user/:id", async (req, res) => {
    const { id } = req.params;
    try {
        const { rows } = await db.query(
            "SELECT id, name, email, sdt, status FROM users WHERE id=$1 AND status='active'",
            [id]
        );
        if (!rows.length) return res.status(404).json({ message: "User not found" });
        res.json(rows[0]);
    } catch (err) {
        console.error("Lỗi lấy user:", err);
        return res.status(500).json({ message: "Lỗi phía server." });
    }
});

router.post("/change-password", async (req, res) => {
    const { email, currentPassword, newPassword } = req.body;
    if (!email || !currentPassword || !newPassword) {
        return res.status(400).json({ success: false, message: "Thiếu thông tin!" });
    }

    try {
        const { rows } = await db.query(
            "SELECT id, password FROM users WHERE email=$1 AND status='active'",
            [email]
        );
        if (!rows.length) return res.status(404).json({ success: false, message: "User not found" });

        const user = rows[0];
        const isMatch = await bcrypt.compare(currentPassword, user.password);
        if (!isMatch) {
            return res.status(401).json({ success: false, message: "Mật khẩu hiện tại không đúng!" });
        }

        const hashed = await bcrypt.hash(newPassword, SALT_ROUNDS);
        await db.query(
            "UPDATE users SET password=$1 WHERE id=$2",
            [hashed, user.id]
        );
        res.json({ success: true, message: "Đổi mật khẩu thành công!" });
    } catch (err) {
        console.error("Lỗi đổi mật khẩu:", err);
        return res.status(500).json({ success: false, message: "Lỗi phía server." });
    }
});

router.put("/user/:id", async (req, res) => {
    const { id } = req.params;
    const { name, sdt } = req.body;
    if (!name && !sdt) {
        return res.status(400).json({ success: false, message: "Không có thông tin để cập nhật!" });
    }

    const updateFields = [];
    const updateValues = [];

    if (name) {
        updateFields.push(`name=$${updateValues.length + 1}`);
        updateValues.push(name);
    }

    if (sdt) {
        const phoneStr = sdt.toString().trim();
        if (!/^\d{9,11}$/.test(phoneStr)) {
            return res.status(400).json({ success: false, message: "Số điện thoại không hợp lệ." });
        }

        try {
            const { rows } = await db.query(
                "SELECT id FROM users WHERE sdt=$1 AND id!=$2 AND status='active'",
                [phoneStr, id]
            );
            if (rows.length > 0) {
                return res.status(400).json({ success: false, message: "Số điện thoại đã được sử dụng!" });
            }
        } catch (err) {
            console.error("Lỗi kiểm tra SĐT:", err);
            return res.status(500).json({ success: false, message: "Lỗi phía server." });
        }

        updateFields.push(`sdt=$${updateValues.length + 1}`);
        updateValues.push(phoneStr);
    }

    const idPlaceholder = `$${updateValues.length + 1}`;
    updateValues.push(id);
    const sql = `UPDATE users SET ${updateFields.join(", ")} WHERE id=${idPlaceholder} AND status='active'`;

    try {
        const result = await db.query(sql, updateValues);
        if (!result.rowCount) {
            return res.status(404).json({ success: false, message: "User not found" });
        }
        res.json({ success: true, message: "Cập nhật thông tin thành công!" });
    } catch (err) {
        console.error("Lỗi cập nhật user:", err);
        return res.status(500).json({ success: false, message: "Lỗi phía server." });
    }
});

module.exports = router;
