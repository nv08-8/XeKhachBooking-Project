# 🔧 FIX: Thiếu bước nhập thông tin hành khách

**Ngày**: 27/12/2025  
**Trạng thái**: ✅ ĐÃ SỬA

---

## 📋 VẤN ĐỀ

User báo: **Không thấy bước nhập thông tin hành khách** khi đặt vé

**Flow hiện tại (SAI)**:
```
Chọn chuyến → Chọn ghế → Chọn điểm đón → Chọn điểm trả → ❌ NHẢY THẲNG SANG THANH TOÁN
```

**Vấn đề**: Bỏ qua bước nhập thông tin hành khách (họ tên, SĐT, email)

---

## 🔍 NGUYÊN NHÂN

Trong `SelectDropoffPointActivity.java` dòng 89, sau khi chọn điểm trả, code tạo Intent đến **PaymentActivity** thay vì **PassengerInfoActivity**:

```java
// ❌ SAI: Nhảy thẳng sang PaymentActivity
Intent intent = new Intent(this, PaymentActivity.class);
intent.putExtra("trip", trip);
// ...
startActivity(intent);
```

---

## ✅ GIẢI PHÁP

### Fix 1: SelectDropoffPointActivity → PassengerInfoActivity

**File**: `SelectDropoffPointActivity.java` (line ~82)

**Trước**:
```java
Intent intent = new Intent(this, PaymentActivity.class);
```

**Sau**:
```java
// ✅ Go to PassengerInfoActivity instead
Intent intent = new Intent(this, PassengerInfoActivity.class);
```

### Fix 2: Update PassengerInfoActivity để nhận Trip object

**File**: `PassengerInfoActivity.java`

**Vấn đề**: Activity này đang nhận `trip_id` (int) nhưng SelectDropoffPointActivity gửi `trip` (Trip object)

**Giải pháp**:

1. **Update fields** (line ~20):
```java
// Trước
private int tripId;
private int amount;
private String origin, destination, operator;

// Sau
private Trip trip;
private int pickupStopId;
private String pickupStopName;
private int dropoffStopId;
private String dropoffStopName;
```

2. **Update onCreate** (line ~58):
```java
// Trước
tripId = intent.getIntExtra("trip_id", 0);
amount = intent.getIntExtra("amount", 0);
origin = intent.getStringExtra("origin");

// Sau
trip = intent.getParcelableExtra("trip");
seatLabels = intent.getStringArrayListExtra("seat_labels");
pickupStopId = intent.getIntExtra("pickup_stop_id", -1);
pickupStopName = intent.getStringExtra("pickup_stop_name");
dropoffStopId = intent.getIntExtra("dropoff_stop_id", -1);
dropoffStopName = intent.getStringExtra("dropoff_stop_name");
```

3. **Update navigateToPayment** (line ~119):
```java
// Trước
paymentIntent.putExtra("trip_id", tripId);
paymentIntent.putExtra("amount", amount);
paymentIntent.putExtra("origin", origin);

// Sau
paymentIntent.putExtra("trip", trip);
paymentIntent.putStringArrayListExtra("seat_labels", seatLabels);
paymentIntent.putExtra("pickup_stop_id", pickupStopId);
paymentIntent.putExtra("pickup_stop_name", pickupStopName);
paymentIntent.putExtra("dropoff_stop_id", dropoffStopId);
paymentIntent.putExtra("dropoff_stop_name", dropoffStopName);
```

---

## 🎯 KẾT QUẢ

### Flow sau khi sửa ✅

```
1. TripListActivity: Chọn chuyến
   ↓
2. SeatSelectionActivity: Chọn ghế
   ↓
3. SelectPickupPointActivity: Chọn điểm đón
   ↓
4. SelectDropoffPointActivity: Chọn điểm trả
   ↓
5. ✅ PassengerInfoActivity: Nhập thông tin (Họ tên, SĐT, Email)
   ↓
6. PaymentActivity: Chọn phương thức thanh toán
```

---

## 🧪 CÁCH TEST

### Test Case: Đặt vé end-to-end

1. **Mở app** và đăng nhập
2. **Tìm chuyến**: Cần Thơ → TP.HCM
3. **Chọn chuyến** bất kỳ
4. **Chọn ghế**: Ví dụ A2
5. **Chọn điểm đón**: Ví dụ "BX Cần Thơ"
6. **Chọn điểm trả**: Ví dụ "BX Miền Tây"
7. **Bấm "Tiếp tục"**

**Kỳ vọng**: ✅ **Màn hình "Thông tin hành khách" xuất hiện**

Với các trường:
- ✅ Họ và tên (tự động điền từ profile)
- ✅ Số điện thoại (tự động điền)
- ✅ Email (tự động điền)
- ✅ Nút "Tiếp tục"

8. **Nhập/verify thông tin**
9. **Bấm "Tiếp tục"**

**Kỳ vọng**: ✅ Chuyển sang màn hình "Thanh toán"

---

## 📊 DATA FLOW

### Trước khi sửa ❌
```
SelectDropoffPointActivity:
  trip: Trip object
  seatLabels: ["A2"]
  pickupStopId: 29
  dropoffStopId: 32
  ↓
  Intent → PaymentActivity (TRỰC TIẾP) ❌
  ↓
  PaymentActivity nhận data
  BUT: fullName, email, phoneNumber = NULL ❌
```

### Sau khi sửa ✅
```
SelectDropoffPointActivity:
  trip: Trip object
  seatLabels: ["A2"]
  pickupStopId: 29
  dropoffStopId: 32
  ↓
  Intent → PassengerInfoActivity ✅
  ↓
PassengerInfoActivity:
  User nhập: fullName, phoneNumber, email
  ↓
  Intent → PaymentActivity ✅
  ↓
PaymentActivity:
  Nhận đầy đủ: trip, seats, pickup, dropoff, passenger info ✅
```

---

## 📝 FILES ĐÃ THAY ĐỔI

| File | Changes | Lines |
|------|---------|-------|
| `SelectDropoffPointActivity.java` | Change Intent target from PaymentActivity to PassengerInfoActivity | 1 line |
| `PassengerInfoActivity.java` | Update to receive Trip object instead of trip_id; Add pickup/dropoff fields | ~30 lines |

---

## ⚠️ LƯU Ý

### Tương thích với old flow

Nếu có flow cũ gọi trực tiếp PaymentActivity (ví dụ: từ MyBookings để tiếp tục thanh toán), nó vẫn hoạt động vì:
- PaymentActivity đã có logic nhận `is_pending_payment`
- Khi pending payment, nó fetch passenger info từ booking details

### Pre-fill thông tin

PassengerInfoActivity tự động điền:
```java
etFullName.setText(sessionManager.getUserName());
etEmail.setText(sessionManager.getUserEmail());
etPhoneNumber.setText(sessionManager.getUserPhone());
```

User chỉ cần verify hoặc sửa nếu cần.

---

## 🎓 WHY THIS MATTERS

### 1. UX Better
- User thấy rõ các bước: Chọn ghế → Điểm đón/trả → **Thông tin** → Thanh toán
- Không bị "nhảy cóc" thiếu bước

### 2. Data Integrity
- Passenger info được nhập và validate ngay
- Không phụ thuộc vào session (có thể là guest)

### 3. Consistency
- Tất cả bookings đều có passenger info đầy đủ
- Không có trường hợp passenger_name/phone/email = NULL

---

## 🚀 DEPLOYMENT

### Build
```bash
cd C:\Users\Admin\Documents\GitHub\XeKhachBooking-Project
.\gradlew clean
.\gradlew :app:assembleDebug
```

### Install
```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## ✅ CHECKLIST

Test sau khi deploy:

- [ ] Chọn chuyến → Chọn ghế → Chọn điểm đón → Chọn điểm trả
- [ ] **Màn hình "Thông tin hành khách" xuất hiện**
- [ ] Các trường được pre-fill từ profile
- [ ] Validate: bắt buộc nhập họ tên, SĐT (10 số), email hợp lệ
- [ ] Bấm "Tiếp tục" → Chuyển sang màn hình thanh toán
- [ ] Trong thanh toán, thông tin hành khách hiển thị đúng
- [ ] Tạo booking thành công với đầy đủ passenger info

---

## 🎉 DONE

**Status**: ✅ **RESOLVED**

Flow đặt vé giờ đã **HOÀN CHỈNH** với đầy đủ các bước:
1. ✅ Chọn chuyến
2. ✅ Chọn ghế
3. ✅ Chọn điểm đón
4. ✅ Chọn điểm trả
5. ✅ **Nhập thông tin hành khách** (FIXED!)
6. ✅ Chọn phương thức thanh toán

---

**Người thực hiện**: GitHub Copilot  
**Ngày hoàn thành**: 27/12/2025  
**Status**: ✅ Resolved & Ready for Testing

