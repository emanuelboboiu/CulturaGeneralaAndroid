package ro.pontes.culturagenerala;

import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DBAdapter {

    // protected static final String TAG = "DataAdapter";

    public SQLiteDatabase mDb;
    private final DataBaseHelper mDbHelper;

    public DBAdapter(Context context) {
        mDbHelper = new DataBaseHelper(context);
    }

    public void createDatabase() throws SQLException {
        try {
            mDbHelper.createDataBase();
        } catch (IOException mIOException) {
            // Log.e(TAG, mIOException.toString() + "  UnableToCreateDatabase");
            throw new Error("UnableToCreateDatabase");
        }
    }

    public void open() throws SQLException {
        mDbHelper.openDataBase();
        mDbHelper.close();
        mDb = mDbHelper.getWritableDatabase();
    }

    public Cursor queryData(String sql) {
        Cursor mCur = mDb.rawQuery(sql, null);
        if (mCur != null) {
            mCur.moveToNext();
        }
        return mCur;
    }

    // A method to update a table:
    public void updateData(String sql) {
        mDb.execSQL(sql);
    } // end update data.

} // end class DBAdapter.
