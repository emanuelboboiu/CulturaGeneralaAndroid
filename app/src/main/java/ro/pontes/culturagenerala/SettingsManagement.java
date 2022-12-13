package ro.pontes.culturagenerala;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;

public class SettingsManagement {

    private final Context mContext;
    private View settingsView = null;

    // The constructor:
    public SettingsManagement(Context context) {
        this.mContext = context;
    } // end constructor.

    @SuppressLint("InflateParams")
    public void showSettingsAlert() {
        // Inflate the about message contents
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        settingsView = inflater.inflate(R.layout.activity_settings, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.title_activity_settings);
        builder.setView(settingsView);
        builder.setPositiveButton(mContext.getString(R.string.close_settings),
                (dialog, whichButton) -> SoundPlayer.playSimple(mContext, "element_finished"));
        builder.create();
        builder.show();

        // Not check and uncheck the check boxes:
        setInitialStateOfCheckBoxes();
    } // end showSettingsAlert() method.

    private void setInitialStateOfCheckBoxes() {
        // Check or check the check boxes, depending of current boolean values:
        // For sounds in program:
        CheckBox cbtSoundsSetting = settingsView
                .findViewById(R.id.cbtSoundsSetting);
        cbtSoundsSetting.setChecked(MainActivity.isSound);

        // For music in program:
        CheckBox cbtMusicSetting = settingsView
                .findViewById(R.id.cbtMusicSetting);
        cbtMusicSetting.setChecked(MainActivity.isMusic);

        // For keeping screen awake:
        CheckBox cbtScreenAwakeSetting = settingsView
                .findViewById(R.id.cbtScreenAwakeSetting);
        if (MainActivity.isTV) {
            // For Android TV we need it to be unavailable and unchecked:
            cbtScreenAwakeSetting.setChecked(false);
            cbtScreenAwakeSetting.setEnabled(false);
        } else {
            cbtScreenAwakeSetting.setChecked(MainActivity.isWakeLock);
        }
    } // end setInitialStateOfCheckboxes() method..

} // end SettingsManagement class.
