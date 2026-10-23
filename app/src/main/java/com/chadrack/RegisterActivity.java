
package com.chadrack.tchadlychat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends Activity {

    private EditText etPseudo;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;

    private Button btnRegister;
    private Button btnBack;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etPseudo = findViewById(R.id.etPseudo);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnRegister = findViewById(R.id.btnRegister);
        btnBack = findViewById(R.id.btnBack);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        btnRegister.setOnClickListener(v -> registerUser());
        btnBack.setOnClickListener(v -> finish());
    }

    private void registerUser() {

        String pseudo = etPseudo.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();
        String confirmPassword =
                etConfirmPassword.getText().toString();

        if (TextUtils.isEmpty(pseudo)) {
            etPseudo.setError("Entrez votre pseudo");
            etPseudo.requestFocus();
            return;
        }

        // Pseudo yemera inyuguti, imibare na underscore.
        if (!pseudo.matches("[a-zA-Z0-9_]{3,30}")) {
            etPseudo.setError(
                    "Pseudo: 3 à 30 caractères, lettres, chiffres ou _"
            );
            etPseudo.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)
                || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Entrez un email valide");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Minimum 6 caractères");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError(
                    "Les mots de passe ne correspondent pas"
            );
            etConfirmPassword.requestFocus();
            return;
        }

        btnRegister.setEnabled(false);

        String pseudoKey = pseudo.toLowerCase(Locale.ROOT);

        // Vérifier si le pseudo est déjà utilisé.
        firestore.collection("usernames")
                .document(pseudoKey)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        btnRegister.setEnabled(true);
                        etPseudo.setError("Ce pseudo est déjà utilisé");
                        etPseudo.requestFocus();
                        return;
                    }

                    creerCompte(pseudo, pseudoKey, email, password);
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    afficherErreur("Vérification du pseudo", e);
                });
    }

    private void creerCompte(
            String pseudo,
            String pseudoKey,
            String email,
            String password) {

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (!task.isSuccessful()) {
                        btnRegister.setEnabled(true);
                        String message = task.getException() != null
                                ? task.getException().getMessage()
                                : "Erreur d'inscription";

                        Toast.makeText(
                                this,
                                message,
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();

                    if (user == null) {
                        btnRegister.setEnabled(true);
                        Toast.makeText(
                                this,
                                "Utilisateur introuvable",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    enregistrerProfil(
                            user,
                            pseudo,
                            pseudoKey,
                            email
                    );
                });
    }

    private void enregistrerProfil(
            FirebaseUser user,
            String pseudo,
            String pseudoKey,
            String email) {

        String uid = user.getUid();

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("pseudo", pseudo);
        userData.put("email", email);
        userData.put("online", false);
        userData.put("photoUrl", "");
        userData.put("photoBase64", "");
        userData.put("createdAt", System.currentTimeMillis());

        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", uid);
        usernameData.put("email", email);
        usernameData.put("pseudo", pseudo);

        WriteBatch batch = firestore.batch();

        batch.set(
                firestore.collection("users").document(uid),
                userData
        );

        batch.set(
                firestore.collection("usernames").document(pseudoKey),
                usernameData
        );

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(
                            this,
                            "Compte créé avec succès",
                            Toast.LENGTH_SHORT
                    ).show();

                    Intent intent = new Intent(
                            RegisterActivity.this,
                            ProfileActivity.class
                    );

                    intent.putExtra("pseudo", pseudo);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    afficherErreur("Enregistrement Firestore", e);
                });
    }

    private void afficherErreur(String operation, Exception e) {
        String detail = e.getMessage() != null
                ? e.getMessage()
                : "Erreur inconnue";

        Toast.makeText(
                this,
                operation + " : " + detail,
                Toast.LENGTH_LONG
        ).show();
    }
}
