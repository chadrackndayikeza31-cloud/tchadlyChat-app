package com.chadrack.tchadlychat;import android.Manifest;import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
public class CallActivity extends Activity{
private static final int REQ_PERMS=4001;
private static final long RING_TIMEOUT=45000L;
private static final String CALL_TOKEN_URL="https://tchadlychat-server.onrender.com/call-token";
private String callId,channel,type,peerName,role,myUid;
private String lastStatus="ringing";
private boolean isVideo,joined=false,finishing=false,muted=false,camOff=false,speaker=true;
private RtcEngine engine;
private FirebaseFirestore db;
private ListenerRegistration listener;
private FrameLayout remoteContainer,localContainer;
private TextView statusText,btnMute,btnSpeaker,btnCam;
private final Handler handler=new Handler(Looper.getMainLooper());
private final Runnable ringTimeout=()->{
if(!"ringing".equals(lastStatus))return;
db.collection("calls").document(callId).update("status","missed","endedAt",Timestamp.now())
.addOnCompleteListener(t->terminer(false));
};
private final IRtcEngineEventHandler evenements=new IRtcEngineEventHandler(){
@Override
public void onJoinChannelSuccess(String ch,int uid,int elapsed){
runOnUiThread(()->statusText.setText("En attente de l'autre personne..."));
}
@Override
public void onUserJoined(int uid,int elapsed){
runOnUiThread(()->{
statusText.setText("En communication");
if(isVideo&&engine!=null){
SurfaceView rv=new SurfaceView(CallActivity.this);
remoteContainer.removeAllViews();
remoteContainer.addView(rv);
engine.setupRemoteVideo(new VideoCanvas(rv,Constants.RENDER_MODE_HIDDEN,uid));
}
});
}
@Override
public void onUserOffline(int uid,int reason){
runOnUiThread(()->terminer(true));
}
};
@Override
protected void onCreate(Bundle savedInstanceState){
super.onCreate(savedInstanceState);
Intent i=getIntent();
callId=i.getStringExtra("callId");
channel=i.getStringExtra("channel");
type=i.getStringExtra("type");
peerName=i.getStringExtra("peerName");
role=i.getStringExtra("role");
if(callId==null||channel==null){finish();return;}
isVideo="video".equals(type);
if(peerName==null||peerName.trim().isEmpty())peerName="Utilisateur";
FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
if(u==null){finish();return;}
myUid=u.getUid();
db=FirebaseFirestore.getInstance();
construireUi();
String[] besoins=isVideo
?new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA}
:new String[]{Manifest.permission.RECORD_AUDIO};
boolean ok=true;
for(String p:besoins){if(checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED)ok=false;}
if(ok)demarrerEcoute();else requestPermissions(besoins,REQ_PERMS);
if("caller".equals(role))handler.postDelayed(ringTimeout,RING_TIMEOUT);
}
@Override
public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
super.onRequestPermissionsResult(requestCode,permissions,results);
if(requestCode!=REQ_PERMS)return;
boolean ok=results.length>0;
for(int r:results){if(r!=PackageManager.PERMISSION_GRANTED)ok=false;}
if(ok)demarrerEcoute();
else{toast("Autorisation du micro ou de la caméra refusée.");terminer(true);}
}
private void demarrerEcoute(){
if(listener!=null)return;
listener=db.collection("calls").document(callId).addSnapshotListener((snap,e)->{
if(e!=null||snap==null||!snap.exists())return;
String st=snap.getString("status");
if(st==null)return;
lastStatus=st;
if("accepted".equals(st)){
if(!joined)rejoindre();
}else if("rejected".equals(st)){
toast("Appel refusé.");terminer(false);
}else if("ended".equals(st)){
toast("Appel terminé.");terminer(false);
}else if("missed".equals(st)){
toast("Pas de réponse.");terminer(false);
}
});
}
private void rejoindre(){
joined=true;
statusText.setText("Connexion...");
new Thread(()->{
try{
final JSONObject r=obtenirToken();
runOnUiThread(()->initialiserMoteur(r.optString("appId"),r.optString("token")));
}catch(Exception e){
runOnUiThread(()->{toast("Connexion impossible.");terminer(true);});
}
}).start();
}
private JSONObject obtenirToken()throws Exception{
FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
if(u==null)throw new Exception("Utilisateur non connecté");
String idToken=Tasks.await(u.getIdToken(false),20,TimeUnit.SECONDS).getToken();
JSONObject body=new JSONObject();
body.put("channel",channel);
byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
HttpURLConnection c=(HttpURLConnection)new URL(CALL_TOKEN_URL).openConnection();
try{
c.setRequestMethod("POST");
c.setConnectTimeout(60000);
c.setReadTimeout(60000);
c.setDoOutput(true);
c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
c.setRequestProperty("Authorization","Bearer "+idToken);
c.setFixedLengthStreamingMode(bytes.length);
try(OutputStream out=c.getOutputStream()){out.write(bytes);}
int code=c.getResponseCode();
String text=lireFlux(code>=200&&code<300?c.getInputStream():c.getErrorStream());
if(code<200||code>=300)throw new Exception("HTTP "+code+" "+text);
JSONObject r=new JSONObject(text);
if(!r.optBoolean("success",false))throw new Exception(r.optString("error"));
return r;
}finally{c.disconnect();}
}
private String lireFlux(InputStream in)throws Exception{
if(in==null)return "";
try(InputStream i=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
byte[] b=new byte[4096];int n;
while((n=i.read(b))!=-1)out.write(b,0,n);
return new String(out.toByteArray(),StandardCharsets.UTF_8);
}
}
private void initialiserMoteur(String appId,String token){
if(finishing)return;
try{
RtcEngineConfig cfg=new RtcEngineConfig();
cfg.mContext=getApplicationContext();
cfg.mAppId=appId;
cfg.mEventHandler=evenements;
engine=RtcEngine.create(cfg);
engine.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION);
engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
engine.enableAudio();
engine.setEnableSpeakerphone(speaker);
if(isVideo){
engine.enableVideo();
SurfaceView local=new SurfaceView(this);
localContainer.removeAllViews();
localContainer.addView(local);
engine.setupLocalVideo(new VideoCanvas(local,Constants.RENDER_MODE_HIDDEN,0));
engine.startPreview();
}
engine.joinChannel(token,channel,"",0);
}catch(Exception e){
toast("Erreur audio ou vidéo.");
terminer(true);
}
}
private void terminer(boolean majFirestore){
if(finishing)return;
finishing=true;
handler.removeCallbacks(ringTimeout);
if(listener!=null){listener.remove();listener=null;}
if(majFirestore&&callId!=null){
db.collection("calls").document(callId).update("status","ended","endedAt",Timestamp.now());
}
if(engine!=null){
try{engine.leaveChannel();}catch(Exception ignored){}
RtcEngine.destroy();
engine=null;
}
finish();
}
@Override
public void onBackPressed(){
terminer(true);
}
@Override
protected void onDestroy(){
handler.removeCallbacks(ringTimeout);
if(listener!=null){listener.remove();listener=null;}
if(engine!=null){
try{engine.leaveChannel();}catch(Exception ignored){}
RtcEngine.destroy();
engine=null;
}
super.onDestroy();
}
private void construireUi(){
FrameLayout root=new FrameLayout(this);
root.setBackgroundColor(Color.rgb(18,24,34));
remoteContainer=new FrameLayout(this);
root.addView(remoteContainer,new FrameLayout.LayoutParams(-1,-1));
localContainer=new FrameLayout(this);
FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(dp(110),dp(150));
lp.gravity=Gravity.TOP|Gravity.END;
lp.setMargins(0,dp(40),dp(12),0);
root.addView(localContainer,lp);
TextView titre=new TextView(this);
titre.setText((isVideo?"Appel vidéo":"Appel audio")+"\n"+peerName);
titre.setTextColor(Color.WHITE);
titre.setTextSize(20);
titre.setGravity(Gravity.CENTER);
FrameLayout.LayoutParams tp=new FrameLayout.LayoutParams(-1,-2);
tp.gravity=Gravity.TOP;
tp.topMargin=dp(40);
root.addView(titre,tp);
statusText=new TextView(this);
statusText.setText("Appel en cours...");
statusText.setTextColor(Color.LTGRAY);
statusText.setTextSize(15);
statusText.setGravity(Gravity.CENTER);
FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-1,-2);
sp.gravity=Gravity.TOP;
sp.topMargin=dp(120);
root.addView(statusText,sp);
LinearLayout barre=new LinearLayout(this);
barre.setOrientation(LinearLayout.HORIZONTAL);
barre.setGravity(Gravity.CENTER);
barre.setPadding(0,dp(16),0,dp(32));
btnMute=bouton("🎤",Color.rgb(60,70,85),()->{
muted=!muted;
if(engine!=null)engine.muteLocalAudioStream(muted);
btnMute.setText(muted?"🔇":"🎤");
});
barre.addView(btnMute);
if(isVideo){
btnCam=bouton("📷",Color.rgb(60,70,85),()->{
camOff=!camOff;
if(engine!=null)engine.muteLocalVideoStream(camOff);
btnCam.setText(camOff?"🚫":"📷");
});
barre.addView(btnCam);
barre.addView(bouton("🔄",Color.rgb(60,70,85),()->{
if(engine!=null)engine.switchCamera();
}));
}
btnSpeaker=bouton("🔊",Color.rgb(60,70,85),()->{
speaker=!speaker;
if(engine!=null)engine.setEnableSpeakerphone(speaker);
btnSpeaker.setText(speaker?"🔊":"🔈");
});
barre.addView(btnSpeaker);
barre.addView(bouton("📞",Color.rgb(200,40,40),()->terminer(true)));
FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-2);
bp.gravity=Gravity.BOTTOM;
root.addView(barre,bp);
setContentView(root);
}
private TextView bouton(String texte,int couleur,Runnable action){
TextView b=new TextView(this);
b.setText(texte);
b.setTextSize(22);
b.setTextColor(Color.WHITE);
b.setGravity(Gravity.CENTER);
GradientDrawable rond=new GradientDrawable();
rond.setShape(GradientDrawable.OVAL);
rond.setColor(couleur);
b.setBackground(rond);
LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(60),dp(60));
p.setMargins(dp(10),0,dp(10),0);
b.setLayoutParams(p);
b.setOnClickListener(v->action.run());
return b;
}
private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+0.5f);}
private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_SHORT).show();}
}