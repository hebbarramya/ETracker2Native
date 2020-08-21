package com.application.product.myapplication;


import android.app.ProgressDialog;
import android.content.Context;

public class CommonDialogs {
    public static ProgressDialog showProgressDialog(Context context, String message){
        ProgressDialog m_Dialog = new ProgressDialog(context);
        m_Dialog.setMessage(message);
        m_Dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        m_Dialog.setCancelable(false);
        return m_Dialog;
    }
    public static ProgressDialog showProgressDialogPercent(Context context, String message){
        ProgressDialog m_Dialog = new ProgressDialog(context);
        m_Dialog.setMessage(message);
        m_Dialog.setIndeterminate(false);
       // m_Dialog.setMax(100);
        m_Dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        m_Dialog.setCancelable(false);
        return m_Dialog;
    }


//    public static void showAlertDialog(final Activity activity,String message){
//        AlertDialog alertDialog = new AlertDialog.Builder(activity, R.style.AlertDialogTheme).create();
//        // Setting Dialog Title
//        alertDialog.setTitle("Confirm Update");
//        // Setting Dialog Message
//        alertDialog.setMessage("Are you sure you want to save ConfigFile?");
//
//        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "YES",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        try {
//                            processApplyConfigChanges();//Save changes
//                        } catch (IOException e) {
//                            Log.d(TAG, "Apply Config failed");
//                        }
//                        dialog.dismiss();
//                    }
//                });
//        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "NO",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                });
//        alertDialog.show();
//
//
//    }
}
