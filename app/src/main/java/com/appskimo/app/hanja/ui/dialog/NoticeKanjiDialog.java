package com.appskimo.app.hanja.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;

import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.service.PrefsService_;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.sharedpreferences.Pref;

import androidx.appcompat.app.AlertDialog;

@EFragment
public class NoticeKanjiDialog extends CommonDialog {
    public static final String TAG = "NoticeKanjiDialog";

    @Pref PrefsService_ prefs;

    private DialogInterface.OnClickListener confirmListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            prefs.checkedKanjiNotice().put(true);
            dismiss();
        }
    };

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.dialog_notice_kanji, null));
        builder.setPositiveButton(R.string.label_i_checked, confirmListener);
        builder.setTitle(R.string.label_noticed);
        return builder.create();
    }

}