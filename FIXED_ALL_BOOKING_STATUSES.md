# ✅ FIXED: Layout hỗ trợ TẤT CẢ trạng thái booking

## 🎯 Vấn đề đã fix:

Bạn đúng! Layout cũ chỉ phù hợp với **vé pending**. Bây giờ đã fix để support:

1. ✅ **Vé đang chờ (Pending)** - Hiển thị payment method + countdown + buttons
2. ✅ **Vé đã thanh toán (Confirmed)** - Hiển thị QR code + nút hủy vé
3. ✅ **Vé đã đi (Completed)** - Hiển thị QR code, không có nút
4. ✅ **Vé đã hủy (Cancelled)** - Chỉ hiển thị thông tin cơ bản

---

## 📊 Layout Structure (đã fix):

```
┌─────────────────────────────────────┐
│  ← Thông tin vé                     │
├─────────────────────────────────────┤
│                                     │
│ [Waiting Card] ← Chỉ PENDING       │
│                                     │
│ [Trip Info Card] ← Luôn hiển thị   │
│                                     │
│ [Details Card] ← Luôn hiển thị     │
│                                     │
│ [Payment Method] ← Chỉ PENDING     │
│                                     │
│ [QR Code] ← Chỉ CONFIRMED/COMPLETED│
│                                     │
│ [Buttons] ← Tùy status              │
└─────────────────────────────────────┘
```

---

## 🔄 Visibility Logic theo Status:

### 1. **PENDING (Chờ thanh toán)**

**Hiển thị:**
- ✅ Waiting card (nếu online payment)
- ✅ Trip info card
- ✅ Details card  
- ✅ Payment method section
- ✅ Action buttons (Hủy vé + Thanh toán)

**Ẩn:**
- ❌ QR code section

**Layout:**
```
[⏰ Đang giữ chỗ - 09:59]
[🚌 Trip Card]
[📋 Details Card]
[💳 Payment Method - ViMoMo]
[Hủy vé] [Thanh toán • 350.000đ]
```

---

### 2. **CONFIRMED (Đã thanh toán)**

**Hiển thị:**
- ✅ Trip info card
- ✅ Details card
- ✅ QR code section
- ✅ Nút "Hủy vé" (nếu cho phép hủy)

**Ẩn:**
- ❌ Waiting card
- ❌ Payment method section
- ❌ Nút "Thanh toán"

**Layout:**
```
[🚌 Trip Card]
[📋 Details Card]

Mã QR lên xe
┌─────────┐
│ [QR]    │
│         │
└─────────┘
Đưa mã này cho nhà xe...

[Hủy vé]
```

---

### 3. **COMPLETED (Đã đi)**

**Hiển thị:**
- ✅ Trip info card
- ✅ Details card
- ✅ QR code section

**Ẩn:**
- ❌ Waiting card
- ❌ Payment method section
- ❌ TẤT CẢ buttons (vì đã đi rồi, không thể hủy)

**Layout:**
```
[🚌 Trip Card]
[📋 Details Card]

Mã QR lên xe
┌─────────┐
│ [QR]    │
│         │
└─────────┘

(Không có buttons)
```

---

### 4. **CANCELLED (Đã hủy)**

**Hiển thị:**
- ✅ Trip info card (chỉ info)
- ✅ Details card (chỉ info)

**Ẩn:**
- ❌ Waiting card
- ❌ Payment method section
- ❌ QR code section
- ❌ TẤT CẢ buttons

**Layout:**
```
[🚌 Trip Card]
[📋 Details Card]

(Không có gì thêm)
```

---

## 🔧 Code Changes:

### Java (BookingDetailActivity.java):

**Added fields:**
```java
private TextView tvPaymentMethodHeading;
private View cardWaiting, cardPaymentMethod, qrCodeSection, actionButtonsContainer;
```

**Updated visibility logic trong `displayBookingDetails()`:**

```java
if ("pending".equals(status)) {
    // Show: waiting card, payment method, buttons
    // Hide: QR code
} else if ("confirmed".equals(status) || "completed".equals(status)) {
    // Show: QR code
    // Hide: waiting card, payment method
    // Buttons: only cancel for confirmed, none for completed
} else {
    // Hide: everything except basic info
}
```

### XML (activity_booking_detail.xml):

**Sections với proper IDs:**
- `cardWaiting` - Countdown card
- `cardPaymentMethod` + `tvPaymentMethodHeading` - Payment section
- `qrCodeSection` - QR code với heading và hint
- `actionButtonsContainer` - Buttons container

**All sections:** `android:visibility="gone"` by default, controlled by Java code

---

## 🧪 Test Cases:

### Test 1: Pending Online Payment
1. Tạo booking với QR payment
2. **Expected:**
   - ✅ Có countdown card
   - ✅ Có payment method section
   - ✅ Có 2 buttons: Hủy vé + Thanh toán
   - ❌ Không có QR code

### Test 2: Confirmed Booking
1. Booking đã thanh toán thành công
2. **Expected:**
   - ❌ Không có countdown card
   - ❌ Không có payment method section
   - ✅ Có QR code to lớn
   - ✅ Có nút "Hủy vé"
   - ❌ Không có nút "Thanh toán"

### Test 3: Completed Booking
1. Chuyến xe đã đi (arrival_time < now)
2. **Expected:**
   - ✅ Có QR code (lịch sử)
   - ❌ KHÔNG có bất kỳ button nào
   - ❌ Không có payment method

### Test 4: Cancelled Booking
1. Vé đã bị hủy
2. **Expected:**
   - ❌ Không có QR code
   - ❌ Không có buttons
   - ❌ Không có payment method
   - ✅ Chỉ hiển thị info cơ bản

---

## 🎨 UI cho từng status:

### Pending:
```
┌───────────────────────────────┐
│ [Clock] Đang giữ chỗ  [09:59]│
├───────────────────────────────┤
│ [Dark Trip Card]              │
├───────────────────────────────┤
│ [Pickup/Dropoff]              │
├───────────────────────────────┤
│ Phương thức thanh toán        │
│ [MoMo] ViMoMo           Đổi   │
├───────────────────────────────┤
│ [Hủy vé] [Thanh toán•350k]   │
└───────────────────────────────┘
```

### Confirmed/Completed:
```
┌───────────────────────────────┐
│ [Dark Trip Card]              │
├───────────────────────────────┤
│ [Pickup/Dropoff]              │
├───────────────────────────────┤
│      Mã QR lên xe             │
│   ┌───────────────┐           │
│   │               │           │
│   │   [QR CODE]   │           │
│   │               │           │
│   └───────────────┘           │
│  Đưa mã này cho nhà xe...     │
├───────────────────────────────┤
│ [Hủy vé] ← Chỉ confirmed     │
└───────────────────────────────┘
```

---

## 📁 Files Changed:

1. ✅ `activity_booking_detail.xml` - Added IDs, restructured sections
2. ✅ `BookingDetailActivity.java` - Updated visibility logic

---

## 🚀 Status:

- ✅ **Committed & Pushed** to GitHub (commit `f1ad8fb`)
- 🔨 **APK đang build** (terminal running)

---

## ✅ Summary:

| Status | Waiting Card | Payment Method | QR Code | Buttons |
|--------|-------------|----------------|---------|---------|
| **Pending** | ✅ (nếu online) | ✅ | ❌ | Hủy vé + Thanh toán |
| **Confirmed** | ❌ | ❌ | ✅ | Hủy vé |
| **Completed** | ❌ | ❌ | ✅ | ❌ None |
| **Cancelled** | ❌ | ❌ | ❌ | ❌ None |

---

**🎉 ĐÃ FIX XONG! Layout bây giờ support đầy đủ tất cả trạng thái! 🚀**

**Đợi build xong và test thử cả 4 scenarios! 📱**

