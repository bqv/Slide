package com.github.ghostdogpr.readability4s;

import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;

public class ReadableDocument extends Readability {
    private Article article;

    public ReadableDocument(Document document) {
        super(document.baseUri(), document.html());
        article = parse().get();
    }

    public String uri() { return article.uri(); }
    public String title() { return article.title(); }
    public String byline() { return article.byline(); }
    public String content() { return article.content(); }
    public String textContent() { return article.textContent(); }
    public Integer length() { return article.length(); }
    public String exerpt() { return article.excerpt(); }
    public URL faviconUrl() throws MalformedURLException { return new URL(article.faviconUrl()); }
    public URL imageUrl() throws MalformedURLException { return new URL(article.imageUrl()); }
}
