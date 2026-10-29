package com.chadrack.tchadlychat;import android.app.Activity;import android.content.Intent;import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;
public class ProfileDetailsActivity extends Activity{
private EditText etNaissance,etVille,etAdresse,etEmploi,etBio;
private Button btnContinuer,btnPasser;
private String uid,pseudo;
private FirebaseFirestore firestore;
@Override
protected void onCreate(Bundle savedInstanceState){
super.onCreate(savedInstanceState);
FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
if(user==null){finish();return;}
uid=user.getUid();
pseudo=getIntent().getStringExtra("pseudo");
if(pseudo==null||pseudo.trim().isEmpty())pseudo="Utilisateur";
firestore=FirebaseFirestore.getInstance();
construireUi();
}
private void construireUi(){
ScrollView sv=new ScrollView(this);
sv.setBackgroundColor(Color.rgb(45,45,45));
LinearLayout root=new LinearLayout(this);
root.setOrientation(LinearLayout.VERTICAL);
root.setPadding(dp(24),dp(24),dp(24),dp(24));
TextView titre=new TextView(this);
titre.setText("Mes informations");
titre.setTextColor(Color.WHITE);
titre.setTextSize(26);
titre.setTypeface(null,Typeface.BOLD);
titre.setGravity(Gravity.CENTER);
root.addView(titre,lp(-1,-2,0,0,0,8));
TextView info=new TextView(this);
info.setText("Tous ces champs sont facultatifs. Tu peux les remplir maintenant ou plus tard.");
info.setTextColor(Color.LTGRAY);
info.setTextSize(14);
root.addView(info,lp(-1,-2,0,0,0,16));
etNaissance=champ("Date de naissance (JJ/MM/AAAA)",InputType.TYPE_CLASS_TEXT,true);
root.addView(etNaissance,lp(-1,-2,0,0,0,12));
etVille=champ("Ville",InputType.TYPE_CLASS_TEXT,true);
root.addView(etVille,lp(-1,-2,0,0,0,12));
etAdresse=champ("Adresse",InputType.TYPE_CLASS_TEXT,true);
root.addView(etAdresse,lp(-1,-2,0,0,0,12));
etEmploi=champ("Emploi / profession",InputType.TYPE_CLASS_TEXT,true);
root.addView(etEmploi,lp(-1,-2,0,0,0,12));
etBio=champ("Petite présentation",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE,false);
root.addView(etBio,lp(-1,-2,0,0,0,24));
btnContinuer=bouton("Continuer",Color.rgb(46,180,95));
btnContinuer.setOnClickListener(v->enregistrer());
root.addView(btnContinuer,lp(-1,dp(50),0,0,0,12));
btnPasser=bouton("Passer",Color.rgb(90,90,90));
btnPasser.setOnClickListener(v->allerPhoto());
root.addView(btnPasser,lp(-1,dp(50),0,0,0,0));
sv.addView(root);
setContentView(sv);
}
private EditText champ(String hint,int type,boolean unLigne){
EditText e=new EditText(this);
e.setHint(hint);
e.setHintTextColor(Color.GRAY);
e.setTextColor(Color.WHITE);
e.setInputType(type);
if(unLigne)e.setSingleLine(true);else e.setMinLines(3);
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
private void enregistrer(){
Map<String,Object> m=new HashMap<>();
ajouter(m,"dateNaissance",etNaissance);
ajouter(m,"ville",etVille);
ajouter(m,"adresse",etAdresse);
ajouter(m,"emploi",etEmploi);
ajouter(m,"bio",etBio);
if(m.isEmpty()){allerPhoto();return;}
btnContinuer.setEnabled(false);
btnPasser.setEnabled(false);
firestore.collection("users").document(uid).set(m,SetOptions.merge())
.addOnSuccessListener(v->allerPhoto())
.addOnFailureListener(e->{
btnContinuer.setEnabled(true);
btnPasser.setEnabled(true);
Toast.makeText(this,"Enregistrement impossible: "+e.getMessage(),Toast.LENGTH_LONG).show();
});
}
private void ajouter(Map<String,Object> m,String cle,EditText e){
String v=e.getText().toString().trim();
if(!v.isEmpty())m.put(cle,v);
}
private void allerPhoto(){
Intent i=new Intent(this,ProfileActivity.class);
i.putExtra("pseudo",pseudo);
startActivity(i);
finish();
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
