package focusedCrawler.link.classifier.builder;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import focusedCrawler.link.BipartiteGraphRepository;
import focusedCrawler.link.LinkMetadata;
import focusedCrawler.link.classifier.LinkClassifier;
import focusedCrawler.link.classifier.LinkClassifierException;
import focusedCrawler.link.classifier.LinkClassifierFactoryImpl;
import focusedCrawler.link.frontier.Frontier;
import focusedCrawler.link.frontier.LinkRelevance;
import focusedCrawler.util.parser.LinkNeighborhood;
import focusedCrawler.util.parser.PaginaURL;
import focusedCrawler.util.persistence.Tuple;
import focusedCrawler.util.string.PorterStemmer;
import focusedCrawler.util.string.StopList;
import focusedCrawler.util.vsm.VSMElement;
import focusedCrawler.util.vsm.VSMElementComparator;
import weka.classifiers.Classifier;
import weka.core.Instances;

public class LinkClassifierBuilder {
	
    private static final Logger logger = LoggerFactory.getLogger(LinkClassifierFactoryImpl.class);
	
	private BipartiteGraphRepository graphRep;
	
	private LinkMetadataWrapper wrapper;

	private StopList stoplist;

	private PorterStemmer stemmer;

	private Frontier linkFrontier;

	private String[] features;
	
	private String modelPath;
	
	public LinkClassifierBuilder(BipartiteGraphRepository graphRep, StopList stoplist, LinkMetadataWrapper wrapper, Frontier linkFrontier, String modelPath){
		this.graphRep = graphRep;
		this.stemmer = new PorterStemmer();
		this.stoplist = stoplist;
		this.wrapper = wrapper;
		this.linkFrontier = linkFrontier;
		this.modelPath = modelPath;
	}
	
	public LinkClassifierBuilder(LinkMetadataWrapper wrapper, StopList stoplist, String modelPath) throws IOException{
		this.stoplist = stoplist;
		this.stemmer = new PorterStemmer();
		this.wrapper = wrapper;
		this.modelPath = modelPath;
	}

	@SuppressWarnings("deprecation")
	public void writeFile(ArrayList<ArrayList<LinkMetadata>> instances, String output) throws IOException{
		String weka = null;
		try {
			weka = createWekaInput(instances,LinkRelevance.TYPE_FORWARD);
		} catch (LinkClassifierException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		OutputStream fout= new FileOutputStream(output,false);
    	OutputStream bout= new BufferedOutputStream(fout);
    	OutputStreamWriter outputFile = new OutputStreamWriter(bout);
    	outputFile.write(weka);
    	outputFile.close();
	}
	
	public Classifier loadClassifier(Reader reader) throws Exception{
		Instances data = new Instances(reader);
		reader.close();
		data.setClassIndex(data.numAttributes() - 1);
//		System.out.println(data.toString());
		 // create new instance of scheme
		Classifier classifier = new weka.classifiers.functions.SMO();
		//Classifier classifier = new weka.classifiers.bayes.NaiveBayesMultinomial();
		 // set options
		classifier.setOptions(weka.core.Utils.splitOptions("-C 1.0 -L 0.0010 -P 1.0E-12 -N 0 -M -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\" -no-cv"));
		try{
			classifier.buildClassifier(data);
		}catch(Exception e){
			throw new LinkClassifierException("Building of classifier with WEKA failed with error: "+ e);
		}
		return classifier;
	}
	
	public LinkClassifier forwardlinkTraining(HashSet<String> relSites, int levels, String className) throws Exception{
		ArrayList<ArrayList<LinkMetadata>> instances = null;
		if(levels == 0){//pos and neg case
			instances = new ArrayList<ArrayList<LinkMetadata>>(2);
			instances.add(new ArrayList<LinkMetadata>());
			instances.add(new ArrayList<LinkMetadata>());			
		}else{//levels case
			instances = new ArrayList<ArrayList<LinkMetadata>>(levels);
			for (int i = 0; i < levels; i++) {
				instances.add(new ArrayList<LinkMetadata>());	
			}
		}
		HashSet<String> visitedLinks = linkFrontier.visitedLinks();
		for(Iterator<String> iterator = visitedLinks.iterator(); iterator.hasNext();) {
			String strURL = (String) iterator.next();
			URL url = new URL(strURL);
			URL normalizedURL = url;
			LinkMetadata lm = graphRep.getOutlinkLM(normalizedURL);
			if(lm == null){
				continue;
			}

			if(levels == 0){
				if(relSites.contains(normalizedURL.toString())){
					instances.get(0).add(lm);
				}else{
					if(instances.get(1).size() < instances.get(0).size()){
						instances.get(1).add(lm);
					}
				}
			}else{
				if(relSites.contains(lm.getUrl())){
					instances.get(0).add(lm);
					addBacklinks(instances,lm.getLink(),1, levels, relSites);
				}
			}
		}
		// check that there are enough instances for training
		if(levels > 0){
			for(int i = 1; i<levels; i++){
				if(instances.get(i).size()<10){
					logger.info("Level "+i+" have too few instances: "+instances.get(i).size()+". Switching to BINARY classifier.");
					return this.forwardlinkTraining(relSites, 0, className);
				}
			}
		}
		StringReader reader;
		Classifier classifier;
		try{
			reader = new StringReader(createWekaInput(instances,LinkRelevance.TYPE_FORWARD));
			classifier = loadClassifier(reader);
		}catch(LinkClassifierException e){
			throw new LinkClassifierException(e.getMessage());
		}
		weka.core.SerializationHelper.write(modelPath+"/link_classifier_forward.model",classifier);
		OutputStream fout= new FileOutputStream(modelPath+"/link_classifier_forward.features",false);
    	OutputStream bout= new BufferedOutputStream(fout);
    	OutputStreamWriter outputFile = new OutputStreamWriter(bout);
    	for (int i = 0; i < features.length; i++) {
        	outputFile.write(features[i] + " ");			
		}
    	outputFile.close();

		String[] classValues = null;
		if(levels == 0){
			classValues = new String[]{"POS","NEG"};
		}else{
			classValues = new String[]{"0","1","2"};
		}
		return LinkClassifierFactoryImpl.createLinkClassifierImpl(features, classValues, classifier, className,levels);
	}
	
	
	private void addBacklinks(ArrayList<ArrayList<LinkMetadata>> instances, URL url, int level, int limit, HashSet<String> relSites) throws IOException{
		if(level >= limit){
			return;
		}
		LinkMetadata[] backlinks = graphRep.getBacklinksLM(url);
		if(backlinks != null){
			for (int i = 0; i < backlinks.length; i++) {
				String tempURL = backlinks[i].getUrl();
				if(!relSites.contains(tempURL)){
					instances.get(level).add(backlinks[i]);				
				}
				addBacklinks(instances,new URL(tempURL),level+1,limit,relSites);
			}
		}
	}
	
	public LinkClassifier backlinkBackwardTraining(HashSet<String> relSites, String className) throws Exception{
		ArrayList<ArrayList<LinkMetadata>> instances = null;
		//pos and neg case
		instances = new ArrayList<ArrayList<LinkMetadata>>(2);
		instances.add(new ArrayList<LinkMetadata>());
		instances.add(new ArrayList<LinkMetadata>());			
		HashSet<String> visitedLinks = linkFrontier.visitedLinks();
		// Add LM of visited link if backlinking leads to another relevant page
		for(Iterator<String> iterator = visitedLinks.iterator(); iterator.hasNext();) {
			String strURL = (String) iterator.next();
			URL normalizedURL = new URL(strURL);
			boolean positive = false;
			LinkMetadata[] lms = graphRep.getBacklinksLM(normalizedURL);
			if(lms == null){
				continue;
			}
			for(LinkMetadata lm : lms){
				String[] urls = graphRep.getChildren(lm.getUrl());
				for(String url : urls){
					if(!url.equals(strURL) && relSites.contains(url)){
						positive = true;
					}
				}
			}
			LinkMetadata lm = graphRep.getLM(normalizedURL);
			if(lm.getIsPageInfoSet()){
				if(positive){
					instances.get(0).add(lm);
				}
				else if(instances.get(1).size() < instances.get(0).size()){
					instances.get(1).add(lm);
				}
			}
		}
		StringReader reader;
		Classifier classifier;
		try{
			reader = new StringReader(createWekaInput(instances,LinkRelevance.TYPE_BACKLINK_BACKWARD));
			classifier = loadClassifier(reader);
		}catch(LinkClassifierException e){
			throw new LinkClassifierException(e.getMessage());
		}
		weka.core.SerializationHelper.write(modelPath+"/backlink_classifier_backward.model",classifier);
		OutputStream fout= new FileOutputStream(modelPath+"/backlink_classifier_backward.features",false);
    	OutputStream bout= new BufferedOutputStream(fout);
    	OutputStreamWriter outputFile = new OutputStreamWriter(bout);
    	for (int i = 0; i < features.length; i++) {
        	outputFile.write(features[i] + " ");			
		}
    	outputFile.close();

		String[] classValues = null;
		classValues = new String[]{"POS","NEG"};
		return LinkClassifierFactoryImpl.createLinkClassifierImpl(features, classValues, classifier, className,0);
	}
	
	public LinkClassifier backlinkForwardTraining(HashSet<String> relSites, String className) throws Exception{
		ArrayList<ArrayList<LinkMetadata>> instances = null;
		//pos and neg case
		instances = new ArrayList<ArrayList<LinkMetadata>>(2);
		instances.add(new ArrayList<LinkMetadata>());
		instances.add(new ArrayList<LinkMetadata>());			
		HashSet<String> visitedLinks = linkFrontier.visitedLinks();
		
//		System.out.println("TOTAL NUMBER BACKLINKS "+graphRep.getBacklinkLMs().length);
//		System.out.println("VISTED LINKS");
//		for(String s : visitedLinks){
//			System.out.println(s);
//		}
		// Add LM of backlink if leads to another relevant page
		for(Iterator<String> iterator = visitedLinks.iterator(); iterator.hasNext();) {
			String strURL = (String) iterator.next();
			URL normalizedURL = new URL(strURL);
			LinkMetadata[] lms = graphRep.getBacklinksLM(normalizedURL);
			if(lms == null){
				continue;
			}
			for(LinkMetadata lm : lms){
				String[] urls = graphRep.getChildren(lm.getUrl());
				for(String url : urls){
					if(!url.equals(strURL) && relSites.contains(url)){
						instances.get(0).add(lm);
						System.out.println("\t"+strURL+" --> " +lm.getUrl() + " --> "+ url);
					}
					else if(instances.get(1).size() < instances.get(0).size()){
						instances.get(1).add(lm);
					}
				}
			}
		}
		StringReader reader;
		Classifier classifier;
		try{
			String weka = createWekaInput(instances,LinkRelevance.TYPE_BACKLINK_FORWARD);
//			System.out.println(weka);
			reader = new StringReader(weka);
			classifier = loadClassifier(reader);
		}catch(LinkClassifierException e){
			throw new LinkClassifierException(e.getMessage());
		}
		weka.core.SerializationHelper.write(modelPath+"/backlink_classifier_forward.model",classifier);
		OutputStream fout= new FileOutputStream(modelPath+"/backlink_classifier_forward.features",false);
    	OutputStream bout= new BufferedOutputStream(fout);
    	OutputStreamWriter outputFile = new OutputStreamWriter(bout);
    	for (int i = 0; i < features.length; i++) {
        	outputFile.write(features[i] + " ");			
		}
    	outputFile.close();

		String[] classValues = null;
		classValues = new String[]{"POS","NEG"};
		return LinkClassifierFactoryImpl.createLinkClassifierImpl(features, classValues, classifier, className,0);
	}
	
	@SuppressWarnings("deprecation")
	public LinkClassifier backlinkTraining(HashMap<String,VSMElement> outlinkWeights) throws Exception{
		ArrayList<VSMElement> trainingSet = new ArrayList<VSMElement>();
		Tuple<String>[] tuples = graphRep.getChildrenGraph();
		for (int i = 0; i < tuples.length; i++) {
			String hubId = tuples[i].getKey();
			String[] outlinks = tuples[i].getValue().split("###");
			double totalProb = 0;
			for (int j = 0; j < outlinks.length; j++) {
				VSMElement elem = outlinkWeights.get(outlinks[j]+"_auth");
				if(elem != null){
					totalProb = totalProb + elem.getWeight();
				}
			}
			String url = graphRep.getHubURL(hubId);
			if(url != null && outlinks.length > 20){
				LinkMetadata ln = graphRep.getBacklinkLM(new URL(url));
				if(ln != null){
					VSMElement elem = new VSMElement(ln.getLink().toString() + ":::" + ln.getAroundString(), totalProb/outlinks.length);
					trainingSet.add(elem);
				}
			}
		}
		System.out.println("TOTAL TRAINING:" + trainingSet.size());
		
		ArrayList<ArrayList<LinkMetadata>> instances = new ArrayList<ArrayList<LinkMetadata>>(2);
		ArrayList<LinkMetadata> posSites = new ArrayList<LinkMetadata>();
		ArrayList<LinkMetadata> negSites = new ArrayList<LinkMetadata>();
		instances.add(posSites);
		instances.add(negSites);
		Collections.sort(trainingSet,new VSMElementComparator());
		ArrayList<LinkMetadata> allLMs = new ArrayList<LinkMetadata>();
		for (int i = 0; i < trainingSet.size(); i++) {
			String[] parts = trainingSet.get(i).getWord().split(":::");
			LinkMetadata lm = new LinkMetadata(parts[0]);
			if(parts.length > 1){
				StringTokenizer tokenizer = new StringTokenizer(parts[1]," ");
				ArrayList<String> aroundTemp = new ArrayList<String>();
				while(tokenizer.hasMoreTokens()){
					aroundTemp.add(tokenizer.nextToken());
	   		  	}
	   		  	String[] aroundArray = new String[aroundTemp.size()];
	   		  	aroundTemp.toArray(aroundArray);
	   		  	lm.setAround(aroundArray);
			}
//			System.out.println(i + ":" + trainingSet.get(i).getWord() + "=" + trainingSet.get(i).getWeight());
			allLMs.add(lm);
		}
		int sampleSize = Math.min(5000,allLMs.size()/2);
		for (int i = 0; i < allLMs.size(); i++) {
			if(posSites.size() < sampleSize){
//				System.out.println(">>" +allLNs.get(i).getLink().toString());
				posSites.add(allLMs.get(i));
			}
		}
		for (int i = allLMs.size()-1; i >= 0 ; i--) {
			if(negSites.size() < sampleSize){
				negSites.add(allLMs.get(i));
			}
		}
		LinkNeighborhood[] pos = new LinkNeighborhood[posSites.size()];
		posSites.toArray(pos);
		LinkNeighborhood[] neg = new LinkNeighborhood[negSites.size()];
		negSites.toArray(neg);
//		execute(pos,neg, new File("/home/lbarbosa/parallel_corpus/pc_crawler1/wekaInput.arff"));
		StringReader reader = new StringReader(createWekaInput(instances,LinkRelevance.TYPE_BACKLINK_BACKWARD));
		Classifier classifier = loadClassifier(reader);
		String[] classValues = new String[]{"POS","NEG"};
		return LinkClassifierFactoryImpl.createLinkClassifierImpl(features, classValues, classifier, "LinkClassifierHub",0);
	}


	/**
	 * Creates the weka input file
	 * @param instances
	 * @param backlink
	 * @return
	 * @throws IOException
	 * @throws LinkClassifierException 
	 */
	private String createWekaInput(ArrayList<ArrayList<LinkMetadata>> instances, int type) throws IOException, LinkClassifierException {
		
 		System.out.println("CREATING WEKA INPUT");
 		for(int i = 0; i<instances.size();i++){
 			System.out.println("Category "+i+" has "+instances.get(i).size()+" instances");
 		}
// 		for(ArrayList<LinkMetadata> al : instances){
// 			System.out.println("NEW CATEGORY");
// 			for(LinkMetadata lm : al){
// 				System.out.println(lm.toString());
// 			}
// 		}
		
		StringBuffer output = new StringBuffer();
		output.append("@relation classifier\n");
		ArrayList<LinkMetadata> allInstances = new ArrayList<LinkMetadata>();
		for (int i = 0; i < instances.size(); i++) {
			allInstances.addAll(instances.get(i));
		}
		features = selectBestFeatures(allInstances,type);
		for (int i = 0; i < features.length; i++) {
			output.append ("@attribute " + features[i] + " REAL \n");
		}
		output.append("@attribute class {");
		for (int i = 1; i < instances.size(); i++) {
			output.append(i+",");	
		}
		output.append(instances.size()+"}\n");
		output.append("\n");
		output.append("@data\n");
		output.append(generatLines(features,instances,type));
//		dout.writeBytes(output.toString());
//		dout.close();
//		StringReader reader = new StringReader(output.toString());
		return output.toString();
	}

	/**
	 * This method creates the a line in the weka file for each instance
	 * @param features
	 * @param instances
	 * @return
	 * @throws IOException
	 */
	private String generatLines(String[] features, ArrayList<ArrayList<LinkMetadata>> instances, int type) throws IOException {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < instances.size(); i++) {
			ArrayList<LinkMetadata> level = instances.get(i);
//			System.out.println(level.size());
			for (int j = 0; j < level.size(); j++) {
				LinkMetadata lm = level.get(j);
				StringBuffer line = new StringBuffer();
				HashMap<String, Instance> featureValue = wrapper.extractData(lm,features,type);
//				System.out.println("GENERATE: "+featureValue.values().toArray()[0].toString());
				Iterator<String> iter = featureValue.keySet().iterator();
				while(iter.hasNext()){
					String url = (String) iter.next();
					Instance instance = (Instance) featureValue.get(url);
					double[] values = instance.getValues();
					line.append("{");
					boolean containsValue = false;
					for (int l = 0; l < values.length; l++) {
						if(values[l] > 0){
							containsValue = true;
							line.append(l + " " +(int)values[l]);
							line.append(",");
						}
					}
					line.append(values.length + " " + (i+1));
					line.append("}");
					line.append("\n");
					if(containsValue){
						buffer.append(line);
					}else{
						line = new StringBuffer();        	   
					}
				}
			}
		}
		return buffer.toString();
	}
	

	/**
	 * This method selects the  features to be used by the classifier.
	 * @param allNeighbors
	 * @param backlink
	 * @return
	 * @throws MalformedURLException
	 * @throws LinkClassifierException 
	 */
	private String[] selectBestFeatures(ArrayList<LinkMetadata> allNeighbors, int type) throws MalformedURLException, LinkClassifierException{
		ArrayList<String> finalWords = new ArrayList<>();
		String[] selectedFeatures;
		
		if(type == LinkRelevance.TYPE_FORWARD){
			Set<String> usedURLTemp = new HashSet<>();
			FrequencyMap urlWords = new FrequencyMap("url",stoplist,stemmer, new FilterData(150,2));
			FrequencyMap anchorWords = new FrequencyMap("anchor",stoplist,stemmer, new FilterData(150,2));
			FrequencyMap aroundWords = new FrequencyMap("around",stoplist,stemmer, new FilterData(100,2));

			for (int l = 0; l < allNeighbors.size(); l++) {
				LinkMetadata element = allNeighbors.get(l);
				anchorWords.addWords(element.getAnchor());
				aroundWords.addWords(element.getAround());
				if(!usedURLTemp.contains(element.getUrl())){
					usedURLTemp.add(element.getUrl());
					PaginaURL pageParser = new PaginaURL(new URL("http://"),element.getLink().getFile().toString(), stoplist);
					urlWords.addWords(pageParser.palavras());
				}
			}
			
//			System.out.println(anchorWords.toString());
		
			if(!aroundWords.getMap().isEmpty()){
				ArrayList<WordFrequency> aroundFinal = aroundWords.filter(null);
				urlWords.filter(aroundFinal);
				anchorWords.filter(null);
			}
			else{
				throw new LinkClassifierException("No data for AROUND field, training is impossible");
			}
			finalWords.addAll(aroundWords.getFinalWords());
			finalWords.addAll(urlWords.getFinalWords());
			finalWords.addAll(anchorWords.getFinalWords());
			
			
			String[][] fieldWords = new String[WordField.FIELD_NAMES.length][];
			fieldWords[WordField.AROUND] = aroundWords.getFieldWords();
			fieldWords[WordField.URLFIELD] = urlWords.getFieldWords();
			fieldWords[WordField.ANCHOR] = anchorWords.getFieldWords();

		wrapper.setFeatures(fieldWords);
		
//		System.out.println("FINAL WORDS");
//		for(String w : finalWords){
//			System.out.println(w);
//		}

		selectedFeatures = new String[finalWords.size()];
		finalWords.toArray(selectedFeatures);
		
		
		}
		else if(type == LinkRelevance.TYPE_BACKLINK_BACKWARD){
			Set<String> usedURLTemp = new HashSet<>();
			FrequencyMap urlWords = new FrequencyMap("url",stoplist,stemmer, new FilterData(150,2));
			FrequencyMap contentWords = new FrequencyMap("content",stoplist,stemmer, new FilterData(150,2));

			for (int l = 0; l < allNeighbors.size(); l++) {
				LinkMetadata element = allNeighbors.get(l);
				contentWords.addWords(element.getPageContent());
				if(!usedURLTemp.contains(element.getUrl())){
					usedURLTemp.add(element.getUrl());
					PaginaURL pageParser = new PaginaURL(new URL("http://"),element.getLink().getFile().toString(), stoplist);
					urlWords.addWords(pageParser.palavras());
				}
			}
		
			if(!contentWords.getMap().isEmpty()){
				ArrayList<WordFrequency> contentFinal = contentWords.filter(null);
				urlWords.filter(contentFinal);
			}
			else{
				throw new LinkClassifierException("No data for CONTENT field, training is impossible");
			}
			finalWords.addAll(contentWords.getFinalWords());
			finalWords.addAll(urlWords.getFinalWords());
			
			
			String[][] fieldWords = new String[WordField.FIELD_NAMES.length][];
			fieldWords[WordField.CONTENT] = contentWords.getFieldWords();
			fieldWords[WordField.URLFIELD] = urlWords.getFieldWords();

		wrapper.setFeatures(fieldWords);
		
		

		selectedFeatures = new String[finalWords.size()];
		finalWords.toArray(selectedFeatures);
		
		}
		else if(type == LinkRelevance.TYPE_BACKLINK_FORWARD){
			Set<String> usedURLTemp = new HashSet<>();
			FrequencyMap urlWords = new FrequencyMap("url",stoplist,stemmer, new FilterData(150,2));
			FrequencyMap titleWords = new FrequencyMap("title",stoplist,stemmer, new FilterData(150,2));
			FrequencyMap snippetWords = new FrequencyMap("snippet",stoplist,stemmer, new FilterData(150,2));

			for (int l = 0; l < allNeighbors.size(); l++) {
				LinkMetadata element = allNeighbors.get(l);
				titleWords.addWords(element.getSearchEngineTitleAsArray());
				snippetWords.addWords(element.getSearchEngineSnippetAsArray());
				if(!usedURLTemp.contains(element.getUrl())){
					usedURLTemp.add(element.getUrl());
					PaginaURL pageParser = new PaginaURL(new URL("http://"),element.getLink().getFile().toString(), stoplist);
					urlWords.addWords(pageParser.palavras());
				}
			}
		
			if(!snippetWords.getMap().isEmpty()){
				ArrayList<WordFrequency> snippetFinal = snippetWords.filter(null);
				urlWords.filter(snippetFinal);
				titleWords.filter(null);
			}
			else{
				throw new LinkClassifierException("No data for SNIPPET field, training is impossible");
			}

			finalWords.addAll(titleWords.getFinalWords());
			finalWords.addAll(snippetWords.getFinalWords());
			finalWords.addAll(urlWords.getFinalWords());
			
			
			String[][] fieldWords = new String[WordField.FIELD_NAMES.length][];
			fieldWords[WordField.TITLE] = titleWords.getFieldWords();
			fieldWords[WordField.SNIPPET] = snippetWords.getFieldWords();
			fieldWords[WordField.URLFIELD] = urlWords.getFieldWords();

		wrapper.setFeatures(fieldWords);
		
		

		selectedFeatures = new String[finalWords.size()];
		finalWords.toArray(selectedFeatures);
		
		}
		else{
			throw new IllegalArgumentException("type "+type+" not supported");
		}
		
		return selectedFeatures;
	}

}
