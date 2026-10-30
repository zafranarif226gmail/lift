package com.lift.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.lift.R;
import com.lift.shared.lift.settings.preferences.LiftWidgetAppSharedPreferences;

@Keep
public class LiftWidgetPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(LiftWidgetPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_widget_preferences, rootKey);
    }

}

class LiftWidgetPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final LiftWidgetAppSharedPreferences mPreferences;

    private static LiftWidgetPreferencesDataStore mInstance;

    private LiftWidgetPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = LiftWidgetAppSharedPreferences.build(context, true);
    }

    public static synchronized LiftWidgetPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new LiftWidgetPreferencesDataStore(context);
        }
        return mInstance;
    }

}
