package focusedCrawler.link.backlink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import focusedCrawler.link.LinkMetadata;
import focusedCrawler.target.model.Page;

public class MozBacklinkApi implements BacklinkApi {

    private static final ObjectMapper jsonMapper = new ObjectMapper();
    
    private static String queryStr = "?Filter=external&Scope=page_to_page&Limit=50&Sort=page_authority&SourceCols=5&TargetCols=4&";
    
    private String authStr;
    
    private long sleepTime = 5000;
    private int connectTimeout = 30000;
    private int readTimeout = 30000;
    
    public MozBacklinkApi(String mozAccessId, String mozKey) {
        MozAuthenticator auth = new MozAuthenticator(mozAccessId, mozKey, 300);
        this.authStr = auth.getAuthenticationStr();
    }

    public LinkMetadata[] downloadBacklinks(String host) throws IOException {

        String backlink = "http://lsapi.seomoz.com/linkscape/links/" + host + queryStr + authStr;

        Page page = downloadPage(newURL(backlink));
        if (page == null) {
            return null;
        }

        LinkMetadata[] backlinks = parseResponse(page.getContent());
        
        return backlinks;
    }

    private LinkMetadata[] parseResponse(String content) throws IOException, JsonProcessingException {
        
        JsonNode root = jsonMapper.readTree(content);
        Iterator<JsonNode> childIterator = root.elements();
        
        int resultSize = root.size();

        LinkMetadata[] backlinks = new LinkMetadata[resultSize];
        for (int i = 0; i < resultSize; i++) {
            JsonNode jsonNode = childIterator.next();
        
            String link = jsonNode.get("uu").asText();
            String title = jsonNode.get("ut").asText();

            backlinks[i] = new LinkMetadata();
            backlinks[i].addBacklinkUrl("http://" + link);
            backlinks[i].addBacklinkTitle(title);
            backlinks[i].addBacklinkSnippet("");
        }

        return backlinks;
    }

    
    private Page downloadPage(URL url) throws IOException {

        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while waiting sleepTime in MozBacklinkApi");
        }
        
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout(connectTimeout);
        conn.setReadTimeout(readTimeout);

        BufferedReader inCon = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuffer buffer = new StringBuffer();
        String inputLine;
        while ((inputLine = inCon.readLine()) != null) {
            buffer.append(inputLine + " ");
        }
        inCon.close();
        
        return new Page(url, buffer.toString());
    }

    private URL newURL(String url) throws MalformedURLException {
        if (url.indexOf("http://") == -1) {
            return new URL("http://" + url);
        } else {
            return new URL(url);
        }
    }

}
