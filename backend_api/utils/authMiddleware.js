const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET || 'dev_jwt_secret_change_me';
const db = require('../db');

/**
 * Middleware xác thực token JWT
 * Kiểm tra xem request có chứa token hợp lệ không
 */
const authenticateToken = async (req, res, next) => {
    // Lấy token từ header Authorization
    const authHeader = req.headers['authorization'];
    // Format: "Bearer <token>"
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) {
        return res.status(401).json({ message: "Không tìm thấy token xác thực!" });
    }

    try {
        // Xác thực token
        const user = jwt.verify(token, JWT_SECRET);
        
        // Kiểm tra user trong database để đảm bảo user vẫn tồn tại và active
        const { rows } = await db.query("SELECT id, role, status FROM users WHERE id=$1", [user.id]);
        
        if (!rows.length || rows[0].status !== 'active') {
             return res.status(403).json({ message: "Tài khoản không hợp lệ hoặc đã bị khóa!" });
        }
        
        // Gán thông tin user vào request để sử dụng ở các middleware/route sau
        req.user = rows[0]; 
        next();
    } catch (err) {
        console.error("JWT Verification Error:", err.message);
        return res.status(403).json({ message: "Token không hợp lệ hoặc đã hết hạn!" });
    }
};

/**
 * Middleware phân quyền (Authorization)
 * Chỉ cho phép các user có role nằm trong danh sách roles được phép truy cập
 * @param {Array} roles - Danh sách các role được phép (ví dụ: ['admin', 'staff'])
 */
const authorizeRole = (roles = []) => {
    // Nếu roles là string đơn lẻ, chuyển thành array
    if (typeof roles === 'string') {
        roles = [roles];
    }

    return (req, res, next) => {
        if (!req.user) {
             return res.status(401).json({ message: "Chưa xác thực người dùng!" });
        }

        if (roles.length && !roles.includes(req.user.role)) {
            // User không có quyền truy cập
            return res.status(403).json({ message: "Bạn không có quyền truy cập tài nguyên này!" });
        }

        next();
    };
};

module.exports = {
    authenticateToken,
    authorizeRole
};
