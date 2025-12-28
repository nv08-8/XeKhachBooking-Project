# 🚀 Database Migration - Add passenger_info JSONB Column

## 📋 Tóm tắt

Migration này thêm cột `passenger_info` kiểu **JSONB** vào bảng `bookings` để lưu thông tin hành khách một cách linh hoạt.

### ✅ Ưu điểm của JSONB:
- 🔧 **Linh hoạt:** Không cần ALTER TABLE khi thêm field mới (CCCD, ghi chú, giới tính...)
- ⚡ **Hiệu năng:** Query nhanh với GIN index
- 📦 **Tiện lợi:** Lưu tất cả info hành khách trong 1 field
- 🔍 **Searchable:** Có thể query từng field bên trong JSON

---

## 🛠️ Cách Chạy Migration

### Option 1: Trên Render Dashboard (Production)

1. **Vào Render Dashboard:** https://dashboard.render.com
2. **Chọn database của bạn** (PostgreSQL service)
3. **Connect to database** hoặc vào **Shell** tab
4. **Copy và paste SQL từ file:** `backend_api/migrations/add_passenger_info_columns.sql`
5. **Run** và verify

### Option 2: Dùng psql Command Line

```bash
psql -U postgres -d xe_khach_db -f backend_api/migrations/add_passenger_info_columns.sql
```

### Option 3: Dùng pgAdmin

1. Mở pgAdmin
2. Connect vào database `xe_khach_db`
3. Tools → Query Tool
4. Paste SQL từ file migration
5. Execute (F5)

### Option 4: Từ PowerShell (Render Database)

```powershell
# Get connection string from Render Dashboard
$connString = "your-render-database-url-here"

# Run migration
psql $connString -f "C:\Users\Admin\Documents\GitHub\XeKhachBooking-Project\backend_api\migrations\add_passenger_info_columns.sql"
```

---

## 📝 Migration File Location

```
backend_api/migrations/add_passenger_info_columns.sql
```

## ✅ Verify Migration Thành Công

Sau khi chạy migration, check:

```sql
-- Check column exists
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'bookings' 
  AND column_name = 'passenger_info';

-- Should return:
-- column_name    | data_type
-- passenger_info | jsonb

-- Check indexes
SELECT indexname 
FROM pg_indexes 
WHERE tablename = 'bookings' 
  AND indexname LIKE '%passenger%';

-- Should return 3 indexes:
-- idx_bookings_passenger_info_gin
-- idx_bookings_passenger_phone
-- idx_bookings_passenger_email
```

---

## 🔄 Workflow Hoàn Chỉnh

### Bước 1: Run Migration (1 lần duy nhất)

Chạy SQL migration trên production database (Render)

### Bước 2: Deploy Backend Code

Code backend đã được update để dùng `passenger_info` JSONB:

```javascript
// Backend tự động build JSON object:
{
  "name": "Vo Nhu",
  "phone": "0987654321", 
  "email": "nhuvonguyen2005@gmail.com"
}
```

### Bước 3: Test

1. Mở app
2. Tạo booking offline
3. Check database:

```sql
SELECT 
    id,
    passenger_info->>'name' as name,
    passenger_info->>'phone' as phone,
    passenger_info->>'email' as email,
    total_amount,
    status
FROM bookings
ORDER BY created_at DESC
LIMIT 5;
```

---

## 🎯 Deployment Steps

1. ✅ **Run migration trên Render Database** (1 lần)
2. ✅ **Commit và push code** (backend đã update)
3. ✅ **Render auto-deploy backend**
4. ✅ **Test booking**

---

## 📊 Example Queries

### Insert with passenger_info

```sql
INSERT INTO bookings (
    trip_id, user_id, payment_method, passenger_info, 
    total_amount, seats_count, status, 
    pickup_stop_id, dropoff_stop_id
)
VALUES (
    62, 24, 'offline',
    '{"name": "Vo Nhu", "phone": "0909123456", "email": "vonhu@gmail.com"}'::jsonb,
    249000, 1, 'pending',
    37, 40
);
```

### Query passenger info

```sql
-- Get all bookings with passenger names
SELECT 
    id,
    passenger_info->>'name' as passenger_name,
    passenger_info->>'phone' as passenger_phone,
    total_amount
FROM bookings
WHERE passenger_info IS NOT NULL;

-- Find booking by phone
SELECT * 
FROM bookings
WHERE passenger_info->>'phone' = '0909123456';

-- Find bookings with email
SELECT *
FROM bookings
WHERE passenger_info ? 'email';

-- Update passenger info
UPDATE bookings
SET passenger_info = jsonb_set(
    passenger_info,
    '{phone}',
    '"0999999999"'
)
WHERE id = 123;
```

---

## 🚨 IMPORTANT: Chạy Migration TRƯỚC KHI Deploy Code!

**Thứ tự đúng:**
1. Run migration trên Render Database ← **QUAN TRỌNG!**
2. Commit & push code
3. Render auto-deploy

**Nếu deploy code trước:**
- Backend sẽ cố INSERT vào `passenger_info` column
- Column chưa tồn tại → **500 ERROR**
- Phải rollback hoặc migration ngay

---

## 📞 Support

Nếu migration fail, check:
- Database connection
- Permissions (need ALTER TABLE permission)
- Column đã tồn tại chưa (migration script check tự động)

---

**🎯 ACTION REQUIRED:**
1. Run migration SQL trên Render Database
2. Commit + push code (đã sẵn sàng)
3. Test booking!

**Migration script đã sẵn sàng, an toàn, và idempotent (chạy nhiều lần không sao)! 🚀**

