package vn.hcmute.busbooking.adapter.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.Driver;

public class DriversAdapter extends RecyclerView.Adapter<DriversAdapter.DriverViewHolder> {

    private List<Driver> driverList;
    private OnDriverClickListener listener;

    public interface OnDriverClickListener {
        void onDeleteDriver(Driver driver);
    }

    public DriversAdapter(List<Driver> driverList, OnDriverClickListener listener) {
        this.driverList = driverList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_driver, parent, false);
        return new DriverViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        Driver driver = driverList.get(position);
        holder.tvDriverName.setText(driver.getName());
        holder.tvDriverPhone.setText(driver.getPhone());
        holder.tvDriverLicense.setText("GPLX: " + driver.getLicenseNumber());

        holder.btnDeleteDriver.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteDriver(driver);
            }
        });
    }

    @Override
    public int getItemCount() {
        return driverList.size();
    }

    static class DriverViewHolder extends RecyclerView.ViewHolder {
        TextView tvDriverName, tvDriverPhone, tvDriverLicense;
        ImageButton btnDeleteDriver;

        public DriverViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDriverName = itemView.findViewById(R.id.tvDriverName);
            tvDriverPhone = itemView.findViewById(R.id.tvDriverPhone);
            tvDriverLicense = itemView.findViewById(R.id.tvDriverLicense);
            btnDeleteDriver = itemView.findViewById(R.id.btnDeleteDriver);
        }
    }
}
