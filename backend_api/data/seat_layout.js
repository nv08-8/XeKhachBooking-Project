/**
 * Tự động sinh mảng 'seats' chi tiết dựa trên bus_type và cấu trúc layout thô.
 * @param {string} busType - Loại xe, ví dụ: 'Giường nằm 40 chỗ'.
 * @param {object} seatLayout - Đối tượng layout gốc từ database.
 * @returns {object} - Đối tượng layout đã được bổ sung mảng 'seats' chi tiết.
 */
function generateDetailedSeatLayout(busType, seatLayout) {
    if (!seatLayout || !seatLayout.floors) {
        return seatLayout; // Trả về nếu dữ liệu không hợp lệ
    }

    const seatLetters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';

    // Hàm phụ trợ để sinh một lưới ghế chuẩn (không gán label trực tiếp)
    const generateGridRaw = (rows, cols) => {
        const gridSeats = [];
        for (let r = 0; r < rows; r++) {
            for (let c = 0; c < cols; c++) {
                gridSeats.push({ row: r, col: c, label: '', type: 'bed' });
            }
        }
        return gridSeats;
    };

    for (let fi = 0; fi < seatLayout.floors.length; fi++) {
        const floor = seatLayout.floors[fi];
        let seats = [];
        let extraEndBed = 0; // Number of extra end bed seats
        const { rows, cols } = floor;

        switch (busType) {
            case 'Giường nằm 40 chỗ':
                seats = generateGridRaw(5, 3); // positions only
                // Thêm 5 ghế băng cuối
                for (let i = 0; i < 5; i++) {
                    seats.push({ row: 5, col: i, label: '', type: 'bed' });
                }
                extraEndBed = 5;
                break;

            case 'Giường nằm 41 chỗ':
                seats = generateGridRaw(6, 3);
                if (floor.floor === 2) {
                    for (let i = 0; i < 5; i++) {
                        seats.push({ row: 6, col: i, label: '', type: 'bed' });
                    }
                    extraEndBed = 5;
                }
                break;

            case 'Limousine 32 giường có WC':
            case 'Giường nằm 32 chỗ có WC':
                seats = generateGridRaw(5, 3);
                seats.push({ row: 5, col: 0, label: '', type: 'bed' });
                extraEndBed = 1;
                break;

            case 'Limousine 34 giường':
            case 'Giường nằm 34 chỗ':
                for (let r = 0; r < 5; r++) {
                    for (let c = 0; c < 3; c++) {
                        if (r === 0 && c === 1) continue;
                        seats.push({ row: r, col: c, label: '', type: 'bed' });
                    }
                }
                for (let i = 0; i < 3; i++) {
                    seats.push({ row: 5, col: i, label: '', type: 'bed' });
                }
                extraEndBed = 3;
                break;

            case 'Giường nằm 38 chỗ có WC':
                seats = generateGridRaw(5, 3); // 5 rows x 3 cols = 15 ghế
                // Thêm 4 ghế băng cuối (extra end bed)
                for (let i = 0; i < 4; i++) {
                    seats.push({ row: 5, col: i, label: '', type: 'bed' });
                }
                extraEndBed = 4;
                break;

            case 'Limousine 24 giường phòng':
            case 'Limousine 24 chỗ':
                seats = generateGridRaw(6, 2);
                break;

            case 'Limousine 22 giường phòng':
            case 'Limousine 22 giường phòng có WC':
                seats = generateGridRaw(floor.rows, floor.cols);
                break;

            default:
                for (let r = 0; r < rows; r++) {
                    for (let c = 0; c < cols; c++) {
                        if (cols === 3 && c === 1) {
                            seats.push({ row: r, col: c, label: '', type: 'aisle' });
                        } else {
                            seats.push({ row: r, col: c, label: '', type: 'bed'});
                        }
                    }
                }
                break;
        }

        // Assign unique labels per floor: prefix letter by floor (A, B, C...) and sequential numbers per floor
        const floorPrefix = seatLetters[fi] || seatLetters[seatLetters.length - 1];
        let seq = 1;
        seats = seats.map(s => {
            if (!s.label && s.type === 'bed') {
                const label = `${floorPrefix}${seq}`;
                seq++;
                return { ...s, label };
            }
            // keep aisles or empty labels as-is
            return s;
        });

        floor.seats = seats; // set detailed seats on this floor
        if (extraEndBed > 0) {
            floor.extra_end_bed = extraEndBed; // add extra_end_bed info
        }
    }
    return seatLayout;
}

module.exports = { generateDetailedSeatLayout };
