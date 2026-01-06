-- Kiểm tra dữ liệu feedbacks trong database
-- Chạy những query này để debug

-- 1. Kiểm tra tổng feedbacks
SELECT COUNT(*) as total_feedbacks FROM feedbacks;

-- 2. Xem chi tiết feedbacks
SELECT f.id, f.user_id, f.booking_id, f.rating, f.comment, f.created_at
FROM feedbacks f
ORDER BY f.created_at DESC
LIMIT 10;

-- 3. Kiểm tra bookings có trip_id không
SELECT b.id, b.trip_id, b.user_id, COUNT(f.id) as feedback_count
FROM bookings b
LEFT JOIN feedbacks f ON f.booking_id = b.id
WHERE b.trip_id IS NOT NULL
GROUP BY b.id
LIMIT 10;

-- 4. Kiểm tra trips có bookings không
SELECT t.id, t.operator, COUNT(b.id) as booking_count, COUNT(f.id) as feedback_count
FROM trips t
LEFT JOIN bookings b ON b.trip_id = t.id
LEFT JOIN feedbacks f ON f.booking_id = b.id
GROUP BY t.id
ORDER BY t.id
LIMIT 10;

-- 5. Test query giống như backend - lấy feedbacks của trip ID = 1
SELECT f.id as feedback_id, f.rating, f.comment, u.name as user_name, f.created_at
FROM feedbacks f
JOIN users u ON f.user_id = u.id
JOIN bookings b ON f.booking_id = b.id
JOIN trips t ON b.trip_id = t.id
WHERE t.id = 1
ORDER BY f.created_at DESC;

-- 6. Lấy xếp hạng operator của trip ID = 1
SELECT
    t.id as trip_id,
    t.operator,
    ROUND(AVG(f.rating)::numeric, 1) as average_rating,
    COUNT(DISTINCT f.id) as total_ratings
FROM trips t
LEFT JOIN bookings b ON b.trip_id = t.id
LEFT JOIN feedbacks f ON f.booking_id = b.id
WHERE t.id = 1
GROUP BY t.id, t.operator;

-- 7. Xem tất cả xếp hạng nhà xe
SELECT
    t.operator,
    ROUND(AVG(f.rating)::numeric, 1) as average_rating,
    COUNT(DISTINCT f.id) as total_ratings,
    COUNT(DISTINCT b.id) as total_bookings
FROM trips t
LEFT JOIN bookings b ON b.trip_id = t.id
LEFT JOIN feedbacks f ON f.booking_id = b.id
WHERE t.operator IS NOT NULL
GROUP BY t.operator
ORDER BY average_rating DESC;

