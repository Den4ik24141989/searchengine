package searchengine.сonnection;

import org.jsoup.Jsoup;

public class Connection {
    private final String url;

    public Connection(String url) {
        this.url = url;
    }
    public org.jsoup.Connection getConnection () {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .referrer("http://www.google.com");
    }
}
