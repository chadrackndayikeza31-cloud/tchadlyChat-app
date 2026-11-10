package com.chadrack.tchadlychat;import com.google.android.gms.tasks.Tasks;import com.google.firebase.auth.FirebaseAuth;import org.json.JSONObject;import java.io.OutputStream;import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
public class NotifPost{
private static final String SERVEUR="https://tchadlychat-server.onrender.com/notify-post";
public static void envoyer(final String postId,final String type){
new Thread(()->{
try{
if(FirebaseAuth.getInstance().getCurrentUser()==null)return;
String token=Tasks.await(FirebaseAuth.getInstance().getCurrentUser().getIdToken(false),20,TimeUnit.SECONDS).getToken();
JSONObject body=new JSONObject();
body.put("postId",postId);
body.put("type",type);
byte[] b=body.toString().getBytes("UTF-8");
HttpURLConnection c=(HttpURLConnection)new URL(SERVEUR).openConnection();
c.setRequestMethod("POST");
c.setConnectTimeout(20000);
c.setReadTimeout(30000);
c.setDoOutput(true);
c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
c.setRequestProperty("Authorization","Bearer "+token);
c.setFixedLengthStreamingMode(b.length);
try(OutputStream o=c.getOutputStream()){o.write(b);}
c.getResponseCode();
c.disconnect();
}catch(Exception ignored){
}
}).start();
}
}
