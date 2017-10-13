package com.example.vahanl.onmyway;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by vahanl on 10/3/17.
 */

public class SharedPrefHelper {

    public static void saveUserType(Activity activity, String userType) {
        SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(activity.getString(R.string.user_type), userType);
        editor.commit();
    }

    public static String getUserType(Activity activity) {
        SharedPreferences preferences = activity.getPreferences(Context.MODE_PRIVATE);
        String userType = preferences.getString(activity.getString(R.string.user_type), Constants.TYPE_FOOTER);
        return userType;
    }
}
