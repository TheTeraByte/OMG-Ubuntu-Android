/*
 * Copyright (C) 2012 - 2013 Ohso Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.ohso.omgubuntu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.ohso.omgubuntu.data.Article;
import com.ohso.omgubuntu.data.ArticleDataSource;
import com.ohso.omgubuntu.data.Articles;
import com.ohso.omgubuntu.data.Articles.OnArticlesLoaded;

public class NotificationService extends IntentService implements OnArticlesLoaded {
    private static final String NOTIFICATION_SERVICE_NAME = "OMGNotificationService";
    public static final String LAST_NOTIFIED_PATH = "NotificationServiceLastNotifiedPath";
    public static final String NOTIFICATION_ACTION =
            "com.ohso.omgubuntu.NotificationService.NEW_ARTICLES";
    public static final String ORIGINATING_CLASS = "com.ohso.omgubuntu.NotificationService.ORIGINATING_CLASS";

    public static boolean isNotificationAlarmActive() {
        boolean isUp = (PendingIntent.getBroadcast(OMGUbuntuApplication.getContext(), 0,
                new Intent(NOTIFICATION_ACTION), PendingIntent.FLAG_NO_CREATE) != null);
        if (isUp) Log.i("OMG!", "Alarm is already active!");
        return isUp;
    }

    public NotificationService() {
        super(NOTIFICATION_SERVICE_NAME);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String originatingClass = intent.getStringExtra(ORIGINATING_CLASS);
        if(!getSharedPreferences(OMGUbuntuApplication.PREFS_FILE, 0)
                .getBoolean(SettingsFragment.NOTIFICATIONS_ENABLED,
                getResources().getBoolean(R.bool.pref_notifications_enabled_default))) {
            // If we're refreshing from the widget, let it go through
            if (originatingClass == null || !originatingClass.equals(ArticlesWidgetProvider.ORIGINATING_NAME)) {
                if (MainActivity.DEVELOPER_MODE) Log.i("OMG!", "Cancelling alarms since it's no longer enabled!");
                NotificationAlarmGenerator.cancelAlarm(getApplicationContext());
                return;
            }
        }

        // Checking for wifi preference and wifi state
        if (getSharedPreferences(OMGUbuntuApplication.PREFS_FILE, 0)
                .getBoolean(SettingsFragment.WIFI_ONLY,
                        getResources().getBoolean(R.bool.pref_notifications_wifi_only_default)) &&
                (originatingClass == null || !originatingClass.equals(ArticlesWidgetProvider.ORIGINATING_NAME))) {
            if (!OMGUbuntuApplication.isWifiAvailable()) return;
        }

        Articles articles = new Articles();
        articles.getLatest(this);
    }

    @Override
    public void articlesLoaded(Articles result) {
        if (MainActivity.DEVELOPER_MODE) Log.i("OMG!", "Got articles in NotificationService");
        List<Article> newArticles = new ArrayList<Article>();
        ArticleDataSource dataSource = new ArticleDataSource(getApplicationContext());
        dataSource.open();
        for (Article article : result) {
            if(dataSource.createArticle(article, false, false)) {
                newArticles.add(article);
            }
        }
        dataSource.close();

        // Comment this out for debugging
        if(newArticles.size() < 1) {
            ArticlesWidgetProvider.notifyUpdate(0);
            NotificationAlarmReceiver.releaseWakeLock();
            return;
        }

        Collections.sort(newArticles, new Article.Compare());

        SharedPreferences sharedPref = getSharedPreferences(OMGUbuntuApplication.PREFS_FILE, 0);
        String lastPath = sharedPref.getString(LAST_NOTIFIED_PATH, null);
        String broadcastPath = null;

        if (lastPath != null) {
            broadcastPath = lastPath;
        } else {
            SharedPreferences.Editor editor = sharedPref.edit();
            // Uncomment next line and comment the one after for debugging
            //broadcastPath = result.get(2).getPath();
            broadcastPath = newArticles.get(0).getPath();
            editor.putString(LAST_NOTIFIED_PATH, broadcastPath);
            editor.commit();
        }


        broadcastArticlesNotification(broadcastPath);
    }

    @Override
    public void articlesError() {
        // Silently fail for now
        if (MainActivity.DEVELOPER_MODE) Log.i("OMG!", "Article retrieval error in NotificationService");
        ArticlesWidgetProvider.notifyUpdate(0);
        NotificationAlarmReceiver.releaseWakeLock();
    }

    private void broadcastArticlesNotification(String lastPath) {
        Intent broadcast = new Intent();
        broadcast.setAction(NOTIFICATION_ACTION);
        broadcast.addCategory(Intent.CATEGORY_DEFAULT);
        broadcast.putExtra("last_path", lastPath);
        sendBroadcast(broadcast);
    }

}
