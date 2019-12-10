package com.appskimo.app.hanja.support;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import com.appskimo.app.hanja.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.ActivityCompat;

public class PermissionChecker {
    public final static int REQUIRED_PERMISSION_REQUEST_CODE = 2121;
    public static final int REQUEST_ID_CAMERA = 0;
    public static final String[] PERMISSIONS_CAMERA = {Manifest.permission.CAMERA};

    private Context context;

    public PermissionChecker(Context context) {
        this.context = context;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public boolean isRequiredPermissionGranted() {
        if (isMarshmallowOrHigher()) {
            return Settings.canDrawOverlays(context);
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public Intent createRequiredPermissionIntent() {
        if (isMarshmallowOrHigher()) {
            return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.getPackageName(), null));
        }
        return null;
    }

    private boolean isMarshmallowOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }


    private static void requestPermissions(final Activity activity, View snackbarLayer, int messageId, final int requestId, final String[] permissions) {
        if (shouldShowRequestPermissionRationale(activity, permissions)) {
            Snackbar.make(snackbarLayer, messageId, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(activity, permissions, requestId);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(activity, permissions, requestId);
        }
    }

    private static boolean shouldShowRequestPermissionRationale(Activity activity, String[] permissions) {
        for (String permission : permissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return false;
            }
        }
        return true;
    }

    public static void checkAndExecute(String[] permissions, Activity activity, View snackbarLayer, int messageId, final int requestId, Executor executor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> notGrantedPermissions = getNotGrantedPermissions(activity, permissions);
            if (notGrantedPermissions.isEmpty()) {
                if (executor != null) {
                    executor.execute();
                }
            } else {
                String[] requestedPermissions = notGrantedPermissions.toArray(new String[notGrantedPermissions.size()]);
                requestPermissions(activity, snackbarLayer, messageId, requestId, requestedPermissions);
            }
        } else {
            if (executor != null) {
                executor.execute();
            }
        }
    }

    private static List<String> getNotGrantedPermissions(Activity context, String[] permissions) {
        List<String> list = new ArrayList<>();
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                list.add(permission);
            }
        }
        return list;
    }

    public interface Executor {
        void execute();
    }
}
