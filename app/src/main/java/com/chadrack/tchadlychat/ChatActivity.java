
package com.chadrack.tchadlychat;import android.Manifest;import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.text.*;
import android.util.LruCache;
import android.view.*;
import android.widget.*;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
public class ChatActivity extends Activity{
private static final int PICK_MEDIA=1001,REQUEST_RECORD_AUDIO=2001,REQUEST_NOTIFICATION_PERMISSION=2002,MAX_RETRIES=8;
private static final long MAX_RECORD_TIME=60000L,MAX_VIDEO_SIZE=50L*1024L*1024L,UPLOAD_RETRY_DELAY=5000L;
private static final String NOTIFICATION_SERVER_URL="https://tchadlychat-server.onrender.com/notify";
private static final String VIDEO_UPLOAD_SERVER_URL="https://tchadlychat-server.onrender.com/create-video-upload";
private static final String SUPABASE_URL="https://ecdzacaaayomyqvtwpyo.supabase.co";
private static final String PREF_QUEUE="tchadly_video_queue",KEY_JOBS="jobs";
private static final ExecutorService UPLOAD_EXECUTOR=Executors.newSingleThreadExecutor();
private static final Handler MAIN=new Handler(Looper.getMainLooper());
private static final List<VideoJob> JOBS=new ArrayList<>();
private static final AtomicBoolean WORKER_BUSY=new AtomicBoolean(false);
private static boolean jobsLoaded=false;
private static WeakReference<ChatActivity> ui;
private final ExecutorService notificationExecutor=Executors.newSingleThreadExecutor();
private final ExecutorService mediaExecutor=Executors.newSingleThreadExecutor();
private final Map<String,DocumentSnapshot> sentDocuments=new HashMap<>(),receivedDocuments=new HashMap<>();
private final LruCache<String,Bitmap> cacheImages=new LruCache<>(30);
private ListenerRegistration sentMessagesListener,receivedMessagesListener;
private TextView tvNomUtilisateur;
private EditText etMessage;
private Button btnEnvoyer;
private ImageButton btnImage;
private LinearLayout layoutMessages;
private ScrollView scrollMessages;
private FirebaseAuth firebaseAuth;
private FirebaseFirestore firestore;
private String senderUid,receiverUid,receiverPseudo,monPseudo,editingMessageId;
private MediaRecorder mediaRecorder;
private MediaPlayer lecteurAudio;
private File lecteurFichier;
private String audioPath;
private boolean isRecording=false,premierPlan=false;
private ImageView imageProfil;
private LinearLayout headerChat;
private String photoReceveur;
private LinearLayout uploadBubble;
private ProgressBar uploadProgressBar;
private TextView uploadProgressText;
private Button btnRetryUpload;
private Button btnPauseUpload;
private final Runnable recordTimeoutRunnable=()->{if(isRecording)arreterEnregistrement();};
private static class VideoJob{
String id,uri,mime,senderUid,receiverUid,path,token,sourcePath,state="QUEUED";
long size,retryAt;
volatile long sent;
int attempts;
boolean uploaded;
JSONObject toJson()throws Exception{
JSONObject o=new JSONObject();
o.put("id",id);o.put("uri",uri);o.put("mime",mime);o.put("size",size);
o.put("sender",senderUid);o.put("receiver",receiverUid);
o.put("path",path==null?JSONObject.NULL:path);
o.put("token",token==null?JSONObject.NULL:token);
o.put("source",sourcePath==null?JSONObject.NULL:sourcePath);
o.put("state",state);o.put("attempts",attempts);o.put("sent",sent);o.put("uploaded",uploaded);o.put("retryAt",retryAt);
return o;
}
static VideoJob fromJson(JSONObject o){
VideoJob j=new VideoJob();
j.id=o.optString("id");j.uri=o.optString("uri");j.mime=o.optString("mime","video/mp4");
j.size=o.optLong("size");j.senderUid=o.optString("sender");j.receiverUid=o.optString("receiver");
j.path=o.isNull("path")?null:o.optString("path");
j.token=o.isNull("token")?null:o.optString("token");
j.sourcePath=o.isNull("source")?null:o.optString("source");
j.state=o.optString("state","QUEUED");j.attempts=o.optInt("attempts");
j.sent=o.optLong("sent");j.uploaded=o.optBoolean("uploaded",false);j.retryAt=o.optLong("retryAt");
return j;
}
}
@Override
protected void onCreate(Bundle savedInstanceState){
super.onCreate(savedInstanceState);
setContentView(R.layout.activity_chat);
tvNomUtilisateur=findViewById(R.id.tvNomUtilisateur);
etMessage=findViewById(R.id.etMessage);
btnEnvoyer=findViewById(R.id.btnEnvoyer);
btnImage=findViewById(R.id.btnImage);
layoutMessages=findViewById(R.id.layoutMessages);
scrollMessages=findViewById(R.id.scrollMessages);
if(tvNomUtilisateur==null||etMessage==null||btnEnvoyer==null||btnImage==null||layoutMessages==null||scrollMessages==null){finish();return;}
try{firebaseAuth=FirebaseAuth.getInstance();firestore=FirebaseFirestore.getInstance();}catch(Exception e){finish();return;}
FirebaseUser user=firebaseAuth.getCurrentUser();
if(user==null){finish();return;}
senderUid=user.getUid();
monPseudo=user.getDisplayName();
if(monPseudo==null||monPseudo.trim().isEmpty())monPseudo="Utilisateur";
receiverUid=getIntent().getStringExtra("receiverUid");
receiverPseudo=getIntent().getStringExtra("receiverPseudo");
if(receiverUid==null||receiverUid.trim().isEmpty()||receiverUid.equals(senderUid)){finish();return;}
if(receiverPseudo==null||receiverPseudo.trim().isEmpty())receiverPseudo="Utilisateur";
ui=new WeakReference<>(this);
tvNomUtilisateur.setText(receiverPseudo);
creerHeaderProfil();
chargerProfilUtilisateur();
chargerMonPseudo();
enregistrerTokenFCM();
if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQUEST_NOTIFICATION_PERMISSION);
mettreBoutonEnvoyer();
etMessage.addTextChangedListener(new TextWatcher(){
@Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
@Override public void onTextChanged(CharSequence s,int start,int before,int count){if(!isRecording)mettreBoutonEnvoyer();}
@Override public void afterTextChanged(Editable s){}
});
btnEnvoyer.setOnClickListener(v->{if(isRecording){arreterEnregistrement();return;}String t=etMessage.getText().toString().trim();if(t.isEmpty())commencerEnregistrement();else envoyerMessage();});
btnImage.setOnClickListener(v->choisirMedia());
chargerMessages();
chargerJobs(getApplicationContext());
traiterFile(getApplicationContext());
}
private boolean activityActive(){return !isFinishing()&&!isDestroyed();}
private void toast(String message){afficherToast(getApplicationContext(),message);}
private static void afficherToast(Context ctx,String message){MAIN.post(()->Toast.makeText(ctx,message,Toast.LENGTH_SHORT).show());}
private String safeString(DocumentSnapshot d,String field){try{Object v=d.get(field);if(v instanceof String)return(String)v;}catch(Exception ignored){}return null;}
private boolean safeBoolean(DocumentSnapshot d,String field){try{Object v=d.get(field);if(v instanceof Boolean)return(Boolean)v;}catch(Exception ignored){}return false;}
private long safeTimestamp(DocumentSnapshot d,String field){try{Object v=d.get(field);if(v instanceof Number)return((Number)v).longValue();if(v instanceof Timestamp)return((Timestamp)v).toDate().getTime();}catch(Exception ignored){}return 0L;}
private int dp(float value){return(int)(value*getResources().getDisplayMetrics().density+0.5f);}
private GradientDrawable formeBulle(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
private String formaterHeure(long timestamp){if(timestamp<=0)return "";return new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(timestamp));}
private LinearLayout creerMessageLayout(boolean mien){
LinearLayout layout=new LinearLayout(this);
layout.setOrientation(LinearLayout.VERTICAL);
layout.setGravity(mien?Gravity.END:Gravity.START);
layout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
return layout;
}
private LinearLayout creerBubble(boolean mien){
LinearLayout bubble=new LinearLayout(this);
bubble.setOrientation(LinearLayout.VERTICAL);
bubble.setPadding(dp(16),dp(10),dp(16),dp(8));
bubble.setBackground(formeBulle(mien?Color.rgb(46,180,95):Color.rgb(125,70,180),20));
LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
params.setMargins(dp(8),dp(5),dp(8),dp(2));
bubble.setLayoutParams(params);
return bubble;
}
private void ajouterInfos(LinearLayout bubble,boolean mien,long timestamp,boolean delivered,boolean seen,boolean edited){
LinearLayout infoLayout=new LinearLayout(this);
infoLayout.setOrientation(LinearLayout.HORIZONTAL);
infoLayout.setGravity(Gravity.END);
if(edited){
TextView modifie=new TextView(this);
modifie.setText("Modifié ");
modifie.setTextSize(11);
modifie.setTextColor(Color.argb(210,255,255,255));
infoLayout.addView(modifie);
}
TextView heure=new TextView(this);
heure.setText(formaterHeure(timestamp));
heure.setTextSize(11);
heure.setTextColor(Color.argb(210,255,255,255));
infoLayout.addView(heure);
if(mien){
TextView ticks=new TextView(this);
if(seen){ticks.setText("  ✓✓");ticks.setTextColor(Color.rgb(30,150,255));}
else if(delivered){ticks.setText("  ✓✓");ticks.setTextColor(Color.WHITE);}
else{ticks.setText("  ✓");ticks.setTextColor(Color.WHITE);}
ticks.setTextSize(12);
infoLayout.addView(ticks);
}
bubble.addView(infoLayout);
}
private void afficherMessageSupprime(boolean mien,long timestamp){
LinearLayout layout=creerMessageLayout(mien);
LinearLayout bubble=new LinearLayout(this);
bubble.setOrientation(LinearLayout.VERTICAL);
bubble.setPadding(dp(16),dp(10),dp(16),dp(8));
bubble.setBackground(formeBulle(Color.rgb(95,100,110),20));
TextView texte=new TextView(this);
texte.setText("Message supprimé");
texte.setTextSize(15);
texte.setTextColor(Color.LTGRAY);
texte.setTypeface(null,Typeface.ITALIC);
bubble.addView(texte);
TextView heure=new TextView(this);
heure.setText(formaterHeure(timestamp));
heure.setTextSize(11);
heure.setTextColor(Color.LTGRAY);
heure.setGravity(Gravity.END);
bubble.addView(heure);
layout.addView(bubble);
layoutMessages.addView(layout);
}
private static String obtenirIdTokenStatic(){
try{FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();if(u==null)return null;return Tasks.await(u.getIdToken(false),20,TimeUnit.SECONDS).getToken();}catch(Exception e){return null;}
}
private static void ajouterAuthStatic(HttpURLConnection c){String t=obtenirIdTokenStatic();if(t!=null&&!t.isEmpty())c.setRequestProperty("Authorization","Bearer "+t);}
private void enregistrerTokenFCM(){
if(firestore==null||senderUid==null)return;
FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token->{if(token==null||token.trim().isEmpty())return;Map<String,Object> data=new HashMap<>();data.put("fcmToken",token);firestore.collection("users").document(senderUid).set(data,SetOptions.merge());});
}
private void chargerMonPseudo(){
if(firestore==null||senderUid==null)return;
firestore.collection("users").document(senderUid).get().addOnSuccessListener(d->{String n=safeString(d,"pseudo");if(n!=null&&!n.trim().isEmpty())monPseudo=n;});
}

private Bitmap creerAvatar(String nom){
int taille=144;
Bitmap bmp=Bitmap.createBitmap(taille,taille,Bitmap.Config.ARGB_8888);
Canvas c=new Canvas(bmp);
String lettre=(nom==null||nom.trim().isEmpty())?"?":nom.trim().substring(0,1).toUpperCase(Locale.getDefault());
int[] couleurs={Color.rgb(46,180,95),Color.rgb(125,70,180),Color.rgb(30,120,200),Color.rgb(200,110,40),Color.rgb(180,60,90)};
int idx=Math.abs(nom==null?0:nom.hashCode())%couleurs.length;
Paint fond=new Paint(Paint.ANTI_ALIAS_FLAG);
fond.setColor(couleurs[idx]);
c.drawCircle(taille/2f,taille/2f,taille/2f,fond);
Paint texte=new Paint(Paint.ANTI_ALIAS_FLAG);
texte.setColor(Color.WHITE);
texte.setTextSize(taille*0.5f);
texte.setTextAlign(Paint.Align.CENTER);
texte.setTypeface(Typeface.DEFAULT_BOLD);
Paint.FontMetrics fm=texte.getFontMetrics();
float y=taille/2f-(fm.ascent+fm.descent)/2f;
c.drawText(lettre,taille/2f,y,texte);
return bmp;
}
private void creerHeaderProfil(){
if(tvNomUtilisateur==null)return;
ViewParent p=tvNomUtilisateur.getParent();
if(!(p instanceof LinearLayout))return;
LinearLayout parent=(LinearLayout)p;
int index=parent.indexOfChild(tvNomUtilisateur);
parent.removeView(tvNomUtilisateur);
headerChat=new LinearLayout(this);
headerChat.setOrientation(LinearLayout.HORIZONTAL);
headerChat.setGravity(Gravity.CENTER_VERTICAL);
headerChat.setPadding(dp(12),dp(8),dp(12),dp(8));
imageProfil=new ImageView(this);
imageProfil.setScaleType(ImageView.ScaleType.CENTER_CROP);
LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(48),dp(48));
ip.setMargins(0,0,dp(12),0);
imageProfil.setLayoutParams(ip);
imageProfil.setBackground(formeBulle(Color.LTGRAY,50));
imageProfil.setClipToOutline(true);
imageProfil.setOnClickListener(v->{if(photoReceveur==null||photoReceveur.isEmpty())toast("Pas de photo");else ouvrirPhoto(photoReceveur);});
imageProfil.setImageBitmap(creerAvatar(receiverPseudo));
tvNomUtilisateur.setTextSize(18);
tvNomUtilisateur.setTextColor(Color.YELLOW);
tvNomUtilisateur.setTypeface(null,Typeface.BOLD);
headerChat.addView(imageProfil);
tvNomUtilisateur.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
headerChat.addView(tvNomUtilisateur);
headerChat.addView(creerBoutonAppel("📞","audio"));
headerChat.addView(creerBoutonAppel("🎥","video"));
headerChat.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));
if(index>=0&&index<=parent.getChildCount())parent.addView(headerChat,index);else parent.addView(headerChat,0);
}
private TextView creerBoutonAppel(String texte,String type){
TextView b=new TextView(this);
b.setText(texte);
b.setTextSize(20);
b.setTextColor(Color.WHITE);
b.setGravity(Gravity.CENTER);
b.setBackground(formeBulle(Color.rgb(46,180,95),50));
LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(44),dp(44));
p.setMargins(dp(6),0,0,0);
b.setLayoutParams(p);
b.setOnClickListener(v->demarrerAppel(type));
return b;
}
private void chargerProfilUtilisateur(){
if(receiverUid==null||firestore==null)return;
firestore.collection("users").document(receiverUid).get().addOnSuccessListener(d->{
if(!activityActive()||!d.exists())return;
String nom=safeString(d,"pseudo");
if(nom==null||nom.trim().isEmpty())nom=safeString(d,"name");
if(nom==null||nom.trim().isEmpty())nom=safeString(d,"username");
if(nom==null||nom.trim().isEmpty())nom=receiverPseudo;
receiverPseudo=nom;
tvNomUtilisateur.setText(nom);
if(imageProfil!=null)imageProfil.setImageBitmap(creerAvatar(nom));
String base64Photo=safeString(d,"photoBase64");
photoReceveur=base64Photo;
if(base64Photo!=null&&!base64Photo.trim().isEmpty()){chargerImageProfilBase64(base64Photo);return;}
String photo=safeString(d,"photoUrl");
if(photo==null||photo.trim().isEmpty())photo=safeString(d,"photoURL");
if(photo==null||photo.trim().isEmpty())photo=safeString(d,"profileImage");
if(photo==null||photo.trim().isEmpty())photo=safeString(d,"imageUrl");
if(photo!=null&&!photo.trim().isEmpty())chargerImageProfil(photo);
});
}

private void chargerImageProfilBase64(String encoded){
new Thread(()->{
Bitmap b=decoderBase64(encoded,256);
if(b!=null)runOnUiThread(()->{if(activityActive()&&imageProfil!=null)imageProfil.setImageBitmap(b);else b.recycle();});
}).start();
}
private void chargerImageProfil(String link){
new Thread(()->{
HttpURLConnection c=null;
try{
c=(HttpURLConnection)new URL(link).openConnection();
c.setConnectTimeout(10000);c.setReadTimeout(10000);
if(c.getResponseCode()<200||c.getResponseCode()>=300)return;
BitmapFactory.Options o=new BitmapFactory.Options();o.inPreferredConfig=Bitmap.Config.RGB_565;
Bitmap b;
try(InputStream in=c.getInputStream()){b=BitmapFactory.decodeStream(in,null,o);}
if(b!=null)runOnUiThread(()->{if(activityActive()&&imageProfil!=null)imageProfil.setImageBitmap(b);else b.recycle();});
}catch(Exception ignored){}finally{if(c!=null)c.disconnect();}
}).start();
}
private Bitmap decoderBase64(String encoded,int max){
try{
byte[] bytes=android.util.Base64.decode(encoded,android.util.Base64.DEFAULT);
BitmapFactory.Options o=new BitmapFactory.Options();
o.inJustDecodeBounds=true;
BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
int s=1;
while(o.outWidth/s>max||o.outHeight/s>max)s*=2;
o.inJustDecodeBounds=false;o.inSampleSize=s;o.inPreferredConfig=Bitmap.Config.RGB_565;
return BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
}catch(Exception|OutOfMemoryError e){return null;}
}
private Bitmap obtenirMiniature(String id,String encoded){
Bitmap b=cacheImages.get(id);
if(b!=null)return b;
b=decoderBase64(encoded,600);
if(b!=null)cacheImages.put(id,b);
return b;
}
private void mettreBoutonEnvoyer(){
if(btnEnvoyer==null||etMessage==null)return;
if(isRecording){btnEnvoyer.setText("⏹");btnEnvoyer.setContentDescription("Arrêter l'enregistrement");}
else if(etMessage.getText().toString().trim().isEmpty()){btnEnvoyer.setText("🎙");btnEnvoyer.setContentDescription("Enregistrer un message vocal");}
else{btnEnvoyer.setText(editingMessageId!=null?"Modifier":"Envoyer");btnEnvoyer.setContentDescription("Envoyer le message");}
}
private void envoyerMessage(){
if(firestore==null||senderUid==null||receiverUid==null)return;
String texte=etMessage.getText().toString().trim();
if(texte.isEmpty())return;
if(editingMessageId!=null){
String id=editingMessageId;
firestore.collection("private_messages").document(id).update("text",texte,"edited",true,"editedAt",Timestamp.now()).addOnSuccessListener(v->{if(!activityActive())return;editingMessageId=null;etMessage.setText("");mettreBoutonEnvoyer();}).addOnFailureListener(e->toast("Modification échouée."));
return;
}
Map<String,Object> m=new HashMap<>();
m.put("sender",senderUid);m.put("receiver",receiverUid);m.put("text",texte);m.put("type","text");m.put("timestamp",Timestamp.now());m.put("delivered",false);m.put("seen",false);m.put("deleted",false);
firestore.collection("private_messages").add(m).addOnSuccessListener(ref->{if(activityActive())etMessage.setText("");envoyerNotification(ref.getId());}).addOnFailureListener(e->toast("Envoi échoué. Vérifiez la connexion."));
}
private void envoyerNotification(String messageId){
if(messageId==null||messageId.trim().isEmpty())return;
try{notificationExecutor.execute(()->notifierServeur(messageId));}catch(Exception ignored){}
}
private void demarrerAppel(String type){
if(!permissionsAppelOk())return;
if(firestore==null||senderUid==null||receiverUid==null)return;
final String channel="call_"+UUID.randomUUID().toString().replace("-","");
Map<String,Object> appel=new HashMap<>();
appel.put("caller",senderUid);
appel.put("callee",receiverUid);
appel.put("type",type);
appel.put("status","ringing");
appel.put("channel",channel);
appel.put("createdAt",Timestamp.now());
firestore.collection("calls").add(appel).addOnSuccessListener(ref->{
notificationExecutor.execute(()->notifierAppel(ref.getId()));
Intent i=new Intent(ChatActivity.this,CallActivity.class);
i.putExtra("callId",ref.getId());
i.putExtra("channel",channel);
i.putExtra("type",type);
i.putExtra("peerName",receiverPseudo);
i.putExtra("role","caller");
i.putExtra("peerUid",receiverUid);
startActivity(i);
}).addOnFailureListener(e->toast("Appel impossible. Vérifiez la connexion."));
}
/*private void demarrerAppel(String type){
if(!permissionsAppelOk())return;
if(firestore==null||senderUid==null||receiverUid==null)return;
Map<String,Object> appel=new HashMap<>();
appel.put("caller",senderUid);
appel.put("callee",receiverUid);
appel.put("type",type);
appel.put("status","ringing");
appel.put("channel","call_"+UUID.randomUUID().toString().replace("-",""));
appel.put("createdAt",Timestamp.now());
firestore.collection("calls").add(appel)
.addOnSuccessListener(ref->toast("Appel en cours..."))
.addOnFailureListener(e->toast("Appel impossible. Vérifiez la connexion."));
}*/
private static void notifierServeur(String messageId){
HttpURLConnection c=null;
try{
c=(HttpURLConnection)new URL(NOTIFICATION_SERVER_URL).openConnection();
c.setRequestMethod("POST");c.setConnectTimeout(60000);c.setReadTimeout(60000);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
ajouterAuthStatic(c);
JSONObject j=new JSONObject();j.put("messageId",messageId);
byte[] b=j.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);
try(OutputStream out=c.getOutputStream()){out.write(b);}
c.getResponseCode();
}catch(Exception ignored){}finally{if(c!=null)c.disconnect();}
}
private LinearLayout barreRec;
private TextView tvChrono,btnRecSupprimer,btnRecPause;
private boolean enPause=false;
private long debutRec=0,cumulRec=0;
private final Runnable chronoRunnable=new Runnable(){
@Override
public void run(){
if(!isRecording)return;
long t=cumulRec+(enPause?0:System.currentTimeMillis()-debutRec);
if(tvChrono!=null)tvChrono.setText("● "+formatTemps(t));
MAIN.postDelayed(this,500L);
}
};
private String formatTemps(long ms){long s=ms/1000;return String.format(Locale.FRANCE,"%02d:%02d",s/60,s%60);}
private Handler uploadHandler(){return MAIN;}
private void afficherBarreRec(){
if(barreRec!=null)return;
LinearLayout parent=(LinearLayout)etMessage.getParent();
int idx=parent.indexOfChild(etMessage);
etMessage.setVisibility(View.GONE);
btnImage.setVisibility(View.GONE);
barreRec=new LinearLayout(this);
barreRec.setOrientation(LinearLayout.HORIZONTAL);
barreRec.setGravity(Gravity.CENTER_VERTICAL);
barreRec.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
btnRecSupprimer=new TextView(this);
btnRecSupprimer.setText("🗑");
btnRecSupprimer.setTextSize(24);
btnRecSupprimer.setPadding(dp(12),dp(8),dp(12),dp(8));
btnRecSupprimer.setOnClickListener(v->supprimerEnregistrement());
tvChrono=new TextView(this);
tvChrono.setText("● 00:00");
tvChrono.setTextColor(Color.rgb(255,80,80));
tvChrono.setTextSize(16);
btnRecPause=new TextView(this);
btnRecPause.setText("⏸");
btnRecPause.setTextSize(24);
btnRecPause.setPadding(dp(12),dp(8),dp(12),dp(8));
btnRecPause.setOnClickListener(v->basculerPause());
barreRec.addView(btnRecSupprimer);
barreRec.addView(tvChrono,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
barreRec.addView(btnRecPause);
parent.addView(barreRec,idx);
}
private void masquerBarreRec(){
MAIN.removeCallbacks(chronoRunnable);
if(barreRec!=null&&barreRec.getParent()!=null)((ViewGroup)barreRec.getParent()).removeView(barreRec);
barreRec=null;tvChrono=null;btnRecSupprimer=null;btnRecPause=null;
etMessage.setVisibility(View.VISIBLE);
btnImage.setVisibility(View.VISIBLE);
}
private void basculerPause(){
if(mediaRecorder==null||!isRecording)return;
try{
if(enPause){
mediaRecorder.resume();
debutRec=System.currentTimeMillis();
enPause=false;
btnRecPause.setText("⏸");
}else{
mediaRecorder.pause();
cumulRec+=System.currentTimeMillis()-debutRec;
enPause=true;
btnRecPause.setText("▶");
}
}catch(Exception e){toast("Pause impossible.");}
}
private void supprimerEnregistrement(){
isRecording=false;
uploadHandler().removeCallbacks(recordTimeoutRunnable);
try{if(mediaRecorder!=null)mediaRecorder.stop();}catch(Exception ignored){}
libererMediaRecorder();
if(audioPath!=null)try{new File(audioPath).delete();}catch(Exception ignored){}
masquerBarreRec();
mettreBoutonEnvoyer();
toast("Enregistrement supprimé.");
}
private void commencerEnregistrement(){
if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_RECORD_AUDIO);return;}
try{
File f=new File(getCacheDir(),"audio_"+System.currentTimeMillis()+".m4a");
audioPath=f.getAbsolutePath();
mediaRecorder=Build.VERSION.SDK_INT>=31?new MediaRecorder(this):new MediaRecorder();
mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
mediaRecorder.setAudioEncodingBitRate(64000);
mediaRecorder.setAudioSamplingRate(44100);
mediaRecorder.setOutputFile(audioPath);
mediaRecorder.prepare();
mediaRecorder.start();
isRecording=true;
enPause=false;
cumulRec=0;
debutRec=System.currentTimeMillis();
mettreBoutonEnvoyer();
afficherBarreRec();
MAIN.post(chronoRunnable);
uploadHandler().removeCallbacks(recordTimeoutRunnable);
uploadHandler().postDelayed(recordTimeoutRunnable,MAX_RECORD_TIME);
}catch(Exception e){isRecording=false;libererMediaRecorder();toast("Impossible de démarrer l'enregistrement.");}
}
private void arreterEnregistrement(){
if(!isRecording)return;
isRecording=false;
uploadHandler().removeCallbacks(recordTimeoutRunnable);
boolean ok=true;
try{
if(enPause&&mediaRecorder!=null)mediaRecorder.resume();
if(mediaRecorder!=null)mediaRecorder.stop();
}catch(Exception e){ok=false;}
libererMediaRecorder();
masquerBarreRec();
mettreBoutonEnvoyer();
if(audioPath==null)return;
File f=new File(audioPath);
if(!ok||!f.exists()||f.length()<100){f.delete();toast("Enregistrement trop court.");return;}
envoyerAudio(f);
}
private void annulerEnregistrement(){
isRecording=false;
uploadHandler().removeCallbacks(recordTimeoutRunnable);
MAIN.removeCallbacks(chronoRunnable);
try{if(mediaRecorder!=null)mediaRecorder.stop();}catch(Exception ignored){}
libererMediaRecorder();
if(audioPath!=null)try{new File(audioPath).delete();}catch(Exception ignored){}
}
private void libererMediaRecorder(){
try{if(mediaRecorder!=null)mediaRecorder.reset();}catch(Exception ignored){}
try{if(mediaRecorder!=null)mediaRecorder.release();}catch(Exception ignored){}
mediaRecorder=null;
}
private void exporterEtAfficher(String url){
String nom=url.substring(url.lastIndexOf('/')+1);
String chemin=ExportVideo.exporter(getApplicationContext(),url,nom);
MAIN.post(()->{
toast(chemin==null?"Enregistrement impossible.":"Vidéo enregistrée : "+chemin);
if(activityActive())afficherMessagesCombines();
});
}
private void telechargerVideo(String url){
toast("Téléchargement en cours...");
if(VideoCache.existe(getApplicationContext(),url)){mediaExecutor.execute(()->exporterEtAfficher(url));return;}
VideoCache.telecharger(getApplicationContext(),url,new VideoCache.Callback(){
@Override
public void onFini(java.io.File f){exporterEtAfficher(url);}
@Override
public void onErreur(String msg){MAIN.post(()->toast("Téléchargement échoué."));}
});
}
private static void notifierAppel(String callId){
HttpURLConnection c=null;
try{
c=(HttpURLConnection)new URL("https://tchadlychat-server.onrender.com/notify-call").openConnection();
c.setRequestMethod("POST");c.setConnectTimeout(60000);c.setReadTimeout(60000);c.setDoOutput(true);
c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
ajouterAuthStatic(c);
JSONObject j=new JSONObject();j.put("callId",callId);
byte[] b=j.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(b.length);
try(OutputStream out=c.getOutputStream()){out.write(b);}
c.getResponseCode();
}catch(Exception ignored){}finally{if(c!=null)c.disconnect();}
}
private void envoyerAudio(File audio){
if(firestore==null||senderUid==null||receiverUid==null||audio==null||!audio.exists())return;
try{mediaExecutor.execute(()->{
try{
byte[] bytes;
try(InputStream in=new FileInputStream(audio);ByteArrayOutputStream out=new ByteArrayOutputStream()){
byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);bytes=out.toByteArray();
}
String encoded=android.util.Base64.encodeToString(bytes,android.util.Base64.NO_WRAP);
long dureeMs=0;try{MediaMetadataRetriever mr=new MediaMetadataRetriever();mr.setDataSource(audio.getAbsolutePath());String dd=mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);mr.release();if(dd!=null)dureeMs=Long.parseLong(dd);}catch(Exception ignored){}
if(encoded.length()>900000){toast("Message vocal trop long.");return;}
Map<String,Object> m=new HashMap<>();
m.put("sender",senderUid);m.put("receiver",receiverUid);m.put("type","audio");m.put("audioBase64",encoded);m.put("audioDuree",dureeMs);m.put("text","Message vocal");m.put("timestamp",Timestamp.now());m.put("delivered",false);m.put("seen",false);m.put("deleted",false);
runOnUiThread(()->firestore.collection("private_messages").add(m).addOnSuccessListener(r->envoyerNotification(r.getId())).addOnFailureListener(e->toast("Échec de l'envoi audio.")));
}catch(Exception e){toast("Impossible de lire l'audio.");}finally{audio.delete();}
});}catch(Exception e){toast("Service d'envoi indisponible.");}
}
/*@Override
public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
super.onRequestPermissionsResult(requestCode,permissions,results);
if(requestCode==REQUEST_RECORD_AUDIO){if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)commencerEnregistrement();else toast("Autorisation du microphone refusée.");}
}*/
@Override
public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
super.onRequestPermissionsResult(requestCode,permissions,grantResults);
if(requestCode==REQUEST_RECORD_AUDIO){
if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)commencerEnregistrement();
else toast("Autorisation du microphone refusée.");
}
if(requestCode==3001){
boolean ok=true;
for(int r:grantResults)if(r!=PackageManager.PERMISSION_GRANTED)ok=false;
if(!ok)toast("Autorisé le micro et lecaméra pour appeler.");
}
}
private void choisirMedia(){
Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
i.addCategory(Intent.CATEGORY_OPENABLE);
i.setType("*/*");
i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","video/*"});
i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
try{startActivityForResult(i,PICK_MEDIA);}catch(Exception e){toast("Impossible d'ouvrir les fichiers.");}
}
@Override
protected void onActivityResult(int requestCode,int resultCode,Intent data){
super.onActivityResult(requestCode,resultCode,data);
if(requestCode!=PICK_MEDIA||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
Uri uri=data.getData();
try{int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);getContentResolver().takePersistableUriPermission(uri,flags);}catch(Exception ignored){}
String mime=getContentResolver().getType(uri);
if(mime==null)mime="";
if(mime.startsWith("video/"))envoyerVideo(uri,mime);
else if(mime.startsWith("image/"))envoyerImage(uri,mime);
else toast("Choisissez une image ou une vidéo.");
}
private long obtenirTailleUri(Uri uri){
if(uri==null)return-1;
try(Cursor c=getContentResolver().query(uri,new String[]{OpenableColumns.SIZE},null,null,null)){
if(c!=null&&c.moveToFirst()){int i=c.getColumnIndex(OpenableColumns.SIZE);if(i>=0&&!c.isNull(i))return c.getLong(i);}
}catch(Exception ignored){}
try(InputStream in=getContentResolver().openInputStream(uri)){
if(in==null)return-1;
long size=0;byte[] b=new byte[32768];int n;
while((n=in.read(b))!=-1){size+=n;if(size>MAX_VIDEO_SIZE)return size;}
return size;
}catch(Exception e){return-1;}
}
private void envoyerVideo(Uri uri,String mime){
if(uri==null||firestore==null||senderUid==null||receiverUid==null)return;
long size=obtenirTailleUri(uri);
if(size<=0){toast("Impossible de lire la taille de la vidéo.");return;}
if(size>MAX_VIDEO_SIZE){toast("La vidéo dépasse la limite de 50 Mo.");return;}
VideoJob job=new VideoJob();
job.id=firestore.collection("private_messages").document().getId();
job.uri=uri.toString();
job.mime=mime==null||mime.isEmpty()?"video/mp4":mime;
job.size=size;
job.senderUid=senderUid;
job.receiverUid=receiverUid;
job.state="QUEUED";
synchronized(JOBS){JOBS.add(job);}
sauvegarderJobs(getApplicationContext());
toast("Vidéo ajoutée à l'envoi.");
rafraichirBulleUpload();
traiterFile(getApplicationContext());
}
private static void chargerJobs(Context ctx){
if(jobsLoaded)return;
jobsLoaded=true;
try{
String s=ctx.getSharedPreferences(PREF_QUEUE,Context.MODE_PRIVATE).getString(KEY_JOBS,"[]");
JSONArray a=new JSONArray(s);
synchronized(JOBS){
JOBS.clear();
for(int i=0;i<a.length();i++){
VideoJob j=VideoJob.fromJson(a.getJSONObject(i));
if("UPLOADING".equals(j.state)||"SAVING".equals(j.state))j.state="QUEUED";
JOBS.add(j);
}
}
}catch(Exception ignored){}
}
private static void sauvegarderJobs(Context ctx){
try{
    JSONArray a=new JSONArray();
synchronized(JOBS){for(VideoJob j:JOBS)a.put(j.toJson());}
ctx.getSharedPreferences(PREF_QUEUE,Context.MODE_PRIVATE).edit().putString(KEY_JOBS,a.toString()).apply();
}catch(Exception ignored){}
}
private static VideoJob prochainJob(){
long now=System.currentTimeMillis();
synchronized(JOBS){
for(VideoJob j:JOBS)if("QUEUED".equals(j.state)&&j.retryAt<=now)return j;
}
return null;
}
private static long delaiProchain(){
long now=System.currentTimeMillis();
long best=-1;
synchronized(JOBS){
for(VideoJob j:JOBS)if("QUEUED".equals(j.state)&&j.retryAt>now){long d=j.retryAt-now;if(best<0||d<best)best=d;}
}
return best;
}
private static void traiterFile(Context ctx){
if(WORKER_BUSY.get())return;
VideoJob job=prochainJob();
if(job==null){
long d=delaiProchain();
if(d>0)MAIN.postDelayed(()->traiterFile(ctx),d);
return;
}
if(!WORKER_BUSY.compareAndSet(false,true))return;
job.state="UPLOADING";
sauvegarderJobs(ctx);
notifierUi();
UPLOAD_EXECUTOR.execute(()->{
try{executerJob(ctx,job);}
finally{WORKER_BUSY.set(false);MAIN.post(()->traiterFile(ctx));}
});
}
private static void executerJob(Context ctx,VideoJob job){
try{
if(job.sourcePath==null){job.sourcePath=copierSource(ctx,job);sauvegarderJobs(ctx);}
if(!job.uploaded){
if(job.path==null||job.token==null){creerSession(job);sauvegarderJobs(ctx);}
uploadFichier(job);
job.uploaded=true;
sauvegarderJobs(ctx);
}
job.state="SAVING";
sauvegarderJobs(ctx);
notifierUi();
enregistrerMessageVideo(job);
notifierServeur(job.id);VideoCache.copierLocal(ctx,SUPABASE_URL+"/storage/v1/object/public/videos/"+job.path,new File(job.sourcePath));
File f=new File(job.sourcePath);
if(f.exists())f.delete();
synchronized(JOBS){JOBS.remove(job);}
sauvegarderJobs(ctx);
notifierUi();
afficherToast(ctx,"Vidéo envoyée.");
}catch(Exception e){
android.util.Log.e("VIDEO_UPLOAD","Echec envoi video: "+e.getMessage(),e);
if("PAUSED".equals(job.state)){sauvegarderJobs(ctx);notifierUi();return;}
job.attempts++;
if(!job.uploaded){job.path=null;job.token=null;}
if(job.attempts>=MAX_RETRIES){job.state="FAILED";}
else{job.state="QUEUED";job.retryAt=System.currentTimeMillis()+UPLOAD_RETRY_DELAY*Math.min(job.attempts,6);}
sauvegarderJobs(ctx);
notifierUi();
}
}
private static String copierSource(Context ctx,VideoJob job)throws IOException{
File target=new File(ctx.getCacheDir(),"upload_"+job.id+".tmp");
try(InputStream in=ctx.getContentResolver().openInputStream(Uri.parse(job.uri));OutputStream out=new FileOutputStream(target)){
if(in==null)throw new IOException("Source inaccessible");
byte[] b=new byte[65536];long total=0;int n;
while((n=in.read(b))!=-1){out.write(b,0,n);total+=n;}
out.flush();
if(total!=job.size)throw new IOException("Taille du fichier modifiée");
}
return target.getAbsolutePath();
}
private static String lireFlux(InputStream in)throws IOException{
if(in==null)return "";
try(InputStream i=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
byte[] b=new byte[4096];int n;
while((n=i.read(b))!=-1)out.write(b,0,n);
return new String(out.toByteArray(),StandardCharsets.UTF_8);
}
}
private boolean permissionsAppelOk(){
String[] besoins={Manifest.permission.RECORD_AUDIO,Manifest.permission.CAMERA};
List<String> manquantes=new ArrayList<>();
for(String p:besoins){
if(checkSelfPermission(p)!=PackageManager.PERMISSION_GRANTED)manquantes.add(p);
}
if(manquantes.isEmpty())return true;
requestPermissions(manquantes.toArray(new String[0]),3001);
return false;
}
/*@Override
public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
super.onRequestPermissionsResult(requestCode,permissions,grantResults);
if(requestCode==3001){
boolean ok=true;
for(int r:grantResults)if(r!=PackageManager.PERMISSION_GRANTED)ok=false;
if(!ok)toast("Ukeneye uburenganzira bwa micro na caméra kugira ngo uhamagare.");
}
}*/
private static void creerSession(VideoJob job)throws Exception{
HttpURLConnection c=(HttpURLConnection)new URL(VIDEO_UPLOAD_SERVER_URL).openConnection();
try{
c.setRequestMethod("POST");c.setConnectTimeout(30000);c.setReadTimeout(60000);c.setDoOutput(true);
c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
ajouterAuthStatic(c);
JSONObject body=new JSONObject();
body.put("userId",job.senderUid);body.put("receiverUid",job.receiverUid);body.put("fileSize",job.size);body.put("mimeType",job.mime);body.put("fileName",new File(job.sourcePath).getName());
byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);
c.setFixedLengthStreamingMode(bytes.length);
try(OutputStream out=c.getOutputStream()){out.write(bytes);}
int code=c.getResponseCode();
String text=lireFlux(code>=200&&code<300?c.getInputStream():c.getErrorStream());
if(code<200||code>=300)throw new IOException("Session HTTP "+code+" "+text);
JSONObject s=new JSONObject(text);
if(!s.optBoolean("success",false))throw new IOException("Session refusée: "+s.optString("error"));
job.path=s.getString("path");
job.token=s.getString("token");
}finally{c.disconnect();}
}
private static void uploadFichier(VideoJob job)throws Exception{
File f=new File(job.sourcePath);
if(!f.exists()||f.length()!=job.size)throw new IOException("Fichier temporaire introuvable");
String url=SUPABASE_URL+"/storage/v1/object/upload/sign/videos/"+job.path+"?token="+URLEncoder.encode(job.token,"UTF-8");
HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
try{
c.setRequestMethod("PUT");c.setConnectTimeout(30000);c.setReadTimeout(180000);c.setDoOutput(true);c.setUseCaches(false);
c.setFixedLengthStreamingMode(job.size);
c.setRequestProperty("Content-Type",job.mime);
c.setRequestProperty("x-upsert","true");
long sent=0,derniere=0;
try(InputStream in=new FileInputStream(f);OutputStream out=c.getOutputStream()){
byte[] b=new byte[65536];int n;
while((n=in.read(b))!=-1){
out.write(b,0,n);
sent+=n;
if("PAUSED".equals(job.state))throw new IOException("PAUSE");
if(sent-derniere>=262144||sent==job.size){derniere=sent;job.sent=sent;notifierUi();}
}
out.flush();
}
int code=c.getResponseCode();
String rep=lireFlux(code>=200&&code<300?c.getInputStream():c.getErrorStream());
if(code<200||code>=300)throw new IOException("Upload HTTP "+code+" "+rep);
job.sent=job.size;
}finally{c.disconnect();}
}
private static void enregistrerMessageVideo(VideoJob job)throws Exception{
String videoUrl=SUPABASE_URL+"/storage/v1/object/public/videos/"+job.path;
Map<String,Object> m=new HashMap<>();
m.put("sender",job.senderUid);m.put("receiver",job.receiverUid);m.put("text","Vidéo");m.put("type","video");
m.put("videoUrl",videoUrl);m.put("videoPath",job.path);m.put("videoMimeType",job.mime);m.put("videoSize",job.size);
m.put("timestamp",Timestamp.now());m.put("delivered",false);m.put("seen",false);m.put("deleted",false);
Tasks.await(FirebaseFirestore.getInstance().collection("private_messages").document(job.id).set(m),30,TimeUnit.SECONDS);
}
private static void notifierUi(){
WeakReference<ChatActivity> r=ui;
ChatActivity a=r==null?null:r.get();
if(a!=null)MAIN.post(()->a.rafraichirBulleUpload());
}
private VideoJob jobAffiche(){
if(senderUid==null||receiverUid==null)return null;
synchronized(JOBS){
for(VideoJob j:JOBS)if(senderUid.equals(j.senderUid)&&receiverUid.equals(j.receiverUid))return j;
}
return null;
}
private int nombreEnFile(){
int n=0;
if(senderUid==null||receiverUid==null)return 0;
synchronized(JOBS){
for(VideoJob j:JOBS)if(senderUid.equals(j.senderUid)&&receiverUid.equals(j.receiverUid))n++;
}
return n;
}
private void reessayerJob(){
synchronized(JOBS){
for(VideoJob j:JOBS)if("FAILED".equals(j.state)){j.state="QUEUED";j.attempts=0;j.retryAt=0;}
}
sauvegarderJobs(getApplicationContext());
rafraichirBulleUpload();
traiterFile(getApplicationContext());
}
private void attacherBulleUpload(){
if(uploadBubble==null||layoutMessages==null||uploadBubble.getParent()!=null)return;
layoutMessages.addView(uploadBubble);
}
private void masquerBulleUpload(){
if(uploadBubble!=null&&uploadBubble.getParent()!=null)((ViewGroup)uploadBubble.getParent()).removeView(uploadBubble);
uploadBubble=null;uploadProgressBar=null;uploadProgressText=null;btnRetryUpload=null;btnPauseUpload=null;
}
private void creerBulleUpload(){
LinearLayout bulle=new LinearLayout(this);
bulle.setOrientation(LinearLayout.VERTICAL);
bulle.setPadding(dp(16),dp(10),dp(16),dp(8));
bulle.setBackground(formeBulle(Color.rgb(46,180,95),20));
int largeur=getResources().getDisplayMetrics().widthPixels;
bulle.setLayoutParams(new LinearLayout.LayoutParams(Math.min((int)(largeur*0.75f),largeur-dp(64)),-2));
TextView titre=new TextView(this);
titre.setText("Envoi de la vidéo");
titre.setTextColor(Color.WHITE);
titre.setTextSize(14);
titre.setTypeface(null,Typeface.BOLD);
bulle.addView(titre);
uploadProgressBar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
uploadProgressBar.setMax(1000);
LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(-1,dp(6));
pp.topMargin=dp(8);
bulle.addView(uploadProgressBar,pp);
uploadProgressText=new TextView(this);
uploadProgressText.setTextColor(Color.argb(210,255,255,255));
uploadProgressText.setTextSize(12);
uploadProgressText.setPadding(0,dp(6),0,dp(6));
bulle.addView(uploadProgressText);
LinearLayout boutons=new LinearLayout(this);
boutons.setOrientation(LinearLayout.HORIZONTAL);
boutons.setGravity(Gravity.END);
btnPauseUpload=new Button(this);
btnPauseUpload.setAllCaps(false);
btnPauseUpload.setTextSize(12);
btnPauseUpload.setOnClickListener(v->{VideoJob j=jobAffiche();if(j!=null&&"PAUSED".equals(j.state))reprendreJob();else mettreEnPause();});
boutons.addView(btnPauseUpload);
btnRetryUpload=new Button(this);
btnRetryUpload.setAllCaps(false);
btnRetryUpload.setTextSize(12);
btnRetryUpload.setText("↻ Réessayer");
btnRetryUpload.setVisibility(View.GONE);
btnRetryUpload.setOnClickListener(v->reessayerJob());
boutons.addView(btnRetryUpload);
bulle.addView(boutons);
LinearLayout row=new LinearLayout(this);
row.setOrientation(LinearLayout.HORIZONTAL);
row.setGravity(Gravity.END);
LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);
rp.setMargins(dp(8),dp(5),dp(8),dp(2));
row.setLayoutParams(rp);
row.addView(bulle);
uploadBubble=row;
}
private void rafraichirBulleUpload(){
if(!activityActive())return;
VideoJob job=jobAffiche();
if(job==null){masquerBulleUpload();return;}
if(uploadBubble==null)creerBulleUpload();
attacherBulleUpload();
long total=Math.max(1L,job.size);
long sent=Math.min(job.sent,total);
boolean pause="PAUSED".equals(job.state);
String etat;
if("FAILED".equals(job.state))etat="Échec de l'envoi";
else if(pause)etat="En pause";
else if("SAVING".equals(job.state))etat="Enregistrement du message…";
else if("QUEUED".equals(job.state)&&job.retryAt>System.currentTimeMillis())etat="Nouvelle tentative bientôt…";
else if("QUEUED".equals(job.state))etat="En attente…";
else etat="Envoi de la vidéo…";
btnRetryUpload.setVisibility("FAILED".equals(job.state)?View.VISIBLE:View.GONE);
btnPauseUpload.setVisibility(("FAILED".equals(job.state)||"SAVING".equals(job.state))?View.GONE:View.VISIBLE);
btnPauseUpload.setText(pause?"▶ Reprendre":"⏸ Pause");
int nb=nombreEnFile();
String pourcentage=String.format(Locale.FRANCE,"%.1f%%",sent*100.0/total);
String taille=String.format(Locale.FRANCE,"%.2f / %.2f Mo",sent/1048576.0,total/1048576.0);
String ligneFile=nb>1?"\n"+nb+" vidéos dans la file":"";
uploadProgressBar.setProgress((int)Math.min(1000L,sent*1000L/total));
uploadProgressText.setText(etat+" · "+pourcentage+"\n"+taille+ligneFile);
}
private void mettreEnPause(){
VideoJob job=jobAffiche();
if(job==null)return;
synchronized(JOBS){if("UPLOADING".equals(job.state)||"QUEUED".equals(job.state))job.state="PAUSED";}
sauvegarderJobs(getApplicationContext());
rafraichirBulleUpload();
toast("Envoi en pause.");
}
private void reprendreJob(){
VideoJob job=jobAffiche();
if(job==null)return;
synchronized(JOBS){if("PAUSED".equals(job.state)){job.state="QUEUED";job.attempts=0;job.retryAt=0;}}
sauvegarderJobs(getApplicationContext());
rafraichirBulleUpload();
traiterFile(getApplicationContext());
}
private void envoyerImage(Uri uri,String mime){
if(uri==null||firestore==null||senderUid==null||receiverUid==null)return;
final String cible=receiverUid;
try{mediaExecutor.execute(()->{
try{
BitmapFactory.Options options=new BitmapFactory.Options();
options.inJustDecodeBounds=true;
try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Image inaccessible");BitmapFactory.decodeStream(in,null,options);}
options.inSampleSize=1;
while(options.outWidth/options.inSampleSize>1280||options.outHeight/options.inSampleSize>1280)options.inSampleSize*=2;
options.inJustDecodeBounds=false;
Bitmap bitmap;
try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Image inaccessible");bitmap=BitmapFactory.decodeStream(in,null,options);}
if(bitmap==null)throw new IOException("Image invalide");
ByteArrayOutputStream out=new ByteArrayOutputStream();
bitmap.compress(Bitmap.CompressFormat.JPEG,75,out);
bitmap.recycle();
String encoded=android.util.Base64.encodeToString(out.toByteArray(),android.util.Base64.NO_WRAP);
if(encoded.length()>900000){toast("Image trop volumineuse. Choisissez une image plus petite.");return;}
Map<String,Object> m=new HashMap<>();
m.put("sender",senderUid);m.put("receiver",cible);m.put("text","Image");m.put("type","image");m.put("imageBase64",encoded);m.put("timestamp",Timestamp.now());m.put("delivered",false);m.put("seen",false);m.put("deleted",false);
runOnUiThread(()->firestore.collection("private_messages").add(m).addOnSuccessListener(r->envoyerNotification(r.getId())).addOnFailureListener(e->toast("Échec de l'envoi de l'image.")));
}catch(Exception|OutOfMemoryError e){toast("Impossible de préparer l'image.");}
});}catch(Exception e){toast("Service d'envoi indisponible.");}
}
private void afficherMessagesCombines(){
if(!activityActive())return;
Map<String,DocumentSnapshot> combined=new HashMap<>();
synchronized(sentDocuments){combined.putAll(sentDocuments);}
synchronized(receivedDocuments){combined.putAll(receivedDocuments);}
List<DocumentSnapshot> list=new ArrayList<>(combined.values());
Collections.sort(list,Comparator.comparingLong(d->safeTimestamp(d,"timestamp")));
runOnUiThread(()->{
if(!activityActive()||layoutMessages==null)return;
layoutMessages.removeAllViews();
for(DocumentSnapshot d:list)afficherBulleMessage(d);
rafraichirBulleUpload();
scrollMessages.post(()->scrollMessages.fullScroll(View.FOCUS_DOWN));
});
}
private void chargerMessages(){
if(firestore==null||senderUid==null||receiverUid==null)return;
if(sentMessagesListener!=null)sentMessagesListener.remove();
if(receivedMessagesListener!=null)receivedMessagesListener.remove();
Query sent=firestore.collection("private_messages").whereEqualTo("sender",senderUid).whereEqualTo("receiver",receiverUid);
Query received=firestore.collection("private_messages").whereEqualTo("sender",receiverUid).whereEqualTo("receiver",senderUid);
sentMessagesListener=sent.addSnapshotListener((s,e)->{
if(e!=null||s==null)return;
synchronized(sentDocuments){sentDocuments.clear();for(DocumentSnapshot d:s.getDocuments())sentDocuments.put(d.getId(),d);}
afficherMessagesCombines();
});
receivedMessagesListener=received.addSnapshotListener((s,e)->{
if(e!=null||s==null)return;
synchronized(receivedDocuments){receivedDocuments.clear();for(DocumentSnapshot d:s.getDocuments())receivedDocuments.put(d.getId(),d);}
afficherMessagesCombines();
marquerRecusCommeVus();
});
}
private void marquerRecusCommeVus(){
if(!premierPlan||firestore==null)return;
List<DocumentSnapshot> liste;
synchronized(receivedDocuments){liste=new ArrayList<>(receivedDocuments.values());}
for(DocumentSnapshot d:liste){
if(safeBoolean(d,"seen"))continue;
Map<String,Object> updates=new HashMap<>();
updates.put("delivered",true);updates.put("seen",true);
d.getReference().update(updates);
}
}
private void afficherBulleMessage(DocumentSnapshot d){
if(masqueePourMoi(d))return;
String sender=safeString(d,"sender");
String type=safeString(d,"type");
String text=safeString(d,"text");
boolean mine=sender!=null&&sender.equals(senderUid);
long timestamp=safeTimestamp(d,"timestamp");
if(safeBoolean(d,"deleted")){afficherMessageSupprime(mine,timestamp);return;}
if("video".equals(type)){
String checkLink=safeString(d,"videoUrl");
if(checkLink==null||checkLink.trim().isEmpty())return;
}
LinearLayout layout=creerMessageLayout(mine);
LinearLayout bubble=creerBubble(mine);
if("image".equals(type)){
String encoded=safeString(d,"imageBase64");
if(encoded!=null&&!encoded.isEmpty()){
Bitmap bitmap=obtenirMiniature(d.getId(),encoded);
if(bitmap!=null){
bubble.setMinimumWidth(dp(220));
ImageView image=new ImageView(this);
image.setImageBitmap(bitmap);
image.setScaleType(ImageView.ScaleType.CENTER_CROP);
image.setBackgroundColor(Color.LTGRAY);
bubble.addView(image,new LinearLayout.LayoutParams(-1,dp(220)));
image.setOnClickListener(v->ouvrirPhoto(encoded));
}
}
}else if("video".equals(type)){
final String videoLink=safeString(d,"videoUrl");
bubble.setMinimumWidth(dp(220));
LinearLayout videoCard=new LinearLayout(this);
videoCard.setOrientation(LinearLayout.VERTICAL);
videoCard.setGravity(Gravity.CENTER);
videoCard.setPadding(dp(12),dp(22),dp(12),dp(22));
videoCard.setBackground(formeBulle(Color.rgb(35,35,35),12));
TextView playVideo=new TextView(this);
playVideo.setText("▶");
playVideo.setTextSize(40);
playVideo.setTextColor(Color.WHITE);
playVideo.setGravity(Gravity.CENTER);
videoCard.addView(playVideo,new LinearLayout.LayoutParams(-1,-2));
TextView videoLabel=new TextView(this);
videoLabel.setText("Vidéo · Appuyer pour lire");
videoLabel.setTextSize(14);
videoLabel.setTextColor(Color.WHITE);
videoLabel.setGravity(Gravity.CENTER);
videoCard.addView(videoLabel,new LinearLayout.LayoutParams(-1,-2));
bubble.addView(videoCard,new LinearLayout.LayoutParams(-1,dp(150)));
videoCard.setOnClickListener(v->ouvrirVideo(videoLink));
TextView dl=new TextView(this);
dl.setTextColor(Color.WHITE);
dl.setTextSize(13);
dl.setPadding(0,dp(6),0,dp(2));
boolean disponible=VideoCache.existe(this,videoLink);
dl.setText("⬇ Enregistrer dans le téléphone");
dl.setOnClickListener(v->telechargerVideo(videoLink));
bubble.addView(dl);
}else if("audio".equals(type)){
bubble.addView(creerBulleAudio(d.getId(),safeString(d,"audioBase64"),safeTimestamp(d,"audioDuree")));
}else{
TextView content=new TextView(this);
content.setText(text==null?"":text);
content.setTextColor(Color.WHITE);
content.setTextSize(16);
bubble.addView(content);
}
ajouterInfos(bubble,mine,timestamp,safeBoolean(d,"delivered"),safeBoolean(d,"seen"),safeBoolean(d,"edited"));
layout.addView(bubble);
layoutMessages.addView(layout);
bubble.setOnLongClickListener(v->{afficherActionsMessage(d);return true;});
}
private void afficherActionsMessage(DocumentSnapshot d){
boolean mine=senderUid.equals(safeString(d,"sender"));
boolean texte="text".equals(safeString(d,"type"));
List<String> actions=new ArrayList<>();
if(texte)actions.add("Copier");
if(mine&&texte)actions.add("Modifier");
actions.add("Supprimer");
new AlertDialog.Builder(this).setItems(actions.toArray(new String[0]),(dialog,which)->{
String action=actions.get(which);
if("Copier".equals(action))copierMessage(d);
else if("Modifier".equals(action))preparerModification(d);
else if(mine)choisirSuppression(d);
else supprimerPourTous(d);
}).show();
}
private void copierMessage(DocumentSnapshot d){
String text=safeString(d,"text");
if(text==null)text="";
ClipboardManager clipboard=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
if(clipboard!=null){clipboard.setPrimaryClip(ClipData.newPlainText("message",text));toast("Copié.");}
}
private void preparerModification(DocumentSnapshot d){
editingMessageId=d.getId();
etMessage.setText(safeString(d,"text"));
etMessage.setSelection(etMessage.length());
mettreBoutonEnvoyer();
}
private void choisirSuppression(DocumentSnapshot d){
new AlertDialog.Builder(this)
.setTitle("Supprimer le message")
.setPositiveButton("Pour tous",(x,w)->supprimerPourTous(d))
.setNegativeButton("Pour vous",(x,w)->supprimerPourMoi(d))
.setNeutralButton("Annuler",null)
.show();
}
private void supprimerPourMoi(DocumentSnapshot d){
firestore.collection("private_messages").document(d.getId())
.update("hiddenFor",FieldValue.arrayUnion(senderUid))
.addOnFailureListener(e->toast("Suppression impossible."));
}
private void supprimerPourTous(DocumentSnapshot d){
firestore.collection("private_messages").document(d.getId())
.update("deleted",true,"deletedAt",Timestamp.now())
.addOnFailureListener(e->toast("Suppression impossible."));
}
private boolean masqueePourMoi(DocumentSnapshot d){
Object o=d.get("hiddenFor");
return o instanceof List && ((List<?>)o).contains(senderUid);
}
private void ouvrirPhoto(String encoded){
if(encoded==null||encoded.isEmpty()){toast("Image indisponible.");return;}
try{Intent i=new Intent(this,PhotoViewerActivity.class);i.putExtra("imageBase64",encoded);startActivity(i);}catch(Exception e){toast("Impossible d'ouvrir l'image.");}
}
private void ouvrirVideo(String link){
if(link==null||link.trim().isEmpty()){toast("Lien vidéo indisponible.");return;}
try{Intent i=new Intent(this,VideoPlayerActivity.class);i.putExtra("videoUrl",link);startActivity(i);}catch(Exception e){toast("Impossible d'ouvrir la vidéo.");}
}
private String idLecture;
private boolean audioEnPause=false;
private LinearLayout creerBulleAudio(String msgId,String encoded,long dureeMs){
LinearLayout row=new LinearLayout(this);
row.setOrientation(LinearLayout.HORIZONTAL);
row.setGravity(Gravity.CENTER_VERTICAL);
row.setLayoutParams(new LinearLayout.LayoutParams(dp(240),LinearLayout.LayoutParams.WRAP_CONTENT));
TextView btn=new TextView(this);
btn.setText(msgId.equals(idLecture)&&!audioEnPause?"⏸":"▶");
btn.setTextColor(Color.WHITE);
btn.setTextSize(18);
btn.setGravity(Gravity.CENTER);
GradientDrawable rond=new GradientDrawable();
rond.setShape(GradientDrawable.OVAL);
rond.setColor(Color.rgb(20,110,60));
btn.setBackground(rond);
LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(dp(40),dp(40));
bp.setMargins(0,0,dp(8),0);
btn.setLayoutParams(bp);
btn.setOnClickListener(v->basculerAudio(msgId,encoded,btn));
row.addView(btn);
LinearLayout ondes=new LinearLayout(this);
ondes.setOrientation(LinearLayout.HORIZONTAL);
ondes.setGravity(Gravity.CENTER_VERTICAL);
Random r=new Random(msgId.hashCode());
for(int i=0;i<30;i++){
View barre=new View(this);
barre.setBackgroundColor(Color.argb(200,255,255,255));
int h=dp(6)+r.nextInt(dp(16));
LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(3),h);
p.setMargins(dp(1),0,dp(1),0);
ondes.addView(barre,p);
}
row.addView(ondes,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
TextView duree=new TextView(this);
duree.setText(dureeMs>0?formatTemps(dureeMs):"--:--");
duree.setTextColor(Color.WHITE);
duree.setTextSize(12);
duree.setPadding(dp(6),0,0,0);
row.addView(duree);
return row;
}
private void basculerAudio(String msgId,String encoded,TextView btn){
if(encoded==null||encoded.isEmpty()){toast("Message vocal indisponible.");return;}
if(msgId.equals(idLecture)&&lecteurAudio!=null){
try{
if(lecteurAudio.isPlaying()){
lecteurAudio.pause();
audioEnPause=true;
btn.setText("▶");
}else{
lecteurAudio.start();
audioEnPause=false;
btn.setText("⏸");
}
}catch(Exception e){toast("Lecture impossible.");}
return;
}
arreterLecture();
idLecture=msgId;
audioEnPause=false;
btn.setText("⏸");
mediaExecutor.execute(()->{
File temp=null;
try{
byte[] bytes=android.util.Base64.decode(encoded,android.util.Base64.DEFAULT);
temp=new File(getCacheDir(),"play_"+msgId+".m4a");
try(OutputStream out=new FileOutputStream(temp)){out.write(bytes);}
final File fichier=temp;
runOnUiThread(()->demarrerLecture(fichier));
}catch(Exception e){
if(temp!=null)temp.delete();
runOnUiThread(()->{toast("Lecture audio impossible.");arreterLecture();});
}
});
}
private void demarrerLecture(File fichier){
if(!activityActive()){fichier.delete();return;}
try{
MediaPlayer mp=new MediaPlayer();
lecteurAudio=mp;
lecteurFichier=fichier;
mp.setDataSource(fichier.getAbsolutePath());
mp.setOnPreparedListener(MediaPlayer::start);
mp.setOnCompletionListener(x->arreterLecture());
mp.setOnErrorListener((x,w,k)->{arreterLecture();toast("Lecture audio impossible.");return true;});
mp.prepareAsync();
}catch(Exception e){arreterLecture();toast("Lecture audio impossible.");}
}
private void arreterLecture(){
if(lecteurAudio!=null){try{lecteurAudio.release();}catch(Exception ignored){}lecteurAudio=null;}
if(lecteurFichier!=null){try{lecteurFichier.delete();}catch(Exception ignored){}lecteurFichier=null;}
idLecture=null;
audioEnPause=false;
if(activityActive())afficherMessagesCombines();
}
private void lireAudio(String encoded){
if(encoded==null||encoded.isEmpty()){toast("Message vocal indisponible.");return;}
arreterLecture();
try{mediaExecutor.execute(()->{
File temp=null;
try{
byte[] bytes=android.util.Base64.decode(encoded,android.util.Base64.DEFAULT);
temp=new File(getCacheDir(),"play_audio_"+System.currentTimeMillis()+".m4a");
try(OutputStream out=new FileOutputStream(temp)){out.write(bytes);}
final File fichier=temp;
runOnUiThread(()->demarrerLecture(fichier));
}catch(Exception e){
if(temp!=null)temp.delete();
toast("Lecture audio impossible.");
}
});}catch(Exception e){toast("Service audio indisponible.");}
}
/*@Override
protected void onResume(){
super.onResume();
ui=new WeakReference<>(this);
premierPlan=true;
marquerRecusCommeVus();
rafraichirBulleUpload();
traiterFile(getApplicationContext());
}
@Override
protected void onPause(){
premierPlan=false;
super.onPause();
}*/
@Override
protected void onResume(){
super.onResume();
ui=new WeakReference<>(this);
premierPlan=true;
Presence.demarrer(senderUid);
marquerRecusCommeVus();
rafraichirBulleUpload();
traiterFile(getApplicationContext());
}
@Override
protected void onPause(){
premierPlan=false;
Presence.arreter(senderUid);
super.onPause();
}
@Override
protected void onDestroy(){
if(sentMessagesListener!=null)sentMessagesListener.remove();
if(receivedMessagesListener!=null)receivedMessagesListener.remove();
uploadHandler().removeCallbacks(recordTimeoutRunnable);
if(isRecording)annulerEnregistrement();else libererMediaRecorder();
arreterLecture();
WeakReference<ChatActivity> r=ui;
if(r!=null&&r.get()==this)ui=null;
try{notificationExecutor.shutdown();}catch(Exception ignored){}
try{mediaExecutor.shutdown();}catch(Exception ignored){}
super.onDestroy();
}
}
