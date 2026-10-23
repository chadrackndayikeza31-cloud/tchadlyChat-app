package com.chadrack.tchadlychat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.RemoteInput;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationReplyReceiver extends BroadcastReceiver {

    public static final String ACTION_REPLY =
            "com.chadrack.tchadlychat.ACTION_REPLY";

    private static final String TAG = "TchadlyChat";

    // URL ya endpoint ya server.
    private static final String SERVER_URL =
            "https://tchadlychat-server.onrender.com/notify";

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    private static final OkHttpClient client = new OkHttpClient();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent == null ||
                !ACTION_REPLY.equals(intent.getAction())) {
            return;
        }

        Bundle results = RemoteInput.getResultsFromIntent(intent);

        if (results == null) {
            Log.e(TAG, "RemoteInput results zirabura.");
            return;
        }

        CharSequence replyText = results.getCharSequence(
                MyFirebaseMessagingService.KEY_TEXT_REPLY
        );

        if (replyText == null || replyText.toString().trim().isEmpty()) {
            return;
        }

        String text = replyText.toString().trim();

        String recipientUid = intent.getStringExtra(
                MyFirebaseMessagingService.KEY_SENDER_UID
        );

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.e(TAG, "User ntiyinjiyemwo.");
            return;
        }

        if (recipientUid == null || recipientUid.trim().isEmpty()) {
            Log.e(TAG, "UID y'uwo turungikira irabura.");
            return;
        }

        String senderUid = currentUser.getUid();

        if (senderUid.equals(recipientUid)) {
            Log.e(TAG, "Ntushobora kwiyandikira.");
            return;
        }

        final PendingResult pendingResult = goAsync();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> message = new HashMap<>();
        message.put("sender", senderUid);
        message.put("receiver", recipientUid);
        message.put("text", text);
        message.put("type", "text");
        message.put("timestamp", FieldValue.serverTimestamp());

        db.collection("private_messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {

                    String messageId = documentReference.getId();

                    Log.d(TAG, "Reply yabitswe: " + messageId);

                    sendNotificationToServer(
                            currentUser,
                            messageId,
                            pendingResult
                    );
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Reply ntibitswe muri Firestore.", e);
                    pendingResult.finish();
                });
    }

    private void sendNotificationToServer(
            FirebaseUser user,
            String messageId,
            PendingResult pendingResult
    ) {

        user.getIdToken(false)
                .addOnSuccessListener(tokenResult -> {

                    String idToken = tokenResult.getToken();

                    if (idToken == null || idToken.isEmpty()) {
                        Log.e(TAG, "Firebase ID token irabura.");
                        pendingResult.finish();
                        return;
                    }

                    try {
                        JSONObject json = new JSONObject();
                        json.put("messageId", messageId);

                        RequestBody requestBody = RequestBody.create(
                                json.toString(),
                                JSON
                        );

                        Request request = new Request.Builder()
                                .url(SERVER_URL)
                                .addHeader(
                                        "Authorization",
                                        "Bearer " + idToken
                                )
                                .post(requestBody)
                                .build();

                        client.newCall(request).enqueue(new Callback() {

                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.e(TAG, "Server request yanse.", e);
                                pendingResult.finish();
                            }

                            @Override
                            public void onResponse(Call call, Response response) {
                                try {
                                    if (!response.isSuccessful()) {
                                        Log.e(
                                                TAG,
                                                "Server error: " + response.code()
                                        );
                                    } else {
                                        Log.d(TAG, "Notification ya reply yarungitswe.");
                                    }
                                } finally {
                                    response.close();
                                    pendingResult.finish();
                                }
                            }
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Request ntishoboye gutegurwa.", e);
                        pendingResult.finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gufata Firebase token vyanse.", e);
                    pendingResult.finish();
                });
    }
}