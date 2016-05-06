package focusedCrawler.link.classifier;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import focusedCrawler.link.LinkMetadata;
import focusedCrawler.link.frontier.LinkRelevance;
import focusedCrawler.util.parser.PaginaURL;

public class MaxDepthLinkClassifier implements LinkClassifier {

    private int maxDepth;
    
    public MaxDepthLinkClassifier(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    @Override
    public LinkRelevance[] classify(PaginaURL page, int type) throws LinkClassifierException {
		if(type == LinkRelevance.TYPE_BACKLINK_BACKWARD){
			throw new IllegalArgumentException("This classifier is not suited for TYPE_BACKLINK_BACKWARD (classifying whether a page URL is worth backlinking)");
		}
        List<LinkRelevance> links = new ArrayList<LinkRelevance>();
        URL[] urls = page.links();
        for (int i = 0; i < urls.length; i++) {
            
            URL url = urls[i];
            double linkRelevance = page.getRelevance() - 1;
            int currentDepth = (int) (LinkRelevance.DEFAULT_RELEVANCE - linkRelevance);
            if(currentDepth <= maxDepth) {
                links.add(new LinkRelevance(url, type, linkRelevance));
            }
        }
        return links.toArray(new LinkRelevance[links.size()]);
    }

    @Override
    public LinkRelevance classify(LinkMetadata lm, int type) throws LinkClassifierException {
        throw new java.lang.UnsupportedOperationException("Method classify(LinkNeighborhood ln) not yet implemented.");
    }
    
	public LinkRelevance[] classify(LinkMetadata[] lms, int type) throws LinkClassifierException{
		return null;
	}

}
