# Admin API Endpoints - Hướng Dẫn Sử Dụng

## Xác thực Admin
Tất cả các API admin yêu cầu header:
```
user-id: <user_id>
```
Người dùng này phải có `role = "admin"` trong database.

---

## 1. QUẢN LÝ TUYẾN XE

### Thêm tuyến xe mới
```
POST /api/admin/routes
Headers: user-id: <admin_id>

Body:
{
  "origin": "Hà Nội",
  "destination": "Hải Phòng",
  "distance_km": 120,
  "duration_min": 120
}

Response:
{
  "id": "3",
  "origin": "Hà Nội",
  "destination": "Hải Phòng",
  "distance_km": 120,
  "duration_min": 120,
  "created_at": "2025-11-21T..."
}
```

### Cập nhật tuyến xe
```
PUT /api/admin/routes/:id
Headers: user-id: <admin_id>

Body:
{
  "origin": "Hà Nội",
  "destination": "Hải Phòng",
  "distance_km": 125,
  "duration_min": 130
}
```

### Xóa tuyến xe
```
DELETE /api/admin/routes/:id
Headers: user-id: <admin_id>

Response:
{
  "message": "Xóa tuyến xe thành công"
}
```

---

## 2. QUẢN LÝ CHUYẾN XE

### Thêm chuyến xe mới
```
POST /api/admin/trips
Headers: user-id: <admin_id>

Body:
{
  "route_id": "3",
  "operator": "Xe Thiên Tây",
  "bus_type": "Giường nằm",
  "departure_time": "2025-11-21T10:00:00Z",
  "arrival_time": "2025-11-21T12:00:00Z",
  "price": 150000,
  "seats_total": 40
}
```

### Cập nhật chuyến xe (giá vé, giờ khởi hành, trạng thái...)
```
PUT /api/admin/trips/:id
Headers: user-id: <admin_id>

Body:
{
  "operator": "Xe Thiên Tây",
  "bus_type": "Giường nằm",
  "departure_time": "2025-11-21T10:30:00Z",
  "arrival_time": "2025-11-21T12:30:00Z",
  "price": 160000,
  "status": "active"
}

Status options: "active", "cancelled", "delayed", "on_time"
```

### Xóa chuyến xe
```
DELETE /api/admin/trips/:id
Headers: user-id: <admin_id>
```

---

## 3. QUẢN LÝ ĐẶT VÉ

### Lấy danh sách tất cả đặt vé
```
GET /api/admin/bookings?trip_id=1&user_id=5&status=confirmed&page=1&page_size=50
Headers: user-id: <admin_id>

Response:
[
  {
    "id": 1,
    "user_id": 5,
    "trip_id": 1,
    "seat_label": "A1",
    "price_paid": 150000,
    "status": "confirmed",
    "created_at": "2025-11-20T...",
    "name": "Nguyễn Văn A",
    "email": "user@example.com",
    "departure_time": "2025-11-21T10:00:00Z",
    "origin": "Hà Nội",
    "destination": "Hải Phòng"
  },
  ...
]
```

Query Parameters:
- `trip_id`: Lọc theo chuyến xe
- `user_id`: Lọc theo người dùng
- `status`: Lọc theo trạng thái (pending, confirmed, cancelled)
- `page`: Trang (mặc định: 1)
- `page_size`: Số bản ghi trên trang (mặc định: 50)

### Xác nhận đặt vé (pending → confirmed)
```
PUT /api/admin/bookings/:id/confirm
Headers: user-id: <admin_id>

Response:
{
  "id": 1,
  "status": "confirmed",
  "updated_at": "2025-11-21T..."
}
```

### Hủy đặt vé
```
PUT /api/admin/bookings/:id/cancel
Headers: user-id: <admin_id>

Response:
{
  "message": "Hủy đặt vé thành công"
}
```
Lưu ý: Hủy vé sẽ giải phóng ghế và tăng số ghế trống của chuyến.

---

## 4. BÁO CÁO DOANH THU

### Doanh thu theo tuyến xe
```
GET /api/admin/revenue/by-route
Headers: user-id: <admin_id>

Response:
[
  {
    "id": "1",
    "origin": "TP.HCM",
    "destination": "Đà Nẵng",
    "total_bookings": 120,
    "total_revenue": 18000000,
    "confirmed_bookings": 115
  },
  ...
]
```

### Doanh thu theo ngày
```
GET /api/admin/revenue/by-date?from_date=2025-11-01&to_date=2025-11-30
Headers: user-id: <admin_id>

Response:
[
  {
    "date": "2025-11-21",
    "total_bookings": 25,
    "total_revenue": 3750000,
    "confirmed_bookings": 24
  },
  ...
]
```

Query Parameters:
- `from_date`: Từ ngày (YYYY-MM-DD)
- `to_date`: Đến ngày (YYYY-MM-DD)

### Doanh thu theo tháng
```
GET /api/admin/revenue/by-month
Headers: user-id: <admin_id>

Response:
[
  {
    "month": "2025-11",
    "total_bookings": 500,
    "total_revenue": 75000000,
    "confirmed_bookings": 480
  },
  ...
]
```

### Doanh thu theo năm
```
GET /api/admin/revenue/by-year
Headers: user-id: <admin_id>

Response:
[
  {
    "year": 2025,
    "total_bookings": 5000,
    "total_revenue": 750000000,
    "confirmed_bookings": 4800
  }
]
```

---

## Ví dụ Requests trong Android (Retrofit)

```java
// Interface
@POST("/api/admin/trips")
Call<Trip> createTrip(@Header("user-id") int userId, @Body Trip trip);

@PUT("/api/admin/routes/{id}")
Call<Route> updateRoute(@Header("user-id") int userId, @Path("id") int routeId, @Body Route route);

@GET("/api/admin/bookings")
Call<List<Booking>> getBookings(@Header("user-id") int userId, @Query("status") String status);

@GET("/api/admin/revenue/by-route")
Call<List<RevenueStats>> getRevenueByRoute(@Header("user-id") int userId);
```

---

## Mã Lỗi

- `400`: Thiếu thông tin bắt buộc
- `401`: Chưa xác thực (missing user-id header)
- `403`: Không có quyền (không phải admin)
- `404`: Không tìm thấy tài nguyên
- `500`: Lỗi phía server

