package com.mdm.installationguide.Utils;

import android.app.ProgressDialog;
import android.content.Context;



public class ResponseDialog {
    public ProgressDialog pDialog;

    public void showDialog() {
        if (!pDialog.isShowing())
            pDialog.show();
    }
    public void hideDialog() {
        if (pDialog != null && pDialog.isShowing())
            pDialog.dismiss();
    }
    public boolean checkOpen(){
        return pDialog.isShowing();
    }

    public ResponseDialog(Context mContext){
        pDialog = new ProgressDialog(mContext);
        pDialog.setCancelable(false);
    }
}