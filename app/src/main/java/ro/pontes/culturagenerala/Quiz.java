package ro.pontes.culturagenerala;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class Quiz {

    private final Context mContext;
    private final Activity activity;
    private final DBAdapter mDbHelper;

    // Fields:
    private final int delayToNextQuestion = 3500;
    private Question question = null;
    private int curQuestionNumber = 1;
    private int lastQuestionId = 1;
    public static int author = 1; // attributed in Question.
    public static int lastCurSetId = 0; // attributed in Question.
    private int curSetId = 1;
    private String[] curSetIds; // an array with all chosen sets.
    private int numberOfChosenTests = 0;
    private int curLevel = 1;
    private String[] levelStatuses;
    private String tvStatusMessage;
    private int lastChosenAnswer = 0;
    private int lastChosenHelpOption = 0;
    private int cumulativeTime = 0;
    private final int TOTAL_TIME = 101; // First second is never shown.
    private int remainedTime = TOTAL_TIME;
    private boolean chronIsRed = false;
    private boolean chronIsRunning = true;
    private int totalPoints = 0; // points in this quiz.
    private int curPoints = 0; // actual question value.
    private int remainedChances = 3;
    private String[] chanceMessages;
    private String[] chanceStars;
    private boolean askFinal = false;
    private boolean askHelp = false;

    // Next static variable keeps information about game status, if it is between question answered and handle start:
    private boolean isTestBetweenQuestions = false;

    // Some objects:
    private final StringTools st;
    private final Statistics stats;
    private final SpeakText tts;

    private TextView tvStatus = null;
    public static int statusHeight = 150; // we need it in Question class.
    private LinearLayout llForIbsLayout = null;
    private TextView chron = null;
    private ImageButton[] ibOptions = null;
    private final boolean[] ibStatus = {true, true, true, false, true};
    private TextView[] tvPBs; // and array for Progress Bar.

    // / To blink the current question on Progress Bar:
    private boolean whichColor = true;
    private int resIdColorFilled;
    private int resIdColorUnfilled;

    // Some resized Bitmaps:
    private Bitmap statusBackground = null;
    private Bitmap chronBackground = null;
    private Bitmap ibsLayoutBackground = null;
    private boolean areVariantsPositionedInMiddle = false;

    // The constructor:
    public Quiz(Context context, Activity activity) {
        this.mContext = context;
        this.activity = activity;
        this.st = new StringTools(mContext);
        this.stats = new Statistics(mContext);
        this.tts = new SpeakText(mContext);

        // Start things for our database:
        mDbHelper = new DBAdapter(this.mContext);
        mDbHelper.createDatabase();
        mDbHelper.open();

        beforeStart();
    } // end constructor.

    @SuppressWarnings("deprecation")
    private void beforeStart() {
        // Get the entire relative layout:
        // GUI controls:
        RelativeLayout layoutMain = activity.findViewById(R.id.layoutMain);
        // Get the tvStatus TextView to have it in this class:
        tvStatus = activity.findViewById(R.id.tvStatus);
        tvStatus.setOnClickListener(view -> showCurrentPointsOnTVStatus());
        // End add listener for short click on tvStatus.

        // Add show set information at long click on tvStatus:
        tvStatus.setOnLongClickListener(view -> {
            showCurrentSet();
            return true;
        });
        // End add listener for long click on tvStatus.

        llForIbsLayout = activity.findViewById(R.id.llForIbsLayout);

        // Charge the chronometer:
        chron = activity.findViewById(R.id.tvGameTimer);

        // Resize the background to have for our screen size:
        // end onGlobalLayout.
        layoutMain.getViewTreeObserver().addOnGlobalLayoutListener(() -> {

            int width;
            int height;

            /*
             * Resize the background for ibsLayout to be 150% of a
             * ImageButton height:
             */
            if (ibsLayoutBackground == null) {
                width = llForIbsLayout.getWidth();
                height = llForIbsLayout.getHeight();
                int newHeight = height + height / 2; // for
                // portrait.
                /*
                 * If is landscape, we add only one quarter to
                 * height:
                 */
                if (!MainActivity.isPortrait) {
                    newHeight = height + height / 4;
                } // end if is landscape.

                ibsLayoutBackground = GUITools.resizeImage(mContext, "background_bottom_buttons", width, newHeight);
                // Change now the background:
                llForIbsLayout.setBackground(new BitmapDrawable(mContext.getResources(), ibsLayoutBackground));
            } // end if ibsLayoutBackground isn't created.

            /*
             * Resize the background for status TextView to be
             * enough for 2 lines of text.
             */
            if (statusBackground == null) {
                width = tvStatus.getWidth();
                height = tvStatus.getHeight();
                // If it is TV, we add 20% to the status height:
                if (MainActivity.isTV) {
                    height = height * 20 / 100 + height;
                } // end if it is Android TV.
                statusHeight = height; // a static value for
                // Question class.
                statusBackground = GUITools.resizeImage(mContext, "background_status", width, height);
                // Change now the background:
                tvStatus.setBackground(new BitmapDrawable(mContext.getResources(), statusBackground));
            } // end if statusBackground isn't created.

            // Now create the background for chronometer resizing an
            // image:
            if (chronBackground == null) {
                /*
                 * Now we know the dimensions of the chronometer. We
                 * can resize the background to wrap content. Only
                 * the width is necessary, the resized background is
                 * a square. We add also x DP instead padding:
                 */
                if (MainActivity.isPortrait) {
                    /*
                     * The size of the chronometer will be the size
                     * of text in width and some padding (12):
                     */
                    width = chron.getWidth();
                    height = chron.getHeight();
                    int dpForChronPadding = 12; // for normal
                    // devices.
                    width = width + GUITools.dpToPx(mContext, dpForChronPadding);

                } else { // Landscape or TV:
                    /*
                     * Charge also the llVariantsCentral, the place
                     * which contains the chronometer. It is
                     * available only in landscape mode. The width
                     * will be a percent from this central layout
                     * which contains the chronometer:
                     */
                    LinearLayout llChron = activity.findViewById(R.id.llVariantsCentral);

                    int w = llChron.getWidth();
                    width = 75 * w / 100;
                    // For TV we also change the text size of the
                    // chronometer:
                    if (MainActivity.isTV) {
                        chron.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize + 4);
                    }
                } // end if it is TV or landscape..

                // Resize it effectively:
                chronBackground = GUITools.resizeImage(mContext, "background_timer", width, width);
                // Change now the background:
                chron.setBackground(new BitmapDrawable(mContext.getResources(), chronBackground));
            } // end if chronBackground isn't created.
            // End resize Bitmaps for backgrounds.

            /*
             * Now set the top margin for first linearLayout
             * variant, this way we can push the variants in the
             * middle between chronometer and bottom band. This is
             * available only for portrait and if it is more place
             * than all 4 variants, we put the variants in the
             * middle between chronometer and bottom band.
             */
            if (!areVariantsPositionedInMiddle && MainActivity.isPortrait) {
                areVariantsPositionedInMiddle = true;

                final Handler handler = new Handler();
                // end run() method.
                handler.postDelayed(() -> {
                    // Do something after a while:

                    LinearLayout llFirstVariant = activity.findViewById(R.id.llFirstVariant);
                    int llHeight = llFirstVariant.getHeight();
                    int chronHeight = chron.getHeight();
                    /*
                     * We need to know how much space is
                     * available for variants zone, to see after
                     * subtract the chronometer height and all
                     * for linear layouts for variant with their
                     * margins, if is more space available.
                     */
                    ScrollView svVariants = activity.findViewById(R.id.svVariants);
                    int svHeight = svVariants.getHeight();
                    /*
                     * Finally, the space between chronometer
                     * and bottom band:
                     */
                    int spaceAvailable = svHeight - chronHeight;

                    /*
                     * Let's see how much space occupies all 4
                     * variants with their margins of 2dp:
                     */
                    int allVariantsHeight = 4 * llHeight + 4 * GUITools.dpToPx(mContext, 2);

                    /*
                     * If the space available is higher than the
                     * all variants, we divide by 2 the remained
                     * space after subtracting the
                     * allVariantsHeight from spaceAvailable:
                     */
                    if (spaceAvailable > allVariantsHeight) {
                        // Only in this case we need to work
                        // with margins:
                        int topMargin = (spaceAvailable - allVariantsHeight) / 2;
                        /*
                         * // Set now the params to
                         * llFirstVariant, including the top
                         * margin. A last check is do not be a
                         * negative number, a very rare case,
                         * who knows:
                         */
                        if (topMargin < 0) {
                            topMargin = 0;
                        }
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) llFirstVariant.getLayoutParams();
                        params.setMargins(0, topMargin, 0, 0);
                        llFirstVariant.setLayoutParams(params);
                    } // end if is more space available.
                }, 110); // a longer delay.
            } // end if is portrait.
            // end put the variants middle between chronometer
            // and bottom.

        });
        // End resize different controls.

        // Reinitialise some variables at the beginnings:
        String temp2 = mContext.getString(R.string.level_statuses);
        levelStatuses = temp2.split("\\|");

        // The message for current question and points:
        tvStatusMessage = mContext.getString(R.string.tv_status);

        // An array with chances messages:
        Resources res = mContext.getResources();
        chanceMessages = res.getStringArray(R.array.chances_array);

        // An array with chances messages:
        chanceStars = res.getStringArray(R.array.stars_chances_array);

        // Fill the array with 21 rectangles for progress bar:
        tvPBs = new TextView[21];
        for (int i = 0; i < 21; i++) {
            int resID = mContext.getResources().getIdentifier("pb" + (i + 1), "id", mContext.getPackageName());
            tvPBs[i] = activity.findViewById(resID);
        } // end for.

        // Determine the resIds for filled and unfilled rectangles on progress
        // bar:
        // First green colour, filled:
        resIdColorFilled = mContext.getResources().getColor(R.color.background_progress_bar_filled);

        // Second red colour, unfilled:
        resIdColorUnfilled = mContext.getResources().getColor(R.color.background_progress_bar_unfilled);
    } // end initial things method.

    /*
     * A method which goes in 3 possible directions: new game, continued game
     * with new question or continued game with same question:
     */
    public void startOrContinueGame() {
        // Get the current set IDs at start and get also the ask or not for final and using help:
        Settings set = new Settings(mContext);
        askFinal = set.getBooleanSettings("askFinal");
        askHelp = set.getBooleanSettings("askHelp");
        curSetIds = set.getStringSettings("curSetIds").split("\\|");
        // Determine the number of sets chosen:
        /*
         * If the curSetsIds has only an item with value 0, it means all sets
         * are chosen and we must query the DB to see how many tests are
         * available:
         */
        if (curSetIds.length == 1 && curSetIds[0].equals("0")) { // all tests:
            String sql = "SELECT COUNT(*) FROM seturi;";
            Cursor cursor = mDbHelper.queryData(sql);
            numberOfChosenTests = cursor.getInt(0);
        } else { // no all tests:
            numberOfChosenTests = curSetIds.length;
        } // end determine numberOfTestsChosen.

        /*
         * We need to see if the last set was suspended after a question, in the
         * handle delay time:
         */
        isTestBetweenQuestions = set.getBooleanSettings("isTestBetweenQuestions");

        if (!MainActivity.isStarted) {
            startGame();
        } else if (isTestBetweenQuestions) {
            chargeLastGame();
            continueGameWithNewQuestion();
            setProgressBarInitial(curQuestionNumber);
        } else {
            chargeLastGame();
            continueGame(); // the last question.
            setProgressBarInitial(curQuestionNumber);
        }
        isTestBetweenQuestions = false;
        set.saveBooleanSettings("isTestBetweenQuestions", isTestBetweenQuestions);
    } // end startOrContinueGame() method.

    private void startGame() {
        MainActivity.isStarted = true;
        this.curQuestionNumber = 1;
        this.remainedChances = 3;
        this.nextQuestion();
    } // end startGame() method.

    private void continueGameWithNewQuestion() {
        // it was saved with previous question number:
        this.curQuestionNumber = this.curQuestionNumber + 1; // increase it.

        // New question, entire time elapsed:
        remainedTime = TOTAL_TIME;

        this.nextQuestion();
    } // end startGame() method.

    private void continueGame() {
        this.getLastQuestion();
    } // end continue() method.

    // A method to set the status of the tvStatus and also the curLevel:
    private void setTVStatus() {
        curLevel = 1;
        int curStatusIndex = 1;
        if (curQuestionNumber > 7 && curQuestionNumber <= 14) {
            curLevel = 2;
            curStatusIndex = 2;
            // Enable change question option if is the 8th, second level:
            if (curQuestionNumber == 8) {
                ibStatus[3] = true;
            } // end if it is exactly the 8th question.
        } else if (curQuestionNumber > 14 && curQuestionNumber < 21) {
            curLevel = 3;
            curStatusIndex = 3;
        } else if (curQuestionNumber == 21) {
            curLevel = 3;
            curStatusIndex = 4;
        } // end if statements to determine the level.
        determineQuestionPointsValue(); // determine the curPoints variable.
        String status = String.format(tvStatusMessage, levelStatuses[curStatusIndex], "" + curQuestionNumber, "" + st.getNumberOfPointsAsString(curPoints));
        Spanned msgStatus = MyHtml.fromHtml(status);
        tvStatus.setText(msgStatus);
    } // end setTVStatus() method.

    // A method to generate a random set from chosen ones:
    private int determineRandomSet() {
        return Integer.parseInt(curSetIds[GUITools.random(0, curSetIds.length - 1)]);
    } // end determineRandomSet() method.

    // A method to go to the next question in current quiz:
    private void nextQuestion() {
        isTestBetweenQuestions = false;
        // Determine a random set from chosen ones:
        curSetId = determineRandomSet();
        chronReset(); // we need it before set status.
        setTVStatus(); // here it is also the curLevel set.
        checkIfAreRemainedQuestionsInSet();
        showHelperButtons();

        question = new Question(mContext, activity, mDbHelper, curSetId, curLevel, lastQuestionId);
        question.make(true);
        // Start also the chronometer:
        chronStart();
    } // end nextQuestion() method.

    // A method to get the last question if is a continued game:
    private void getLastQuestion() {
        setTVStatus();
        showHelperButtons();

        question = new Question(mContext, activity, mDbHelper, curSetId, curLevel, lastQuestionId);
        question.make(false); // false means last question.
        chronStart();
    } // end lastQuestion() method.

    public void confirmAnswer(int answer) {
        // We disable all variants until a confirmation is given:
        question.enableOrDisableAllVariants(false);
        question.changeTVForConfirmation(answer);
        // Now change the llIbsLayout to as if this is the final answer:
        showAnswerConfirmation(answer);
    } // end confirmAnswer() method.

    // A method which shows the wrong answer:
    public void showWrongAnswer() {
        question.showTVForWrongAnswer(lastChosenAnswer);
        // Also announce the correct one for blind users:
        question.sayCorrectAnswer();
    } // end showWrongAnswer() method.

    /* A method which check for answer if yes is pressed for the confirmation: */
    public void checkIfIsCorrectFinalAnswer() {
        // Stop the chronometer:
        chronStop();
        question.showTVForCorrectAnswer();
        if (question.isCorrect(lastChosenAnswer)) {
            // If is a winner:
            // If it was the 21st correct answer:
            if (curQuestionNumber >= 21) {
                MainActivity.isStarted = false;
                isTestBetweenQuestions = false;
                curQuestionNumber = curQuestionNumber + 1; // to be a winner 21
                // in DB
                saveLastGame();
                // Next line must occur after chronometer is stopped:
                setProgressBarFilled(curQuestionNumber - 1); // initial is 22.
                // Stuff for being a winner:
                showInformationAboutWinnerReached();
                final Handler handler = new Handler();
                // Do something after a while:
                // Just one action:
                handler.postDelayed(this::finishActivity, (delayToNextQuestion * 3));

            } else { // no winner, but next question:
                isTestBetweenQuestions = true;
                showInformationAboutCorrectAnswer();
                // Next line must occur after chronometer is stopped:
                setProgressBarFilled(curQuestionNumber);
                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    // Do something after a while:
                    curQuestionNumber = curQuestionNumber + 1;
                    nextQuestion();
                    showHelperButtons();
                }, delayToNextQuestion);
            } // end if next question, not winner.
        } else { // a wrong answer:
            // Here there are two branches, finish or more chances:
            if (remainedChances <= 0) { // finish:
                MainActivity.isStarted = false;
                isTestBetweenQuestions = false;
                saveLastGame();
                showInformationAboutWrongAnswer();
                showWrongAnswer(); // just changes the colour.
                // Next line must occur after chronometer is stopped:
                setProgressBarUnfilled(curQuestionNumber);

                final Handler handler = new Handler();
                // Do something after a while:
                handler.postDelayed(this::finishActivity, (delayToNextQuestion * 2));
            } // end if is finished.

            else { // next question, there are more chances:
                isTestBetweenQuestions = true;
                showInformationAboutWrongAnswerButChance();
                showWrongAnswer(); // just changes the colour.
                // Next line must occur after chronometer is stopped:
                setProgressBarUnfilled(curQuestionNumber);
                /*
                 * We decrement curQuestion Number with 1 to be possible next
                 * time to increase it for next question. It is because if is
                 * closed in suspension time, to be OK for next question when
                 * starting again:
                 */
                curQuestionNumber = curQuestionNumber - 1;
                // Same code like for correct answer:
                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    // Do something after a while:
                    curQuestionNumber = curQuestionNumber + 1;
                    nextQuestion();
                    showHelperButtons();
                }, (delayToNextQuestion * 3 / 2));
            } // end if there are more chances.
        } // end else finished or more chances.
    } // end checkIfIsCorrect() method.

    // A method to go back to question after no was pressed for the confirmation:
    public void backToQuestion() {
        SoundPlayer.playSimple(mContext, "an_action");
        question.changeTVForNormalVariant(lastChosenAnswer);
        // We make again all variants clickable:
        question.enableOrDisableAllVariants(true);
        showHelperButtons();
    } // end backToQuestion() method.

    // This method shows the question in the lower part of the activity:
    private void showAnswerConfirmation(int answer) {
        lastChosenAnswer = answer;
        // If askFinal is true let's ask if it is final:
        if (askFinal) {
            SoundPlayer.playSimple(mContext, "var_chosen");
            // Delete what was previous in the llForIbsButtons:
            llForIbsLayout.removeAllViews();

            /*
             * Inflate the layout with confirm message and yes / no buttons from
             * XML:
             */
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.ll_confirm_final_answer, llForIbsLayout, true);
            String temp = mContext.getString(R.string.are_you_sure);
            speakOthers(temp);
        } // end if it must ask for confirmation.
        else { // dont ask for confirmation:
            checkIfIsCorrectFinalAnswer();
        } // end if not confirmation for final answer is needed.
    } // end showAnswerConfirmation() method.

    // A method to inflate the IBS in the lower part:
    public void showHelperButtons() {
        // Delete what was previous in the llForIbsButtons:
        llForIbsLayout.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.ll_ibs_layout, llForIbsLayout, true);

        // Now charge the buttons in their array:
        // Charge all the bottom ImageButtons:
        ibOptions = new ImageButton[5];
        for (int i = 0; i < ibOptions.length; i++) {
            int resID = mContext.getResources().getIdentifier("ibOption" + (i + 1), "id", mContext.getPackageName());
            ibOptions[i] = activity.findViewById(resID);
        } // end for.

        setIbsState();
    } // end showHelperButtons() method.

    // Sets the state for the helper buttons:
    private void setIbsState() {
        for (int i = 0; i < ibOptions.length; i++) {
            /*
             * Add the disabled image if status is false, by default it is a
             * normal image, enabled:
             */
            if (!ibStatus[i]) {
                String fileName = "ib_option" + (i + 1) + "b";
                int resId = mContext.getResources().getIdentifier(fileName, "drawable", mContext.getPackageName());
                ibOptions[i].setImageResource(resId);
                ibOptions[i].setEnabled(ibStatus[i]);
            } // end if IB must be changed because is disabled.
        } // end for.
    } // end setIbsState() method.

    // This method shows in the lower part if it was a correct answer and other things:
    private void showInformationAboutCorrectAnswer() {
        /*
         * If is a level finished we give the bonus. 10 points for first, 20
         * points for second level. We also play two sound in sequence if is a
         * level finishing:
         */
        if (curQuestionNumber % 7 == 0) { // level finished:
            SoundPlayer.playTwoSoundsInSequence("var_correct", "level_finished", mContext);
            curPoints = curPoints + (curLevel * 10 * numberOfChosenTests);
        } else { // no level finished:
            SoundPlayer.playSimple(mContext, "var_correct");
            // the curPoints remains as defined during current question.
        }

        // Add the current points to total points:
        addPointsToTotal(); // a method.
        showBottomInformation(1);
    } // end showInformationAboutCorrectAnswer() method.

    // This method shows in the lower part if it was a wrong answer::
    private void showInformationAboutWrongAnswer() {
        SoundPlayer.playSimple(mContext, "var_wrong");
        curPoints = curLevel * 10 * -1;
        addPointsToTotal(); // a method.
        showBottomInformation(2);
        showFinalInfoOnTVStatus(2);
    } // end showInformationAboutWrongAnswer() method.

    // This method shows in the lower part if it was a wrong answer but chances:
    private void showInformationAboutWrongAnswerButChance() {
        SoundPlayer.playSimple(mContext, "var_wrong_chance");
        curPoints = curLevel * 10 * -1;
        addPointsToTotal(); // a method.
        showBottomInformation(2); // same as lose.
        showFinalInfoOnTVStatus(1);
        remainedChances = remainedChances - 1;
        showRemainedChancesByStars();
    } // end showInformationAboutWrongAnswerButChance() method.

    // A method which shows dynamically the remained chances:
    private void showRemainedChancesByStars() {
        long blinkDuration = delayToNextQuestion;
        long blinkInterval = 300;

        chron.setText(chanceStars[remainedChances + 1]);
        chron.setTag("yourFirstStatus");

        final CountDownTimer blinkTimer = new CountDownTimer(blinkDuration, blinkInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (chron.getTag() == "yourFirstStatus") {
                    chron.setText(chanceStars[remainedChances]);
                    chron.setTag("yourSecondStatus");
                } else if (chron.getTag() == "yourSecondStatus") {
                    chron.setText(chanceStars[remainedChances + 1]);
                    chron.setTag("yourFirstStatus");
                }
            }

            @Override
            public void onFinish() {
                /* Check if happened a wrong thing, an empty cell to be shown: */
                if (chron.getTag() == "yourFirstStatus") {
                    chron.setText(chanceStars[remainedChances]);
                    chron.setTag("yourSecondStatus");
                } // end if.
            }
        };

        // Now start the think, the blinking:
        blinkTimer.start();
    } // end showRemainedChancesByStars() method.

    // This method shows in the lower part if time has expired:
    private void showInformationAboutTimeExpired() {
        SoundPlayer.playTwoSoundsInSequence("time_expired", "var_wrong", mContext);
        curPoints = (40 - curLevel * 10) * -1;
        addPointsToTotal(); // a method.
        // Because is expired, we must set chances to 0:
        remainedChances = 0;
        showBottomInformation(3);
        question.sayCorrectAnswer();
        // Change also the tvStatus
        showFinalInfoOnTVStatus(3);
    } // end showInformationAboutTimeExpired() method.

    // This method shows in the lower part if it was a winner:
    private void showInformationAboutWinnerReached() {
        SoundPlayer.playSimple(mContext, "millionaire_event");
        // Here calculate the points for winning the game.
        // We give 50 points for each chosen set:
        curPoints = curPoints + (50 * numberOfChosenTests);
        addPointsToTotal(); // a method.
        showBottomInformation(4);
        // Change also the tvStatus:
        showFinalInfoOnTVStatus(4);
    } // end showInformationAboutWinnerReached() method.

    // This method shows in the lower part if it was an abandon:
    private void showInformationAboutAbandon() {
        question.enableOrDisableAllVariants(false);
        question.showTVForCorrectAnswer();
        curPoints = 0;
        addPointsToTotal(); // a method.
        // because is finished, we must make chances to 0:
        remainedChances = 0;
        showBottomInformation(5);
        question.sayCorrectAnswer();
        // Change also the tvStatus:
        showFinalInfoOnTVStatus(5);
        chronStop();
        MainActivity.isStarted = false;
        isTestBetweenQuestions = false;
        saveLastGame();
        // Next line must occur after chronometer is stopped:
        setProgressBarUnfilled(curQuestionNumber);

        // Finish the activity after a while:
        final Handler handler = new Handler();
        // Do something after a while:
        // Just one action:
        handler.postDelayed(this::finishActivity, delayToNextQuestion * 2);
    } // end showInformationAboutAbandon() method.

    // A method which changes the tvStatus at game over:
    private void showFinalInfoOnTVStatus(int what) {
        String statusMessage = mContext.getString(R.string.tv_final_status_message);
        String msgFirstLine = "";
        String msgSecondLine;

        switch (what) {
            case 1: // wrong with chance:
                msgFirstLine = chanceMessages[remainedChances];
                break;

            case 2: // wrong loser:
                msgFirstLine = mContext.getString(R.string.tv_status_wrong_finished);
                break;

            case 3: // looser time expired:
                msgFirstLine = mContext.getString(R.string.tv_status_time_expired);
                break;

            case 4: // winner:
                msgFirstLine = mContext.getString(R.string.tv_status_winner);
                break;

            case 5: // abandon:
                msgFirstLine = mContext.getString(R.string.tv_status_abandoned);
                break;

            default:
                // Nothing yet.
                break;
        } // end switch.

        /*
         * First if there are more chances, it means second line is different,
         * penalise:
         */
        if (remainedChances > 0) {
            // It is penalised:
            msgSecondLine = ""; // not text yet on second line.
        } else {
            // Determine the second line, position, record etc::
            msgSecondLine = stats.getTestPositionInStats(totalPoints, cumulativeTime);
        } // end else if no more chances.

        statusMessage = MyHtml.fromHtml(String.format(statusMessage, msgFirstLine, msgSecondLine)).toString();
        tvStatus.setText(statusMessage);
    } // end showFinalInfoOnTVStatus() method.

    // A method to add current points to total:
    private void addPointsToTotal() {
        totalPoints = totalPoints + curPoints;
    } // end addPointsToTotal() method.

    // A method which make the status string for bottom information:
    private String getPointsStatusAsString() {
        String temp;
        int tempCurPoints = curPoints;
        // We have two possibilities, penalise or plus:
        if (curPoints >= 0) {
            temp = mContext.getString(R.string.tv_points_status);
        } else {
            temp = mContext.getString(R.string.tv_points_status_negative);
            tempCurPoints = curPoints * -1; // make it positive.
        }
        temp = String.format(temp, st.getNumberOfPointsAsString(tempCurPoints), st.getNumberOfPointsAsString(totalPoints));
        return temp;
    } // end getPointsStatusAsString() method.

    // A method to show current points on tvStatus at short click:
    private void showCurrentPointsOnTVStatus() {
        String msg = MyHtml.fromHtml(String.format(mContext.getString(R.string.tv_status_actual_points), st.getNumberOfPointsAsString(totalPoints))).toString();
        tvStatus.setText(msg);
        speakOthers(msg);
    } // end showCurrentPointsOnTVStatus() method.

    // A method to show current set on tvQuestion at long click:
    private void showCurrentSet() {
        // Determine the set name:
        String sql = "SELECT nume FROM seturi WHERE setId='" + lastCurSetId + "';";
        Cursor tempCursor = mDbHelper.queryData(sql);
        String setName = tempCursor.getString(0);
        // Determine the author name:
        sql = "SELECT nume FROM autori WHERE autorId=" + author + ";";
        tempCursor = mDbHelper.queryData(sql);
        String authorName = tempCursor.getString(0);
        String msg = MyHtml.fromHtml(String.format(mContext.getString(R.string.tv_set_item), setName, authorName)).toString();
        tvStatus.setText(msg);
        speakOthers(msg);
    } // end showCurrentSet() method.

    /*
     * This method shows in the lower part an information like> correct answer,
     * wrong answer, millionaire, time has expired etc.:
     */
    private void showBottomInformation(int what) {
        // Delete what was previous in the llForIbsButtons:
        llForIbsLayout.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.ll_bottom_information, llForIbsLayout, true);

        // Change the text view depending of the what variable:
        TextView tvInfo = activity.findViewById(R.id.tvBottomInformation);
        String msgInfo = "";

        // We also make the string for points status:
        String msgPointsStatus = getPointsStatusAsString();

        switch (what) {
            case 1: // correct answer:
                msgInfo = mContext.getString(R.string.tv_correct_answer);
                break;

            case 2: // wrong answer:
                msgInfo = mContext.getString(R.string.tv_wrong_answer);
                break;

            case 3: // time has expired:
                msgInfo = mContext.getString(R.string.tv_time_has_expired);
                break;
            case 4: // It is a winner:
                msgInfo = mContext.getString(R.string.tv_winner_reached);
                break;
            case 5: // It was an abandon:
                msgInfo = mContext.getString(R.string.used_abandon);
                break;

            default:
                // Do nothing yet.
                break;
        } // end switch.

        msgInfo = String.format(msgInfo, msgPointsStatus);
        msgInfo = MyHtml.fromHtml(msgInfo).toString();
        tvInfo.setText(msgInfo);
        speakOthers(msgInfo);
    } // end showBottomInformation() method.

    // A method for use a helping option confirmation. This method shows the question in the lower part of the activity:
    public void showUseHelpConfirmation(int option) {
        // Save the chosen help option to use it after yes pressed:
        lastChosenHelpOption = option;

        // Only if it is askHelp true:
        if (askHelp) {
            SoundPlayer.playSimple(mContext, "an_action");
            // Delete what was previous in the llForIbsButtons:
            llForIbsLayout.removeAllViews();

            /*
             * Inflate the layout with confirm message and yes / no buttons from
             * XML:
             */
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.ll_confirm_use_help, llForIbsLayout, true);

            // Take the TextView for the question:
            TextView tvConfirmation = activity.findViewById(R.id.tvConfirmUseHelp);
            // Get the question confirmation from the string array:
            String msgQuestion = mContext.getResources().getStringArray(R.array.use_options_question_array)[option];
            tvConfirmation.setText(msgQuestion);
            speakOthers(msgQuestion);
        } // end if askHelp is true.
        else { // no use help confirmation needed:
            useHelpOptionEffectively();
        } // end if no use help confirmation is needed.
    } // end showUseHelpConfirmation() method.

    // A method which uses effectively the help option:
    public void useHelpOptionEffectively() {
         // The lastChosenHelpOption variable says which help options to be use here:
        // We change in the ibStatus to false an used option:
        ibStatus[lastChosenHelpOption] = false;

        // We make a switch on lastChosenHelpOption:
        String msgAfterHelpOption = "";
        switch (lastChosenHelpOption) {
            case 0: // Fifty-Fifty:
                msgAfterHelpOption = question.fiftyFifty();
                break;

            case 1: // Call a friend:
                msgAfterHelpOption = question.callFriend();
                break;

            case 2: // ask the public:
                msgAfterHelpOption = question.askPublic();
                break;

            case 3: // change question:
                msgAfterHelpOption = question.changeQuestion();
                // Needed a delay to show again the buttons:
                final Handler handler = new Handler();
                // Do something after a while:
                // Just one action:
                handler.postDelayed(this::showHelperButtons, delayToNextQuestion);

                break;

            case 4: // abandon:
                showInformationAboutAbandon();
                return;
        } // end switch.

        showResultsAfterUseHelpOptions(msgAfterHelpOption);
    } // end useHelpOptionEffectively() method.

    // A method which show the results after using a help option:
    private void showResultsAfterUseHelpOptions(String msgResults) {
        // Delete what was previous in the llForIbsButtons:
        llForIbsLayout.removeAllViews();
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.ll_show_results_after_use_help_options, llForIbsLayout, true);

        // Change the text view:
        TextView tv = activity.findViewById(R.id.tvResultsAfterUseHelpOptions);
        SoundPlayer.playSimple(mContext, "use_help");
        tv.setText(msgResults);
        speakOthers(msgResults);
    } // end showResultsAfterUseHelpOptions() method.

    // A method which checks if there are more questions not used:
    private void checkIfAreRemainedQuestionsInSet() {
        // If it is a chosen set or all of them:
        String sql;
        if (curSetId == 0) {
            sql = "SELECT COUNT(*) FROM intrebari WHERE prag=" + curLevel + " AND consumat=0;";
        } else {
            sql = "SELECT COUNT(*) FROM intrebari WHERE setId=" + curSetId + " AND prag=" + curLevel + " AND consumat=0;";
        }
        Cursor cursor = mDbHelper.queryData(sql);
        int remainedQuestions = cursor.getInt(0);
        // If there are no remained questions:
        if (remainedQuestions == 0) {
            // We set the status of consumed to 0 for this set and level:
            if (curSetId == 0) { // all sets:
                sql = "UPDATE intrebari SET consumat=0 WHERE prag=" + curLevel + ";";
            } else {
                sql = "UPDATE intrebari SET consumat=0 WHERE setId=" + curSetId + " AND prag=" + curLevel + ";";
            } // end if a chosen set.
            mDbHelper.updateData(sql);
        } // end if there are no remained questions.
    } // end checkIfAreRemainedQuestionsInSet() method.

    // Methods to start, reset and stop the Chronometer:
    private void chronStart() {
        chronIsRunning = true;
    } // end chronStart() method.

    private void chronStop() {
        chronIsRunning = false;
        // Determine time elapsed:
        int curQTE = TOTAL_TIME - remainedTime;
        cumulativeTime = cumulativeTime + curQTE;
    } // end chronStop() method.

    private void chronReset() {
        remainedTime = TOTAL_TIME;
        chronIsRed = false;
        chron.setTextColor(Color.WHITE);
    } // end chronStop() method.
    // end chronometer methods.

    public void finishActivity() {
        // Post the Statistics:
        postStatistics();
        GUITools.goToMainActivity(mContext);
        activity.finish();
    } // end finishActivity() method.

    // A method to post the statistics about a finished game:
    private void postStatistics() {
        // Take the string with current chosen sets:
        Settings set = new Settings(mContext);
        String tempSets = set.getStringSettings("curSetIds");

        // Determine if is android TV:
        int androidTV = 0;
        if (MainActivity.isTV) {
            androidTV = 1;
        }

        // Make a string for used buttons:
        // Make a string from the ibStatus array:
        StringBuilder tempIbStatus = new StringBuilder();
        for (int i = 0; i < ibStatus.length; i++) {
            if (!ibStatus[i]) {
                /*
                 * If question is before 11 and ibStatus[3] false, it must be
                 * done false because is the change option help variant and it
                 * is sure it was not used:
                 */
                if (i == 3 && curQuestionNumber < 8) {
                    tempIbStatus.append("0"); // it is not used.
                } else {
                    tempIbStatus.append("1"); // it is used.
                }
            } // end if is false.
            else {
                tempIbStatus.append("0"); // it is not used.
            } // end if is true.
        } // end for.
        String tempUsedOptions = tempIbStatus.toString();

        // A variable with value 1 or 0, was or not posted online:
        int wasOnlinePosted = 0;

        if (GUITools.isNetworkAvailable(mContext)) {
            wasOnlinePosted = 1; // We thing is will be posted.
            stats.postFinishedTestOnline(tempSets, curQuestionNumber - 1, totalPoints, cumulativeTime, androidTV, tempUsedOptions);
        } // end if it is an Internet connection.

        // Now post also locally:
        stats.postFinishedTestLocally(tempSets, curQuestionNumber - 1, totalPoints, cumulativeTime, androidTV, tempUsedOptions, wasOnlinePosted);
    } // end postStatistics() method.

    // Methods to charge and save current game:
    private void chargeLastGame() {
        if (MainActivity.isStarted) {
            Settings set = new Settings(mContext);
            // Charge values for ibStatus boolean array:
            String tempIbStatus = set.getStringSettings("ibStatus");
            String[] ibStatuses = tempIbStatus.split("\\|");
            for (int i = 0; i < ibStatuses.length; i++) {
                ibStatus[i] = Boolean.parseBoolean(ibStatuses[i]);
            } // end for.
            curQuestionNumber = set.getIntSettings("lastCurQuestionNumber");
            lastQuestionId = set.getIntSettings("lastQuestionId");
            totalPoints = set.getIntSettings("totalPoints");
            remainedChances = set.getIntSettings("remainedChances");
            remainedTime = set.getIntSettings("remainedTime");
            cumulativeTime = set.getIntSettings("cumulativeTime");
            chronStart();
        } // end if is started.
    } // end chargeLastGame() method.

    public void saveLastGame() {
        Settings set = new Settings(mContext);
        if (MainActivity.isStarted) {
            set.saveBooleanSettings("isStarted", MainActivity.isStarted);
            set.saveBooleanSettings("isTestBetweenQuestions", isTestBetweenQuestions);
            set.saveIntSettings("lastCurQuestionNumber", curQuestionNumber);
            set.saveIntSettings("lastQuestionId", question.lastQuestionId);
            set.saveIntSettings("remainedTime", remainedTime);
            set.saveIntSettings("cumulativeTime", cumulativeTime);
            set.saveIntSettings("totalPoints", totalPoints);
            set.saveIntSettings("remainedChances", remainedChances);
            // Make a string from the ibStatus array:
            StringBuilder tempIbStatus = new StringBuilder();
            for (int i = 0; i < ibStatus.length; i++) {
                if (!ibStatus[i]) {
                    tempIbStatus.append("false");
                } // end if is false.
                else {
                    tempIbStatus.append("true");
                } // end if is true.
                // If isn't the last item, append also a vertical bar:
                if (i < ibStatus.length - 1) {
                    tempIbStatus.append("|");
                }
            } // end for.
            set.saveStringSettings("ibStatus", tempIbStatus.toString());
        } // end if is started.
        else { // the game is finished, we save this:
            set.saveBooleanSettings("isStarted", MainActivity.isStarted);
        } // end if isStarted is false;
    } // end saveLastGame() method.
    // End charge and save last game methods.

    // Methods to calculate points values:
    /*
     * This method calculates the value for a question depending of the number
     * of seconds passed, level and possible other things:
     */
    private void determineQuestionPointsValue() {
        /*
         * For first level a question earns 10 points, for second 20, for the
         * third one - 30 points. For each 10 seconds passed, it is cut 10% from
         * question value:
         */

        // Finally, we attribute the tempPoints to curPoints global variable:
        curPoints = curLevel * (remainedTime / 10);
    } // end determineQuestionPointsValue() method.

    // This method is called at each second for timer:
    public void timerEvent() {
        if (chronIsRunning) {
            if (remainedTime > 0) {
                remainedTime = remainedTime - 1;

                // Set the colour to red at the chosen moment:
                int CHRON_RED_SECOND = 15;
                if (!chronIsRed && remainedTime <= CHRON_RED_SECOND) {
                    chron.setTextColor(Color.RED);
                    chronIsRed = true;
                } // end make the chronometer red.
                // A beep if time is under CHRON_RED_SECOND:
                if (remainedTime < CHRON_RED_SECOND) {
                    SoundPlayer.playSimple(mContext, "beep_sound");
                }

                // Determine the question value in points at each 10 seconds:
                if (remainedTime % 10 == 0) {
                    setTVStatus();
                } // end determine the question value in points.

                chron.setText("" + remainedTime);

                // Now blink also the progress bar current item:
                updateColor(curQuestionNumber);
                whichColor = !whichColor;

            } // end if remainedTime isn't 0.

            else {
                // Time has expired:
                question.enableOrDisableAllVariants(false);
                question.showTVForCorrectAnswer();
                showInformationAboutTimeExpired();
                chronStop();
                MainActivity.isStarted = false;
                saveLastGame();
                // Next line must occur after chronometer is stopped:
                setProgressBarUnfilled(curQuestionNumber);

                // Finish the activity after a while:
                final Handler handler = new Handler();
                // Do something after a while:
                // Just one action:
                handler.postDelayed(this::finishActivity, delayToNextQuestion * 3);
            } // end else, if remained time reached to 0.
        } // end if chronIsRunning is true;
    } // end timerEvent() method.

    // A method to make filled the progress bar if it is continued game:
    private void setProgressBarInitial(int untilExclusive) {
        for (int i = 0; i < untilExclusive - 1; i++) {
            tvPBs[i].setBackgroundColor(resIdColorFilled);
        } // end for.
    } // end setProgressBarInitial() method.

    // A method to make filled a rectangle for progress bar::
    private void setProgressBarFilled(int rectForQuestion) {
        tvPBs[rectForQuestion - 1].setBackgroundColor(resIdColorFilled);
    } // end setProgressBarFilled() method.

    // A method to make unfilled a rectangle for progress bar::
    private void setProgressBarUnfilled(int rectForQuestion) {
        tvPBs[rectForQuestion - 1].setBackgroundColor(resIdColorUnfilled);
    } // end setProgressBarUnfilled() method.

    // Update the colour for tvPBs blinking:
    private void updateColor(final int whichTV) {
        activity.runOnUiThread(() -> {
            if (whichColor) {
                tvPBs[whichTV - 1].setBackgroundColor(resIdColorUnfilled);
            } else {
                tvPBs[whichTV - 1].setBackgroundColor(resIdColorFilled);
            } // end else.
        });
    } // end updateColor() method.

    // A method to send feedback about current question via alert:
    public void showFeedbackAlert() {
        Feedback feedback = new Feedback(mContext, question.lastQuestionId);
        feedback.feedbackStart();
    } // end showFeedbackAlert()() method.

    // A method to speak other things, but stop other speak at the beginning:
    private void speakOthers(String text) {
        stopTTS();
        this.tts.sayOthers(text, true);
    } // end speakOthers() method.

    public void stopTTS() {
        if (question != null) {
            question.stopTTS();
        }
        if (tts != null) {
            tts.stop();
        }
    } // end stopTTS() method.

} // end Quiz class.
