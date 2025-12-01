package vn.hcmute.busbooking.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.slider.RangeSlider;

import java.util.ArrayList;

import vn.hcmute.busbooking.R;

public class FilterBottomSheetFragment extends BottomSheetDialogFragment {

    private RangeSlider sliderPrice, sliderTime;
    private Button btnApplyFilter, btnClearFilter;
    private android.widget.Spinner spinnerOperator, spinnerBusType, spinnerPickup, spinnerDropoff;

    // new persistent value labels
    private android.widget.TextView tvPriceRange, tvTimeFrom, tvTimeTo;

    public interface FilterListener {
        void onFilterApplied(float minPrice, float maxPrice, float minTime, float maxTime,
                             String operator, String busType, String pickup, String dropoff);
    }

    private FilterListener filterListener;

    // Lists to be populated by caller
    private java.util.List<String> operators = new ArrayList<>();
    private java.util.List<String> busTypes = new ArrayList<>();
    private java.util.List<String> pickups = new ArrayList<>();
    private java.util.List<String> dropoffs = new ArrayList<>();

    public void setFilterListener(FilterListener listener) {
        this.filterListener = listener;
    }

    public void setOperators(java.util.List<String> ops) { if (ops != null) this.operators = ops; }
    public void setBusTypes(java.util.List<String> types) { if (types != null) this.busTypes = types; }
    public void setPickupLocations(java.util.List<String> p) { if (p != null) this.pickups = p; }
    public void setDropoffLocations(java.util.List<String> d) { if (d != null) this.dropoffs = d; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_filter, container, false);

        sliderPrice = view.findViewById(R.id.slider_price);
        sliderTime = view.findViewById(R.id.slider_time);
        btnApplyFilter = view.findViewById(R.id.btn_apply_filter);
        btnClearFilter = view.findViewById(R.id.btn_clear_filter);
        spinnerOperator = view.findViewById(R.id.spinner_operator);
        spinnerBusType = view.findViewById(R.id.spinner_bus_type);
        spinnerPickup = view.findViewById(R.id.spinner_pickup);
        spinnerDropoff = view.findViewById(R.id.spinner_dropoff);

        // bind new labels
        tvPriceRange = view.findViewById(R.id.tv_price_range);
        tvTimeFrom = view.findViewById(R.id.tv_time_from);
        tvTimeTo = view.findViewById(R.id.tv_time_to);

        // Initialize labels from slider initial values
        try {
            updatePriceLabel(sliderPrice.getValues().get(0), sliderPrice.getValues().get(1));
        } catch (Exception ignored) {}
        try {
            updateTimeLabels(sliderTime.getValues().get(0), sliderTime.getValues().get(1));
        } catch (Exception ignored) {}

        // Listen to slider changes to update persistent labels live
        sliderPrice.addOnChangeListener((slider, value, fromUser) -> {
            try {
                float min = slider.getValues().get(0);
                float max = slider.getValues().get(1);
                updatePriceLabel(min, max);
            } catch (Exception ignored) {}
        });

        sliderTime.addOnChangeListener((slider, value, fromUser) -> {
            try {
                float min = slider.getValues().get(0);
                float max = slider.getValues().get(1);
                updateTimeLabels(min, max);
            } catch (Exception ignored) {}
        });

        // Populate spinners (ensure first item means All)
        try {
            android.content.Context ctx = requireContext();
            java.util.List<String> ops = new ArrayList<>();
            ops.add("Tất cả"); if (!operators.isEmpty()) ops.addAll(operators);
            spinnerOperator.setAdapter(new android.widget.ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, ops));

            java.util.List<String> types = new ArrayList<>();
            types.add("Tất cả"); if (!busTypes.isEmpty()) types.addAll(busTypes);
            spinnerBusType.setAdapter(new android.widget.ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, types));

            java.util.List<String> ps = new ArrayList<>(); ps.add("Tất cả"); if (!pickups.isEmpty()) ps.addAll(pickups);
            spinnerPickup.setAdapter(new android.widget.ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, ps));

            java.util.List<String> ds = new ArrayList<>(); ds.add("Tất cả"); if (!dropoffs.isEmpty()) ds.addAll(dropoffs);
            spinnerDropoff.setAdapter(new android.widget.ArrayAdapter<>(ctx, android.R.layout.simple_spinner_dropdown_item, ds));
        } catch (Exception ignored) {}

        btnApplyFilter.setOnClickListener(v -> {
            if (filterListener != null) {
                float minPrice = sliderPrice.getValues().get(0);
                float maxPrice = sliderPrice.getValues().get(1);
                float minTime = sliderTime.getValues().get(0);
                float maxTime = sliderTime.getValues().get(1);

                String op = (spinnerOperator.getSelectedItem() == null) ? "" : spinnerOperator.getSelectedItem().toString();
                if ("Tất cả".equals(op)) op = "";
                String bt = (spinnerBusType.getSelectedItem() == null) ? "" : spinnerBusType.getSelectedItem().toString();
                if ("Tất cả".equals(bt)) bt = "";
                String pk = (spinnerPickup.getSelectedItem() == null) ? "" : spinnerPickup.getSelectedItem().toString();
                if ("Tất cả".equals(pk)) pk = "";
                String dp = (spinnerDropoff.getSelectedItem() == null) ? "" : spinnerDropoff.getSelectedItem().toString();
                if ("Tất cả".equals(dp)) dp = "";

                filterListener.onFilterApplied(minPrice, maxPrice, minTime, maxTime, op, bt, pk, dp);
            }
            dismiss();
        });

        btnClearFilter.setOnClickListener(v -> {
            sliderPrice.setValues(0f, 2000000f);
            sliderTime.setValues(0f, 24f);
            updatePriceLabel(0f, 2000000f);
            updateTimeLabels(0f, 24f);
            if (filterListener != null) {
                filterListener.onFilterApplied(0, 2000000, 0, 24, "", "", "", "");
            }
            dismiss();
        });

        return view;
    }

    // Format price like: 0đ, 2.000.000đ
    private void updatePriceLabel(float min, float max) {
        try {
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi","VN"));
            String minStr = nf.format((long) min) + "đ";
            String maxStr = nf.format((long) max) + "đ";
            if (tvPriceRange != null) tvPriceRange.setText(minStr + " - " + maxStr);
        } catch (Exception e) {
            try { if (tvPriceRange != null) tvPriceRange.setText((int)min + " - " + (int)max); } catch (Exception ignored) {}
        }
    }

    // Format time like 00:00 - 24:00
    private void updateTimeLabels(float min, float max) {
        try {
            if (tvTimeFrom != null) tvTimeFrom.setText(formatHour(min));
            if (tvTimeTo != null) tvTimeTo.setText(formatHour(max));
        } catch (Exception ignored) {}
    }

    private String formatHour(float h) {
        int hour = (int) Math.floor(h);
        int minute = Math.round((h - hour) * 60);
        // clamp
        if (hour < 0) hour = 0; if (hour > 24) hour = 24;
        if (minute < 0) minute = 0; if (minute >= 60) minute = 59;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", hour, minute);
    }
}
