package vn.hcmute.busbooking.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.model.User;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onEditUser(User user);
        void onDeleteUser(User user);
    }

    public UsersAdapter(List<User> userList, OnUserClickListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.tvUserName.setText(user.getName());
        holder.tvUserEmail.setText(user.getEmail());
        holder.tvUserRole.setText(user.getRole());

        if ("admin".equals(user.getRole())) {
            holder.ivUserIcon.setImageResource(R.drawable.ic_admin_icon);
        } else {
            holder.ivUserIcon.setImageResource(R.drawable.ic_account_circle);
        }

        holder.btnEditUser.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditUser(user);
            }
        });

        holder.btnDeleteUser.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteUser(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvUserEmail, tvUserRole;
        ImageView ivUserIcon;
        ImageButton btnEditUser, btnDeleteUser;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            ivUserIcon = itemView.findViewById(R.id.ivUserIcon);
            btnEditUser = itemView.findViewById(R.id.btnEditUser);
            btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}
