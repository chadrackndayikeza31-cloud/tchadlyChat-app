package com.chadrack.tchadlychat;

import android.Manifest;
import android.app.AlertDialog;
import android.os.Handler;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.HashSet;
import java.util.Set;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends Activity {
    private static final int REQUEST_NOTIFICATION_PERMISSION=1001;
    private LinearLayout usersContainer;
    private EditText etRecherche;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String currentUid;
    private List<UserInfo> utilisateurs=new ArrayList<>();
    private Button btnDeconnecter;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        usersContainer=findViewById(R.id.usersContainer);
        etRecherche=findViewById(R.id.etRecherche);
        firebaseAuth=FirebaseAuth.getInstance();
        firestore=FirebaseFirestore.getInstance();

        if(usersContainer==null||etRecherche==null){
            Toast.makeText(this,"Erreur: éléments de HomeActivity manquants",Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        FirebaseUser user=firebaseAuth.getCurrentUser();

        if(user==null){
            allerLogin();
            return;
        }

        currentUid=user.getUid();
        creerBoutonDeconnexion();
        demanderPermissionNotification();
        enregistrerFcmToken();
        chargerUtilisateurs();

        etRecherche.addTextChangedListener(new android.text.TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){
                afficherUtilisateurs(s==null?"":s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s){}
        });
    }
    @Override
protected void onResume(){
super.onResume();
if(firebaseAuth==null){
firebaseAuth=FirebaseAuth.getInstance();
}
FirebaseUser user=firebaseAuth.getCurrentUser();
if(user==null){
allerLogin();
return;
}
currentUid=user.getUid();
Presence.demarrer(currentUid);
enregistrerFcmToken();
chargerUtilisateurs();
rafraichissement.removeCallbacks(rafraichirRunnable);
rafraichissement.postDelayed(rafraichirRunnable,30000L);
ecouterAppelsEntrants();
}
@Override
protected void onPause(){
super.onPause();
rafraichissement.removeCallbacks(rafraichirRunnable);
if(currentUid!=null)Presence.arreter(currentUid);
if(appelsListener!=null){
appelsListener.remove();
appelsListener=null;
}
}
   /* @Override
protected void onResume(){
super.onResume();
if(firebaseAuth==null){
firebaseAuth=FirebaseAuth.getInstance();
}
FirebaseUser user=firebaseAuth.getCurrentUser();
if(user==null){
allerLogin();
return;
}
currentUid=user.getUid();
Presence.demarrer(currentUid);
enregistrerFcmToken();
chargerUtilisateurs();
rafraichissement.removeCallbacks(rafraichirRunnable);
rafraichissement.postDelayed(rafraichirRunnable,30000L);
}
@Override
protected void onPause(){
super.onPause();
rafraichissement.removeCallbacks(rafraichirRunnable);
if(currentUid!=null)Presence.arreter(currentUid);
}*/

   /* @Override
    protected void onResume(){
        super.onResume();
        if(firebaseAuth==null){
            firebaseAuth=FirebaseAuth.getInstance();
        }
        FirebaseUser user=firebaseAuth.getCurrentUser();
        if(user==null){
            allerLogin();
            return;
        }
        currentUid=user.getUid();
        enregistrerFcmToken();
        chargerUtilisateurs();
    }*/

    private void creerBoutonDeconnexion(){
        if(btnDeconnecter!=null)return;

        FrameLayout root=findViewById(android.R.id.content);
        if(root==null)return;

        btnDeconnecter=new Button(this);
        btnDeconnecter.setText("Déconnecter");
        btnDeconnecter.setTextColor(Color.WHITE);
        btnDeconnecter.setTextSize(14);
        btnDeconnecter.setAllCaps(false);
        btnDeconnecter.setGravity(Gravity.CENTER);

        GradientDrawable background=new GradientDrawable();
        background.setColor(Color.rgb(190,45,55));
        background.setCornerRadius(dp(20));
        btnDeconnecter.setBackground(background);

        FrameLayout.LayoutParams params=new FrameLayout.LayoutParams(
                dp(135),
                dp(48)
        );
        params.gravity=Gravity.TOP|Gravity.END;
        params.setMargins(0,dp(8),dp(12),0);
        btnDeconnecter.setLayoutParams(params);

        btnDeconnecter.setOnClickListener(v->deconnecterUtilisateur());
        root.addView(btnDeconnecter);
    }
    private ListenerRegistration appelsListener;
private final Set<String> appelsAffiches=new HashSet<>();
private final Handler appelHandler=new Handler(android.os.Looper.getMainLooper());
    private final android.os.Handler rafraichissement=new android.os.Handler(android.os.Looper.getMainLooper());
private final Runnable rafraichirRunnable=new Runnable(){
@Override
public void run(){
chargerUtilisateurs();
rafraichissement.postDelayed(this,30000L);
}
};


    private void deconnecterUtilisateur(){
        if(firebaseAuth==null){
            firebaseAuth=FirebaseAuth.getInstance();
        }

        if(firestore!=null&&currentUid!=null){
            Map<String,Object> data=new HashMap<>();
            data.put("online",false);
            firestore.collection("users")
                    .document(currentUid)
                    .set(data,SetOptions.merge())
                    .addOnCompleteListener(task->{
                        firebaseAuth.signOut();
                        allerLogin();
                    });
        }else{
            firebaseAuth.signOut();
            allerLogin();
        }
    }

    private void allerLogin(){
       // Intent intent=new Intent(HomeActivity.this,LoginActivity.class);
       Intent intent=new Intent(HomeActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void demanderPermissionNotification(){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.TIRAMISU){
            if(ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
            )!=PackageManager.PERMISSION_GRANTED){
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION
                );
            }
        }
    }

    private void enregistrerFcmToken(){
        if(firebaseAuth==null||firebaseAuth.getCurrentUser()==null||firestore==null)return;

        String uid=firebaseAuth.getCurrentUser().getUid();

        FirebaseMessaging.getInstance()
                .getToken()
                .addOnSuccessListener(token->{
                    if(token==null||token.trim().isEmpty())return;

                    Map<String,Object> data=new HashMap<>();
                    data.put("fcmToken",token);

                    firestore.collection("users")
                            .document(uid)
                            .set(data,SetOptions.merge());
                })
                .addOnFailureListener(e->{});
    }

    private void chargerUtilisateurs(){
        if(firestore==null||currentUid==null)return;

        firestore.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot->{
                    utilisateurs.clear();

                    for(QueryDocumentSnapshot document:querySnapshot){
                        String uid=document.getId();

                        if(uid==null||uid.equals(currentUid))continue;

                        String pseudo=document.getString("pseudo");
                        String photoBase64=document.getString("photoBase64");

                       /* Object onlineObject=document.get("online");
                        boolean online=onlineObject instanceof Boolean&&(Boolean)onlineObject;*/
Object onlineObject=document.get("online");
boolean onlineFlag=onlineObject instanceof Boolean&&(Boolean)onlineObject;
Object lastSeenObject=document.get("lastSeen");
long lastSeen=lastSeenObject instanceof Number?((Number)lastSeenObject).longValue():0L;
boolean online=Presence.estEnLigne(onlineFlag,lastSeen);
                        long createdAt=0;
                        Object createdAtObject=document.get("createdAt");

                        if(createdAtObject instanceof Long){
                            createdAt=(Long)createdAtObject;
                        }else if(createdAtObject instanceof Number){
                            createdAt=((Number)createdAtObject).longValue();
                        }else if(createdAtObject instanceof Timestamp){
                            createdAt=((Timestamp)createdAtObject).toDate().getTime();
                        }

                        utilisateurs.add(new UserInfo(
                                uid,
                                pseudo,
                                photoBase64,
                                online,
                                createdAt
                        ));
                    }

                    chargerDerniersMessages();
                })
                .addOnFailureListener(e->Toast.makeText(
                        HomeActivity.this,
                        "Erreur chargement: "+e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void chargerDerniersMessages(){
        if(firestore==null||currentUid==null)return;

        firestore.collection("private_messages")
                .whereEqualTo("sender",currentUid)
                .get()
                .addOnSuccessListener(sentMessages->{
                    for(QueryDocumentSnapshot document:sentMessages){
                        enregistrerDernierMessage(document);
                    }

                    firestore.collection("private_messages")
                            .whereEqualTo("receiver",currentUid)
                            .get()
                            .addOnSuccessListener(receivedMessages->{
                                for(QueryDocumentSnapshot document:receivedMessages){
                                    enregistrerDernierMessage(document);
                                }

                                trierUtilisateurs();

                                String recherche="";
                                if(etRecherche!=null&&etRecherche.getText()!=null){
                                    recherche=etRecherche.getText().toString();
                                }

                                afficherUtilisateurs(recherche);
                            })
                            .addOnFailureListener(e->{
                                trierUtilisateurs();
                                afficherUtilisateurs("");
                            });
                })
                .addOnFailureListener(e->{
                    trierUtilisateurs();
                    afficherUtilisateurs("");
                });
    }
    private void ecouterAppelsEntrants(){
if(firestore==null||currentUid==null)return;
if(appelsListener!=null)appelsListener.remove();
appelsListener=firestore.collection("calls")
.whereEqualTo("callee",currentUid)
.whereEqualTo("status","ringing")
.addSnapshotListener((snap,e)->{
if(e!=null||snap==null)return;
for(DocumentChange dc:snap.getDocumentChanges()){
if(dc.getType()!=DocumentChange.Type.ADDED)continue;
DocumentSnapshot d=dc.getDocument();
if(appelsAffiches.contains(d.getId()))continue;
com.google.firebase.Timestamp created=d.getTimestamp("createdAt");
long creeLe=created==null?0L:created.toDate().getTime();
if(System.currentTimeMillis()-creeLe>120000L)continue;
appelsAffiches.add(d.getId());
afficherAppelEntrant(d);
}
});
}
/*private void afficherAppelEntrant(DocumentSnapshot appel){
String callId=appel.getId();
String callerUid=appel.getString("caller");
String type=appel.getString("type");
if(callerUid==null)return;
firestore.collection("users").document(callerUid).get().addOnSuccessListener(u->{
if(isFinishing())return;
String nom=u.getString("pseudo");
if(nom==null||nom.trim().isEmpty())nom="Utilisateur";
String libelle="video".equals(type)?"Appel vidéo":"Appel audio";
AlertDialog dlg=new AlertDialog.Builder(this)
.setTitle(libelle)
.setMessage(nom+" vous appelle...")
.setCancelable(false)
.setPositiveButton("Accepter",(x,w)->repondreAppel(callId,true))
.setNegativeButton("Refuser",(x,w)->repondreAppel(callId,false))
.create();
dlg.show();
appelHandler.postDelayed(()->{if(dlg.isShowing())dlg.dismiss();},45000L);
});
}*/
private void afficherAppelEntrant(DocumentSnapshot appel){
String callId=appel.getId();
String callerUid=appel.getString("caller");
String type=appel.getString("type");
String channel=appel.getString("channel");
if(callerUid==null||channel==null)return;
firestore.collection("users").document(callerUid).get().addOnSuccessListener(u->{
if(isFinishing())return;
String pseudo=u.getString("pseudo");
final String nomAppelant=(pseudo==null||pseudo.trim().isEmpty())?"Utilisateur":pseudo;
String libelle="video".equals(type)?"Appel vidéo":"Appel audio";
AlertDialog dlg=new AlertDialog.Builder(this)
.setTitle(libelle)
.setMessage(nomAppelant+" vous appelle...")
.setCancelable(false)
.setPositiveButton("Accepter",(x,w)->repondreAppel(callId,true,channel,type,nomAppelant))
.setNegativeButton("Refuser",(x,w)->repondreAppel(callId,false,channel,type,nomAppelant))
.create();
dlg.show();
appelHandler.postDelayed(()->{if(dlg.isShowing())dlg.dismiss();},45000L);
});
}
private void repondreAppel(String callId,boolean accepte,String channel,String type,String nomAppelant){
if(firestore==null)return;
Map<String,Object> data=new HashMap<>();
data.put("status",accepte?"accepted":"rejected");
firestore.collection("calls").document(callId).update(data)
.addOnSuccessListener(v->{
if(accepte){
Intent i=new Intent(HomeActivity.this,CallActivity.class);
i.putExtra("callId",callId);
i.putExtra("channel",channel);
i.putExtra("type",type);
i.putExtra("peerName",nomAppelant);
i.putExtra("role","callee");
startActivity(i);
}
})
.addOnFailureListener(e->Toast.makeText(this,"Erreur de réponse à l'appel.",Toast.LENGTH_SHORT).show());
}
private void repondreAppel(String callId,boolean accepte){
if(firestore==null)return;
Map<String,Object> data=new HashMap<>();
data.put("status",accepte?"accepted":"rejected");
firestore.collection("calls").document(callId).update(data)
.addOnSuccessListener(v->{
if(accepte)Toast.makeText(this,"Appel accepté.",Toast.LENGTH_SHORT).show();
})
.addOnFailureListener(e->Toast.makeText(this,"Erreur de réponse à l'appel.",Toast.LENGTH_SHORT).show());
}

    private void enregistrerDernierMessage(QueryDocumentSnapshot document){
        if(document==null)return;

        String sender=document.getString("sender");
        String receiver=document.getString("receiver");

        if(sender==null||receiver==null)return;

        long timestamp=0;
        Object timestampObject=document.get("timestamp");

        if(timestampObject instanceof Timestamp){
            timestamp=((Timestamp)timestampObject).toDate().getTime();
        }else if(timestampObject instanceof Number){
            timestamp=((Number)timestampObject).longValue();
        }

        if(timestamp<=0)return;

        String autreUid=currentUid.equals(sender)?receiver:sender;

        for(UserInfo utilisateur:utilisateurs){
            if(utilisateur!=null&&utilisateur.uid!=null&&utilisateur.uid.equals(autreUid)){
                if(timestamp>utilisateur.latestMessage){
                    utilisateur.latestMessage=timestamp;
                }

                if(currentUid.equals(receiver)){
                    Object seenObject=document.get("seen");
                    boolean seen=seenObject instanceof Boolean&&(Boolean)seenObject;

                    if(!seen){
                        utilisateur.unread=true;
                    }
                }
                break;
            }
        }
    }

    private void trierUtilisateurs(){
        Collections.sort(
                utilisateurs,
                new Comparator<UserInfo>(){
                    @Override
                    public int compare(UserInfo a,UserInfo b){
                        boolean aChat=a.latestMessage>0;
                        boolean bChat=b.latestMessage>0;

                        if(aChat&&!bChat)return -1;
                        if(!aChat&&bChat)return 1;

                        if(aChat&&bChat){
                            return Long.compare(
                                    b.latestMessage,
                                    a.latestMessage
                            );
                        }

                        return Long.compare(
                                b.createdAt,
                                a.createdAt
                        );
                    }
                }
        );
    }

    private void afficherUtilisateurs(String recherche){
        if(usersContainer==null)return;

        usersContainer.removeAllViews();

        String texteRecherche=recherche==null
                ?""
                :recherche.trim().toLowerCase(Locale.getDefault());

        for(UserInfo utilisateur:utilisateurs){
            if(utilisateur==null)continue;

            String pseudo=utilisateur.pseudo;

            if(pseudo==null||pseudo.isEmpty()){
                pseudo="Utilisateur";
            }

            if(!texteRecherche.isEmpty()&&!pseudo
                    .toLowerCase(Locale.getDefault())
                    .contains(texteRecherche)){
                continue;
            }

            ajouterUtilisateur(utilisateur);
        }
    }
        private void ajouterUtilisateur(UserInfo utilisateur){
        if(utilisateur==null)return;

        LinearLayout ligne=new LinearLayout(this);
        ligne.setOrientation(LinearLayout.HORIZONTAL);
        ligne.setGravity(Gravity.CENTER_VERTICAL);
        ligne.setPadding(dp(4),dp(10),dp(4),dp(10));

        FrameLayout photoContainer=new FrameLayout(this);

        LinearLayout.LayoutParams photoContainerParams=
                new LinearLayout.LayoutParams(dp(64),dp(64));

        photoContainerParams.setMargins(0,0,dp(16),0);
        photoContainer.setLayoutParams(photoContainerParams);

        ImageView image=new ImageView(this);

        FrameLayout.LayoutParams imageParams=
                new FrameLayout.LayoutParams(dp(64),dp(64));

        image.setLayoutParams(imageParams);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);

        GradientDrawable cercle=new GradientDrawable();
        cercle.setShape(GradientDrawable.OVAL);
        cercle.setColor(Color.rgb(55,76,100));

        image.setBackground(cercle);
        image.setClipToOutline(true);

        image.setOutlineProvider(new ViewOutlineProvider(){
            @Override
            public void getOutline(View view,Outline outline){
                outline.setOval(
                        0,
                        0,
                        view.getWidth(),
                        view.getHeight()
                );
            }
        });

        if(utilisateur.photoBase64!=null&&!utilisateur.photoBase64.isEmpty()){
            try{
                byte[] imageBytes=Base64.decode(
                        utilisateur.photoBase64,
                        Base64.DEFAULT
                );

                Bitmap bitmap=BitmapFactory.decodeByteArray(
                        imageBytes,
                        0,
                        imageBytes.length
                );

                if(bitmap!=null){
                    image.setImageBitmap(bitmap);
                }else{
                    image.setImageResource(
                            android.R.drawable.ic_menu_camera
                    );
                }
            }catch(Exception e){
                image.setImageResource(
                        android.R.drawable.ic_menu_camera
                );
            }
        }else{
            image.setImageResource(
                    android.R.drawable.ic_menu_camera
            );
        }

        photoContainer.addView(image);

        if(utilisateur.unread){
            TextView badge=new TextView(this);

            FrameLayout.LayoutParams badgeParams=
                    new FrameLayout.LayoutParams(
                            dp(14),
                            dp(14)
                    );

            badgeParams.gravity=Gravity.TOP|Gravity.END;
            badge.setLayoutParams(badgeParams);

            GradientDrawable badgeBackground=
                    new GradientDrawable();

            badgeBackground.setShape(GradientDrawable.OVAL);
            badgeBackground.setColor(Color.RED);

            badge.setBackground(badgeBackground);
            photoContainer.addView(badge);
        }

        LinearLayout partieTexte=new LinearLayout(this);
        partieTexte.setOrientation(LinearLayout.VERTICAL);
        partieTexte.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams texteParams=
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                );

        partieTexte.setLayoutParams(texteParams);

        TextView nom=new TextView(this);

        String pseudo=utilisateur.pseudo;

        if(pseudo==null||pseudo.isEmpty()){
            pseudo="Utilisateur";
        }

        nom.setText(pseudo);
        nom.setTextColor(Color.WHITE);
        nom.setTextSize(19);
        nom.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        partieTexte.addView(nom);

        TextView statut=new TextView(this);

        if(utilisateur.online){
            statut.setText("●  En ligne");
            statut.setTextColor(
                    Color.rgb(40,220,125)
            );
        }else{
            statut.setText("●  Hors ligne");
            statut.setTextColor(
                    Color.rgb(155,170,190)
            );
        }

        statut.setTextSize(15);

        LinearLayout.LayoutParams statutParams=
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        statutParams.topMargin=dp(3);
        statut.setLayoutParams(statutParams);

        partieTexte.addView(statut);

        TextView fleche=new TextView(this);
        fleche.setText("›");
        fleche.setTextColor(
                Color.rgb(190,205,225)
        );
        fleche.setTextSize(38);
        fleche.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams flecheParams=
                new LinearLayout.LayoutParams(
                        dp(40),
                        dp(70)
                );

        fleche.setLayoutParams(flecheParams);

        ligne.addView(photoContainer);
        ligne.addView(partieTexte);
        ligne.addView(fleche);

        final String receiverUid=utilisateur.uid;
        final String receiverPseudo=utilisateur.pseudo;

        ligne.setOnClickListener(v->{
            Intent intent=new Intent(
                    HomeActivity.this,
                    //VideoPlayerActivity.class
                    ChatActivity.class
            );

            intent.putExtra(
                    "receiverUid",
                    receiverUid
            );

            intent.putExtra(
                    "receiverPseudo",
                    receiverPseudo
            );

            startActivity(intent);
        });

        usersContainer.addView(ligne);

        View separation=new View(this);

        separation.setBackgroundColor(
                Color.rgb(35,55,78)
        );

        LinearLayout.LayoutParams separationParams=
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                );

        separationParams.leftMargin=dp(80);
        separation.setLayoutParams(separationParams);

        usersContainer.addView(separation);
    }

    private int dp(int value){
        return (int)(
                value*
                getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private static class UserInfo{
        String uid;
        String pseudo;
        String photoBase64;
        boolean online;
        boolean unread;
        long createdAt;
        long latestMessage;

        UserInfo(
                String uid,
                String pseudo,
                String photoBase64,
                boolean online,
                long createdAt
        ){
            this.uid=uid;
            this.pseudo=pseudo;
            this.photoBase64=photoBase64;
            this.online=online;
            this.unread=false;
            this.createdAt=createdAt;
            this.latestMessage=0;
        }
    }
}