package com.chadrack.tchadlychat;import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class DiscussionsActivity extends Activity{
private static class Conv{String partner;long time;String texte;}
private String moi;
private String filtre="";
private FirebaseFirestore db;
private LinearLayout liste;
private ListenerRegistration regEnvoyes,regRecus;
private List<DocumentSnapshot> envoyes=new ArrayList<>();
private List<DocumentSnapshot> recus=new ArrayList<>();
private final Map<String,Conv> conv=new HashMap<>();
private final Map<String,String> pseudos=new HashMap<>();
@Override
protected void onCreate(Bundle b){
super.onCreate(b);
FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
if(u==null){finish();return;}
moi=u.getUid();
db=FirebaseFirestore.getInstance();
FrameLayout root=new FrameLayout(this);
root.setBackgroundColor(Color.rgb(12,16,26));
ScrollView sv=new ScrollView(this);
liste=new LinearLayout(this);
liste.setOrientation(LinearLayout.VERTICAL);
liste.setPadding(dp(12),dp(4),dp(12),dp(70));
sv.addView(liste);
FrameLayout.LayoutParams lpSv=new FrameLayout.LayoutParams(-1,-1);
lpSv.topMargin=dp(108);
root.addView(sv,lpSv);
LinearLayout haut=new LinearLayout(this);
haut.setOrientation(LinearLayout.HORIZONTAL);
haut.setGravity(Gravity.CENTER_VERTICAL);
haut.setBackgroundColor(Color.rgb(20,28,40));
haut.setPadding(dp(16),0,dp(16),0);
TextView titre=new TextView(this);
titre.setText("Discussion");
titre.setTextColor(Color.WHITE);
titre.setTextSize(20);
titre.setTypeface(null,Typeface.BOLD);
haut.addView(titre,new LinearLayout.LayoutParams(-1,-2));
FrameLayout.LayoutParams lpHaut=new FrameLayout.LayoutParams(-1,dp(56));
lpHaut.gravity=Gravity.TOP;
root.addView(haut,lpHaut);
EditText recherche=new EditText(this);
recherche.setHint("🔍 Rechercher une personne…");
recherche.setHintTextColor(Color.rgb(120,135,160));
recherche.setTextColor(Color.WHITE);
recherche.setSingleLine(true);
GradientDrawable fond=new GradientDrawable();
fond.setCornerRadius(dp(12));
fond.setColor(Color.rgb(30,40,58));
recherche.setBackground(fond);
recherche.setPadding(dp(14),0,dp(14),0);
recherche.addTextChangedListener(new TextWatcher(){
@Override
public void beforeTextChanged(CharSequence s,int st,int c,int a){}
@Override
public void onTextChanged(CharSequence s,int st,int bf,int c){
filtre=s.toString().trim().toLowerCase(Locale.ROOT);
afficher();
}
@Override
public void afterTextChanged(Editable s){}
});
FrameLayout.LayoutParams lpRech=new FrameLayout.LayoutParams(-1,dp(44));
lpRech.gravity=Gravity.TOP;
lpRech.setMargins(dp(12),dp(62),dp(12),0);
root.addView(recherche,lpRech);
setContentView(root);
BarreNav.ajouter(this,BarreNav.DISCUSSION);
}
@Override
protected void onStart(){
super.onStart();
regEnvoyes=db.collection("private_messages").whereEqualTo("sender",moi).addSnapshotListener((s,e)->{
if(e!=null||s==null)return;
envoyes=s.getDocuments();
reconstruire();
});
regRecus=db.collection("private_messages").whereEqualTo("receiver",moi).addSnapshotListener((s,e)->{
if(e!=null||s==null)return;
recus=s.getDocuments();
reconstruire();
});
}
@Override
protected void onStop(){
if(regEnvoyes!=null){regEnvoyes.remove();regEnvoyes=null;}
if(regRecus!=null){regRecus.remove();regRecus=null;}
super.onStop();
}
private void reconstruire(){
conv.clear();
for(DocumentSnapshot d:envoyes)ajouter(d,d.getString("receiver"));
for(DocumentSnapshot d:recus)ajouter(d,d.getString("sender"));
afficher();
}
private void ajouter(DocumentSnapshot d,String partner){
if(partner==null||partner.equals(moi))return;
if(Boolean.TRUE.equals(d.getBoolean("deleted")))return;
Object h=d.get("hiddenFor");
if(h instanceof List&&((List<?>)h).contains(moi))return;
Timestamp ts=d.getTimestamp("createdAt");
long t=ts==null?0L:ts.toDate().getTime();
Conv c=conv.get(partner);
if(c==null){
c=new Conv();
c.partner=partner;
c.time=0L;
c.texte="";
conv.put(partner,c);
}
if(t>=c.time){
c.time=t;
c.texte=apercu(d);
}
}
private String apercu(DocumentSnapshot d){
String type=d.getString("type");
if("image".equals(type))return "📷 Photo";
if("video".equals(type))return "🎥 Vidéo";
if("audio".equals(type))return "🎤 Message vocal";
if("document".equals(type))return "📄 Document";
String t=d.getString("text");
return t==null?"":t;
}
private void afficher(){
liste.removeAllViews();
List<Conv> l=new ArrayList<>(conv.values());
Collections.sort(l,(x,y)->Long.compare(y.time,x.time));
for(Conv c:l){
if(!filtre.isEmpty()){
String p=pseudos.get(c.partner);
if(p==null||!p.toLowerCase(Locale.ROOT).contains(filtre))continue;
}
liste.addView(ligne(c));
}
}
private LinearLayout ligne(final Conv c){
final LinearLayout row=new LinearLayout(this);
row.setOrientation(LinearLayout.HORIZONTAL);
row.setGravity(Gravity.CENTER_VERTICAL);
row.setPadding(dp(12),dp(12),dp(12),dp(12));
GradientDrawable f=new GradientDrawable();
f.setCornerRadius(dp(14));
f.setColor(Color.rgb(22,28,42));
row.setBackground(f);
LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
lp.setMargins(0,0,0,dp(10));
row.setLayoutParams(lp);
row.setVisibility(View.GONE);
final ImageView av=new ImageView(this);
av.setScaleType(ImageView.ScaleType.CENTER_CROP);
av.setBackgroundColor(Color.rgb(55,70,95));
av.setOutlineProvider(new ViewOutlineProvider(){
@Override
public void getOutline(View v,Outline o){o.setOval(0,0,v.getWidth(),v.getHeight());}
});
av.setClipToOutline(true);
row.addView(av,new LinearLayout.LayoutParams(dp(50),dp(50)));
LinearLayout info=new LinearLayout(this);
info.setOrientation(LinearLayout.VERTICAL);
LinearLayout.LayoutParams pi=new LinearLayout.LayoutParams(0,-2,1f);
pi.setMargins(dp(12),0,dp(8),0);
row.addView(info,pi);
final TextView nom=new TextView(this);
nom.setText("…");
nom.setTextColor(Color.WHITE);
nom.setTextSize(16);
nom.setTypeface(null,Typeface.BOLD);
info.addView(nom);
TextView apercu=new TextView(this);
apercu.setText(c.texte);
apercu.setTextColor(Color.rgb(150,165,185));
apercu.setTextSize(13);
apercu.setMaxLines(1);
apercu.setEllipsize(android.text.TextUtils.TruncateAt.END);
info.addView(apercu);
TextView temps=new TextView(this);
temps.setText(tempsEcoule(c.time));
temps.setTextColor(Color.rgb(150,165,185));
temps.setTextSize(12);
row.addView(temps);
chargerProfil(c.partner,nom,av);
verifierAcces(c.partner,row);
row.setOnClickListener(v->{
Intent i=new Intent(this,ChatActivity.class);
i.putExtra("receiverUid",c.partner);
String p=pseudos.get(c.partner);
i.putExtra("receiverPseudo",p==null?"Utilisateur":p);
startActivity(i);
});
return row;
}
private void chargerProfil(final String uid,final TextView nom,final ImageView av){
if(pseudos.containsKey(uid))nom.setText(pseudos.get(uid));
db.collection("users").document(uid).get().addOnSuccessListener(us->{
String p=us.getString("pseudo");
String v=p==null?"Utilisateur":p;
pseudos.put(uid,v);
nom.setText(v);
Bitmap bm=decoder(us.getString("photoBase64"));
if(bm!=null)av.setImageBitmap(bm);
if(!filtre.isEmpty())afficher();
});
}
private void verifierAcces(final String uid,final View row){
String id=uid.compareTo(moi)<0?uid+"_"+moi:moi+"_"+uid;
db.collection("chat_requests").document(id).get().addOnSuccessListener(r->{
if(!r.exists()){
row.setVisibility(View.VISIBLE);
return;
}
String st=r.getString("status");
String from=r.getString("from");
if("accepted".equals(st)||moi.equals(from))row.setVisibility(View.VISIBLE);
});
}
private String tempsEcoule(long ms){
if(ms<=0L)return "";
long s=(System.currentTimeMillis()-ms)/1000L;
if(s<60L)return "maintenant";
long min=s/60L;
if(min<60L)return min+"min";
long h=min/60L;
if(h<24L)return h+"h";
long j=h/24L;
if(j<7L)return j+"j";
if(j<364L)return (j/7L)+"semaine";
return Math.max(1L,j/365L)+"an";
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
private int dp(int v){
return(int)(v*getResources().getDisplayMetrics().density+0.5f);
}
}
