package com.appskimo.app.hanja.ui.frags;

import android.Manifest;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.appskimo.app.hanja.Constants;
import com.appskimo.app.hanja.R;
import com.appskimo.app.hanja.event.UnconsciousTheme;
import com.appskimo.app.hanja.service.MiscService;
import com.appskimo.app.hanja.service.PrefsService_;
import com.appskimo.app.hanja.service.VocabService;
import com.appskimo.app.hanja.support.PermissionChecker;
import com.appskimo.app.hanja.support.ScreenOffService_;
import com.appskimo.app.hanja.support.UnconsciousService_;

import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.CheckedChange;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.SeekBarProgressChange;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

@EFragment(R.layout.fragment_lock_settings)
public class LockSettingsFragment extends Fragment implements EasyPermissions.PermissionCallbacks {
    @ViewById(R.id.lockscreenUse) Switch lockscreenUse;
    @ViewById(R.id.switchLayer) LinearLayout switchLayer;
    @ViewById(R.id.optionAll) RadioButton optionAll;
    @ViewById(R.id.optionLearning) RadioButton optionLearning;
    @ViewById(R.id.optionMastered) RadioButton optionMastered;
    @ViewById(R.id.optionChecked) RadioButton optionChecked;
    @ViewById(R.id.clockUse) Switch clockUse;
    @ViewById(R.id.interval) SeekBar interval;
    @ViewById(R.id.intervalValue) TextView intervalValue;
    @ViewById(R.id.unconsciousUse) Switch unconsciousUse;
    @ViewById(R.id.themeGrey) RadioButton themeGrey;
    @ViewById(R.id.themeBlue) RadioButton themeBlue;
    @ViewById(R.id.themeGreen) RadioButton themeGreen;
    @ViewById(R.id.themePink) RadioButton themePink;
    @ViewById(R.id.themePurple) RadioButton themePurple;

    @Bean VocabService vocabService;
    @Bean MiscService miscService;
    @Pref PrefsService_ prefs;

    private PermissionChecker permissionChecker;
    private List<Switch> switchList = new ArrayList<>();

    @Override
    public void onResume() {
        super.onResume();
        init();
    }

    @AfterInject
    void afterInject() {
        miscService.applyFontScale(getActivity());
        permissionChecker = new PermissionChecker(getActivity());
    }

    private void init() {
        lockscreenUse.setChecked(prefs.useLockScreen().get());

        var params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.RIGHT;

        final var checkedCategories = prefs.checkedCategories().get();
        CompoundButton.OnCheckedChangeListener listener = (button, isChecked) -> {
            String uid = (String) button.getTag();
            if (isChecked) {
                checkedCategories.add(uid);
            } else {
                checkedCategories.remove(uid);
            }
            prefs.checkedCategories().put(checkedCategories);
            prefs.categoryWordUid().put(0);

            prefs.collectionType().put(0);
            optionAll.setChecked(true);
            optionLearning.setChecked(false);
            optionMastered.setChecked(false);
            optionChecked.setChecked(false);
        };

        var offText = getResources().getString(R.string.label_unselect);
        var onText = getResources().getString(R.string.label_select);
        var categories = vocabService.getCategories();

        switchLayer.removeAllViews();
        for (int i = 0; i < categories.size() - 1; i++) {
            var category = categories.get(i);
            var uid = String.valueOf(category.getCategoryUid());

            var sw = new Switch(getActivity());
            sw.setText(category.getName());
            sw.setTextOff(offText);
            sw.setTextOn(onText);
            sw.setTag(uid);
            sw.setChecked(checkedCategories.contains(uid));
            sw.setOnCheckedChangeListener(listener);
            sw.setSwitchPadding(lockscreenUse.getSwitchPadding());

            switchLayer.addView(sw, params);
            switchList.add(sw);
        }

        optionAll.setChecked(prefs.collectionType().get() == 0);
        optionLearning.setChecked(prefs.collectionType().get() == 1);
        optionMastered.setChecked(prefs.collectionType().get() == 2);
        optionChecked.setChecked(prefs.collectionType().get() == 3);

        clockUse.setChecked(prefs.useClock().get());

        unconsciousUse.setChecked(prefs.useUnconscious().get());

        int interVal = prefs.interval().get();
        intervalValue.setText(String.valueOf(interVal));
        interval.setProgress(interVal / intervalFactor);

        themeBlue.setChecked(prefs.bgResId().getOr(R.drawable.unconscious_grey) == R.drawable.unconscious_blue);
        themeGreen.setChecked(prefs.bgResId().getOr(R.drawable.unconscious_grey) == R.drawable.unconscious_green);
        themeGrey.setChecked(prefs.bgResId().getOr(R.drawable.unconscious_grey) == R.drawable.unconscious_grey);
        themePink.setChecked(prefs.bgResId().getOr(R.drawable.unconscious_grey) == R.drawable.unconscious_pink);
        themePurple.setChecked(prefs.bgResId().getOr(R.drawable.unconscious_grey) == R.drawable.unconscious_purple);
    }

    private int intervalFactor = 3;

    @CheckedChange(R.id.lockscreenUse)
    void onCheckedSwitch(CompoundButton button, boolean isChecked) {
        if (hasPermissions()) {
            if (prefs.useLockScreen().get() != isChecked) {
                prefs.useLockScreen().put(isChecked);
                if (isChecked) {
                    ScreenOffService_.start(getActivity());
                } else {
                    ScreenOffService_.intent(getActivity()).stop();
                }
            }
        } else {
            if (isChecked) {
                lockscreenUse.setChecked(false);
                requestPermissions();
            }
        }
    }

    @Click({R.id.optionAll, R.id.optionLearning, R.id.optionMastered, R.id.optionChecked})
    void onClickOptions(View view) {
        if (view.getId() == R.id.optionAll) {
            prefs.collectionType().put(0);
            checkOptions();
            return;
        }

        int afterCollectionTypeValue = 1;
        var afterCollectionType = Constants.CollectionType.LEARNING;
        if (view.getId() == R.id.optionChecked) {
            afterCollectionTypeValue = 3;
            afterCollectionType = Constants.CollectionType.CHECKED;
        } else if (view.getId() == R.id.optionMastered) {
            afterCollectionTypeValue = 2;
            afterCollectionType = Constants.CollectionType.MASTERED;
        } else {
            afterCollectionTypeValue = 1;
            afterCollectionType = Constants.CollectionType.LEARNING;
        }

        long countOf = vocabService.countOf(prefs.checkedCategories().get(), afterCollectionType);
        if (countOf > 0L) {
            prefs.collectionType().put(afterCollectionTypeValue);
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.message_no_words), Toast.LENGTH_LONG).show();
        }

        checkOptions();
    }

    private void checkOptions() {
        int collectionTypeValue = prefs.collectionType().get();
        optionAll.setChecked(collectionTypeValue == 0);
        optionLearning.setChecked(collectionTypeValue == 1);
        optionMastered.setChecked(collectionTypeValue == 2);
        optionChecked.setChecked(collectionTypeValue == 3);
    }

    @CheckedChange(R.id.clockUse)
    void onCheckedClockUse(CompoundButton button, boolean isChecked) {
        if (prefs.useClock().get() != isChecked) {
            prefs.useClock().put(isChecked);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PermissionChecker.REQUIRED_PERMISSION_REQUEST_CODE) {
            if (permissionChecker.isRequiredPermissionGranted()) {
                Toast.makeText(getActivity(), R.string.message_permissions_granted, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), R.string.message_permissions_not_granted, Toast.LENGTH_LONG).show();
            }
        }
    }

    @CheckedChange(R.id.unconsciousUse)
    void onCheckedUnconscious(CompoundButton button, boolean isChecked) {
        if (!permissionChecker.isRequiredPermissionGranted()) {
            button.setChecked(false);

            var intent = permissionChecker.createRequiredPermissionIntent();
            startActivityForResult(intent, PermissionChecker.REQUIRED_PERMISSION_REQUEST_CODE);
        } else {
            if (prefs.useUnconscious().get() != isChecked) {
                prefs.useUnconscious().put(isChecked);
                if(isChecked) {
                    UnconsciousService_.start(getActivity());
                } else {
                    UnconsciousService_.intent(getActivity()).stop();
                }
            }
        }
    }

    @SeekBarProgressChange(R.id.interval)
    void onProgressChangeOnSeekBar(SeekBar seekBar, int progress) {
        if (progress < 1) {
            interval.setProgress(1);
            progress = 1;
        }

        int value = progress * intervalFactor;
        intervalValue.setText(String.valueOf(value));
        prefs.interval().put(value);
    }

    @Click(value = {R.id.themeGrey, R.id.themeBlue, R.id.themeGreen, R.id.themePink, R.id.themePurple})
    void onClickThemes(View view) {
        int resId = R.drawable.unconscious_grey;

        int viewId = view.getId();
        if (viewId == R.id.themeBlue) {
            resId = R.drawable.unconscious_blue;
        } else if (viewId == R.id.themeGreen) {
            resId = R.drawable.unconscious_green;
        } else if (viewId == R.id.themePink) {
            resId = R.drawable.unconscious_pink;
        } else if (viewId == R.id.themePurple) {
            resId = R.drawable.unconscious_purple;
        } else {
            resId = R.drawable.unconscious_grey;
        }

        prefs.bgResId().put(resId);
        EventBus.getDefault().post(new UnconsciousTheme(resId));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    private static final int RC_READ_PHONE_STATE = 5001;
    private static final String[] PERMISSIONS = {Manifest.permission.READ_PHONE_STATE};

    private boolean hasPermissions() {
        return EasyPermissions.hasPermissions(this.getContext(), PERMISSIONS);
    }

    private void requestPermissions() {
        EasyPermissions.requestPermissions(this, getString(R.string.message_permissions_phone), RC_READ_PHONE_STATE, PERMISSIONS);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
    }
}
