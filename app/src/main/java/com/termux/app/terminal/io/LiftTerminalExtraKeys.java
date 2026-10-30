package com.lift.app.terminal.io;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.lift.app.LiftActivity;
import com.lift.app.terminal.LiftTerminalSessionActivityClient;
import com.lift.app.terminal.LiftTerminalViewClient;
import com.lift.shared.logger.Logger;
import com.lift.shared.lift.extrakeys.ExtraKeysConstants;
import com.lift.shared.lift.extrakeys.ExtraKeysInfo;
import com.lift.shared.lift.settings.properties.LiftPropertyConstants;
import com.lift.shared.lift.settings.properties.LiftSharedProperties;
import com.lift.shared.lift.terminal.io.TerminalExtraKeys;
import com.lift.view.TerminalView;

import org.json.JSONException;

public class LiftTerminalExtraKeys extends TerminalExtraKeys {

    private ExtraKeysInfo mExtraKeysInfo;

    final LiftActivity mActivity;
    final LiftTerminalViewClient mLiftTerminalViewClient;
    final LiftTerminalSessionActivityClient mLiftTerminalSessionActivityClient;

    private static final String LOG_TAG = "LiftTerminalExtraKeys";

    public LiftTerminalExtraKeys(LiftActivity activity, @NonNull TerminalView terminalView,
                                   LiftTerminalViewClient liftTerminalViewClient,
                                   LiftTerminalSessionActivityClient liftTerminalSessionActivityClient) {
        super(terminalView);

        mActivity = activity;
        mLiftTerminalViewClient = liftTerminalViewClient;
        mLiftTerminalSessionActivityClient = liftTerminalSessionActivityClient;

        setExtraKeys();
    }


    /**
     * Set the terminal extra keys and style.
     */
    private void setExtraKeys() {
        mExtraKeysInfo = null;

        try {
            // The mMap stores the extra key and style string values while loading properties
            // Check {@link #getExtraKeysInternalPropertyValueFromValue(String)} and
            // {@link #getExtraKeysStyleInternalPropertyValueFromValue(String)}
            String extrakeys = (String) mActivity.getProperties().getInternalPropertyValue(LiftPropertyConstants.KEY_EXTRA_KEYS, true);
            String extraKeysStyle = (String) mActivity.getProperties().getInternalPropertyValue(LiftPropertyConstants.KEY_EXTRA_KEYS_STYLE, true);

            ExtraKeysConstants.ExtraKeyDisplayMap extraKeyDisplayMap = ExtraKeysInfo.getCharDisplayMapForStyle(extraKeysStyle);
            if (ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY.equals(extraKeyDisplayMap) && !LiftPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE.equals(extraKeysStyle)) {
                Logger.logError(LiftSharedProperties.LOG_TAG, "The style \"" + extraKeysStyle + "\" for the key \"" + LiftPropertyConstants.KEY_EXTRA_KEYS_STYLE + "\" is invalid. Using default style instead.");
                extraKeysStyle = LiftPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE;
            }

            mExtraKeysInfo = new ExtraKeysInfo(extrakeys, extraKeysStyle, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        } catch (JSONException e) {
            Logger.showToast(mActivity, "Could not load and set the \"" + LiftPropertyConstants.KEY_EXTRA_KEYS + "\" property from the properties file: " + e.toString(), true);
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not load and set the \"" + LiftPropertyConstants.KEY_EXTRA_KEYS + "\" property from the properties file: ", e);

            try {
                mExtraKeysInfo = new ExtraKeysInfo(LiftPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS, LiftPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
            } catch (JSONException e2) {
                Logger.showToast(mActivity, "Can't create default extra keys",true);
                Logger.logStackTraceWithMessage(LOG_TAG, "Could create default extra keys: ", e);
                mExtraKeysInfo = null;
            }
        }
    }

    public ExtraKeysInfo getExtraKeysInfo() {
        return mExtraKeysInfo;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onTerminalExtraKeyButtonClick(View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean fnDown) {
        if ("KEYBOARD".equals(key)) {
            if(mLiftTerminalViewClient != null)
                mLiftTerminalViewClient.onToggleSoftKeyboardRequest();
        } else if ("DRAWER".equals(key)) {
            DrawerLayout drawerLayout = mLiftTerminalViewClient.getActivity().getDrawer();
            if (drawerLayout.isDrawerOpen(Gravity.LEFT))
                drawerLayout.closeDrawer(Gravity.LEFT);
            else
                drawerLayout.openDrawer(Gravity.LEFT);
        } else if ("PASTE".equals(key)) {
            if(mLiftTerminalSessionActivityClient != null)
                mLiftTerminalSessionActivityClient.onPasteTextFromClipboard(null);
        }  else if ("SCROLL".equals(key)) {
            TerminalView terminalView = mLiftTerminalViewClient.getActivity().getTerminalView();
            if (terminalView != null && terminalView.mEmulator != null)
                terminalView.mEmulator.toggleAutoScrollDisabled();
        } else {
            super.onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown);
        }
    }

}
