package vn.hcmute.busbooking.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.adapter.BookingAdapter;
import vn.hcmute.busbooking.model.Booking;

public class BookingListFragment extends Fragment {

    private RecyclerView rvBookings;
    private LinearLayout layoutEmpty;
    private List<Booking> bookingList = new ArrayList<>();

    public static BookingListFragment newInstance(ArrayList<Booking> bookings) {
        BookingListFragment fragment = new BookingListFragment();
        Bundle args = new Bundle();
        args.putSerializable("bookings", bookings);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookingList = (ArrayList<Booking>) getArguments().getSerializable("bookings");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_booking_list, container, false);

        rvBookings = view.findViewById(R.id.rvBookings);
        layoutEmpty = view.findViewById(R.id.layoutEmpty);

        rvBookings.setLayoutManager(new LinearLayoutManager(getContext()));

        if (bookingList == null || bookingList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvBookings.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvBookings.setVisibility(View.VISIBLE);
            BookingAdapter adapter = new BookingAdapter(bookingList, bookingId -> {
                // Handle cancel click
            });
            rvBookings.setAdapter(adapter);
        }

        return view;
    }
}
