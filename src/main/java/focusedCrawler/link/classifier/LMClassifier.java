package focusedCrawler.link.classifier;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.Map;

import weka.classifiers.Classifier;
import weka.core.Instances;
import focusedCrawler.link.LinkMetadata;
import focusedCrawler.link.classifier.builder.Instance;
import focusedCrawler.link.classifier.builder.LinkMetadataWrapper;
import focusedCrawler.util.ParameterFile;
import focusedCrawler.util.string.StopList;

public class LMClassifier {

	private Classifier classifier;
	private Instances instances;
	private LinkMetadataWrapper wrapper;
	private String[] attributes;

	public LMClassifier(Classifier classifier, Instances instances,
	                    LinkMetadataWrapper wrapper, String[] attributes) {
		this.classifier = classifier;
		this.instances = instances;
		this.wrapper = wrapper;
		this.attributes = attributes;
	}
	
	public double[] classify(LinkMetadata lm, int type) throws Exception {
		Map<String, Instance> urlWords = wrapper.extractData(lm, attributes, type);
		Iterator<String> iter = urlWords.keySet().iterator();
		String url = iter.next();
		Instance instance = (Instance)urlWords.get(url);
		double[] values = instance.getValues();
		weka.core.Instance instanceWeka = new weka.core.Instance(1, values);
		instanceWeka.setDataset(instances);
		double[] probs = classifier.distributionForInstance(instanceWeka);
		return probs;
	}
	
	public static LMClassifier create(String featureFilePath,
	                                  String modelFilePath,
	                                  StopList stoplist)
                                      throws ClassNotFoundException,
                                             IOException {
	    ParameterFile config = new ParameterFile(featureFilePath); 
	    String[] attributes = config.getParam("ATTRIBUTES", " ");
	    String[] classValues = config.getParam("CLASS_VALUES", " ");
	    return create(attributes, classValues, modelFilePath, stoplist);
	}
	
	public static LMClassifier create(String[] attributes, String[] classValues,
	                                  String modelFilePath, StopList stoplist)
                                      throws ClassNotFoundException,
                                             IOException {
	    weka.core.FastVector vectorAtt = new weka.core.FastVector();
	    for (int i = 0; i < attributes.length; i++) {
	        vectorAtt.addElement(new weka.core.Attribute(attributes[i]));
	    }
	    weka.core.FastVector classAtt = new weka.core.FastVector();
	    for (int i = 0; i < classValues.length; i++) {
	        classAtt.addElement(classValues[i]);
	    }
	    vectorAtt.addElement(new weka.core.Attribute("class", classAtt));
	    Instances insts = new Instances("link_classification", vectorAtt, 1);
	    insts.setClassIndex(attributes.length);
	    
	    
	    LinkMetadataWrapper wrapper = loadWrapper(attributes, stoplist);
	    
	    Classifier classifier = loadClassifier(modelFilePath);
	    
	    return new LMClassifier(classifier, insts, wrapper, attributes);
	    
	}
    
    public static LinkMetadataWrapper loadWrapper(String[] attributes, StopList stoplist) {
        LinkMetadataWrapper wrapper = new LinkMetadataWrapper(stoplist);
        wrapper.setFeatures(attributes);
        return wrapper;
    }
    
    private static Classifier loadClassifier(String modelFilePath) 
            throws IOException, ClassNotFoundException {
        InputStream is = null;
        try {
            is = new FileInputStream(modelFilePath);
        }
        catch (FileNotFoundException ex1) {
            // FIXME
            ex1.printStackTrace();
        }
        ObjectInputStream objectInputStream = new ObjectInputStream(is);
        Classifier classifier = (Classifier) objectInputStream.readObject();
        objectInputStream.close();
        return classifier;
    }
	
}
