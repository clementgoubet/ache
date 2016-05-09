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

import java.net.MalformedURLException;

import focusedCrawler.link.LinkMetadata;
import focusedCrawler.link.frontier.LinkRelevance;
import focusedCrawler.util.parser.PaginaURL;

/**
 *
 * <p> </p>
 *
 * <p>Description: This classifier uses the naive bayes link classifier to
 * set the link priority.</p>
 *
 * <p>Copyright: Copyright (c) 2004</p>
 *
 * <p> </p>
 *
 * @author Luciano Barbosa
 * @version 1.0
 */

public class LinkClassifierImpl implements LinkClassifier{

	private int[] weights;
	private int intervalRandom = 100;
	private LMClassifier lmClassifier;

	public LinkClassifierImpl(LMClassifier lmClassifier) {
		this.weights = new int[]{2,1,0};
		this.lmClassifier = lmClassifier;
	}

  /**
   * This method classifies links based on the priority set by the
   * naive bayes link classifier.
   * @param page Page
   * @return LinkRelevance[]
   * @throws LinkClassifierException
   */
	
  public LinkRelevance[] classify(PaginaURL page, int type) throws LinkClassifierException {
	  throw new IllegalArgumentException("This classifier is not suited for this use");
  }

  public LinkRelevance classify(LinkMetadata lm, int type) throws LinkClassifierException {
	  // if not yet trained, use the baseline classifier
	  if(lmClassifier==null){
		  return classifyBaseline(lm,type);
	  }
	  
	  LinkRelevance linkRel = null;
	  try {
		  double[] prob = lmClassifier.classify(lm);
		  int classificationResult = -1;
		  double maxProb = -1;
		  for (int i = 0; i < prob.length; i++) {
			  if(prob[i] > maxProb){
				  maxProb = prob[i];
				  classificationResult = i;
			  }
		  }
		  double probability = prob[classificationResult]*100;
		  if(probability == 100){
			  probability = 99;
		  }
		  classificationResult = weights[classificationResult];
		  double result = (classificationResult * intervalRandom) + probability ;  	
		  linkRel = new LinkRelevance(lm.getUrl(),type,result);
	  }catch (MalformedURLException ex) {
		  ex.printStackTrace();
		  throw new LinkClassifierException(ex.getMessage());
	  }catch (Exception ex) {
		  ex.printStackTrace();
		  throw new LinkClassifierException(ex.getMessage());
	  }
	  return linkRel;
  }
  
  public LinkRelevance classifyBaseline(LinkMetadata lm, int type) throws LinkClassifierException {

	  LinkRelevance linkRel = null;
	  try {
		  double[] prob = lmClassifier.classify(lm);
		  int classificationResult = -1;
		  double maxProb = -1;
		  for (int i = 0; i < prob.length; i++) {
			  if(prob[i] > maxProb){
				  maxProb = prob[i];
				  classificationResult = i;
			  }
		  }
		  double probability = prob[classificationResult]*100;
		  if(probability == 100){
			  probability = 99;
		  }
		  classificationResult = weights[classificationResult];
		  double result = (classificationResult * intervalRandom) + probability ;  	
		  linkRel = new LinkRelevance(lm.getUrl(),type,result);
	  }catch (MalformedURLException ex) {
		  ex.printStackTrace();
		  throw new LinkClassifierException(ex.getMessage());
	  }catch (Exception ex) {
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

}
