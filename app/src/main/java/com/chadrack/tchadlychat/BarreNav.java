package com.chadrack.tchadlychat;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
public class BarreNav{
public static final int GROUPES=0,ACCUEIL=1,DISCUSSION=2,PROFIL=3;
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
barre.addView(item(a,icones[i],libelles[i],i==actif,cibles[i]));
}
FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,(int)(60*d));
lp.gravity=Gravity.BOTTOM;
root.addView(barre,lp);
}
private static LinearLayout item(final Activity a,String icone,String libelle,final boolean actif,final Class<?> cible){
LinearLayout l=new LinearLayout(a);
l.setOrientation(LinearLayout.VERTICAL);
l.setGravity(Gravity.CENTER);
l.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1f));
TextView t1=new TextView(a);
t1.setText(icone);
t1.setTextSize(20);
t1.setGravity(Gravity.CENTER);
TextView t2=new TextView(a);
t2.setText(libelle);
t2.setTextSize(11);
t2.setGravity(Gravity.CENTER);
t2.setTextColor(actif?Color.rgb(46,180,95):Color.rgb(150,165,185));
if(actif)t2.setTypeface(null,android.graphics.Typeface.BOLD);
l.addView(t1);
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
}
