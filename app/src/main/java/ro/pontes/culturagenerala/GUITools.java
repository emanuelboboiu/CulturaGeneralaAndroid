package ro.pontes.culturagenerala;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

/*
 * Started by Manu on Sunday, 24 September 2017, 00:16.
 */

public class GUITools {

    // A method to go to MainActivity:
    public static void goToMainActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    } // end goToMainActivity() method.

    // A method to go to QuizActivity:
    public static void goToQuizActivity(Context context) {
        Intent intent = new Intent(context, QuizActivity.class);
        context.startActivity(intent);
    } // end goToQuizActivity() method.

    // A method to show an alert with title and message, just an OK button:
    public static void alert(Context mContext, String title, String message) {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        // The title:
        alert.setTitle(title);
        // The body creation:
        // Create a LinearLayout with ScrollView with all contents as TextViews:
        ScrollView sv = new ScrollView(mContext);
        LinearLayout ll = new LinearLayout(mContext);
        ll.setOrientation(LinearLayout.VERTICAL);
        int mPadding = GUITools.dpToPx(mContext, 10);
        if (MainActivity.isTV) {
            mPadding = GUITools.dpToPx(mContext, 100);
        }
        ll.setPadding(mPadding, mPadding / 2, mPadding, mPadding / 2);

        String[] mParagraphs = message.split("\n");
        // A for for each paragraph in the message as TextView:
        for (String mParagraph : mParagraphs) {
            TextView tv = new TextView(mContext);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            tv.setText(mParagraph);
            ll.addView(tv);
        } // end for.

        // Add now the LinearLayout into ScrollView:
        sv.addView(ll);

        alert.setView(sv);

        alert.setPositiveButton(mContext.getString(R.string.ok), (dialog, whichButton) -> {
            // Do nothing yet...
        });
        alert.show();
    } // end alert static method.

    // A method to show help in an alert LinearLayout:
    public static void showHelp(Context mContext) {
        // Create a LinearLayout with ScrollView with all contents as TextViews:
        ScrollView sv = new ScrollView(mContext);
        LinearLayout ll = new LinearLayout(mContext);
        ll.setOrientation(LinearLayout.VERTICAL);
        int paddingDP = 16;
        // We make it more if it is android TV:
        if (MainActivity.isTV) {
            paddingDP = paddingDP * 8;
        }
        int paddingPX = GUITools.dpToPx(mContext, paddingDP);
        ll.setPadding(paddingPX, 0, paddingPX, 0);

        // Get the items to be shown in alert:
        Resources res = mContext.getResources();
        String[] aInformation = res.getStringArray(R.array.information_array);

        // A for for each message in the history as TextView:
        for (String s : aInformation) {
            TextView tv = new TextView(mContext);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            tv.setText(MyHtml.fromHtml(s));
            ll.addView(tv);
        } // end for.
        sv.addView(ll);

        // Create now the alert:
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setTitle(mContext.getString(R.string.help_alert_title));
        alertDialog.setView(sv);
        alertDialog.setPositiveButton(mContext.getString(R.string.close_help), null);
        AlertDialog alert = alertDialog.create();
        alert.show();
    } // end showHelp() method.

    // A method for about dialog for this package:
    public static void aboutDialog(Context context) {
        // Inflate the about message contents
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View messageView = inflater.inflate(R.layout.about_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.about_title);
        builder.setView(messageView);
        builder.setPositiveButton(context.getString(R.string.close), null);
        builder.create();
        builder.show();

        /*
         * Let's try to find the link and hide it it it is Android TV. It is not
         * allowed a link on Android TV:
         */
        if (MainActivity.isTV) {
            TextView tvWebAddress = messageView.findViewById(R.id.tvWebAddress);
            tvWebAddress.setVisibility(View.GONE);
        } // end if it is Android TV.
    } // end about dialog.

    // A method which opens a browser and a URL:
    public static void openBrowser(final Context context, String url) {
        String HTTPS = "https://";
        String HTTP = "http://";

        if (!url.startsWith(HTTP) && !url.startsWith(HTTPS)) {
            url = HTTP + url;
        }

        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        context.startActivity(browserIntent);
    } // end start browser with an URL in it.

    // A method to rate this application:
    public static void showRateDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setIcon(R.drawable.ic_launcher).setTitle(context.getString(R.string.title_rate_app));

        ScrollView sv = new ScrollView(context);
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        int mPadding = GUITools.dpToPx(context, 10);
        if (MainActivity.isTV) {
            mPadding = GUITools.dpToPx(context, 100);
        }
        ll.setPadding(mPadding, 5, mPadding, 5);

        TextView tv = new TextView(context);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
        tv.setText(context.getString(R.string.body_rate_app));
        ll.addView(tv);

        // Add now the LinearLayout into ScrollView:
        sv.addView(ll);

        builder.setView(sv);
        builder.setPositiveButton(context.getString(R.string.bt_rate), (dialog, which) -> {
            Settings set = new Settings(context);
            set.saveBooleanSettings("wasRated", true);
            String link = "market://details?id=";
            try {
                // play market available
                context.getPackageManager().getPackageInfo("com.android.vending", 0);
                // not available
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                // Should use browser
                link = "https://play.google.com/store/apps/details?id=";
            }
            // Starts external action
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link + context.getPackageName())));
        }).setNegativeButton(context.getString(R.string.bt_not_now), null);
        builder.show();
    } // end showRateDialog() method.

    // A method which checks if the app was rated:
    public static void checkIfRated(Context context) {
        Settings set = new Settings(context);
        boolean wasRated = set.getBooleanSettings("wasRated");
        if (!wasRated) {

            if (MainActivity.numberOfLaunches % 11 == 0 && MainActivity.numberOfLaunches > 0) {
                GUITools.showRateDialog(context);
            } // end if was x launches.
        } // end if it was not rated.
    } // end checkIfRated() method.

    // A method to detect if Internet connection is available:
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    } // end isNetworkAvailable() method.

    // A method which detects if accessibility is enabled:
    public static boolean isAccessibilityEnabled(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        // boolean isAccessibilityEnabled = am.isEnabled();
        return am.isTouchExplorationEnabled();
    } // end isAccessibilityEnabled() method.

    // A method to give a toast, simple message on the screen:
    public static void toast(String message, int duration, Context context) {
        Toast.makeText(context, message, duration).show();
    } // end make toast.

    // A static method to get a random number between two integers:
    public static int random(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    } // end random method.

    // A method to resize image:
    public static Bitmap resizeImage(final Context context, String imageName, int mWidth, int mHeight) {

        int resId = context.getResources().getIdentifier(imageName, "drawable", context.getPackageName());

        // Get the resized image:
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resId);
        return Bitmap.createScaledBitmap(bmp, mWidth, mHeight, true);
    } // end resizeImage() method.

    // Convert DP to pixels:
    public static int dpToPx(Context mContext, int dp) {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    } // end dpToPx() method.

    // A method which returns true if is portrait, false otherwise:
    public static boolean isPortraitOrientation(Activity activity) {
        // Get the size of the screen:
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        return screenWidth <= screenHeight;
    } // end determine if isPortraitOrientation() method.

    // A method to determine if it is AndroidTV:
    public static boolean isAndroidTV(final Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    } // end determine if isAndroidTV() method.

    /*
     * A method which makes a string delimited by vertical bars from an array, a
     * serialisation:
     */
    public static String arrayToDelimitedString(String[] arrString) {
        // Make a string from the arrString array:
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < arrString.length; i++) {

            temp.append(arrString[i]);
            // If isn't the last item, append also a vertical bar:
            if (i < arrString.length - 1) {
                temp.append("|");
            }
        } // end for.
        return temp.toString();
    } // end arrayToDelimitedString() method.

    // A method to round a double value:
    public static double round(double number, int decimals) {
        double temp = Math.pow(10, decimals);
        return Math.round(number * temp) / temp;
    } // end round() method.

    // A method to get current time in seconds:
    public static long getTimeInSeconds() {
        Calendar cal = Calendar.getInstance();
        long timeInMilliseconds = cal.getTimeInMillis();
        return timeInMilliseconds / 1000;
    } // end getTimeInSeconds() method.

    // For formatting a date:
    public static String timeStampToString(Context context, int paramCurTime) {
        long curTime = (long) paramCurTime * 1000;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(curTime);

        // Now format the string:
        // See if it is today or yesterday:
        int today = getIsToday(curTime);
        String dayOfWeek;
        if (today == 1) {
            dayOfWeek = context.getString(R.string.today);
        } else if (today == 2) {
            dayOfWeek = context.getString(R.string.yesterday);
        } else {
            dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        }

        // Make the hour and minute with 0 in front if they are less
        // than 10:
        String curHour;
        int iHour = cal.get(Calendar.HOUR_OF_DAY);
        if (iHour < 10) {
            curHour = "0" + iHour;
        } else {
            curHour = "" + iHour;
        }
        String curMinute;
        int iMinute = cal.get(Calendar.MINUTE);
        if (iMinute < 10) {
            curMinute = "0" + iMinute;
        } else {
            curMinute = "" + iMinute;
        }

        return String.format(context.getString(R.string.date_format), dayOfWeek, "" + cal.get(Calendar.DAY_OF_MONTH), "" + cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()), "" + cal.get(Calendar.YEAR), curHour, curMinute);
    } // end timeStampToString() method.

    /*
     * This method returns 1 if a date in milliseconds at parameter is today, 2
     * if it was yesterday or 0 on another date.
     */
    public static int getIsToday(long smsTimeInMillis) {
        Calendar smsTime = Calendar.getInstance();
        smsTime.setTimeInMillis(smsTimeInMillis);

        Calendar now = Calendar.getInstance();
        if (now.get(Calendar.YEAR) == smsTime.get(Calendar.YEAR)) {
            if (now.get(Calendar.DAY_OF_YEAR) == smsTime.get(Calendar.DAY_OF_YEAR)) {
                return 1; // today.
            } else if (now.get(Calendar.DAY_OF_YEAR) - smsTime.get(Calendar.DAY_OF_YEAR) == 1) {
                return 2; // yesterday.
            } else if (smsTime.get(Calendar.DAY_OF_YEAR) - now.get(Calendar.DAY_OF_YEAR) == 1) {
                return 3; // tomorrow.
            } else {
                return 0; // another date.
            }
        } // end if it is the same year.
        else { // another year, it is also 0:
            return 0; // another date.
        } // end if it is not the same year.
    } // end determine if a date is today or yesterday.

    /*
     * This method sets the volume for background depending of accessibility
     * enabled or not:
     */
    public static void setVolumeForBackground(Context mContext) {
        if (GUITools.isAccessibilityEnabled(mContext)) {
            MainActivity.soundBackgroundPercentage = 15;
            MainActivity.soundMusicPercentage = 15;
        } else {
            MainActivity.soundBackgroundPercentage = 75;
            MainActivity.soundMusicPercentage = 75;
        } // end if is not accessibility.
    } // end setVolumeForBackground() method.

    // This method sets the locale independent of the OS language, 2 means
    // Romanian:
    public static void setLocale(Activity activity, int numLang) {
        // If it is not the device language, normal change:
        if (numLang > 0) {
            // An array with language codes:
            String[] languages = {"", "en", "ro"};
            String lang = languages[numLang];
            Locale locale = new Locale(lang);
            Locale.setDefault(locale);
            Configuration config = activity.getBaseContext().getResources().getConfiguration();
            config.locale = locale;
            activity.getBaseContext().getResources().updateConfiguration(config, activity.getBaseContext().getResources().getDisplayMetrics());
        } // end if langNumber is not 0, no device language.
    } // end setLocale() method.

} // end GUITools class.
