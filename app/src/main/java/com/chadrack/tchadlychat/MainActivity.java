
package com.chadrack.tchadlychat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class MainActivity extends Activity {

    private EditText etPseudo;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnRegister;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private static final int NOTIFICATION_PERMISSION_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        CrashHandler.installer(this);
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            ouvrirHome();
            return;
        }

        setContentView(R.layout.activity_main);
        CrashHandler.afficherSiExiste(this);

        etPseudo = findViewById(R.id.etPseudo);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);

        demanderPermissionNotifications();

        btnLogin.setOnClickListener(v -> {
            String pseudo = etPseudo.getText().toString().trim();
            String password = etPassword.getText().toString();

            if (TextUtils.isEmpty(pseudo)) {
                etPseudo.setError("Entrez votre pseudo");
                etPseudo.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError("Entrez votre mot de passe");
                etPassword.requestFocus();
                return;
            }

            btnLogin.setEnabled(false);
            rechercherUtilisateur(pseudo, password);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    RegisterActivity.class
            );
            startActivity(intent);
        });
    }

    private void demanderPermissionNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    NOTIFICATION_PERMISSION_CODE
            );
        }
    }

    private void rechercherUtilisateur(String pseudo, String password) {
        String pseudoKey = pseudo.trim().toLowerCase(Locale.ROOT);

        firestore.collection("usernames")
                .document(pseudoKey)
                .get()
                .addOnSuccessListener(document -> {
                    if (isFinishing() || isDestroyed()) return;

                    if (!document.exists()) {
                        btnLogin.setEnabled(true);
                        Toast.makeText(
                                MainActivity.this,
                                "Pseudo introuvable dans usernames",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    String email = document.getString("email");

                    if (email == null || email.trim().isEmpty()) {
                        btnLogin.setEnabled(true);
                        Toast.makeText(
                                MainActivity.this,
                                "Email absent dans le document du pseudo",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    connecterAvecFirebase(
                            email.trim(),
                            password,
                            pseudo
                    );
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;

                    btnLogin.setEnabled(true);

                    Toast.makeText(
                            MainActivity.this,
                            "Erreur Firestore : " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void connecterAvecFirebase(
            String email,
            String password,
            String pseudo
    ) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (isFinishing() || isDestroyed()) return;

                    btnLogin.setEnabled(true);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                MainActivity.this,
                                "Connexion réussie",
                                Toast.LENGTH_SHORT
                        ).show();

                        ouvrirHome();
                    } else {
                        String message = "Pseudo ou mot de passe incorrect";

                        if (task.getException() != null) {
                            message = task.getException().getMessage();
                            if (message == null || message.trim().isEmpty()) {
                                message = "Connexion impossible. Réessayez.";
                            }
                        }

                        Toast.makeText(
                                MainActivity.this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void ouvrirHome() {
        Intent intent = new Intent(
                MainActivity.this,
                HomeActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
