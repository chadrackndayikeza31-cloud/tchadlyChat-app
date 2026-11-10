package com.chadrack.tchadlychat;import android.app.Activity;import android.content.Intent;import android.graphics.Bitmap;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
public class ProfilActivity extends Activity{
private String uid;
@Override
protected void onCreate(Bundle b){
super.onCreate(b);
FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
if(u==null){finish();return;}
uid=u.getUid();
ScrollView sv=new ScrollView(this);
sv.setBackgroundColor(Color.rgb(12,16,26));
LinearLayout root=new LinearLayout(this);
root.setOrientation(LinearLayout.VERTICAL);
root.setGravity(Gravity.CENTER_HORIZONTAL);
root.setPadding(dp(24),dp(40),dp(24),dp(90));
final ImageView av=new ImageView(this);
av.setScaleType(ImageView.ScaleType.CENTER_CROP);
av.setBackgroundColor(Color.rgb(55,70,95));
av.setOutlineProvider(new ViewOutlineProvider(){
@Override
public void getOutline(View v,Outline o){o.setOval(0,0,v.getWidth(),v.getHeight());}
});
av.setClipToOutline(true);
root.addView(av,lp(dp(110),dp(110),0,0,0,14));
final TextView nom=new TextView(this);
nom.setText("…");
nom.setTextColor(Color.WHITE);
nom.setTextSize(22);
nom.setTypeface(null,Typeface.BOLD);
nom.setGravity(Gravity.CENTER);
root.addView(nom,lp(-1,-2,0,0,0,6));
TextView mail=new TextView(this);
mail.setText(u.getEmail()==null?"":u.getEmail());
mail.setTextColor(Color.rgb(150,165,185));
mail.setTextSize(14);
mail.setGravity(Gravity.CENTER);
root.addView(mail,lp(-1,-2,0,0,0,28));
Button monProfil=bouton("👤 Mon profil",Color.rgb(70,110,170));
monProfil.setOnClickListener(v->{
Intent i=new Intent(this,ProfilUtilisateurActivity.class);
i.putExtra("uid",uid);
i.putExtra("pseudo",nom.getText().toString());
startActivity(i);
});
root.addView(monProfil,lp(-1,dp(50),0,0,0,12));
Button param=bouton("⚙ Paramètres",Color.rgb(70,110,170));
param.setOnClickListener(v->startActivity(new Intent(this,ParametresActivity.class)));
root.addView(param,lp(-1,dp(50),0,0,0,12));
Button deco=bouton("🚪 Déconnecter",Color.rgb(190,45,55));
deco.setOnClickListener(v->{
FirebaseAuth.getInstance().signOut();
Intent i=new Intent(this,MainActivity.class);
i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
startActivity(i);
finish();
});
root.addView(deco,lp(-1,dp(50),0,0,0,0));
sv.addView(root);
setContentView(sv);
BarreNav.ajouter(this,BarreNav.PROFIL);
FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener(d->{
String p=d.getString("pseudo");
nom.setText(p==null?"Utilisateur":p);
Bitmap bm=decoder(d.getString("photoBase64"));
if(bm!=null)av.setImageBitmap(bm);
});
}
private Button bouton(String texte,int couleur){
Button b=new Button(this);
b.setText(texte);
b.setAllCaps(false);
b.setTextColor(Color.WHITE);
GradientDrawable g=new GradientDrawable();
g.setCornerRadius(dp(12));
g.setColor(couleur);
b.setBackground(g);
return b;
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
private LinearLayout.LayoutParams lp(int w,int h,int l,int t,int r,int bo){
LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);
p.setMargins(dp(l),dp(t),dp(r),dp(bo));
return p;
}
private int dp(int v){
return(int)(v*getResources().getDisplayMetrics().density+0.5f);
}
}

