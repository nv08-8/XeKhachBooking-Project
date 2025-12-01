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

import java.util.List;

import vn.hcmute.busbooking.R;

public class TimeBottomSheetFragment extends BottomSheetDialogFragment {

    public interface TimeListener {
        void onTimeApplied(float minTime, float maxTime);
    }

    private TimeListener listener;

    public void setTimeListener(TimeListener l) { this.listener = l; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_time, container, false);
        RangeSlider slider = view.findViewById(R.id.slider_time);
        Button btnApply = view.findViewById(R.id.btn_apply_time);
        btnApply.setOnClickListener(v -> {
            List<Float> vals = slider.getValues();
            float a = vals.get(0); float b = vals.get(1);
            if (listener != null) listener.onTimeApplied(a, b);
            dismiss();
        });
        return view;
    }
}

