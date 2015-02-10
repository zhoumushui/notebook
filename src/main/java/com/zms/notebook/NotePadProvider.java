
package com.zms.notebook;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentProvider.PipeDataWriter;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;

import com.zms.notebook.NotePad.Notes;

import java.util.Calendar;
import java.util.HashMap;

public class NotePadProvider extends ContentProvider implements
        PipeDataWriter<Cursor> {
    private static final String TAG = "NotePadProvider";
    private static final String DATABASE_NAME = "note_pad.db";
    private static final int DATABASE_VERSION = 2;
    private static HashMap<String, String> sNotesProjectionMap;
    private static HashMap<String, String> sLiveFolderProjectionMap;
    private static final int READ_NOTE_NOTE_INDEX = 1;
    private static final int READ_NOTE_TITLE_INDEX = 2;
    private static final int NOTES = 1;
    private static final int NOTE_ID = 2;
    private static final int LIVE_FOLDER_NOTES = 3;
    private static final UriMatcher S_MATCHER;
    private DatabaseHelper mOpenHelper;

    static {
        S_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        S_MATCHER.addURI(NotePad.AUTHORITY, "notes", NOTES); 
        S_MATCHER.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);
        S_MATCHER.addURI(NotePad.AUTHORITY, "live_folders/notes",
                LIVE_FOLDER_NOTES);
        sNotesProjectionMap = new HashMap<String, String>();
        sNotesProjectionMap.put(BaseColumns._ID, BaseColumns._ID);
        sNotesProjectionMap.put(Notes.COLUMN_NAME_TITLE,
                Notes.COLUMN_NAME_TITLE);
        sNotesProjectionMap.put(Notes.COLUMN_NAME_NOTE, Notes.COLUMN_NAME_NOTE);
        sNotesProjectionMap.put(Notes.COLUMN_NAME_GROUP,
                Notes.COLUMN_NAME_GROUP);
        sNotesProjectionMap.put(Notes.COLUMN_NAME_CREATE_DATE,
                Notes.COLUMN_NAME_CREATE_DATE);
        sNotesProjectionMap.put(Notes.COLUMN_NAME_MODIFICATION_DATE,
                Notes.COLUMN_NAME_MODIFICATION_DATE);
        sLiveFolderProjectionMap = new HashMap<String, String>();
    }

   static class DatabaseHelper extends SQLiteOpenHelper {
       DatabaseHelper(Context context) {
           super(context, DATABASE_NAME, null, DATABASE_VERSION);
       }

       @Override
       public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + Notes.TABLE_NAME + " (" + BaseColumns._ID
                    + " INTEGER PRIMARY KEY," + Notes.COLUMN_NAME_TITLE
                    + " TEXT," + Notes.COLUMN_NAME_NOTE + " TEXT,"
                   + Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                   + Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                    + Notes.COLUMN_NAME_GROUP + " TEXT" + ");");
       }

       @Override
       public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            /*
             * Log.w(TAG, "Upgrading database from version " + oldVersion +
             * " to " + newVersion + ", which will destroy all old data");
             * db.execSQL("DROP TABLE IF EXISTS notes"); onCreate(db);
             */
       }
   }

   @Override
   public boolean onCreate() {
       mOpenHelper = new DatabaseHelper(getContext());
       return true;
   }

   @Override
    public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
       SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
       qb.setTables(Notes.TABLE_NAME);
       switch (S_MATCHER.match(uri)) {
           case NOTES:
               qb.setProjectionMap(sNotesProjectionMap);
               break;
           case NOTE_ID:
               qb.setProjectionMap(sNotesProjectionMap);
            qb.appendWhere(BaseColumns._ID + "="
                    + uri.getPathSegments().get(Notes.NOTE_ID_PATH_POSITION));
               break;          
           default:
               throw new IllegalArgumentException("Unknown URI " + uri);
       }
       SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null,
                null, sortOrder);
       c.setNotificationUri(getContext().getContentResolver(), uri);
       return c;
   }

   @Override
   public String getType(Uri uri) {
       return Notes.CONTENT_ITEM_TYPE;
    }

    static ClipDescription sNoteStreamTypes = new ClipDescription(null,
            new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN });

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri,
            String mimeType, Bundle opts, Cursor c) {
        /*
         * FileOutputStream fout = new
         * FileOutputStream(output.getFileDescriptor()); PrintWriter pw = null;
         * try { pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
         * pw.println(c.getString(READ_NOTE_TITLE_INDEX)); pw.println("");
         * pw.println(c.getString(READ_NOTE_NOTE_INDEX)); } catch
         * (UnsupportedEncodingException e) { } finally { c.close(); if (pw !=
         * null) { pw.flush(); } try { fout.close(); } catch (IOException e) { }
         * }
         */
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        String year;
        String month;
        String day;
        String hour;
        String minute;
        String createTime;
        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }
        Long now = Long.valueOf(System.currentTimeMillis());
        Calendar c = Calendar.getInstance();
        year = String.valueOf(c.get(Calendar.YEAR));
        month = String.valueOf(c.get(Calendar.MONTH) + 1);
        day = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
        hour = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
        minute = String.valueOf(c.get(Calendar.MINUTE));
        if (c.get(Calendar.HOUR_OF_DAY) < 10) {
            hour = "0" + hour;
        }
        if (c.get(Calendar.MINUTE) < 10) {
            minute = "0" + minute;
        }
        createTime = String.valueOf(year) + " " + month + " " + day + " "
                + hour + ":" + minute;
        
        if (!values.containsKey(Notes.COLUMN_NAME_CREATE_DATE)) {
            values.put(Notes.COLUMN_NAME_CREATE_DATE, createTime);
        }

        if (!values.containsKey(Notes.COLUMN_NAME_MODIFICATION_DATE)) {
            values.put(Notes.COLUMN_NAME_MODIFICATION_DATE, now);
        }

        if (!values.containsKey(Notes.COLUMN_NAME_TITLE)) {
            Resources r = Resources.getSystem();
            values.put(Notes.COLUMN_NAME_TITLE,
                    r.getString(android.R.string.untitled));
        }

        if (!values.containsKey(Notes.COLUMN_NAME_NOTE)) {
            values.put(Notes.COLUMN_NAME_NOTE, "");
        }

        if (!values.containsKey(Notes.COLUMN_NAME_GROUP)) {
            values.put(Notes.COLUMN_NAME_GROUP, "");
        }
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowId = db
                .insert(Notes.TABLE_NAME, Notes.COLUMN_NAME_NOTE, values);

        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Notes.CONTENT_ID_URI_BASE,
                    rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }
        Notes.sSdcardFull = true;
        return null;
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        count = db.delete(Notes.TABLE_NAME, where, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where,
            String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;
        finalWhere = BaseColumns._ID + " = "
                + uri.getPathSegments().get(Notes.NOTE_ID_PATH_POSITION);
        count = db.update(Notes.TABLE_NAME, values, finalWhere, whereArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    /*
     * DatabaseHelper getOpenHelperForTest() { return mOpenHelper; }
     */
}
