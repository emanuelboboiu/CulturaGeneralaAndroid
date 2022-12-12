package ro.pontes.culturagenerala;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class DbOpenAndReadMain extends SQLiteOpenHelper {

	public DbOpenAndReadMain(Context context, String name,
			CursorFactory factory, int version) {
		super(context, name, factory, version);

	} // end constructor.

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Nothing yet.
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Nothing yet.
	}

} // end DbOpenAndReadMain class.
