// Quick script to run refund migration
// Usage: node run-refund-migration.js

const db = require('./db');
const fs = require('fs');
const path = require('path');

async function runMigration() {
    try {
        console.log('🔄 Running refund columns migration...\n');

        const migrationPath = path.join(__dirname, 'migrations', '003_add_refund_columns.sql');
        const sql = fs.readFileSync(migrationPath, 'utf8');

        await db.query(sql);

        console.log('✅ Migration completed successfully!\n');
        console.log('Added columns:');
        console.log('  - refund_amount (DECIMAL)');
        console.log('  - refund_percentage (INTEGER)');
        console.log('  - cancelled_at (TIMESTAMP)\n');

        // Verify columns were added
        const { rows } = await db.query(`
            SELECT column_name, data_type, is_nullable
            FROM information_schema.columns
            WHERE table_name = 'bookings'
            AND column_name IN ('refund_amount', 'refund_percentage', 'cancelled_at')
            ORDER BY column_name
        `);

        console.log('Verification:');
        rows.forEach(col => {
            console.log(`  ✓ ${col.column_name} (${col.data_type})`);
        });

        process.exit(0);
    } catch (err) {
        console.error('❌ Migration failed:', err.message);
        console.error(err.stack);
        process.exit(1);
    }
}

runMigration();

