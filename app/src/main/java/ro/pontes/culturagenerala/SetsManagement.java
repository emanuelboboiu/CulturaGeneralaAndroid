package ro.pontes.culturagenerala;

import android.app.AlertDialog;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils.TruncateAt;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;

public class SetsManagement {

    private final DBAdapter mDbHelper;
    private final Context mContext;
    private TextView[] arrTV = null;
    private ArrayList<String> arrSets;
    private boolean isAllSets = false;
    private final int nrSets;
    private final int lastId; // for last item unselect all.

    // The constructor:
    public SetsManagement(Context context) {
        this.mContext = context;

        // Start things for our database:
        mDbHelper = new DBAdapter(this.mContext);
        mDbHelper.createDatabase();
        mDbHelper.open();

        // To have the number of sets:
        nrSets = getNumberOfSets();

        // To have the max set id plus 1 for last item in list:
        lastId = getMaxSetId() + 1;

        // Instantiate the array list for chosen sets::
        arrSets = new ArrayList<>();
        // Fill the array list with current chosen sets:
        Settings set = new Settings(this.mContext);
        String temp = set.getStringSettings("curSetIds");
        String[] curSetIds = temp.split("\\|");
        for (String curSetId : curSetIds) {
            arrSets.add(curSetId);
        } // end for.

        // If now it is contained the 0 in it, it means all sets were chosen and
        // we put all setIDs:
        if (arrSetsContainsSetId("0") > -1) {
            arrSets = getAllSetIds();
            isAllSets = true;
        } // end if all sets were needed.
        // end fill the array list with chosen sets IDs.
    } // end constructor.

    // A method to show sets in an alert LinearLayout:
    public void showSets() {
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

        // A LayoutParams for text views dimensions:
        // We add to height 25%:
        int tvHeight = MainActivity.textHeight * 25 / 100 + MainActivity.textHeight;
        LinearLayout.LayoutParams tvParam = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, tvHeight);

        // Get all sets from DB:
        String sql = "SELECT * FROM seturi ORDER BY nume COLLATE LOCALIZED;";
        Cursor cursor = mDbHelper.queryData(sql);

        /*
         * We instantiate the arrTV array, number of sets plus 2, meaning the
         * all sets item and no selection:
         */
        arrTV = new TextView[cursor.getCount() + 2];
        // A iterator variable as integer to for the array index:
        int arrTVIndex = 0;

        // Add first the all sets item:
        int totalNumberOfQuestions = getTotalNumberOfQuestions();
        String tvItemAll = mContext.getString(R.string.tv_set_item_all);
        String tvText = String.format(tvItemAll, "" + totalNumberOfQuestions);
        CharSequence tvSeq = MyHtml.fromHtml(tvText);
        TextView tv = createTextView();
        tv.setText(tvSeq);
        // Let's hope that it doesn't interfere.
        int BASE_VIEW_ID = 1000010000;
        tv.setId(BASE_VIEW_ID);
        tv.setTag(0);
        // For a short click, set new set and close:
        tv.setOnClickListener(view -> {
            int setId = 0;
            afterClickAnItem(setId, 0);
        });
        // End add listener for short click on a set item.

        // Add the TextView into arrTV:
        arrTV[arrTVIndex] = tv;

        ll.addView(tv, tvParam);
        // end adding the first item, all sets.

        // A string like "Geography (John Smith) (123)":
        String tvItem = mContext.getString(R.string.tv_set_item);
        // In a while make TVs for each set:
        cursor.moveToFirst();
        do {
            final int setId = cursor.getInt(0);
            String setName = cursor.getString(1);
            String authorName = getSetAuthor(setId);
            int nrQuestions = getNumberOfQuestionsBySetId(setId);
            tvText = String.format(tvItem, setName, authorName, "" + nrQuestions);
            tvSeq = MyHtml.fromHtml(tvText);
            tv = createTextView();
            tv.setText(tvSeq);
            tv.setId(BASE_VIEW_ID + setId);
            tv.setTag(setId);
            arrTVIndex++; // increase the index for arrTV:
            final int tempIndex = arrTVIndex; // we need it in another thread.
            // For a short click, set new set and close:
            tv.setOnClickListener(view -> afterClickAnItem(setId, tempIndex));
            // End add listener for short click on a set item.

            // Add the TextView into arrTV:
            arrTV[arrTVIndex] = tv;

            ll.addView(tv, tvParam);
        } while (cursor.moveToNext());

        // Add last the no sets item:
        tvText = mContext.getString(R.string.tv_set_item_none);
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        tv.setId(BASE_VIEW_ID + lastId);
        tv.setTag(lastId);
        // For a short click, set new set and close:
        tv.setOnClickListener(view -> afterClickAnItem(lastId, lastId));
        // End add listener for short click on unselect all.

        // Add the TextView into arrTV:
        arrTV[arrTVIndex + 1] = tv;

        ll.addView(tv, tvParam);
        // end adding the last item, none sets.

        // Add the linear layout into SV layout.
        sv.addView(ll);

        // The alert dialog is created and shown:
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setTitle(mContext.getString(R.string.set_management_alert_title));
        alertDialog.setView(sv);

        alertDialog.setPositiveButton(mContext.getString(R.string.save_sets), (dialog, whichButton) -> {
            SoundPlayer.playSimple(mContext, "element_finished");
            /*
             * If no set where chosen, we put the set 1 like as
             * default. This is the CULTURA GENERALA GP sets:
             */
            if (arrSets.size() == 0) {
                arrSets.add("" + 1);
                arrSets.add("" + 24);
            }

            // If all sets were chosen, add 0 the value in array:
            if (isAllSets) {
                arrSets.clear();
                arrSets.add("0");
            } // end if all sets were chosen.

            // Save as delimited string the chosen sets:
            // Make the ArrayList an array:
            String[] aTemp = arrSets.toArray(new String[arrSets.size()]);
            String chosenSets = GUITools.arrayToDelimitedString(aTemp);
            Settings set = new Settings(mContext);
            set.saveStringSettings("curSetIds", chosenSets);
        });
        alertDialog.setNegativeButton(mContext.getString(R.string.cancel), (dialog, whichButton) -> {
            // Nothing...
        });

        alertDialog.create();
        alertDialog.show();

        // Make all as selected or not:
        setAllTVsAsSelectedOrNot();
    } // end showSets() method..

    // A method to perform action after clicking an item in sets list:
    private void afterClickAnItem(int setId, int whichInArrTV) {
        SoundPlayer.playSimple(mContext, "element_clicked");
        /*
         * If the selected item is 0, we will select all and we put only 0 as
         * value in arrSets ArrayList:
         */
        if (setId == 0) {
            // Remove the other sets and put all of them:
            arrSets.clear();
            // Add all setIds::
            arrSets = getAllSetIds();
            isAllSets = true;

            // Make all ass selected or not:
            setAllTVsAsSelectedOrNot();
        } else if (setId == lastId) { // none sets was clicked:
            arrSets.clear();
            isAllSets = false;
            // Make all ass selected or not:
            setAllTVsAsSelectedOrNot();

        } else { // other sets where clicked:
            // If we have 0, we must remove it from arrSets:
            if (arrSetsContainsSetId("0") > -1) {
                arrSets.remove(arrSetsContainsSetId("0"));
                makeItemAsChosenOrNot(false, 0); // this is the first one.
            } // end remove 0 if it exists, all sets.

            /*
             * There are two possibilities, it was or not already selected. It
             * is already in arrSets or not:
             */
            int tempExists = arrSetsContainsSetId("" + setId);
            if (tempExists > -1) {
                // If it already is chosen:
                // We remove it from arrSets:
                arrSets.remove(tempExists);
                makeItemAsChosenOrNot(false, whichInArrTV);
            } else {
                // It is not already chosen:
                // We add it into arrSets:
                arrSets.add("" + setId);
                makeItemAsChosenOrNot(true, whichInArrTV);
            } // end if it is not already chosen.
        } // end if not 0 set all was clicked.

        // Select or unselect the all item in list:
        if (arrSets.size() == nrSets) {
            makeItemAsChosenOrNot(true, 0);
            isAllSets = true;
        } // end if all sets are selected.

        else if (arrSets.size() == 0) {
            makeItemAsChosenOrNot(true, lastId);
            isAllSets = false;
        } // end if none sets are selected.

        else { // no all sets are selected or unselected:
            makeItemAsChosenOrNot(false, 0);
            makeItemAsChosenOrNot(false, lastId);
            isAllSets = false;
        } // end if no all sets are selected.
    } // end afterClickAnItem() method.

    @SuppressWarnings("deprecation")
    private void makeItemAsChosenOrNot(boolean isSelected, int arrTVIndex) {
        /*
         * Take the text and make it as content description with selected or not
         * at the end for blind users:
         */
        String text = arrTV[arrTVIndex].getText().toString();

        if (isSelected) {
            arrTV[arrTVIndex].setBackgroundResource(R.drawable.var_green);
            // Set the selector for black text as default:
            arrTV[arrTVIndex].setTextColor(mContext.getResources().getColorStateList(R.color.selector_text_black));

            // Change also the text for content description:
            text = "Selectat. " + text;
        } else { // not selected, white on blue:
            arrTV[arrTVIndex].setBackgroundResource(R.drawable.var_blue);
            // Set the selector for white text as default:
            arrTV[arrTVIndex].setTextColor(mContext.getResources().getColorStateList(R.color.selector_text_white));

        } // end if is not selected.
        // Set the content description for blind users:
        arrTV[arrTVIndex].setContentDescription(text);
    } // end makeItemAsChosen() method.

    // A method to set for first time selected or not the items sets:
    private void setAllTVsAsSelectedOrNot() {
        for (int i = 0; i < arrTV.length; i++) {
            int setId = (int) arrTV[i].getTag();

            /*
             * There are two possibilities, it was or not already selected. It
             * is already in arrSets or not:
             */
            int tempExists = arrSetsContainsSetId("" + setId);
            if (tempExists > -1) {
                // If it is chosen:
                makeItemAsChosenOrNot(true, i);
            } else {
                // It is not chosen:
                makeItemAsChosenOrNot(false, i);
            } // end if it is not already chosen.
        } // end for.

        /*
         * if all sets are chosen, lets make as selected also the first item,
         * all sets:
         */
        if (isAllSets && arrSets.size() == nrSets) {
            makeItemAsChosenOrNot(true, 0);
        } // end if all sets are selected.

        /*
         * if all sets are unselected, lets make as selected also the first
         * item, all sets:
         */
        if (arrSets.size() == 0) {
            makeItemAsChosenOrNot(true, lastId);
        } // end if all sets are unselected.
    } // end setAllTVsAsSelectedOrNot() method.

    private int arrSetsContainsSetId(String setId) {
        int exists = -1;
        for (int i = 0; i < arrSets.size(); i++) {
            if (arrSets.get(i).equals(setId)) {
                exists = i;
                break;
            } // end if string is contained.
        } // end for.
        return exists;
    } // end arrSetsContainsSetId() method.

    // A method to get author name for a set:
    private String getSetAuthor(int setId) {
        // Get first a question from set, there we have also the authorId:
        String sql = "SELECT autorId FROM intrebari WHERE setId=" + setId + " LIMIT 1";
        Cursor cursor = mDbHelper.queryData(sql);
        cursor.moveToFirst();
        int authorId = cursor.getInt(0);

        // Take now the author's name from AUTORI DB:
        sql = "SELECT nume FROM autori WHERE autorId = " + authorId + ";";
        cursor = mDbHelper.queryData(sql);
        cursor.moveToFirst();

        return cursor.getString(0);
    } // end getSetAuthor() method.

    // A method to return an array with all sets IDs:
    private ArrayList<String> getAllSetIds() {
        ArrayList<String> arr = new ArrayList<>();
        // Get all sets from DB:
        String sql = "SELECT setId FROM seturi ORDER BY setId;";
        Cursor cursor = mDbHelper.queryData(sql);
        cursor.moveToFirst();
        int count = cursor.getCount();
        for (int i = 0; i < count; i++) {
            arr.add(cursor.getString(0));
            cursor.moveToNext();
        } // end for adding all setIds.
        return arr;
    } // end getAllSetIds() method.

    // A method to return the number of available sets:
    private int getNumberOfSets() {
        // Get count sets from DB:
        String sql = "SELECT COUNT(*) FROM seturi;";
        Cursor cursor = mDbHelper.queryData(sql);
        cursor.moveToFirst();
        return cursor.getInt(0);
    } // end getNumberOfSets() method.

    // A method to return the max setID:
    private int getMaxSetId() {
        // Get max setId from DB:
        String sql = "SELECT MAX(setId) FROM seturi;";
        Cursor cursor = mDbHelper.queryData(sql);
        cursor.moveToFirst();
        return cursor.getInt(0);
    } // end getMaxSetId() method.

    // A method to return the number of questions by setId:
    private int getNumberOfQuestionsBySetId(int setId) {
        String sql = "SELECT COUNT(*) FROM intrebari WHERE setId=" + setId + ";";
        Cursor cursor = mDbHelper.queryData(sql);
        cursor.moveToFirst();
        return cursor.getInt(0);
    } // end getNumberOfQuestionsBySetId() method.

    // A method to return the total number of questions:
    private int getTotalNumberOfQuestions() {
        String sql = "SELECT COUNT(*) FROM intrebari;";
        Cursor cursor = mDbHelper.queryData(sql);
        cursor.moveToFirst();
        return cursor.getInt(0);
    } // end getTotalNumberOfQuestions() method.

    // A method to create a text view for items in this alert:
    private TextView createTextView() {
        TextView tv = new TextView(mContext);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);

        // We need a a padding:
        int mPaddingDP = 12;
        // For TV we need more padding:
        if (MainActivity.isTV) {
            mPaddingDP = mPaddingDP * 8;
        }
        // Calculate the pixels in DP for mPaddingDP, for TextViews.
        mPaddingDP = GUITools.dpToPx(mContext, mPaddingDP);
        tv.setPadding(mPaddingDP, 0, mPaddingDP, 0);
        tv.setFocusable(true);
        tv.setClickable(true);
        tv.setEllipsize(TruncateAt.MARQUEE);
        tv.setMarqueeRepeatLimit(-1);
        tv.setSingleLine(true);
        tv.setHorizontallyScrolling(true);
        tv.setHorizontalScrollBarEnabled(true);
        tv.setMovementMethod(new ScrollingMovementMethod());
        return tv;
    } // end createTextView() method.

} // end SetsManagement class.
