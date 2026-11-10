package com.chadrack.tchadlychat;import android.app.Activity;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.os.Bundle;import android.view.Gravity;import android.view.View;import android.widget.Button;import android.widget.EditText;import android.widget.LinearLayout;import android.widget.ScrollView;import android.widget.TextView;import android.widget.Toast;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
public class CommentairesActivity extends Activity{
private String postId,moi;
private LinearLayout liste,barreReponse;
private TextView tvReponse;
private EditText etMsg;
private FirebaseFirestore db;
private ListenerRegistration reg;
private final Map<String,String> pseudos=new HashMap<>();
private final List<DocumentSnapshot> tous=new ArrayList<>();
private final Set<String> ouverts=new HashSet<>();
private String replyTo=null;
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
barreReponse=new LinearLayout(this);
barreReponse.setOrientation(LinearLayout.HORIZONTAL);
barreReponse.setGravity(Gravity.CENTER_VERTICAL);
barreReponse.setBackgroundColor(Color.rgb(30,40,58));
barreReponse.setPadding(dp(12),dp(8),dp(8),dp(8));
tvReponse=new TextView(this);
tvReponse.setTextColor(Color.rgb(190,205,225));
tvReponse.setTextSize(14);
barreReponse.addView(tvReponse,new LinearLayout.LayoutParams(0,-2,1f));
TextView fermer=new TextView(this);
fermer.setText("✕");
fermer.setTextColor(Color.WHITE);
fermer.setTextSize(18);
fermer.setPadding(dp(12),0,dp(8),0);
fermer.setOnClickListener(v->annulerReponse());
barreReponse.addView(fermer);
barreReponse.setVisibility(View.GONE);
root.addView(barreReponse,new LinearLayout.LayoutParams(-1,-2));
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
tous.clear();
tous.addAll(snap.getDocuments());
afficher();
});
}
@Override
protected void onStop(){
if(reg!=null){reg.remove();reg=null;}
super.onStop();
}
private long temps(DocumentSnapshot d){
Timestamp t=d.getTimestamp("createdAt");
return t==null?0L:t.toDate().getTime();
}
private void afficher(){
liste.removeAllViews();
List<DocumentSnapshot> racines=new ArrayList<>();
Map<String,List<DocumentSnapshot>> reps=new HashMap<>();
for(DocumentSnapshot d:tous){
String p=d.getString("parentId");
if(p==null||p.isEmpty()){
racines.add(d);
}else{
if(!reps.containsKey(p))reps.put(p,new ArrayList<DocumentSnapshot>());
reps.get(p).add(d);
}
}
Collections.sort(racines,(x,y)->Long.compare(temps(x),temps(y)));
for(List<DocumentSnapshot> l:reps.values())Collections.sort(l,(x,y)->Long.compare(temps(x),temps(y)));
for(DocumentSnapshot r:racines){
liste.addView(ligne(r,false));
List<DocumentSnapshot> enfants=reps.get(r.getId());
if(enfants!=null&&!enfants.isEmpty()){
liste.addView(resume(r.getId(),enfants));
if(ouverts.contains(r.getId())){
for(DocumentSnapshot e:enfants)liste.addView(ligne(e,true));
}
}
}
}
private void chargerPseudo(final String uid){
if(uid==null||pseudos.containsKey(uid))return;
db.collection("users").document(uid).get().addOnSuccessListener(us->{
String p=us.getString("pseudo");
pseudos.put(uid,p==null?"Utilisateur":p);
afficher();
});
}
private TextView resume(final String parentId,List<DocumentSnapshot> enfants){
Set<String> auteurs=new HashSet<>();
List<String> ordre=new ArrayList<>();
for(DocumentSnapshot e:enfants){
String a=e.getString("author");
if(a!=null&&!auteurs.contains(a)){
auteurs.add(a);
ordre.add(a);
}
}
String premier=ordre.isEmpty()?"":ordre.get(0);
chargerPseudo(premier);
String nomTxt=pseudos.containsKey(premier)?pseudos.get(premier):"…";
if(auteurs.size()>1)nomTxt+=" et les autres";
int n=enfants.size();
boolean ouvert=ouverts.contains(parentId);
TextView t=new TextView(this);
t.setText((ouvert?"▴ Masquer ":"▾ Voir ")+n+(n>1?" réponses":" réponse")+" · "+nomTxt);
t.setTextColor(Color.rgb(109,40,217));
t.setTextSize(13);
t.setTypeface(null,Typeface.BOLD);
t.setPadding(dp(28),dp(4),0,dp(8));
t.setOnClickListener(v->{
if(ouvert)ouverts.remove(parentId);
else ouverts.add(parentId);
afficher();
});
return t;
}
private LinearLayout ligne(final DocumentSnapshot d,boolean reponse){
final String auteur=d.getString("author");
final String texte=d.getString("text");
final Timestamp ts=d.getTimestamp("createdAt");
Object ro=d.get("reactions");
final Map<?,?> reacts=ro instanceof Map?(Map<?,?>)ro:new HashMap<String,Object>();
Object mo=reacts.get(moi);
final String mien=mo instanceof String?(String)mo:null;
LinearLayout c=new LinearLayout(this);
c.setOrientation(LinearLayout.VERTICAL);
c.setPadding(dp(12),dp(10),dp(12),dp(10));
GradientDrawable f=new GradientDrawable();
f.setCornerRadius(dp(12));
f.setColor(Color.rgb(30,40,58));
c.setBackground(f);
LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
lp.setMargins(reponse?dp(28):0,0,0,dp(8));
c.setLayoutParams(lp);
chargerPseudo(auteur);
TextView nom=new TextView(this);
nom.setText(auteur!=null&&pseudos.containsKey(auteur)?pseudos.get(auteur):"…");
nom.setTextColor(Color.rgb(46,180,95));
nom.setTextSize(14);
nom.setTypeface(null,Typeface.BOLD);
c.addView(nom);
TextView t=new TextView(this);
t.setText(texte==null?"":texte);
t.setTextColor(Color.WHITE);
t.setTextSize(15);
t.setPadding(0,dp(4),0,dp(4));
c.addView(t);
StringBuilder rs=new StringBuilder();
String[] em={"👍","❤️","😂","😮","😢"};
for(String e:em){
int n=0;
for(Object val:reacts.values())if(e.equals(val))n++;
if(n>0)rs.append(e).append(" ").append(n).append("   ");
}
if(rs.length()>0){
TextView tr=new TextView(this);
tr.setText(rs.toString().trim());
tr.setTextColor(Color.rgb(200,210,225));
tr.setTextSize(13);
c.addView(tr);
}
LinearLayout actions=new LinearLayout(this);
actions.setGravity(Gravity.CENTER_VERTICAL);
actions.setPadding(0,dp(6),0,0);
TextView date=new TextView(this);
date.setText(ts==null?"":new SimpleDateFormat("dd/MM HH:mm",Locale.FRANCE).format(ts.toDate()));
date.setTextColor(Color.rgb(150,165,185));
date.setTextSize(11);
actions.addView(date,new LinearLayout.LayoutParams(0,-2,1f));
if(!reponse){
TextView rep=new TextView(this);
rep.setText("↩ Répondre");
rep.setTextColor(Color.rgb(46,180,95));
rep.setTextSize(13);
rep.setTypeface(null,Typeface.BOLD);
rep.setPadding(dp(8),dp(4),dp(8),dp(4));
rep.setOnClickListener(v->commencerReponse(d.getId(),auteur));
actions.addView(rep);
}
if(moi.equals(auteur)){
TextView sup=new TextView(this);
sup.setText("🗑 Supprimer");
sup.setTextColor(Color.rgb(230,90,90));
sup.setTextSize(13);
sup.setPadding(dp(8),dp(4),dp(8),dp(4));
sup.setOnClickListener(v->supprimer(d));
actions.addView(sup);
}
c.addView(actions);
c.setOnLongClickListener(v->{
choisirReaction(d.getId(),mien);
return true;
});
return c;
}
private void choisirReaction(final String id,final String mien){
final String[] em={"👍","❤️","😂","😮","😢"};
new android.app.AlertDialog.Builder(this)
.setTitle("Réagir")
.setItems(em,(x,w)->reagir(id,em[w],em[w].equals(mien)))
.show();
}
private void reagir(String id,String e,boolean actif){
Object val=actif?FieldValue.delete():e;
db.collection("comments").document(id).update("reactions."+moi,val)
.addOnFailureListener(x->toast("Réaction impossible."));
}
private void commencerReponse(String parentId,String auteur){
replyTo=parentId;
String nom=auteur!=null&&pseudos.containsKey(auteur)?pseudos.get(auteur):"…";
tvReponse.setText("↩ Réponse à "+nom);
barreReponse.setVisibility(View.VISIBLE);
etMsg.requestFocus();
}
private void annulerReponse(){
replyTo=null;
barreReponse.setVisibility(View.GONE);
}
private void supprimer(DocumentSnapshot d){
db.collection("comments").document(d.getId()).delete()
.addOnSuccessListener(v->db.collection("posts").document(postId).update("commentCount",FieldValue.increment(-1)))
.addOnFailureListener(e->toast("Suppression impossible."));
}
private void envoyer(){
String t=etMsg.getText().toString().trim();
if(t.isEmpty())return;
if(t.length()>500){
toast("Commentaire trop long (max 500 caractères).");
return;
}
final String parent=replyTo;
Map<String,Object> m=new HashMap<>();
m.put("postId",postId);
m.put("author",moi);
m.put("text",t);
m.put("createdAt",Timestamp.now());
m.put("parentId",parent==null?"":parent);
db.collection("comments").add(m)
.addOnSuccessListener(r->{
etMsg.setText("");
if(parent!=null)ouverts.add(parent);
annulerReponse();
db.collection("posts").document(postId).update("commentCount",FieldValue.increment(1));
})
.addOnFailureListener(e->toast("Envoi impossible."));
}
private void toast(final String s){
Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
}
private int dp(int v){
return(int)(v*getResources().getDisplayMetrics().density+0.5f);
}
}

