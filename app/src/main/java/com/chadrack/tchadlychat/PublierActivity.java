package com.chadrack.tchadlychat;import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
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
public class PublierActivity extends Activity{
private static final int PICK_IMG=4101,PICK_VID=4102;
private static final long MAX_VID=50L*1024L*1024L;
private static final String SERVEUR="https://tchadlychat-server.onrender.com/create-video-upload";
private static final String SUPABASE="https://ecdzacaaayomyqvtwpyo.supabase.co";
private EditText etTexte;
private TextView tvEtat;
private Button btnPublier;
private Uri mediaUri;
private String mediaType="none";
private String moi;
@Override
protected void onCreate(Bundle b){
super.onCreate(b);
FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
if(u==null){finish();return;}
moi=u.getUid();
ScrollView sv=new ScrollView(this);
sv.setBackgroundColor(Color.rgb(18,24,34));
LinearLayout root=new LinearLayout(this);
root.setOrientation(LinearLayout.VERTICAL);
root.setPadding(dp(20),dp(20),dp(20),dp(20));
TextView titre=new TextView(this);
titre.setText("Nouvelle publication");
titre.setTextColor(Color.WHITE);
titre.setTextSize(22);
titre.setTypeface(null,Typeface.BOLD);
root.addView(titre,lp(-1,-2,0,0,0,16));
etTexte=new EditText(this);
etTexte.setHint("Quoi de neuf ?");
etTexte.setHintTextColor(Color.GRAY);
etTexte.setTextColor(Color.WHITE);
etTexte.setMinLines(4);
etTexte.setGravity(Gravity.TOP);
etTexte.setBackgroundColor(Color.rgb(60,60,60));
etTexte.setPadding(dp(14),dp(12),dp(14),dp(12));
root.addView(etTexte,lp(-1,-2,0,0,0,12));
Button bImg=bouton("📷 Ajouter une photo",Color.rgb(70,110,170));
bImg.setOnClickListener(x->choisir("image/*",PICK_IMG));
root.addView(bImg,lp(-1,dp(48),0,0,0,8));
Button bVid=bouton("🎥 Ajouter une vidéo (max 50 Mo)",Color.rgb(70,110,170));
bVid.setOnClickListener(x->choisir("video/*",PICK_VID));
root.addView(bVid,lp(-1,dp(48),0,0,0,16));
tvEtat=new TextView(this);
tvEtat.setTextColor(Color.rgb(190,205,225));
tvEtat.setTextSize(14);
root.addView(tvEtat,lp(-1,-2,0,0,0,12));
btnPublier=bouton("Publier",Color.rgb(46,180,95));
btnPublier.setOnClickListener(x->publier());
root.addView(btnPublier,lp(-1,dp(50),0,0,0,8));
Button annuler=bouton("Annuler",Color.rgb(90,90,90));
annuler.setOnClickListener(x->finish());
root.addView(annuler,lp(-1,dp(50),0,0,0,0));
sv.addView(root);
setContentView(sv);
}
private void choisir(String type,int code){
Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
i.addCategory(Intent.CATEGORY_OPENABLE);
i.setType(type);
startActivityForResult(i,code);
}
@Override
protected void onActivityResult(int req,int res,Intent data){
super.onActivityResult(req,res,data);
if(res!=RESULT_OK||data==null||data.getData()==null)return;
mediaUri=data.getData();
mediaType=req==PICK_IMG?"image":"video";
tvEtat.setText("Média sélectionné : "+(req==PICK_IMG?"photo":"vidéo"));
}
private void publier(){
final String texte=etTexte.getText().toString().trim();
if(texte.length()>1000){toast("Texte trop long (max 1000 caractères).");return;}
if(texte.isEmpty()&&mediaUri==null){toast("Écris un texte, ou ajoute une photo ou une vidéo.");return;}
btnPublier.setEnabled(false);
final Uri uri=mediaUri;
final String type=uri==null?"none":mediaType;
new Thread(()->{
try{
String url=null;
if(uri!=null){
setEtat("Préparation du média…");
File f=preparer(uri,type);
setEtat("Envoi vers le stockage…");
url=envoyer(f,type);
f.delete();
}
Map<String,Object> m=new HashMap<>();
m.put("author",moi);
m.put("text",texte);
m.put("mediaType",type);
m.put("mediaUrl",url==null?"":url);
m.put("createdAt",Timestamp.now());
m.put("reactions",new HashMap<String,Object>());
FirebaseFirestore.getInstance().collection("posts").add(m)
.addOnSuccessListener(r->{
toast("Publication envoyée.");
finish();
})
.addOnFailureListener(e->{
runOnUiThread(()->btnPublier.setEnabled(true));
toast("Publication impossible.");
});
}catch(Exception e){
runOnUiThread(()->{
btnPublier.setEnabled(true);
tvEtat.setText("");
});
toast(e.getMessage()==null?"Erreur":e.getMessage());
}
}).start();
}
private File preparer(Uri uri,String type)throws Exception{
File f=new File(getCacheDir(),"post_"+System.currentTimeMillis()+("image".equals(type)?".jpg":".mp4"));
if("image".equals(type)){
InputStream in=getContentResolver().openInputStream(uri);
if(in==null)throw new Exception("Photo inaccessible");
Bitmap bm=BitmapFactory.decodeStream(in);
in.close();
if(bm==null)throw new Exception("Photo invalide");
int w=bm.getWidth(),h=bm.getHeight();
float r=Math.min(1024f/w,1024f/h);
if(r<1f)bm=Bitmap.createScaledBitmap(bm,Math.round(w*r),Math.round(h*r),true);
try(FileOutputStream out=new FileOutputStream(f)){
bm.compress(Bitmap.CompressFormat.JPEG,80,out);
}
}else{
long taille=0;
try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(f)){
if(in==null)throw new Exception("Vidéo inaccessible");
byte[] b=new byte[65536];
int n;
while((n=in.read(b))!=-1){
taille+=n;
if(taille>MAX_VID){
throw new Exception("Vidéo trop grande (max 50 Mo)");
}
out.write(b,0,n);
}
}
}
return f;
}
private String envoyer(File f,String type)throws Exception{
String ext="image".equals(type)?"jpg":"mp4";
String mime="image".equals(type)?"image/jpeg":"video/mp4";
String token=Tasks.await(FirebaseAuth.getInstance().getCurrentUser().getIdToken(false),20,TimeUnit.SECONDS).getToken();
JSONObject body=new JSONObject();
body.put("userId",moi);
body.put("receiverUid","");
body.put("fileSize",f.length());
body.put("mimeType",mime);
body.put("fileName",f.getName());
body.put("bucket","posts");
body.put("ext",ext);
byte[] bytes=body.toString().getBytes("UTF-8");
HttpURLConnection c=(HttpURLConnection)new URL(SERVEUR).openConnection();
c.setRequestMethod("POST");
c.setConnectTimeout(30000);
c.setReadTimeout(60000);
c.setDoOutput(true);
c.setRequestProperty("Content-Type","application/json; charset=UTF-8");
c.setRequestProperty("Authorization","Bearer "+token);
c.setFixedLengthStreamingMode(bytes.length);
try(OutputStream out=c.getOutputStream()){out.write(bytes);}
int code=c.getResponseCode();
String txt=lire(code>=200&&code<300?c.getInputStream():c.getErrorStream());
c.disconnect();
if(code<200||code>=300)throw new Exception("Serveur HTTP "+code);
JSONObject r=new JSONObject(txt);
if(!r.optBoolean("success",false))throw new Exception(r.optString("error","Erreur serveur"));
String path=r.getString("path");
String tk=r.getString("token");
HttpURLConnection p=(HttpURLConnection)new URL(SUPABASE+"/storage/v1/object/upload/sign/posts/"+path+"?token="+URLEncoder.encode(tk,"UTF-8")).openConnection();
p.setRequestMethod("PUT");
p.setConnectTimeout(30000);
p.setReadTimeout(600000);
p.setDoOutput(true);
p.setUseCaches(false);
p.setFixedLengthStreamingMode(f.length());
p.setRequestProperty("Content-Type",mime);
p.setRequestProperty("x-upsert","true");
try(InputStream in=new FileInputStream(f);OutputStream out=p.getOutputStream()){
byte[] bb=new byte[65536];
int n;
while((n=in.read(bb))!=-1)out.write(bb,0,n);
}
int pc=p.getResponseCode();
lire(pc>=200&&pc<300?p.getInputStream():p.getErrorStream());
p.disconnect();
if(pc<200||pc>=300)throw new Exception("Envoi du média HTTP "+pc);
return SUPABASE+"/storage/v1/object/public/posts/"+path;
}
private String lire(InputStream in)throws Exception{
if(in==null)return "";
ByteArrayOutputStream o=new ByteArrayOutputStream();
byte[] b=new byte[4096];
int n;
while((n=in.read(b))!=-1)o.write(b,0,n);
in.close();
return new String(o.toByteArray(),"UTF-8");
}
private void setEtat(final String s){
runOnUiThread(()->tvEtat.setText(s));
}
private void toast(final String s){
runOnUiThread(()->Toast.makeText(this,s,Toast.LENGTH_SHORT).show());
}
private Button bouton(String texte,int couleur){
Button b=new Button(this);
b.setText(texte);
b.setAllCaps(false);
b.setTextColor(Color.WHITE);
b.setBackgroundColor(couleur);
return b;
}
private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int b){
LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);
p.setMargins(dp(l),dp(t),dp(r),dp(b));
return p;
}
private int dp(int v){
return(int)(v*getResources().getDisplayMetrics().density+0.5f);
}
}

