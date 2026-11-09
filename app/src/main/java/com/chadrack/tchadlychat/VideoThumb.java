package com.chadrack.tchadlychat;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.HashMap;
public class VideoThumb{
public static FrameLayout creer(final Activity a,final String url,int margeDp,View.OnClickListener onClick){
final float d=a.getResources().getDisplayMetrics().density;
final int largeur=a.getResources().getDisplayMetrics().widthPixels-(int)(margeDp*d);
final FrameLayout cadre=new FrameLayout(a);
cadre.setBackgroundColor(Color.rgb(20,24,32));
cadre.setMinimumWidth(largeur);
cadre.setMinimumHeight((int)(largeur*9f/16f));
final ImageView im=new ImageView(a);
im.setScaleType(ImageView.ScaleType.FIT_CENTER);
cadre.addView(im,new FrameLayout.LayoutParams(-1,-1));
TextView play=new TextView(a);
play.setText("▶");
play.setTextColor(Color.WHITE);
play.setTextSize(24);
play.setGravity(Gravity.CENTER);
GradientDrawable rond=new GradientDrawable();
rond.setShape(GradientDrawable.OVAL);
rond.setColor(Color.argb(160,0,0,0));
play.setBackground(rond);
cadre.addView(play,new FrameLayout.LayoutParams((int)(56*d),(int)(56*d),Gravity.CENTER));
cadre.setOnClickListener(onClick);
new Thread(()->{
try{
MediaMetadataRetriever r=new MediaMetadataRetriever();
r.setDataSource(url,new HashMap<String,String>());
String sw=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
String sh=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
String rot=r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
int w=sw==null?16:Integer.parseInt(sw);
int h=sh==null?9:Integer.parseInt(sh);
if("90".equals(rot)||"270".equals(rot)){int t=w;w=h;h=t;}
final Bitmap fr=r.getFrameAtTime(0,MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
r.release();
final int hauteur=Math.max((int)(160*d),Math.min((int)(largeur*(float)h/Math.max(1,w)),(int)(460*d)));
a.runOnUiThread(()->{
cadre.setMinimumHeight(hauteur);
if(fr!=null)im.setImageBitmap(fr);
cadre.requestLayout();
});
}catch(Exception e){
}
}).start();
return cadre;
}
}
