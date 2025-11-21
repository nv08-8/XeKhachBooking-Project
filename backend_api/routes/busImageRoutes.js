const express = require("express");
const router = express.Router();
const images = require("../data/bus_images.json");

// GET /api/bus-image?operator=Nhà xe Hải Vân&bus_type=Giường nằm 32 chỗ có WC
router.get("/bus-image", (req, res) => {
    const { operator, bus_type } = req.query;

    // Validate required parameters
    if (!operator || !bus_type) {
        return res.status(400).json({
            success: false,
            message: "Missing required parameters: operator and bus_type",
            image: "https://placehold.co/600x300?text=Missing+Parameters"
        });
    }

    // Find matching bus image
    const found = images.find(
        item =>
            item.operator === operator &&
            item.bus_type === bus_type
    );

    if (!found) {
        return res.json({
            success: false,
            message: "No image found for this operator and bus type",
            image: "https://placehold.co/600x300?text=No+Image"
        });
    }

    // Return the first image URL (primary image)
    return res.json({
        success: true,
        image: found.image_urls[0],
        all_images: found.image_urls // Optional: return all images if needed
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

