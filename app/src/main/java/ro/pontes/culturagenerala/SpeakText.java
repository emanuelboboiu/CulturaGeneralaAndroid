package ro.pontes.culturagenerala;

import android.content.Context;
import android.os.Handler;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

public class SpeakText {

    private TextToSpeech mTTS;
    private final boolean speakOthers;

    // The constructor:
    public SpeakText(Context context) {
        // For TextToSpeech:
        Settings set = new Settings(context);
        this.speakOthers = set.getBooleanSettings("speakOthers");
        mTTS = new TextToSpeech(context, status -> {
            if (status != TextToSpeech.ERROR) {
                if (mTTS != null) {
                    Locale romanianLocale = new Locale("ro", "RO");
                    mTTS.setLanguage(romanianLocale);
                } // end if mTTS isn't null..
            }
        });
        // end for TextToSpeech.
    } // end constructor.

    public void say(final String toSay, final boolean interrupt) {
        if (MainActivity.isSpeech) {
            int speakMode;
            if (interrupt) {
                speakMode = TextToSpeech.QUEUE_FLUSH;
            } else {
                speakMode = TextToSpeech.QUEUE_ADD;
            } // end if is not interruption.
            mTTS.speak(toSay, speakMode, null, null);
        } // end if isSpeech.
    } // end say method.

    public void sayDelayed(final String toSay, final boolean interrupt) {
        final Handler handler = new Handler();
        handler.postDelayed(() -> {
            // Do something after some milliseconds:
            this.say(toSay, interrupt);
        }, 500);
    } // end sayDelayed() method.

    // Next method is only used in the quiz objects to speak other things than question and variants, only if it is chosen in settings to be spoken:
    public void sayOthers(final String toSay, final boolean interrupt) {
        if (speakOthers) {
            this.sayDelayed(toSay, interrupt);
        } // end if is set to speak other information in quiz.
    } // end sayOthers() method.

    public void speakTest(String text) {
        this.sayDelayed(text, true);
    } // end speakTest() method.

    public void stop() {
        if (mTTS != null) {
            mTTS.stop();
        }
    } // end stop method of the SpeakText class.

} // end SpeakText class.
