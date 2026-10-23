
package com.chadrack.tchadlychat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import org.json.JSONObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class QuickReplyReceiver extends BroadcastReceiver{
public static final String ACTION_REPLY="com.chadrack.tchadlychat.ACTION_REPLY";
public static final String KEY_TEXT_REPLY="key_text_reply";
public static final String KEY_SENDER_UID="senderUid";
public static final String KEY_RECEIVER_UID="receiverUid";
public static final String KEY_NOTIFICATION_ID="notificationId";
private static final String SERVER_URL="https://tchadlychat-server.onrender.com/notify";
private static final MediaType JSON_TYPE=MediaType.get("application/json; charset=utf-8");
private static final OkHttpClient HTTP=new OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).build();
private static final long SAFETY_DELAY=25000L;
@Override
public void onReceive(Context context,Intent intent){
if(intent==null||!ACTION_REPLY.equals(intent.getAction()))return;
Bundle results=RemoteInput.getResultsFromIntent(intent);
if(results==null)return;
CharSequence reply=results.getCharSequence(KEY_TEXT_REPLY);
if(reply==null)return;
String text=reply.toString().trim();
if(text.isEmpty())return;
String senderUid=intent.getStringExtra(KEY_SENDER_UID);
String receiverUid=intent.getStringExtra(KEY_RECEIVER_UID);
if(senderUid==null||senderUid.trim().isEmpty()||receiverUid==null||receiverUid.trim().isEmpty())return;
FirebaseUser currentUser=FirebaseAuth.getInstance().getCurrentUser();
if(currentUser==null)return;
String currentUid=currentUser.getUid();
if(!currentUid.equals(receiverUid)||currentUid.equals(senderUid))return;
final int notificationId=intent.getIntExtra(KEY_NOTIFICATION_ID,-1);
final Context appContext=context.getApplicationContext();
final PendingResult pending=goAsync();
final AtomicBoolean done=new AtomicBoolean(false);
new Handler(Looper.getMainLooper()).postDelayed(()->terminer(pending,done),SAFETY_DELAY);
Map<String,Object> message=new HashMap<>();
message.put("sender",currentUid);
message.put("receiver",senderUid);
message.put("text",text);
message.put("type","text");
message.put("timestamp",Timestamp.now());
message.put("delivered",false);
message.put("seen",false);
message.put("deleted",false);
message.put("edited",false);
FirebaseFirestore.getInstance().collection("private_messages").add(message).addOnSuccessListener(ref->{
    annulerNotification(appContext,notificationId);
envoyerNotification(currentUser,ref.getId(),pending,done);
}).addOnFailureListener(e->terminer(pending,done));
}
private static void envoyerNotification(FirebaseUser user,String messageId,PendingResult pending,AtomicBoolean done){
user.getIdToken(false).addOnSuccessListener(result->{
String token=result==null?null:result.getToken();
if(token==null||token.isEmpty()){terminer(pending,done);return;}
try{
JSONObject json=new JSONObject();
json.put("messageId",messageId);
Request request=new Request.Builder()
.url(SERVER_URL)
.addHeader("Authorization","Bearer "+token)
.post(RequestBody.create(json.toString(),JSON_TYPE))
.build();
HTTP.newCall(request).enqueue(new Callback(){
    @Override
public void onFailure(Call call,IOException e){terminer(pending,done);}
@Override
public void onResponse(Call call,Response response){
try{response.close();}finally{terminer(pending,done);}
}
});
}catch(Exception e){terminer(pending,done);}
}).addOnFailureListener(e->terminer(pending,done));
}
private static void annulerNotification(Context context,int notificationId){
if(notificationId!=-1)NotificationManagerCompat.from(context).cancel(notificationId);
}
private static void terminer(PendingResult pending,AtomicBoolean done){
if(done.compareAndSet(false,true)&&pending!=null)pending.finish();
}
}