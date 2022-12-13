package ro.pontes.culturagenerala;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Statistics {

    private final Context mContext;
    private final DBAdapter2 mDbHelper2;
    private final StringTools st;

    // The constructor:
    public Statistics(Context context) {
        this.mContext = context;

        // Start things for our database:
        mDbHelper2 = new DBAdapter2(mContext);
        mDbHelper2.createDatabase();
        mDbHelper2.open();

        st = new StringTools(mContext);
    } // end constructor.

    // Methods to show statistics in a alert:
    public void showStats() {
        /*
         * We make a linear layout and we put there text views for each
         * information. We add also the LinearLayout into a ScrollView:
         */

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
        LinearLayout.LayoutParams tvParam = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

        // 3 variables we use repetitive:
        TextView tv;
        CharSequence tvSeq;
        String tvText;

        // First, an introductory message:
        tvText = String.format(mContext.getString(R.string.stats_introduction),
                getFirstTestDate());
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // Number of finished tests:
        tvText = String.format(
                mContext.getString(R.string.stats_finished_tests), ""
                        + getNumberOfFinishedTests());
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // The average question reached:
        tvText = String.format(mContext.getString(R.string.stats_average_nrq),
                "" + getAverageQuestionsPerTest());
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // Number of total points:
        tvText = String.format(mContext.getString(R.string.stats_total_points),
                st.getNumberOfPointsAsString(getNumberOfTotalPoints()));
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // The average points per test:
        tvText = String.format(
                mContext.getString(R.string.stats_average_points), ""
                        + getAveragePointsPerTest());
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // Number of won tests:
        tvText = String.format(mContext.getString(R.string.stats_won_tests), ""
                + getNumberOfWonTests());
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // Determine now the average duration:
        tvText = String.format(
                mContext.getString(R.string.stats_average_duration), ""
                        + getAverageDurationInSeconds());
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // Determine now the total duration:
        tvText = String.format(
                mContext.getString(R.string.stats_total_duration),
                getTotalDurationAsString());
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // Get now the number of start menu appearances:
        Settings set = new Settings(mContext);
        int nrLaunches = set.getIntSettings("numberOfLaunches");
        tvText = String.format(
                mContext.getString(R.string.stats_main_menu_launches), ""
                        + nrLaunches);
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // Show here the last tests finished:
        // tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText("");
        ll.addView(tv, tvParam);

        tvText = mContext.getString(R.string.stats_last_tests_title);
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // Now a for to show the last tests:
        // Query the IDs for last tests:
        int nrTestsToShow = 5;
        String sql = "SELECT id FROM statistici ORDER BY data DESC LIMIT "
                + nrTestsToShow + ";";
        Cursor cursor = mDbHelper2.queryData(sql);
        // Only if the cursor contains some tests:
        if (cursor.getCount() > 0) {
            // Make text views in a do ... while:
            cursor.moveToFirst();
            do {
                tvText = getTestById(cursor.getInt(0));
                tvSeq = MyHtml.fromHtml(tvText);
                tv = createTextView();
                tv.setText(tvSeq);
                ll.addView(tv, tvParam);
            } while (cursor.moveToNext());
        } else { // no records found:
            tvText = mContext.getString(R.string.stats_no_records_found);
            tvSeq = MyHtml.fromHtml(tvText);
            tv = createTextView();
            tv.setText(tvSeq);
            ll.addView(tv, tvParam);
        } // end if no records found for last tests.
        // End show last solved tests.

        // Show here the best tests finished:
        // tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText("");
        ll.addView(tv, tvParam);

        tvText = mContext.getString(R.string.stats_best_tests_title);
        tvSeq = MyHtml.fromHtml(tvText);
        tv = createTextView();
        tv.setText(tvSeq);
        ll.addView(tv, tvParam);

        // Now a for to show the best tests:
        // Query the IDs for best tests:
        sql = "SELECT id FROM statistici ORDER BY puncte DESC, durata ASC LIMIT "
                + nrTestsToShow + ";";
        cursor = mDbHelper2.queryData(sql);
        // Only if the cursor contains some tests:
        if (cursor.getCount() > 0) {
            // Make text views in a do ... while:
            boolean theBestWasMemorized = false;
            int tempBestId = 1; // just a simple value.
            cursor.moveToFirst();
            do {
                int tempId = cursor.getInt(0);
                if (!theBestWasMemorized) {
                    tempBestId = tempId;
                    theBestWasMemorized = true;
                }
                tvText = getTestById(tempId);
                tvSeq = MyHtml.fromHtml(tvText);
                tv = createTextView();
                tv.setText(tvSeq);
                ll.addView(tv, tvParam);
            } while (cursor.moveToNext());
            final int bestId = tempBestId;

            // tvSeq = MyHtml.fromHtml(tvText);
            tv = createTextView();
            tv.setText("");
            ll.addView(tv, tvParam);

            // Create the button to send the best set:
            // We need a LayoutParam to add the button at the centre:
            LinearLayout.LayoutParams btParam = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            btParam.gravity = Gravity.CENTER_HORIZONTAL;

            Button btSend = new Button(mContext);
            btSend.setTextSize(MainActivity.textSize);
            btSend.setText(mContext.getString(R.string.bt_send_best_test));
            btSend.setOnClickListener(view -> {
                // Only if there is an Internet connection:
                if (GUITools.isNetworkAvailable(mContext)) {
                    checkIfWasSent(bestId);
                } else {
                    GUITools.alert(mContext, mContext
                            .getString(R.string.warning), mContext
                            .getString(R.string.no_internet_is_available));
                } // end if no Internet is available.
            }); // end onClickListener.
            ll.addView(btSend, btParam);

            // Create the button to get current position:
            Button btGetPosition = new Button(mContext);
            btGetPosition.setTextSize(MainActivity.textSize);
            btGetPosition.setText(mContext
                    .getString(R.string.bt_get_best_test_position));
            btGetPosition.setOnClickListener(view -> {
                // Only if there is an Internet connection:
                if (GUITools.isNetworkAvailable(mContext)) {
                    getBetsTestPosition();
                } else {
                    GUITools.alert(mContext, mContext
                            .getString(R.string.warning), mContext
                            .getString(R.string.no_internet_is_available));
                } // end if no Internet is available.
            }); // end onClickListener.
            ll.addView(btGetPosition, btParam);

        } else { // no records found:
            tvText = mContext.getString(R.string.stats_no_records_found);
            tvSeq = MyHtml.fromHtml(tvText);
            tv = createTextView();
            tv.setText(tvSeq);
            ll.addView(tv, tvParam);
        } // end if no records found for last tests.
        // End show best solved tests.

        // Create the button to go to general statistics:
        // This button is available only if is not Android TV:
        if (!MainActivity.isTV) {
            // We need a LayoutParam to add the button at the right:
            LinearLayout.LayoutParams btParam = new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            btParam.gravity = Gravity.RIGHT;

            Button btGS = new Button(mContext);
            btGS.setTextSize(MainActivity.textSize);
            btGS.setText(mContext.getString(R.string.bt_web_statistics));
            btGS.setOnClickListener(view -> GUITools.openBrowser(mContext,
                    "http://www.android.pontes.ro/cg/")); // end onClickListener.
            ll.addView(btGS, btParam);
        } // end if it is not Android TV.

        // Add the linear layout into SV layout.
        sv.addView(ll);

        // The alert dialog is created and shown:
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setTitle(mContext.getString(R.string.statistics_title));
        alertDialog.setView(sv);

        alertDialog.setPositiveButton(
                mContext.getString(R.string.close_statistics),
                (dialog, whichButton) -> {
                    // Do nothing.
                });

        alertDialog.create();
        alertDialog.show();
    } // end showStats() method.

    // A method to take number of finished tests:
    private int getNumberOfFinishedTests() {
        // Get number of tests:
        String sql = "SELECT COUNT(*) FROM statistici;";
        Cursor cursor = mDbHelper2.queryData(sql);
        return cursor.getInt(0);
    } // end getNumberOfFinishedTests() method.

    // A method to take number of total points:
    private int getNumberOfTotalPoints() {
        // Get now the number of points:
        String sql = "SELECT SUM(puncte) FROM statistici;";
        Cursor cursor = mDbHelper2.queryData(sql);
        return cursor.getInt(0);
    } // end getNumberOfFinishedTests() method.

    // A method to take number of won tests:
    private int getNumberOfWonTests() {
        // Get number of tests:
        String sql = "SELECT COUNT(*) FROM statistici WHERE nrq=21;";
        Cursor cursor = mDbHelper2.queryData(sql);
        return cursor.getInt(0);
    } // end getNumberOfWonTests() method.

    // Determine the average question reached per test:
    private double getAverageQuestionsPerTest() {
        String sql = "SELECT AVG(nrq) FROM statistici;";
        Cursor cursor = mDbHelper2.queryData(sql);
        double avg = cursor.getFloat(0);
        avg = GUITools.round(avg, 2);
        return avg;
    } // end getAverageQuestionsPerTest() method.

    // Determine the average points per test:
    private double getAveragePointsPerTest() {
        String sql = "SELECT AVG(puncte) FROM statistici;";
        Cursor cursor = mDbHelper2.queryData(sql);
        double avg = cursor.getFloat(0);
        avg = GUITools.round(avg, 2);
        return avg;
    } // end getAveragePointsPerTest() method.

    // A method to take the first test date:
    private String getFirstTestDate() {
        String sql = "SELECT data FROM statistici ORDER BY data ASC LIMIT 1;";
        Cursor cursor = mDbHelper2.queryData(sql);

        String temp = mContext.getString(R.string.stats_no_records_found);
        // If there is a record:
        if (cursor.getCount() > 0) {
            int firstTimeStamp = cursor.getInt(0);
            temp = GUITools.timeStampToString(mContext, firstTimeStamp);
        } // end if there is at least a record.

        return temp;
    } // end getFirstTestDate() method.

    // A method to get the duration of playing:
    private String getTotalDurationAsString() {
        // Determine total seconds in database:
        String sql = "SELECT SUM(durata) FROM statistici;";
        Cursor cursor = mDbHelper2.queryData(sql);
        int totalSeconds = cursor.getInt(0);

        int days = totalSeconds / (60 * 60 * 24);
        int rest = totalSeconds % (60 * 60 * 24);
        int hours = rest / (60 * 60);
        rest = rest % (60 * 60);
        int minutes = rest / 60;
        rest = rest % 60;
        int seconds = rest;

        return String.format(
                mContext.getString(R.string.stats_duration_as_string), ""
                        + days, "" + hours, "" + minutes, "" + seconds);
    } // end getTotalDurationAsString() method.

    // A method to get the average duration in seconds:
    private int getAverageDurationInSeconds() {
        String sql = "SELECT AVG(durata) FROM statistici;";
        Cursor cursor = mDbHelper2.queryData(sql);
        double avg = cursor.getFloat(0);
        avg = GUITools.round(avg, 0);
        return (int) avg;
    } // end getAverageDurationInSeconds() method.

    // here a method to determine the position of the test in local DB.
    public String getTestPositionInStats(int points, int duration) {
        String msg;

        String sql = "SELECT COUNT(*) FROM statistici WHERE puncte>" + points
                + ";";
        Cursor cursor = mDbHelper2.queryData(sql);

        if (cursor.getCount() == 1) {
            // Get the number of better tests, the cursor unique result:
            int betterTests = cursor.getInt(0);
            // The position is the next after the total better tests:
            int position = betterTests + 1;
            // Format the string to be returned:
            if (position == 1) { // record:
                msg = mContext.getString(R.string.tv_status_record);
            } else { // no record, normal position:
                msg = String.format(
                        mContext.getString(R.string.tv_status_test_position),
                        "" + position);
            } // end if else to determine record or position.
        } // End if getCount() is 1, a result for SQL count.
        else { // no count result, error:
            msg = mContext.getString(R.string.stats_error_get_test_position);
        }

        return msg;
    } // end getTestPositionInStats() method.

    // A method to create a text view for items in this alert:
    private TextView createTextView() {
        TextView tv = new TextView(mContext);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);

        return tv;
    } // end createTextView() method.
    // end show statistics in a alert methods.

    public void postFinishedTestLocally(String sets, int nrq, int points,
                                        int duration, int androidTV, String usedOptions, int wasPostedOnline) {
        // Determine local time:
        long curTime = GUITools.getTimeInSeconds();

        // We format the SQL string to insert a finished game:
        String sql = "INSERT INTO statistici (random_id, seturi, nrq, puncte, durata, android_tv, optiuni_utilizate, data, postat_online) VALUES ('"
                + MainActivity.randomId
                + "', '"
                + sets
                + "', '"
                + nrq
                + "', '"
                + points
                + "', '"
                + duration
                + "', '"
                + androidTV
                + "', '"
                + usedOptions
                + "', '"
                + curTime
                + "', '"
                + wasPostedOnline
                + "');";
        mDbHelper2.insertData(sql);
    } // end postFinishedTestLocaly() method.

    public void postFinishedTestOnline(String sets, int nrq, int points,
                                       int duration, int androidTV, String usedOptions) {
        String url = "http://www.android.pontes.ro/cg/insert_stats.php?random_id="
                + MainActivity.randomId
                + "&seturi="
                + sets
                + "&nrq="
                + nrq
                + "&puncte="
                + points
                + "&durata="
                + duration
                + "&android_tv="
                + androidTV + "&optiuni_utilizate=" + usedOptions;
        new GetWebData().execute(url);
    } // end PostFinishedTest() method.

    // A method to post test online from local DB:
    public void postOnlineNotPostedFinishedTests() {
        // If there are test in local DB not posted:
        String sql = "SELECT * FROM statistici WHERE postat_online=0;";
        Cursor cursor = mDbHelper2.queryData(sql);
        int nrNotPosted = cursor.getCount();
        // GUITools.alert(mContext, "ABC", "" + nrNotPosted);
        if (nrNotPosted > 0) {
            /*
             * We try to post maximum 3 tests at a time if they are available.
             * Only if an Internet connection is available:
             */
            if (GUITools.isNetworkAvailable(mContext)) {
                cursor.moveToFirst();
                for (int i = 0; i < 3 && i < nrNotPosted; i++) {
                    // We create the variables for each value from cursor:
                    int id = cursor.getInt(0);
                    String sets = cursor.getString(2);
                    int nrq = cursor.getInt(3);
                    int points = cursor.getInt(4);
                    int duration = cursor.getInt(5);
                    int androidTV = cursor.getInt(6);
                    String usedOptions = cursor.getString(7);

                    // Make current records a posted on-line in local DB:
                    sql = "UPDATE statistici SET postat_online=1 WHERE id="
                            + id + ";";
                    mDbHelper2.updateData(sql);

                    // Post it effectively:
                    postFinishedTestOnline(sets, nrq, points, duration,
                            androidTV, usedOptions);
                    cursor.moveToNext();
                } // end for 3 posted tests.
            } // end if there is an Internet connection available.
        } // end if there are not posted tests in local DB.
    } // end postOnlineUnpostedFinishedTests() method.

    // Methods for showing tests like last or best:
    private String getTestById(int id) {
        String sql = "SELECT * FROM statistici WHERE id=" + id + ";";
        Cursor cursor = mDbHelper2.queryData(sql);
        // Let's take the information from cursor:
        // String sets = cursor.getString(2);
        int nrq = cursor.getInt(3);
        int points = cursor.getInt(4);
        int duration = cursor.getInt(5);
        int date = cursor.getInt(8);

        return String.format(
                mContext.getString(R.string.stats_test_info),
                GUITools.timeStampToString(mContext, date), "" + nrq, ""
                        + points, "" + duration);
    } // end getTestFromHistoryById() method.

    // This is a subclass:
    private static class GetWebData extends AsyncTask<String, String, String> {

        // execute before task:
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        // Execute task
        String urlText = "";

        @Override
        protected String doInBackground(String... strings) {
            StringBuilder content = new StringBuilder();
            urlText = strings[0];
            try {
                // Create a URL object:
                URL url = new URL(urlText);
                // Create a URLConnection object:
                URLConnection urlConnection = url.openConnection();
                // Wrap the URLConnection in a BufferedReader:
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream()));
                String line;
                // Read from the URLConnection via the BufferedReader:
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line);
                }
                bufferedReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return content.toString();
        } // end doInBackground() method.

        // Execute after task with the task result as string:
        @Override
        protected void onPostExecute(String s) {
            // Do nothing yet.
        } // end postExecute() method.
    } // end subclass.

    // This is another subclass for top table:
    private class GetWebTop extends AsyncTask<String, String, String> {
        private ProgressDialog pd;

        // execute before task:

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(mContext);
            pd.setMessage(mContext
                    .getString(R.string.stats_please_wait_sending));
            pd.setIndeterminate(false);
            pd.setCancelable(true);
            pd.show();
        } // end onPreExecute() method.

        // Execute task
        String urlText = "";

        @Override
        protected String doInBackground(String... strings) {
            StringBuilder content = new StringBuilder();
            urlText = strings[0];
            try {
                // Create a URL object:
                URL url = new URL(urlText);
                // Create a URLConnection object:
                URLConnection urlConnection = url.openConnection();
                // Wrap the URLConnection in a BufferedReader:
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream()));
                String line;
                // Read from the URLConnection via the BufferedReader:
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line);
                }
                bufferedReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return content.toString();
        } // end doInBackground() method.

        // Execute after task with the task result as string:
        @Override
        protected void onPostExecute(String s) {
            pd.dismiss();
            afterSendingBestResult(s);
        } // end postExecute() method.
    } // end subclass for top.

    // Methods to send a result for top:
    // A method which checks if the best test was not already sent:
    private void checkIfWasSent(final int id) {
        String sql = "SELECT trimis_top FROM statistici WHERE id=" + id + ";";
        Cursor cursor = mDbHelper2.queryData(sql);
        int wasSent = cursor.getInt(0);
        if (wasSent == 1) { // already sent:
            GUITools.alert(mContext, mContext.getString(R.string.warning),
                    mContext.getString(R.string.stats_test_was_already_sent));
        } else { // continue sending:
            // Here an alert for nickname:
            AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
            // The title:
            alert.setTitle(mContext.getString(R.string.nickname_title));

            // The body in a ScrollView and LinearLayout:
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

            // A LayoutParams to add next items into addLLMain:
            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            // The text view where we say about nickname:
            TextView tv = new TextView(mContext);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            String tempMessage = mContext.getString(R.string.nickname_body);
            tv.setText(tempMessage);
            ll.addView(tv, llParams);

            // A blank line:
            tv = createTextView();
            tv.setText("");

            ll.addView(tv, llParams);
            // Add an EditText view to get user input
            final EditText input = new EditText(mContext);
            input.setTextSize(MainActivity.textSize);
            input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            input.setHint(mContext.getString(R.string.nickname_hint));
            // Add also an action listener:
            input.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Do nothing.
                }
                return false;
            });
            // End add action listener for the IME done button of the keyboard..

            ll.addView(input, llParams);
            // Add the LinearLayout into ScrollView:
            sv.addView(ll);
            alert.setView(sv);
            // end if OK was pressed.
            alert.setPositiveButton(mContext.getString(R.string.bt_send),
                    (dialog, whichButton) -> {
                        // Next two line are also at the done button:
                        String nickname = input.getText().toString();
                        // Check if the nickname has enough characters:
                        if (nickname.length() < 4 || nickname.length() > 20) { // wrong:
                            GUITools.alert(
                                    mContext,
                                    mContext.getString(R.string.error),
                                    mContext.getString(R.string.nickname_wrong_message));
                        } else { // correct:
                            beforeSendingResultForTop(id, nickname);
                        } // end correct nickname.
                    });

            alert.setNegativeButton(mContext.getString(R.string.cancel),
                    (dialog, whichButton) -> {
                        // Cancelled, do nothing yet.
                    });

            alert.create();
            alert.show();
            // end of alert dialog with edit sequence.
            // end alert for nickname.
        } // end if was not already sent.
    } // end checkIfWasSent() method.

    private void beforeSendingResultForTop(int id, String nickname) {
        // Extract the result form DB:
        String sql = "SELECT * FROM statistici WHERE id=" + id + ";";
        Cursor cursor = mDbHelper2.queryData(sql);
        // We create the variables for each value from cursor:
        String sets = cursor.getString(2);
        int nrq = cursor.getInt(3);
        int points = cursor.getInt(4);
        int duration = cursor.getInt(5);
        int androidTV = cursor.getInt(6);
        String usedOptions = cursor.getString(7);

        // Make current records a sent to top in local DB:
        sql = "UPDATE statistici SET trimis_top=1 WHERE id=" + id + ";";
        mDbHelper2.updateData(sql);

        // Send it effectively:
        postTestForTopOnline(nickname, sets, nrq, points, duration, androidTV,
                usedOptions);
    } // end beforeSendingResultForTop() method.

    /*
     * This method sends the test for top effectively, it happens before the
     * subclass:
     */
    private void postTestForTopOnline(String nickname, String sets, int nrq,
                                      int points, int duration, int androidTV, String usedOptions) {
        String url = "http://www.android.pontes.ro/cg/insert_top.php?random_id="
                + MainActivity.randomId
                + "&porecla="
                + nickname
                + "&seturi="
                + sets
                + "&nrq="
                + nrq
                + "&puncte="
                + points
                + "&durata="
                + duration
                + "&android_tv="
                + androidTV
                + "&optiuni_utilizate="
                + usedOptions;
        new GetWebTop().execute(url);
    } // end postTestForTopOnline() method.
    // End method before asynchronous task in subclass.

    // Method to be used after post execute:

    private void afterSendingBestResult(String gotS) {
        SoundPlayer.playSimple(mContext, "send_something");
        // Check if there is an error:
        if (gotS.equalsIgnoreCase("error")) { // an error occurred:
            GUITools.alert(mContext, mContext.getString(R.string.error),
                    mContext.getString(R.string.error_occurred));
        } else { // no error:
            int pos = Integer.parseInt(gotS);
            pos = pos + 1;
            String body = String.format(
                    mContext.getString(R.string.stats_result_sent_body), ""
                            + pos);
            GUITools.alert(mContext,
                    mContext.getString(R.string.stats_result_sent_title), body);
        } // end if no error.
    } // end afterSendingBestResult() method.

    // Methods to get current position of the best test sent:
    // This is another subclass for top table, getting best position:
    private class GetBestPosition extends AsyncTask<String, String, String> {
        private ProgressDialog pd;

        // execute before task:

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(mContext);
            pd.setMessage(mContext
                    .getString(R.string.stats_please_wait_getting));
            pd.setIndeterminate(false);
            pd.setCancelable(true);
            pd.show();
        } // end onPreExecute() method.

        // Execute task
        String urlText = "";

        @Override
        protected String doInBackground(String... strings) {
            StringBuilder content = new StringBuilder();
            urlText = strings[0];
            try {
                // Create a URL object:
                URL url = new URL(urlText);
                // Create a URLConnection object:
                URLConnection urlConnection = url.openConnection();
                // Wrap the URLConnection in a BufferedReader:
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(urlConnection.getInputStream()));
                String line;
                // Read from the URLConnection via the BufferedReader:
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line);
                }
                bufferedReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return content.toString();
        } // end doInBackground() method.

        // Execute after task with the task result as string:
        @Override
        protected void onPostExecute(String s) {
            pd.dismiss();
            afterGettingBestResultPosition(s);
        } // end postExecute() method.
    } // end subclass for top best test position.

    /*
     * This method sends the test for top effectively, it happens before the
     * subclass:
     */
    private void getBetsTestPosition() {
        String url = "http://www.android.pontes.ro/cg/get_top.php?random_id="
                + MainActivity.randomId;
        new GetBestPosition().execute(url);
    } // end getBetsTestPosition() method, is before asynchronous.

    private void afterGettingBestResultPosition(String s) {
        // We analyse now what we receive via string:
        String msgBody;
        String msgTitle;
        // First, if no record found in top on-line:
        if (s.equalsIgnoreCase("nothing")) {
            msgTitle = mContext.getString(R.string.warning);
            msgBody = mContext
                    .getString(R.string.stats_no_results_found_in_top);
        } else if (s.equalsIgnoreCase("error")) {
            msgTitle = mContext.getString(R.string.error);
            msgBody = mContext
                    .getString(R.string.stats_error_get_sent_result_position);
        } // end if another error.
        else { // another string received:
            /*
             * We thing we have a delimited string by vertical bars and we split
             * it:
             */
            String[] arrS = s.split("\\|");
            // We need to be a 6 in length array:
            if (arrS.length == 6) {
                msgTitle = mContext
                        .getString(R.string.stats_current_top_position_title);

                // We format the string:
                /*
                 * First we format the test info, after we include in string
                 * with the position:
                 */
                String testInfo = mContext
                        .getString(R.string.stats_test_info_best_result);
                testInfo = String.format(
                        testInfo,
                        GUITools.timeStampToString(mContext,
                                Integer.parseInt(arrS[5])), arrS[2], arrS[3],
                        arrS[4], arrS[1]);

                msgBody = MyHtml
                        .fromHtml(
                                String.format(
                                        mContext.getString(R.string.stats_current_top_position_body),
                                        arrS[0], testInfo)).toString();

            } else { // another error, no 6 items in arrS:
                msgTitle = mContext.getString(R.string.error);
                msgBody = mContext.getString(R.string.error_occurred);
            } // end if arrS is not 6 in length.
        } // end if another string, no error or nothing.

        // We show now finally the alert:
        GUITools.alert(mContext, msgTitle, msgBody);
    } // end afterGettingBestResultPosition() method.
} // end Statistics class.

