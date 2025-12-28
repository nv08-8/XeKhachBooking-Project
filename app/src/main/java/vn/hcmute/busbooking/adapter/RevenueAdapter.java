package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

import vn.hcmute.busbooking.R;

public class RevenueAdapter extends RecyclerView.Adapter<RevenueAdapter.RevenueViewHolder> {

    private List<Map<String, Object>> revenues;

    public RevenueAdapter(List<Map<String, Object>> revenues) {
        this.revenues = revenues;
    }

    @Override
    public RevenueViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_revenue, parent, false);
        return new RevenueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RevenueViewHolder holder, int position) {
        Map<String, Object> revenue = revenues.get(position);

        Object routeObj = revenue.get("route");
        Object dateObj = revenue.get("date");
        Object totalObj = revenue.get("total_revenue");
        Object ticketsObj = revenue.get("total_tickets");

        holder.tvRevenueTitle.setText(routeObj != null ? routeObj.toString() : "?");
        holder.tvRevenueDate.setText(dateObj != null ? dateObj.toString() : "");
        holder.tvRevenueAmount.setText((totalObj != null ? totalObj : "0") + " VNĐ");
        holder.tvRevenueTickets.setText((ticketsObj != null ? ticketsObj : "0") + " vé");
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

