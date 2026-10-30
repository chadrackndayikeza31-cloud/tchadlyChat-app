package com.chadrack.tchadlychat;import android.app.Activity;import android.graphics.Bitmap;import android.graphics.BitmapFactory;import android.graphics.Color;import android.graphics.Outline;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
public class ProfilUtilisateurActivity extends Activity{
private LinearLayout root;
private String uid,pseudo;
@Override
protected void onCreate(Bundle savedInstanceState){
super.onCreate(savedInstanceState);
uid=getIntent().getStringExtra("uid");
pseudo=getIntent().getStringExtra("pseudo");
if(uid==null||uid.isEmpty()){finish();return;}
ScrollView sv=new ScrollView(this);
sv.setBackgroundColor(Color.rgb(18,24,34));
root=new LinearLayout(this);
root.setOrientation(LinearLayout.VERTICAL);
root.setGravity(Gravity.CENTER_HORIZONTAL);
root.setPadding(dp(24),dp(32),dp(24),dp(32));
sv.addView(root);
setContentView(sv);
chargerProfil();
}
private void chargerProfil(){
FirebaseFirestore.getInstance().collection("users").document(uid).get()
.addOnSuccessListener(d->construireProfil(d.exists()?d:null))
.addOnFailureListener(e->construireProfil(null));
}
private void construireProfil(DocumentSnapshot d){
String nom=d==null?null:lire(d,"pseudo");
if(nom==null)nom=(pseudo==null||pseudo.trim().isEmpty())?"Utilisateur":pseudo;
String photo=d==null?null:lire(d,"photoBase64");
root.addView(creerAvatar(nom,photo),lp(dp(180),dp(180),0,0,0,20));
TextView tvNom=new TextView(this);
tvNom.setText(nom);
tvNom.setTextColor(Color.WHITE);
tvNom.setTextSize(24);
tvNom.setTypeface(null,Typeface.BOLD);
tvNom.setGravity(Gravity.CENTER);
root.addView(tvNom,lp(-1,-2,0,0,0,20));
if(d==null)return;
ajouterLigne("E-mail",lire(d,"email"));
ajouterLigne("Date de naissance",lire(d,"dateNaissance"));
ajouterLigne("Adresse",lire(d,"adresse"));
ajouterLigne("Ville",lire(d,"ville"));
ajouterLigne("Emploi",lire(d,"emploi"));
ajouterLigne("Présentation",lire(d,"bio"));
}
private FrameLayout creerAvatar(String nom,String photo){
FrameLayout cadre=new FrameLayout(this);
Bitmap bmp=decoder(photo);
if(bmp!=null){
ImageView img=new ImageView(this);
img.setImageBitmap(bmp);
img.setScaleType(ImageView.ScaleType.CENTER_CROP);
img.setOutlineProvider(new ViewOutlineProvider(){
@Override
public void getOutline(View view,Outline outline){outline.setOval(0,0,view.getWidth(),view.getHeight());}
});
img.setClipToOutline(true);
cadre.addView(img,new FrameLayout.LayoutParams(-1,-1));
}else{
GradientDrawable rond=new GradientDrawable();
rond.setShape(GradientDrawable.OVAL);
rond.setColor(Color.rgb(125,70,180));
cadre.setBackground(rond);
TextView init=new TextView(this);
init.setText(nom.trim().isEmpty()?"?":nom.trim().substring(0,1).toUpperCase());
init.setTextColor(Color.WHITE);
init.setTextSize(64);
init.setTypeface(null,Typeface.BOLD);
init.setGravity(Gravity.CENTER);
cadre.addView(init,new FrameLayout.LayoutParams(-1,-1));
}
return cadre;
}
private void ajouterLigne(String label,String valeur){
if(valeur==null)return;
LinearLayout carte=new LinearLayout(this);
carte.setOrientation(LinearLayout.VERTICAL);
carte.setPadding(dp(16),dp(12),dp(16),dp(12));
GradientDrawable fond=new GradientDrawable();
fond.setCornerRadius(dp(14));
fond.setColor(Color.rgb(35,45,60));
carte.setBackground(fond);
TextView l=new TextView(this);
l.setText(label);
l.setTextColor(Color.rgb(150,165,185));
l.setTextSize(12);
TextView v=new TextView(this);
v.setText(valeur);
v.setTextColor(Color.WHITE);
v.setTextSize(16);
carte.addView(l);
carte.addView(v);
root.addView(carte,lp(-1,-2,0,0,0,10));
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
private String lire(DocumentSnapshot d,String champ){
Object o=d.get(champ);
if(o instanceof String){
String s=((String)o).trim();
return s.isEmpty()?null:s;
}
return null;
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
