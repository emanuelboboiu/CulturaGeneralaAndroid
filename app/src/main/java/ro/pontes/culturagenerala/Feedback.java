package ro.pontes.culturagenerala;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Feedback {

    private final Context context;
    private final int questionId;
    private int errorType = 0;

    // We need a constructor for context and mDB initiate:
    public Feedback(Context context, int questionId) {
        this.context = context;
        this.questionId = questionId;
    } // end constructor.

    // A method which sends a feedback about a question:
    @SuppressLint("InflateParams")
    public void feedbackStart() {
        if (GUITools.isNetworkAvailable(context)) {
            // create an alert inflating an XML:
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View feedbackView = inflater.inflate(R.layout.feedback_dialog, null);

            // Get the strings to make an alert:
            String tempTitle = context.getString(R.string.title_feedback_dialog);

            // Get the linear layout to add controls there:
            LinearLayout ll = feedbackView.findViewById(R.id.llFeedback);

            // A LayoutParams to add the controls:
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);

            // Add the editText for description:
            final EditText etDescription = new EditText(context);
            etDescription.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            etDescription.setHint(context.getString(R.string.hint_feedback));
            etDescription.setInputType(InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE);
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(350);
            etDescription.setFilters(filterArray);
            etDescription.setImeOptions(EditorInfo.IME_ACTION_DONE);
            ll.addView(etDescription, lp);

            // Now add radio buttons for error type:
            RadioGroup radioGroup = new RadioGroup(context);
            RadioButton rb0 = new RadioButton(context);
            rb0.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            rb0.setText(context.getString(R.string.rb_feedback_0));
            rb0.setFocusable(true);
            rb0.setOnClickListener(v -> errorType = 0);
            radioGroup.addView(rb0, lp);

            RadioButton rb1 = new RadioButton(context);
            rb1.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            rb1.setText(context.getString(R.string.rb_feedback_1));
            rb1.setFocusable(true);
            rb1.setOnClickListener(v -> errorType = 1);
            radioGroup.addView(rb1, lp);

            RadioButton rb2 = new RadioButton(context);
            rb2.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            rb2.setText(context.getString(R.string.rb_feedback_2));
            rb2.setFocusable(true);
            rb2.setOnClickListener(v -> errorType = 2);
            radioGroup.addView(rb2, lp);

            RadioButton rb3 = new RadioButton(context);
            rb3.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            rb3.setText(context.getString(R.string.rb_feedback_3));
            rb3.setFocusable(true);
            rb3.setOnClickListener(v -> errorType = 3);
            radioGroup.addView(rb3, lp);

            ll.addView(radioGroup, lp);

            // end if send button was pressed.
            AlertDialog.Builder alert = new AlertDialog.Builder(context).setTitle(tempTitle).setView(feedbackView).setPositiveButton(R.string.bt_send, (dialog, whichButton) -> {
                String newDescription = etDescription.getText().toString();
                sendAdd(errorType, newDescription);
            }).setNegativeButton(android.R.string.cancel, null);

            alert.create();
            alert.show();
        } else {
            GUITools.alert(context, context.getString(R.string.warning), context.getString(R.string.no_connection_for_feedback));
        }
    } // end send feedback method.

    // A method to send effectively the description and error type from method
    // above:
    private void sendAdd(int type, String description) {
        // Save now the new record:
        // First of all, check if the edit text has text:
        String newDescription = (MyHtml.fromHtml(description).toString()).trim();
        if (newDescription.length() >= 2) {
            // Add here into online database:

            String encodedDescription = newDescription.replace(" ", "%20");

            String url = "https://android.pontes.ro/cg/insert_feedback.php?id_intrebare=" + questionId + "&tip_eroare=" + type + "&random_id=" + MainActivity.randomId + "&descriere=" + encodedDescription;
            new SendFeedback().execute(url);
        } // end if the length are OK.
        else {
            GUITools.alert(context, context.getString(R.string.warning), context.getString(R.string.no_texts_for_description));
        } // end if edit text haven't text.
    } // end send new add Edit() method.

    // A subclass to send data to server:
    private class SendFeedback extends AsyncTask<String, String, String> {
        private ProgressDialog pd;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(context);
            pd.setMessage(context.getString(R.string.please_wait_sending_feedback));
            pd.setIndeterminate(false);
            pd.setCancelable(true);
            pd.show();
        } // end onPreExecute() method.

        @Override
        protected String doInBackground(String... params) {
            StringBuilder content = new StringBuilder();
            String urlString = params[0]; // URL to call
            try {
                // Create a URL object:
                URL url = new URL(urlString);
                // Create a URLConnection object:
                URLConnection urlConnection = url.openConnection();
                // Wrap the URLConnection in a BufferedReader:
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                // Read from the URLConnection via the BufferedReader:
                while ((line = bufferedReader.readLine()) != null) {
                    content.append(line);
                }
                bufferedReader.close();
            } catch (Exception e) {
                // e.printStackTrace();
            }
            return content.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            // Clear progress dialog:
            pd.dismiss();

            if (result.contains("succes")) {
                SoundPlayer.playSimple(context, "send_something");
                GUITools.toast(context.getString(R.string.feedback_sent_successfully), 2000, context);
            } else {
                GUITools.toast(context.getString(R.string.feedback_sent_unsuccessfully), 2000, context);
            }
        } // end postExecute() method.

    } // end subclass SendFeedback.

} // end Feedback class.
