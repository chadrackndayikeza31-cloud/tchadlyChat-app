package com.chadrack.tchadlychat;import android.content.ContentValues;import android.content.Context;import android.net.Uri;import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
public class ExportVideo{
public static String exporter(Context c,String url,String nom){return exporter(c,url,nom,"video/mp4");}
public static String exporter(Context c,String url,String nom,String mime){
File src=VideoCache.fichier(c,url);
if(!src.exists())return null;
try{
if(Build.VERSION.SDK_INT>=29){
    ContentValues v=new ContentValues();
v.put(MediaStore.Downloads.DISPLAY_NAME,nom);
v.put(MediaStore.Downloads.MIME_TYPE,mime);
v.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/TchadlyChat");
Uri u=c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
if(u==null)return null;
try(InputStream in=new FileInputStream(src);OutputStream out=c.getContentResolver().openOutputStream(u)){
    copier(in,out);
}
return "Téléchargements/TchadlyChat/"+nom;
}
File dir=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"TchadlyChat");
if(!dir.exists())dir.mkdirs();
File dst=new File(dir,nom);
try(InputStream in=new FileInputStream(src);OutputStream out=new FileOutputStream(dst)){
copier(in,out);
}
return dst.getAbsolutePath();
}catch(Exception e){
return null;
}
}
private static void copier(InputStream in,OutputStream out)throws Exception{
byte[] b=new byte[65536];
int n;
while((n=in.read(b))!=-1)out.write(b,0,n);
}
}
