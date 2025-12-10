package com.example.trackify;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class SignUpActivity extends AppCompatActivity {

    private TextInputEditText etFirstName, etLastName, etEmail, etPhone, etPassword, etConfirmPassword;
    private MaterialButton btnSignUp;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private TextView tvLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        tvLogin = findViewById(R.id.tvLogin);

        // Views
        etFirstName        = findViewById(R.id.etFirstName);
        etLastName         = findViewById(R.id.etLastName);
        etEmail            = findViewById(R.id.etEmail);
        etPhone            = findViewById(R.id.etPhone);
        etPassword         = findViewById(R.id.etPassword);
        etConfirmPassword  = findViewById(R.id.etConfirmPassword);
        btnSignUp          = findViewById(R.id.btnSignUp);
        tvLogin.setOnClickListener(view -> {
            Intent intent = new Intent(SignUpActivity.this, SignIn.class);
            startActivity(intent);
        });
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String firstName = etFirstName.getText().toString().trim();
                String lastName  = etLastName.getText().toString().trim();
                String email     = etEmail.getText().toString().trim();
                String phone     = etPhone.getText().toString().trim();
                String password  = etPassword.getText().toString().trim();
                String confirm   = etConfirmPassword.getText().toString().trim();

                if (TextUtils.isEmpty(firstName)) {
                    etFirstName.setError("Required");
                    etFirstName.requestFocus();
                    return;
                }
                if (TextUtils.isEmpty(lastName)) {
                    etLastName.setError("Required");
                    etLastName.requestFocus();
                    return;
                }
                if (TextUtils.isEmpty(email)) {
                    etEmail.setError("Required");
                    etEmail.requestFocus();
                    return;
                }
                if (TextUtils.isEmpty(password)) {
                    etPassword.setError("Required");
                    etPassword.requestFocus();
                    return;
                }
                if (!password.equals(confirm)) {
                    etConfirmPassword.setError("Passwords do not match");
                    etConfirmPassword.requestFocus();
                    return;
                }
                if (password.length() < 6) {
                    etPassword.setError("Password must be at least 6 characters");
                    etPassword.requestFocus();
                    return;
                }





                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                // progressBar.setVisibility(View.GONE);

                                if (task.isSuccessful()) {
                                    Toast.makeText(SignUpActivity.this,
                                            "Account created successfully",
                                            Toast.LENGTH_SHORT).show();

                                    // ➜ بعد النجاح نروح لصفحة تسجيل الدخول
                                    Intent intent = new Intent(SignUpActivity.this, SignIn.class);
                                    // ممكن تبغين تمريين الإيميل عشان نعبّيه تلقائي
                                    intent.putExtra("email", email);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // هنا يطلع لك رساله الخطأ بدال ما الكود يكرش
                                    String msg = (task.getException() != null)
                                            ? task.getException().getMessage()
                                            : "Failed to sign up";
                                    Toast.makeText(SignUpActivity.this, msg, Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}
