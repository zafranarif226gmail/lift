package com.lift.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.lift.R;
import com.lift.shared.lift.settings.preferences.LiftAppSharedPreferences;

@Keep
public class LiftPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(LiftPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_preferences, rootKey);
    }

}

class LiftPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final LiftAppSharedPreferences mPreferences;

    private static LiftPreferencesDataStore mInstance;

    private LiftPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = LiftAppSharedPreferences.build(context, true);
    }

    public static synchronized LiftPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new LiftPreferencesDataStore(context);
        }
        return mInstance;
    }

}
