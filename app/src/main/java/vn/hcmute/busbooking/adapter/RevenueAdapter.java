package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class RevenueAdapter extends RecyclerView.Adapter<RevenueAdapter.RevenueViewHolder> {

    private List<Map<String, Object>> revenues;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> revenue);
    }

    public RevenueAdapter(List<Map<String, Object>> revenues, OnItemClickListener listener) {
        this.revenues = revenues;
        this.listener = listener;
    }

    @Override
    public RevenueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_revenue, parent, false);
        return new RevenueViewHolder(view);
    }

    private String formatDateValue(Object dateObj) {
        if (dateObj == null) return "";
        String dateStr = dateObj.toString();
        try {
            // Handle full date-time format
            if (dateStr.contains("T")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
                Date date = sdf.parse(dateStr);
                SimpleDateFormat newSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                return newSdf.format(date);
            }
        } catch (ParseException e) {
            // Fallback for other formats or if parsing fails
        }
        return dateStr;
    }

    @Override
    public void onBindViewHolder(RevenueViewHolder holder, int position) {
        Map<String, Object> revenue = revenues.get(position);

        Object routeObj = revenue.get("route");
        Object dateObj = revenue.get("date");
        Object totalObj = revenue.get("total_revenue");
        Object ticketsObj = revenue.get("total_tickets");

        holder.tvRevenueTitle.setText(routeObj != null ? routeObj.toString() : "?");
        holder.tvRevenueDate.setText(formatDateValue(dateObj));
        holder.tvRevenueAmount.setText((totalObj != null ? totalObj : "0") + " VNĐ");
        holder.tvRevenueTickets.setText((ticketsObj != null ? ticketsObj : "0") + " vé");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(revenue);
            }
        });
    }

    @Override
    public int getItemCount() {
        return revenues != null ? revenues.size() : 0;
    }

    public static class RevenueViewHolder extends RecyclerView.ViewHolder {
        TextView tvRevenueTitle, tvRevenueDate, tvRevenueAmount, tvRevenueTickets;

        public RevenueViewHolder(View itemView) {
            super(itemView);
            tvRevenueTitle = itemView.findViewById(R.id.tvRevenueTitle);
            tvRevenueDate = itemView.findViewById(R.id.tvRevenueDate);
            tvRevenueAmount = itemView.findViewById(R.id.tvRevenueAmount);
            tvRevenueTickets = itemView.findViewById(R.id.tvRevenueTickets);
        }
    }
}
