package com.chadrack.tchadlychat;import android.app.Activity;import android.content.Intent;import android.graphics.Bitmap;import android.graphics.BitmapFactory;import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
public class PostsActivity extends Activity{
private String moi;
private LinearLayout liste;
private FirebaseFirestore db;
private ListenerRegistration reg;
private boolean populaires=false;
private final android.os.Handler h=new android.os.Handler(android.os.Looper.getMainLooper());
private final Runnable maj=new Runnable(){public void run(){afficher();h.postDelayed(this,60000L);}};
private TextView tabRecentes,tabPopulaires;
private final List<DocumentSnapshot> docs=new ArrayList<>();
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
liste.setPadding(dp(12),dp(12),dp(12),dp(70));
sv.addView(liste);
FrameLayout.LayoutParams lpSv=new FrameLayout.LayoutParams(-1,-1);
lpSv.topMargin=dp(100);
root.addView(sv,lpSv);
LinearLayout top=new LinearLayout(this);
top.setOrientation(LinearLayout.VERTICAL);
top.setBackgroundColor(Color.rgb(20,28,40));
LinearLayout haut=new LinearLayout(this);
haut.setOrientation(LinearLayout.HORIZONTAL);
haut.setGravity(Gravity.CENTER_VERTICAL);
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
top.addView(haut,new LinearLayout.LayoutParams(-1,dp(56)));
LinearLayout onglets=new LinearLayout(this);
onglets.setOrientation(LinearLayout.HORIZONTAL);
tabRecentes=onglet("Récentes",true);
tabPopulaires=onglet("Populaires",false);
tabRecentes.setOnClickListener(v->choisirOnglet(false));
tabPopulaires.setOnClickListener(v->choisirOnglet(true));
onglets.addView(tabRecentes,new LinearLayout.LayoutParams(0,-1,1f));
onglets.addView(tabPopulaires,new LinearLayout.LayoutParams(0,-1,1f));
top.addView(onglets,new LinearLayout.LayoutParams(-1,dp(44)));
FrameLayout.LayoutParams lpTop=new FrameLayout.LayoutParams(-1,dp(100));
lpTop.gravity=Gravity.TOP;
root.addView(top,lpTop);
setContentView(root);
BarreNav.ajouter(this,BarreNav.ACCUEIL);
}
private TextView onglet(String texte,boolean actif){
TextView t=new TextView(this);
t.setText(texte);
t.setGravity(Gravity.CENTER);
t.setTextSize(15);
t.setTypeface(null,Typeface.BOLD);
t.setTextColor(actif?Color.rgb(46,180,95):Color.rgb(150,165,185));
return t;
}
private void choisirOnglet(boolean pop){
populaires=pop;
tabRecentes.setTextColor(pop?Color.rgb(150,165,185):Color.rgb(46,180,95));
tabPopulaires.setTextColor(pop?Color.rgb(46,180,95):Color.rgb(150,165,185));
afficher();
}
private void afficher(){
liste.removeAllViews();
List<DocumentSnapshot> l=new ArrayList<>(docs);
if(populaires)Collections.sort(l,(x,y)->Integer.compare(totalReactions(y),totalReactions(x)));
for(DocumentSnapshot d:l)liste.addView(carte(d));
}
private int totalReactions(DocumentSnapshot d){
Object ro=d.get("reactions");
return ro instanceof Map?((Map<?,?>)ro).size():0;
}
@Override
protected void onStart(){
super.onStart();
h.postDelayed(maj,60000L);
reg=db.collection("posts").orderBy("createdAt",Query.Direction.DESCENDING).limit(50).addSnapshotListener((snap,e)->{
if(e!=null||snap==null)return;
docs.clear();
docs.addAll(snap.getDocuments());
afficher();
});
}
@Override
protected void onStop(){
if(reg!=null){reg.remove();reg=null;}
h.removeCallbacks(maj);
super.onStop();
}
private View carte(DocumentSnapshot d){
final String id=d.getId();
final String auteur=d.getString("author");
final String texte=d.getString("text");
final String type=d.getString("mediaType");
final String url=d.getString("mediaUrl");
final Timestamp ts=d.getTimestamp("createdAt");
Boolean ed=d.getBoolean("edited");
final boolean modifie=ed!=null&&ed;
Long nc=d.getLong("commentCount");
final long nbCom=nc==null?0L:nc;
Object ro=d.get("reactions");
final Map<?,?> reacts=ro instanceof Map?(Map<?,?>)ro:new HashMap<String,Object>();
Object mo=reacts.get(moi);
final String mien=mo instanceof String?(String)mo:null;
final boolean estMoi=moi.equals(auteur);
final LinearLayout c=new LinearLayout(this);
c.setOrientation(LinearLayout.VERTICAL);
c.setPadding(dp(14),dp(12),dp(14),dp(10));
GradientDrawable f=new GradientDrawable();
f.setCornerRadius(dp(16));
f.setColor(Color.rgb(22,28,42));
f.setStroke(dp(2),Color.rgb(124,58,237));
c.setBackground(f);
LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
lp.setMargins(0,0,0,dp(14));
c.setLayoutParams(lp);
if(auteur!=null&&!estMoi){
db.collection("blocks").document(moi+"_"+auteur).get().addOnSuccessListener(bd->{
if(bd.exists())c.setVisibility(View.GONE);
});
}
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
en.addView(av,new LinearLayout.LayoutParams(dp(46),dp(46)));
LinearLayout info=new LinearLayout(this);
info.setOrientation(LinearLayout.VERTICAL);
LinearLayout.LayoutParams pi=new LinearLayout.LayoutParams(0,-2,1f);
pi.setMargins(dp(10),0,dp(6),0);
en.addView(info,pi);
final TextView nom=new TextView(this);
nom.setText("…");
nom.setTextColor(Color.WHITE);
nom.setTextSize(16);
nom.setTypeface(null,Typeface.BOLD);
info.addView(nom);
TextView date=new TextView(this);
String dateTxt=ts==null?"":ilYA(ts.toDate().getTime());
date.setText(modifie?dateTxt+" · modifié":dateTxt);
date.setTextColor(Color.rgb(150,165,185));
date.setTextSize(12);
info.addView(date);
final TextView btnMenu=new TextView(this);
btnMenu.setText("⋮");
btnMenu.setTextColor(Color.WHITE);
btnMenu.setTextSize(22);
btnMenu.setGravity(Gravity.CENTER);
GradientDrawable rond=new GradientDrawable();
rond.setShape(GradientDrawable.OVAL);
rond.setColor(Color.rgb(55,70,95));
btnMenu.setBackground(rond);
en.addView(btnMenu,new LinearLayout.LayoutParams(dp(40),dp(40)));
btnMenu.setOnClickListener(x->menuPublication(btnMenu,id,auteur,texte,url,estMoi,mien));
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
LinearLayout.LayoutParams pim=new LinearLayout.LayoutParams(-1,dp(260));
pim.setMargins(0,dp(8),0,0);
c.addView(im,pim);
chargerUrl(im,url);
im.setOnClickListener(x->ouvrirPhoto(url));
}
if("video".equals(type)&&url!=null&&!url.isEmpty()){
LinearLayout.LayoutParams pv=new LinearLayout.LayoutParams(-1,-2);
pv.setMargins(0,dp(8),0,0);
c.addView(VideoThumb.creer(this,url,40,x->{
Intent i=new Intent(this,VideoPlayerActivity.class);
i.putExtra("videoUrl",url);
startActivity(i);
}),pv);
}
int[] cnt=new int[3];
for(Object val:reacts.values()){
if("👍".equals(val))cnt[0]++;
else if("❤️".equals(val))cnt[1]++;
else if("😂".equals(val))cnt[2]++;
}
LinearLayout cntRow=new LinearLayout(this);
cntRow.setGravity(Gravity.CENTER_VERTICAL);
cntRow.setPadding(0,dp(10),0,dp(8));
TextView tReac=new TextView(this);
tReac.setText("👍 "+cnt[0]+"   ❤️ "+cnt[1]+"   😂 "+cnt[2]);
tReac.setTextColor(Color.rgb(200,210,225));
tReac.setTextSize(14);
cntRow.addView(tReac,new LinearLayout.LayoutParams(0,-2,1f));
TextView tCom=new TextView(this);
tCom.setText(nbCom+(nbCom>1?" commentaires":" commentaire"));
tCom.setTextColor(Color.rgb(150,165,185));
tCom.setTextSize(13);
cntRow.addView(tCom);
c.addView(cntRow);
View sep=new View(this);
sep.setBackgroundColor(Color.rgb(50,60,80));
c.addView(sep,new LinearLayout.LayoutParams(-1,dp(1)));
LinearLayout actions=new LinearLayout(this);
actions.setOrientation(LinearLayout.HORIZONTAL);
actions.setPadding(0,dp(10),0,0);
boolean aimé="👍".equals(mien);
Button jaime=pill("👍 J'aime",aimé?Color.rgb(37,99,235):Color.rgb(40,50,70));
jaime.setOnClickListener(x->reagir(id,"👍",aimé));
LinearLayout.LayoutParams pa=new LinearLayout.LayoutParams(0,dp(44),1f);
pa.setMargins(0,0,dp(6),0);
actions.addView(jaime,pa);
Button comm=pill("💬 Commenter",Color.rgb(109,40,217));
comm.setOnClickListener(x->ouvrirCommentaires(id));
LinearLayout.LayoutParams pc=new LinearLayout.LayoutParams(0,dp(44),1f);
pc.setMargins(0,0,dp(6),0);
actions.addView(comm,pc);
Button part=pill("↗ Partager",Color.rgb(40,50,70));
part.setOnClickListener(x->partager(texte,url));
actions.addView(part,new LinearLayout.LayoutParams(0,dp(44),1f));
c.addView(actions);
return c;
}
private void menuPublication(View ancre,final String id,final String auteur,final String texte,final String url,final boolean estMoi,final String mien){
PopupMenu pm=new PopupMenu(this,ancre);
pm.getMenu().add(0,1,0,"👍 Interaction · J'aime / Réagir");
pm.getMenu().add(0,2,1,"💬 Commentaires · Voir / Ajouter");
if(estMoi){
if(texte!=null&&!texte.isEmpty())pm.getMenu().add(0,3,2,"✏ Modifier · Éditer ce post");
pm.getMenu().add(0,4,3,"🗑 Supprimer · Supprimer ce post");
}else{
pm.getMenu().add(0,6,3,"🚩 Signaler · Signaler ce post");
}
pm.getMenu().add(0,5,4,"↗ Partager · Envoyer ce post");
pm.setOnMenuItemClickListener(it->{
switch(it.getItemId()){
case 1:choisirReaction(id,mien);break;
case 2:ouvrirCommentaires(id);break;
case 3:modifier(id,texte);break;
case 4:supprimer(id);break;
case 5:partager(texte,url);break;
case 6:signaler(id,auteur);break;
}
return true;
});
pm.show();
}
private void choisirReaction(final String id,final String mien){
final String[] em={"👍","❤️","😂"};
new android.app.AlertDialog.Builder(this)
.setTitle("Réagir")
.setItems(em,(x,w)->reagir(id,em[w],em[w].equals(mien)))
.show();
}
private void ouvrirCommentaires(String id){
Intent i=new Intent(this,CommentairesActivity.class);
i.putExtra("postId",id);
startActivity(i);
}
private void supprimer(String id){
db.collection("posts").document(id).delete()
.addOnFailureListener(e->toast("Suppression impossible."));
}
private void partager(String texte,String url){
StringBuilder sb=new StringBuilder();
if(texte!=null&&!texte.isEmpty())sb.append(texte);
if(url!=null&&!url.isEmpty()){
if(sb.length()>0)sb.append("\n");
sb.append(url);
}
if(sb.length()==0){
toast("Rien à partager.");
return;
}
Intent i=new Intent(Intent.ACTION_SEND);
i.setType("text/plain");
i.putExtra(Intent.EXTRA_TEXT,sb.toString());
startActivity(Intent.createChooser(i,"Partager via"));
}
private void signaler(final String id,final String auteur){
if(auteur==null)return;
final String[] motifs={"Spam","Harcèlement","Contenu inapproprié","Arnaque","Autre"};
new android.app.AlertDialog.Builder(this)
.setTitle("Signaler la publication")
.setItems(motifs,(x,w)->{
Map<String,Object> m=new HashMap<>();
m.put("reporter",moi);
m.put("reported",auteur);
m.put("postId",id);
m.put("type","post");
m.put("motif",motifs[w]);
m.put("createdAt",Timestamp.now());
db.collection("reports").add(m)
.addOnSuccessListener(r->toast("Signalement envoyé. Merci."))
.addOnFailureListener(e->toast("Signalement impossible."));
})
.setNegativeButton("Annuler",null)
.show();
}
private void modifier(final String id,String ancien){
final EditText et=new EditText(this);
et.setText(ancien);
et.setMinLines(3);
et.setGravity(Gravity.TOP);
new android.app.AlertDialog.Builder(this)
.setTitle("Modifier la publication")
.setView(et)
.setPositiveButton("Enregistrer",(x,w)->{
String t=et.getText().toString().trim();
if(t.isEmpty()||t.length()>1000){
toast("Texte vide ou trop long (max 1000 caractères).");
return;
}
db.collection("posts").document(id).update("text",t,"edited",true,"editedAt",Timestamp.now())
.addOnFailureListener(e->toast("Modification impossible."));
})
.setNegativeButton("Annuler",null)
.show();
}
private void reagir(String id,String e,boolean actif){
Object val=actif?FieldValue.delete():e;
db.collection("posts").document(id).update("reactions."+moi,val)
.addOnFailureListener(x->toast("Réaction impossible."));
}
private void ouvrirPhoto(final String url){
new Thread(()->{
try{
HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
c.setConnectTimeout(15000);
c.setReadTimeout(30000);
ByteArrayOutputStream o=new ByteArrayOutputStream();
try(InputStream in=c.getInputStream()){
byte[] buf=new byte[65536];
int n;
while((n=in.read(buf))!=-1)o.write(buf,0,n);
}
final String b64=Base64.encodeToString(o.toByteArray(),Base64.NO_WRAP);
runOnUiThread(()->{
Intent i=new Intent(this,PhotoViewerActivity.class);
i.putExtra("imageBase64",b64);
startActivity(i);
});
}catch(Exception e){
toast("Photo indisponible.");
}
}).start();
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
private String ilYA(long ms){
long s=(System.currentTimeMillis()-ms)/1000L;
if(s<60)return "maintenant";
long min=s/60L;
if(min<60)return min+"min";
long h=min/60L;
if(h<24)return h+"h";
long j=h/24L;
if(j<7)return j+"j";
if(j<364)return (j/7L)+"semaine";
return Math.max(1L,j/365L)+"an";
}
private void toast(final String s){
runOnUiThread(()->Toast.makeText(this,s,Toast.LENGTH_SHORT).show());
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
private Button pill(String texte,int couleur){
Button b=new Button(this);
b.setText(texte);
b.setAllCaps(false);
b.setTextColor(Color.WHITE);
b.setTextSize(13);
GradientDrawable g=new GradientDrawable();
g.setCornerRadius(dp(22));
g.setColor(couleur);
b.setBackground(g);
b.setMinHeight(0);
b.setMinimumHeight(0);
return b;
}
private int dp(int v){
return(int)(v*getResources().getDisplayMetrics().density+0.5f);
}
}
