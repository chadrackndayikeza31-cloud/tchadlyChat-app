package com.chadrack.tchadlychat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PreviewPublicationActivity extends AppCompatActivity {
    private static final long MAX_VID = 50L * 1024L * 1024L;
    private static final String SERVEUR = "https://tchadlychat-server.onrender.com/create-video-upload";
    private static final String SUPABASE = "https://ecdzacaaayomyqvtwpyo.supabase.co";
    private EditText etLegende;
    private ImageView imgApercu;
    private VideoView videoApercu;
    private Button btnPublier;
    private Uri mediaUri;
    private boolean estVideo;
    private String moi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) { finish(); return; }
        moi = u.getUid();
        setContentView(R.layout.activity_preview_publication);
        etLegende = findViewById(R.id.etLegende);
        imgApercu = findViewById(R.id.imgApercu);
        videoApercu = findViewById(R.id.videoApercu);
        btnPublier = findViewById(R.id.btnPublier);
        Button btnSupprimer = findViewById(R.id.btnSupprimer);
        mediaUri = Uri.parse(getIntent().getStringExtra("media_uri"));
        estVideo = getIntent().getBooleanExtra("est_video", false);
        if (estVideo) {
            imgApercu.setVisibility(View.GONE);
            videoApercu.setVisibility(View.VISIBLE);
            videoApercu.setMediaController(new MediaController(this));
            videoApercu.setVideoURI(mediaUri);
            videoApercu.start();
        } else {
            imgApercu.setImageURI(mediaUri);
        }
        btnSupprimer.setOnClickListener(v -> finish());
        btnPublier.setOnClickListener(v -> {
            String legende = etLegende.getText().toString().trim();
            if (legende.length() > 1000) { toast("Texte trop long (max 1000 caractères)."); return; }
            btnPublier.setEnabled(false);
            publier(legende, mediaUri, estVideo);
        });
    }

    private void publier(String legende, Uri uri, boolean video) {
        final String type = video ? "video" : "image";
        new Thread(() -> {
            try {
                setEtat("Préparation…");
                File f = preparer(uri, type);
                setEtat("Envoi en cours…");
                String url = envoyer(f, type);
                f.delete();
                Map<String, Object> m = new HashMap<>();
                m.put("author", moi);
                m.put("text", legende);
                m.put("mediaType", type);
                m.put("mediaUrl", url);
                m.put("createdAt", Timestamp.now());
                m.put("reactions", new HashMap<String, Object>());
                FirebaseFirestore.getInstance().collection("posts").add(m)
                    .addOnSuccessListener(r -> {
                        toast("Publication envoyée.");
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        runOnUiThread(() -> { btnPublier.setEnabled(true); btnPublier.setText("Publier"); });
                        toast("Publication impossible.");
                    });
            } catch (Exception e) {
                runOnUiThread(() -> { btnPublier.setEnabled(true); btnPublier.setText("Publier"); });
                toast(e.getMessage() == null ? "Erreur" : e.getMessage());
            }
        }).start();
    }

    private File preparer(Uri uri, String type) throws Exception {
        File f = new File(getCacheDir(), "post_" + System.currentTimeMillis() + ("image".equals(type) ? ".jpg" : ".mp4"));
        if ("image".equals(type)) {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) throw new Exception("Photo inaccessible");
            Bitmap bm = BitmapFactory.decodeStream(in);
            in.close();
            if (bm == null) throw new Exception("Photo invalide");
            int w = bm.getWidth(), h = bm.getHeight();
            float r = Math.min(1024f / w, 1024f / h);
            if (r < 1f) bm = Bitmap.createScaledBitmap(bm, Math.round(w * r), Math.round(h * r), true);
            try (FileOutputStream out = new FileOutputStream(f)) {
                bm.compress(Bitmap.CompressFormat.JPEG, 80, out);
            }
        } else {
            long taille = 0;
            try (InputStream in = getContentResolver().openInputStream(uri); FileOutputStream out = new FileOutputStream(f)) {
                if (in == null) throw new Exception("Vidéo inaccessible");
                byte[] b = new byte[65536];
                int n;
                while ((n = in.read(b)) != -1) {
                    taille += n;
                    if (taille > MAX_VID) throw new Exception("Vidéo trop grande (max 50 Mo)");
                    out.write(b, 0, n);
                }
            }
        }
        return f;
    }

    private String envoyer(File f, String type) throws Exception {
        String ext = "image".equals(type) ? "jpg" : "mp4";
        String mime = "image".equals(type) ? "image/jpeg" : "video/mp4";
        String token = Tasks.await(FirebaseAuth.getInstance().getCurrentUser().getIdToken(false), 20, TimeUnit.SECONDS).getToken();
        JSONObject body = new JSONObject();
        body.put("userId", moi);
        body.put("receiverUid", "");
        body.put("fileSize", f.length());
        body.put("mimeType", mime);
        body.put("fileName", f.getName());
        body.put("bucket", "posts");
        body.put("ext", ext);
        byte[] bytes = body.toString().getBytes("UTF-8");
        HttpURLConnection c = (HttpURLConnection) new URL(SERVEUR).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(30000);
        c.setReadTimeout(60000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        c.setRequestProperty("Authorization", "Bearer " + token);
        c.setFixedLengthStreamingMode(bytes.length);
        try (OutputStream out = c.getOutputStream()) { out.write(bytes); }
        int code = c.getResponseCode();
        String txt = lire(code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream());
        c.disconnect();
        if (code < 200 || code >= 300) throw new Exception("Serveur HTTP " + code + " " + coupe(txt));
        JSONObject r = new JSONObject(txt);
        if (!r.optBoolean("success", false)) throw new Exception(r.optString("error", "Erreur serveur"));
        String path = r.getString("path");
        String tk = r.getString("token");
        String bk = r.optString("bucket", "videos");
        HttpURLConnection p = (HttpURLConnection) new URL(SUPABASE + "/storage/v1/object/upload/sign/" + bk + "/" + path + "?token=" + URLEncoder.encode(tk, "UTF-8")).openConnection();
        p.setRequestMethod("PUT");
        p.setConnectTimeout(30000);
        p.setReadTimeout(600000);
        p.setDoOutput(true);
        p.setUseCaches(false);
        p.setFixedLengthStreamingMode(f.length());
        p.setRequestProperty("Content-Type", mime);
        p.setRequestProperty("x-upsert", "true");
        try (InputStream in = new FileInputStream(f); OutputStream out = p.getOutputStream()) {
            byte[] bb = new byte[65536];
            int n;
            while ((n = in.read(bb)) != -1) out.write(bb, 0, n);
        }
        int pc = p.getResponseCode();
        String rep = lire(pc >= 200 && pc < 300 ? p.getInputStream() : p.getErrorStream());
        p.disconnect();
        if (pc < 200 || pc >= 300) throw new Exception("Envoi du média HTTP " + pc + " " + coupe(rep));
        return SUPABASE + "/storage/v1/object/public/" + bk + "/" + path;
    }

    private String coupe(String s) {
        if (s == null) return "";
        return s.length() > 160 ? s.substring(0, 160) : s;
    }

    private String lire(InputStream in) throws Exception {
        if (in == null) return "";
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        byte[] b = new byte[4096];
        int n;
        while ((n = in.read(b)) != -1) o.write(b, 0, n);
        in.close();
        return new String(o.toByteArray(), "UTF-8");
    }

    private void setEtat(final String s) {
        runOnUiThread(() -> btnPublier.setText(s));
    }

    private void toast(final String s) {
        runOnUiThread(() -> Toast.makeText(this, s, Toast.LENGTH_LONG).show());
    }
}
