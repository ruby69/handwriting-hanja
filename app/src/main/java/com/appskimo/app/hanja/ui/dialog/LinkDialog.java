package com.appskimo.app.hanja.ui.dialog;

import androidx.appcompat.app.AlertDialog;

public class LinkDialog extends WebViewDialog {
    public static final String TAG = "LinkDialog";
    private String url;

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) getDialog();
        if (dialog != null) {
            webView.loadUrl(url);
        }
    }

    public LinkDialog setUrl(String url) {
        this.url = url;
        return this;
    }
}