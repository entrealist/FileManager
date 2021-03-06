package pro.filemanager.core

import android.app.Activity
import android.os.Build
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import pro.filemanager.R

/*
    A wrapper class encapsulating platform version, permission status, rationale checks; permission requests and request result handling.
    It also shows rationale dialogs and takes user to application details when "Deny and do not ask again" option is selected.
*/

object PermissionWrapper {
    const val KEY_SP_DO_NOT_ASK_AGAIN_EXTERNAL_STORAGE_SELECTED = "doNotAskAgainExternalStorageSelected"
    const val REQUEST_CODE_EXTERNAL_STORAGE = 1

    fun requestExternalStorage(activity: Activity, alreadyGrantedOrLessThanApi23Action: Runnable = Runnable {}) {
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if(activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) || activity.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    showDeniedRationaleDialog(activity)
                } else if(activity.getSharedPreferences(KEY_SP_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SP_DO_NOT_ASK_AGAIN_EXTERNAL_STORAGE_SELECTED, false)) {
                    showDoNotAskAgainSelectedRationaleDialog(activity)
                } else {
                    activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_EXTERNAL_STORAGE)
                }
            } else {
                alreadyGrantedOrLessThanApi23Action.run()
            }
        } else {
            alreadyGrantedOrLessThanApi23Action.run()
        }
    }

    @SuppressLint("NewApi")
    fun handleExternalStorageRequestResult(activity: Activity, requestCode: Int, grantResults: IntArray, successAction: Runnable = Runnable {}, denialAction: Runnable = Runnable {}) {
        if(requestCode == REQUEST_CODE_EXTERNAL_STORAGE) {
            if(grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                activity.getSharedPreferences(KEY_SP_NAME, Context.MODE_PRIVATE).edit().remove(KEY_SP_DO_NOT_ASK_AGAIN_EXTERNAL_STORAGE_SELECTED).apply()
                successAction.run()
            } else {
                if(!activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) || !activity.shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    activity.getSharedPreferences(KEY_SP_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_SP_DO_NOT_ASK_AGAIN_EXTERNAL_STORAGE_SELECTED, true).apply()
                }

                denialAction.run()
            }
        }
    }

    @SuppressLint("NewApi")
    fun showDeniedRationaleDialog(activity: Activity) {
        AlertDialog.Builder(activity)
                .setTitle(R.string.external_storage_denied_rationale_title)
                .setMessage(R.string.external_storage_denied_rationale_message)
                .setPositiveButton(R.string.permission_grant) { _: DialogInterface, _: Int ->
                    activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_CODE_EXTERNAL_STORAGE)
                }
                .setNegativeButton(R.string.permission_deny) { _: DialogInterface, _: Int ->
                    activity.onBackPressed()
                }
                .create()
                .show()
    }

    fun showDoNotAskAgainSelectedRationaleDialog(activity: Activity) {
        AlertDialog.Builder(activity)
                .setTitle(R.string.external_storage_denied_rationale_title)
                .setMessage(R.string.external_storage_do_not_ask_again_selected_rationale_message)
                .setPositiveButton(R.string.permission_grant) { _: DialogInterface, _: Int ->
                    openAppDetailsSettings(activity)
                }
                .setNegativeButton(R.string.permission_deny) { _: DialogInterface, _: Int ->
                    activity.onBackPressed()
                }
                .create()
                .show()
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }

        context.startActivity(intent)
    }

    @SuppressLint("NewApi")
    fun checkExternalStoragePermissions(context: Context) =
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP ||
                    (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                            context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

}