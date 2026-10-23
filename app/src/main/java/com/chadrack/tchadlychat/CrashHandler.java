package com.chadrack.tchadlychat;import android.app.Activity;import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.PrintWriter;import java.io.StringWriter;
public class CrashHandler implements Thread.UncaughtExceptionHandler{
private static final String PREF="crash_log",KEY="trace";
private final Context ctx;
private final Thread.UncaughtExceptionHandler precedent;
private CrashHandler(Context c){
ctx=c.getApplicationContext();
precedent=Thread.getDefaultUncaughtExceptionHandler();
}
public static void installer(Context c){
Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(c));
}
@Override
public void uncaughtException(Thread t,Throwable e){
try{
StringWriter sw=new StringWriter();
e.printStackTrace(new PrintWriter(sw));
ctx.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,sw.toString()).commit();
}catch(Exception ignored){}
if(precedent!=null)precedent.uncaughtException(t,e);
}
public static void afficherSiExiste(Activity a){
SharedPreferences p=a.getSharedPreferences(PREF,Context.MODE_PRIVATE);
final String trace=p.getString(KEY,null);
if(trace==null)return;
p.edit().remove(KEY).apply();
TextView tv=new TextView(a);
tv.setText(trace);
tv.setTextIsSelectable(true);
tv.setTextSize(11);
tv.setPadding(40,20,40,20);
ScrollView sv=new ScrollView(a);
sv.addView(tv);
new AlertDialog.Builder(a)
.setTitle("Erreur de l'application")
.setView(sv)
.setPositiveButton("Copier",(d,w)->{
ClipboardManager cm=(ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);
if(cm!=null)cm.setPrimaryClip(ClipData.newPlainText("erreur",trace));
Toast.makeText(a,"Copié",Toast.LENGTH_SHORT).show();
})
.setNegativeButton("Fermer",null)
.show();
}
}
