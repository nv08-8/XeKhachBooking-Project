package vn.hcmute.busbooking;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.model.Seat;

public class SeatAdapter extends RecyclerView.Adapter<SeatAdapter.SeatViewHolder> {

    private List<Seat> seatList;
    private OnSeatClickListener listener;

    // Interface để Activity xử lý khi chọn ghế
    public interface OnSeatClickListener {
        void onSeatClick(Seat seat);
    }

    public SeatAdapter(List<Seat> seatList) {
        this.seatList = seatList;
    }

    public void setOnSeatClickListener(OnSeatClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public SeatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Đảm bảo bạn có file layout item_seat.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_seat, parent, false);
        return new SeatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeatViewHolder holder, int position) {
        Seat seat = seatList.get(position);

        // Hiển thị tên ghế (VD: A01) - Dùng getLabel() nếu model của bạn tên là label
        // Nếu model của bạn là getSeatNumber() thì giữ nguyên, nhưng API trả về 'label'
        // Bạn nên kiểm tra lại model Seat xem tên hàm getter là gì.
        // Giả sử API trả về 'label', model nên có getLabel().
        // Ở đây tôi dùng getLabel() theo đúng API backend chúng ta vừa sửa.
        holder.tvSeatNumber.setText(seat.getLabel());

        // LOGIC MÀU SẮC QUAN TRỌNG
        // 1. Kiểm tra ghế đã bị người khác đặt (isBooked từ API)
        if (seat.isBooked()) {
            // Ghế đã đặt: Màu Xám, Chữ Trắng
            holder.itemView.setBackgroundColor(Color.GRAY);
            holder.tvSeatNumber.setTextColor(Color.WHITE);

            // Vô hiệu hóa click
            holder.itemView.setEnabled(false);
            holder.itemView.setOnClickListener(null);
        }
        // 2. Kiểm tra ghế đang được người dùng hiện tại chọn (isSelected logic local)
        else if (seat.isSelected()) {
            // Ghế đang chọn: Màu Cam (hoặc màu chủ đạo), Chữ Trắng
            holder.itemView.setBackgroundColor(Color.parseColor("#FFB000"));
            holder.tvSeatNumber.setTextColor(Color.WHITE);

            // Vẫn cho click để bỏ chọn
            holder.itemView.setEnabled(true);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSeatClick(seat);
            });
        }
        // 3. Ghế trống bình thường
        else {
            // Ghế trống: Màu Trắng (hoặc Xanh nhạt), Chữ Đen
            holder.itemView.setBackgroundColor(Color.parseColor("#E0E0E0")); // Màu xám rất nhạt hoặc trắng
            holder.tvSeatNumber.setTextColor(Color.BLACK);

            // Cho phép click để chọn
            holder.itemView.setEnabled(true);
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSeatClick(seat);
            });
        }
    }

    @Override
    public int getItemCount() {
        return seatList != null ? seatList.size() : 0;
    }

    public static class SeatViewHolder extends RecyclerView.ViewHolder {
        TextView tvSeatNumber;

        public SeatViewHolder(@NonNull View itemView) {
            super(itemView);
            // Đảm bảo id này khớp với file item_seat.xml
            tvSeatNumber = itemView.findViewById(R.id.tvSeatNumber);
        }
    }
}
