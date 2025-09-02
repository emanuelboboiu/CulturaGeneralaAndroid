package ro.pontes.culturagenerala;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

// Class started on Sunday, 24 September 2017, created by Emanuel Boboiu.

public class DataBaseHelper extends SQLiteOpenHelper {
    // private static String TAG = "DataBaseHelper"; // Tag just for the LogCat.
    // destination path (location) of our database on device
    private static String DB_PATH = "";
    private static final String DB_NAME = "cultura_generala.db";
    private static final String SP_KEY_DB_VER = "dbVer";
    private static final int DATABASE_VERSION = 48;
    private static final int lastUpdateTimestamp = 1752429600; // 13 July 2025.

    private SQLiteDatabase mDataBase;
    private final Context mContext;

    public DataBaseHelper(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
        // Get the path to our DB:
        DB_PATH = context.getDatabasePath(DB_NAME).getPath();
        this.mContext = context;
        initialise();
    } // end constructor.

    /**
     * Initialises database. Creates database if doesn't exist. Deletes the
     * DataBase if version is different.
     */
    private void initialise() {
        if (checkDataBase()) {
            Settings set = new Settings(mContext);
            int dbVersion = set.getIntSettings(SP_KEY_DB_VER);
            if (DATABASE_VERSION != dbVersion) {
                /*
                 * Before deleting the database we save the ID and CONSUMAT
                 * fields:
                 */
                if (dbVersion > 0) {
                    saveDatabaseUsedQuestions(dbVersion);
                }
                File dbFile = mContext.getDatabasePath(DB_NAME);
                if (!dbFile.delete()) {
                    // Log.w(TAG, "Unable to update database");
                } // end if file was not deleted.
            } // end if the versions are different.
        } // end if DataBase exits.
        if (!checkDataBase()) {
            try {
                createDataBase();
            } catch (IOException e) {
                // e.printStackTrace();
            }
        } // end if DataBase doesn't exist.
    } // end initialise.

    public void createDataBase() throws IOException {
        // If database not exists copy it from the assets

        boolean mDataBaseExist = checkDataBase();
        if (!mDataBaseExist) {
            this.getWritableDatabase();
            this.close();
            try {
                // Copy the database from assests:
                copyDataBase();
                // Log.e(TAG, "createDatabase database created");
            } catch (IOException mIOException) {
                throw new Error("ErrorCopyingDataBase");
            }
        } // end if DataBase doesn't exist.
    } // end createDatabase method.

    // Check that the database exists here: /data/data/your
    // package/databases/DatabaseName:
    private boolean checkDataBase() {
        File dbFile = new File(DB_PATH);
        // Log.v("dbFile", dbFile + "   "+ dbFile.exists());
        return dbFile.exists();
    } // end check if file exists, checkDataBase() method.

    // Copy the database from assets
    private void copyDataBase() throws IOException {
        InputStream mInput = mContext.getAssets().open(DB_NAME);
        String outFileName = DB_PATH;
        OutputStream mOutput = new FileOutputStream(outFileName);
        byte[] mBuffer = new byte[1024];
        int mLength;
        while ((mLength = mInput.read(mBuffer)) > 0) {
            mOutput.write(mBuffer, 0, mLength);
        }
        mOutput.flush();
        mOutput.close();
        mInput.close();
        Settings set = new Settings(mContext);
        set.saveIntSettings(SP_KEY_DB_VER, DATABASE_VERSION);
        set.saveIntSettings("lastUpdate", lastUpdateTimestamp);
        // Set back the consumed values:
        setUsedFieldValuesBack();
    } // end copy database into DB folder.

    // Open the database, so we can query it
    public void openDataBase() throws SQLException {
        String mPath = DB_PATH;
        mDataBase = SQLiteDatabase.openDatabase(mPath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
    }

    @Override
    public synchronized void close() {
        if (mDataBase != null) mDataBase.close();
        super.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Do nothing yet.
    }

    // A method to save into statistics DB the used questions:
    private void saveDatabaseUsedQuestions(int oldDvVersion) {
        // Connect to the cultura_generala DB separately from our adapter class:
        DbOpenAndReadMain mDbHelper = new DbOpenAndReadMain(mContext, DB_NAME, null, oldDvVersion);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        // Extract now all IDs and CONSUMAT field:
        String sql = "SELECT intrebareId FROM intrebari WHERE consumat='1'";

        Cursor cursor;
        cursor = db.rawQuery(sql, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        /*
         * No we have the cursor with IDs of questions with consumed status.
         * Connect to the statistics DB. We do this only we have records in
         * cursor:
         */
        // We make the initial for DB2 because to have it for future:
        DBAdapter2 m2 = new DBAdapter2(mContext);
        m2.createDatabase();
        m2.open();

        if (cursor != null && cursor.getCount() > 0) {
            // We create the table UTILIZATE:
            sql = "CREATE TABLE IF NOT EXISTS utilizate(intrebareId INTEGER);";
            m2.executeSQLCode(sql);

            // Delete all the entries if exist:
            sql = "DELETE FROM utilizate;";
            m2.executeSQLCode(sql);

            // Fill now the table from cursor:
            sql = "INSERT INTO utilizate (intrebareId) VALUES (?);";
            m2.mDb.beginTransaction();
            SQLiteStatement stmt = m2.mDb.compileStatement(sql);

            cursor.moveToFirst();
            do {
                stmt.bindLong(1, cursor.getInt(0));
                stmt.executeInsert();
                stmt.clearBindings();
            } while (cursor.moveToNext());
            m2.mDb.setTransactionSuccessful();
            m2.mDb.endTransaction();
        } // end if there are records with CONSUMAT = 1.
    } // end saveDatabaseUsedQuestions() method.

    // A method which puts back the CONSUMAT values into main DB:
    private void setUsedFieldValuesBack() {
        // Connect to the statistics DB:
        DBAdapter2 m2;
        m2 = new DBAdapter2(mContext);
        m2.createDatabase();
        m2.open();
        /*
         * To be easier, we create the table UTILIZATE if not exists, this way
         * we don't need to check if it exists:
         */
        String sql = "CREATE TABLE IF NOT EXISTS utilizate(intrebareId INTEGER);";
        m2.executeSQLCode(sql);

        // Extract now all IDs from UTILIZATE:
        sql = "SELECT * FROM utilizate;";
        Cursor cursor = m2.queryData(sql);

        /*
         * No we have the cursor with IDs of questions for which the consumed
         * status was 1. Connect to the CULTURA_GENERALA DB only if there are
         * records in cursor:
         */

        if (cursor.getCount() > 0) {
            DBAdapter m1 = new DBAdapter(mContext);
            m1.createDatabase();
            m1.open();

            // Update now the CONSUMAT field of table from cursor:
            m1.mDb.beginTransaction();
            cursor.moveToFirst();
            do {
                m1.updateData("UPDATE intrebari SET consumat=1 WHERE intrebareId=" + cursor.getInt(0));
            } while (cursor.moveToNext());
            m1.mDb.setTransactionSuccessful();
            m1.mDb.endTransaction();
        } // end if there are records in UTILIZATE.
    } // end setUsedFieldValuesBack() method.

} // end DataBaseHelper.
