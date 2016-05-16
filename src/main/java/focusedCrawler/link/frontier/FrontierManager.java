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
package focusedCrawler.link.frontier;

import java.util.Iterator;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import focusedCrawler.link.frontier.selector.LinkSelector;
import focusedCrawler.util.LinkFilter;

/**
 * This class manages the crawler frontier
 * 
 * @author Luciano Barbosa
 * @version 1.0
 */

public class FrontierManager {

    private static final Logger logger = LoggerFactory.getLogger(FrontierManager.class);

    private PriorityQueueLink priorityQueue;

    private Frontier linkFrontier;
    
    private Frontier backlinkFrontier;


    private int linksToLoad;

    private LinkFilter linkFilter;

    private final LinkSelector outlinkSelector;
    
    private final LinkSelector backlinkForwardSelector;
    
    private final LinkSelector backlinkBackwardSelector;

    public FrontierManager(Frontier linkFrontier,
    					   Frontier backlinkFrontier,
                           int maxSizeLinkQueue,
                           int linksToLoad,
                           LinkSelector outlinkSelector,
                           LinkSelector backlinkForwardSelector,
                           LinkSelector backlinkBackwardSelector,
                           LinkFilter linkFilter) {
        this.outlinkSelector = outlinkSelector;
        this.backlinkBackwardSelector = backlinkBackwardSelector;
        this.backlinkForwardSelector = backlinkForwardSelector;
        this.priorityQueue = new PriorityQueueLink(maxSizeLinkQueue);
        this.linkFrontier = linkFrontier;
        this.backlinkFrontier = backlinkFrontier;
        this.linksToLoad = linksToLoad;
        this.linkFilter = linkFilter;
        this.loadQueue(linksToLoad);

    }

    public Frontier getLinkFrontierPersistent() {
        return this.linkFrontier;
    }
    
    public Frontier getBacklinkFrontierPersistent() {
        return this.backlinkFrontier;
    }

    public void clearFrontier() {
        logger.info("Cleaning frontiers... current queue size: " + priorityQueue.size());
        priorityQueue.clear();
        logger.info("# Queue size:" + priorityQueue.size());
    }

    private void loadQueue(int numberOfLinks) {

    	// WEIGHTS SHOULD NOT BE HARDCODED
    	int[] weights = {500,1,1};
    	int total = weights[0];
		if(backlinkForwardSelector != null){
			total+=weights[1];
		}
		if(backlinkBackwardSelector != null){
			total+=weights[2];
		}
    	
        priorityQueue.clear();
        linkFrontier.commit();
        backlinkFrontier.commit();
        Vector<LinkRelevance[]> links = new Vector<LinkRelevance[]>();
		links.add(outlinkSelector.select(linkFrontier, LinkRelevance.TYPE_FORWARD, Math.max(1,Math.round(weights[0]*numberOfLinks/total))));
		if(backlinkForwardSelector != null){
			links.add(backlinkForwardSelector.select(linkFrontier, LinkRelevance.TYPE_BACKLINK_FORWARD, Math.max(1,Math.round(weights[1]*numberOfLinks/total))));
		}
		if(backlinkBackwardSelector != null){
			links.add(backlinkBackwardSelector.select(backlinkFrontier, LinkRelevance.TYPE_BACKLINK_BACKWARD,Math.max(1,Math.round(weights[2]*numberOfLinks/total))));
		}
		for(Iterator<LinkRelevance[]> it = links.iterator(); it.hasNext();){
			LinkRelevance[] item = it.next();
	        for (int i = 0; i < item.length; i++) {
	            priorityQueue.insert(item[i]);
	            //System.out.println("\tSELECTED "+item[i].toString());
	        }
		}
    }

    public boolean isRelevant(LinkRelevance elem, int frontierId) throws FrontierPersistentException {
        if (elem == null || elem.getRelevance() <= 0) {
            return false;
        }
        
        Integer value;
        if(frontierId==Frontier.LINK_FRONTIER_ID){
        	value = linkFrontier.exist(elem);
        }
        else if(frontierId==Frontier.BACKLINK_FRONTIER_ID){
        	value = backlinkFrontier.exist(elem);
        }
        else{
        	throw new FrontierPersistentException("frontierId "+frontierId+" doesn't exist");
        }
        
        if (value != null) {
            return false;
        }

        String url = elem.getURL().toString();
        if (linkFilter.accept(url) == false) {
            return false;
        }

        return true;
    }

    public void insert(LinkRelevance[] linkRelevance, int frontierId) throws FrontierPersistentException {
        for (int i = 0; i < linkRelevance.length; i++) {
            LinkRelevance elem = linkRelevance[i];
            this.insert(elem, frontierId);
        }
    }

    public boolean insert(LinkRelevance linkRelevance, int frontierId) throws FrontierPersistentException {
        boolean insert = isRelevant(linkRelevance, frontierId);
        if (insert) {
        	if(frontierId==Frontier.LINK_FRONTIER_ID){
        		insert = linkFrontier.insert(linkRelevance);
            }
            else if(frontierId==Frontier.BACKLINK_FRONTIER_ID){
            	insert = backlinkFrontier.insert(linkRelevance);
            }
            else{
            	throw new FrontierPersistentException("frontierId "+frontierId+" doesn't exist");
            }
        }
        return insert;
    }

    public LinkRelevance nextURL() throws FrontierPersistentException {

        if(priorityQueue.size() == 0) {
            // Load more links from frontier into the priority queue
            loadQueue(linksToLoad);
        }
        LinkRelevance linkRelev = (LinkRelevance) priorityQueue.pop();
        if (linkRelev == null) {
            return null;
        }

        // Delete from frontiers
        int type = linkRelev.getType();
        if(type == LinkRelevance.TYPE_FORWARD || type == LinkRelevance.TYPE_BACKLINK_FORWARD){
        	linkFrontier.delete(linkRelev);
        }
        else if (type == LinkRelevance.TYPE_BACKLINK_BACKWARD){
        	backlinkFrontier.delete(linkRelev);
        }
        else{
        	throw new FrontierPersistentException("type "+type+" unrecognised");
        }
            
        logger.info("\n> URL:" + linkRelev.getURL() +
        			"\n> TYPE:" + linkRelev.getType() +
                    "\n> REL:" + ((int) linkRelev.getRelevance() / 100) +
                    "\n> RELEV:" + (int)linkRelev.getRelevance());
        
       return linkRelev;
    }
    
    public void close() {
        linkFrontier.commit();
        backlinkFrontier.commit();
        linkFrontier.close();
        backlinkFrontier.close();
    }

    public Frontier getLinkFrontier() {
        return linkFrontier;
    }
    
    public Frontier getBacklinkFrontier() {
        return backlinkFrontier;
    }

}
