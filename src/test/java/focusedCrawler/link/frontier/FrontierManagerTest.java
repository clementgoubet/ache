package focusedCrawler.link.frontier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import focusedCrawler.link.frontier.selector.LinkSelector;
import focusedCrawler.link.frontier.selector.NonRandomLinkSelector;
import focusedCrawler.link.frontier.selector.SiteLinkSelector;
import focusedCrawler.util.LinkFilter;

public class FrontierManagerTest {

    @Rule
    // a new temp folder is created for each test case
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private LinkFilter emptyLinkFilter = new LinkFilter(new ArrayList<String>());

    private Frontier frontier;
    private Frontier backlinkFrontier;
    
    @Before
    public void setUp() throws Exception {
        frontier = new Frontier(tempFolder.newFolder().toString(), 1000);
    }
    
    @After
    public void tearDown() throws IOException {
    }
    
    @Test
    public void shouldNotInsertLinkOutOfScope() throws Exception {
        // given
        
        LinkRelevance link1 = new LinkRelevance(new URL("http://www.example1.com/index.html"), LinkRelevance.DEFAULT_TYPE, 1);
        LinkRelevance link2 = new LinkRelevance(new URL("http://www.example2.com/index.html"), LinkRelevance.DEFAULT_TYPE, 2);
        
        Map<String, Integer> scope = new HashMap<String, Integer>();
        scope.put("www.example1.com", -1);
        
        
        LinkSelector linkSelector = new SiteLinkSelector();
        frontier = new Frontier(tempFolder.newFolder().toString(), 1000, scope);
        backlinkFrontier = new Frontier(tempFolder.newFolder().toString(), 1000, scope);
        FrontierManager frontierManager = new FrontierManager(frontier, backlinkFrontier, 2, 2, linkSelector, null, null, new LinkFilter(new ArrayList<String>()));
        
        // when
        frontierManager.insert(link1,1);
        frontierManager.insert(link2,1);
        
        LinkRelevance selectedLink1 = frontierManager.nextURL();
        LinkRelevance selectedLink2 = frontierManager.nextURL();
        
        // then
        assertThat(selectedLink1, is(notNullValue()));
        assertThat(selectedLink1.getURL(), is(notNullValue()));
        assertThat(selectedLink1.getURL(), is(link1.getURL()));
        
        assertThat(selectedLink2, is(nullValue()));
        
        frontierManager.close();
    }
    

    @Test
    public void shouldInsertUrl() throws Exception {
        // given
        LinkSelector linkSelector = new NonRandomLinkSelector();
        frontier = new Frontier(tempFolder.newFolder().toString(), 1000);
        backlinkFrontier = new Frontier(tempFolder.newFolder().toString(), 1000);
        FrontierManager frontierManager = new FrontierManager(frontier, backlinkFrontier, 2, 2, linkSelector, null, null, emptyLinkFilter);
        
        LinkRelevance link1 = new LinkRelevance(new URL("http://www.example1.com/index.html"), LinkRelevance.DEFAULT_TYPE, 1);
        
        // when
        frontierManager.insert(link1,1);
        
        LinkRelevance nextURL = frontierManager.nextURL();
        
        // then
        assertThat(nextURL, is(notNullValue()));
        assertThat(nextURL.getURL(), is(notNullValue()));
        assertThat(nextURL.getURL(), is(link1.getURL()));
        assertThat(nextURL.getRelevance(), is(link1.getRelevance()));
        
        frontierManager.close();
    }
    
    @Test
    public void shouldInsertUrlsAndSelectUrlsInSortedByRelevance() throws Exception {
        // given
        LinkSelector linkSelector = new NonRandomLinkSelector();
        frontier = new Frontier(tempFolder.newFolder().toString(), 1000);
        backlinkFrontier = new Frontier(tempFolder.newFolder().toString(), 1000);
        FrontierManager frontierManager = new FrontierManager(frontier, backlinkFrontier, 2, 2, linkSelector, null, null, emptyLinkFilter);
        
        LinkRelevance link1 = new LinkRelevance(new URL("http://www.example1.com/index.html"), LinkRelevance.DEFAULT_TYPE, 1);
        LinkRelevance link2 = new LinkRelevance(new URL("http://www.example2.com/index.html"), LinkRelevance.DEFAULT_TYPE, 2);
        LinkRelevance link3 = new LinkRelevance(new URL("http://www.example3.com/index.html"), LinkRelevance.DEFAULT_TYPE, 3);
        
        // when
        frontierManager.insert(link1,1);
        System.out.println(frontierManager.getLinkFrontier().unvisited(1));
        frontierManager.insert(link2,1);
        System.out.println(frontierManager.getLinkFrontier().unvisited(1));
        frontierManager.insert(link3,1);
        System.out.println(frontierManager.getLinkFrontier().unvisited(1));
        
        LinkRelevance selectedLink1 = frontierManager.nextURL();
        LinkRelevance selectedLink2 = frontierManager.nextURL();
        LinkRelevance selectedLink3 = frontierManager.nextURL();
        LinkRelevance selectedLink4 = frontierManager.nextURL();
        
        // then
        
        // should return only 3 inserted links, 4th should be null 
        assertThat(selectedLink1, is(notNullValue()));
        assertThat(selectedLink2, is(notNullValue()));
        assertThat(selectedLink3, is(notNullValue()));
        assertThat(selectedLink4, is(nullValue()));
        
        // should return bigger relevance values first
        assertThat(selectedLink1.getURL(), is(link3.getURL()));
        assertThat(selectedLink2.getURL(), is(link2.getURL()));
        assertThat(selectedLink3.getURL(), is(link1.getURL()));
        
        frontierManager.close();
    }
    
    
    @Test
    public void shouldNotReturnAgainALinkThatWasAlreadyReturned() throws Exception {
        // given
        LinkSelector linkSelector = new NonRandomLinkSelector();
        frontier = new Frontier(tempFolder.newFolder().toString(), 1000);
        backlinkFrontier = new Frontier(tempFolder.newFolder().toString(), 1000);
        FrontierManager frontierManager = new FrontierManager(frontier, backlinkFrontier, 2, 2, linkSelector, null, null, emptyLinkFilter);
        
        LinkRelevance link1 = new LinkRelevance(new URL("http://www.example1.com/index.html"), LinkRelevance.DEFAULT_TYPE, 1);
        LinkRelevance link2 = new LinkRelevance(new URL("http://www.example2.com/index.html"), LinkRelevance.DEFAULT_TYPE, 2);
        
        // when
        frontierManager.insert(link1,1);
        frontierManager.insert(link2,1);
        LinkRelevance selectedLink1 = frontierManager.nextURL();
        LinkRelevance selectedLink2 = frontierManager.nextURL();
        
        LinkRelevance selectedLink3 = frontierManager.nextURL();
        
        frontierManager.insert(link1,1); // insert link 1 again, should not be returned
        LinkRelevance selectedLink4 = frontierManager.nextURL();
        
        // then
        assertThat(selectedLink1, is(notNullValue()));
        assertThat(selectedLink2, is(notNullValue()));
        
        assertThat(selectedLink3, is(nullValue()));
        
        assertThat(selectedLink4, is(nullValue()));
        
        frontierManager.close();
        
    }

}
