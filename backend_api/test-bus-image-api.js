// Test script for Bus Image API
// Run with: node test-bus-image-api.js

const images = require("./data/bus_images.json");

console.log("🚌 Bus Image API Test\n");
console.log("=".repeat(60));

// Test 1: Check total images
console.log(`\n✅ Total bus image entries: ${images.length}`);

// Test 2: Show unique operators
const operators = [...new Set(images.map(item => item.operator))];
console.log(`\n✅ Unique operators (${operators.length}):`);
operators.forEach(op => console.log(`   - ${op}`));

// Test 3: Show unique bus types
const busTypes = [...new Set(images.map(item => item.bus_type))];
console.log(`\n✅ Unique bus types (${busTypes.length}):`);
busTypes.forEach(type => console.log(`   - ${type}`));

// Test 4: Sample queries
console.log("\n" + "=".repeat(60));
console.log("\n🧪 Sample API Queries:\n");

const sampleQueries = [
    { operator: "Nhà xe Hải Vân", bus_type: "Giường nằm 32 chỗ có WC" },
    { operator: "Nhà xe Phương Trang", bus_type: "Giường nằm 44 chỗ" },
    { operator: "Nhà xe Thành Bưởi", bus_type: "Limousine 22 giường phòng" },
    { operator: "Nhà xe Kumho", bus_type: "Giường nằm 40 chỗ" },
    { operator: "Nhà xe Thuận Tiên", bus_type: "Limousine 24 chỗ" }
];

sampleQueries.forEach((query, index) => {
    const found = images.find(
        item => item.operator === query.operator && item.bus_type === query.bus_type
    );

    console.log(`${index + 1}. Operator: "${query.operator}"`);
    console.log(`   Bus Type: "${query.bus_type}"`);

    if (found) {
        console.log(`   ✅ FOUND - Primary Image: ${found.image_urls[0]}`);
        console.log(`   📸 Total images: ${found.image_urls.length}`);
    } else {
        console.log(`   ❌ NOT FOUND - Would return placeholder`);
    }
    console.log();
});

// Test 5: Check for duplicates
console.log("=".repeat(60));
console.log("\n🔍 Checking for duplicates...\n");

const combinations = images.map(item => `${item.operator}|||${item.bus_type}`);
const uniqueCombinations = new Set(combinations);

if (combinations.length === uniqueCombinations.size) {
    console.log("✅ No duplicates found - All combinations are unique!");
} else {
    console.log("⚠️  Duplicates found:");
    const duplicates = combinations.filter((item, index) => combinations.indexOf(item) !== index);
    duplicates.forEach(dup => {
        const [operator, busType] = dup.split("|||");
        console.log(`   - Operator: "${operator}", Bus Type: "${busType}"`);
    });
}

// Test 6: Validate image URLs
console.log("\n" + "=".repeat(60));
console.log("\n🔗 Validating image URLs...\n");

let totalUrls = 0;
let validUrls = 0;
let invalidUrls = [];

images.forEach(item => {
    item.image_urls.forEach(url => {
        totalUrls++;
        if (url && url.startsWith("http")) {
            validUrls++;
        } else {
            invalidUrls.push({
                operator: item.operator,
                busType: item.bus_type,
                url: url
            });
        }
    });
});

console.log(`✅ Total image URLs: ${totalUrls}`);
console.log(`✅ Valid URLs: ${validUrls}`);

if (invalidUrls.length > 0) {
    console.log(`⚠️  Invalid URLs: ${invalidUrls.length}`);
    invalidUrls.forEach(item => {
        console.log(`   - ${item.operator} / ${item.busType}: "${item.url}"`);
    });
} else {
    console.log("✅ All URLs are valid!");
}

console.log("\n" + "=".repeat(60));
console.log("\n🎉 Test completed!\n");

// Generate example API URLs
console.log("📋 Example API Request URLs:\n");
sampleQueries.slice(0, 3).forEach((query, index) => {
    const encodedOperator = encodeURIComponent(query.operator);
    const encodedBusType = encodeURIComponent(query.bus_type);
    console.log(`${index + 1}. /api/bus-image?operator=${encodedOperator}&bus_type=${encodedBusType}`);
});

console.log("\n" + "=".repeat(60) + "\n");

