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
import java.net.URL;
import java.util.Random;

import focusedCrawler.link.LinkMetadata;
import focusedCrawler.link.frontier.LinkRelevance;
import focusedCrawler.util.parser.PaginaURL;

/**
 *
 * <p>Description:This class implements a baseline crawler setting the link
 * relevance according to the page relevance given by the form classsifier.</p>
 *
 * <p>Copyright: Copyright (c) 2004</p>
 *
 * <p> </p>
 *
 * @author Luciano Barbosa
 * @version 1.0
 */
public class LinkClassifierBaseline implements LinkClassifier{

  private Random randomGenerator;

  public LinkClassifierBaseline() {
     this.randomGenerator = new Random();
   }

  /**
   * This method classifies pages according to its relevance given by the form.
   *
   * @param page Page
   * @return LinkRelevance[]
   */
  public LinkRelevance[] classify(PaginaURL page, int type) throws LinkClassifierException {
        try {
        	URL[] links;
        	if(type == LinkRelevance.TYPE_BACKLINK_FORWARD || type == LinkRelevance.TYPE_BACKLINK_FORWARD){
        		links = page.links();
        	}
        	else if(type == LinkRelevance.TYPE_BACKLINK_BACKWARD){
        		links = new URL[1];
        		links[0] = page.getURL();
        	}
        	else{
        		throw new IllegalArgumentException("type "+type+" not yet implemented");
        	}
        	LinkRelevance[] linkRelevance = new LinkRelevance[links.length];

        	for (int i = 0; i < links.length; i++) {
            	String url = links[i].toString();
        		double relevance = 100;
        		if(relevance == 100){
        			relevance = relevance + randomGenerator.nextInt(100);
        		}
        		linkRelevance[i] = new LinkRelevance(new URL(url), type, relevance);
			}
        	return linkRelevance;
        }
        catch (MalformedURLException ex) {
        	throw new LinkClassifierException(ex.getMessage(), ex);
        }
  }

  public LinkRelevance classify(LinkMetadata lm, int type) throws LinkClassifierException{
  		double relevance = 100 + randomGenerator.nextInt(100);
  		try {
  			if(lm != null && lm.getUrl() != null){
  				return new LinkRelevance(new URL(lm.getUrl()), type, relevance);
  			}
  			else if(lm != null && lm.getBacklinkUrls().size()>0){
  				return new LinkRelevance(new URL(lm.getBacklinkUrls().elementAt(0)), type, relevance);
  			}
  			else
  				return null;
		} catch (MalformedURLException e) {
			throw new LinkClassifierException(e.getMessage(), e);
		}
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

