package vn.hcmute.busbooking.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import vn.hcmute.busbooking.R;
import vn.hcmute.busbooking.api.ApiClient;
import vn.hcmute.busbooking.api.ApiService;
import vn.hcmute.busbooking.model.User;
import vn.hcmute.busbooking.utils.SessionManager;

public class EditUserActivity extends AppCompatActivity {

    private EditText etUserName, etUserEmail, etUserPhone;
    private Spinner spinnerUserRole, spinnerUserStatus;
    private Button btnSaveChanges;

    private ApiService apiService;
    private SessionManager sessionManager;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_user);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        etUserName = findViewById(R.id.etUserName);
        etUserEmail = findViewById(R.id.etUserEmail);
        etUserPhone = findViewById(R.id.etUserPhone);
        spinnerUserRole = findViewById(R.id.spinnerUserRole);
        spinnerUserStatus = findViewById(R.id.spinnerUserStatus);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);

        // Setup spinners
        ArrayAdapter<CharSequence> roleAdapter = ArrayAdapter.createFromResource(this,
                R.array.user_roles, android.R.layout.simple_spinner_item);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserRole.setAdapter(roleAdapter);

        ArrayAdapter<CharSequence> statusAdapter = ArrayAdapter.createFromResource(this,
                R.array.user_statuses, android.R.layout.simple_spinner_item);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserStatus.setAdapter(statusAdapter);

        // Get user data from intent
        if (getIntent().hasExtra("user")) {
            currentUser = (User) getIntent().getSerializableExtra("user");
            populateUserData();
        }

        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void populateUserData() {
        if (currentUser != null) {
            etUserName.setText(currentUser.getName());
            etUserEmail.setText(currentUser.getEmail());
            etUserPhone.setText(currentUser.getPhone());

            // Set spinner selections
            setSpinnerSelection(spinnerUserRole, R.array.user_roles, currentUser.getRole());
            setSpinnerSelection(spinnerUserStatus, R.array.user_statuses, currentUser.getStatus());
        }
    }

    private void setSpinnerSelection(Spinner spinner, int arrayId, String value) {
        String[] array = getResources().getStringArray(arrayId);
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void saveChanges() {
        if (currentUser == null) return;

        // Update user object from UI fields
        currentUser.setName(etUserName.getText().toString());
        currentUser.setEmail(etUserEmail.getText().toString());
        currentUser.setPhone(etUserPhone.getText().toString());
        currentUser.setRole(spinnerUserRole.getSelectedItem().toString());
        currentUser.setStatus(spinnerUserStatus.getSelectedItem().toString());

        int adminId = sessionManager.getUserId();
        Call<User> call = apiService.updateUser(adminId, currentUser.getId(), currentUser);
        call.enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditUserActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(EditUserActivity.this, "Lưu thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(EditUserActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
