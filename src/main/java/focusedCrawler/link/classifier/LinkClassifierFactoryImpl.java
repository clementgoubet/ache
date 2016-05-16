/*
############################################################################
##
## Copyright (C) 2006-2009 University of Utah. All rights reserved.
##
## This file is part of DeepPeep.
##
## This file may be used under the terms of the GNU General Public
## License version 2.0 as published by the Free Software Foundation
## and appearing in the file LICENSE.GPL included in the packaging of
## this file.  Please review the following to ensure GNU General Public
## Licensing requirements will be met:
## http://www.opensource.org/licenses/gpl-license.php
##
## If you are unsure which license is appropriate for your use (for
## instance, you are interested in developing a commercial derivative
## of DeepPeep), please contact us at deeppeep@sci.utah.edu.
##
## This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
## WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
##
############################################################################
*/
package focusedCrawler.link.classifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import focusedCrawler.link.classifier.builder.LinkMetadataWrapper;
import focusedCrawler.util.ParameterFile;
import focusedCrawler.util.string.StopList;
import focusedCrawler.util.string.StopListArquivo;
import weka.classifiers.Classifier;
import weka.core.Instances;


/**
 * <p>Description: Creates concrete LinkClassifiers</p>
 *
 * <p>Copyright: Copyright (c) 2004</p>
 *
 *
 * @author Luciano Barbosa
 * @version 1.0
 */

public class LinkClassifierFactoryImpl implements LinkClassifierFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(LinkClassifierFactoryImpl.class);

  
  private static StopList stoplist;

  private String modelPath;

  public LinkClassifierFactoryImpl(String stoplistFile, String modelPath) {
    this.modelPath = modelPath;
    try {
		stoplist = new StopListArquivo(stoplistFile);
	} catch (IOException e) {
		throw new IllegalArgumentException("Could not load stopwords from file: "+stoplistFile, e);
	}
  }


  /**
   * This method creates a concrete LinkClassifier
   * @param className String class name of the LinkClassifier
   * @return LinkClassifier
   * @throws LinkClassifierFactoryException
   */
   public LinkClassifier createLinkClassifier(String className) throws LinkClassifierFactoryException {
	   LinkClassifier linkClassifier = null;
	    try {
	        linkClassifier = setClassifier(className);
	        logger.info("LINK_CLASSIFIER: " + linkClassifier.getClass());
	    }
	    catch (IOException ex) {
	        throw new LinkClassifierFactoryException(ex.getMessage(), ex);
	    }
	    catch (ClassNotFoundException ex) {
	        throw new LinkClassifierFactoryException(ex.getMessage(), ex);
	    }
	    return linkClassifier;
  }


  public LinkClassifier setClassifier(String className) throws IOException, ClassNotFoundException{
	  LinkClassifier linkClassifier = null;
	  
	  String featureFilePath = Paths.get(modelPath, "linkclassifier.features").toString();
	  String modelFilePath   = Paths.get(modelPath, "linkclassifier.model").toString();
	  
      if(className.indexOf("LinkClassifierBreadthSearch") != -1){
          ParameterFile config = new ParameterFile(featureFilePath); 
          String[] attributes = config.getParam("ATTRIBUTES", " ");
          
          LinkMetadataWrapper wrapper = new LinkMetadataWrapper(stoplist);
          wrapper.setFeatures(attributes);
          linkClassifier= new LinkClassifierBreadthSearch(wrapper,attributes);
      }
      if(className.indexOf("LinkClassifierBaseline") != -1){
    	  linkClassifier= new LinkClassifierBaseline();
      }
	  if(className.indexOf("LinkClassifierHub") != -1){
		  linkClassifier = new LinkClassifierHub();
	  }
	  if(className.indexOf("LinkClassifierAuthority") != -1){
		  linkClassifier = new LinkClassifierAuthority();
	  }
	  if(className.indexOf("LinkClassifierImpl") != -1){
		  File f1 = new File(featureFilePath);
		  File f2 = new File(modelFilePath);
		  if(f1.exists() && f2.exists()){
			  LMClassifier lnClassifier = null;
			  try{
		    	  lnClassifier = LMClassifier.create(featureFilePath, modelFilePath, stoplist);
			  } catch(IOException e){
				  logger.info("No featureFile or modelFile for LinkClassifier. Using Baseline until LinkClassifier is trained by OnlineLearning.");
			  }
	    	  linkClassifier = new LinkClassifierImpl(lnClassifier);
		  }
		  else{
			  linkClassifier= new LinkClassifierBaseline();
		  }
      }
	  if(className.indexOf("MaxDepthLinkClassifier") != -1){
	      linkClassifier = new MaxDepthLinkClassifier(1);
	  }
	  return linkClassifier;  
  }
  
  public static LinkMetadataWrapper loadWrapper(String[] attributes, StopList stoplist) {
      LinkMetadataWrapper wrapper = new LinkMetadataWrapper(stoplist);
      wrapper.setFeatures(attributes);
      return wrapper;
  }
  
  
  public static LinkClassifier createLinkClassifierImpl(String[] attributes, String[] classValues, Classifier classifier, String className, int levels) throws IOException {
	  LinkClassifier linkClassifier = null;
	  LinkMetadataWrapper wrapper = loadWrapper(attributes, stoplist);
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
	  if(className.indexOf("LinkClassifierImpl") != -1){
		  LMClassifier lnClassifier = new LMClassifier(classifier, insts, wrapper, attributes);
		  linkClassifier = new LinkClassifierImpl(lnClassifier);
	  }
	  if(className.indexOf("LinkClassifierAuthority") != -1){
		  linkClassifier = new LinkClassifierAuthority(classifier, insts, wrapper,attributes);
	  }
	  if(className.indexOf("LinkClassifierHub") != -1){
		  linkClassifier = new LinkClassifierHub(classifier, insts, wrapper,attributes);
	  }
	  return linkClassifier;
  }
}

