package ro.pontes.culturagenerala;

import static com.google.android.gms.common.util.CollectionUtils.listOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.util.List;

public class MainActivity extends Activity {

    public static boolean isPremium = false;
    private final String mProduct = "cg_premium";
    public static String mUpgradePrice = "�";

    private final Context mFinalContext = this;
    public static int curVer = 750;
    public static boolean isFirstLaunchInSession = true;
    public static int randomId = 0;
    public static boolean isPortrait = true;
    public static boolean isTV = false;
    public static boolean isAccessibility = false;
    public static boolean isSpeech = true;
    public static boolean isSound = true;
    public static boolean isMusic = true;
    public static int soundBackgroundPercentage = 75;
    public static int soundMusicPercentage = 75;
    public static boolean isWakeLock = true;
    public static int numberOfLaunches = 0;
    public static boolean isStarted = false;
    public static int textSize = 20;
    public static int textHeight = 100; // we need it in sets management to resize a background.

    // For GUI:
    private LinearLayout llMainMenu = null;
    private TextView[] tvMenuItems;

    // Resized Bitmap for menu item background:
    private Bitmap menuItemBackground = null;

    private final Context mContext = this;

    // For background sound:
    private SoundPlayer sndBackground;

    // For billing:
    private PurchasesUpdatedListener purchasesUpdatedListener;
    private BillingClient billingClient;
    private AcknowledgePurchaseResponseListener acknowledgePurchaseResponseListener;
    List<ProductDetails> myProducts;

    public static int mPaddingDP = 3;

    // For google interstitial ads:
    private InterstitialAd mInterstitialAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Charge settings:
        Settings set = new Settings(this);
        set.chargeSettings();
        GUITools.setLocale(this, 2);

        // Determine if it's a TV or not:
        isTV = GUITools.isAndroidTV(this);
// Determine if it is accessibility enabled:
        isAccessibility = GUITools.isAccessibilityEnabled(this);

        // Determine the orientation:
        MainActivity.isPortrait = GUITools.isPortraitOrientation(this);

        setContentView(R.layout.activity_main);

        getTextViews();
        setFirstThings();

        // Some variables for moment of interstitial and auto unregister:
        int howOftenAd = 3;
        int howOftenUnregister = 24;

        // We don't want to have this announcement about rating together with an interstitial, also when is TV and first launch after a while:
        if (!isTV && !isFirstLaunchInSession && numberOfLaunches % howOftenAd != 0) {
            GUITools.checkIfRated(this); // now it is at each 11 menu shows.
        } // end if is TV.

        /*
         * We show what's new only if is first start of the main menu in current
         * session, no from a finished game:
         */
        if (isFirstLaunchInSession) {
            showWhatsNew();
            isFirstLaunchInSession = false;
        } // end if it is first launch of this in session.

        // To post the still not posted tests from local to on-line DB:
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

        // Sometimes we set as it is not registered as premium, let's say at each howOftenUnregister launches:
        if (isPremium) {
            if (numberOfLaunches % howOftenUnregister == 0) {
                set.saveBooleanSettings("isPremium", false);
                isPremium = false;
            } // end if numberOfLaunches is divisible with howOftenUnregister.
        } // end if it is premium.
// Below it will be made premium again if it is really bought and other actions:
        if (!isPremium) {
            // For billing:
            startBillingDependencies();

            // We also show interstitial ads if it is not premium and not first launch also if not accessibility enabled and also if not tv and also if not too early:
            if (!isFirstLaunchInSession && !isAccessibility && !isTV && numberOfLaunches % howOftenAd == 0 && numberOfLaunches % howOftenUnregister != 0 && numberOfLaunches > howOftenAd + 1) {
                interstitialAdSequence();
            } // end if is time to show the interstitial ad.
        } // end if it is not premium.
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

    // Method for interstitial ads:
    public void interstitialAdSequence() {
        MobileAds.initialize(this, initializationStatus -> {
        });
        AdRequest adRequest = new AdRequest.Builder().build();

        InterstitialAd.load(this, getString(R.string.interstitial_ad_unit_id), adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                // The mInterstitialAd reference will be null until an ad is loaded.
                mInterstitialAd = interstitialAd;
                fullScreenTheInterstitialAd(); // user defined method under the current one.
                showInterstitialAdEffectively(); // the third user defined method for interstitial ads.
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                // Handle the error
                // Log.d(TAG, loadAdError.toString());
                mInterstitialAd = null;
            }
        });
    } // end interstitialAdSequence() method.

    // Now if it is loaded successfully, set full screen:
    public void fullScreenTheInterstitialAd() {
        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdClicked() {
                // Called when a click is recorded for an ad.
                // Log.d(TAG, "Ad was clicked.");
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                // Log.d(TAG, "Ad dismissed fullscreen content.");
                mInterstitialAd = null;
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Called when ad fails to show.
                // Log.e(TAG, "Ad failed to show fullscreen content.");
                mInterstitialAd = null;
            }

            @Override
            public void onAdImpression() {
                // Called when an impression is recorded for an ad.
                // Log.d(TAG, "Ad recorded an impression.");
            }

            @Override
            public void onAdShowedFullScreenContent() {
                // Called when ad is shown.
                // Log.d(TAG, "Ad showed fullscreen content.");
            }
        });
    }  // end fullScreenTheInterstitialAd() method.

    // A method to show the interstitial ad effectively:
    public void showInterstitialAdEffectively() {
        if (mInterstitialAd != null) {
            mInterstitialAd.show(MainActivity.this);
        }
    } // end showInterstitialAdEffectively() method.

    // A method to take the menu item TVs into an array:
    private void getTextViews() {
        tvMenuItems = new TextView[7];
        for (int i = 0; i < tvMenuItems.length; i++) {
            int resID = mContext.getResources().getIdentifier("tvMenuItem" + (i + 1), "id", getPackageName());
            tvMenuItems[i] = findViewById(resID);
        } // end for.
    } // end getTextViews() method.

    // A method to set some initial things, the backgrounds:
    private void setFirstThings() {
        // Get the linear layout with the menu items:
        llMainMenu = findViewById(R.id.llMainMenu);

        // Resize the item menu background:
        llMainMenu.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            /*
             * Resize the button background for menu items to be for
             * a specific screen size.
             */
            if (menuItemBackground == null) {
                int height = tvMenuItems[0].getHeight();
                textHeight = height; // textHeight is a public static variable.
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

                menuItemBackground = GUITools.resizeImage(mContext, "button_main_menu", width, height);

                // Change now the background for each item:
                for (TextView tvMenuItem : tvMenuItems) {
                    tvMenuItem.setBackground(new BitmapDrawable(getResources(), menuItemBackground));
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
            GUITools.alert(this, getString(R.string.warning), String.format(getString(R.string.warning_choose_not_available), "" + nrq));
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

            case R.id.cbtAskFinal:
                set.saveBooleanSettings("askFinal", checked);
                break;

            case R.id.cbtAskHelp:
                set.saveBooleanSettings("askHelp", checked);
                break;

            case R.id.cbtSpeakQuestion:
                set.saveBooleanSettings("speakQuestion", checked);
                break;

            case R.id.cbtSpeakVariants:
                set.saveBooleanSettings("speakVariants", checked);
                break;

            case R.id.cbtSpeakOthers:
                set.saveBooleanSettings("speakOthers", checked);
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

        alert.setIcon(R.drawable.ic_launcher).setPositiveButton(R.string.yes, (dialog, whichButton) -> {
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
    private void showWhatsNew() {
        Settings set = new Settings(this);
        boolean wasAnnounced = set.getBooleanSettings("wasAnnounced" + curVer);

        // Only if it was not set not to be announced anymore, wasAnnounced true:
        if (!wasAnnounced) {
            // create an alert inflating an XML:
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View newView = inflater.inflate(R.layout.whatsnew_dialog, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(R.drawable.ic_launcher);
            builder.setTitle(R.string.whatsnew_title);
            builder.setView(newView);
            builder.setPositiveButton(R.string.close, (dialog, whichButton) -> SoundPlayer.playSimple(mContext, "element_finished"));
            builder.create();
            builder.show();
        } // end if it was not announced.
    } // end showWhatsNew() method.

    // A method to speak a TTS test from settings:
    public void ttsTest(View view) {
        SpeakText tts = new SpeakText(this);
        tts.speakTest(getString(R.string.tts_test_text));
    } // end ttsTest() method.

    // A method to open TTS Settings:
    public void openTTSSettings(View view) {
        // Open TTS settings
        Intent intent = new Intent();
        intent.setAction("com.android.settings.TTS_SETTINGS");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            GUITools.alert(this, getString(R.string.error), getString(R.string.system_tts_settings_not_found));
        }
    } // end openTTSSettings() method.

    // In app billing section starts here:
    public void showPremium(View view) {
        upgradeAlert();
    } // end showPremium() method.

    public void upgradeAlert() {
        // Make a context for this alert dialog:
        Context context = new ContextThemeWrapper(this, R.style.MyAlertDialog);
        // Create now the alert:
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        if (GUITools.isNetworkAvailable(this)) {
            ScrollView sv = new ScrollView(context);
            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);
            // The message:
            TextView tv = new TextView(context);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            tv.setPadding(mPaddingDP, mPaddingDP, mPaddingDP, mPaddingDP);
            String message;
            if (isPremium) {
                message = getString(R.string.premium_version_alert_message);
            } else {
                message = String.format(getString(R.string.non_premium_version_alert_message), mUpgradePrice);
            } // end if is not premium.
            tv.setText(message);
            tv.setFocusable(true);
            ll.addView(tv);
            // Add the LinearLayout into ScrollView:
            sv.addView(ll);
            alertDialog.setTitle(getString(R.string.premium_version_alert_title));
            alertDialog.setView(sv);
            // The button can be close or Get now!:
            String buttonName;
            if (isPremium) {
                buttonName = getString(R.string.close);
            } else {
                buttonName = getString(R.string.bt_buy_premium);
            }
            alertDialog.setPositiveButton(buttonName, (dialog, whichButton) -> {
                // Start the payment process:
                // Only if is not premium:
                if (!isPremium) {
                    upgradeToPremiumActions();
                }
            });
            alertDialog.create();
            alertDialog.show();
        } // end if is connection available.
        else {
            GUITools.alert(this, getString(R.string.warning), getString(R.string.no_connection_available));
        } // end if connection is not available.
    } // end upgradeAlert() method.

    public void upgradeToPremiumActions() {
        initiatePurchase();
    } // end upgradeToPremiumActions() method.

    private void startBillingDependencies() {
        // end onPurchasesUpdated() method.
        purchasesUpdatedListener = (billingResult, purchases) -> {
            // If item newly purchased
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (Purchase purchase : purchases) {
                    handlePurchase(purchase);
                } // end for.
            }
            // If item already purchased then check and reflect changes
            else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                registerTheGameEffectively();
            }
            //if purchase cancelled
            else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                GUITools.alert(mFinalContext, getString(R.string.warning), getString(R.string.purchase_canceled));
            }
            // Handle any other error messages
            else {
                GUITools.alert(mFinalContext, getString(R.string.warning), getString(R.string.billing_unknown_error));
            }
        };

        billingClient = BillingClient.newBuilder(this).setListener(purchasesUpdatedListener).enablePendingPurchases().build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here,
                    QueryProductDetailsParams queryProductDetailsParams = QueryProductDetailsParams.newBuilder().setProductList(listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(mProduct).setProductType(BillingClient.ProductType.INAPP).build())).build();

                    // Now check if it is already purchased:
                    billingClient.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(), (billingResult12, purchases) -> {
                        // check billingResult and process returned purchase list, e.g. display the products user owns
                        if (purchases != null && purchases.size() > 0) { // it means there are items:
                            Purchase myOldPurchase = purchases.get(0);
                            if (myOldPurchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                                registerTheGameEffectively();
                            }
                        } // end process the purchases list.
                    });
                    // end check if it is already purchased.

                    // Now let's query for our product:
                    billingClient.queryProductDetailsAsync(queryProductDetailsParams, (billingResult1, productDetailsList) -> {
                        // check billingResult
                        // process returned productDetailsList
                        myProducts = productDetailsList;
                        // Get the price of the 0 item if there is at least one product:
                        if (myProducts != null && myProducts.size() > 0) {
                            ProductDetails productDetail = myProducts.get(0);
                            ProductDetails.OneTimePurchaseOfferDetails offer = productDetail.getOneTimePurchaseOfferDetails();
                            if (offer != null) {
                                mUpgradePrice = offer.getFormattedPrice();
                            }
                        }
                    });
                    // End query purchase.
                }
            } // end startConnection successfully.

            @Override
            public void onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        }); // end startConnection.

        // Create also the acknowledge purchase listener:
        acknowledgePurchaseResponseListener = billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                // if purchase is acknowledged
                // Grant entitlement to the user. and restart activity
                registerTheGameEffectively(); // here is also saved everything in shared preferences.
            }
        };
// End acknowledge listener creation..
    } // end startBillingDependencies() method.

    private void initiatePurchase() {
        // We purchase here the only one item found in myProducts list:
        if (myProducts != null && myProducts.size() > 0) { // only if there is at least one product available:
            ProductDetails productDetails = myProducts.get(0);

// An activity reference from which the billing flow will be launched.
            Activity activity = this;

            List<BillingFlowParams.ProductDetailsParams> productDetailsParamsList = listOf(BillingFlowParams.ProductDetailsParams.newBuilder()
                    // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                    .setProductDetails(productDetails).build());

            BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).build();

// Launch the billing flow
            BillingResult billingResult = billingClient.launchBillingFlow(activity, billingFlowParams);
        } // end if there is at least one productDetails object in myProducts list.
        else { // no items available:
            GUITools.alert(mFinalContext, getString(R.string.warning), getString(R.string.no_purchases_available));
        }
    } // end initiatePurchase() method.

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseResponseListener);
            }
        }
    } // end handlePurchase() method.

    // A method which sets the game as registered after upgrading:
    public void registerTheGameEffectively() {
        // We save it as a premium version:
        isPremium = true;
        Settings set = new Settings(this);
        set.saveBooleanSettings("isPremium", true);
        // Now show an alert about this purchase:
    } // end registerTheGameEffectively() method.
    // End methods for InAppBilling.

} // end MainActivity class.
