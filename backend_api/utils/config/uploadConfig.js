const multer = require('multer');
const path = require('path');
const fs = require('fs');

// Create uploads directory if it doesn't exist
const uploadsDir = path.join(__dirname, '..', 'uploads', 'avatars');
if (!fs.existsSync(uploadsDir)) {
    fs.mkdirSync(uploadsDir, { recursive: true });
}

// Configure storage for multer
const storage = multer.diskStorage({
    destination: function (req, file, cb) {
        cb(null, uploadsDir);
    },
    filename: function (req, file, cb) {
        // Create unique filename with user id and timestamp
        const userId = req.headers['user-id'] || req.body.user_id || 'unknown';
        const ext = path.extname(file.originalname);
        const filename = `user_${userId}_${Date.now()}${ext}`;
        cb(null, filename);
    }
});

// Filter for image files only
const fileFilter = (req, file, cb) => {
    // Allowed MIME types - JPG can be image/jpeg, image/jpg, or image/x-jpg
    const allowedMimes = [
        'image/jpeg',
        'image/jpg',
        'image/x-jpg',
        'image/png',
        'image/webp'
    ];

    if (allowedMimes.includes(file.mimetype)) {
        cb(null, true);
    } else {
        cb(new Error('Chỉ chấp nhận các định dạng ảnh (JPG, JPEG, PNG, WebP). Loại nhận được: ' + file.mimetype));
    }
};

// Configure multer
const upload = multer({
    storage: storage,
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB max file size
    },
    fileFilter: fileFilter
});

module.exports = upload;

