package com.chadrack.tchadlychat;import com.google.firebase.Timestamp;import com.google.firebase.firestore.DocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
public class Statut{private static final long SEUIL_EN_LIGNE=2L*60L*1000L;
public static boolean enLigne(DocumentSnapshot d){
long ms=lastSeen(d);
return ms>0L&&System.currentTimeMillis()-ms<=SEUIL_EN_LIGNE;
}
public static String texte(DocumentSnapshot d){long ms=lastSeen(d);
if(ms<=0L)return "Hors ligne";
if(System.currentTimeMillis()-ms<=SEUIL_EN_LIGNE)return "En ligne";
Date date=new Date(ms);
Calendar maintenant=Calendar.getInstance();
Calendar cal=Calendar.getInstance();cal.setTime(date);
String heure=new SimpleDateFormat("HH:mm",Locale.FRANCE).format(date);
if(memeJour(cal,maintenant))return "Vu à "+heure;
Calendar hier=Calendar.getInstance();
hier.add(Calendar.DAY_OF_YEAR,-1);
if(memeJour(cal,hier))return "Vu hier à "+heure;return "Vu le "+new SimpleDateFormat("dd/MM",Locale.FRANCE).format(date)+" à "+heure;
}
private static boolean memeJour(Calendar a,Calendar b){
return a.get(Calendar.YEAR)==b.get(Calendar.YEAR)&&a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR);
}
private static long lastSeen(DocumentSnapshot d){Object o=d.get("lastSeen");
if(o instanceof Timestamp)return ((Timestamp)o).toDate().getTime();
if(o instanceof Number)return ((Number)o).longValue();
return 0L;
}
public static String texteDepuis(long ms){
if(ms<=0L)return "Hors ligne";
if(System.currentTimeMillis()-ms<=SEUIL_EN_LIGNE)return "En ligne";
Date date=new Date(ms);
Calendar maintenant=Calendar.getInstance();
Calendar cal=Calendar.getInstance();
cal.setTime(date);
String heure=new SimpleDateFormat("HH:mm",Locale.FRANCE).format(date);
if(memeJour(cal,maintenant))return "Vu à "+heure;
Calendar hier=Calendar.getInstance();
hier.add(Calendar.DAY_OF_YEAR,-1);
if(memeJour(cal,hier))return "Vu hier à "+heure;
return "Vu le "+new SimpleDateFormat("dd/MM",Locale.FRANCE).format(date)+" à "+heure;
}
public static String texteDepuis(long ms){
if(ms<=0L)return "Hors ligne";
if(System.currentTimeMillis()-ms<=SEUIL_EN_LIGNE)return "En ligne";
Date date=new Date(ms);
Calendar maintenant=Calendar.getInstance();
Calendar cal=Calendar.getInstance();
cal.setTime(date);
String heure=new SimpleDateFormat("HH:mm",Locale.FRANCE).format(date);
if(memeJour(cal,maintenant))return "Vu à "+heure;
Calendar hier=Calendar.getInstance();
hier.add(Calendar.DAY_OF_YEAR,-1);
if(memeJour(cal,hier))return "Vu hier à "+heure;
return "Vu le "+new SimpleDateFormat("dd/MM",Locale.FRANCE).format(date)+" à "+heure;
}
}
