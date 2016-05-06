package focusedCrawler.link;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Vector;

import focusedCrawler.link.LinkStorageConfig.BiparitieGraphRepConfig;
import focusedCrawler.util.persistence.PersistentHashtable;
import focusedCrawler.util.persistence.Tuple;

public class BipartiteGraphRepository {

	private PersistentHashtable<String> parentsGraph;

	private PersistentHashtable<String> childrenGraph;
	
	private PersistentHashtable<LinkMetadata> nodeID; 
	
	private PersistentHashtable<String> url2id;

	private final String separator = "###";
	
	
	public BipartiteGraphRepository(String dataPath, BiparitieGraphRepConfig config) {
	    int cacheSize = 10000;
	    this.parentsGraph = new PersistentHashtable<String>(dataPath + "/" + config.getParentsGraphDirectory(), cacheSize, String.class);
        this.url2id = new PersistentHashtable<String>(dataPath + "/" + config.getUrlIdDirectory(), cacheSize, String.class);
        this.childrenGraph = new PersistentHashtable<String>(dataPath + "/" + config.getChildrenGraphDirectory(), cacheSize, String.class);
        this.nodeID = new PersistentHashtable<LinkMetadata>(dataPath + "/" + config.getNodeIdDirectory(), cacheSize, LinkMetadata.class);
	}
	
	public Tuple<String>[] getParentsGraph() throws Exception{
		return parentsGraph.getTableAsArray();
	}

	public Tuple<String>[] getChildrenGraph() throws Exception{
		return childrenGraph.getTableAsArray();
	}
	
	public String getID(String url){
		return url2id.get(url);
	}
	
	public String getHubURL(String id) throws IOException{
		LinkMetadata lm = nodeID.get(id);
		String url = null;
		if(lm != null){
			url = lm.getUrl();
		}
		return url;
	}
	
	public String getAuthURL(String id){
		LinkMetadata lm = nodeID.get(id);
		String url = null;
		if(lm != null){
			url = lm.getUrl();
		}
		return url;
	}
	
	public String[] getOutlinkIDs(String id){
		String links = childrenGraph.get(id);
		if(links != null){
			return links.split("###");	
		}else{
			return null;
		}
	}

	public String[] getBacklinkIDs(String id){
		String links = parentsGraph.get(id);
		if(links != null){
			return links.split("###");	
		}else{
			return null;
		}

	}
	
	/**
	 * This method returns ALL the LMs that are stored.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public LinkMetadata[] getLMs() throws Exception{
		Tuple<LinkMetadata>[] tuples = nodeID.getTableAsArray();
		LinkMetadata[] lms = new LinkMetadata[tuples.length];
		for (int i = 0; i < lms.length; i++) {
			LinkMetadata lm = tuples[i].getValue();
			if(lm != null){
				lms[i]=lm;
			}
		}
		return lms;
	}
	
	/**
	 * This method returns ALL the LMs of urls target of outlinks.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public LinkMetadata[] getOutlinkLMs() throws Exception{
		Tuple<LinkMetadata>[] tuples = nodeID.getTableAsArray();
		Vector<LinkMetadata> temp = new Vector<LinkMetadata>();
		for (int i = 0; i < tuples.length; i++) {
			LinkMetadata lm = tuples[i].getValue();
			if(lm != null && lm.getIsTargetOfOutlink()){
				temp.add(lm);
			}
		}
		LinkMetadata[] lms = new LinkMetadata[temp.size()];
        return temp.toArray(lms);
	}
	
	/**
	 * This method returns ALL the LMs of urls target of backlinks.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public LinkMetadata[] getBacklinkLMs() throws Exception{
		Tuple<LinkMetadata>[] tuples = nodeID.getTableAsArray();
		Vector<LinkMetadata> temp = new Vector<LinkMetadata>();
		for (int i = 0; i < tuples.length; i++) {
			LinkMetadata lm = tuples[i].getValue();
			if(lm != null && lm.getIsTargetOfBacklink()){
				temp.add(lm);
			}
		}
		LinkMetadata[] lms = new LinkMetadata[temp.size()];
        return temp.toArray(lms);
	}
	
	/**
	 * This method returns LM of the url if it is target of a backlink.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public LinkMetadata getBacklinkLM(URL url) throws MalformedURLException{
		LinkMetadata lm = null;
		String urlId = url2id.get(url.toString());
		if(urlId != null){
			lm = nodeID.get(urlId);
			if(lm != null && lm.getIsTargetOfBacklink()){
				return lm;
			}
		}
		return null;
	}
	
	/**
	 * This method returns LM of the url if it is target of an outlink.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public LinkMetadata getOutlinkLM(URL url) throws MalformedURLException{
		LinkMetadata lm = null;
		URL normalizedURL = url; 
		String urlId = url2id.get(normalizedURL.toString());
		if(urlId != null){
			lm = nodeID.get(urlId);
			if(lm != null && lm.getIsTargetOfOutlink()){
				return lm;
			}
		}
		return null;
	}
	
	/**
	 * This method returns LM of the url if it exists.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public LinkMetadata getLM(URL url) throws MalformedURLException{
		LinkMetadata lm = null;
		URL normalizedURL = url; 
		String urlId = url2id.get(normalizedURL.toString());
		if(urlId != null){
			lm = nodeID.get(urlId);
		}
		return lm;
	}

	/**
	 * This method returns the LMs of the urls targeted by outlinks of this url.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public LinkMetadata[] getOutlinksLM(URL url) throws IOException{
		String urlId = url2id.get(url.toString());
		if(urlId == null){
			return null;
		} else {
			String linksIds = childrenGraph.get(urlId);
			if(linksIds != null){
				String[] linkIds = linksIds.split("###");
				LinkMetadata[] lms = new LinkMetadata[linkIds.length];
				for (int i = 0; i < lms.length; i++) {
					LinkMetadata lm = nodeID.get(linkIds[i]);
					if(lm != null && lm.getIsTargetOfOutlink()){
						lms[i] = lm;	
					}
				}
				return lms;
			}
			return null;
		}
	}
	
	
	/**
	 * This method returns the LMs of the urls targeted by backlinks of this url.
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public LinkMetadata[] getBacklinksLM(URL url) throws IOException {
		String urlId = url2id.get(url.toString());
		if(urlId == null){
			return null;
		}
		String strLinks = parentsGraph.get(urlId);
		if(strLinks == null){
			return null;
		} else {
			Vector<LinkMetadata> tempLMs = new Vector<LinkMetadata> (); 
			String[] linkIds = strLinks.split("###");
			for (int i = 0; i < linkIds.length; i++) {
				LinkMetadata lm = nodeID.get(linkIds[i]);
				if(lm != null && lm.getIsPageInfoSearchEngineSet()){
					tempLMs.add(lm);
				}
			}
			LinkMetadata[] lms = new LinkMetadata[tempLMs.size()];
			tempLMs.toArray(lms);
			return lms;
		}
	}
	

	/**
	 * Insert outlinks from hubs 
	 * @param page
	 */
	
	// WARNING: as implemented, Metadata is not updated if the edge in the graph already exists
	public void insertOutlinks(URL url, LinkMetadata[] lms){
		
		String urlId = getId(url.toString());
		String strCurrentLinks = childrenGraph.get(urlId);
		HashSet<String> currentLinks = parseRecordForwardLink(strCurrentLinks);
		StringBuffer buffer = new StringBuffer();	
		for (int i = 0; i < lms.length; i++) {
			if(lms[i] != null){
				String lnURL = lms[i].getUrl();
				String id = getId(lnURL);
				if(!currentLinks.contains(id)){
					LinkMetadata lm = nodeID.get(id);
					// Create if totally new
					if(lm == null){
						LinkMetadata lm2 = new LinkMetadata(lnURL);
						lm2.updateOutlinkMetadata(lms[i]);
						nodeID.put(id, lm2);
					}
					// Update if exists but not yet target of Outlink (can be target of Backlink...)
					else if(!lm.getIsTargetOfOutlink()){
						lm.updateOutlinkMetadata(lms[i]);
						nodeID.put(id, lm);
					}
					buffer.append(id);
					buffer.append(separator);
					currentLinks.add(id);
				}
				String strLinks = parentsGraph.get(id);
				HashSet<String> tempCurrentLinks = parseRecordBacklink(strLinks);
				if(!tempCurrentLinks.contains(urlId)){
					if(tempCurrentLinks.size() == 0){
						strLinks = urlId + separator;
					}else{
						strLinks = strLinks + urlId + separator;
					}
					parentsGraph.put(id, strLinks);
				}
			}
		}
		if(strCurrentLinks == null){
			strCurrentLinks = buffer.toString();
		} else {
			strCurrentLinks =  strCurrentLinks + buffer.toString();
		}
		if(!strCurrentLinks.equals("")){
			childrenGraph.put(urlId, strCurrentLinks);	
		}
		
	}
	
	/**
	 * Insert backlinks from authorities
	 * @param page
	 * @throws IOException 
	 */
	
	// WARNING: as implemented, Metadata is not updated if the edge in the graph already exists
	public void insertBacklinks(URL url, LinkMetadata[] links) throws IOException{
		String urlId = getId(url.toString());
		String strCurrentLinks = parentsGraph.get(urlId);
		HashSet<String> currentLinks = parseRecordBacklink(strCurrentLinks);
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < links.length; i++) {
			String id = getId(links[i].getBacklinkUrls().elementAt(0));
			if(!currentLinks.contains(id)){
				LinkMetadata lm = nodeID.get(urlId);
				// Create if totally new
				if(lm == null){
					LinkMetadata lm2 = new LinkMetadata(url.toString());
					lm2.updateBacklinkMetadata(links[i]);
					nodeID.put(urlId, lm2);
				}
				// Update if exists but "backlink page metadata" is empty
				else {
					lm.updateBacklinkMetadata(links[i]);
					nodeID.put(urlId, lm);
				}
				buffer.append(id);
				buffer.append(separator);
				currentLinks.add(id);
			}
			
			String strLinks = childrenGraph.get(id);
			HashSet<String> tempCurrentLinks = parseRecordForwardLink(strLinks);
			if(!tempCurrentLinks.contains(urlId)){
				if(tempCurrentLinks.size() == 0){
					strLinks = urlId + separator;
				}else{
					strLinks = strLinks + urlId + separator;
				}
				childrenGraph.put(id, strLinks);
				
				// Create if possible the linkMetadata of the "backlink page"
				if(links[i].getBacklinkUrls().size() != 0 && links[i].getBacklinkSnippets().size() != 0 && links[i].getBacklinkTitles().size() != 0){
					LinkMetadata newLm = new LinkMetadata(links[i].getBacklinkUrls().elementAt(0));
					newLm.setSearchEngineSnippet(links[i].getBacklinkSnippets().elementAt(0));
					newLm.setSearchEngineTitle(links[i].getBacklinkTitles().elementAt(0));
					
					LinkMetadata lm = nodeID.get(id);
					// Create if totally new
					if(lm == null){
						LinkMetadata lm2 = new LinkMetadata(newLm.getUrl());
						lm2.updatePageSearchEngineMetadata(newLm);
						nodeID.put(id, lm2);
					}
					// Update if exists
					else if(!lm.getIsPageInfoSearchEngineSet()){
						lm.updatePageSearchEngineMetadata(newLm);
						nodeID.put(id, lm);
					}
				}
			}
		}
		if(strCurrentLinks == null){
			strCurrentLinks = buffer.toString();
		}else{
			strCurrentLinks =  strCurrentLinks + buffer.toString();
		}
		parentsGraph.put(urlId, strCurrentLinks);	
	}
	
	public void insertPage(URL url, LinkMetadata lm){
		
		String urlId = getId(url.toString());
		LinkMetadata lm2 = nodeID.get(urlId);
		// Create if totally new
		if(lm2 == null){
			LinkMetadata lm3 = lm.clone();
			lm3.setIsPageInfoSet(true);
			nodeID.put(urlId, lm3);
		}
		// Update if exists
		else if(!lm2.getIsPageInfoSet()){
			lm2.updatePageMetadata(lm);
			nodeID.put(urlId, lm2);
		}
	}

	
	
	private String getId(String url){
		String id = url2id.get(url);
		if(id == null){
			String maxId = url2id.get("MAX");
			if(maxId == null){
				maxId = "0";
			}
			int newId = Integer.parseInt(maxId) + 1;
			id = newId+"";
			url2id.put(url, id);
			url2id.put("MAX", id);
		}
		return id;
	}

	public void commit(){
		nodeID.commit();
		url2id.commit();
		parentsGraph.commit();
		childrenGraph.commit();
	}
	
	public void close(){
	    this.commit();
		nodeID.close();
        url2id.close();
        parentsGraph.close();
        childrenGraph.close();
    }
	
	private HashSet<String> parseRecordBacklink(String strLinks){
		HashSet<String> currentLinks = new HashSet<String>();
		if(strLinks != null){
			String[] links = strLinks.split("###");
			for (int i = 0; i < links.length; i++) {
				currentLinks.add(links[i]);
			}
		}
		return currentLinks;
	}

	
	private HashSet<String> parseRecordForwardLink(String strLinks){
		HashSet<String> currentLinks = new HashSet<String>();
		if(strLinks != null){
			String[] linkIds = strLinks.split("###");
			for (int i = 0; i < linkIds.length; i++) {
				currentLinks.add(linkIds[i]);					
			}
		}
		return currentLinks;
	}
	
	
}
