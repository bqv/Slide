package me.ccrama.redditslide;

import java.util.Locale;

import ltd.ucode.slide.App;
import ltd.ucode.slide.SettingValues;

public class UserTags {


    /**
     * Gets the tag for a specific username.
     *
     * @param username The username to find the tag of
     * @return String for the tag
     */
    public static String getUserTag(String username) {
        return SettingValues.INSTANCE.getTags().getString("user-tag" + username.toLowerCase(Locale.ENGLISH), "");
    }

    /**
     * Gets whether a username is tagged.
     *
     * @param username The username to find the tag of
     * @return Boolean if username is tagged
     */
    public static boolean isUserTagged(String username) {
        return SettingValues.INSTANCE.getTags().contains("user-tag" + username.toLowerCase(Locale.ENGLISH));
    }


    public static void setUserTag(final String username, String tag) {
        SettingValues.INSTANCE.getTags().edit().putString("user-tag" + username.toLowerCase(Locale.ENGLISH), tag).apply();
    }

    public static void removeUserTag(final String username) {
        SettingValues.INSTANCE.getTags().edit().remove("user-tag" + username.toLowerCase(Locale.ENGLISH)).apply();
    }
}
