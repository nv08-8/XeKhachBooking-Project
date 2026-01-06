const express = require("express");
const router = express.Router();
const db = require("../db");
const bcrypt = require("bcrypt");
const sendEmail = require("../utils/sendEmail");
const upload = require("../utils/uploadConfig");
const SALT_ROUNDS = 10;
const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET || 'dev_jwt_secret_change_me';
const { authenticateToken } = require("../utils/authMiddleware");

// Import express-validator
const { body, validationResult } = require('express-validator');

// Rate limiting cho auth routes để ngăn chặn brute-force/DDoS
const rateLimit = require('express-rate-limit');
const authLimiter = rateLimit({
    windowMs: 15 * 60 * 1000, // 15 phút
    max: 20, // Giới hạn 20 request mỗi IP cho các route auth
    message: { message: "Quá nhiều lần thử đăng nhập/đăng ký, vui lòng thử lại sau 15 phút." }
});

// ==========================================
// PUBLIC ROUTES (No Token Required)
// ==========================================

// Validate email rules
const emailValidation = [
    body('email')
        .isEmail().withMessage('Email không hợp lệ')
        .normalizeEmail()
];

router.post("/send-otp", authLimiter, emailValidation, async (req, res) => {
    // Check validation results
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
    }

    const { email } = req.body;

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

router.post("/verify-otp", authLimiter, emailValidation, async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
    }

    const { email, otp } = req.body;
    
    // Sanitize OTP just in case (though it should be number/string)
    if (!otp) return res.status(400).json({ message: "Thiếu OTP" });

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

// Validation rules for registration
const registerValidationRules = [
    body('name').trim().escape().notEmpty().withMessage('Tên không được để trống'),
    body('phone').trim().escape().matches(/^\d{9,11}$/).withMessage('Số điện thoại không hợp lệ'),
    body('email').isEmail().withMessage('Email không hợp lệ').normalizeEmail(),
    body('password').isLength({ min: 6 }).withMessage('Mật khẩu phải có ít nhất 6 ký tự')
];

router.post("/finish-register", authLimiter, registerValidationRules, async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
    }

    const { name, phone, email, password } = req.body;

    try {
        const { rows: phoneRows } = await db.query(
            "SELECT id FROM users WHERE phone=$1 AND status='active'",
            [phone]
        );
        if (phoneRows.length > 0) {
            return res.status(400).json({ message: "Số điện thoại đã được sử dụng!" });
        }

        const hashed = await bcrypt.hash(password, SALT_ROUNDS);
        const { rowCount } = await db.query(
            "UPDATE users SET name=$1, phone=$2, password=$3, otp_code=NULL, status='active' WHERE email=$4",
            [name, phone, hashed, email]
        );
        if (!rowCount) {
            return res.status(404).json({ message: "Không tìm thấy tài khoản (hoặc email chưa verify)." });
        }

        return res.json({ message: "Tạo tài khoản thành công!" });
    } catch (err) {
        console.error("Lỗi tạo tài khoản:", err);
        return res.status(500).json({ message: "Lỗi phía server." });
    }
});

// Validation for login
const loginValidation = [
    body('email').isEmail().withMessage('Email không hợp lệ').normalizeEmail(),
    body('password').notEmpty().withMessage('Mật khẩu không được để trống')
];

router.post("/login", authLimiter, loginValidation, async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ errors: errors.array() });
    }

    const { email, password } = req.body;
    try {
        const { rows } = await db.query(
            "SELECT * FROM users WHERE email=$1",
            [email]
        );
        if (!rows.length)
            return res.status(401).json({ message: "Sai email hoặc mật khẩu!" });

        const user = rows[0];

        // Check if account is deleted
        if (user.status === "deleted") {
            return res.status(403).json({ message: "Tài khoản đã bị xóa!" });
        }

        if (user.status !== "active") {
            return res.status(403).json({ message: "Tài khoản chưa xác thực email!" });
        }

        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch)
            return res.status(401).json({ message: "Sai email hoặc mật khẩu!" });

        // Remove sensitive fields
        delete user.password;
        delete user.otp_code; // Ensure OTP isn't sent back

        // Create JWT token (include minimal payload)
        const token = jwt.sign({ id: user.id }, JWT_SECRET, { expiresIn: '7d' });

        return res.json({
            message: "Đăng nhập thành công!",
            user,
            token
        });
    } catch (err) {
        console.error("Lỗi đăng nhập:", err);
        return res.status(500).json({ message: "Lỗi phía server." });
    }
});

router.post("/forgot-password", authLimiter, emailValidation, async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });

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

const resetPasswordValidation = [
    body('email').isEmail().normalizeEmail(),
    body('newPassword').isLength({ min: 6 }).withMessage('Mật khẩu mới phải có ít nhất 6 ký tự')
];

router.post("/reset-password", authLimiter, resetPasswordValidation, async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });

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

// ==========================================
// PROTECTED ROUTES (Token Required)
// ==========================================

// Get user profile - Secured
router.get("/user/:id", authenticateToken, async (req, res) => {
    const { id } = req.params;
    
    // Authorization: Check if user is requesting their own data or is admin
    // Note: req.user.id is number, params.id is string
    if (req.user.id != id && req.user.role !== 'admin') {
        return res.status(403).json({ message: "Bạn không có quyền xem thông tin này!" });
    }

    try {
        const { rows } = await db.query(
            "SELECT id, name, email, phone, dob, gender, status, role, avatar FROM users WHERE id=$1",
            [id]
        );
        if (!rows.length) return res.status(404).json({ message: "User not found" });
        res.json(rows[0]);
    } catch (err) {
        console.error("Lỗi lấy user:", err);
        return res.status(500).json({ message: "Lỗi phía server." });
    }
});

// Change password - Secured
router.post("/change-password", authenticateToken, [
    body('newPassword').isLength({ min: 6 }).withMessage('Mật khẩu mới quá ngắn')
], async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });

    const { email, currentPassword, newPassword } = req.body;
    
    // Allow using email from token if not provided in body (or force match)
    const userEmail = email || req.user.email;
    
    // Security check: ensure the email matches the logged-in user
    if (userEmail !== req.user.email && req.user.role !== 'admin') {
         return res.status(403).json({ success: false, message: "Bạn không có quyền đổi mật khẩu cho tài khoản này!" });
    }

    if (!currentPassword || !newPassword) {
        return res.status(400).json({ success: false, message: "Thiếu thông tin!" });
    }

    try {
        const { rows } = await db.query(
            "SELECT id, password FROM users WHERE email=$1 AND status='active'",
            [userEmail]
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

// Update user profile - Secured
// Validation for update
const updateProfileValidation = [
    body('name').optional().trim().escape(),
    body('phone').optional().trim().escape().matches(/^\d{9,11}$/).withMessage('Số điện thoại không hợp lệ'),
    body('gender').optional().trim().escape().isIn(['Nam', 'Nữ', 'Khác', 'Male', 'Female', 'Other']).withMessage('Giới tính không hợp lệ')
];

router.put("/user/:id", authenticateToken, updateProfileValidation, async (req, res) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ errors: errors.array() });

    const { id } = req.params;
    
    // Authorization
    if (req.user.id != id && req.user.role !== 'admin') {
        return res.status(403).json({ success: false, message: "Bạn không có quyền cập nhật thông tin này!" });
    }

    const { name, phone, dob, gender } = req.body;

    console.log("PUT /user/:id request:");
    console.log("  ID:", id);
    console.log("  Body:", { name, phone, dob, gender });

    if (!name && !phone && !dob && !gender) {
        console.log("  Error: No fields to update");
        return res.status(400).json({ success: false, message: "Không có thông tin để cập nhật!" });
    }

    const updateFields = [];
    const updateValues = [];

    if (name) {
        updateFields.push(`name=$${updateValues.length + 1}`);
        updateValues.push(name);
    }

    if (phone) {
        const phoneStr = phone.toString().trim();
        // Check duplicate phone if changed
        try {
            const { rows } = await db.query(
                "SELECT id FROM users WHERE phone=$1 AND id!=$2 AND status='active'",
                [phoneStr, id]
            );
            if (rows.length > 0) {
                return res.status(400).json({ success: false, message: "Số điện thoại đã được sử dụng!" });
            }
        } catch (err) {
            console.error("Lỗi kiểm tra SĐT:", err);
            return res.status(500).json({ success: false, message: "Lỗi phía server." });
        }

        updateFields.push(`phone=$${updateValues.length + 1}`);
        updateValues.push(phoneStr);
    }

    // Only try to update dob/gender if values are provided and not empty
    if (dob && dob.trim()) {
        updateFields.push(`dob=$${updateValues.length + 1}`);
        updateValues.push(dob.trim());
    }

    if (gender && gender.trim()) {
        updateFields.push(`gender=$${updateValues.length + 1}`);
        updateValues.push(gender.trim());
    }

    // Build id placeholder based on current updateValues length
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
        console.error("  Database error:", err.message);
        
        // If it's a column doesn't exist error, try without dob/gender
        if (err.message && err.message.includes("column") && err.message.includes("does not exist")) {
            // ... (Retry logic maintained same as before)
             // Rebuild query without dob/gender
             const updateFields2 = [];
             const updateValues2 = [];
 
             if (name) {
                 updateFields2.push(`name=$${updateValues2.length + 1}`);
                 updateValues2.push(name);
             }
 
             if (phone) {
                 updateFields2.push(`phone=$${updateValues2.length + 1}`);
                 updateValues2.push(phone.toString().trim());
             }
 
             if (updateFields2.length === 0) {
                 return res.status(400).json({ success: false, message: "Không có thông tin để cập nhật!" });
             }
 
             const idPlaceholder2 = `$${updateValues2.length + 1}`;
             updateValues2.push(id);
             const sql2 = `UPDATE users SET ${updateFields2.join(", ")} WHERE id=${idPlaceholder2} AND status='active'`;
 
             try {
                 const result2 = await db.query(sql2, updateValues2);
                 if (!result2.rowCount) {
                     return res.status(404).json({ success: false, message: "User not found" });
                 }
                 res.json({ success: true, message: "Cập nhật thông tin thành công!" });
             } catch (err2) {
                 console.error("Retry failed:", err2.message);
                 return res.status(500).json({ success: false, message: "Lỗi phía server: " + err2.message });
             }
        } else {
            return res.status(500).json({ success: false, message: "Lỗi phía server: " + err.message });
        }
    }
});

// Upload avatar endpoint - Secured
router.post("/upload-avatar", authenticateToken, upload.single('avatar'), async (req, res) => {
    try {
        console.log("uploadAvatar: Request received");
        
        if (!req.file) {
            console.error("uploadAvatar: No file received");
            return res.status(400).json({ success: false, message: "Chưa chọn ảnh!" });
        }

        // Use ID from authenticated token instead of trusting header/body
        const userId = req.user.id;
        console.log("uploadAvatar: userId (from token) =", userId);

        // Construct the public URL for the uploaded image (relative path only)
        const imageUrl = `/uploads/avatars/${req.file.filename}`;
        console.log("uploadAvatar: imageUrl =", imageUrl);

        // Update user avatar in database
        const updateResult = await db.query(
            "UPDATE users SET avatar=$1 WHERE id=$2",
            [imageUrl, userId]
        );

        if (updateResult.rowCount === 0) {
            console.error("uploadAvatar: Update failed - no rows affected");
            try {
                require('fs').unlinkSync(req.file.path);
            } catch (e) {
                console.error("uploadAvatar: Failed to delete file");
            }
            return res.status(500).json({ success: false, message: "Cập nhật database thất bại!" });
        }

        // Return success response with the image URL
        console.log("uploadAvatar: Upload successful");
        const successResponse = {
            success: true,
            message: "Cập nhật ảnh đại diện thành công!",
            data: {
                avatar: imageUrl,
                user_id: userId
            }
        };
        return res.json(successResponse);
    } catch (err) {
        console.error("uploadAvatar: Error caught:", err);

        // Try to delete file if it exists
        if (req.file) {
            try {
                require('fs').unlinkSync(req.file.path);
            } catch (e) {
                console.error("uploadAvatar: Failed to delete uploaded file:", e);
            }
        }
        return res.status(500).json({ success: false, message: "Lỗi phía server: " + err.message });
    }
});

module.exports = router;
