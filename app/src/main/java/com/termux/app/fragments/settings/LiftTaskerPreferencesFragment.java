package com.lift.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.lift.R;
import com.lift.shared.lift.settings.preferences.LiftTaskerAppSharedPreferences;

@Keep
public class LiftTaskerPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(LiftTaskerPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.termux_tasker_preferences, rootKey);
    }

}

class LiftTaskerPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final LiftTaskerAppSharedPreferences mPreferences;

    private static LiftTaskerPreferencesDataStore mInstance;

    private LiftTaskerPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = LiftTaskerAppSharedPreferences.build(context, true);
    }

    public static synchronized LiftTaskerPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new LiftTaskerPreferencesDataStore(context);
        }
        return mInstance;
    }

}
