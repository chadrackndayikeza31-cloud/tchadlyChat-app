package com.chadrack.tchadlychat;import android.app.Activity;import android.graphics.Color;import android.net.Uri;import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.io.File;
public class VideoPlayerActivity extends Activity{
private ExoPlayer player;
private String url;
private File local;
private TextView statut;
private TextView btnTelecharger;
private final Handler h=new Handler(Looper.getMainLooper());
@Override
protected void onCreate(Bundle savedInstanceState){
super.onCreate(savedInstanceState);
url=getIntent().getStringExtra("videoUrl");
if(url==null||url.isEmpty()){finish();return;}
FrameLayout root=new FrameLayout(this);
root.setBackgroundColor(Color.BLACK);
PlayerView pv=new PlayerView(this);
root.addView(pv,new FrameLayout.LayoutParams(-1,-1));
statut=new TextView(this);
statut.setTextColor(Color.WHITE);
statut.setGravity(Gravity.CENTER);
FrameLayout.LayoutParams sp=new FrameLayout.LayoutParams(-1,-2);
sp.gravity=Gravity.TOP;
sp.topMargin=dp(20);
root.addView(statut,sp);
btnTelecharger=new TextView(this);
btnTelecharger.setTextColor(Color.WHITE);
btnTelecharger.setBackgroundColor(Color.argb(160,0,0,0));
btnTelecharger.setPadding(dp(14),dp(8),dp(14),dp(8));
FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-2,-2);
bp.gravity=Gravity.TOP|Gravity.END;
bp.setMargins(0,dp(40),dp(12),0);
root.addView(btnTelecharger,bp);
setContentView(root);
local=VideoCache.fichier(this,url);
player=new ExoPlayer.Builder(this).build();
pv.setPlayer(player);
player.addListener(new Player.Listener(){
@Override
public void onPlaybackStateChanged(int state){
if(state==Player.STATE_READY)statut.setText("");
}
});
if(VideoCache.existe(this,url)){
jouer(Uri.fromFile(local));
marquerTelechargee();
}else{
statut.setText("Chargement...");
jouer(Uri.parse(url));
btnTelecharger.setText("⬇ Télécharger");
btnTelecharger.setOnClickListener(v->telechargerMaintenant());
VideoCache.telecharger(this,url,new VideoCache.Callback(){
@Override
public void onFini(File f){
h.post(()->marquerTelechargee());
}
@Override
public void onErreur(String msg){
h.post(()->btnTelecharger.setText("⬇ Réessayer"));
}
});
}
}
private void jouer(Uri u){
player.setMediaItem(MediaItem.fromUri(u));
player.prepare();
player.setPlayWhenReady(true);
}
private void marquerTelechargee(){
btnTelecharger.setText("✓ Téléchargée");
btnTelecharger.setOnClickListener(null);
}
private void telechargerMaintenant(){
if(VideoCache.existe(this,url)){
marquerTelechargee();
return;
}
btnTelecharger.setText("Téléchargement...");
btnTelecharger.setOnClickListener(null);
VideoCache.telecharger(this,url,new VideoCache.Callback(){
@Override
public void onFini(File f){
h.post(()->marquerTelechargee());
}
@Override
public void onErreur(String msg){
    h.post(()->{
btnTelecharger.setText("⬇ Réessayer");
btnTelecharger.setOnClickListener(v->telechargerMaintenant());
});
}
});
}
private int dp(int v){
return(int)(v*getResources().getDisplayMetrics().density+0.5f);
}
@Override
protected void onDestroy(){
if(player!=null){
player.release();
player=null;
}
super.onDestroy();
}
}
