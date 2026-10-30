package com.chadrack.tchadlychat;import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class CommentairesActivity extends Activity{
private String postId,moi;
private LinearLayout liste;
private EditText etMsg;
private FirebaseFirestore db;
private ListenerRegistration reg;
private final Map<String,String> pseudos=new HashMap<>();
@Override
protected void onCreate(Bundle b){
super.onCreate(b);
postId=getIntent().getStringExtra("postId");
FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
if(postId==null||u==null){finish();return;}
moi=u.getUid();
db=FirebaseFirestore.getInstance();
LinearLayout root=new LinearLayout(this);
root.setOrientation(LinearLayout.VERTICAL);
root.setBackgroundColor(Color.rgb(18,24,34));
TextView titre=new TextView(this);
titre.setText("Commentaires");
titre.setTextColor(Color.WHITE);
titre.setTextSize(20);
titre.setTypeface(null,Typeface.BOLD);
titre.setBackgroundColor(Color.rgb(20,28,40));
titre.setPadding(dp(16),dp(14),dp(16),dp(14));
root.addView(titre,new LinearLayout.LayoutParams(-1,-2));
ScrollView sv=new ScrollView(this);
liste=new LinearLayout(this);
liste.setOrientation(LinearLayout.VERTICAL);
liste.setPadding(dp(12),dp(12),dp(12),dp(12));
sv.addView(liste);
root.addView(sv,new LinearLayout.LayoutParams(-1,0,1f));
LinearLayout bas=new LinearLayout(this);
bas.setOrientation(LinearLayout.HORIZONTAL);
bas.setGravity(Gravity.CENTER_VERTICAL);
bas.setBackgroundColor(Color.rgb(20,28,40));
bas.setPadding(dp(8),dp(8),dp(8),dp(8));
etMsg=new EditText(this);
etMsg.setHint("Écrire un commentaire…");
etMsg.setHintTextColor(Color.GRAY);
etMsg.setTextColor(Color.WHITE);
etMsg.setSingleLine(true);
etMsg.setBackgroundColor(Color.rgb(60,60,60));
etMsg.setPadding(dp(12),dp(10),dp(12),dp(10));
bas.addView(etMsg,new LinearLayout.LayoutParams(0,-2,1f));
Button envoyer=new Button(this);
envoyer.setText("Envoyer");
envoyer.setAllCaps(false);
envoyer.setTextColor(Color.WHITE);
envoyer.setBackgroundColor(Color.rgb(46,180,95));
envoyer.setOnClickListener(v->envoyer());
bas.addView(envoyer,new LinearLayout.LayoutParams(-2,-2));
root.addView(bas,new LinearLayout.LayoutParams(-1,-2));
setContentView(root);
}
@Override
protected void onStart(){
super.onStart();
reg=db.collection("comments").whereEqualTo("postId",postId).addSnapshotListener((snap,e)->{
if(e!=null||snap==null)return;
List<DocumentSnapshot> l=new ArrayList<>(snap.getDocuments());
Collections.sort(l,(x,y)->{
Timestamp tx=x.getTimestamp("createdAt");
Timestamp ty=y.getTimestamp("createdAt");
long a=tx==null?0L:tx.toDate().getTime();
long c=ty==null?0L:ty.toDate().getTime();
return Long.compare(a,c);
});
liste.removeAllViews();
for(DocumentSnapshot d:l)liste.addView(ligne(d));
});
}
@Override
protected void onStop(){
if(reg!=null){reg.remove();reg=null;}
super.onStop();
}
private LinearLayout ligne(DocumentSnapshot d){
final String auteur=d.getString("author");
final String texte=d.getString("text");
final Timestamp ts=d.getTimestamp("createdAt");
LinearLayout c=new LinearLayout(this);
c.setOrientation(LinearLayout.VERTICAL);
c.setPadding(dp(12),dp(10),dp(12),dp(10));
GradientDrawable f=new GradientDrawable();
f.setCornerRadius(dp(12));
f.setColor(Color.rgb(30,40,58));
c.setBackground(f);
LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
lp.setMargins(0,0,0,dp(10));
c.setLayoutParams(lp);
final TextView nom=new TextView(this);
nom.setText("…");
nom.setTextColor(Color.rgb(46,180,95));
nom.setTextSize(14);
nom.setTypeface(null,Typeface.BOLD);
c.addView(nom);
if(auteur!=null){
if(pseudos.containsKey(auteur)){
nom.setText(pseudos.get(auteur));
}else{
db.collection("users").document(auteur).get().addOnSuccessListener(us->{
String p=us.getString("pseudo");
String v=p==null?"Utilisateur":p;
pseudos.put(auteur,v);
nom.setText(v);
});
}
}
TextView t=new TextView(this);
t.setText(texte==null?"":texte);
t.setTextColor(Color.WHITE);
t.setTextSize(15);
t.setPadding(0,dp(4),0,dp(4));
c.addView(t);
TextView date=new TextView(this);
date.setText(ts==null?"":new SimpleDateFormat("dd/MM HH:mm",Locale.FRANCE).format(ts.toDate()));
date.setTextColor(Color.rgb(150,165,185));
date.setTextSize(11);
c.addView(date);
if(moi.equals(auteur)){
c.setOnLongClickListener(v->{
db.collection("posts").document(postId).update("commentCount",FieldValue.increment(-1));
db.collection("comments").document(d.getId()).delete();
Toast.makeText(this,"Commentaire supprimé.",Toast.LENGTH_SHORT).show();
return true;
});
}
return c;
}
private void envoyer(){
String t=etMsg.getText().toString().trim();
if(t.isEmpty())return;
if(t.length()>500){
Toast.makeText(this,"Commentaire trop long (max 500 caractères).",Toast.LENGTH_SHORT).show();
return;
}
Map<String,Object> m=new HashMap<>();
m.put("postId",postId);
m.put("author",moi);
m.put("text",t);
m.put("createdAt",Timestamp.now());
db.collection("posts").document(postId).update("commentCount",FieldValue.increment(1));
db.collection("comments").add(m)
.addOnSuccessListener(r->etMsg.setText(""))
.addOnFailureListener(e->Toast.makeText(this,"Envoi impossible.",Toast.LENGTH_SHORT).show());
}
private int dp(int v){
return(int)(v*getResources().getDisplayMetrics().density+0.5f);
}
}
