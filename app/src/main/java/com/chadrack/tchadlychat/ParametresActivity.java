package com.chadrack.tchadlychat;import android.app.Activity;import android.content.Intent;import android.graphics.Bitmap;import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
public class ParametresActivity extends Activity{
private static final int PICK_PHOTO=3101;
private ImageView ivPhoto;
private EditText etEmail,etAdresse,etEmploi;
private Button btnSauvegarder,btnRetour,btnChangerPhoto;
private String uid,photoEnAttente;
private FirebaseFirestore firestore;
@Override
protected void onCreate(Bundle savedInstanceState){
super.onCreate(savedInstanceState);
FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
if(user==null){finish();return;}
uid=user.getUid();
firestore=FirebaseFirestore.getInstance();
construireUi();
chargerDonnees(user.getEmail());
}
private void construireUi(){
ScrollView sv=new ScrollView(this);
sv.setBackgroundColor(Color.rgb(18,24,34));
LinearLayout root=new LinearLayout(this);
root.setOrientation(LinearLayout.VERTICAL);
root.setGravity(Gravity.CENTER_HORIZONTAL);
root.setPadding(dp(24),dp(24),dp(24),dp(24));
TextView titre=new TextView(this);
titre.setText("Paramètres");
titre.setTextColor(Color.WHITE);
titre.setTextSize(24);
titre.setTypeface(null,Typeface.BOLD);
root.addView(titre,lp(-1,-2,0,0,0,20));
ivPhoto=new ImageView(this);
ivPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
ivPhoto.setBackgroundColor(Color.rgb(55,70,95));
ivPhoto.setOutlineProvider(new ViewOutlineProvider(){
@Override
public void getOutline(View view,Outline outline){outline.setOval(0,0,view.getWidth(),view.getHeight());}
});
ivPhoto.setClipToOutline(true);
root.addView(ivPhoto,lp(dp(160),dp(160),0,0,0,12));
btnChangerPhoto=bouton("Changer la photo",Color.rgb(70,110,170));
btnChangerPhoto.setOnClickListener(v->choisirPhoto());
root.addView(btnChangerPhoto,lp(-1,dp(48),0,0,0,20));
etEmail=champ("E-mail",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
root.addView(etEmail,lp(-1,-2,0,0,0,12));
etAdresse=champ("Adresse",InputType.TYPE_CLASS_TEXT);
root.addView(etAdresse,lp(-1,-2,0,0,0,12));
etEmploi=champ("Emploi / profession",InputType.TYPE_CLASS_TEXT);
root.addView(etEmploi,lp(-1,-2,0,0,0,24));
btnSauvegarder=bouton("Sauvegarder",Color.rgb(46,180,95));
btnSauvegarder.setOnClickListener(v->sauvegarder());
root.addView(btnSauvegarder,lp(-1,dp(50),0,0,0,12));
btnRetour=bouton("Retourner",Color.rgb(90,90,90));
btnRetour.setOnClickListener(v->finish());
root.addView(btnRetour,lp(-1,dp(50),0,0,0,0));
sv.addView(root);
setContentView(sv);
}
private void chargerDonnees(String emailAuth){
firestore.collection("users").document(uid).get().addOnSuccessListener(d->{
if(d.exists()){
String email=lire(d,"email");
etEmail.setText(email==null?(emailAuth==null?"":emailAuth):email);
etAdresse.setText(valeur(d,"adresse"));
etEmploi.setText(valeur(d,"emploi"));
Bitmap b=decoder(lire(d,"photoBase64"));
if(b!=null)ivPhoto.setImageBitmap(b);
}else{
etEmail.setText(emailAuth==null?"":emailAuth);
}
});
}
private void choisirPhoto(){
    Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
i.addCategory(Intent.CATEGORY_OPENABLE);
i.setType("image/*");
startActivityForResult(i,PICK_PHOTO);
}
@Override
protected void onActivityResult(int requestCode,int resultCode,Intent data){
super.onActivityResult(requestCode,resultCode,data);
if(requestCode==PICK_PHOTO&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
try{
photoEnAttente=encoderImage(data.getData());
ivPhoto.setImageBitmap(decoder(photoEnAttente));
}catch(Exception e){
Toast.makeText(this,"Photo invalide ou trop grande.",Toast.LENGTH_SHORT).show();
}
}
}
private String encoderImage(Uri uri)throws Exception{
InputStream in=getContentResolver().openInputStream(uri);
if(in==null)throw new Exception("Photo inaccessible");
Bitmap b=BitmapFactory.decodeStream(in);
in.close();
if(b==null)throw new Exception("Photo invalide");
int w=b.getWidth(),h=b.getHeight();
float ratio=Math.min(400f/w,400f/h);
if(ratio<1f)b=Bitmap.createScaledBitmap(b,Math.round(w*ratio),Math.round(h*ratio),true);
ByteArrayOutputStream out=new ByteArrayOutputStream();
b.compress(Bitmap.CompressFormat.JPEG,60,out);
String s=Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
if(s.length()>900000)throw new Exception("Photo trop grande");
return s;
}
private void sauvegarder(){
    Map<String,Object> m=new HashMap<>();
String email=etEmail.getText().toString().trim();
if(!email.isEmpty())m.put("email",email);
m.put("adresse",etAdresse.getText().toString().trim());
m.put("emploi",etEmploi.getText().toString().trim());
if(photoEnAttente!=null)m.put("photoBase64",photoEnAttente);
btnSauvegarder.setEnabled(false);
firestore.collection("users").document(uid).set(m,SetOptions.merge())
.addOnSuccessListener(v->{
Toast.makeText(this,"Modifications enregistrées.",Toast.LENGTH_SHORT).show();
finish();
})
.addOnFailureListener(e->{
btnSauvegarder.setEnabled(true);
Toast.makeText(this,"Enregistrement impossible: "+e.getMessage(),Toast.LENGTH_LONG).show();
});
}
private String lire(DocumentSnapshot d,String champ){
Object o=d.get(champ);
if(o instanceof String){
String s=((String)o).trim();
return s.isEmpty()?null:s;
}
return null;
}
private String valeur(DocumentSnapshot d,String champ){
String s=lire(d,champ);
return s==null?"":s;
}
private Bitmap decoder(String photo){
if(photo==null||photo.isEmpty())return null;
try{
byte[] b=Base64.decode(photo,Base64.DEFAULT);return BitmapFactory.decodeByteArray(b,0,b.length);
}catch(Exception e){
return null;
}
}
private EditText champ(String hint,int type){
EditText e=new EditText(this);
e.setHint(hint);
e.setHintTextColor(Color.GRAY);
e.setTextColor(Color.WHITE);
e.setInputType(type);
e.setSingleLine(true);
e.setBackgroundColor(Color.rgb(60,60,60));
e.setPadding(dp(14),dp(12),dp(14),dp(12));
return e;
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
