package com.example.g_google_drive;
//http://cmnocsexperience.blogspot.com/2019/01/android-app-google-google-drive.html
//https://github.com/mesadhan/google-drive-app/blob/master/app/src/main/java/io/github/mesadhan/drive_rest_sample/DriveServiceHelper.java

import androidx.annotation.Nullable;
//                .requestId() //指定你的Application請求ID
//                .requestIdToken() //指定的請求Token(Service用戶端將會被驗證的用戶端ID)
//                .requestProfile()  //指定你的Application請求用戶的配置文件訊息
//                .requestServerAuthCode()  //指令離線請求訪問(需要驗證的id)

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

/* com.google.api.services.drive.model.FileList
 * drive.FileList
 * */
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.EditText;

import java.util.Collections;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "hank";

    private static final int REQUEST_CODE_SIGN_IN = 1;
    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;

    private DriveServiceHelper mDriveServiceHelper;
    private String mOpenFileId;

    private EditText mFileTitleEditText;
    private EditText mDocContentEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        //8.準備開檔案,創造檔案,儲存檔案,查詢檔案按鈕方法

        findViewById(R.id.open_btn).setOnClickListener(view -> openFilePicker());
        findViewById(R.id.create_btn).setOnClickListener(view -> createFile());
        findViewById(R.id.save_btn).setOnClickListener(view -> saveFile());
        findViewById(R.id.query_btn).setOnClickListener(view -> query());

        mFileTitleEditText = findViewById(R.id.file_title_edittext);
        mDocContentEditText = findViewById(R.id.doc_content_edittext);

        requestSignIn();
    }
    //3.當使用者同意登入Gmail,將資料給GoogleDriver後
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                break;

            //9.接收openFilePicker傳來的Intent
            case REQUEST_CODE_OPEN_DOCUMENT:
                if (resultCode == Activity.RESULT_OK && resultData != null) {  //如果使用者按下ok,且intent有進來的話
                    Uri uri = resultData.getData();
                    if (uri != null) {
                        openFileFromFilePicker(uri);
                    }
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, resultData);
    }


    //1.登入
    private void requestSignIn() {
        Log.d(TAG, "Requesting sign-in");

        //1.取得GoogleSignInOptions物件實體,其中 requestScopes 設定服務類型，requestIdToken 就填入剛才申請的 Client ID。
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN) // GoogleSignInOptions.Builder =>建構要登入的功能()(呼叫service的email登入)
                        .requestEmail()
                        .requestScopes(new Scope(DriveScopes.DRIVE_FILE)) //指定你的Application請求用戶的配置文件訊息(DRIVE_FILE == 查看管理你的或創建Google雲頓硬碟與文件 , )
                        .requestIdToken("490722671996-tkt938euvqbppv44p6ov9ul35k0rhe9i.apps.googleusercontent.com")//設定指定的Token(這邊是剛Google給我的ClientId)
                        .build(); //建立

        //2.取得GoogleSignInClient進行登入,Intent呼叫出登入Google帳號
        GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);//取得Google登入的Client (1.Context,2.Siging選項設定)
        startActivityForResult(client.getSignInIntent(), REQUEST_CODE_SIGN_IN);//將這個設定好的取得GoogleIntent (回傳值Intente)
    }

    //4.取得使用者登入的intnent的googleAccount物件,當取得時會跳出允許授權取得雲端的Dialog,當使用者成功登入GoogleDriver後的Handle方法
    private void handleSignInResult(Intent result) {

        //取得使用者登入的intnent的googleAccount物件
        GoogleSignIn.getSignedInAccountFromIntent(result)  //取得使用者登入的intnent的googleAccount物件,當取得時會跳出允許授權取得雲端的Dialog
                //當使用者都允許權限時,取得使用者Google的資訊
                .addOnSuccessListener(googleAccount -> {
                    String email = googleAccount.getEmail();//取得Google-email
                    String displayName = googleAccount.getDisplayName();//取得Google-名稱
                    String familyName = googleAccount.getFamilyName(); //取得Google-姓氏
                    String givenName = googleAccount.getGivenName(); //取得Google-指定的名稱
                    Set<Scope> scopeSet = googleAccount.getGrantedScopes(); //取得Google允許的權限範圍
                    String id = googleAccount.getId(); //取得Google-ID
                    String token = googleAccount.getIdToken();  //取得Google-Token
                    Uri photoUrl = googleAccount.getPhotoUrl(); //取得Google-大頭貼照片網址
                    String serverAuthCode = googleAccount.getServerAuthCode();
                    Log.v(TAG, "addOnSuccessListener =>  googleAccount.getEmail():" + email + "\n" + "/displayName:" + displayName + "\n" + "/familyName:" + familyName + "\n" + "/givenName:" + givenName + "\n" + "/id:" + id + "\n" + "/token:" + token + "\n" + "/serverAuthCode:" + serverAuthCode + photoUrl);


                    //6.需要 GoogleAccountCredential有實作HttpTransport
//                  GoogleAccountCredential.usingOAuth2(Context context, Collection<String> scopes)://GoogleAccountCredential物件實體化(1.Context,2.Collection<String> scopes)
                    GoogleAccountCredential googleAccountCredential =
                            GoogleAccountCredential.usingOAuth2(
                                    this, //1.content
                                    Collections.singleton(DriveScopes.DRIVE_FILE) //2.選擇功能範圍
                            );
                    googleAccountCredential.setSelectedAccount(googleAccount.getAccount());


                    //5.創建GoogleDrive
                    //https://googleapis.dev/java/google-api-client/latest/com/google/api/client/googleapis/services/json/AbstractGoogleJsonClient.Builder.html
                    //Drive.Builder( //Drive.建立物件實體化
//                    com.google.api.client.http.HttpTransport transport, //1.新兼容運輸
//                    com.google.api.client.json.JsonFactory jsonFactory,//Json工廠
//                    com.google.api.client.http.HttpRequestInitializer httpRequestInitializer)//3.Http請求初始化物件

                    //6.創建Drive
                    Drive googleDriveService = new Drive.Builder(//Drive.建立物件實體化
                            AndroidHttp.newCompatibleTransport(), //1.新兼容運輸
                            new GsonFactory(), //2.Json工廠
                            googleAccountCredential) //3.Http請求初始化物件
                            .setApplicationName("AppName")//設定Application
                            .build();

                    //7.Drive封裝了新增,查詢,創建,打開方法
                    mDriveServiceHelper = new DriveServiceHelper(googleDriveService);
                })
                //當失敗時
                .addOnFailureListener(excetion -> {
                    Log.v(TAG, "addOnFailureListener");
                });
    }

    /**
     * Opens the Storage Access Framework file picker using {@link #REQUEST_CODE_OPEN_DOCUMENT}.
     */
    private void openFilePicker() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening file picker.");

            Intent pickerIntent = mDriveServiceHelper.createFilePickerIntent();

            // The result of the SAF Intent is handled in onActivityResult.
            startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT);
        }
    }

    //10.打開檔案選擇器方法,將名字跟內容顯示在UI上
    private void openFileFromFilePicker(Uri uri) {  //取得drive的name跟Content
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Opening " + uri.getPath());

            mDriveServiceHelper.openFileUsingStorageAccessFramework(getContentResolver(), uri)
                    .addOnSuccessListener(nameAndContent -> {
                        String name = nameAndContent.first;
                        String content = nameAndContent.second;

                        mFileTitleEditText.setText(name);
                        mDocContentEditText.setText(content);

                        Log.v(TAG, "openFileFromFilePicker => 成功:" + "name:" + name + "/content:" + content);
                        setReadOnlyMode();
                    })
                    .addOnFailureListener(e -> {
                        Log.v(TAG, "openFileFromFilePicker => 失敗:" + e.toString());
                    });
        }
    }

    //11.按下按鈕創建Drive.檔案
    private void createFile() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a file.");

            mDriveServiceHelper.createFile()
                    .addOnSuccessListener(fileId -> readFile(fileId)) //目前創建是固定的File Name
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "Couldn't create file.", exception));
        }
    }


    //12.讀取檔案,呈現在ui
    private void readFile(String fileId) {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Reading file " + fileId);

            mDriveServiceHelper.readFile(fileId)
                    .addOnSuccessListener(nameAndContent -> {
                        String name = nameAndContent.first;
                        String content = nameAndContent.second;

                        mFileTitleEditText.setText(name);
                        mDocContentEditText.setText(content);

                        setReadWriteMode(fileId);
                    })
                    .addOnFailureListener(e -> {
                        Log.v(TAG, "Couldn't read file:" + e.toString());
                    });
        }
    }

    //13.按下存檔按鈕
    private void saveFile() {
        if (mDriveServiceHelper != null && mOpenFileId != null) { //如果Drive有近來而且已經有創建driveFile拿到id
            Log.d(TAG, "Saving " + mOpenFileId);

            String fileName = mFileTitleEditText.getText().toString();
            String fileContent = mDocContentEditText.getText().toString();

            mDriveServiceHelper.saveFile(mOpenFileId, fileName, fileContent)
                    .addOnFailureListener(exception ->
                            Log.e(TAG, "無法通過save保存檔案:" + exception));
        }
    }

    //14查詢使用者APP上可看的所有Files
    private void query() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Querying for files.");

            mDriveServiceHelper.queryFiles() //取得FileList
                    .addOnSuccessListener(fileList -> { //尋訪FileList,拿到File物件
                        StringBuilder builder = new StringBuilder();
                        for (File file : fileList.getFiles()) {
                            Log.v(TAG, file.getName()); //取得File的Name
                            builder.append(file.getName()).append("\n"); //一行一行加上去加換行
                        }
                        String fileNames = builder.toString();

                        mFileTitleEditText.setText("File List");
                        mDocContentEditText.setText(fileNames);

                        setReadOnlyMode();
                    })
                    .addOnFailureListener(exception -> Log.e(TAG, "Unable to query files.", exception));
        }
    }

    /**
     * Updates the UI to read-only mode.
     */
    private void setReadOnlyMode() {
        mFileTitleEditText.setEnabled(false);
        mDocContentEditText.setEnabled(false);
        mOpenFileId = null;
    }

    /**
     * Updates the UI to read/write mode on the document identified by {@code fileId}.
     */
    private void setReadWriteMode(String fileId) {
        mFileTitleEditText.setEnabled(true);
        mDocContentEditText.setEnabled(true);
        mOpenFileId = fileId;
    }
}