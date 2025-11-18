package vn.hcmute.busbooking.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.ArrayList;
import java.util.List;
import vn.hcmute.busbooking.fragment.BookingListFragment;
import vn.hcmute.busbooking.model.Booking;

public class BookingViewPagerAdapter extends FragmentStateAdapter {

    private List<Booking> currentBookings;
    private List<Booking> pastBookings;
    private List<Booking> cancelledBookings;

    public BookingViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, 
                                   List<Booking> current, List<Booking> past, List<Booking> cancelled) {
        super(fragmentActivity);
        this.currentBookings = current;
        this.pastBookings = past;
        this.cancelledBookings = cancelled;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return BookingListFragment.newInstance(new ArrayList<>(pastBookings));
            case 2:
                return BookingListFragment.newInstance(new ArrayList<>(cancelledBookings));
            default:
                return BookingListFragment.newInstance(new ArrayList<>(currentBookings));
        }
    }

    @Override
    public int getItemCount() {
        return 3; // We have 3 tabs
    }
}
