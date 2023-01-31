package com.mdm.installationguide.Utils;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.os.EnvironmentCompat;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by RIITLP on 1/26/2017.
 */

public class App_Constant  {
    public static String externalSDCard = "";
    public static Uri uri = null;

    public static void disabledScreenshot(Activity myActivityReference){
        myActivityReference.requestWindowFeature(Window.FEATURE_NO_TITLE);
        myActivityReference.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    public static String[] getExternalSDCardPath(Context mContext){
        String[] storageDirectories = {};
        try {

            List<String> results = new ArrayList<>();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { //Method 1 for KitKat & above
                File[] externalDirs = mContext.getExternalFilesDirs(null);

                for (File file : externalDirs) {
                    String path = file.getPath().split("/Android")[0];

                    boolean addPath = false;

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        addPath = Environment.isExternalStorageRemovable(file);
                    }
                    else{
                        addPath = Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(file));
                    }

                    if(addPath){
                        results.add(path);
                    }
                }
            }

            if(results.isEmpty()) { //Method 2 for all versions
                // better variation of: http://stackoverflow.com/a/40123073/5002496
                String output = "";
                try {
                    final Process process = new ProcessBuilder().command("mount | grep /dev/block/vold")
                            .redirectErrorStream(true).start();
                    process.waitFor();
                    final InputStream is = process.getInputStream();
                    final byte[] buffer = new byte[1024];
                    while (is.read(buffer) != -1) {
                        output = output + new String(buffer);
                    }
                    is.close();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                if(!output.trim().isEmpty()) {
                    String devicePoints[] = output.split("");
                    for(String voldPoint: devicePoints) {
                        results.add(voldPoint.split(" ")[2]);
                    }
                }
            }

            //Below few lines is to remove paths which may not be external memory card, like OTG (feel free to comment them out)
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                for (int i = 0; i < results.size(); i++) {
                    if (!results.get(i).toLowerCase().matches(".*[0-9a-f]{4}[-][0-9a-f]{4}")) {
                        //  Log.d(LOG_TAG, results.get(i) + " might not be extSDcard");
                        results.remove(i--);
                    }
                }
            } else {
                for (int i = 0; i < results.size(); i++) {
                    if (!results.get(i).toLowerCase().contains("ext") && !results.get(i).toLowerCase().contains("sdcard")) {
                        //  Log.d(LOG_TAG, results.get(i)+" might not be extSDcard");
                        results.remove(i--);
                    }
                }
            }

            storageDirectories = new String[results.size()];
            for(int i=0; i<results.size(); ++i) storageDirectories[i] = results.get(i);

        }catch (Exception e){
            e.printStackTrace();
        }

        return storageDirectories;
    }
}
