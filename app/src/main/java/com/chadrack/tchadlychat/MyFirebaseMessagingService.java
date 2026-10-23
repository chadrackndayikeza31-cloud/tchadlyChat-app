
package com.chadrack.tchadlychat;import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.HashMap;
import java.util.Map;
public class MyFirebaseMessagingService extends FirebaseMessagingService{
public static final String KEY_TEXT_REPLY=QuickReplyReceiver.KEY_TEXT_REPLY;
public static final String KEY_SENDER_UID=QuickReplyReceiver.KEY_SENDER_UID;
public static final String KEY_RECEIVER_UID=QuickReplyReceiver.KEY_RECEIVER_UID;
private static final String CHANNEL_ID="tchadlychat_messages";
private static final String CHANNEL_NAME="Messages";
@Override
public void onNewToken(String token){
super.onNewToken(token);
enregistrerToken(token);
}
private void enregistrerToken(String token){
if(token==null||token.trim().isEmpty())return;
FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
if(user==null)return;
Map<String,Object> data=new HashMap<>();
data.put("fcmToken",token);
FirebaseFirestore.getInstance().collection("users").document(user.getUid()).set(data,SetOptions.merge());
}
@Override
public void onMessageReceived(RemoteMessage remoteMessage){
super.onMessageReceived(remoteMessage);
Map<String,String> data=remoteMessage.getData();
if(data==null||data.isEmpty())return;
if("call".equals(data.get("kind"))){
afficherNotificationAppel(data);
return;
}
String title=data.get("title");
String body=data.get("body");
String type=data.get("type");
String senderUid=data.get("senderUid");
String receiverUid=data.get("receiverUid");
String messageId=data.get("messageId");
if(title==null||title.trim().isEmpty())title="TchadlyChat";
if(body==null)body="";
if(type==null||type.trim().isEmpty())type="text";
createNotificationChannel();
boolean isTextMessage="text".equalsIgnoreCase(type);
int notificationId=(messageId!=null&&!messageId.trim().isEmpty())?(messageId.hashCode()&0x7fffffff):(int)(System.currentTimeMillis()&0x7fffffff);
Intent openIntent=new Intent(this,HomeActivity.class);
openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
PendingIntent openPendingIntent=PendingIntent.getActivity(this,notificationId,openIntent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
NotificationCompat.Builder builder=new NotificationCompat.Builder(this,CHANNEL_ID)
.setSmallIcon(android.R.drawable.ic_dialog_email)
.setContentTitle(title)
.setContentText(body)
.setStyle(new NotificationCompat.BigTextStyle().bigText(body))
.setPriority(NotificationCompat.PRIORITY_HIGH)
.setCategory(NotificationCompat.CATEGORY_MESSAGE)
.setAutoCancel(true)
.setContentIntent(openPendingIntent);
if(isTextMessage&&senderUid!=null&&!senderUid.trim().isEmpty()&&receiverUid!=null&&!receiverUid.trim().isEmpty()){
Intent replyIntent=new Intent(this,QuickReplyReceiver.class);
replyIntent.setAction(QuickReplyReceiver.ACTION_REPLY);
replyIntent.putExtra(QuickReplyReceiver.KEY_SENDER_UID,senderUid);
replyIntent.putExtra(QuickReplyReceiver.KEY_RECEIVER_UID,receiverUid);
if(messageId!=null)replyIntent.putExtra("messageId",messageId);
replyIntent.putExtra("senderName",title);
int replyFlags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=Build.VERSION_CODES.S?PendingIntent.FLAG_MUTABLE:PendingIntent.FLAG_IMMUTABLE);
PendingIntent replyPendingIntent=PendingIntent.getBroadcast(this,notificationId+1,replyIntent,replyFlags);
RemoteInput remoteInput=new RemoteInput.Builder(QuickReplyReceiver.KEY_TEXT_REPLY).setLabel("Andika inyishu...").build();
NotificationCompat.Action replyAction=new NotificationCompat.Action.Builder(android.R.drawable.ic_menu_send,"Répondre",replyPendingIntent).addRemoteInput(remoteInput).setAllowGeneratedReplies(true).build();
builder.addAction(replyAction);
}
NotificationManagerCompat.from(this).notify(notificationId,builder.build());
}
private void afficherNotificationAppel(Map<String,String> data){
String callId=data.get("callId");
if(callId==null||callId.isEmpty())return;
String nom=data.get("callerName");
if(nom==null||nom.trim().isEmpty())nom="Utilisateur";
String libelle="video".equals(data.get("callType"))?"Appel vidéo":"Appel audio";
if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
NotificationChannel canal=new NotificationChannel("tchadlychat_calls","Appels",NotificationManager.IMPORTANCE_HIGH);
canal.setDescription("Appels entrants TchadlyChat");
canal.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
if(manager!=null)manager.createNotificationChannel(canal);
}
int id=callId.hashCode()&0x7fffffff;
Intent ouvrir=new Intent(this,HomeActivity.class);
ouvrir.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
PendingIntent pOuvrir=PendingIntent.getActivity(this,id,ouvrir,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
Intent accepter=new Intent(this,CallActionReceiver.class);
accepter.setAction(CallActionReceiver.ACTION_ACCEPTER);
accepter.putExtra(CallActionReceiver.EXTRA_CALL_ID,callId);
accepter.putExtra(CallActionReceiver.EXTRA_NOTIF_ID,id);
PendingIntent pAccepter=PendingIntent.getBroadcast(this,id+1,accepter,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
Intent refuser=new Intent(this,CallActionReceiver.class);
refuser.setAction(CallActionReceiver.ACTION_REFUSER);
refuser.putExtra(CallActionReceiver.EXTRA_CALL_ID,callId);
refuser.putExtra(CallActionReceiver.EXTRA_NOTIF_ID,id);
PendingIntent pRefuser=PendingIntent.getBroadcast(this,id+2,refuser,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
NotificationCompat.Builder builder=new NotificationCompat.Builder(this,"tchadlychat_calls")
.setSmallIcon(android.R.drawable.ic_menu_call)
.setContentTitle(libelle)
.setContentText(nom+" vous appelle...")
.setPriority(NotificationCompat.PRIORITY_MAX)
.setCategory(NotificationCompat.CATEGORY_CALL)
.setOngoing(true)
.setAutoCancel(false)
.setTimeoutAfter(45000L)
.setFullScreenIntent(pOuvrir,true)
.setContentIntent(pOuvrir)
.addAction(android.R.drawable.ic_menu_call,"Accepter",pAccepter)
.addAction(android.R.drawable.ic_menu_close_clear_cancel,"Refuser",pRefuser);
NotificationManagerCompat.from(this).notify(id,builder.build());
}
private void createNotificationChannel(){
if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
if(manager==null)return;
NotificationChannel channel=new NotificationChannel(CHANNEL_ID,CHANNEL_NAME,NotificationManager.IMPORTANCE_HIGH);
channel.setDescription("Notifications de TchadlyChat");
manager.createNotificationChannel(channel);
}
}
}