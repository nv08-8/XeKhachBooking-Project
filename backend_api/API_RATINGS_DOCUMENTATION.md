# API Documentation - Bus Operator Ratings

## Tổng quan
Hệ thống lấy trung bình đánh giá từ người dùng cho từng nhà xe (operator). Thông tin này được hiển thị khi người dùng xem danh sách chuyến xe hoặc chi tiết chuyến xe.

---

## Endpoints

### 1. Lấy danh sách tất cả nhà xe với xếp hạng

**Endpoint:** `GET /api/operators/ratings`

**Mô tả:** Lấy danh sách tất cả nhà xe với trung bình đánh giá, sắp xếp theo xếp hạng cao nhất.

**Response:**
```json
[
  {
    "operator": "Nhà xe ABC",
    "total_ratings": 45,
    "average_rating": 4.5,
    "latest_rating_date": "2026-01-06T10:30:00Z"
  },
  {
    "operator": "Nhà xe XYZ",
    "total_ratings": 28,
    "average_rating": 4.2,
    "latest_rating_date": "2026-01-05T15:45:00Z"
  }
]
```

---

### 2. Lấy chi tiết đánh giá của một nhà xe cụ thể

**Endpoint:** `GET /api/operators/:operator/ratings`

**Mô tả:** Lấy thông tin chi tiết về đánh giá của một nhà xe, bao gồm danh sách các feedback.

**Parameters:**
- `operator` (URL parameter): Tên nhà xe (ví dụ: "Nhà xe ABC")

**Response:**
```json
{
  "operator": "Nhà xe ABC",
  "total_ratings": 45,
  "average_rating": 4.5,
  "min_rating": 3,
  "max_rating": 5,
  "positive_count": 38,
  "neutral_count": 5,
  "negative_count": 2,
  "feedbacks": [
    {
      "feedback_id": 1,
      "rating": 5,
      "comment": "Tài xế lịch sự, xe sạch sẽ",
      "user_name": "Nguyễn Văn A",
      "feedback_date": "2026-01-06T10:30:00Z",
      "trip_date": "2026-01-05T08:00:00Z",
      "trip_origin": "Hà Nội",
      "trip_destination": "Hải Phòng"
    },
    {
      "feedback_id": 2,
      "rating": 4,
      "comment": "Chuyến xe tốt, chỉ hơi trễ một chút",
      "user_name": "Trần Thị B",
      "feedback_date": "2026-01-05T18:00:00Z",
      "trip_date": "2026-01-05T08:00:00Z",
      "trip_origin": "Hà Nội",
      "trip_destination": "Hải Phòng"
    }
  ]
}
```

---

### 3. Danh sách chuyến xe (hiện thông tin xếp hạng)

**Endpoint:** `GET /api/trips`

**Mô tả:** Lấy danh sách chuyến xe với thông tin xếp hạng của nhà xe đã được tính toán.

**Response:**
```json
[
  {
    "id": 1,
    "route_id": 5,
    "operator": "Nhà xe ABC",
    "bus_type": "Limousine",
    "departure_time": "2026-01-10T08:00:00",
    "arrival_time": "2026-01-10T11:30:00",
    "price": 250000,
    "seats_total": 36,
    "seats_available": 15,
    "status": "scheduled",
    "origin": "Hà Nội",
    "destination": "Hải Phòng",
    "distance_km": 120,
    "duration_min": 210,
    "driver_name": "Nguyễn Văn C",
    "operator_rating": 4.5,
    "total_ratings": 45,
    "bus_image_url": "https://example.com/bus.jpg"
  }
]
```

---

### 4. Chi tiết chuyến xe (hiện thông tin xếp hạng)

**Endpoint:** `GET /api/trips/:id`

**Mô tả:** Lấy chi tiết một chuyến xe với thông tin xếp hạng của nhà xe.

**Response:**
```json
{
  "id": 1,
  "route_id": 5,
  "operator": "Nhà xe ABC",
  "bus_type": "Limousine",
  "departure_time": "2026-01-10T08:00:00",
  "arrival_time": "2026-01-10T11:30:00",
  "price": 250000,
  "seats_total": 36,
  "seats_available": 15,
  "status": "scheduled",
  "origin": "Hà Nội",
  "destination": "Hải Phòng",
  "distance_km": 120,
  "duration_min": 210,
  "driver_name": "Nguyễn Văn C",
  "operator_rating": 4.5,
  "total_ratings": 45,
  "bus_image_url": "https://example.com/bus.jpg",
  "amenities": {
    "wifi": true,
    "water": true,
    "ac": true,
    "wc": true,
    "tv": true,
    "charging": true
  },
  "timeline": [...]
}
```

---

## Các Endpoints để tạo/quản lý Feedback

### 5. Gửi đánh giá cho một chuyến xe

**Endpoint:** `POST /api/feedbacks`

**Mô tả:** Người dùng gửi đánh giá và bình luận cho một chuyến xe đã đi.

**Request Body:**
```json
{
  "user_id": 1,
  "booking_id": 10,
  "rating": 5,
  "comment": "Tài xế lịch sự, xe sạch sẽ"
}
```

**Response:**
```json
{
  "id": 1,
  "user_id": 1,
  "booking_id": 10,
  "rating": 5,
  "comment": "Tài xế lịch sự, xe sạch sẽ",
  "created_at": "2026-01-06T10:30:00Z"
}
```

---

### 6. Lấy danh sách feedback đang chờ (chuyến xe đã hoàn thành nhưng chưa được đánh giá)

**Endpoint:** `GET /api/feedbacks/pending`

**Parameters:**
- `user_id` (query parameter): ID người dùng

**Response:**
```json
[
  {
    "booking_id": 10,
    "trip_id": 5,
    "total_amount": 250000,
    "booking_date": "2026-01-04T10:00:00Z",
    "departure_time": "2026-01-05T08:00:00Z",
    "arrival_time": "2026-01-05T11:30:00Z",
    "operator": "Nhà xe ABC",
    "bus_type": "Limousine",
    "origin": "Hà Nội",
    "destination": "Hải Phòng",
    "price": "250000 VND",
    "duration": "3 giờ 30 phút",
    "date": "Mon, 05 Jan",
    "seat_labels": ["A1", "A2"]
  }
]
```

---

### 7. Lấy danh sách feedback đã gửi

**Endpoint:** `GET /api/feedbacks/reviewed`

**Parameters:**
- `user_id` (query parameter): ID người dùng

**Response:**
```json
[
  {
    "feedback_id": 1,
    "rating": 5,
    "comment": "Tài xế lịch sự, xe sạch sẽ",
    "feedback_date": "2026-01-06T10:30:00Z",
    "booking_id": 10,
    "total_amount": 250000,
    "departure_time": "2026-01-05T08:00:00Z",
    "arrival_time": "2026-01-05T11:30:00Z",
    "operator": "Nhà xe ABC",
    "bus_type": "Limousine",
    "origin": "Hà Nội",
    "destination": "Hải Phòng",
    "user_name": "Nguyễn Văn A",
    "price": "250000 VND",
    "duration": "3 giờ 30 phút",
    "date": "Mon, 05 Jan"
  }
]
```

---

## Cách sử dụng trong Frontend

### 1. Hiển thị xếp hạng nhà xe trong danh sách chuyến xe

```javascript
// Lấy danh sách chuyến xe
const response = await fetch('/api/trips');
const trips = await response.json();

// Hiển thị xếp hạng
trips.forEach(trip => {
  console.log(`${trip.operator}: ${trip.operator_rating}⭐ (${trip.total_ratings} đánh giá)`);
});
```

### 2. Hiển thị chi tiết đánh giá nhà xe

```javascript
// Lấy chi tiết đánh giá
const operator = "Nhà xe ABC";
const response = await fetch(`/api/operators/${encodeURIComponent(operator)}/ratings`);
const data = await response.json();

console.log(`Trung bình: ${data.average_rating}⭐`);
console.log(`Tổng đánh giá: ${data.total_ratings}`);
console.log(`Đánh giá tích cực: ${data.positive_count}`);
console.log(`Đánh giá trung bình: ${data.neutral_count}`);
console.log(`Đánh giá tiêu cực: ${data.negative_count}`);
```

### 3. Gửi đánh giá sau khi hoàn thành chuyến xe

```javascript
const feedback = {
  user_id: 1,
  booking_id: 10,
  rating: 5,
  comment: "Tài xế lịch sự, xe sạch sẽ"
};

const response = await fetch('/api/feedbacks', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(feedback)
});

const result = await response.json();
console.log('Feedback submitted:', result);
```

---

## Lưu ý quan trọng

1. **Rating Scale**: Đánh giá từ 1-5 sao
2. **One feedback per booking**: Mỗi booking chỉ có thể có một feedback
3. **Operator name case-insensitive**: Tên nhà xe không phân biệt hoa thường
4. **Aggregate by operator**: Xếp hạng được tính dựa trên tất cả chuyến xe của nhà xe, không phải chuyến xe cụ thể

---

## Dữ liệu trong cơ sở dữ liệu

### Bảng feedbacks
```sql
CREATE TABLE feedbacks (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    booking_id INTEGER NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(booking_id)
);
```

### Các trường liên quan:
- `bookings.trip_id`: Liên kết tới chuyến xe
- `trips.operator`: Tên nhà xe

---

## Ví dụ thực tế

### Quy trình đánh giá nhà xe

1. **Sau khi hoàn thành chuyến**: Gọi `GET /api/feedbacks/pending?user_id=1` để lấy danh sách chuyến chưa đánh giá
2. **Gửi đánh giá**: Gọi `POST /api/feedbacks` với rating (1-5) và comment
3. **Hiển thị feedback**: Gọi `GET /api/feedbacks/reviewed?user_id=1` để xem các feedback đã gửi
4. **Xem xếp hạng nhà xe**: Gọi `GET /api/operators/ratings` để xem xếp hạng của tất cả nhà xe

---

## Công thức tính toán

```
average_rating = SUM(rating) / COUNT(rating)
positive_count = SUM(CASE WHEN rating >= 4 THEN 1 ELSE 0 END)
neutral_count = SUM(CASE WHEN rating = 3 THEN 1 ELSE 0 END)
negative_count = SUM(CASE WHEN rating < 3 THEN 1 ELSE 0 END)
```

---

Ngày cập nhật: 2026-01-06

