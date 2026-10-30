package com.lift.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.lift.R;
import com.lift.shared.lift.settings.preferences.LiftFloatAppSharedPreferences;

@Keep
public class LiftFloatPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(LiftFloatPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_float_preferences, rootKey);
    }

}

class LiftFloatPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final LiftFloatAppSharedPreferences mPreferences;

    private static LiftFloatPreferencesDataStore mInstance;

    private LiftFloatPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = LiftFloatAppSharedPreferences.build(context, true);
    }

    public static synchronized LiftFloatPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new LiftFloatPreferencesDataStore(context);
        }
        return mInstance;
    }

}
