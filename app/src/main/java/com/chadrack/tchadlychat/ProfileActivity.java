package com.chadrack.tchadlychat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileActivity extends Activity {

    private static final int PICK_IMAGE = 100;

    private ImageView ivPhotoProfil;
    private TextView tvPseudoProfil;
    private Button btnAjouterPhoto;
    private Button btnContinuer;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    private String uid;
    private String pseudo;

    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        ivPhotoProfil =
                findViewById(R.id.ivPhotoProfil);

        tvPseudoProfil =
                findViewById(R.id.tvPseudoProfil);

        btnAjouterPhoto =
                findViewById(R.id.btnAjouterPhoto);

        btnContinuer =
                findViewById(R.id.btnContinuer);

        firebaseAuth =
                FirebaseAuth.getInstance();

        firestore =
                FirebaseFirestore.getInstance();

        FirebaseUser user =
                firebaseAuth.getCurrentUser();

        if (user == null) {

            Toast.makeText(
                    this,
                    "Utilisateur non connecté",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
            return;
        }

        uid = user.getUid();

        pseudo =
                getIntent()
                        .getStringExtra("pseudo");

        if (pseudo == null || pseudo.isEmpty()) {
            pseudo = "Utilisateur";
        }

        tvPseudoProfil.setText(pseudo);

        btnAjouterPhoto.setOnClickListener(v -> {
            ouvrirGalerie();
        });

        btnContinuer.setOnClickListener(v -> {

            if (imageUri != null) {

                enregistrerPhoto();

            } else {

                continuerSansPhoto();
            }
        });
    }

    private void ouvrirGalerie() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.setType("image/*");

        startActivityForResult(
                intent,
                PICK_IMAGE
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == PICK_IMAGE
                && resultCode == RESULT_OK
                && data != null) {

            imageUri = data.getData();

            if (imageUri != null) {

                ivPhotoProfil.setImageURI(
                        imageUri
                );

                btnAjouterPhoto.setText(
                        "Changer la photo"
                );
            }
        }
    }

    private void enregistrerPhoto() {

        btnAjouterPhoto.setEnabled(false);
        btnContinuer.setEnabled(false);

        try {

            InputStream inputStream =
                    getContentResolver()
                            .openInputStream(imageUri);

            if (inputStream == null) {

                afficherErreur(
                        "Impossible de lire la photo"
                );

                return;
            }

            Bitmap bitmap =
                    BitmapFactory.decodeStream(
                            inputStream
                    );

            inputStream.close();

            if (bitmap == null) {

                afficherErreur(
                        "Photo invalide"
                );

                return;
            }

            // Réduire la photo
            Bitmap photoReduite =
                    reduirePhoto(bitmap, 400);

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            // Compression JPEG
            photoReduite.compress(
                    Bitmap.CompressFormat.JPEG,
                    60,
                    outputStream
            );

            byte[] imageBytes =
                    outputStream.toByteArray();

            // Transformer en texte Base64
            String photoBase64 =
                    Base64.encodeToString(
                            imageBytes,
                            Base64.NO_WRAP
                    );

            // Vérifier la taille
            if (photoBase64.length() > 900000) {

                afficherErreur(
                        "La photo est encore trop grande"
                );

                return;
            }

            firestore
                    .collection("users")
                    .document(uid)
                    .update(
                            "photoBase64",
                            photoBase64
                    )
                    .addOnSuccessListener(
                            unused -> {

                                Toast.makeText(
                                        ProfileActivity.this,
                                        "Photo enregistrée",
                                        Toast.LENGTH_SHORT
                                ).show();

                                ouvrirAccueil();
                            }
                    )
                    .addOnFailureListener(
                            e -> {

                                afficherErreur(
                                        "Erreur profil: "
                                                + e.getMessage()
                                );
                            }
                    );

        } catch (Exception e) {

            afficherErreur(
                    "Erreur photo: "
                            + e.getMessage()
            );
        }
    }

    private Bitmap reduirePhoto(
            Bitmap original,
            int tailleMax) {

        int largeur =
                original.getWidth();

        int hauteur =
                original.getHeight();

        float ratio =
                Math.min(
                        (float) tailleMax / largeur,
                        (float) tailleMax / hauteur
                );

        if (ratio >= 1) {
            return original;
        }

        int nouvelleLargeur =
                Math.round(
                        largeur * ratio
                );

        int nouvelleHauteur =
                Math.round(
                        hauteur * ratio
                );

        return Bitmap.createScaledBitmap(
                original,
                nouvelleLargeur,
                nouvelleHauteur,
                true
        );
    }

    private void continuerSansPhoto() {

        firestore
                .collection("users")
                .document(uid)
                .update(
                        "photoBase64",
                        ""
                )
                .addOnSuccessListener(
                        unused -> {

                            ouvrirAccueil();
                        }
                )
                .addOnFailureListener(
                        e -> {

                            Toast.makeText(
                                    ProfileActivity.this,
                                    "Erreur: "
                                            + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                );
    }

    private void afficherErreur(
            String message) {

        btnAjouterPhoto.setEnabled(true);
        btnContinuer.setEnabled(true);

        Toast.makeText(
                ProfileActivity.this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void ouvrirAccueil() {

        Intent intent =
                new Intent(
                        ProfileActivity.this,
                        HomeActivity.class
                );

        startActivity(intent);

        finish();
    }
}