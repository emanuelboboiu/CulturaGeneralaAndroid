package ro.pontes.culturagenerala;

import java.io.IOException;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class DBAdapter2 {

    public SQLiteDatabase mDb;
    private final DataBaseHelper2 mDbHelper;

    public DBAdapter2(Context context) {
        mDbHelper = new DataBaseHelper2(context);
    } // end constructor.

    public void createDatabase() throws SQLException {
        try {
            mDbHelper.createDataBase();
        } catch (IOException mIOException) {
            throw new Error("UnableToCreateDatabase");
        }
    }

    public void open() throws SQLException {
        mDbHelper.openDataBase();
        mDbHelper.close();
        mDb = mDbHelper.getWritableDatabase();
        mDb.execSQL("PRAGMA foreign_keys=ON;");
    }

    public Cursor queryData(String sql) {
        Cursor mCur = mDb.rawQuery(sql, null);
        if (mCur != null) {
            mCur.moveToNext();
        }
        return mCur;
    }

    // A method to to execute SQL code:
    public void executeSQLCode(String sql) {
        mDb.execSQL(sql);
    } // end executeSQLCode() method.

    // A method to update a table:
    public void updateData(String sql) {
        mDb.execSQL(sql);
    } // end update data.

    // A method to insert into a table:
    public void insertData(String sql) {
        mDb.execSQL(sql);
    } // end insert data.

} // end DBAdapter2 class, for using my vocabulary.
