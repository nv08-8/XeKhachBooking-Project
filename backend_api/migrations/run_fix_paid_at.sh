#!/bin/bash

# Script to apply paid_at timezone fix migration
# This adds 7 hours to all existing paid_at values in the bookings table

echo "🔄 Running paid_at timezone fix migration..."
echo "This will add 7 hours to all existing paid_at values to correct timezone offset."
echo ""

# Run the SQL migration file
psql -c "$(cat migrations/20260106_fix_paid_at_timezone.sql)"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Migration completed successfully!"
    echo ""
    echo "📊 Verification query:"
    echo "SELECT COUNT(*) FROM bookings WHERE paid_at IS NOT NULL;"
else
    echo ""
    echo "❌ Migration failed!"
    exit 1
fi

