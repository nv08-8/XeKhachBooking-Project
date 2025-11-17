# Database Migration - Fix Phone Number Type

## Problem
Phone numbers starting with 0 (e.g., 0987654322) were losing the leading zero because they were stored as INT/BIGINT in the database.

## Solution
Change the `sdt` (phone number) column from INT/BIGINT to VARCHAR(15) to preserve the leading zero.

## How to Run This Migration

### Option 1: Using MySQL Command Line
```bash
mysql -u your_username -p your_database < fix_phone_number_type.sql
```

### Option 2: Using MySQL Workbench or phpMyAdmin
1. Open your database management tool
2. Connect to your database
3. Open and execute the SQL file: `fix_phone_number_type.sql`

### Option 3: Using Node.js Script
Run from the backend_api directory:
```bash
node run-migration.js
```

## What This Migration Does

1. **Changes column type**: Converts `sdt` column from INT/BIGINT to VARCHAR(15)
2. **Fixes existing data**: Adds leading zero to any 9-digit phone numbers
3. **Preserves data**: All existing phone numbers are preserved

## After Running Migration

- All new phone numbers will be stored as strings
- Leading zeros will be preserved
- The app will display phone numbers correctly (e.g., 0987654322)

## Backend Changes Made

- `/routes/authRoutes.js` - Updated to store phone as string in both register and update profile endpoints
- Phone validation now uses regex: `/^\d{9,11}$/` to accept 9-11 digit phone numbers

## Frontend Changes Made

- `EditProfileActivity.java` - Added logic to format phone numbers correctly and add leading zero if missing
- Phone numbers from API are now properly formatted before display

## Notes

- Make sure to backup your database before running this migration
- Test on a development database first
- If you're using Railway or a cloud database, you may need to run this through their console

