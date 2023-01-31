package com.mdm.installationguide;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import com.mdm.installationguide.Utils.App_Constant;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.documentfile.provider.DocumentFile;

import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import fr.maxcom.http.CipherFactory;
import fr.maxcom.http.LocalSingleHttpServer;

public class MainActivity extends AppCompatActivity {

    private String MDM_Offline_App_AN = "MDM-Offline-App.apk";
    private String MDM_File_Reader_App_AN = "MDM-File-Reader.apk";

    private String MDM_Offline_App_PN = "com.eduday.sakaldev";
    private String MDM_File_Reader_App_PN = "com.mdm.filereader";

    private Context mContext;
    private DecryptFileListener taskAsyncTask;
    private AppCompatButton installMDMApp,installMDMFileReader,unzipMDMFile,checkStatus;
    private LinearLayout extractingProgress;
    private TextView extractingProgressMessage,mdmappstatus,mdmfilereaderstatus,mdmfileextractstatus ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        extractingProgressMessage= findViewById(R.id.extractingProgressMessage);
        extractingProgress = findViewById(R.id.extractingProgress);
        installMDMApp = findViewById(R.id.installMDMApp);
        installMDMFileReader = findViewById(R.id.installMDMFileReader);
        unzipMDMFile = findViewById(R.id.unzipMDMFile);
        checkStatus = findViewById(R.id.checkStatus);

        mdmappstatus= findViewById(R.id.mdmappstatus);
        mdmfilereaderstatus= findViewById(R.id.mdmfilereaderstatus);
        mdmfileextractstatus= findViewById(R.id.mdmfileextractstatus);

        installMDMApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (App_Constant.uri == null) {
                    accessStoragePermission();
                }else{
                    installMDMAppFunc();
                }
            }
        });

        installMDMFileReader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (App_Constant.uri == null) {
                    accessStoragePermission();
                }else{
                    installMDMFileReaderFunc();
                }
            }
        });

        unzipMDMFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (App_Constant.uri == null) {
                    accessStoragePermission();
                }else{
                    unzipFiles();
                }
            }
        });

        checkStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mdmappstatus.setVisibility(View.GONE);
                mdmfilereaderstatus.setVisibility(View.GONE);

                if(isPackageInstalled(MDM_Offline_App_PN, mContext.getPackageManager())) {
                    mdmappstatus.setText("MDM Offline app installed successfully");
                }else {
                    mdmappstatus.setText("MDM Offline app is not installed");
                }
                mdmappstatus.setVisibility(View.VISIBLE);

                if(isPackageInstalled(MDM_File_Reader_App_PN, mContext.getPackageManager())){
                    mdmfilereaderstatus.setText("MDM File reader app installed successfully");
                }else {
                    mdmfilereaderstatus.setText("MDM File reader app is not installed");
                }
                mdmfilereaderstatus.setVisibility(View.VISIBLE);

             //   mdmfileextractstatus;
            }
        });

        String[] dd = App_Constant.getExternalSDCardPath(mContext);
        if (dd.length > 0) {
            App_Constant.externalSDCard = dd[0];
        }
        Log.d("dddddd", "onCreate()...");

        accessStoragePermission();
    }

    private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{


        }catch (Exception e){
            e.printStackTrace();
        }


    }

    @Override
    public void onBackPressed() {

    }

    /*
     * @auther - MKN
     * @purpose - SD Card Activation workflow  - Receiver - Call from OnMemoryCardChanges Listener
     * @date - 06/03/2019
     * */
//    BroadcastReceiver onClose = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {    // internet lost alert dialog method call from here...
//            finish();
//        }
//    };


    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("dddddd", "onRestart()...");
        ////finish();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d("dddddd", "onResume()...");
        ////finish();
    }


    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    private void accessStoragePermission(){
        Activity activity = (Activity) mContext;
        // Check if we have necessary permissions
        int storagePermissionWrite = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int storagePermissionRead = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);

        ArrayList<String> permissions = new ArrayList<>();

        if (storagePermissionWrite != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (storagePermissionRead != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (permissions.size() > 0) {
            String[] permissionArray = new String[permissions.size()];
            permissions.toArray(permissionArray);

            ActivityCompat.requestPermissions(activity, permissionArray, REQUEST_EXTERNAL_STORAGE);
        }else{
            if (App_Constant.uri == null) {
                new AlertDialog.Builder(mContext).setTitle("Grant Permission").setMessage("Please grant the permission").
                        setCancelable(false)
                        .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Toast.makeText(mContext, "Permission is not granted.You can not view this document.", Toast.LENGTH_SHORT).show();
                            }
                        }).
                        setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                Activity activity = ((Activity) mContext);
                                activity.startActivityForResult(intent, 42);
                            }
                        }).create().show();
            }else{
                //Permission Granted
                //loadFile();
                Toast.makeText(activity, "Permission Granted - Internal storage", Toast.LENGTH_SHORT).show();
            }

        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (App_Constant.uri == null) {
                    new AlertDialog.Builder(mContext).setTitle("Grant Permission").setMessage("Please grant the permission").
                            setCancelable(false)
                            .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Toast.makeText(mContext, "Permission is not granted.You can not view this document.", Toast.LENGTH_SHORT).show();
                                }
                            }).
                            setPositiveButton("Grant", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                                    Activity activity = ((Activity) mContext);
                                    activity.startActivityForResult(intent, 42);
                                }
                            }).create().show();
                }else{
                    //Permission Granted
                    //loadFile();
                    Toast.makeText(mContext, "Permission Granted - External SD card storage", Toast.LENGTH_SHORT).show();
                }
            }else{
                accessStoragePermission();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("onActivityResult", "-- "+requestCode+ resultCode);

        if(requestCode == 1234){
            Toast.makeText(mContext, "App installation Permission is granted.", Toast.LENGTH_SHORT).show();
        }else if (data == null) {
            Toast.makeText(mContext, "Oops Permission is not granted.Please grant the permission again.", Toast.LENGTH_SHORT).show();
        } else {
            try {
                DocumentFile pickedDir = DocumentFile.fromTreeUri(mContext, data.getData());
                String pathSelected = pickedDir.getName();
                //Toast.makeText(mContext, "pathSelected"+pathSelected+App_Constant.externalSDCard, Toast.LENGTH_SHORT).show();
                //
                if (App_Constant.externalSDCard.contains(pathSelected)) {
                    App_Constant.uri = data.getData();
                    mContext.grantUriPermission(mContext.getPackageName(), App_Constant.uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    mContext.getContentResolver().takePersistableUriPermission(App_Constant.uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    //loadFile();
                    Toast.makeText(mContext, "Permission Granted - External SD card storage", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, "You have not selected External SD Card as Folder due to this Permission is not granted.Please select External SD card directory.", Toast.LENGTH_SHORT).show();
                }

                /*String pathSelected = pickedDir.getName();
                if(pathSelected.equals("edutab")){
                    mContext.grantUriPermission(mContext.getPackageName(), App_Constant.uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    mContext.getContentResolver().takePersistableUriPermission(App_Constant.uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    Toast.makeText(mContext, "Good. Permission is granted.Please click on download button again.", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(mContext, "Oops Permission is not granted.Modified changes will not getting saved.", Toast.LENGTH_SHORT).show();
                }*/


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    void installAPK(File file){

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uriFromFile(getApplicationContext(), file), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            getApplicationContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("TAG", "Error in opening the file!");
        }
    }
    Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }
    
    private void installMDMAppFunc() {
        try{
            File MDMFolder = new File(App_Constant.externalSDCard+"/"+"MDM Setup Guide");
            if(MDMFolder.exists()){
                File mdmAppUrl = new File(MDMFolder.getAbsolutePath(),MDM_Offline_App_AN);

                if(mdmAppUrl.exists()){

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (!getPackageManager().canRequestPackageInstalls()) {
                            new AlertDialog.Builder(mContext)
                                    .setTitle("Grant Permission")
                                    .setMessage("Please grant the external app installation permission")

                                    // Specifying a listener allows you to take an action before dismissing the dialog.
                                    // The dialog is automatically dismissed when a dialog button is clicked.
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Continue with delete operation
                                            startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", getPackageName()))), 1234);
                                        }
                                    })
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        } else {
                            installAPK(mdmAppUrl);
                        }
                    }else {
                        installAPK(mdmAppUrl);
                    }

//                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//                    StrictMode.setVmPolicy(builder.build()); // temp solution
//                    Intent intent = new Intent(Intent.ACTION_VIEW);
//                    intent.setDataAndType(Uri.fromFile(mdmAppUrl), "application/vnd.android.package-archive");
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(intent);
                }else{
                    Toast.makeText(mContext, "MDM Offline App apk is not found", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(mContext, "MDM Setup Guide folder is not found", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            Toast.makeText(mContext, "MDM Offline App Error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void installMDMFileReaderFunc() {
        try{
            File MDMFolder = new File(App_Constant.externalSDCard+"/"+"MDM Setup Guide");
            if(MDMFolder.exists()){
                File mdmAppUrl = new File(MDMFolder.getAbsolutePath(),MDM_File_Reader_App_AN);

                if(mdmAppUrl.exists()){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (!getPackageManager().canRequestPackageInstalls()) {
                                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).setData(Uri.parse(String.format("package:%s", getPackageName()))), 1234);
                            } else {
                                installAPK(mdmAppUrl);
                            }
                        }else {
                            installAPK(mdmAppUrl);
                        }
//                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
//                    StrictMode.setVmPolicy(builder.build()); // temp solution
//                    Intent intent = new Intent(Intent.ACTION_VIEW);
//                    intent.setDataAndType(Uri.fromFile(mdmAppUrl), "application/vnd.android.package-archive");
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    mContext.startActivity(intent);
                }else{
                    Toast.makeText(mContext, "MDM File Reader App apk is not found", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(mContext, "MDM Setup Guide folder is not found", Toast.LENGTH_SHORT).show();
            }
        }catch (Exception e){
            Toast.makeText(mContext, "MDM File Reader Error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }


//    private String readFile(File file){
//        //Read text from file
//        StringBuilder text = new StringBuilder();
//
//        try {
//            BufferedReader br = new BufferedReader(new FileReader(file));
//            String line;
//
//            while ((line = br.readLine()) != null) {
//                text.append(line);
//                text.append('\n');
//            }
//            br.close();
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//            //You'll need to add proper error handling here
//        }
//        return  text.toString();
//    }
//    private void loadFile(){
//        try{
//
//            new android.os.Handler().postDelayed(
//            new Runnable() {
//                public void run() {
//
//                    File path = new File(Environment.getExternalStorageDirectory() + "/MDM/info.txt");
//                    String text = readFile(path);
//                    Log.d("data -- ", text);
//                    Toast.makeText(mContext, "Loading file ", Toast.LENGTH_SHORT).show();
//                    String[] fileDetails = text.split("--#MDM#--");
//                    if(fileDetails.length == 3){
//                        file_name = fileDetails[0];//getIntent().getStringExtra("file_name");
//                        pdf_path = fileDetails[1];//getIntent().getStringExtra("file_type");
//                        file_type = fileDetails[2];//getIntent().getStringExtra("pdf_path");
//
//                        File file = new File(App_Constant.externalSDCard+"/"+"MDM"+"/"+pdf_path);
//
//                        if(file.exists())
//                            startDecryptFile(file);
//                        else{
//                            Toast.makeText(mContext, "File not found.", Toast.LENGTH_SHORT).show();
//                        }
//                    }else{
//                        try{
//                            new AlertDialog.Builder(mContext)
//                                    .setTitle("Error")
//                                    .setMessage("Invalid file")
//
//                                    // Specifying a listener allows you to take an action before dismissing the dialog.
//                                    // The dialog is automatically dismissed when a dialog button is clicked.
//                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
//                                        public void onClick(DialogInterface dialog, int which) {
//                                            // Continue with delete operation
//                                            finish();
//                                        }
//                                    })
//                                    .setIcon(android.R.drawable.ic_dialog_alert)
//                                    .show();
//                        }catch (Exception e){
//                            Toast.makeText(mContext, "Closing app", Toast.LENGTH_SHORT).show();
//                            e.printStackTrace();
//                        }
//                    }
//
//
//
//                }
//            }, 1000);
//
//
//        }catch (Exception e){
//
//            e.printStackTrace();
//        }
//    }

    private void unzipFiles(){
        try {


            File MDMFolder = new File(App_Constant.externalSDCard+"/"+"MDM Setup Guide");
            if(MDMFolder.exists()){
                File mdmAppUrl = new File(MDMFolder.getAbsolutePath(),".MDMImages.zip");

                if(mdmAppUrl.exists()){

                    extractingProgress.setVisibility(View.VISIBLE);
                    File MDMExtractFolder = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+".MDMImages");
                    if(MDMExtractFolder.exists()){
                        MDMExtractFolder.delete();
                    }

                   // List<ZipEntry> fileList =  wZip.getFilesInfoFromZip(mdmAppUrl);
                   // Toast.makeText(mContext, "filelist - "+fileList.size(), Toast.LENGTH_SHORT).show();

//                    try {
//                        Toast.makeText(MainActivity.this,"Extract files to "+MDMExtractFolder.getPath(),Toast.LENGTH_SHORT).show();
//
//                        ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(mdmAppUrl)));
//                        ZipEntry ze;
//                        int count;
//                        byte[] buffer = new byte[8192];
//                        while ((ze = zis.getNextEntry()) != null) {
//                            File file = new File(Environment.getExternalStorageDirectory(), ze.getName());
//                            File dir = ze.isDirectory() ? file : file.getParentFile();
//                            if (!dir.isDirectory() && !dir.mkdirs())
//                                throw new FileNotFoundException("Failed to ensure directory: " +
//                                        dir.getAbsolutePath());
//                            if (ze.isDirectory())
//                                continue;
//                            FileOutputStream fout = new FileOutputStream(file);
//                            try {
//                                while ((count = zis.read(buffer)) != -1)
//                                    fout.write(buffer, 0, count);
//                            } finally {
//                                fout.close();
//                            }
//                        }
//                        zis.close();
//                        Toast.makeText(mContext,"Support files extracted successfully",Toast.LENGTH_SHORT).show();
////                        if(Zip.unzip(mdmAppUrl, Environment.getExternalStorageDirectory())){
////                            Toast.makeText(mContext,"Support files extracted successfully",Toast.LENGTH_SHORT).show();
////                        }else{
////                            Toast.makeText(mContext,"Support files extracted unsuccessfully",Toast.LENGTH_SHORT).show();
////                        }
                        if(taskAsyncTask != null && taskAsyncTask.getStatus() == AsyncTask.Status.RUNNING)
                            taskAsyncTask.cancel(true);

                        taskAsyncTask = new DecryptFileListener(mdmAppUrl, Environment.getExternalStorageDirectory());
                        taskAsyncTask.execute();

//
//                    }catch (Exception e){
//                        e.printStackTrace();
//                        Toast.makeText(MainActivity.this,"ERROR",Toast.LENGTH_SHORT).show();
//                        extractingProgress.setVisibility(View.GONE);
//                    }


//                    wZip.unzip(mdmAppUrl,
//                            new File(Environment.getExternalStorageDirectory().getAbsolutePath()),
//                            ".MDMImages",
//                            MainActivity.this);

                }else{
                    Toast.makeText(mContext, "MDM Support files is not found", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(mContext, "MDM Setup Guide folder is not found", Toast.LENGTH_SHORT).show();
            }


        } catch (Exception e) {
            Toast.makeText(mContext, "MDM Support files Error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();

        }
    }

     private class DecryptFileListener extends AsyncTask<Void, Integer, Void> {

        private File zipFile;
        private File targetDirectory;
        public DecryptFileListener(File iZipFile,File iTargetDirectory) {
            super();
            zipFile = iZipFile;
            targetDirectory = iTargetDirectory;
        }

        @Override
        protected void onPreExecute() {
            Log.i("makemachine", "onPreExecute()");
            super.onPreExecute();
        }

        @Override
        protected synchronized Void doInBackground(Void... params) {


            try {
                int doneCount = 0;
                ZipInputStream zis =  new ZipInputStream(
                        new BufferedInputStream(new FileInputStream(zipFile)));
                ZipEntry ze;


                int count;
                byte[] buffer = new byte[8192];
                while ((ze = zis.getNextEntry()) != null) {
                    File file = new File(targetDirectory, ze.getName());
                    File dir = ze.isDirectory() ? file : file.getParentFile();
                    if (!dir.isDirectory() && !dir.mkdirs())
                        throw new FileNotFoundException("Failed to ensure directory: " +
                                dir.getAbsolutePath());
                    if (ze.isDirectory())
                        continue;
                    FileOutputStream fout = new FileOutputStream(file);
                    try {
                        while ((count = zis.read(buffer)) != -1)
                            fout.write(buffer, 0, count);
                    } finally {
                        doneCount++;
                        publishProgress(doneCount);
                        fout.close();
                    }

                }
                zis.close();
            }catch (Exception e){
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            extractingProgressMessage.setText("Extracting files...("+String.valueOf(values[0])+")");
            Log.d("makemachine", "onProgressUpdate(): " + String.valueOf(values[0]));
        }

        @Override
        protected void onPostExecute(Void result) {

            super.onPostExecute(result);
            Toast.makeText(mContext,"Support files extracted successfully",Toast.LENGTH_SHORT).show();
            extractingProgress.setVisibility(View.GONE);
        }


    }


}