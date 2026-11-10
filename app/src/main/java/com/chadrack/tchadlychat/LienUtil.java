package com.chadrack.tchadlychat;import android.content.Context;import android.content.Intent;import android.graphics.Color;import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.util.Linkify;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class LienUtil{
private static final Pattern URL_PATTERN=Pattern.compile("(?i)\\b((?:https?://|www\\.)[^\\s]+)");
public static void ajouterTexte(final Context c,LinearLayout parent,String texte){
String t=texte==null?"":texte;
TextView tv=new TextView(c);
tv.setText(t);
tv.setTextColor(Color.WHITE);
tv.setTextSize(16);
tv.setLinkTextColor(Color.rgb(120,190,255));
Linkify.addLinks(tv,Linkify.WEB_URLS);
parent.addView(tv);
Matcher m=URL_PATTERN.matcher(t);
if(m.find()){
parent.addView(carte(c,m.group(1)));
}
}
private static LinearLayout carte(final Context c,final String brut){
final String url=brut.startsWith("http")?brut:"https://"+brut;
String domaine;
try{
domaine=Uri.parse(url).getHost();
}catch(Exception e){
domaine=null;
}
if(domaine==null||domaine.isEmpty())domaine=brut;
LinearLayout card=new LinearLayout(c);
card.setOrientation(LinearLayout.VERTICAL);
card.setPadding(dp(c,12),dp(c,10),dp(c,12),dp(c,10));
GradientDrawable f=new GradientDrawable();
f.setCornerRadius(dp(c,10));
f.setColor(Color.rgb(20,30,45));
f.setStroke(dp(c,1),Color.rgb(60,90,130));
card.setBackground(f);
LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
lp.setMargins(0,dp(c,8),0,0);
card.setLayoutParams(lp);
TextView d=new TextView(c);
d.setText("🔗 "+domaine);
d.setTextColor(Color.rgb(120,190,255));
d.setTextSize(13);
TextView o=new TextView(c);
o.setText("Ouvrir le lien");
o.setTextColor(Color.rgb(200,210,225));
o.setTextSize(12);
o.setPadding(0,dp(c,4),0,0);
card.addView(d);
card.addView(o);
card.setOnClickListener(v->{
Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(url));
i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
c.startActivity(i);
});
return card;
}
private static int dp(Context c,int v){
return (int)(v*c.getResources().getDisplayMetrics().density+0.5f);
}
}

