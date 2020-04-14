package com.example.g_google_drive;
/*
 7.創建DriveServiceHelper處理GoogleDrive的設定
 *通過REST API對雲端硬盤文件執行讀/寫操作並打開
 *通過Storage Access Framework的文件選擇器UI。
 * 主要玩得是:從Activity建好的Drive
 * Tasks =>https://developers.google.com/android/reference/com/google/android/gms/tasks/Tasks
 *com.google.api.services.drive.model.File =>https://developers.google.com/resources/api-libraries/documentation/drive/v3/java/latest/com/google/api/services/drive/model/File.html
 //Google Http API:https://googleapis.dev/java/google-http-client/latest/com/google/api/client/http/ByteArrayContent.html
 //IntentAPI:https://blog.csdn.net/zhangjg_blog/article/details/10901293
 */

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive driveService; //activity設定好的Drive


    //A.建構式取得Drive
    public DriveServiceHelper(Drive driveService) {
        this.driveService = driveService;
    }
    //Tasks.call(Executor var0,Callable<TResult> var1):(回傳<TResult> Task<TResult> )

    //com.google.api.services.drive.model.File
    /*
    *drive.File.setParents(java.util.List<java.lang.String> parents) ://設定包含父類別的ID,如果創建中未指定,則直接在放置在Drive中,更新Request的話用addParents,removeParents參數來修改父列表(回傳File)
    *drive.File.setMimeType(java.lang.String mimeType):設定檔案類型(設定類型)(回傳File)
    *文件的MIME類型。 驅動器將嘗試自動從中檢測適當的值
    *如果沒有提供值，則上傳的內容。 除非更改了新版本，否則無法更改該值。
    *上傳。如果使用Google Doc MIME類型創建文件，則上傳的內容將為
    *盡可能進口。 受支持的導入格式在“關於”資源中發布。
    *
    *drive.File.setName(java.lang.String name): //設定文件名稱("自訂文件名")(回傳File)
    *drive.File.getId()://回傳drive.File的ID(回傳值String)
    *drive.get(java.lang.String fileId)://取得Drive.File的物件實體(Drive.File的id)(回傳值Get)
    * */

    //com.google.api.services.Drive
    /*
     * Drive.files()://用於Files創建Request集合的訪問器
     * Drive.create(com.google.api.services.drive.model.File content)://創建File(要上傳的drive File)(回傳Create)
     * Drive.execute()://將數據傳送到Service,並起將解析的數據從response回來(回傳T解析好的資料結構)
     * Drive.executeMediaAsInputStream():drive.File輸入串流(回傳直java.io.InputStream)
     *
     * Drive.update( //drive.update將內容上傳更新drive
     * java.lang.String fileId, //1.drive.檔案ID
     *  com.google.api.services.drive.model.File content, //2.要上傳的檔案路徑
     *  com.google.api.client.http.AbstractInputStreamContent mediaContent  //3.上傳的Stream格式類型
     * )
     * Drive.list://列出drive文件表
    //Drive.setSpaces(java.lang.String spaces) //設定文字逗號空間(設定空間的字串)(回傳List)
     * */

    //com.google.api.client.http.ByteArrayContent  //基於字節數組提供http內容
    /*
     * ByteArrayContent.fromString(String type, String contentString): //設定串流類型(1.上傳檔案格式,2.上傳的內容)(回傳ByteArrayContent)
     * */

    //B.在雲端用戶創建資料夾,並且回傳我的file ID
    public Task<String> createFile() {
        //1.執行呼叫任務
        return Tasks.call(mExecutor, () -> {

            //2.準備好drive.File設定檔案類型,名稱放入create
            com.google.api.services.drive.model.File driveFile = new com.google.api.services.drive.model.File()
                    .setParents(Collections.singletonList("root"))//設定包含父類別的ID,如果創建中未指定,則直接在放置在Drive中,更新Request的話用addParents,removeParents參數來修改父列表
                    .setMimeType("text/plain")//設定檔案類型
                    .setName("Hank File"); //設定文件名稱()


            //3.創建googleFile,並且將設定好的數據傳送到Service
            File googleFile = driveService
                    .files() //用於Files創建Request集合的訪問器
                    .create(driveFile) //創建File
                    .execute(); //將數據傳送到Service,並起將解析的數據從response回來

            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation");
            }

            return googleFile.getId(); //回傳drive.File的ID
        });
    }


    /**
     * C.取得drive的檔案名,跟讀取檔案內容,回傳pair資料
     * @param fileId = >drive.檔案ID
     * */
    public Task<Pair<String, String>> readFile(String fileId) {
        //1.執行呼叫任務
        return Tasks.call(mExecutor, () -> {
            //2.取得Drive.File物件實體,靠get(指定FileId)
            File metadata = driveService.files()
                    .get(fileId)//取得Drive.File的物件實體(Drive.File的id)
                    .execute();
            String fileName = metadata.getName();

            //3.讀取GoogleDrive的檔案內容串流
            try (InputStream inputStream = driveService.files().get(fileId)
                    .executeMediaAsInputStream();//取得drive.file的輸入串流
                 BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                String contents = stringBuilder.toString();
                return Pair.create(fileName, contents);
            }
        });
    }

    /**
    *D.儲存並且更新檔案
    * @param fileId   =>drive.檔案id
    * @param FileName =>drive.檔案名稱
    * @param content  =>drive.字串內容
    */
    public Task<Void> saveFiles(String fileId, String FileName, String content) {
        return Tasks.call(mExecutor, () -> {
            //2.創建可以更改任何數據的File,放入update
            File file = new File().setName(FileName);

            //3.將內容轉為有包含抽象的輸入串流
            ByteArrayContent byteArrayContentStream = ByteArrayContent.fromString("text/plain", content); //設定串流類型(1.上傳檔案格式,2.上傳的內容)(回傳ByteArrayContent)

            //1.drive.update將內容上傳更新drive
            driveService.files()
                    .update( //drive.update將內容上傳更新drive
                            fileId,//1.drive的檔案id
                            file,  //2.檔案
                            byteArrayContentStream //3.串流格式
                    );
            return null;
        });
    }

    /**
     * E.回傳使用者雲端,所有可見的文件 @{link FileList}
     */
    public Task<FileList> queryFiles() {
        return Tasks.call(mExecutor, new Callable<FileList>() {
            @Override
            public FileList call() throws Exception {

                return driveService.files()
                        .list() //列出drive文件表
                        .setSpaces("drive") //設定文字逗號空間()
                        .execute();
            }
        });
    }

    /**
     * F.產生一個檔案選擇器的Intent* */
    public Intent createFilePickerIntent(){
        //Intent.ACTION_OPEN_DOCUMENT:文件劉覽器
        //intent.CATEGORY_OPENABLE:打開多個應用選取文件的選擇器
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(intent.CATEGORY_OPENABLE); //設定intent類別功能為,打開多個應用選取文件的選擇器
        intent.setType("text/plain");
        return intent;
    }

    /**
     * G.透過Intent.getData.Uri網址,跟ContentResolver取得檔案跟解析內容
     *OpenableColumns:https://developer.android.com/reference/android/provider/OpenableColumns
     * */
    public Task<Pair<String, String>> openFileUsingStorageAccessFrameWork(ContentResolver contentResolver, Uri uri) {
        //  ContentResolver.query( //查詢(回傳Cursor)
        //            @RequiresPermission.Read @NonNull Uri uri,
        //            @Nullable String[] projection,
        //            @Nullable String selection,
        //            @Nullable String[] selectionArgs,
        //            @Nullable String sortOrder)
        return Tasks.call(mExecutor, () -> {
            //1.返回drive文件的顯示名
            String name;
            try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToNext()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);//取得要查詢的欄位:顯示名稱
                    name = cursor.getString(nameIndex);
                } else {
                    throw new IOException("Empty cursor returned for file.");
                }
            }
            //2.讀取drive.文件內容
            String content;
            try (InputStream inputStream = contentResolver.openInputStream(uri)) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder stringBuilder = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                content = stringBuilder.toString();
            }
            return Pair.create(name, content);
        });

    }


}
