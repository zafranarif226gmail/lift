package com.lift.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.lift.R;
import com.lift.shared.lift.settings.preferences.LiftAPIAppSharedPreferences;

@Keep
public class LiftAPIPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(LiftAPIPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_api_preferences, rootKey);
    }

}

class LiftAPIPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final LiftAPIAppSharedPreferences mPreferences;

    private static LiftAPIPreferencesDataStore mInstance;

    private LiftAPIPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = LiftAPIAppSharedPreferences.build(context, true);
    }

    public static synchronized LiftAPIPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new LiftAPIPreferencesDataStore(context);
        }
        return mInstance;
    }

}
