package ro.pontes.culturagenerala;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.text.TextUtils.TruncateAt;
import android.text.method.ScrollingMovementMethod;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;

public class Question {

    private final Context mContext;
    private final Activity activity;
    private final DBAdapter mDbHelper;
    public int lastQuestionId; // just if is continued.
    private final int set;
    private final int threshold;
    private String questionText = "";
    private String[] variants = new String[4];
    private int correctVariantIndex = 0;
    private final String[] aABCD = {"A", "B", "C", "D"};

    // For GUI:
    private TextView tvQuestion;
    private TextView[] tvVariants;
    private TextView[] tvLetters;
    private boolean isVariantsResized = false;

    // For text to speech:
    private final SpeakText tts;
    private final boolean speakQuestion;
    private final boolean speakVariants;
    private final StringBuilder sbQuestion;

    // The constructor for a new question:
    public Question(Context context, Activity activity, DBAdapter mDbHelper, int set, int threshold, int lastQuestionId) {
        this.mContext = context;
        this.activity = activity;
        this.mDbHelper = mDbHelper;
        this.lastQuestionId = lastQuestionId;
        this.set = set;
        this.threshold = threshold;

        this.sbQuestion = new StringBuilder();
        this.tts = new SpeakText(mContext);
        Settings settings = new Settings(mContext);
        this.speakQuestion = settings.getBooleanSettings("speakQuestion");
        this.speakVariants = settings.getBooleanSettings("speakVariants");

        getTextViews();
    } // end constructor for a new question.

    // A public method to make a question effectively:
    public void make(boolean isNew) {
        if (isNew) {
            SoundPlayer.playSimple(mContext, "new_question");
        } // end if is new, for sound.
        this.generate(isNew);
        this.draw();
    } // end make() method.

    // A method to take a corresponding question from database:
    private void generate(boolean isNew) {
        String sql;
        if (isNew) { // generate a new question:
            // If set is 0, it means all sets:
            if (set <= 0) {
                sql = "SELECT * FROM intrebari WHERE prag=" + threshold + " AND consumat=0 ORDER BY random() LIMIT 1;";
            } else { // a chosen set:
                sql = "SELECT * FROM intrebari WHERE setId=" + set + " AND prag=" + threshold + " AND consumat=0 ORDER BY random() LIMIT 1;";
            } // end make query for a chosen set.
        } else { // get the last question:
            sql = "SELECT * FROM intrebari WHERE intrebareId=" + lastQuestionId + ";";
        } // end if is not a new question.

        Cursor cursor = mDbHelper.queryData(sql);
        Quiz.author = cursor.getInt(1);
        Quiz.lastCurSetId = cursor.getInt(2); // removed.
        questionText = cursor.getString(4);
        lastQuestionId = cursor.getInt(0);
        // The variants in a for:
        for (int i = 0; i < 4; i++) {
            // The column index for first variant is 5 in cursor:
            variants[i] = cursor.getString(i + 5);
        } // end for variants array.

        // We set this question as consumed in DB:
        int curQuestionId = cursor.getInt(0);
        sql = "UPDATE intrebari SET consumat=1 WHERE intrebareId=" + curQuestionId + ";";
        mDbHelper.updateData(sql);

        // We shuffle and determine the index of correct index:
        shuffle();
    }// end generateQuestion() method.

    // A method to shuffle the variants, used in generate():
    private void shuffle() {
        ArrayList<String> strList = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            strList.add(i, variants[i + 1]);
        } // end for fill the ArrayList with incorrect variants.
        // Shuffle now the 3 incorrect variants:
        Collections.shuffle(strList);
        // Add now randomly the correct variant at the one of the positions:
        // We determine where to place the correct variant:
        correctVariantIndex = GUITools.random(0, 3);
        strList.add(correctVariantIndex, variants[0]);
        // Back to our object array for variants:
        variants = strList.toArray(new String[strList.size()]);
    } // end shuffle() method.

    private void getTextViews() {
        // We take all the TextViews to fill them with text:
        tvQuestion = activity.findViewById(R.id.tvQuestion);
        tvQuestion.setMovementMethod(new ScrollingMovementMethod());

        tvVariants = new TextView[4];
        for (int i = 0; i < 4; i++) {
            int resID = mContext.getResources().getIdentifier("tvVariant" + (i + 1), "id", mContext.getPackageName());
            tvVariants[i] = activity.findViewById(resID);
        } // end for.

        tvLetters = new TextView[4];
        for (int i = 0; i < 4; i++) {
            int resID = mContext.getResources().getIdentifier("tvLetter" + (i + 1), "id", mContext.getPackageName());
            tvLetters[i] = activity.findViewById(resID);
        } // end for.

        // Now work at resize in a tree observer:
        LinearLayout llVariants = activity.findViewById(R.id.llVariants);
        llVariants.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (!isVariantsResized) {
                // Take the width of the llVariants Layout:
                LinearLayout llVariants1 = activity.findViewById(R.id.llVariants);
                int llVariantsWidth = llVariants1.getWidth();
                int procToAddToHeight = 65;
                if (MainActivity.isTV) {
                    // It means the variants can be larger:
                    procToAddToHeight = 85;
                }
                int height = procToAddToHeight * Quiz.statusHeight / 100;
                // it's a square.

                // A LayoutParams for letters:
                LayoutParams paramsForLetters = (LayoutParams) tvLetters[0].getLayoutParams();
                paramsForLetters.height = height;
                paramsForLetters.width = height;

                // A LayoutParams for variants:
                int tvVariantsWidth = llVariantsWidth - height - GUITools.dpToPx(mContext, 2);
                LayoutParams paramsForVariants = (LayoutParams) tvVariants[0].getLayoutParams();
                paramsForVariants.height = height;
                paramsForVariants.width = tvVariantsWidth;

                // Set all dimensions for variants controls in a
                // for:
                for (int i = 0; i < 4; i++) {
                    tvLetters[i].setLayoutParams(paramsForLetters);
                    tvVariants[i].setLayoutParams(paramsForVariants);
                    // Make also scrollable horizontally:
                    tvVariants[i].setEllipsize(TruncateAt.MARQUEE);
                    tvVariants[i].setMarqueeRepeatLimit(-1);
                    tvVariants[i].setSingleLine(true);
                    tvVariants[i].setHorizontallyScrolling(true);
                    // tvVariants[i].setHorizontalScrollBarEnabled(true);
                    tvVariants[i].setMovementMethod(new ScrollingMovementMethod());
                } // end for.

                isVariantsResized = true;
            } // end if isVariantsResized is false.
            // End resize Bitmaps for backgrounds.
        });
        // End resize the backgrounds for different controls.
    } // end getTextViews() method.

    private void draw() {
        tvQuestion.setText(questionText);
        if (this.speakQuestion) {
            sbQuestion.append(questionText).append("\n");
        }

        tvQuestion.scrollTo(0, 0);

        // Make as normal appearance style, simple variant:
        makeAllVariantsAsNormal();
        enableOrDisableAllVariants(true);
        // Set now the text for each tvVariant:
        for (int i = 0; i < 4; i++) {
            tvVariants[i].setText(variants[i]);
            tvVariants[i].scrollTo(0, 0);

            if (this.speakVariants) {
                sbQuestion.append(aABCD[i]).append(": ").append(variants[i]).append(".\n");
            }
        } // end for.

        // Say the question and or the variants:
        tts.sayDelayed(sbQuestion.toString(), true);
    } // end draw() method.

    // A method which makes all variants as normal:
    public void makeAllVariantsAsNormal() {
        for (int i = 0; i < 4; i++) {
            changeTVForNormalVariant(i);
        } // end for.
    } // end makeAllVariantsAsNormal() method.

    // A method which enables or disables all variants:
    public void enableOrDisableAllVariants(boolean status) {
        for (int i = 0; i < 4; i++) {
            tvLetters[i].setEnabled(status);
            tvVariants[i].setEnabled(status);
            // Make it focusable or not, depending of the status:
            tvVariants[i].setFocusable(status);
        } // end for.
    } // end disableAllVariants() method.

    // A method which set the TV with a variant as normal:
    @SuppressWarnings("deprecation")
    public void changeTVForNormalVariant(int variant) {
        // Change the background for letter::
        String fileName = "var_blue" + (variant + 1);
        int resId = mContext.getResources().getIdentifier(fileName, "drawable", mContext.getPackageName());
        tvLetters[variant].setBackgroundResource(resId);

        // Change the background for variant text:
        tvVariants[variant].setBackgroundResource(R.drawable.var_blue);
        // Change the text colour of the variant text:
        tvVariants[variant].setTextColor(ContextCompat.getColor(mContext, R.color.tvTextWhite));

        tvVariants[variant].setTextColor(mContext.getResources().getColorStateList(R.color.selector_text_white));
        tvVariants[variant].setFocusable(true);
    } // end changeTVForNormalVariant() method.

    // A method which set the TV with chosen variant before confirmation:
    public void changeTVForConfirmation(int variant) {
        // Change the background for letter::
        String fileName = "var_yellow" + (variant + 1);
        int resId = mContext.getResources().getIdentifier(fileName, "drawable", mContext.getPackageName());
        tvLetters[variant].setBackgroundResource(resId);

        // Change the background for text variant:
        tvVariants[variant].setBackgroundResource(R.drawable.var_yellow);
        // Change the text colour:
        tvVariants[variant].setTextColor(ContextCompat.getColor(mContext, R.color.tvTextBlack));
    } // end changeTVForConfirmation() method.

    // A method which shows a wrong answer:
    public void showTVForWrongAnswer(int variant) {
        // Change the background for letter::
        String fileName = "var_red" + (variant + 1);
        int resId = mContext.getResources().getIdentifier(fileName, "drawable", mContext.getPackageName());
        tvLetters[variant].setBackgroundResource(resId);
        // We disable it because we use this in fifty_fifty too.
        tvLetters[variant].setEnabled(false);

        // Change the background for the text variant:
        tvVariants[variant].setBackgroundResource(R.drawable.var_red);
        // We disable it because we use this in fifty_fifty too.
        tvVariants[variant].setEnabled(false);

        // Change the text colour:
        tvVariants[variant].setTextColor(ContextCompat.getColor(mContext, R.color.tvTextWhite));

        // tvVariants[variant].setFocusable(false);
    } // end showTVForWrong() method.

    // A method which shows the correct answer:
    public void showTVForCorrectAnswer() {
        // Change the background for letter::
        String fileName = "var_green" + (correctVariantIndex + 1);
        int resId = mContext.getResources().getIdentifier(fileName, "drawable", mContext.getPackageName());
        tvLetters[correctVariantIndex].setBackgroundResource(resId);

        // Change the background for the text variant::
        tvVariants[correctVariantIndex].setBackgroundResource(R.drawable.var_green);
        // Change the text colour:
        tvVariants[correctVariantIndex].setTextColor(ContextCompat.getColor(mContext, R.color.tvTextBlack));
    } // end showTVForCorrect() method.

    /* A method which returns true if final answer is correct, false otherwise: */
    public boolean isCorrect(int answer) {
        return correctVariantIndex == answer;
    } // end isCorrect() method.

    public String fiftyFifty() {
        // SoundPlayer.playSimple(mContext, "fifty");
        int first = correctVariantIndex;
        while (first == correctVariantIndex) {
            first = GUITools.random(0, 3);
        } // end while, choosing a first wrong variant..

        int second = correctVariantIndex;
        while (second == correctVariantIndex || second == first) {
            second = GUITools.random(0, 3);
        } // end while, choosing a second wrong variant.

        // Make red and disable those 2 variants:
        this.showTVForWrongAnswer(first);
        this.showTVForWrongAnswer(second);

        return mContext.getString(R.string.used_fifty);
    } // end fiftyFifty() method.

    public String callFriend() {
        int[] aPercentValues = {40, 50, 60, 70, 80, 90, 100};
        int minPercent = 2; // this is for first threshold.
        // An if to determine which is the minPROC:
        if (threshold >= 3) {
            minPercent = 0;
        } else if (threshold >= 2) {
            minPercent = 1;
        } // end if.

        int randPercent = GUITools.random(minPercent, aPercentValues.length - 1);
        randPercent = aPercentValues[randPercent];
        int temCorrectVarriantIndex = correctVariantIndex;
        // If randPercent is less than 60%, the friend give a random answer:
        if (randPercent < 60) {
            temCorrectVarriantIndex = GUITools.random(0, 3);
        } // end if.

        // Format the string message to be returned:
        return String.format(mContext.getString(R.string.used_call), "" + randPercent, aABCD[temCorrectVarriantIndex]);
    } // end callFriend() method.

    public String askPublic() {
        // decide the minimum and maximum percent for correct variant depending
        // of questions threshold:
        int minPercent = 60;
        int maxPercent = 80; // these min and max are for first set, easiest.
        if (threshold >= 3) { // if third threshold, the most difficult:
            minPercent = 40;
            maxPercent = 60;
        } else if (threshold >= 2) { // for second threshold, medium.
            minPercent = 50;
            maxPercent = 70;
        } // end if.

        StringBuilder rezPublic = new StringBuilder(); // a variable with public vote.
        int proc = 100; // a variant which is 100 max, to take from.

        // an array to keep the percent for corresponding variants.
        int[] aProcVariants = {0, 0, 0, 0};
        // Set the percentage for correct variant:
        aProcVariants[correctVariantIndex] = GUITools.random(minPercent, maxPercent);
        proc = proc - aProcVariants[correctVariantIndex]; // to have the
        // remaining
        // percentage.
        // Insert other values to other 3 variants:
        for (int i = 0; i < aProcVariants.length; i++) {
            if (aProcVariants[i] == 0) {
                aProcVariants[i] = GUITools.random(0, proc);
                proc = proc - aProcVariants[i];
            } // end if.

            // Add the remaining percentage to a random index in the array to be
            // total 100%, but excepting the correct variant:
            // this occurs if i is 3:
            if (i == 3) {
                // a while to avoid the correct variant at random:
                int whereToAdd = correctVariantIndex; // first to be the correct
                // variant, after to
                // become another one.
                while (whereToAdd == correctVariantIndex) {
                    whereToAdd = GUITools.random(0, 3);
                } // end while.
                // Now add the remaining PROC variable to the random
                // incorrect
                // variant:
                aProcVariants[whereToAdd] += proc;
            } // end if is 3 in for.
        } // end for.

        // Make the string given by public:
        for (int i = 0; i < aProcVariants.length; i++) {
            rezPublic.append(aABCD[i]).append(" = ").append(aProcVariants[i]).append("%, ");
        } // end for.
        // Cut the final comma:
        rezPublic = new StringBuilder(rezPublic.substring(0, rezPublic.length() - 2));
        return String.format(mContext.getString(R.string.used_ask), rezPublic);
    } // end askPublic() method.

    public String changeQuestion() {
        this.showTVForCorrectAnswer();
        sayCorrectAnswer();
        // Needed a delay:
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            // Do something after a while:
            // Just one action:
            make(true); // generates a new question.
        }, 2000);

        return mContext.getString(R.string.used_change);
    } // end changeQuestion() method.

    // Method for text to speech,:
    public void sayCorrectAnswer() {
        // We try to postpone a little the say action:
        final String message = String.format(mContext.getString(R.string.tts_correct_answer), aABCD[correctVariantIndex], variants[correctVariantIndex]);

        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            // Do something after a while:
            // Just one action, show a toast with correct answer:
            GUITools.toast(message, 1000, mContext);
        }, 2500);
    } // end sayCorrectAnswer() method.

    // A method to stop the tts for question and variants:
    public void stopTTS() {
        if (tts != null) {
            tts.stop();
        }
    } // end stopTTS() method.

} // end Question class.
