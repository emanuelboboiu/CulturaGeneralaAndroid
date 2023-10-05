package ro.pontes.culturagenerala;

import android.content.Context;
import android.content.res.Resources;

public class StringTools {

    private final Resources res;

    // A constructor for context and number of points:
    public StringTools(Context context) {
        res = context.getResources();
    } // end constructor.

    public String getNumberOfPointsAsString(int nrOfPoints) {

        // First take the corresponding plural resource:
        return res.getQuantityString(R.plurals.msg_number_of_points, nrOfPoints, nrOfPoints);
    } // end getNumberOfPointsAsString() method.

} // end StringTools class.
