package focusedCrawler.link.backlink;

import java.io.IOException;

import focusedCrawler.link.LinkMetadata;

public interface BacklinkApi {

    public LinkMetadata[] downloadBacklinks(String url) throws IOException;
}
