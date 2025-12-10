package com.example.trackify;  // غيري الباكيج حسب مشروعك

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

// Firebase Auth
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.GoogleAuthProvider;
// Google Sign-In
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class SignIn extends AppCompatActivity {

    private static final int RC_GOOGLE_SIGN_IN = 100;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleClient;
    private FrameLayout btnGoogle, btnApple;
     private TextView tvGoToSignUp;

    private MaterialButton  btnLogin;
    private TextInputEditText etEmailLogin, etPasswordLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Firebase
        mAuth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.google_app_id))
                .requestEmail()
                .build();

        googleClient = GoogleSignIn.getClient(this, gso);


        etEmailLogin     = findViewById(R.id.etEmailLogin);
        etPasswordLogin  = findViewById(R.id.etPasswordLogin);
        btnLogin         = findViewById(R.id.btnLogin);
        tvGoToSignUp     = findViewById(R.id.tvGoToSignUp);

        btnGoogle = findViewById(R.id.google);
        btnApple  = findViewById(R.id.apple);
        tvGoToSignUp.setOnClickListener(view -> {
            Intent intent = new Intent(SignIn.this, SignUpActivity.class);
            startActivity(intent);
        });
        btnLogin.setOnClickListener(v -> loginUser());

        tvGoToSignUp.setOnClickListener(v -> {
            Intent intent = new Intent(SignIn.this, SignUpActivity.class);
            startActivity(intent);
        });

        btnGoogle.setOnClickListener(v -> {
            Intent signInIntent = googleClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });

        btnApple.setOnClickListener(v ->
                Toast.makeText(SignIn.this, "Apple login coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    private void loginUser() {
        String email    = etEmailLogin.getText() != null ? etEmailLogin.getText().toString().trim() : "";
        String password = etPasswordLogin.getText() != null ? etPasswordLogin.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmailLogin.setError("Enter email");
            etEmailLogin.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPasswordLogin.setError("Enter password");
            etPasswordLogin.requestFocus();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(SignIn.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()) {
                            Intent intent = new Intent(SignIn.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    | Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String msg = (task.getException() != null)
                                    ? task.getException().getMessage()
                                    : "Login failed";
                            Toast.makeText(SignIn.this, msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        Toast.makeText(SignIn.this, "Google Login Successful", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(SignIn.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(SignIn.this, "Google login failed", Toast.LENGTH_SHORT).show();
                    }

                });
    }
}
