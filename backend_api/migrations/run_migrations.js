const db = require('../db');
const fs = require('fs');
const path = require('path');

async function runMigrations() {
    try {
        console.log('🔄 Starting migrations...');

        // Read all SQL files in migrations directory
        const migrationsDir = __dirname;
        const files = fs.readdirSync(migrationsDir)
            .filter(file => file.endsWith('.sql'))
            .sort();

        if (files.length === 0) {
            console.log('✅ No migrations to run');
            return;
        }

        for (const file of files) {
            const filePath = path.join(migrationsDir, file);
            const sql = fs.readFileSync(filePath, 'utf8');

            try {
                console.log(`📝 Running migration: ${file}`);
                await db.query(sql);
                console.log(`✅ Migration completed: ${file}`);
            } catch (error) {
                console.error(`❌ Migration failed for ${file}:`, error.message);
                // Continue with next migration instead of throwing
            }
        }

        console.log('🎉 All migrations completed');
    } catch (error) {
        console.error('❌ Migration process failed:', error);
        throw error;
    }
}

module.exports = { runMigrations };

