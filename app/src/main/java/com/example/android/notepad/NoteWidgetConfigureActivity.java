package com.example.android.notepad;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

/**
 * The configuration screen for the NoteWidgetProvider.
 */
public class NoteWidgetConfigureActivity extends Activity {

    private static final String PREFS_NAME = "com.example.android.notepad.NoteWidgetProvider";
    private static final String PREF_PREFIX_KEY = "appwidget_";
    int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    ListView mListView;

    public NoteWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

        // 使用一个简单的布局，只包含一个 ListView
        setContentView(R.layout.notes_list_view); 
        
        // 注意：notes_list_view 里面可能包含 GridView 和 ListView，这里我们只用 ListView
        // 并且为了避免冲突，我们可以隐藏 GridView 如果有的话，或者直接创建一个新的 layout。
        // 但为了保持一致性，我将直接使用 ListView。
        // 为了更简单且不依赖 notes_list_view 的复杂结构（如果有），
        // 我将动态创建一个 ListView 或者使用现有的布局并确保配置正确。
        // 之前我使用了 new ListView(this)，这可能导致主题丢失或没有 padding。
        // 让我们尝试使用 notes_list_view 布局，但只用其中的 ListView 部分。
        
        // 实际上，为了获得更好的预览效果，我应该使用自定义的 Item 布局 (noteslist_item.xml)
        
        mListView = (ListView) findViewById(android.R.id.list);
        if (mListView == null) {
            // 如果布局中没有 id 为 list 的 View，回退到动态创建
            mListView = new ListView(this);
            setContentView(mListView);
        }

        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // Query notes
        Cursor cursor = getContentResolver().query(
                NotePad.Notes.CONTENT_URI,
                new String[] { 
                    NotePad.Notes._ID, 
                    NotePad.Notes.COLUMN_NAME_TITLE,
                    NotePad.Notes.COLUMN_NAME_NOTE,
                    NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                    NotePad.Notes.COLUMN_NAME_BACK_COLOR
                },
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        // Adapter
        String[] from = new String[] { 
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_BACK_COLOR
        };
        
        int[] to = new int[] { 
            R.id.title,
            R.id.text,
            R.id.modified,
            R.id.back_color
        };

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item, // 使用自定义的布局
                cursor,
                from,
                to,
                0
        );
        
        // 设置 ViewBinder 处理背景颜色和日期
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.back_color) {
                    int x = cursor.getInt(columnIndex);
                    try {
                        GradientDrawable background = (GradientDrawable) view.getBackground().mutate();
                        background.setColor(x != 0 ? x : Color.WHITE);
                    } catch (Exception e) {
                        if (x != 0) {
                            view.setBackgroundColor(x);
                        } else {
                            view.setBackgroundResource(R.drawable.card_background);
                        }
                    }
                    return true;
                } else if (view.getId() == R.id.modified) {
                    long millis = cursor.getLong(columnIndex);
                    // 简单的日期格式化，或者可以复用 NotesList 中的逻辑
                    // 这里为了简单不显示具体时间，或者您可以添加格式化代码
                    // 如果不想显示时间，返回 true 并设为空字符串
                     if (millis > 0) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
                        sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Shanghai"));
                        ((TextView) view).setText(sdf.format(new java.util.Date(millis)));
                    } else {
                        ((TextView) view).setText("");
                    }
                    return true;
                }
                return false;
            }
        });
        
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Context context = NoteWidgetConfigureActivity.this;

                // When the button is clicked, store the string locally
                saveNoteIdPref(context, mAppWidgetId, id);

                // It is the responsibility of the configuration activity to update the app widget
                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
                NoteWidgetProvider.updateAppWidget(context, appWidgetManager, mAppWidgetId);

                // Make sure we pass back the original appWidgetId
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            }
        });
    }

    // Write the prefix to the SharedPreferences object for this widget
    static void saveNoteIdPref(Context context, int appWidgetId, long noteId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putLong(PREF_PREFIX_KEY + appWidgetId, noteId);
        prefs.apply();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static long loadNoteIdPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        return prefs.getLong(PREF_PREFIX_KEY + appWidgetId, -1);
    }

    static void deleteNoteIdPref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.apply();
    }
}
