package focusedCrawler.link.classifier;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import weka.classifiers.Classifier;
import weka.core.Instances;
import focusedCrawler.link.LinkMetadata;
import focusedCrawler.link.classifier.builder.Instance;
import focusedCrawler.link.classifier.builder.LinkMetadataWrapper;
import focusedCrawler.link.frontier.LinkRelevance;
import focusedCrawler.util.parser.PaginaURL;

public class LinkClassifierAuthority implements LinkClassifier{

	  private LinkMetadataWrapper wrapper;
	  private String[] attributes;
	  private Classifier classifier;
	  private Instances instances;
	  
	  public LinkClassifierAuthority(Classifier classifier, Instances instances, LinkMetadataWrapper wrapper,String[] attributes) {
		  this.wrapper = wrapper;
		  this.attributes = attributes;
		  this.classifier = classifier;
		  this.instances = instances;
	  }
	  
	  public LinkClassifierAuthority() {
	  }

	  
	  public LinkClassifierAuthority(LinkMetadataWrapper wrapper,String[] attributes) {
		  this.wrapper = wrapper;
		  this.attributes = attributes;
	  }

	  
	  public LinkRelevance[] classify(PaginaURL page, int type) throws LinkClassifierException {
		  if(type == LinkRelevance.TYPE_BACKLINK_BACKWARD){
			  throw new IllegalArgumentException("This classifier is not suited for TYPE_BACKLINK_BACKWARD (classifying whether a page URL is worth backlinking)");
		  }
		  try {
		      LinkRelevance[] linkRelevance = null;
			  if(classifier != null){
				  HashMap<String, Instance> urlWords = wrapper.extractLinks(page, attributes);
				  linkRelevance = new LinkRelevance[urlWords.size()];
		          
		          int count = 0;
		          
		          for (String urlStr : urlWords.keySet()) {
		              
		        	  URL url = new URL(urlStr);
		        	  double relevance = -1;
		        	  if(!page.getURL().getHost().equals(url.getHost())){
		        		  Instance instance = urlWords.get(urlStr);
		        		  double[] values = instance.getValues();
		        		  weka.core.Instance instanceWeka = new weka.core.Instance(1, values);
		        		  instanceWeka.setDataset(instances);
		        		  double[] prob = classifier.distributionForInstance(instanceWeka);
		        		  relevance = LinkRelevance.DEFAULT_AUTH_RELEVANCE + (prob[0]*100);
		        	  }
			          linkRelevance[count] = new LinkRelevance(url, type, relevance);
			          count++;
		          }
			  }else{
				  LinkMetadata[] lms = page.getLinkMetadatas();
				  linkRelevance = new LinkRelevance[lms.length];
				  for (int i = 0; i < lms.length; i++) {
					  double relevance = -1;
					  if(!page.getURL().getHost().equals(lms[i].getLink().getHost())){
						  relevance = LinkRelevance.DEFAULT_AUTH_RELEVANCE+1;
					  }
					  linkRelevance[i] = new LinkRelevance(lms[i].getLink(), type, relevance);
				  }
			  }
			  return linkRelevance;
		  }catch (MalformedURLException ex) {
			  ex.printStackTrace();
			  throw new LinkClassifierException(ex.getMessage());
		  }catch (Exception e) {
			  e.printStackTrace();
			  throw new LinkClassifierException(e.getMessage());
		  }
	  }

	@Override
	public LinkRelevance classify(LinkMetadata lm, int type) throws LinkClassifierException {
		  LinkRelevance linkRel = null;
		  try{
		      HashMap<String, Instance> urlWords = wrapper.extractLinks(lm, attributes);
		      
	    	  for (String url : urlWords.keySet()) {
		    	  double relevance = -1;
		    	  if(isRootPage(url)){
		    		  if(classifier != null){
		    			  Instance instance = (Instance)urlWords.get(url);
		    			  double[] values = instance.getValues();
		    			  weka.core.Instance instanceWeka = new weka.core.Instance(1, values);
		    			  instanceWeka.setDataset(instances);
		    			  double[] prob = classifier.distributionForInstance(instanceWeka);
		    			  if(prob[0] == 1){
		    				  prob[0] = 0.99;
		    			  }
	    				  relevance = LinkRelevance.DEFAULT_AUTH_RELEVANCE + (prob[0]*100);		    			  
		    		  }else{
		    			  relevance = LinkRelevance.DEFAULT_AUTH_RELEVANCE+1;		            	
		    		  }
		    	  }
	    		  linkRel = new LinkRelevance(new URL(url),type,relevance);
		      }
		  } catch (MalformedURLException ex) {
			  ex.printStackTrace();
			  throw new LinkClassifierException(ex.getMessage());
		  } catch (Exception ex) {
			  ex.printStackTrace();
			  throw new LinkClassifierException(ex.getMessage());
		  }
		  return linkRel;
	}
	
	  public LinkRelevance[] classify(LinkMetadata[] lms, int type) throws LinkClassifierException{
		  if(lms == null){
			  return null;
		  }
		  else{
			  LinkRelevance[] result = new LinkRelevance[lms.length];
				for(int i=0; i< lms.length; i++){
					result[i]=classify(lms[i],type);
				}
				return result;
		  }
	  }
		  
	private boolean isRootPage(String urlStr) throws MalformedURLException {
		boolean result = false;
		URL url = new URL(urlStr);
		String file = url.getFile();
		if(file.equals("/") || file.equals("") || file.equals("index.htm") || file.equals("index.html")){
			result = true;
		}
		return result;
	}

}
