package com.appskimo.app.hanja.ui.dialog;

public class LinkDialog extends WebViewDialog {
    public static final String TAG = "LinkDialog";
    private String url;

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            webView.loadUrl(url);
        }
    }

    public LinkDialog setUrl(String url) {
        this.url = url;
        return this;
    }
}
