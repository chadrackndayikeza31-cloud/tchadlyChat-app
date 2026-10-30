package com.chadrack.tchadlychat;import android.app.Activity;import android.content.Intent;import android.graphics.Color;import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
public class BarreNav{
public static final int DISCUSSIONS=0,PUBLICATIONS=1;
public static void ajouter(Activity a,int actif){
float d=a.getResources().getDisplayMetrics().density;
FrameLayout root=(FrameLayout)a.findViewById(android.R.id.content);
LinearLayout barre=new LinearLayout(a);
barre.setOrientation(LinearLayout.HORIZONTAL);
barre.setBackgroundColor(Color.rgb(20,28,40));barre.setGravity(Gravity.CENTER);
barre.addView(bouton(a,"💬 Discussions",actif==DISCUSSIONS,DISCUSSIONS,d));
barre.addView(bouton(a,"📰 Publications",actif==PUBLICATIONS,PUBLICATIONS,d));
FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,(int)(56*d));
lp.gravity=Gravity.BOTTOM;
root.addView(barre,lp);
}
private static TextView bouton(final Activity a,String texte,final boolean actif,final int cible,float d){
TextView b=new TextView(a);
b.setText(texte);
b.setGravity(Gravity.CENTER);
b.setTextSize(15);b.setTextColor(actif?Color.rgb(46,180,95):Color.WHITE);
b.setLayoutParams(new LinearLayout.LayoutParams(0,-1,1f));
b.setOnClickListener(v->{
if(actif)return;
Intent i=new Intent(a,cible==PUBLICATIONS?PostsActivity.class:HomeActivity.class);
i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
a.startActivity(i);
if(a instanceof PostsActivity)a.finish();
});
return b;
}
}
