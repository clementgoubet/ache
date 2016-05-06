package focusedCrawler.link.classifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Random;

import focusedCrawler.link.LinkMetadata;
import focusedCrawler.link.classifier.builder.Instance;
import focusedCrawler.link.classifier.builder.LinkNeighborhoodWrapper;
import focusedCrawler.link.frontier.LinkRelevance;
import focusedCrawler.util.parser.PaginaURL;

public class LinkClassifierBreadthSearch implements LinkClassifier {

    private LinkNeighborhoodWrapper wrapper;
    private String[] attributes;
    private Random randomGenerator;

    public LinkClassifierBreadthSearch(LinkNeighborhoodWrapper wrapper, String[] attribute) {
        this.wrapper = wrapper;
        this.attributes = attribute;
        this.randomGenerator = new Random();
    }

    public LinkRelevance[] classify(PaginaURL page, int type) throws LinkClassifierException {
		if(type == LinkRelevance.TYPE_BACKLINK_BACKWARD){
			throw new IllegalArgumentException("This classifier is not suited for TYPE_BACKLINK_BACKWARD (classifying whether a page URL is worth backlinking)");
		}
        try {
            HashMap<String, Instance> urlWords = wrapper.extractLinks(page, attributes);

            LinkRelevance[] linkRelevance = new LinkRelevance[urlWords.size()];
            
            int count = 0;
            for(String url : urlWords.keySet()) {
                
                int level = (int) (page.getRelevance() / 100);
                double relevance = (level - 1) * 100 + randomGenerator.nextInt(100);
                if (relevance < -1) {
                    relevance = -1;
                }
                linkRelevance[count] = new LinkRelevance(new URL(url), type, relevance);
                count++;
            }
            return linkRelevance;
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            throw new LinkClassifierException(ex.getMessage());
        }
    }

    public LinkRelevance classify(LinkMetadata lm, int type) throws LinkClassifierException {
        // TODO Auto-generated method stub
        return null;
    }
    
	public LinkRelevance[] classify(LinkMetadata[] lms, int type) throws LinkClassifierException{
		return null;
	}

}
