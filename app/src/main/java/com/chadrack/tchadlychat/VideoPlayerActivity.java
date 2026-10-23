package com.chadrack.tchadlychat;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class VideoPlayerActivity extends Activity {

private ExoPlayer player;  
private PlayerView playerView;  

@Override  
protected void onCreate(Bundle savedInstanceState) {  
    super.onCreate(savedInstanceState);  

    getWindow().setFlags(  
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,  
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN  
    );  

    String videoUrl = getIntent().getStringExtra("videoUrl");  

    if (videoUrl == null || videoUrl.trim().isEmpty()) {  
        afficherErreur("URL de la vidéo introuvable");  
        return;  
    }  

    playerView = new PlayerView(this);  

    playerView.setBackgroundColor(Color.BLACK);  

    playerView.setLayoutParams(  
            new android.view.ViewGroup.LayoutParams(  
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,  
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT  
            )  
    );  

    playerView.setKeepScreenOn(true);  

    playerView.setUseController(true);  

    setContentView(playerView);  

    player = new ExoPlayer.Builder(this).build();  

    playerView.setPlayer(player);  

    MediaItem mediaItem = MediaItem.fromUri(videoUrl);  

    player.setMediaItem(mediaItem);  

    player.addListener(new Player.Listener() {  

        @Override  
        public void onPlaybackStateChanged(int playbackState) {  

            if (playbackState == Player.STATE_READY) {  
                playerView.setVisibility(View.VISIBLE);  
            }  

            if (playbackState == Player.STATE_ENDED) {  
                playerView.showController();  
            }  
        }  

        @Override  
        public void onPlayerError(PlaybackException error) {  
            afficherErreur("Impossible de lire la vidéo");  
        }  
    });  

    player.prepare();  

    player.play();  
}  

private void afficherErreur(String message) {  

    TextView errorText = new TextView(this);  

    errorText.setText(message);  

    errorText.setTextColor(Color.WHITE);  

    errorText.setTextSize(16);  

    errorText.setGravity(android.view.Gravity.CENTER);  

    errorText.setPadding(40, 40, 40, 40);  

    errorText.setBackgroundColor(Color.BLACK);  

    setContentView(errorText);  
}  

@Override  
protected void onStop() {  

    super.onStop();  

    if (player != null) {  
        player.pause();  
    }  
}  

@Override  
protected void onDestroy() {  

    if (player != null) {  
        player.release();  
        player = null;  
    }  

    super.onDestroy();  
}

}