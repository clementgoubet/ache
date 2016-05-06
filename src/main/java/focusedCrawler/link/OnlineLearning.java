package focusedCrawler.link;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import focusedCrawler.link.classifier.LinkClassifier;
import focusedCrawler.link.classifier.builder.LinkClassifierBuilder;
import focusedCrawler.link.frontier.Frontier;
import focusedCrawler.link.frontier.LinkRelevance;
import focusedCrawler.link.linkanalysis.HITS;
import focusedCrawler.link.linkanalysis.SALSA;
import focusedCrawler.util.vsm.VSMElement;


public class OnlineLearning {

	private Frontier linkFrontier;

	private Frontier backlinkFrontier;
	
	private BipartiteGraphManager manager;
	
	private BipartiteGraphRepository rep;
	
	private LinkClassifierBuilder classifierBuilder;
	
	private String method;
	
	private String targetPath;
	
	public OnlineLearning(Frontier linkFrontier, Frontier backlinkFrontier, BipartiteGraphManager manager, LinkClassifierBuilder classifierBuilder, String method, String path){
		this.linkFrontier = linkFrontier;
		this.backlinkFrontier = backlinkFrontier;
		this.manager = manager;
		this.classifierBuilder = classifierBuilder;
		this.method = method;
		this.targetPath = path;
		this.rep = manager.getRepository();
	}
	
	public void execute() throws Exception{
		linkFrontier.commit();
		backlinkFrontier.commit();
		System.out.println("COMMIT DONE");
		if(method.equals("SALSA")){
			runSALSA(null,false);
		}
		if(method.equals("SALSA_SEED")){
			runSALSA(loadRelSites(false),false);	
		}
		if(method.equals("SALSA_CLASSIFIER")){
			runSALSA(loadRelSites(false),true);	
		}
		if(method.equals("HITS")){
			runHITS(null);
		}
		if(method.equals("HITS_1ST")){
			runHITS(loadRelSites(false));
		}
		if(method.equals("LINK_CLASSIFIERS")){
			createClassifiers(loadRelSites(false),true);
		}
		if(method.equals("FORWARD_CLASSIFIER_BINARY")){
			forwardClassifier(loadRelSites(true),true,0);
		}
		if(method.equals("FORWARD_CLASSIFIER_LEVELS")){
			forwardClassifier(loadRelSites(true),true,3);
		}

		linkFrontier.commit();
		backlinkFrontier.commit();
	}
	
	private HashSet<String> loadRelSites(boolean isDir) throws IOException{
		HashSet<String> relSites = new HashSet<String>();
		if(isDir){
			File[] dirs = new File(targetPath).listFiles();
			System.out.println(">>REL SITESs");
			for (int i = 0; i < dirs.length; i++) {
				File[] files = dirs[i].listFiles();
				for (int j = 0; j < files.length; j++) {
					String url = URLDecoder.decode(files[j].getName(), "UTF-8");
					if(!relSites.contains(url)){
						relSites.add(url);
						System.out.println(">>" + url);
					}
				}
			}
		}else{
			File file = new File(targetPath + File.separator + "entry_points");
			try(BufferedReader input = new BufferedReader(new FileReader(file))) {
    			for (String line = input.readLine(); line != null; line = input.readLine()) {
    				if(line.startsWith("------")){
    					String host = line.replace("-", "");
    					String url = "http://" + host + "/";
    					if(!relSites.contains(url)){
    						relSites.add(url);
    						System.out.println(">>" + url);
    					}
    				}
    			}
			}
		}
		return relSites;
	}
	
	public void runSALSA(HashSet<String> relSites, boolean useClassifier) throws Exception{
		SALSA salsa = new SALSA(rep);
		if(relSites != null){
			HashMap<String,VSMElement> probs = new HashMap<String, VSMElement>();
			if(useClassifier){
				probs = createClassifiers(relSites,false);
			}else{
				Iterator<String> iter = relSites.iterator();
				while(iter.hasNext()){
					String site = iter.next();
					System.out.println(">>>>>>>>" + site);
					String id = rep.getID(site);
					if(id == null){
						continue;
					}
					probs.put(id + "_auth", new VSMElement(id,1));
					String[] backlinks = rep.getBacklinkIDs(id);
					for (int i = 0; i < backlinks.length; i++) {
						VSMElement elem = probs.get(id + "_hub");
						if(elem == null){
							elem = new VSMElement(id, 0);
							probs.put(id + "_hub", elem);
						}
						elem.setWeight(elem.getWeight()+1);
					}
				}
			}
			normalize(probs);
			salsa.setNodeRelevance(probs);
		}
		salsa.execute();
		VSMElement[] hubRelevance = salsa.getHubValues();
		double rel = 199;
		System.out.println(">>>>>>>FRONTIER UPDATE...");
		LinkRelevance lr = new LinkRelevance(new URL(hubRelevance[0].getWord()), LinkRelevance.TYPE_FORWARD, rel);
		linkFrontier.update(lr);
		for (int i = 1; i < hubRelevance.length; i++) {
			if(i % (hubRelevance.length/99) == 0 ){
				rel--;
			}
			if(hubRelevance[i].getWord() != null){
				lr = new LinkRelevance(new URL(hubRelevance[i].getWord()), LinkRelevance.TYPE_FORWARD, rel);
//				if(i < 50){
//					System.out.println("###" + lr.getURL().toString() + "=" + lr.getRelevance());	
//				}
				linkFrontier.update(lr);
			}
		}
		VSMElement[] authRelevance = salsa.getAuthValues();
		rel = 299;
		lr = new LinkRelevance(new URL(authRelevance[0].getWord()), LinkRelevance.TYPE_FORWARD, rel);
		linkFrontier.update(lr);
		for (int i = 1; i < authRelevance.length; i++) {
			if(i % (authRelevance.length/99) == 0 ){
				rel--;
			}
			if(authRelevance[i].getWord() != null){
				lr = new LinkRelevance(new URL(authRelevance[i].getWord()), LinkRelevance.TYPE_FORWARD, rel);
//				if(i < 500){
//					System.out.println("###" + i + ":" + lr.getURL().toString() + "=" + lr.getRelevance() + ":" + authRelevance[i].getWeight());					
//				}
				linkFrontier.update(lr);
			}
		}
		salsa = null;
	}

	
	private void normalize(HashMap<String,VSMElement> values){
		//normalize
		double totalAuth = 0;
		double totalHub = 0;
		Iterator<String> iter = values.keySet().iterator();
		while(iter.hasNext()){
			String key = iter.next();
			VSMElement elem = values.get(key);
			if(key.endsWith("_auth")){
				totalAuth = totalAuth + elem.getWeight();				
			}
			if(key.endsWith("_hub")){
				totalHub = totalHub + elem.getWeight();
			}
		}		
		iter = values.keySet().iterator();
		while(iter.hasNext()){
			String key = iter.next();
			VSMElement elem = values.get(key);
			if(key.endsWith("_auth")){
				elem.setWeight(elem.getWeight()/totalAuth);				
			}
			if(key.endsWith("_hub")){
				elem.setWeight(elem.getWeight()/totalHub);
			}
		}		
	}

	
	public void runHITS(HashSet<String> relSites) throws Exception{
		HITS hits = new HITS(rep);
		if(relSites != null){
			hits.firstIteration(relSites);
		}else{
			hits.originalHITS();	
		}
		System.out.println(">>>>>>>FRONTIER UPDATE...");
		VSMElement[] hubRelevance = hits.getHubRelevance();
		double rel = 199;
		LinkRelevance lr = new LinkRelevance(new URL(hubRelevance[0].getWord()), LinkRelevance.TYPE_FORWARD, rel); 
		linkFrontier.update(lr);
		for (int i = 1; i < hubRelevance.length; i++) {
			if(i % (hubRelevance.length/99) == 0 ){
				rel--;
			}
			if(hubRelevance[i].getWord() != null){
				lr = new LinkRelevance(new URL(hubRelevance[i].getWord()), LinkRelevance.TYPE_FORWARD, rel); 
				linkFrontier.update(lr);
			}
		}
		VSMElement[] authRelevance = hits.getAuthRelevance();
		rel = 299;
		lr = new LinkRelevance(new URL(authRelevance[0].getWord()), LinkRelevance.TYPE_FORWARD, rel);  
		linkFrontier.update(lr);
		for (int i = 1; i < authRelevance.length; i++) {
			if(i % (authRelevance.length/99) == 0 ){
				rel--;
			}
			if(authRelevance[i].getWord() != null){
				lr = new LinkRelevance(new URL(authRelevance[i].getWord()), LinkRelevance.TYPE_FORWARD, rel);
//				System.out.println(">>>>>AUTH:" + lr.getURL().toString() + "=" + lr.getRelevance());
				linkFrontier.update(lr);
			}
		}
	}

	
	private void forwardClassifier(HashSet<String> relSites, boolean updateFrontier, int levels) throws Exception{
		System.out.println(">>>BUILDING OUTLINK CLASSIFIER...:");
		LinkClassifier outlinkClassifier = classifierBuilder.forwardlinkTraining(relSites,levels, "LinkClassifierImpl");
		if(updateFrontier){
			manager.setOutlinkClassifier(outlinkClassifier);
		}
		LinkMetadata[] outLMs = rep.getOutlinkLMs();
		for (int i = 0; i < outLMs.length; i++) {
			if(outLMs[i] != null){
				LinkRelevance lr = outlinkClassifier.classify(outLMs[i], 1); // Backward compatibility note: old relevance corresponds to type 1
				if(updateFrontier){
					linkFrontier.update(lr);
				}
			}
		}
	}
	
	private HashMap<String,VSMElement> createClassifiers(HashSet<String> relSites, boolean updateFrontier) throws Exception{
		HashMap<String,VSMElement> elems = new HashMap<String,VSMElement>();
		System.out.println(">>>BUILDING OUTLINK CLASSIFIER...:");
		LinkClassifier outlinkClassifier = classifierBuilder.forwardlinkTraining(relSites,0,"LinkClassifierAuthority");
		if(updateFrontier){
			manager.setOutlinkClassifier(outlinkClassifier);
		}
		LinkMetadata[] outLMs = rep.getOutlinkLMs();
		HashSet<String> visitedAuths = linkFrontier.visitedAuths();
		HashSet<String> usedLinks = new HashSet<String>();
		for (int i = 0; i < outLMs.length; i++) {
			if(outLMs[i] != null){
				LinkRelevance lr = outlinkClassifier.classify(outLMs[i],LinkRelevance.TYPE_FORWARD);
				if(updateFrontier){
					linkFrontier.update(lr);
					usedLinks.add(lr.getURL().toString());
				}
				String id = rep.getID(outLMs[i].getLink().toString());
				if(id != null){
					VSMElement elem = new VSMElement(id, (lr.getRelevance()-200)/100);
					if(visitedAuths.contains(outLMs[i].getLink().toString())){
						if(relSites.contains(outLMs[i].getLink().toString())){
							elem.setWeight(1d);
						}else{
							elem.setWeight(0.0000001);
						}
					}
					elems.put(id + "_auth",elem);
				}
			}
		}
		System.out.println(">>>BUILDING BACKLINK BACKWARD CLASSIFIER...");
		LinkClassifier backlinkClassifier = classifierBuilder.backlinkTraining(elems);
		if(updateFrontier){
			manager.setBacklinkForwardClassifier(backlinkClassifier);
		}
		LinkMetadata[] backLNs = rep.getBacklinkLMs();
		for (int i = 0; i < backLNs.length; i++) {
			if(backLNs[i] != null){
				LinkRelevance lr = backlinkClassifier.classify(backLNs[i],LinkRelevance.TYPE_BACKLINK_FORWARD);
				if(updateFrontier && lr != null && !usedLinks.contains(lr.getURL().toString())){
					backlinkFrontier.update(lr);
				}
				String id = rep.getID(backLNs[i].getLink().toString());
				if(id != null && lr != null){
					VSMElement elem = new VSMElement(id, (lr.getRelevance()-100)/100);
					elems.put(id + "_hub",elem);
				}
			}
		}
		return elems;
	}
	
}
