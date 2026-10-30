package com.lift.shared.lift.settings.properties;

import android.content.Context;

import androidx.annotation.NonNull;

import com.lift.shared.lift.LiftConstants;

public class LiftAppSharedProperties extends LiftSharedProperties {

    private static LiftAppSharedProperties properties;


    private LiftAppSharedProperties(@NonNull Context context) {
        super(context, LiftConstants.LIFT_APP_NAME,
            LiftConstants.LIFT_PROPERTIES_FILE_PATHS_LIST, LiftPropertyConstants.LIFT_APP_PROPERTIES_LIST,
            new LiftSharedProperties.SharedPropertiesParserClient());
    }

    /**
     * Initialize the {@link #properties} and load properties from disk.
     *
     * @param context The {@link Context} for operations.
     * @return Returns the {@link LiftAppSharedProperties}.
     */
    public static LiftAppSharedProperties init(@NonNull Context context) {
        if (properties == null)
            properties = new LiftAppSharedProperties(context);

        return properties;
    }

    /**
     * Get the {@link #properties}.
     *
     * @return Returns the {@link LiftAppSharedProperties}.
     */
    public static LiftAppSharedProperties getProperties() {
        return properties;
    }

}
