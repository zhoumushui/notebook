
package com.zms.notebook;

import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;

import com.zms.notebook.NoteAdapter.NoteItem;
import com.zms.notebook.NotePad.Notes;

public class QueryHandler extends AsyncQueryHandler {
    private static final String TAG = "QueryHandler";
    private NotesList mLcontext;
    private NoteReading mRcontext;
    public NoteAdapter nadapter;
    private static final int MSG_QUERY_DONE_TO_SET_DATA = 1001;
    private Cursor mQueryCursor;
    private NoteWaitCursorView mNoteWaitProgress;

    public QueryHandler(ContentResolver cr, NotesList context) {
        super(cr);
        mLcontext = context;
        Log.d(TAG, "created from Noteslists");
        mThread.start();
    }

    public QueryHandler(ContentResolver cr, NoteReading context) {
        super(cr);
        mRcontext = context;
        Log.d(TAG, "created from NoteReading");
    }

    @Override
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        Log.i(TAG, "onQueryComplete");
        Notes.sNoteCount = cursor.getCount();
        if (Notes.sNoteCount > 0) {
            mLcontext.countView.setText(String.valueOf(cursor.getCount()));
        } else {
            mLcontext.countView.setText("");
        }
        nadapter = mLcontext.noteadapter;
        nadapter.cur = cursor;
        mQueryCursor = cursor;
        //setData(cursor, token);
        mLcontext.setListAdapter(nadapter);
        mLcontext.invalidateOptionsMenu();
        mNoteWaitProgress = ((NoteWaitCursorView) cookie);
        if (mHandler == null) {
            try {
                synchronized (mThread) {
                    mThread.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mHandler.sendEmptyMessage(MSG_QUERY_DONE_TO_SET_DATA);
    }

    @Override
    public void onDeleteComplete(int token, Object cookie, int result) {
        if (token == Notes.NOTESLIST_DELETE_TOKEN) {
            mLcontext.mActionMode.finish();
            mLcontext.queryUpdateData();
        } else if (token == Notes.NOTEREADING_DELETE_TOKEN) {
            Notes.sFlagNotesChanged = true;
            mRcontext.finish();
        } else if (token == Notes.NOTESLIST_DELETE_ALL_TOKEN) {
            mLcontext.queryUpdateData();
        }
        ((ProgressDialog) cookie).cancel();
    }

    public void setData(Cursor cursor) {
        NoteItem item;
        String note;
        String createTime;
        String currentTime;
        String notegroup;
        String modifyTime;
        int id;
        if (cursor.moveToFirst()) {
            int idColumn = cursor.getColumnIndex(BaseColumns._ID);
            int titleColumn = cursor.getColumnIndex(Notes.COLUMN_NAME_NOTE);
            int createColumn = cursor
                    .getColumnIndex(Notes.COLUMN_NAME_CREATE_DATE);
            int groupColumn = cursor.getColumnIndex(Notes.COLUMN_NAME_GROUP);
            int modifyColumn = cursor
                    .getColumnIndex(Notes.COLUMN_NAME_MODIFICATION_DATE);
            do {
                id = cursor.getInt(idColumn);
                note = cursor.getString(titleColumn);
                createTime = cursor.getString(createColumn);
                notegroup = cursor.getString(groupColumn);
                modifyTime = cursor.getString(modifyColumn);
                item = nadapter.new NoteItem();
                item.id = id;
                item.note = note;
                NoteReading nr = new NoteReading();
                currentTime = nr.currentDay(createTime, mLcontext);
                //currentTime = currentDay(createTime);
                item.modify_time = modifyTime;
                item.create_time = currentTime;
                item.notegroup = getGroup(notegroup);
                nadapter.addList(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    /*public String currentDay(String modifyDay) {
        Calendar c = Calendar.getInstance();
        String currentDay = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
        String currentYear = String.valueOf(c.get(Calendar.YEAR));
        String currentMonth = String.valueOf(c.get(Calendar.MONTH) + 1);
        String saveYear;
        String saveMonth;
        String saveDay;
        String []mt = modifyDay.split(" ");
        Resources resource = mLcontext.getResources();

        if (Locale.getDefault().getLanguage().equals("en")) {
            saveMonth = mt[1];
            saveDay = mt[2];
            saveYear = mt[0]; 
            if (currentYear.equals(saveYear)) {
                if (currentMonth.equals(saveMonth) && currentDay.equals(saveDay)) {
                    return mt[3];
                } else {
                    return NotePad.Notes.MONTH[Integer.valueOf(mt[1]) - 1] + " " + mt[2] + " " + mt[3];
                }
            } else {
                return NotePad.Notes.MONTH[Integer.valueOf(mt[1]) - 1] + " " + mt[2] + "," + mt[0] + " " + mt[3];
            }
        } else {
            String displayMonth = (String) resource.getString(R.string.month);
            String displayDay = (String) resource.getString(R.string.day);
            saveYear = mt[0]; 
            saveMonth = mt[1];
            saveDay = mt[2];
            if (mt[1].equals("Jan")) {
                mt[1] = "1";
            } else if (mt[1].equals("Feb")) {
                mt[1] = "2";
            } else if (mt[1].equals("Mar")) {
                mt[1] = "3";
            } else if (mt[1].equals("Apr")) {
                mt[1] = "4";
            } else if (mt[1].equals("May")) {
                mt[1] = "5";
            } else if (mt[1].equals("June")) {
                mt[1] = "6";
            } else if (mt[1].equals("July")) {
                mt[1] = "7";
            } else if (mt[1].equals("Aug")) {
                mt[1] = "8";
            } else if (mt[1].equals("Sep")) {
                mt[1] = "9";
            } else if (mt[1].equals("Oct")) {
                mt[1] = "10";
            } else if (mt[1].equals("Nov")) {
                mt[1] = "11";
            } else if (mt[1].equals("Dec")) {
                mt[1] = "12";
            }
            if (currentYear.equals(saveYear)) {
                if (currentMonth.equals(saveMonth) && currentDay.equals(saveDay)) {
                    return mt[3];
                } else {
                    return mt[1] + displayMonth + mt[2] + displayDay + " " + mt[3]; 
                }
            } else {
                return mt[0] + "-" + mt[1] + "-" + mt[2] + " " + mt[3];
            }
        }
    } */

    public String getGroup(String i) {
        Resources resource;
        resource = mLcontext.getResources();
        String groupWork = (String) resource.getString(R.string.menu_work);
        String groupPersonal = (String) resource
                .getString(R.string.menu_personal);
        String groupFamily = (String) resource.getString(R.string.menu_family);
        String groupStudy = (String) resource.getString(R.string.menu_study);
        if (i.equals("1")) {
            return groupWork;
        } else if (i.equals("2")) {
            return groupPersonal;
        } else if (i.equals("3")) {
            return groupFamily;
        } else if (i.equals("4")) {
            return groupStudy;
        } else {
            return "";
        }
    }

    private Handler mHandler;
    private Thread mThread = new Thread() {
        @Override
        public void run() {
            Looper.prepare();
            mHandler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case MSG_QUERY_DONE_TO_SET_DATA:
                            setData(mQueryCursor);
                            mLcontext.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    nadapter.notifyDataSetChanged();
                                    mNoteWaitProgress.stopWaitCursor();
                                    mLcontext.invalidateOptionsMenu();
                                }
                            });
                            break;
                        default:
                            break;
                    }

                }
            };
            synchronized (mThread) {
                notify();
            }
            Looper.loop();
        }
    };
}
