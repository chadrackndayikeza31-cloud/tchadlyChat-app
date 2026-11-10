package com.chadrack.tchadlychat;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.List;
public class BarreNav{
public static final int GROUPES=0,ACCUEIL=1,DISCUSSION=2,PROFIL=3;
private static TextView badgeDiscussion;
private static int nonLus=0;
private static ListenerRegistration regNonLus;
private static String uidEcoute=null;
public static void ajouter(final Activity a,int actif){
float d=a.getResources().getDisplayMetrics().density;
FrameLayout root=(FrameLayout)a.findViewById(android.R.id.content);
LinearLayout barre=new LinearLayout(a);
barre.setOrientation(LinearLayout.HORIZONTAL);
barre.setBackgroundColor(Color.rgb(18,22,32));
barre.setGravity(Gravity.CENTER);
String[] icones={"👥","🏠","💬","👤"};
String[] libelles={"Groupes","Accueil","Discussion","Profil"};
Class<?>[] cibles={HomeActivity.class,PostsActivity.class,DiscussionsActivity.class,ProfilActivity.class};
for(int i=0;i<4;i++){
barre.addView(item(a,icones[i],libelles[i],i==actif,cibles[i],i,d));
}
FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,(int)(60*d));
lp.gravity=Gravity.BOTTOM;
root.addView(barre,lp);
ecouterNonLus();
}
private static LinearLayout item(final Activity a,String icone,String libelle,final boolean actif,final Class<?> cible,int index,float d){
LinearLayout l=new LinearLayout(a);
l.setOrientation(LinearLayout.VERTICAL);
l.setGravity(Gravity.CENTER);
l.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1f));
FrameLayout cadreIcone=new FrameLayout(a);
TextView t1=new TextView(a);
t1.setText(icone);
t1.setTextSize(20);
t1.setGravity(Gravity.CENTER);
cadreIcone.addView(t1,new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));
if(index==DISCUSSION){
TextView badge=new TextView(a);
badge.setTextColor(Color.WHITE);
badge.setTextSize(11);
badge.setTypeface(null,android.graphics.Typeface.BOLD);
badge.setGravity(Gravity.CENTER);
badge.setPadding((int)(6*d),0,(int)(6*d),0);
GradientDrawable rouge=new GradientDrawable();
rouge.setCornerRadius(9*d);
rouge.setColor(Color.rgb(220,38,38));
badge.setBackground(rouge);
FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-2,(int)(18*d),Gravity.TOP|Gravity.END);
bp.setMargins(0,0,(int)(-14*d),0);
cadreIcone.addView(badge,bp);
badgeDiscussion=badge;
majBadge();
}
TextView t2=new TextView(a);
t2.setText(libelle);
t2.setTextSize(11);
t2.setGravity(Gravity.CENTER);
t2.setTextColor(actif?Color.rgb(46,180,95):Color.rgb(150,165,185));
if(actif)t2.setTypeface(null,android.graphics.Typeface.BOLD);
l.addView(cadreIcone);
l.addView(t2);
l.setOnClickListener(v->{
if(actif)return;
Intent i=new Intent(a,cible);
i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
a.startActivity(i);
if(!a.getClass().equals(cible))a.finish();
});
return l;
}
private static void ecouterNonLus(){
FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
if(u==null)return;
if(regNonLus!=null&&u.getUid().equals(uidEcoute))return;
if(regNonLus!=null)regNonLus.remove();
final String moi=u.getUid();
uidEcoute=moi;
regNonLus=FirebaseFirestore.getInstance().collection("private_messages")
.whereEqualTo("receiver",moi)
.whereEqualTo("seen",false)
.addSnapshotListener((s,e)->{
if(e!=null||s==null)return;
int n=0;
for(DocumentSnapshot d:s.getDocuments()){
if(Boolean.TRUE.equals(d.getBoolean("deleted")))continue;
Object h=d.get("hiddenFor");
if(h instanceof List&&((List<?>)h).contains(moi))continue;
n++;
}
nonLus=n;
majBadge();
});
}
private static void majBadge(){
if(badgeDiscussion==null)return;
badgeDiscussion.setText(nonLus>99?"99+":String.valueOf(nonLus));
badgeDiscussion.setVisibility(nonLus>0?View.VISIBLE:View.GONE);
}
}

