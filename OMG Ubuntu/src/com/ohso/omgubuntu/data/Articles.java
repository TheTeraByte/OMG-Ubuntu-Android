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
package com.ohso.omgubuntu.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ohso.omgubuntu.OMGUbuntuApplication;
import com.ohso.omgubuntu.R;
import com.ohso.util.UrlFactory;
import com.ohso.util.rss.FeedParser;

public class Articles extends ArrayList<Article> {
    private static final long serialVersionUID = 1L;
    private Context context = OMGUbuntuApplication.getContext();
    private OnArticlesLoaded mCallback;
    private OnNextPageLoaded mNextPageCallback;

    public void getLatest(OnArticlesLoaded caller) {
        mCallback = caller;
        if (OMGUbuntuApplication.isNetworkAvailable()) {
            new getArticlesAsync().execute();
        } else {
            mCallback.articlesError();
        }
    }

    public void getNextPage(OnNextPageLoaded caller, int page) {
        mNextPageCallback = caller;
        ArticleDataSource dataSource = new ArticleDataSource(context);
        dataSource.open();
        Articles articles = dataSource.getArticlesOnPage(page);
        dataSource.close();
        if (articles.size() == ArticleDataSource.MAX_ARTICLES_PER_PAGE) {
            mNextPageCallback.nextPageLoaded(articles);
        } else {
            if (!OMGUbuntuApplication.isNetworkAvailable()) {
                mNextPageCallback.nextPageError();
                return;
            }
            new getNextPageAsync().execute(page);
        }

    }

    public void getNextCategoryPage(OnNextPageLoaded caller, String urlFragment, int page) {
        mNextPageCallback = caller;
        ArticleDataSource dataSource = new ArticleDataSource(context);
        dataSource.open();
        Articles articles = dataSource.getArticlesWithCategoryOnPage(urlFragment, page);
        dataSource.close();
        if (articles.size() == ArticleDataSource.MAX_ARTICLES_PER_PAGE) {
            mNextPageCallback.nextPageLoaded(articles);
        } else {
            if (!OMGUbuntuApplication.isNetworkAvailable()) {
                mNextPageCallback.nextPageError();
                return;
            }
            new getNextCategoryPageAsync(urlFragment, page).execute();
        }
    }

    private class getNextCategoryPageAsync extends AsyncTask<Object, Void, Articles> {
        private final String mUrlFragment;
        private final int mPage;
        public getNextCategoryPageAsync(String name, int page) {
            mUrlFragment = name;
            mPage = page;
        }
        @Override
        protected Articles doInBackground(Object... params) {
            try {
                return loadXmlFromNetwork(UrlFactory.forCategoryPage(mUrlFragment, mPage));
            } catch (IOException e) {
                Log.e("OMG!", context.getResources().getString(R.string.connection_error) + e.toString());
                return null;
            } catch (XmlPullParserException e) {
                Log.e("OMG!", context.getResources().getString(R.string.xml_error) + e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Articles result) {
            if(result == null) {
                mNextPageCallback.nextPageError();
                return;
            }
            else mNextPageCallback.nextPageLoaded(result);
            ArticleDataSource dataSource = new ArticleDataSource(context);
            dataSource.open();
            for (Article article : result) {
                dataSource.createArticle(article, false, false);
            }
            dataSource.close();
            super.onPostExecute(result);
        }
    }

    private class getNextPageAsync extends AsyncTask<Integer, Void, Articles> {
        @Override
        protected Articles doInBackground(Integer... params) {
            try {
                return loadXmlFromNetwork(UrlFactory.forPage(params[0]));
            } catch (IOException e) {
                Log.e("OMG!", context.getResources().getString(R.string.connection_error) + e.toString());
                return null;
            } catch (XmlPullParserException e) {
                Log.e("OMG!", context.getResources().getString(R.string.xml_error) + e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Articles result) {
            if(result == null) {
                mNextPageCallback.nextPageError();
                return;
            }
            else mNextPageCallback.nextPageLoaded(result);
            ArticleDataSource dataSource = new ArticleDataSource(context);
            dataSource.open();
            for (Article article : result) {
                dataSource.createArticle(article, false, false);
            }
            dataSource.close();
            super.onPostExecute(result);
        }
    }

    private class getArticlesAsync extends AsyncTask<Void, Void, Articles> {
        @Override
        protected Articles doInBackground(Void... params) {
            try {
                return loadXmlFromNetwork(UrlFactory.fromFragment("feed"));
            } catch (IOException e) {
                Log.e("OMG!", context.getResources().getString(R.string.connection_error) + e.toString());
                return null;
            } catch (XmlPullParserException e) {
                Log.e("OMG!", context.getResources().getString(R.string.xml_error) + e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Articles result) {
            if(result == null) mCallback.articlesError();
            else mCallback.articlesLoaded(result);
            super.onPostExecute(result);
        }
    }

    public void getLatestInCategory(OnArticlesLoaded caller, String categoryName) {
        mCallback = caller;
        if (!OMGUbuntuApplication.isNetworkAvailable()) mCallback.articlesError();
        new getLatestInCategoryAsync().execute(categoryName);
    }

    private class getLatestInCategoryAsync extends AsyncTask<String, Void, Articles> {
        @Override
        protected Articles doInBackground(String... params) {
            try {
                return loadXmlFromNetwork(UrlFactory.fromFragment(UrlFactory.fragmentForCategory(params[0])));
            } catch (IOException e) {
                Log.e("OMG!", context.getResources().getString(R.string.connection_error) + e.toString());
                return null;
            } catch (XmlPullParserException e) {
                Log.e("OMG!", context.getResources().getString(R.string.xml_error) + e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Articles result) {
            if(result == null) mCallback.articlesError();
            else mCallback.articlesLoaded(result);
            super.onPostExecute(result);
        }
    }

    private Articles loadXmlFromNetwork(String urlFragment) throws XmlPullParserException, IOException {
        InputStream stream = null;
        FeedParser omgParser = new FeedParser();
        Articles articles = null;
        try {
            stream = downloadUrl(urlFragment);
            articles = omgParser.parseArticles(stream);
        } catch (XmlPullParserException e) {
            Log.e("OMG!", "XML Exception from loadXmlFromNetwork! " + e.toString());
        } catch (IOException e) {
            Log.e("OMG!", "IOException from loadXmlFromNetwork " + e.toString());
        } finally {
            if (stream != null) stream.close();
        }
        return articles;
    }

    private InputStream downloadUrl(String urlFragment) throws IOException {
        URL url = new URL(urlFragment);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if(context.getResources().getString(R.string.rss_user_agent).length() > 0) {
            conn.setRequestProperty("User-Agent", context.getResources().getString(R.string.rss_user_agent));
        }
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setUseCaches(false);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        InputStream stream = conn.getInputStream();
        return stream;
    }

    public interface OnArticlesLoaded {
        void articlesLoaded(Articles result);
        void articlesError();
    }

    public interface OnNextPageLoaded {
        void nextPageLoaded(Articles result);
        void nextPageError();
    }
}
