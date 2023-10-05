package ro.pontes.culturagenerala;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.Timer;
import java.util.TimerTask;

public class QuizActivity extends Activity {

    // Creating object of AdView:
    private AdView bannerAdView;

    // Class fields:
    private Quiz quiz = null;
    private Timer t = null;

    // For music background:
    private SoundPlayer sndMusic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GUITools.setLocale(this, 2); // Romanian.

        if (MainActivity.isTV) {
            setContentView(R.layout.activity_quiz_tv);
        } else {
            setContentView(R.layout.activity_quiz);
        }

        // Determine the screen orientation:
        MainActivity.isPortrait = GUITools.isPortraitOrientation(this);

        // To keep screen awake:
        if (MainActivity.isWakeLock) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } // end wake lock.

        quiz = new Quiz(this, this);
        startOrContinueQuiz();

        // Start the AdMob only if it is not TV and not premium:
        if (!MainActivity.isTV && !MainActivity.isPremium) {
            // Initializing the AdView object
            bannerAdView = findViewById(R.id.bannerAdView);
            adMobSequence();
        } // end if it is not TV and not Premium.
        // If it is premium and not TV, we hide the ad zone to have more space, for TV we have another XML layout:
        if (MainActivity.isPremium && !MainActivity.isTV) {
            hideAds();
        } // end if it is premium and not TV, hide the ads zone for more space.
    }// end onCreate() method.

    @Override
    public void onResume() {
        super.onResume();
        GUITools.setVolumeForBackground(this);
        // Generate a new track in background music:
        sndMusic = new SoundPlayer();
        sndMusic.playMusic(this);
    } // end onResume method.

    @Override
    public void onPause() {
        // Add here what you want to happens on pause:
        quiz.stopTTS();
        quiz.saveLastGame();
        sndMusic.stopLooped();
        super.onPause();
    } // end onPause method.

    /*
     * To know when focus is gained or lost, to pause and resume the
     * chronometer:
     */
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Start the timer:
            setTheTimer();
        } else { // focus lost:
            t.cancel();
            t = null;
        } // end focus lost.
    }// end onWindowFocusChanged() method.

    @Override
    public void onBackPressed() {
        GUITools.goToMainActivity(this);
        this.finish();
    } // end onBackPressed()

    private void startOrContinueQuiz() {
        quiz.startOrContinueGame();
    } // end startQuiz() method.

    // The method to generate the AdMob sequence:
    private void adMobSequence() {
        //initializing the Google Admob SDK
        MobileAds.initialize(this, initializationStatus -> {
            // Now, because it is initialized, we load the ad:
            loadBannerAd();
        });
    } // end adMobSequence().

    // Now we will create a simple method to load the Banner Ad inside QuizActivity class as shown below:
    private void loadBannerAd() {
        // Creating  a Ad Request
        AdRequest adRequest = new AdRequest.Builder().build();
        // load Ad with the Request
        bannerAdView.loadAd(adRequest);
    } // end loadBannerAd() method.

    // A method to hide the linear layout that includes Google Ads:
    private void hideAds() {
        LinearLayout llBottomAd = findViewById(R.id.llBottomAd);
        llBottomAd.setVisibility(View.GONE);
    } // end hideAds() method.

    // Methods for all four variants:
    public void variant1(View view) {
        variantClicked(0);
    } // end variant1() method.

    public void variant2(View view) {
        variantClicked(1);
    } // end variant2() method.

    public void variant3(View view) {
        variantClicked(2);
    } // end variant3() method.

    public void variant4(View view) {
        variantClicked(3);
    } // end variant4() method.

    // The method which calculates the option chosen:
    public void variantClicked(int var) {
        quiz.confirmAnswer(var);
    }// end variantClicked() method.

    // Methods for confirmation:
    public void yesPressedForFinalAnswer(View view) {
        quiz.checkIfIsCorrectFinalAnswer();
    } // end yesPressedForFinalAnswer() method.

    public void noPressedForFinalAnswer(View view) {
        quiz.backToQuestion();
    } // end yesPressedForFinalAnswer() method.

    // Methods for buttons in the lower part:
    public void abandon(View view) {
        helpOptionClicked(4, view);
    } // end abandon.

    public void fifty(View view) {
        helpOptionClicked(0, view);
    } // end fifty() method.

    public void call(View view) {
        helpOptionClicked(1, view);
    } // end call() method.

    public void ask(View view) {
        helpOptionClicked(2, view);
    } // end ask() method.

    public void change(View view) {
        helpOptionClicked(3, view);
    } // end change() method.
    // end methods for buttons in the lower part.

    private void helpOptionClicked(int option, View v) {
        if (v.isEnabled()) {
            quiz.showUseHelpConfirmation(option);
        } // end if the button is not enabled.
    } // end helpOptionClicked() method.

    // A method which confirms the use of a help option:
    public void yesPressedForUseHelp(View view) {
        quiz.useHelpOptionEffectively();
    } // end yesPressedForUseHelp() method.

    // A method which informs that helping option is not used:
    public void noPressedForUseHelp(View view) {
        quiz.backToQuestion();
    } // end noPressedForUseHelp() method.

    /*
     * A method called when dismissing the results after using a help option
     * button:
     */
    public void dismissResultsAfterUseHelpOption(View view) {
        SoundPlayer.playSimple(this, "an_action");
        quiz.showHelperButtons();
    } // end dismissResultsAfterUseHelpOption() method.

    // A method which is called at timer pressed:
    public void chronPressed(View view) {
        quiz.showFeedbackAlert();
    } // end chronPressed() method.

    // A method to set the timer, in startOrContinue() method:
    public void setTheTimer() {

        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(() -> {
                    // Here what happens.
                    quiz.timerEvent();
                });
            }
        }, 1000, 1000);
    } // end setTheTimer method.

} // end QuizActivity class.
