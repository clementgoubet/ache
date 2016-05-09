package focusedCrawler.link.classifier;

import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;

import weka.classifiers.Classifier;
import weka.core.Instances;
import focusedCrawler.link.LinkMetadata;
import focusedCrawler.link.classifier.builder.Instance;
import focusedCrawler.link.classifier.builder.LinkMetadataWrapper;
import focusedCrawler.link.frontier.LinkRelevance;
import focusedCrawler.util.parser.PaginaURL;

/**
 * This class implements the link classifier for the hub links.
 * @author lbarbosa
 *
 */
public class LinkClassifierHub implements LinkClassifier{

	private Classifier classifier;
	private Instances instances;
	private LinkMetadataWrapper wrapper;
	private String[] attributes;
	
	public LinkClassifierHub(){
		
	}
	
	public LinkClassifierHub(Classifier classifier, Instances instances, LinkMetadataWrapper wrapper,String[] attributes) {
		this.classifier = classifier;
		this.instances = instances;
		this.wrapper = wrapper;
		this.attributes = attributes;
	}
	
	public LinkRelevance classify(LinkMetadata lm, int type) throws LinkClassifierException {
		LinkRelevance result = null;
		try {
			if(classifier == null){
				result = new LinkRelevance(lm.getLink(), type, LinkRelevance.DEFAULT_HUB_RELEVANCE+1);				
			}else{
				Map<String, Instance> urlWords = wrapper.extractLinks(lm, attributes);
				Iterator<String> iter = urlWords.keySet().iterator();
				while(iter.hasNext()){
					String url = (String)iter.next();
			        Instance instance = (Instance)urlWords.get(url);
			        double[] values = instance.getValues();
			        weka.core.Instance instanceWeka = new weka.core.Instance(1, values);
			        instanceWeka.setDataset(instances);
			        double[] prob = classifier.distributionForInstance(instanceWeka);
			        double relevance = LinkRelevance.DEFAULT_HUB_RELEVANCE + prob[0]*100;
			        result = new LinkRelevance(lm.getLink(),type,relevance);
				}
			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public LinkRelevance[] classify(PaginaURL page, int type)
			throws LinkClassifierException {
		// TODO Auto-generated method stub
		return null;
	}
	
	public LinkRelevance[] classify(LinkMetadata[] lms, int type) throws LinkClassifierException{
		return null;
	}
	
}
