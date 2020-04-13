package com.example.g_google_drive;
//http://cmnocsexperience.blogspot.com/2019/01/android-app-google-google-drive.html
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
//                .requestId() //指定你的Application請求ID
//                .requestIdToken() //指定的請求Token(Service用戶端將會被驗證的用戶端ID)
//                .requestProfile()  //指定你的Application請求用戶的配置文件訊息
//                .requestServerAuthCode()  //指令離線請求訪問(需要驗證的id)
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private final static int REQUEST_CODE_SIGN_IN = 1;
    private ImageView img;
    private Uri photoUrl;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        img = findViewById(R.id.img);
        requestSignIn();
    }
//DriveScopes.DRIVE_FILE 查看和管理您使用此應用程序打開或創建的Google雲端硬盤文件和文件夾。
//DriveScopes.DRIVE_APPDATA 在您的Google雲端硬盤中查看和管理自己的配置數據。
//GoogleSignInOptions.Builder(GoogleSignInOptions var1):選擇要登入的功能(登入選項);
//GoogleSignInOptions.requestId() //指定你的Application請求ID
//GoogleSignInOptions.requestIdToken() //指定的請求Token(Service用戶端將會被驗證的用戶端ID)
//GoogleSignInOptions.requestProfile()  //指定你的Application請求用戶的配置文件訊息
//GoogleSignInOptionsrequestServerAuthCode()  //指令離線請求訪問(需要驗證的id)
//GoogleSignInOptions.requestScopes(Scope var1, Scope... var2):要存取的Googlez範圍(1.什麼範圍~2.什麼範圍)

//GoogleSignInClient.getClient(Activity var0,GoogleSignInOptions var1)://取得Google登入的Client (1.Context,2.Siging選項設定)
//GoogleSignIn.getClient.getSignInIntent()://將這個設定好的取得GoogleIntent (回傳值Intente)


    //1.登入
    private void requestSignIn() {
        //1.取得GoogleSignInOptions物件實體,其中 requestScopes 設定服務類型，requestIdToken 就填入剛才申請的 Client ID。
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)  //選擇要登入的功能()
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE),
                                new Scope(DriveScopes.DRIVE_APPDATA))//指定你的Application請求用戶的配置文件訊息(DRIVE_FILE == 查看管理你的或創建Google雲頓硬碟與文件 , )
                        .requestIdToken("490722671996-tkt938euvqbppv44p6ov9ul35k0rhe9i.apps.googleusercontent.com")  //設定指定的Token(這邊是剛Google給我的ClientId)
                        .build(); //建立


        //2.取得GoogleSignInClient進行登入,Intent呼叫出登入Google帳號
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, signInOptions);//取得Google登入的Client (1.Context,2.Siging選項設定)
        startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN); ////將這個設定好的取得GoogleIntent (回傳值Intente)
    }

    //3.當使用者同意登入Gmail,將資料給GoogleDriver後
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                handleSignInResult(data);
                break;
        }
    }

    //4.取得使用者登入的intnent的googleAccount物件,當取得時會跳出允許授權取得雲端的Dialog,當使用者成功登入GoogleDriver後的Handle方法
//    Task<GoogleSignInAccount> getSignedInAccountFromIntent(@Nullable Intent var0)
    private void handleSignInResult(Intent data) {
        //取得使用者登入的intnent的googleAccount物件
        GoogleSignIn.getSignedInAccountFromIntent(data)  //取得使用者登入的intnent的googleAccount物件,當取得時會跳出允許授權取得雲端的Dialog
                //當使用者都允許權限時,取得使用者Google的資訊
                .addOnSuccessListener(googleAccount -> {
                    String email = googleAccount.getEmail();//取得Google-email
                    String displayName = googleAccount.getDisplayName();//取得Google-名稱
                    String familyName = googleAccount.getFamilyName(); //取得Google-姓氏
                    String givenName = googleAccount.getGivenName(); //取得Google-指定的名稱
                    Set<Scope> scopeSet = googleAccount.getGrantedScopes(); //取得Google允許的權限範圍
                    String id = googleAccount.getId(); //取得Google-ID
                    String token = googleAccount.getIdToken();  //取得Google-Token
                    photoUrl = googleAccount.getPhotoUrl(); //取得Google-大頭貼照片網址
                    img.setImageURI(photoUrl);
                    String serverAuthCode = googleAccount.getServerAuthCode();
                    Log.v("hank", "addOnSuccessListener =>  googleAccount.getEmail():" + email + "\n" + "/displayName:" + displayName + "\n" + "/familyName:" + familyName + "\n" + "/givenName:" + givenName + "\n" + "/id:" + id + "\n" + "/token:" + token + "\n" + "/serverAuthCode:" + serverAuthCode + photoUrl);



                    //6.需要 GoogleAccountCredential有實作HttpTransport
//                  GoogleAccountCredential.usingOAuth2(Context context, Collection<String> scopes)://GoogleAccountCredential物件實體化(1.Context,2.Collection<String> scopes)
                    GoogleAccountCredential googleAccountCredential =
                            GoogleAccountCredential.usingOAuth2(
                                    this,
                                    Collections.singleton(DriveScopes.DRIVE_FILE)
                            );
                    googleAccountCredential.setSelectedAccount(googleAccount.getAccount());


                    //5.創建GoogleDrive
                    //https://googleapis.dev/java/google-api-client/latest/com/google/api/client/googleapis/services/json/AbstractGoogleJsonClient.Builder.html
                    //Drive.Builder( //Drive.建立物件實體化
//                    com.google.api.client.http.HttpTransport transport, //1.新兼容運輸
//                    com.google.api.client.json.JsonFactory jsonFactory,//Json工廠
//                    com.google.api.client.http.HttpRequestInitializer httpRequestInitializer)//3.Http請求初始化物件

                    Drive googleDriveService = new Drive.Builder(//Drive.建立物件實體化
                            AndroidHttp.newCompatibleTransport(), //1.新兼容運輸
                            new GsonFactory(), //2.Json工廠
                            googleAccountCredential) //3.Http請求初始化物件
                            .setApplicationName("AppName")//設定Application
                            .build();



                })
                //當失敗時
                .addOnFailureListener(excetion -> {
                    Log.v("hank", "addOnFailureListener");
                });
    }
}
