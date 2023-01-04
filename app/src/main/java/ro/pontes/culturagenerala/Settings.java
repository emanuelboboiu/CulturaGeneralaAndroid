package ro.pontes.culturagenerala;

import android.content.Context;
import android.content.SharedPreferences;

/*
 * Class started on Tuesday, 20 June 2017, created by Emanuel Boboiu.
 * This class contains useful methods like save or get settings.
 * */

public class Settings {

    // The file name for save and load preferences:
    private final static String PREFS_NAME = "cgSettings";

    private final Context context;

    // The constructor:
    public Settings(Context context) {
        this.context = context;
    } // end constructor.

    // A method to detect if a preference exist or not:
    public boolean preferenceExists(String key) {
        // Restore preferences
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.contains(key);
    } // end detect if a preference exists or not.

    // Methods for save and read preferences with SharedPreferences:
    // Save a boolean value:
    public void saveBooleanSettings(String key, boolean value) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        // Commit the edits!
        editor.apply();
    } // end save boolean.

    // Read boolean preference:
    public boolean getBooleanSettings(String key) {
        boolean value;
        // Restore preferences:
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        value = settings.getBoolean(key, false);
        return value;
    } // end get boolean preference from SharedPreference.

    // Save a integer value:
    public void saveIntSettings(String key, int value) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        // Commit the edits!
        editor.apply();
    } // end save integer.

    // Read integer preference:
    public int getIntSettings(String key) {
        int value;
        // Restore preferences
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        value = settings.getInt(key, 0);
        return value;
    } // end get integer preference from SharedPreference.

    // Save a String value:
    public void saveStringSettings(String key, String value) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        // Commit the edits!
        editor.apply();
    } // end save String.

    // Read String preference:
    public String getStringSettings(String key) {
        String value;
        // Restore preferences
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        value = settings.getString(key, null);
        return value;
    } // end get String preference from SharedPreference.
    // End read and write settings in SharedPreferences.

    // Charge Settings function:
    public void chargeSettings() {
        // Determine if is first launch of the program:
        boolean isNotFirstRunning = getBooleanSettings("isFirstRunning");

        if (!isNotFirstRunning) {
            saveBooleanSettings("isFirstRunning", true);
            // Make default values in SharedPreferences:
            setDefaultSettings();
        }

        // Now charge settings:

        MainActivity.isPremium = getBooleanSettings("isPremium");

        MainActivity.isStarted = getBooleanSettings("isStarted");

        // Play or not the sounds and speech:
        MainActivity.isSound = getBooleanSettings("isSound");
        MainActivity.isMusic = getBooleanSettings("isMusic");

        // Wake lock, keep screen awake:
        MainActivity.isWakeLock = getBooleanSettings("isWakeLock");

        /* About number of launches, useful for information, rate and others: */
        // Get current number of launches:
        MainActivity.numberOfLaunches = getIntSettings("numberOfLaunches");
        // Increase it by one:
        MainActivity.numberOfLaunches++;
        // Save the new number of launches:
        saveIntSettings("numberOfLaunches", MainActivity.numberOfLaunches);

        // Generate the randomId if it not exists:
        if (preferenceExists("randomId")) {
            MainActivity.randomId = getIntSettings("randomId");
        } else { // if randomId doesn't exist:
            MainActivity.randomId = GUITools.random(1, 2147000000);
            saveIntSettings("randomId", MainActivity.randomId);
        } // end if randomId doesn't exist.
    } // end charge settings.

    public void setDefaultSettings() {

        saveBooleanSettings("isPremium", false);
        MainActivity.isPremium = false;

        saveBooleanSettings("isStarted", false);
        MainActivity.isStarted = false;

        saveBooleanSettings("isSound", true);
        saveBooleanSettings("isMusic", false);

        // For keeping screen awake:
        saveBooleanSettings("isWakeLock", true);

        // Save DataBases version to 0:
        saveIntSettings("dbVer", 0);

        // The default set is 1:
        saveStringSettings("curSetIds", "1|24");
    } // end setDefaultSettings function.

} // end Settings Class.
