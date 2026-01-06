const express = require("express");
const router = express.Router();
const db = require("../db");

// Helper function to normalize strings for comparison
function normalize(str) {
    if (!str) return "";
    return str.toLowerCase().trim().replace(/\s+/g, " ");
}

// GET /api/bus-image?operator=&bus_type=
router.get("/bus-image", async (req, res) => {
    const { operator, bus_type } = req.query;

    console.log(`🔍 Bus image request - operator: "${operator}", bus_type: "${bus_type}"`);

    // Validate required parameters
    if (!operator || !bus_type) {
        return res.status(400).json({
            success: false,
            message: "Missing required parameters: operator and bus_type",
            image: "https://placehold.co/600x300?text=Missing+Parameters"
        });
    }

    try {
        // Query database for matching bus image
        // Use ILIKE for case-insensitive search
        const query = `
            SELECT bus_type, image_urls
            FROM bus_images
            WHERE LOWER(TRIM(bus_type)) = LOWER(TRIM($1))
            LIMIT 1
        `;

        const { rows } = await db.query(query, [bus_type]);

        if (!rows || rows.length === 0) {
            console.log(`❌ No match found for bus_type: "${bus_type}"`);

            // Try to get all available bus types for debugging
            const { rows: allTypes } = await db.query('SELECT DISTINCT bus_type FROM bus_images LIMIT 10');
            console.log(`   Available bus types:`, allTypes.map(r => r.bus_type));

            return res.json({
                success: false,
                message: `No image found for bus type: "${bus_type}"`,
                requested: { operator, bus_type },
                image: "https://placehold.co/600x300?text=No+Image"
            });
        }

        const found = rows[0];
        console.log(`✅ Found match with bus_type: ${found.bus_type}`);

        // Parse image_urls from JSONB
        let imageUrls = [];
        try {
            if (typeof found.image_urls === 'string') {
                imageUrls = JSON.parse(found.image_urls);
            } else if (Array.isArray(found.image_urls)) {
                imageUrls = found.image_urls;
            }
        } catch (e) {
            console.error('Error parsing image_urls:', e);
            imageUrls = [];
        }

        // Filter out TikTok URLs (they're blocked with HTTP 403)
        const nonTikTokUrls = imageUrls.filter(url =>
            url && typeof url === 'string' && !url.includes('tiktok.com')
        );

        console.log(`   Total URLs: ${imageUrls.length}, Non-TikTok URLs: ${nonTikTokUrls.length}`);

        // If we have non-TikTok URLs, use them. Otherwise fallback to original
        const finalUrls = nonTikTokUrls.length > 0 ? nonTikTokUrls : imageUrls;

        if (finalUrls.length === 0) {
            return res.json({
                success: false,
                message: "No valid image URLs found",
                image: "https://placehold.co/600x300?text=No+Valid+Images"
            });
        }

        // Return only reliable non-TikTok images
        return res.json({
            success: true,
            image: finalUrls[0],
            image_urls: finalUrls,
            all_images: finalUrls,
            bus_type: found.bus_type,
            note: nonTikTokUrls.length === 0 ? "Only TikTok URLs available (may be blocked)" : "Using reliable non-TikTok URLs"
        });
    } catch (error) {
        console.error('Error fetching bus image:', error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
});

// GET /api/bus-images - Get all available bus images
router.get("/bus-images", async (req, res) => {
    try {
        const { rows } = await db.query('SELECT * FROM bus_images ORDER BY bus_type');
        return res.json({
            success: true,
            count: rows.length,
            data: rows
        });
    } catch (error) {
        console.error('Error fetching all bus images:', error);
        return res.status(500).json({
            success: false,
            message: "Server error",
            error: error.message
        });
    }
});

module.exports = router;
