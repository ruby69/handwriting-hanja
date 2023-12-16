package com.appskimo.app.hanja.support;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;

import androidx.core.app.ActivityCompat;

import com.appskimo.app.hanja.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class PermissionChecker {
    public final static int REQUIRED_PERMISSION_REQUEST_CODE = 2121;

    private Context context;

    public PermissionChecker(Context context) {
        this.context = context;
    }

    public boolean isRequiredPermissionGranted() {
        return Settings.canDrawOverlays(context);
    }

    public Intent createRequiredPermissionIntent() {
        return new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.getPackageName(), null));
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
        for (var permission : permissions) {
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return false;
            }
        }
        return true;
    }

    public static void checkAndExecute(String[] permissions, Activity activity, View snackbarLayer, int messageId, final int requestId, Executor executor) {
        var notGrantedPermissions = getNotGrantedPermissions(activity, permissions);
        if (notGrantedPermissions.isEmpty()) {
            if (executor != null) {
                executor.execute();
            }
        } else {
            var requestedPermissions = notGrantedPermissions.toArray(new String[notGrantedPermissions.size()]);
            requestPermissions(activity, snackbarLayer, messageId, requestId, requestedPermissions);
        }
    }

    private static List<String> getNotGrantedPermissions(Activity context, String[] permissions) {
        var list = new ArrayList<String>();
        for (var permission : permissions) {
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
