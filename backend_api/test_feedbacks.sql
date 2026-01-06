-- Test data: Tạo feedback test để kiểm tra
-- Chạy câu lệnh này trước khi test API

-- 1. Kiểm tra xem có bookings và trips không
SELECT COUNT(*) as total_bookings FROM bookings;
SELECT COUNT(*) as total_trips FROM trips;

-- 2. Lấy một trip_id để test
SELECT t.id, t.operator, COUNT(b.id) as booking_count
FROM trips t
LEFT JOIN bookings b ON b.trip_id = t.id
GROUP BY t.id
LIMIT 5;

-- 3. Kiểm tra xem có feedbacks không
SELECT f.id, f.rating, f.comment, u.name, t.operator
FROM feedbacks f
JOIN users u ON f.user_id = u.id
JOIN bookings b ON f.booking_id = b.id
JOIN trips t ON b.trip_id = t.id
LIMIT 10;

-- 4. Nếu không có feedbacks, tạo test data
-- Thay đổi booking_id và user_id tùy theo dữ liệu của bạn
INSERT INTO feedbacks (user_id, booking_id, rating, comment)
VALUES
  (1, 1, 5, 'Tuyệt vời! Tài xế lịch sự, xe sạch sẽ'),
  (2, 2, 4, 'Chuyến xe tốt, chỉ hơi trễ một chút'),
  (3, 3, 5, 'Dịch vụ rất tốt'),
  (1, 4, 3, 'Bình thường'),
  (2, 5, 5, 'Xuất sắc!')
ON CONFLICT (booking_id) DO NOTHING;

-- 5. Kiểm tra lại feedbacks sau khi insert
SELECT f.id, f.rating, f.comment, u.name, t.operator, b.trip_id
FROM feedbacks f
JOIN users u ON f.user_id = u.id
JOIN bookings b ON f.booking_id = b.id
JOIN trips t ON b.trip_id = t.id
ORDER BY f.created_at DESC
LIMIT 20;

-- 6. Kiểm tra xếp hạng nhà xe
SELECT
    t.operator,
    COUNT(f.id) as total_ratings,
    ROUND(AVG(f.rating)::numeric, 1) as average_rating
FROM feedbacks f
JOIN bookings b ON f.booking_id = b.id
JOIN trips t ON b.trip_id = t.id
GROUP BY t.operator
ORDER BY average_rating DESC;

