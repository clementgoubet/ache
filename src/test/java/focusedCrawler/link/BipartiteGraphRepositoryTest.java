package focusedCrawler.link;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import focusedCrawler.link.LinkStorageConfig.BiparitieGraphRepConfig;

public class BipartiteGraphRepositoryTest {
    
    @Rule
    // a new temp folder is created for each test case
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void shoudBuildGraphAndStoreLinkMetadatas() throws Exception {
        // given
    	URL urlParent = new URL("http://urlParent.com");
    	URL urlChild1 = new URL("http://urlChild1.com");
    	URL urlChild2 = new URL("http://urlChild2.com");
    	URL urlBacklink = new URL("http://urlBacklink.com");
    	String[] anchor1 = new String[]{"words","of","anchor","1"};
    	String[] anchor2 = new String[]{"words","of","anchor","2"};
    	String pageContent = "<!DOCTYPE html><html><body><h1>My Parent Heading</h1><p>My first paragraph.</p></body></html>";
    	String pageContent1 = "<!DOCTYPE html><html><body><h1>My Child 1 Heading</h1><p>My first paragraph.</p></body></html>";
    	String title = "Backlink Title";
    	String snippet = "Backlink Snippet";
    	
    	BiparitieGraphRepConfig config = new BiparitieGraphRepConfig();
    	BipartiteGraphRepository repo = new BipartiteGraphRepository(tempFolder.newFolder().getPath(),config);
    	
        LinkMetadata lm1 = new LinkMetadata(urlParent);
        lm1.setPageContent(pageContent);
        lm1.setPageRelevance(1);
        LinkMetadata[] lms = new LinkMetadata[2];
        lms[0] = new LinkMetadata(urlChild1);
        lms[0].setAnchor(anchor1);
        lms[1] = new LinkMetadata(urlChild2);
        lms[1].setAnchor(anchor2);
        LinkMetadata lm4 = new LinkMetadata(urlChild1);
        lm4.setPageContent(pageContent1);
        lm4.setPageRelevance(2);
        LinkMetadata lm5 = new LinkMetadata();
        lm5.addBacklinkTitle(title);
        lm5.addBacklinkSnippet(snippet);
        lm5.addBacklinkUrl(urlBacklink.toString());
        LinkMetadata[] lms2 = new LinkMetadata[1];
        lms2[0]=lm5.clone();


        
        // when
        repo.insertPage(urlParent, lm1);
        repo.insertOutlinks(urlParent, lms);
        repo.insertPage(urlChild1, lm4);
        repo.insertBacklinks(urlChild2, lms2);
        repo.commit();
        
        // Display data
        for(LinkMetadata LM : repo.getLMs()){
        	System.out.println(LM.toString());
        }
        
        
        // then
        assertThat(repo.getID(urlParent.toString()), is("1"));
        assertThat(repo.getID(urlChild1.toString()), is("2"));
        assertThat(repo.getID(urlChild2.toString()), is("3"));
        assertThat(repo.getID(urlBacklink.toString()), is("4"));
        
        assertThat(repo.getOutlinkIDs("1")[0], is("2"));
        assertThat(repo.getOutlinkIDs("1")[1], is("3"));
        assertNull(repo.getBacklinkIDs("1"));
        assertThat(repo.getBacklinkIDs("2").length, is(1));
        assertThat(repo.getBacklinkIDs("2")[0], is("1"));
        assertThat(repo.getBacklinkIDs("3").length, is(2));
        assertThat(repo.getBacklinkIDs("3")[1], is("4"));
        
        assertNull(repo.getBacklinkLM(urlParent));
        assertNull(repo.getOutlinkLM(urlParent));
        assertNotNull(repo.getLM(urlParent));
        
        assertNotNull(repo.getOutlinkLM(urlChild1));
        
        assertThat(repo.getOutlinkLMs().length,is(2));
        
        assertThat(repo.getOutlinksLM(urlParent).length,is(2));
        assertThat(repo.getOutlinksLM(urlParent)[0].getIsTargetOfOutlink(),is(true));
        assertThat(repo.getOutlinksLM(urlParent)[0].getIsPageInfoSet(),is(true));
        assertThat(repo.getOutlinksLM(urlParent)[0].getAnchorString(),is("words of anchor 1 "));
        assertThat(repo.getOutlinksLM(urlParent)[0].getPageContent(),is(pageContent1));
        assertThat(repo.getOutlinksLM(urlParent)[0].getPageRelevance(),is(2));
       
        assertThat(repo.getBacklinksLM(urlChild2).length,is(1));
        assertThat(repo.getBacklinksLM(urlChild2)[0].getIsPageInfoSearchEngineSet(),is(true));
        assertThat(repo.getBacklinksLM(urlChild2)[0].getSearchEngineSnippet(),is(snippet));
        
        assertThat(repo.getLM(urlChild2).getIsTargetOfOutlink(),is(true));
        assertThat(repo.getLM(urlChild2).getIsTargetOfBacklink(),is(true));
        assertThat(repo.getLM(urlChild2).getBacklinkUrls().size(),is(1));
        assertThat(repo.getLM(urlChild2).getBacklinkUrls().get(0),is(urlBacklink.toString()));
        
    }
    
}