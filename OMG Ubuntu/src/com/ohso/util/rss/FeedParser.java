/*
 * Copyright (C) 2012 Ohso Ltd
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
package com.ohso.util.rss;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParserException;

import com.ohso.omgubuntu.data.Article;
import com.ohso.omgubuntu.data.Articles;

public class FeedParser {
    public Articles parseArticles(InputStream in) throws XmlPullParserException, IOException {
        return new ArticlesHandler().parse(in);
    }
    public Article parseArticle(InputStream in) throws XmlPullParserException, IOException {
        return new ArticleHandler().parse(in);
    }
}
