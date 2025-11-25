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

    // Hàm phụ trợ để sinh một lưới ghế chuẩn
    const generateGrid = (rows, cols, startLetterIdx = 0) => {
        const gridSeats = [];
        for (let r = 0; r < rows; r++) {
            for (let c = 0; c < cols; c++) {
                gridSeats.push({
                    row: r,
                    col: c,
                    label: `${seatLetters[c + startLetterIdx]}${r + 1}`,
                    type: 'bed'
                });
            }
        }
        return gridSeats;
    };

    seatLayout.floors.forEach(floor => {
        let seats = [];
        const { rows, cols } = floor;

        switch (busType) {
            case 'Giường nằm 40 chỗ':
                seats = generateGrid(5, 3); // 15 ghế (A,B,C)
                // Thêm 5 ghế băng cuối
                for (let i = 0; i < 5; i++) {
                    seats.push({ row: 5, col: i, label: `E${i + 1}`, type: 'bed' });
                }
                break;

            case 'Giường nằm 41 chỗ':
                seats = generateGrid(6, 3); // 18 ghế
                if (floor.floor === 2) {
                    // Tầng trên thêm 5 ghế băng cuối
                    for (let i = 0; i < 5; i++) {
                        seats.push({ row: 6, col: i, label: `E${i + 1}`, type: 'bed' });
                    }
                }
                break;

            case 'Limousine 32 giường có WC':
            case 'Giường nằm 32 chỗ có WC':
                seats = generateGrid(5, 3); // 15 ghế
                // Thêm 1 ghế cuối
                seats.push({ row: 5, col: 0, label: `E1`, type: 'bed' });
                break;

            case 'Limousine 34 giường':
            case 'Giường nằm 34 chỗ':
                // 5 hàng, 3 cột nhưng hàng đầu chỉ có 2 ghế
                for (let r = 0; r < 5; r++) {
                    for (let c = 0; c < 3; c++) {
                        // Bỏ qua ghế giữa của hàng đầu tiên
                        if (r === 0 && c === 1) continue;
                        seats.push({
                            row: r,
                            col: c,
                            label: `${seatLetters[c]}${r + 1}`,
                            type: 'bed'
                        });
                    }
                }
                // Thêm 3 ghế cuối để đủ 17 ghế/tầng (tổng 34)
                // (34/2 = 17, 14 ghế ở trên + 3 ghế cuối = 17)
                 for (let i = 0; i < 3; i++) {
                    seats.push({ row: 5, col: i, label: `E${i + 1}`, type: 'bed' });
                }
                break;

            case 'Giường nằm 38 chỗ có WC':
                seats = generateGrid(5, 3); // 15 ghế
                // Thêm 1 ghế gần cuối
                seats.push({ row: 4, col: 3, label: `D5`, type: 'bed' });
                // Thêm băng 3 ghế cuối
                for (let i = 0; i < 3; i++) {
                    seats.push({ row: 5, col: i, label: `E${i + 1}`, type: 'bed' });
                }
                break;

            case 'Limousine 24 giường phòng':
            case 'Limousine 24 chỗ':
                seats = generateGrid(6, 2); // 12 ghế/tầng
                break;

            case 'Limousine 22 giường phòng':
            case 'Limousine 22 giường phòng có WC':
                seats = generateGrid(floor.rows, floor.cols); // Tầng 1: 5x2, Tầng 2: 6x2
                break;

            default:
                // Fallback: Nếu không có loại xe nào khớp, tạo theo rows và cols
                 for (let r = 0; r < rows; r++) {
                    for (let c = 0; c < cols; c++) {
                         // Giả định layout 3 cột có lối đi ở giữa
                        if (cols === 3 && c === 1) {
                             seats.push({ row: r, col: c, label: '', type: 'aisle' });
                        } else {
                            let letterIdx = c > 1 ? c -1 : c;
                            seats.push({ row: r, col: c, label: `${seatLetters[letterIdx]}${r + 1}`, type: 'bed'});
                        }
                    }
                }
                break;
        }
        floor.seats = seats; // Gán mảng seats chi tiết vào
    });
    return seatLayout;
}

module.exports = { generateDetailedSeatLayout };
