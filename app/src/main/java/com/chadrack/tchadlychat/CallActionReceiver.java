package com.chadrack.tchadlychat;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
public class CallActionReceiver extends BroadcastReceiver{
public static final String ACTION_ACCEPTER="com.chadrack.tchadlychat.CALL_ACCEPTER";
public static final String ACTION_REFUSER="com.chadrack.tchadlychat.CALL_REFUSER";
public static final String EXTRA_CALL_ID="callId";
public static final String EXTRA_NOTIF_ID="notifId";
@Override
public void onReceive(Context context,Intent intent){
if(intent==null)return;
String callId=intent.getStringExtra(EXTRA_CALL_ID);
int notifId=intent.getIntExtra(EXTRA_NOTIF_ID,-1);
if(callId==null||callId.isEmpty())return;
Map<String,Object> data=new HashMap<>();
data.put("status",ACTION_ACCEPTER.equals(intent.getAction())?"accepted":"rejected");
FirebaseFirestore.getInstance().collection("calls").document(callId).update(data);
if(notifId!=-1)NotificationManagerCompat.from(context).cancel(notifId);
}
}