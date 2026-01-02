-- Migration: Update route_stops with sample coordinates
-- Date: 2026-01-02
-- Description: Add sample latitude/longitude data to common Vietnamese cities

-- Update coordinates for stops in major cities
-- Hà Nội area coordinates
UPDATE route_stops
SET latitude = 21.0285, longitude = 105.8535
WHERE LOWER(address) LIKE '%hà nội%' OR LOWER(address) LIKE '%ha noi%' OR LOWER(address) LIKE '%hanoi%';

UPDATE route_stops
SET latitude = 21.0285, longitude = 105.8535
WHERE LOWER(name) LIKE '%hà nội%' OR LOWER(name) LIKE '%ha noi%' OR LOWER(name) LIKE '%hanoi%';

-- TP HCM area coordinates
UPDATE route_stops
SET latitude = 10.7769, longitude = 106.7009
WHERE LOWER(address) LIKE '%tp.hcm%' OR LOWER(address) LIKE '%hồ chí minh%' OR LOWER(address) LIKE '%ho chi minh%'
   OR LOWER(address) LIKE '%sài gòn%' OR LOWER(address) LIKE '%saigon%';

UPDATE route_stops
SET latitude = 10.7769, longitude = 106.7009
WHERE LOWER(name) LIKE '%tp.hcm%' OR LOWER(name) LIKE '%hồ chí minh%' OR LOWER(name) LIKE '%ho chi minh%'
   OR LOWER(name) LIKE '%sài gòn%' OR LOWER(name) LIKE '%saigon%';

-- Đà Lạt coordinates
UPDATE route_stops
SET latitude = 11.9404, longitude = 108.4481
WHERE LOWER(address) LIKE '%đà lạt%' OR LOWER(address) LIKE '%da lat%' OR LOWER(address) LIKE '%dalat%'
   OR LOWER(address) LIKE '%lâm đồng%' OR LOWER(address) LIKE '%lam dong%';

UPDATE route_stops
SET latitude = 11.9404, longitude = 108.4481
WHERE LOWER(name) LIKE '%đà lạt%' OR LOWER(name) LIKE '%da lat%' OR LOWER(name) LIKE '%dalat%'
   OR LOWER(name) LIKE '%lâm đồng%' OR LOWER(name) LIKE '%lam dong%';

-- Nha Trang coordinates
UPDATE route_stops
SET latitude = 12.2388, longitude = 109.1967
WHERE LOWER(address) LIKE '%nha trang%' OR LOWER(address) LIKE '%khánh hòa%' OR LOWER(address) LIKE '%khanh hoa%';

UPDATE route_stops
SET latitude = 12.2388, longitude = 109.1967
WHERE LOWER(name) LIKE '%nha trang%' OR LOWER(name) LIKE '%khánh hòa%' OR LOWER(name) LIKE '%khanh hoa%';

-- Đà Nẵng coordinates
UPDATE route_stops
SET latitude = 16.0544, longitude = 108.2022
WHERE LOWER(address) LIKE '%đà nẵng%' OR LOWER(address) LIKE '%da nang%' OR LOWER(address) LIKE '%danang%';

UPDATE route_stops
SET latitude = 16.0544, longitude = 108.2022
WHERE LOWER(name) LIKE '%đà nẵng%' OR LOWER(name) LIKE '%da nang%' OR LOWER(name) LIKE '%danang%';

-- Huế coordinates
UPDATE route_stops
SET latitude = 16.4637, longitude = 107.5909
WHERE LOWER(address) LIKE '%huế%' OR LOWER(address) LIKE '%hue%' OR LOWER(address) LIKE '%thừa thiên huế%';

UPDATE route_stops
SET latitude = 16.4637, longitude = 107.5909
WHERE LOWER(name) LIKE '%huế%' OR LOWER(name) LIKE '%hue%' OR LOWER(name) LIKE '%thừa thiên huế%';

-- Vũng Tàu coordinates
UPDATE route_stops
SET latitude = 10.3596, longitude = 107.0842
WHERE LOWER(address) LIKE '%vũng tàu%' OR LOWER(address) LIKE '%vung tau%' OR LOWER(address) LIKE '%bà rịa%';

UPDATE route_stops
SET latitude = 10.3596, longitude = 107.0842
WHERE LOWER(name) LIKE '%vũng tàu%' OR LOWER(name) LIKE '%vung tau%' OR LOWER(name) LIKE '%bà rịa%';

-- Cần Thơ coordinates
UPDATE route_stops
SET latitude = 10.0282, longitude = 105.7808
WHERE LOWER(address) LIKE '%cần thơ%' OR LOWER(address) LIKE '%can tho%';

UPDATE route_stops
SET latitude = 10.0282, longitude = 105.7808
WHERE LOWER(name) LIKE '%cần thơ%' OR LOWER(name) LIKE '%can tho%';

-- ✅ Coordinates updated for common Vietnamese cities

