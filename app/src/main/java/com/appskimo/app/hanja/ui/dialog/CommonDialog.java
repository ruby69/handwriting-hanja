package com.appskimo.app.hanja.ui.dialog;

import android.content.DialogInterface;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

public abstract class CommonDialog extends DialogFragment {
    protected boolean shown;

    @Override
    public void show(FragmentManager manager, String tag) {
        if (shown) return;
        super.show(manager, tag);
        shown = true;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        shown = false;
        super.onDismiss(dialog);
    }

    @SuppressWarnings("unused")
    public boolean isShown() {
        return shown;
    }
}
