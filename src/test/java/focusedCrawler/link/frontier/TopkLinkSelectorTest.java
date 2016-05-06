package focusedCrawler.link.frontier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import focusedCrawler.link.frontier.selector.TopkLinkSelector;

public class TopkLinkSelectorTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    @Test
    public void shouldSelectTopkLinksOfHigherRelevance() throws Exception {
        // given
        TopkLinkSelector selector = new TopkLinkSelector();
        Frontier frontier = new Frontier(tempFolder.newFolder().toString(), 100);
        frontier.insert(new LinkRelevance("http://localhost/001", LinkRelevance.DEFAULT_TYPE, 1));
        frontier.insert(new LinkRelevance("http://localhost/099", LinkRelevance.DEFAULT_TYPE, 99));
        frontier.insert(new LinkRelevance("http://localhost/199", LinkRelevance.DEFAULT_TYPE, 199));
        frontier.insert(new LinkRelevance("http://localhost/299", LinkRelevance.DEFAULT_TYPE, 299));
        frontier.commit();
        
        // when
        LinkRelevance[] links = selector.select(frontier, LinkRelevance.DEFAULT_TYPE, 2);
        
        // then
        assertThat(links, is(notNullValue()));
        assertThat(links.length, is(2));
        assertThat(links[0].getURL().toString(), is("http://localhost/299"));
        assertThat(links[1].getURL().toString(), is("http://localhost/199"));
    }
    
    @Test
    public void shouldNotSelectPagesWithNegativeRelevance() throws Exception {
        // given
        TopkLinkSelector selector = new TopkLinkSelector();
        Frontier frontier = new Frontier(tempFolder.newFolder().toString(), 100);
        frontier.insert(new LinkRelevance("http://localhost/001", LinkRelevance.DEFAULT_TYPE, -1));
        frontier.insert(new LinkRelevance("http://localhost/099", LinkRelevance.DEFAULT_TYPE, 99));
        frontier.insert(new LinkRelevance("http://localhost/199", LinkRelevance.DEFAULT_TYPE, -199));
        frontier.insert(new LinkRelevance("http://localhost/299", LinkRelevance.DEFAULT_TYPE, -299));
        frontier.commit();
        
        // when
        LinkRelevance[] links = selector.select(frontier, LinkRelevance.DEFAULT_TYPE, 2);
        
        // then
        assertThat(links, is(notNullValue()));
        assertThat(links.length, is(1));
        assertThat(links[0].getURL().toString(), is("http://localhost/099"));
    }


}
