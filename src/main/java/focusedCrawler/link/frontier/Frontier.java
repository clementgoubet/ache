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

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import focusedCrawler.util.persistence.PersistentHashtable;
import focusedCrawler.util.persistence.Tuple;


public class Frontier {

    protected PersistentHashtable<LinkRelevance> urlRelevance;
    protected Map<String, Integer> scope = null;
    private boolean useScope = false;
    
    public static int LINK_FRONTIER_ID = 1;
	public static int BACKLINK_FRONTIER_ID = 2;

    public Frontier(String directory, int maxCacheUrlsSize, Map<String, Integer> scope) {
        
        this.urlRelevance = new PersistentHashtable<>(directory, maxCacheUrlsSize, LinkRelevance.class);
        
        if (scope == null) {
            this.useScope = false;
            this.scope = new HashMap<String, Integer>();
        } else {
            this.scope = scope;
            this.useScope = true;
        }
    }

    public Frontier(String directory, int maxCacheUrlsSize) {
        this(directory, maxCacheUrlsSize, null);
    }

    public void commit() {
        urlRelevance.commit();
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public HashSet<String> visited(int type) throws Exception {
        HashSet<String> result = new HashSet<String>();
        List<Tuple<LinkRelevance>> tuples = urlRelevance.getTable();
        for (Tuple<LinkRelevance> tuple : tuples) {
			Double value = tuple.getValue().getRelevance(type);
        	if (value != null && value < 0) {
        		result.add(URLDecoder.decode(tuple.getKey(), "UTF-8"));
        	}
        }
        return result;
    }
    
    public HashSet<String> unvisited(int type) throws Exception {
    	HashSet<String> result = new HashSet<String>();
    	List<Tuple<LinkRelevance>> tuples = urlRelevance.getTable();
    	for (Tuple<LinkRelevance> tuple : tuples) {
			Double value = tuple.getValue().getRelevance(type);
    		if (value != null && value > 0) {
    			result.add(URLDecoder.decode(tuple.getKey(), "UTF-8"));
    		}
    	}
    	return result;
    }
    
    public HashSet<String> visitedAuths() throws Exception {
    	HashSet<String> result = new HashSet<String>();
    	List<Tuple<LinkRelevance>> tuples = urlRelevance.getTable();
    	for (Tuple<LinkRelevance> tuple : tuples) {
			Double value = tuple.getValue().getRelevance(LinkRelevance.DEFAULT_TYPE);
    		if (value != null && value < -200) {
    			result.add(URLDecoder.decode(tuple.getKey(), "UTF-8"));
            }
        }
        return result;
    }

	public HashSet<String> visitedLinks() throws Exception {
        HashSet<String> result = new HashSet<String>();
        List<Tuple<LinkRelevance>> tuples = urlRelevance.getTable();
        for (Tuple<LinkRelevance> tuple : tuples) {
            Double value = tuple.getValue().getRelevance(LinkRelevance.DEFAULT_TYPE);
            if (value != null && value < 0) {
            	result.add(URLDecoder.decode(tuple.getKey(), "UTF-8"));
            }
        }
        return result;
    }

	public HashSet<String> unvisitedAuths() throws Exception {
        HashSet<String> result = new HashSet<String>();
        List<Tuple<LinkRelevance>> tuples = urlRelevance.getTable();
        for (Tuple<LinkRelevance> tuple : tuples) {
            Double value = tuple.getValue().getRelevance(LinkRelevance.DEFAULT_TYPE);
            if (value != null && value > 200) {
                result.add(URLDecoder.decode(tuple.getKey(), "UTF-8"));
            }
        }
        return result;
    }

	public HashSet<String> visitedHubs() throws Exception {
        HashSet<String> result = new HashSet<String>();
        List<Tuple<LinkRelevance>> tuples = urlRelevance.getTable();
        for (Tuple<LinkRelevance> tuple : tuples) {
            Double value = tuple.getValue().getRelevance(LinkRelevance.DEFAULT_TYPE);
            if (value != null && value > -200 && value < -100) {
                result.add(URLDecoder.decode(tuple.getKey(), "UTF-8"));
            }
        }
        return result;
    }

	public HashSet<String> unvisitedHubs() throws Exception {
        HashSet<String> result = new HashSet<String>();
        List<Tuple<LinkRelevance>> tuples = urlRelevance.getTable();
        for (Tuple<LinkRelevance> tuple : tuples) {
            Double value = tuple.getValue().getRelevance(LinkRelevance.DEFAULT_TYPE);
            if (value != null && value > 100 && value < 200) {
                result.add(URLDecoder.decode(tuple.getKey(), "UTF-8"));
            }
        }
        return result;
    }

    public void update(LinkRelevance linkRelevance) {
        String url = linkRelevance.getURL().toString();
        LinkRelevance link = urlRelevance.get(url);
        if (link != null) {
            if (link.getRelevance() > 0) { // not visited url
                urlRelevance.put(url, linkRelevance);
            }
        }
    }

    /**
     * This method inserts a new link into the frontier
     * 
     * @param linkRelev
     * @return
     * @throws FrontierPersistentException
     */
    public boolean insert(LinkRelevance linkRelev) throws FrontierPersistentException {
        boolean inserted = false;
        String url = linkRelev.getURL().toString();
        Integer rel = exist(linkRelev);

        if (rel == null && url.toString().length() < 210) {
            urlRelevance.put(url, linkRelev);
            inserted = true;
        }

        return inserted;
    }

    /**
     * It verifies whether a given URL was already visited or does not belong to
     * the scope.
     * 
     * @param linkRelev
     * @return
     * @throws FrontierPersistentException
     */
    public Integer exist(LinkRelevance linkRelev) throws FrontierPersistentException {
        String url = linkRelev.getURL().toString();
        LinkRelevance resStr = urlRelevance.get(url);
        if (resStr != null) {
            return (int) resStr.getRelevance();
        } else {
            Integer result = new Integer(-1);
            if (useScope == true) {
                String host = linkRelev.getURL().getHost();
                if (scope.get(host) != null) {
                    result = null;
                }
            } else {
                result = null;
            }
            return result;
        }
    }

    /**
     * It deletes a URL from frontier (marks as visited).
     * 
     * @param linkRelevance
     * @throws FrontierPersistentException
     */
    public void delete(LinkRelevance linkRelevance) throws FrontierPersistentException {

        String url = linkRelevance.getURL().toString();
        if (exist(linkRelevance) != null) {
            // we don't want to delete the URL file, it is useful to avoid visiting an old url
            urlRelevance.put(url, new LinkRelevance(linkRelevance.getURL(), linkRelevance.getType(), -linkRelevance.getRelevance()));
        }
    }

    public void close() {
        urlRelevance.close();
    }

    public PersistentHashtable<LinkRelevance> getUrlRelevanceHashtable() {
        return urlRelevance;
    }

    public Map<String, Integer> getScope() {
        return scope;
    }
    
    @Override
    public String toString(){
    	Iterator<Tuple<LinkRelevance>> it = urlRelevance.getTable().iterator();
    	String result = "";
    	while(it.hasNext()){
    		Tuple<LinkRelevance> elem = it.next();
    		result+=elem.getValue().toString()+"\n";
    	}
    	return result;
    }

}
