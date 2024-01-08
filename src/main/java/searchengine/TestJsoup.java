package searchengine;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;

public class TestJsoup {
    public static void main(String[] args) throws IOException {
        String url = "https://05.ru/catalog/computers/";

        Connection.Response response = Jsoup.connect(url)
                .userAgent("MySearchEngine")
                .referrer("http://www.google.com")
                .execute();

        String body = response.body();
        int statusCode = response.statusCode();
        String text = Jsoup.parse(body).text();

        Document document = Jsoup.parse(body);
        Element element = document.selectFirst("h1");

        System.out.println(element.text());
    }
}
