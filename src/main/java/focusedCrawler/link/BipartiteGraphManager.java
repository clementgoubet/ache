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
package focusedCrawler.link;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.util.Vector;

import focusedCrawler.link.backlink.BacklinkSurfer;
import focusedCrawler.link.classifier.LinkClassifier;
import focusedCrawler.link.classifier.LinkClassifierException;
import focusedCrawler.link.frontier.Frontier;
import focusedCrawler.link.frontier.FrontierManager;
import focusedCrawler.link.frontier.FrontierPersistentException;
import focusedCrawler.link.frontier.LinkRelevance;
import focusedCrawler.target.model.Page;
import focusedCrawler.util.parser.PaginaURL;

/**
 * This class is responsible to manage the info in the graph (backlinks and outlinks).
 * @author lbarbosa
 *
 */

public class BipartiteGraphManager {

	private FrontierManager frontierManager;
	
	private BacklinkSurfer surfer;
	
	private LinkClassifier backlinkForwardClassifier;

	private LinkClassifier backlinkBackwardClassifier;

	private LinkClassifier outlinkClassifier;

	private BipartiteGraphRepository rep;
	
	private int count = 0;

  //Data structure for stop conditions //////////////////////////
  private int maxPages = 100; //Maximum number of pages per each domain
  private HashMap<String, Integer> domainCounter;//Count number of pages for each domain
  ///////////////////////////////////////////////////////////////
	
	private final int pagesToCommit = 100;
	
	public BipartiteGraphManager(FrontierManager frontierManager, BipartiteGraphRepository rep, LinkClassifier outlinkClassifier) {
		this.frontierManager = frontierManager;
		this.outlinkClassifier = outlinkClassifier;
		this.rep = rep;
		this.domainCounter = new HashMap<String, Integer>();
	}
	

	public BipartiteGraphManager(FrontierManager frontierManager, BipartiteGraphRepository rep, LinkClassifier outlinkClassifier,
								 LinkClassifier backlinkForwardClassifier, LinkClassifier backlinkBackwardClassifier) {
		this.frontierManager = frontierManager;
		this.outlinkClassifier = outlinkClassifier;
		this.backlinkForwardClassifier = backlinkForwardClassifier;
		this.backlinkBackwardClassifier=backlinkBackwardClassifier;
		this.rep = rep;
		this.domainCounter = new HashMap<String, Integer>();
	}

  public void setMaxPages(int max){
    this.maxPages = max;
  }

	public void setBacklinkSurfer(BacklinkSurfer surfer){
		this.surfer = surfer;
	}
	
	public void setBacklinkBackwardClassifier(LinkClassifier classifier){
		this.backlinkBackwardClassifier = classifier;
	}
	
	public void setBacklinkForwardClassifier(LinkClassifier classifier){
		this.backlinkForwardClassifier = classifier;
	}

	public void setOutlinkClassifier(LinkClassifier classifier){
		this.outlinkClassifier = classifier;
	}

	
	public BipartiteGraphRepository getRepository(){
		return this.rep;
	}
	
    public void insertOutlinks(Page page) throws IOException, FrontierPersistentException, LinkClassifierException {
    	
    	LinkMetadata[] lms = rep.getOutlinksLM(page.getURL());
         
        LinkRelevance[] linksRelevance = outlinkClassifier.classify(lms,LinkRelevance.TYPE_FORWARD);
       
        ArrayList<LinkRelevance> temp = new ArrayList<LinkRelevance>();
        HashSet<String> relevantURLs = new HashSet<String>();
        
        if(linksRelevance != null){
	        for (int i = 0; i < linksRelevance.length; i++) {
	            if (frontierManager.isRelevant(linksRelevance[i],Frontier.LINK_FRONTIER_ID)) {
	                            	
	                String url = linksRelevance[i].getURL().toString();
	                if (!relevantURLs.contains(url)) {
	                    
	                    String domain = linksRelevance[i].getTopLevelDomainName();
	                    Integer domainCount = domainCounter.get(domain);
	                    
	                    if (domainCount == null)
	                        domainCount = 0;
	                    
	                    if (domainCount < maxPages) {// Stop Condition
	                        domainCount++;
	                        domainCounter.put(domain, domainCount);
	                        relevantURLs.add(url);
	                        temp.add(linksRelevance[i]);
	                    }
	                    
	                }
	            }
	        }
        }

        LinkRelevance[] filteredLinksRelevance = temp.toArray(new LinkRelevance[relevantURLs.size()]);
        
        /*LinkMetadata[] lms2 = page.getPageURL().getLinkMetadatas();
        for (int i = 0; i < lms2.length; i++) {
            if (!relevantURLs.contains(lms2[i].getUrl())) {
                lms2[i] = null;
            }
        }
        
        rep.insertOutlinks(page.getURL(), lms2);*/
        frontierManager.insert(filteredLinksRelevance,Frontier.LINK_FRONTIER_ID);

        /*if (count == pagesToCommit) {
            rep.commit();
            count = 0;
        }
        count++;*/
    }
	
    // classifies page URL to decide whether to crawl backwards or not
    public void insertBacklinks(Page page) throws IOException, FrontierPersistentException, LinkClassifierException {
    	
        LinkMetadata lm = rep.getLM(page.getURL());
        
        LinkRelevance linkRelevance = backlinkBackwardClassifier.classify(lm,LinkRelevance.TYPE_BACKLINK_BACKWARD);
        
        if(frontierManager.isRelevant(linkRelevance, Frontier.BACKLINK_FRONTIER_ID)){ 
            frontierManager.insert(linkRelevance,Frontier.BACKLINK_FRONTIER_ID);
        }
        
    	//frontierManager.getFrontier().commit();
        if (count == pagesToCommit) {
            rep.commit();
            count = 0;
        }
        count++;
    } 
    
    // grabs backlinks and classifies them
	public void insertBacklinks(URL url) throws IOException, FrontierPersistentException, LinkClassifierException{
		LinkMetadata[] links = rep.getBacklinksLM(url);
		if(links == null || (links != null && links.length < 10)){
			links = surfer.getLMBacklinks(url);	

		}
		if(links != null && links.length > 0){
			LinkRelevance[] linksRelevance = new LinkRelevance[links.length];
			for (int i = 0; i < links.length; i++){
				if(links[i] != null){
					// TITLE TOKENISING MUST BE DOE SOMEWHERE
					/*LinkMetadata lm = new LinkMetadata(new URL(links[i].getUrl()));
					String title = links[i].getBacklinkTitle();
					if(title != null){
						StringTokenizer tokenizer = new StringTokenizer(title," ");
						Vector<String> anchorTemp = new Vector<String>();
						while(tokenizer.hasMoreTokens()){
							 anchorTemp.add(tokenizer.nextToken());
			   		  	}
			   		  	String[] aroundArray = new String[anchorTemp.size()];
			   		  	anchorTemp.toArray(aroundArray);
			   		  	lm.setAround(aroundArray);
					}*/
					linksRelevance[i] = backlinkForwardClassifier.classify(links[i],LinkRelevance.TYPE_BACKLINK_FORWARD);
				}
			}
			frontierManager.insert(linksRelevance,Frontier.LINK_FRONTIER_ID);
			
			URL normalizedURL = new URL(url.getProtocol(), url.getHost(), "/"); 
			rep.insertBacklinks(normalizedURL, links);
		}

/*		if(count == pagesToCommit){
			rep.commit();
			count = 0;
		}
		count++;*/
	}

}
