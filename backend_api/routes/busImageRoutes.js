const express = require("express");
const router = express.Router();
const path = require("path");
const fs = require("fs");

// Use absolute path for compatibility with different environments
const imagesPath = path.join(__dirname, "../data/bus_images.json");
let images = [];

try {
    const data = fs.readFileSync(imagesPath, "utf8");
    images = JSON.parse(data);
    // Remove TikTok URLs from image lists to avoid blocked/403 images
    images.forEach(item => {
        if (Array.isArray(item.image_urls)) {
            item.image_urls = item.image_urls.filter(url => typeof url === 'string' && !url.includes('tiktok.com'));
        }
    });
    console.log(`✅ Loaded ${images.length} bus images (TikTok links removed)`);
} catch (error) {
    console.error("❌ Error loading bus_images.json:", error.message);
    images = []; // Fallback to empty array
}

// Helper function to normalize strings for comparison
function normalize(str) {
    if (!str) return "";
    return str.toLowerCase().trim().replace(/\s+/g, " ");
}

// GET /api/bus-image?operator=Nhà xe Hải Vân&bus_type=Giường nằm 32 chỗ có WC
router.get("/bus-image", (req, res) => {
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

    // Normalize input for comparison
    const normalizedOperator = normalize(operator);
    const normalizedBusType = normalize(bus_type);

    // Find matching bus image with normalized comparison
    const found = images.find(
        item =>
            normalize(item.operator) === normalizedOperator &&
            normalize(item.bus_type) === normalizedBusType
    );

    if (!found) {
        console.log(`❌ No exact match found`);
        console.log(`   Looking for: operator="${normalizedOperator}", bus_type="${normalizedBusType}"`);
        console.log(`   Available operators:`, [...new Set(images.map(i => i.operator))]);

        return res.json({
            success: false,
            message: `No image found for operator: "${operator}" and bus type: "${bus_type}"`,
            requested: { operator, bus_type },
            image: "https://placehold.co/600x300?text=No+Image"
        });
    }

    console.log(`✅ Found match with ${found.image_urls.length} images`);

    // Filter out TikTok URLs (they're blocked with HTTP 403) -- defensive: keep finalUrls only non-tiktok
    const nonTikTokUrls = found.image_urls.filter(url => !url.includes('tiktok.com'));

    console.log(`   Total URLs: ${found.image_urls.length}, Non-TikTok URLs: ${nonTikTokUrls.length}`);

    // If we have non-TikTok URLs, use them. Otherwise fallback to original
    const finalUrls = nonTikTokUrls.length > 0 ? nonTikTokUrls : found.image_urls;

    // Return only reliable non-TikTok images and include `image_urls` key for Android client compatibility
    return res.json({
        success: true,
        image: finalUrls[0],
        image_urls: finalUrls,
        all_images: finalUrls,
        note: nonTikTokUrls.length === 0 ? "Only TikTok URLs available (may be blocked)" : "Using reliable non-TikTok URLs"
    });
});

// GET /api/bus-images - Get all available bus images (optional endpoint)
router.get("/bus-images", (req, res) => {
    return res.json({
        success: true,
        count: images.length,
        data: images
    });
});

module.exports = router;
