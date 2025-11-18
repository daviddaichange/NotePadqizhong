package com.example.android.notepad;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.widget.RemoteViews;

public class NoteWidgetProvider extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // 获取 Widget 存储的 Note ID
        long noteId = NoteWidgetConfigureActivity.loadNoteIdPref(context, appWidgetId);
        if (noteId == -1) {
            return;
        }

        // 查询笔记内容
        Uri noteUri = android.content.ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, noteId);
        Cursor cursor = context.getContentResolver().query(
                noteUri,
                new String[] { NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_BACK_COLOR },
                null,
                null,
                null
        );

        String title = "";
        String text = "";
        int color = Color.WHITE;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                title = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_TITLE));
                text = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_NOTE));
                color = cursor.getInt(cursor.getColumnIndexOrThrow(NotePad.Notes.COLUMN_NAME_BACK_COLOR));
            }
            cursor.close();
        }
        
        if (color == 0) {
            color = Color.WHITE;
        }

        // 构建 RemoteViews
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_note);
        views.setTextViewText(R.id.widget_title, title);
        views.setTextViewText(R.id.widget_text, text);
        
        // 设置标题背景和内容背景
        int darkerColor = darkenColor(color);
        
        // 标题背景：上半圆角，深色
        views.setImageViewBitmap(R.id.widget_header_bg, createRoundedBitmap(darkerColor, true, false));
        
        // 内容背景：下半圆角，原色
        views.setImageViewBitmap(R.id.widget_content_bg, createRoundedBitmap(color, false, true));

        // 点击 Widget 跳转到编辑页面
        Intent intent = new Intent(context, NoteEditor.class);
        intent.setAction(Intent.ACTION_EDIT);
        intent.setData(noteUri);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent);

        // 更新 Widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
    
    private static int darkenColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] *= 0.8f; // 降低亮度 20%
        return Color.HSVToColor(hsv);
    }
    
    private static Bitmap createRoundedBitmap(int color, boolean topRounded, boolean bottomRounded) {
        // 创建一个圆角矩形 Bitmap
        int w = 200; // 任意大小，会被拉伸
        int h = 200;
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        RectF rect = new RectF(0, 0, w, h);
        float radius = 24f; // 圆角半径
        
        // Path path = new Path(); ... 比较复杂，这里用更简单的方式绘制
        // 或者分别画圆角矩形然后覆盖不需要圆角的地方
        
        canvas.drawRoundRect(rect, radius, radius, paint);
        
        if (!topRounded) {
             // 填充上半部分为直角
             canvas.drawRect(0, 0, w, radius, paint);
        }
        if (!bottomRounded) {
            // 填充下半部分为直角
             canvas.drawRect(0, h - radius, w, h, paint);
        }
        
        return bitmap;
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // 更新所有 Widget
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // 删除 Widget 时清除 Preference
        for (int appWidgetId : appWidgetIds) {
            NoteWidgetConfigureActivity.deleteNoteIdPref(context, appWidgetId);
        }
    }
}
