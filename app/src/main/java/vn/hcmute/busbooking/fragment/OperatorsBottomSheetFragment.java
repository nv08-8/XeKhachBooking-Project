package vn.hcmute.busbooking.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

import vn.hcmute.busbooking.R;

public class OperatorsBottomSheetFragment extends BottomSheetDialogFragment {

    public interface OperatorsListener {
        void onOperatorsApplied(String operator); // empty = all
    }

    private OperatorsListener listener;
    private List<String> operators = new ArrayList<>();
    private OperatorsAdapter adapter;

    public void setOperatorsListener(OperatorsListener l) { this.listener = l; }
    public void setOperators(List<String> ops) { this.operators = ops == null ? new ArrayList<>() : new ArrayList<>(ops); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_operators, container, false);
        EditText etSearch = view.findViewById(R.id.et_search_operator);
        RecyclerView rv = view.findViewById(R.id.rv_operators);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OperatorsAdapter(operators);
        rv.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        Button btnClear = view.findViewById(R.id.btn_clear_operators);
        Button btnApply = view.findViewById(R.id.btn_apply_operators);
        btnClear.setOnClickListener(v -> {
            if (listener != null) listener.onOperatorsApplied("");
            dismiss();
        });
        btnApply.setOnClickListener(v -> {
            String sel = adapter.getSelected();
            if (listener != null) listener.onOperatorsApplied(sel == null ? "" : sel);
            dismiss();
        });

        return view;
    }

    // RecyclerView Adapter inside fragment
    private static class OperatorsAdapter extends RecyclerView.Adapter<OperatorsAdapter.VH> {
        private List<String> original;
        private List<String> items;
        private int selectedIndex = -1;
        OperatorsAdapter(List<String> data) { this.original = new ArrayList<>(data); this.items = new ArrayList<>(data); }
        void filter(String q) {
            items.clear();
            String nq = q == null ? "" : q.toLowerCase();
            for (String s : original) if (s.toLowerCase().contains(nq)) items.add(s);
            selectedIndex = -1;
            notifyDataSetChanged();
        }
        String getSelected() { return selectedIndex >= 0 && selectedIndex < items.size() ? items.get(selectedIndex) : null; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_single_choice, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH holder, int position) {
            String text = items.get(position);
            boolean checked = position == selectedIndex;
            holder.bind(text, checked);
            holder.itemView.setOnClickListener(v -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                selectedIndex = pos;
                notifyDataSetChanged();
            });
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            android.widget.CheckedTextView tv;
            VH(@NonNull View itemView) { super(itemView); tv = itemView.findViewById(android.R.id.text1); }
            void bind(String text, boolean checked) { tv.setText(text); tv.setChecked(checked); }
        }
    }
}
