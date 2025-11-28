package vn.hcmute.busbooking.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import vn.hcmute.busbooking.R;

public class SortBottomSheetFragment extends BottomSheetDialogFragment {

    public interface SortListener {
        void onSortSelected(int mode);
    }

    private SortListener sortListener;

    public void setSortListener(SortListener l) { this.sortListener = l; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_sort, container, false);
        ListView list = view.findViewById(R.id.list_sort);
        String[] items = new String[] { "Mặc định", "Giờ đi sớm nhất", "Giờ đi muộn nhất", "Đánh giá cao nhất", "Giá giảm dần", "Giá tăng dần" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_single_choice, items);
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, v, position, id) -> {
            if (sortListener != null) sortListener.onSortSelected(position);
            dismiss();
        });

        Button btnClear = view.findViewById(R.id.btn_clear_filter);
        Button btnApply = view.findViewById(R.id.btn_apply_filter);

        btnClear.setOnClickListener(v -> {
            // clear selection
            list.clearChoices();
            adapter.notifyDataSetChanged();
        });

        btnApply.setOnClickListener(v -> {
            int checked = list.getCheckedItemPosition();
            if (checked != ListView.INVALID_POSITION && sortListener != null) {
                sortListener.onSortSelected(checked);
            }
            dismiss();
        });

        return view;
    }
}
