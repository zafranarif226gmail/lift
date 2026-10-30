package com.lift.shared.lift.theme;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lift.shared.lift.settings.properties.LiftPropertyConstants;
import com.lift.shared.lift.settings.properties.LiftSharedProperties;
import com.lift.shared.theme.NightMode;

public class LiftThemeUtils {

    /** Get the {@link LiftPropertyConstants#KEY_NIGHT_MODE} value from the properties file on disk
     * and set it to app wide night mode value. */
    public static void setAppNightMode(@NonNull Context context) {
        NightMode.setAppNightMode(LiftSharedProperties.getNightMode(context));
    }

    /** Set name as app wide night mode value. */
    public static void setAppNightMode(@Nullable String name) {
        NightMode.setAppNightMode(name);
    }

}
