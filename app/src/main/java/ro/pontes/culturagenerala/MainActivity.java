package ro.pontes.culturagenerala;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    public static int curVer = 570;
    public static boolean isFirstLaunchInSession = true;
    public static int randomId = 0;
    public static boolean isPortrait = true;
    public static boolean isTV = false;
    public static boolean isSound = true;
    public static boolean isMusic = true;
    public static int soundBackgroundPercentage = 75;
    public static int soundMusicPercentage = 75;
    public static boolean isWakeLock = true;
    public static int numberOfLaunches = 0;
    public static boolean isStarted = false;
    public static int textSize = 20;
    public static int textHeight = 100; // we need it in sets management to
    // resize a background.

    // For GUI:
    private LinearLayout llMainMenu = null;
    private TextView[] tvMenuItems;

    // Resized Bitmap for menu item background:
    private Bitmap menuItemBackground = null;

    private final Context mContext = this;

    // For background sound:
    private SoundPlayer sndBackground;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Charge settings:
        Settings set = new Settings(this);
        set.chargeSettings();
        GUITools.setLocale(this, 2);

        // Determine if it's a TV or not:
        if (GUITools.isAndroidTV(this)) {
            MainActivity.isTV = true;
        } // end determine if it is TV.

        // Determine the orientation:
        MainActivity.isPortrait = GUITools.isPortraitOrientation(this);

        setContentView(R.layout.activity_main);

        getTextViews();
        setFirstThings();

        if (!isTV) {
            GUITools.checkIfRated(this);
        } // end if is TV.

        /*
         * We show what's new only if is first start of the main menu in current
         * session, no from a finished game:
         */
        if (isFirstLaunchInSession) {
            showWhatsNew();
            isFirstLaunchInSession = false;
        } // end if it is first launch of this in session.

        // To post not posted tests from local to on-line DB:
        Statistics stats = new Statistics(this);
        stats.postOnlineNotPostedFinishedTests();

        /*
         * For first time we want to initialise the main DB to make some things
         * here where is a little easier for CPU:
         */
        DBAdapter mDbHelper;
        mDbHelper = new DBAdapter(this);
        mDbHelper.createDatabase();
        mDbHelper.open();
    } // end onCreate method.

    @Override
    public void onResume() {
        super.onResume();
        GUITools.setVolumeForBackground(this);
        sndBackground = new SoundPlayer();
        sndBackground.playLooped(this, "main_background");
    } // end onResume method.

    @Override
    public void onPause() {
        super.onPause();
        if (sndBackground != null) {
            sndBackground.stopLooped();
        }
    } // end onPause method.

    // A method to take the menu item TVs into an array:
    private void getTextViews() {
        tvMenuItems = new TextView[6];
        for (int i = 0; i < tvMenuItems.length; i++) {
            int resID = mContext.getResources().getIdentifier(
                    "tvMenuItem" + (i + 1), "id", getPackageName());
            tvMenuItems[i] = findViewById(resID);
        } // end for.
    } // end getTextViews() method.

    // A method to set some initial things, the backgrounds:
    private void setFirstThings() {
        // Get the linear layout with the menu items:
        llMainMenu = findViewById(R.id.llMainMenu);

        // Resize the item menu background:
        llMainMenu.getViewTreeObserver().addOnGlobalLayoutListener(
                () -> {

                    /*
                     * Resize the button background for menu items to be for
                     * a specific screen size.
                     */
                    if (menuItemBackground == null) {
                        int height = tvMenuItems[0].getHeight();
                        textHeight = height; // textHeight is a public
                        // static variable.
                        height = height * 2;
                        /*
                         * We search for the longest menu item by text and
                         * we make the buttons according to that item:
                         */
                        int width = tvMenuItems[0].getWidth();
                        for (int i = 1; i < tvMenuItems.length; i++) {
                            if (tvMenuItems[i].getWidth() > width) {
                                width = tvMenuItems[i].getWidth();
                            }
                        } // end for.

                        /*
                         * We have now the longest text view in main menu.
                         * Now, we get the width of the layout with matches
                         * parent. If 75% from layout width is greater than
                         * the largest text view, we make the background
                         * 75%, otherwise we make it like the longest text
                         * view.
                         */
                        int llWidth = llMainMenu.getWidth();
                        // If is TV we make it 65 %:
                        if (MainActivity.isTV) {
                            llWidth = llWidth * 65 / 100;
                        } else if (MainActivity.isPortrait) { // 75%:
                            llWidth = llWidth * 75 / 100;
                        } else { // it is simple landscape:
                            llWidth = llWidth * 55 / 100;
                        } // end if it is landscape, not TV or portrait.

                        if (llWidth > width) {
                            width = llWidth;
                        }

                        menuItemBackground = GUITools.resizeImage(mContext,
                                "button_main_menu", width, height);

                        // Change now the background for each item:
                        for (TextView tvMenuItem : tvMenuItems) {
                            tvMenuItem
                                    .setBackground(new BitmapDrawable(
                                            getResources(),
                                            menuItemBackground));
                        } // end for set background for all items.
                    } // end if menuItemBackground wasn't created..

                    // End resize Bitmaps for backgrounds.
                });
        // End resize the backgrounds for different controls.

        // Write start or continue on the first item in menu:
        // If is started we must write continue there:
        if (isStarted) {
            tvMenuItems[0].setText(R.string.bt_continue_quiz);
        }
    } // end setFirstThings() method.

    // A method to go to quiz activity:
    public void startQuiz(View view) {
        GUITools.goToQuizActivity(this);
        this.finish();
    } // end startQuiz() method.

    // A method to go to sets management alert:
    public void goToStatistics(View view) {
        Statistics stats = new Statistics(this);
        stats.showStats();
    } // end goToStatistics() method.

    // A method to go to sets management alert:
    public void goToSetsManagement(View view) {
        // Only if the game is not started:
        if (!MainActivity.isStarted) {
            SetsManagement sm = new SetsManagement(this);
            sm.showSets();
        } else {
            // Get the NRQ to show the warning message more accurately:
            Settings set = new Settings(this);
            int nrq = set.getIntSettings("lastCurQuestionNumber");
            GUITools.alert(this, getString(R.string.warning), String.format(
                    getString(R.string.warning_choose_not_available), "" + nrq));
        }
    } // end goToSetsManagement() method.

    // A method to go to settings activity:
    public void settings(View view) {
        SettingsManagement sm = new SettingsManagement(this);
        sm.showSettingsAlert();
    } // end settings() method.

    public void showAbout(View view) {
        GUITools.aboutDialog(this);
    } // end showAbout() method.

    // A method to go to rate the application:
    public void rateTheApp(View view) {
        GUITools.showRateDialog(mContext);
    } // end rateTheApp() method.

    // Copied from SettingsActivity class:
    // Let's see what happens when a check box is clicked in settings alert:
    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        Settings set = new Settings(this); // to save changes.

        // Check which check box was clicked
        switch (view.getId()) {
            case R.id.cbtSoundsSetting:
                MainActivity.isSound = checked;
                set.saveBooleanSettings("isSound", MainActivity.isSound);
                break;

            case R.id.cbtMusicSetting:
                if (checked) {
                    MainActivity.isMusic = true;
                    GUITools.setVolumeForBackground(this);
                    sndBackground = new SoundPlayer();
                    sndBackground.playLooped(this, "main_background");
                } else {
                    MainActivity.isMusic = false;
                    sndBackground.stopLooped();
                    sndBackground = null;
                }
                set.saveBooleanSettings("isMusic", MainActivity.isMusic);
                break;

            case R.id.cbtScreenAwakeSetting:
                MainActivity.isWakeLock = checked;
                set.saveBooleanSettings("isWakeLock", MainActivity.isWakeLock);
                break;

            case R.id.cbtGotIt:
                set.saveBooleanSettings("wasAnnounced" + curVer, checked);
                break;
        } // end switch.
        SoundPlayer.playSimple(mContext, "element_clicked");
    } // end onClick method.

    // The method to reset to defaults:
    public void resetToDefaults(View view) {
        // Make an alert with the question:
        // Get the strings to make an alert:
        String tempTitle = getString(R.string.title_default_settings);
        String tempBody = getString(R.string.body_default_settings);
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(tempTitle);
        ScrollView sv = new ScrollView(this);
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);

        TextView tv = new TextView(this);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
        tv.setText(tempBody);
        ll.addView(tv);

        // Add now the LinearLayout into ScrollView:
        sv.addView(ll);

        alert.setView(sv);

        alert.setIcon(R.drawable.ic_launcher)
                .setPositiveButton(R.string.yes,
                        (dialog, whichButton) -> {
                            Settings set = new Settings(mContext);
                            set.setDefaultSettings();
                            set.chargeSettings();
                            // recreateThisActivity();
                        }).setNegativeButton(R.string.no, null).show();
    } // end resetToDefaults() method.

    // A method to show the help:
    public void showHelp(View view) {
        GUITools.showHelp(this);
    } // end showHelp() method.

    // A method to show alert for what's new:
    @SuppressLint("InflateParams")
    private void showWhatsNew() {
        Settings set = new Settings(this);
        boolean wasAnnounced = set.getBooleanSettings("wasAnnounced" + curVer);
        /*
         * Only if it was not set not to be announced anymore, wasAnnounced
         * true:
         */
        if (!wasAnnounced) {
            // create an alert inflating an XML:
            LayoutInflater inflater = (LayoutInflater) this
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View newView = inflater.inflate(R.layout.whatsnew_dialog, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.drawable.ic_launcher);
            builder.setTitle(R.string.whatsnew_title);
            builder.setView(newView);
            builder.setPositiveButton(R.string.close,
                    (dialog, whichButton) -> SoundPlayer
                            .playSimple(mContext, "element_finished"));
            builder.create();
            builder.show();
        } // end if it was not announced.
    } // end showWhatsNew() method.

} // end MainActivity class.
