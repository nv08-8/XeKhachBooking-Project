-- Test SQL: Kiểm tra logic xếp hạng nhà xe từ các chuyến quá khứ

-- 1. Xem tất cả trips và feedbacks
SELECT
    t.id as trip_id,
    t.operator,
    t.departure_time,
    CASE
        WHEN t.departure_time < NOW() THEN 'QUÁ KHỮ ✓'
        ELSE 'TƯƠNG LAI →'
    END as status,
    COUNT(f.id) as feedback_count
FROM trips t
LEFT JOIN bookings b ON b.trip_id = t.id
LEFT JOIN feedbacks f ON f.booking_id = b.id
GROUP BY t.id
ORDER BY t.departure_time DESC;

-- 2. Xem xếp hạng từng nhà xe
SELECT
    t.operator,
    COUNT(DISTINCT t.id) as total_trips,
    COUNT(f.id) as total_feedbacks,
    ROUND(AVG(f.rating)::numeric, 2) as average_rating,
    MIN(f.rating) as min_rating,
    MAX(f.rating) as max_rating
FROM trips t
LEFT JOIN bookings b ON b.trip_id = t.id
LEFT JOIN feedbacks f ON f.booking_id = b.id
WHERE t.operator IS NOT NULL
GROUP BY t.operator
ORDER BY average_rating DESC;

-- 3. Chi tiết: Lấy xếp hạng operator khi xem trip ID = 1
-- (Giả sử trip 1 là "Nhà xe ABC")
SELECT
    t.id as trip_id,
    t.operator,
    t.departure_time,
    (SELECT COALESCE(ROUND(AVG(f2.rating)::numeric, 1), 0)
     FROM feedbacks f2
     JOIN bookings b2 ON f2.booking_id = b2.id
     JOIN trips t2 ON b2.trip_id = t2.id
     WHERE LOWER(TRIM(t2.operator)) = LOWER(TRIM(t.operator))) as operator_rating_from_all_trips,
    (SELECT COALESCE(COUNT(f2.id), 0)
     FROM feedbacks f2
     JOIN bookings b2 ON f2.booking_id = b2.id
     JOIN trips t2 ON b2.trip_id = t2.id
     WHERE LOWER(TRIM(t2.operator)) = LOWER(TRIM(t.operator))) as total_ratings_from_all_trips
FROM trips t
WHERE t.id = 1;

-- 4. Lấy danh sách feedbacks của nhà xe ABC (giống API reviewsQuery)
SELECT f.id as feedback_id, f.rating, f.comment, u.name as user_name, f.created_at,
       t.operator, t.id as trip_id
FROM feedbacks f
JOIN users u ON f.user_id = u.id
JOIN bookings b ON f.booking_id = b.id
JOIN trips t ON b.trip_id = t.id
WHERE LOWER(TRIM(t.operator)) = LOWER(TRIM('Nhà xe ABC'))
ORDER BY f.created_at DESC
LIMIT 20;

-- 5. Kiểm tra feedback count từng mức sao
SELECT
    t.operator,
    f.rating,
    COUNT(*) as count
FROM feedbacks f
JOIN bookings b ON f.booking_id = b.id
JOIN trips t ON b.trip_id = t.id
GROUP BY t.operator, f.rating
ORDER BY t.operator, f.rating DESC;

-- 6. So sánh: Xếp hạng riêng trip vs xếp hạng operator
SELECT
    t.id as trip_id,
    t.operator,
    -- Xếp hạng CHỈ TỪ CHUYẾN NÀY
    (SELECT COALESCE(ROUND(AVG(f3.rating)::numeric, 1), 0)
     FROM feedbacks f3
     JOIN bookings b3 ON f3.booking_id = b3.id
     WHERE b3.trip_id = t.id) as rating_this_trip_only,
    -- Xếp hạng TỪ TẤT CẢ CHUYẾN CỦA OPERATOR
    (SELECT COALESCE(ROUND(AVG(f2.rating)::numeric, 1), 0)
     FROM feedbacks f2
     JOIN bookings b2 ON f2.booking_id = b2.id
     JOIN trips t2 ON b2.trip_id = t2.id
     WHERE LOWER(TRIM(t2.operator)) = LOWER(TRIM(t.operator))) as rating_all_trips_of_operator
FROM trips t
WHERE t.operator IS NOT NULL
ORDER BY t.id;

