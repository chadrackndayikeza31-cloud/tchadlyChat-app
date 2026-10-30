package com.chadrack.tchadlychat;import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
public class PostsActivity extends Activity{
private String moi;
private LinearLayout liste;
private FirebaseFirestore db;
private ListenerRegistration reg;
@Override
protected void onCreate(Bundle b){
super.onCreate(b);
FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
if(u==null){finish();return;}
moi=u.getUid();
db=FirebaseFirestore.getInstance();
FrameLayout root=new FrameLayout(this);
root.setBackgroundColor(Color.rgb(18,24,34));
ScrollView sv=new ScrollView(this);
liste=new LinearLayout(this);
liste.setOrientation(LinearLayout.VERTICAL);
liste.setPadding(dp(12),dp(12),dp(12),dp(70));
sv.addView(liste);
FrameLayout.LayoutParams lpSv=new FrameLayout.LayoutParams(-1,-1);
lpSv.topMargin=dp(56);
root.addView(sv,lpSv);
LinearLayout haut=new LinearLayout(this);
haut.setOrientation(LinearLayout.HORIZONTAL);
haut.setGravity(Gravity.CENTER_VERTICAL);
haut.setBackgroundColor(Color.rgb(20,28,40));
haut.setPadding(dp(16),0,dp(8),0);
TextView titre=new TextView(this);
titre.setText("Publications");
titre.setTextColor(Color.WHITE);
titre.setTextSize(20);
titre.setTypeface(null,Typeface.BOLD);
haut.addView(titre,new LinearLayout.LayoutParams(0,-2,1f));
Button publier=bouton("+ Publier",Color.rgb(46,180,95));
publier.setOnClickListener(v->startActivity(new Intent(this,PublierActivity.class)));
haut.addView(publier);
FrameLayout.LayoutParams lpHaut=new FrameLayout.LayoutParams(-1,dp(56));
lpHaut.gravity=Gravity.TOP;
root.addView(haut,lpHaut);
setContentView(root);
BarreNav.ajouter(this,BarreNav.PUBLICATIONS);
}
@Override
protected void onStart(){
super.onStart();
reg=db.collection("posts").orderBy("createdAt",Query.Direction.DESCENDING).limit(50).addSnapshotListener((snap,e)->{
if(e!=null||snap==null)return;
liste.removeAllViews();for(DocumentSnapshot d:snap.getDocuments())liste.addView(carte(d));
});
}
@Override
protected void onStop(){
if(reg!=null){reg.remove();reg=null;}
super.onStop();
}
private View carte(DocumentSnapshot d){
final String id=d.getId();
final String auteur=d.getString("author");
final String texte=d.getString("text");
final String type=d.getString("mediaType");
final String url=d.getString("mediaUrl");
final Timestamp ts=d.getTimestamp("createdAt");
Object ro=d.get("reactions");
final Map<?,?> reacts=ro instanceof Map?(Map<?,?>)ro:new HashMap<String,Object>();
Object mo=reacts.get(moi);
final String mien=mo instanceof String?(String)mo:null;
LinearLayout c=new LinearLayout(this);
c.setOrientation(LinearLayout.VERTICAL);
c.setPadding(dp(14),dp(12),dp(14),dp(12));
GradientDrawable f=new GradientDrawable();
f.setCornerRadius(dp(14));
f.setColor(Color.rgb(30,40,58));
c.setBackground(f);
LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
lp.setMargins(0,0,0,dp(12));
c.setLayoutParams(lp);
LinearLayout en=new LinearLayout(this);
en.setOrientation(LinearLayout.HORIZONTAL);
en.setGravity(Gravity.CENTER_VERTICAL);
final ImageView av=new ImageView(this);
av.setScaleType(ImageView.ScaleType.CENTER_CROP);
av.setBackgroundColor(Color.rgb(55,70,95));
av.setOutlineProvider(new ViewOutlineProvider(){
@Override
public void getOutline(View v,Outline o){o.setOval(0,0,v.getWidth(),v.getHeight());}
});
av.setClipToOutline(true);
en.addView(av,new LinearLayout.LayoutParams(dp(40),dp(40)));
final TextView nom=new TextView(this);
nom.setText("…");
nom.setTextColor(Color.WHITE);
nom.setTextSize(15);
nom.setTypeface(null,Typeface.BOLD);
LinearLayout.LayoutParams pn=new LinearLayout.LayoutParams(0,-2,1f);
pn.setMargins(dp(10),0,dp(8),0);
en.addView(nom,pn);
TextView date=new TextView(this);
date.setText(ts==null?"":new SimpleDateFormat("dd/MM HH:mm",Locale.FRANCE).format(ts.toDate()));
date.setTextColor(Color.rgb(150,165,185));
date.setTextSize(12);
en.addView(date);
c.addView(en);
if(auteur!=null){
db.collection("users").document(auteur).get().addOnSuccessListener(us->{
String p=us.getString("pseudo");
nom.setText(p==null?"Utilisateur":p);
Bitmap bm=decoder(us.getString("photoBase64"));
if(bm!=null)av.setImageBitmap(bm);
});
}
if(texte!=null&&!texte.isEmpty()){
TextView t=new TextView(this);
t.setText(texte);
t.setTextColor(Color.WHITE);
t.setTextSize(16);
t.setPadding(0,dp(10),0,dp(6));
c.addView(t);
}
if("image".equals(type)&&url!=null&&!url.isEmpty()){
ImageView im=new ImageView(this);
im.setScaleType(ImageView.ScaleType.CENTER_CROP);
im.setBackgroundColor(Color.rgb(20,28,40));
c.addView(im,new LinearLayout.LayoutParams(-1,dp(260)));
chargerUrl(im,url);
}
if("video".equals(type)&&url!=null&&!url.isEmpty()){
TextView v=new TextView(this);
v.setText("▶  Vidéo · Appuyer pour lire");
v.setGravity(Gravity.CENTER);
v.setTextColor(Color.WHITE);
v.setTextSize(16);v.setBackgroundColor(Color.rgb(35,35,35));
v.setPadding(0,dp(60),0,dp(60));
v.setOnClickListener(x->{
Intent i=new Intent(this,VideoPlayerActivity.class);
i.putExtra("videoUrl",url);
startActivity(i);
});
c.addView(v,new LinearLayout.LayoutParams(-1,-2));
}
String[] em={"👍","❤️","😂"};
LinearLayout rx=new LinearLayout(this);
rx.setPadding(0,dp(10),0,0);
for(final String e:em){
int n=0;
for(Object val:reacts.values())if(e.equals(val))n++;
final boolean actif=e.equals(mien);
TextView bt=new TextView(this);
bt.setText(e+" "+n);
bt.setTextColor(actif?Color.rgb(46,180,95):Color.WHITE);
bt.setTextSize(15);
bt.setPadding(dp(10),dp(6),dp(12),dp(6));
bt.setOnClickListener(x->reagir(id,e,actif));
rx.addView(bt);
}
c.addView(rx);
LinearLayout pied=new LinearLayout(this);
pied.setPadding(0,dp(8),0,0);
Button com=bouton("💬 Commentaires",Color.rgb(60,70,85));
com.setOnClickListener(x->{
Intent i=new Intent(this,CommentairesActivity.class);
i.putExtra("postId",id);
startActivity(i);
});
pied.addView(com);
if(moi.equals(auteur)){
Button sup=bouton("🗑 Supprimer",Color.rgb(120,45,50));
sup.setOnClickListener(x->db.collection("posts").document(id).delete()
.addOnFailureListener(e->Toast.makeText(this,"Suppression impossible.",Toast.LENGTH_SHORT).show()));
pied.addView(sup);
}
c.addView(pied);
return c;
}
private void reagir(String id,String e,boolean actif){
Object val=actif?FieldValue.delete():e;
db.collection("posts").document(id).update("reactions."+moi,val)
.addOnFailureListener(x->Toast.makeText(this,"Réaction impossible.",Toast.LENGTH_SHORT).show());
}
private void chargerUrl(final ImageView im,final String url){
new Thread(()->{
try{
HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
c.setConnectTimeout(15000);
c.setReadTimeout(30000);
BitmapFactory.Options o=new BitmapFactory.Options();
o.inSampleSize=2;
try(InputStream in=c.getInputStream()){
final Bitmap bm=BitmapFactory.decodeStream(in,null,o);
if(bm!=null)runOnUiThread(()->im.setImageBitmap(bm));
}
}catch(Exception ignored){}
}).start();
}
private Bitmap decoder(String photo){
if(photo==null||photo.isEmpty())return null;
try{
byte[] b=Base64.decode(photo,Base64.DEFAULT);
return BitmapFactory.decodeByteArray(b,0,b.length);
}catch(Exception e){
return null;
}
}
private Button bouton(String texte,int couleur){
Button b=new Button(this);
b.setText(texte);
b.setAllCaps(false);
b.setTextColor(Color.WHITE);
b.setBackgroundColor(couleur);
return b;
}
private int dp(int v){
return(int)(v*getResources().getDisplayMetrics().density+0.5f);
}
}
