package focusedCrawler.link.frontier;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import focusedCrawler.link.frontier.selector.PoliteTopkLinkSelector;

public class PoliteTopkLinkSelectorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void shouldSelectTopkLinksOfHigherRelevanceOfEachDomain() throws Exception {
        // given
        PoliteTopkLinkSelector selector = new PoliteTopkLinkSelector(2); 
        
        Frontier frontier = new Frontier(tempFolder.newFolder().toString(), 100);
        
        frontier.insert(new LinkRelevance("http://asdf/1", LinkRelevance.DEFAULT_TYPE, 201));
        frontier.insert(new LinkRelevance("http://asdf/2", LinkRelevance.DEFAULT_TYPE, 202));
        frontier.insert(new LinkRelevance("http://asdf/3", LinkRelevance.DEFAULT_TYPE, 203));
        frontier.insert(new LinkRelevance("http://asdf/4", LinkRelevance.DEFAULT_TYPE, 204));
        
        frontier.insert(new LinkRelevance("http://qwer/1", LinkRelevance.DEFAULT_TYPE, 101));
        frontier.insert(new LinkRelevance("http://qwer/2", LinkRelevance.DEFAULT_TYPE, 102));
        frontier.insert(new LinkRelevance("http://qwer/3", LinkRelevance.DEFAULT_TYPE, 103));
        frontier.insert(new LinkRelevance("http://qwer/4", LinkRelevance.DEFAULT_TYPE, 104));
        
        frontier.insert(new LinkRelevance("http://zxcv/1", LinkRelevance.DEFAULT_TYPE, 1));
        frontier.insert(new LinkRelevance("http://zxcv/2", LinkRelevance.DEFAULT_TYPE, 2));
        frontier.insert(new LinkRelevance("http://zxcv/3", LinkRelevance.DEFAULT_TYPE, 3));
        frontier.insert(new LinkRelevance("http://zxcv/4", LinkRelevance.DEFAULT_TYPE, 4));
        frontier.commit();
        
        // when
        LinkRelevance[] links = selector.select(frontier, LinkRelevance.DEFAULT_TYPE, 5);

        // then
        assertThat(links, is(notNullValue()));
        assertThat(links.length, is(5));
        
        Set<String> urls = new HashSet<String>();
        for (LinkRelevance linkRelevance : links) {
            urls.add(linkRelevance.getURL().toString());
        }
        
        assertThat(urls, hasItem("http://asdf/4"));
        assertThat(urls, hasItem("http://asdf/3"));
        assertThat(urls, hasItem("http://qwer/4"));
        assertThat(urls, hasItem("http://qwer/3"));
        assertThat(urls, hasItem("http://zxcv/4"));
    }
    
    
    @Test
    public void shouldNotSelectLinkFromSameDomainBeforeMininumTime() throws Exception {
        // given
        final int maxUrlPerDomain = 1;
        final long minimumAccessTimeInterval = 500;
        
        PoliteTopkLinkSelector selector = new PoliteTopkLinkSelector(maxUrlPerDomain, minimumAccessTimeInterval); 
        
        Frontier frontier = new Frontier(tempFolder.newFolder().toString(), 100);
        frontier.insert(new LinkRelevance("http://asdf/1", LinkRelevance.DEFAULT_TYPE, 201));
        frontier.insert(new LinkRelevance("http://qwer/1", LinkRelevance.DEFAULT_TYPE, 101));
        frontier.commit();
        
        LinkRelevance[] links = null;
        
        // when
        long timeBeforeFirstSelect = System.currentTimeMillis();
        links = selector.select(frontier, LinkRelevance.DEFAULT_TYPE, 2);
        // then
        assertThat(links, is(notNullValue()));
        assertThat(links.length, is(2));
        
        // when
        links = selector.select(frontier, LinkRelevance.DEFAULT_TYPE, 2);
        long timeLastSelect = System.currentTimeMillis();

        // then
        assertThat(links, is(notNullValue()));
        
        final long interval = timeLastSelect - timeBeforeFirstSelect;
        assertThat(interval, greaterThan(minimumAccessTimeInterval));
    }
    
}
