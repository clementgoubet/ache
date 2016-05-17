package focusedCrawler.link.backlink;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import focusedCrawler.link.LinkMetadata;

public class GoogleBacklinkApi implements BacklinkApi {
    
    final String userAgent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";
    
    public LinkMetadata[] downloadBacklinks(String host) throws IOException {
        
        // 21 -> max number allowed by google... decreases after
        String backlink = "https://www.google.com/search?q=link:" + host + "&num=21";

        try {
        	Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost",8118));
            URLConnection connection = new URL(backlink).openConnection(proxy);
            connection.setRequestProperty("User-Agent", userAgent);
            connection.connect();
    
            Document doc = Jsoup.parse(connection.getInputStream(), "UTF-8", host);
            Elements searchItems = doc.select("div#search").select(".g");
            
            ArrayList<LinkMetadata> backlinksList = new ArrayList<>();
            for (Element item : searchItems) {
                Elements linkUrl = item.select("a[href]");
                Elements linkSnippet = item.select(".st");
                LinkMetadata lm = new LinkMetadata();
                try{
                	//check that url is not malformed
                	new URL(linkUrl.attr("href"));
                	lm.addBacklinkUrl(linkUrl.attr("href"));
                	lm.addBacklinkTitle(linkUrl.text());
                	lm.addBacklinkSnippet(linkSnippet.text());
                	backlinksList.add(lm);
                } catch(MalformedURLException e){
                	// skip malformed urls
                }
            }
            LinkMetadata[] backlinks = new LinkMetadata[backlinksList.size()];
            backlinksList.toArray(backlinks);

            return backlinks;
        } catch (IOException e) {
            throw new IOException("Failed to download backlinks from Google.", e);
        }
    
    }
}

