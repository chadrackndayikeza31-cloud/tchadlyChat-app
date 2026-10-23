package com.chadrack.tchadlychat;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;
public class Presence{
public static final long DELAI_EN_LIGNE=120000L;
private static final long INTERVALLE=60000L;
private static final Handler HANDLER=new Handler(Looper.getMainLooper());
private static String uidActif;
private static final Runnable BATTEMENT=new Runnable(){
@Override
public void run(){
if(uidActif!=null)ecrire(uidActif,true);
HANDLER.postDelayed(this,INTERVALLE);
}
};
public static void demarrer(String uid){
if(uid==null)return;
uidActif=uid;
ecrire(uid,true);
HANDLER.removeCallbacks(BATTEMENT);
HANDLER.postDelayed(BATTEMENT,INTERVALLE);
}
public static void arreter(String uid){
HANDLER.removeCallbacks(BATTEMENT);
uidActif=null;
if(uid!=null)ecrire(uid,false);
}
public static boolean estEnLigne(boolean online,long lastSeen){
return online&&(System.currentTimeMillis()-lastSeen)<DELAI_EN_LIGNE;
}
private static void ecrire(String uid,boolean enLigne){
Map<String,Object> data=new HashMap<>();
data.put("online",enLigne);
data.put("lastSeen",System.currentTimeMillis());
FirebaseFirestore.getInstance().collection("users").document(uid).set(data,SetOptions.merge());
}
}