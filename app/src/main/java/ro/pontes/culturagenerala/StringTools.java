package ro.pontes.culturagenerala;

import android.content.Context;
import android.content.res.Resources;

public class StringTools {

	private final Context mContext;
	private Resources res = null;

	// A constructor for context and number of points:
	public StringTools(Context context) {
		mContext = context;
		res = mContext.getResources();
	} // end constructor.

	public String getNumberOfPointsAsString(int nrOfPoints) {

		// First take the corresponding plural resource:
		String numberOfPointsMessage = res.getQuantityString(
				R.plurals.msg_number_of_points, nrOfPoints, nrOfPoints);
		return numberOfPointsMessage;
	} // end getNumberOfPointsAsString() method.

} // end StringTools class.
