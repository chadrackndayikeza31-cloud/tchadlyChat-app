package com.chadrack.tchadlychat;import android.content.Context;import java.io.File;import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
public class VideoCache{
public interface Callback{
void onFini(File f);
void onErreur(String msg);
}
public static File fichier(Context c,String url){
File dir=new File(c.getFilesDir(),"videos");
if(!dir.exists())dir.mkdirs();
return new File(dir,hash(url)+".mp4");
}
public static boolean existe(Context c,String url){
File f=fichier(c,url);
return f.exists()&&f.length()>0;
}
public static void telecharger(Context c,String url,Callback cb){
new Thread(()->{
File dst=fichier(c,url);
File tmp=new File(dst.getParentFile(),dst.getName()+".part");
HttpURLConnection con=null;
try{
con=(HttpURLConnection)new URL(url).openConnection();
con.setConnectTimeout(30000);
con.setReadTimeout(60000);
if(con.getResponseCode()/100!=2)throw new IOException("HTTP "+con.getResponseCode());
try(InputStream in=con.getInputStream();OutputStream out=new FileOutputStream(tmp)){
    byte[] b=new byte[65536];
int n;
while((n=in.read(b))!=-1)out.write(b,0,n);
}
if(dst.exists())dst.delete();
if(!tmp.renameTo(dst))throw new IOException("renommage impossible");
cb.onFini(dst);
}catch(Exception e){
tmp.delete();
cb.onErreur(e.getMessage()==null?"Erreur":e.getMessage());
}finally{
    if(con!=null)con.disconnect();
}
}).start();
}
public static void copierLocal(Context c,String url,File src){
try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(fichier(c,url))){
byte[] b=new byte[65536];
int n;
while((n=in.read(b))!=-1)out.write(b,0,n);
}catch(Exception ignored){}
}
private static String hash(String s){
try{
byte[] d=MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
StringBuilder sb=new StringBuilder();
for(byte x:d)sb.append(String.format("%02x",x));
return sb.toString();
}catch(Exception e){
return String.valueOf(s.hashCode());
}
}
}
